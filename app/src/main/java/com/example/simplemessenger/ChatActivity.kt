package com.example.simplemessenger

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ChatActivity : AppCompatActivity() {
    private val client = OkHttpClient()
    private val serverUrl = "http://37.113.99.158:5000"

    private var lastMessageCount = 0

    private lateinit var userName: String
    private lateinit var recipient: String
    private lateinit var messagesView: TextView
    private lateinit var chatFile: File

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    // Регистрация запроса разрешения
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Разрешение на уведомления не получено", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        userName = intent.getStringExtra("userName")!!
        recipient = intent.getStringExtra("recipient")!!
        title = "Чат с $recipient"

        val messageEdit = findViewById<EditText>(R.id.messageEdit)
        val sendButton = findViewById<Button>(R.id.sendButton)
        messagesView = findViewById(R.id.messagesView)

        val chatFileName = "chat_${userName}_$recipient.json"
        chatFile = File(filesDir, chatFileName)

        sendButton.setOnClickListener {
            val text = messageEdit.text.toString()
            if (text.isNotBlank()) {
                sendMessage(text)
                messageEdit.setText("")
            }
        }

        checkNotificationPermission()
        loadLocalMessages()

        val handler = android.os.Handler(mainLooper)
        val task = object : Runnable {
            override fun run() {
                fetchMessages()
                handler.postDelayed(this, 3000)
            }
        }
        handler.post(task)
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> {
                    // Уже есть разрешение
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Можно показать объяснение (по желанию)
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    // Запросить разрешение
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun sendMessage(text: String) {
        val timestamp = dateFormat.format(Date())
        val encryptedText = CryptoHelper.encrypt(text)

        val json = JSONObject()
        json.put("from", userName)
        json.put("to", recipient)
        json.put("text", encryptedText)
        json.put("date", timestamp)

        val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("$serverUrl/send")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                saveLocalMessage(json)
                fetchMessages()
            }
        })
    }

    private fun fetchMessages() {
        val request = Request.Builder()
            .url("$serverUrl/messages?from=$userName&to=$recipient")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    messagesView.text = "Ошибка подключения"
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val result = response.body?.string() ?: "[]"
                val arr = JSONArray(result)

                val existing = readLocalMessages()
                for (i in 0 until arr.length()) {
                    val newMsg = arr.getJSONObject(i)
                    if (!existsInArray(existing, newMsg)) {
                        existing.put(newMsg)
                    }
                }
                writeLocalMessages(existing)

                val decryptedArr = decryptMessages(existing)
                runOnUiThread {
                    messagesView.text = formatMessages(decryptedArr)
                    if (decryptedArr.length() > lastMessageCount) {
                        val newMessages = decryptedArr.length() - lastMessageCount
                        showNotification(newMessages)
                        lastMessageCount = decryptedArr.length()
                    }
                }
            }
        })
    }

    private fun showNotification(newMessagesCount: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val builder = NotificationCompat.Builder(this, "messages_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("SimpleMessenger")
            .setContentText("У вас $newMessagesCount новых сообщений в чате с $recipient")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            notify(1001, builder.build())
        }
    }

    private fun loadLocalMessages() {
        val arr = readLocalMessages()
        val decryptedArr = decryptMessages(arr)
        messagesView.text = formatMessages(decryptedArr)
        lastMessageCount = decryptedArr.length()
    }

    private fun readLocalMessages(): JSONArray {
        return try {
            if (!chatFile.exists()) return JSONArray()
            val content = chatFile.readText()
            JSONArray(content)
        } catch (e: Exception) {
            JSONArray()
        }
    }

    private fun writeLocalMessages(arr: JSONArray) {
        chatFile.writeText(arr.toString())
    }

    private fun saveLocalMessage(msg: JSONObject) {
        val arr = readLocalMessages()
        arr.put(msg)
        writeLocalMessages(arr)
    }

    private fun decryptMessages(arr: JSONArray): JSONArray {
        val decrypted = JSONArray()
        for (i in 0 until arr.length()) {
            val msg = arr.getJSONObject(i)
            val encryptedText = msg.getString("text")
            val decryptedText = try {
                CryptoHelper.decrypt(encryptedText)
            } catch (e: Exception) {
                "[ошибка расшифровки]"
            }
            val newMsg = JSONObject()
            newMsg.put("from", msg.getString("from"))
            newMsg.put("text", decryptedText)
            newMsg.put("date", msg.optString("date", ""))
            decrypted.put(newMsg)
        }
        return decrypted
    }

    private fun formatMessages(arr: JSONArray): String {
        val result = StringBuilder()
        for (i in 0 until arr.length()) {
            val m = arr.getJSONObject(i)
            val author = m.getString("from")
            val text = m.getString("text")
            val date = m.optString("date", "время неизвестно")
            result.append("$author [$date]: $text\n")
        }
        return result.toString()
    }

    private fun existsInArray(array: JSONArray, msg: JSONObject): Boolean {
        val text = msg.optString("text")
        val date = msg.optString("date")
        val from = msg.optString("from")
        for (i in 0 until array.length()) {
            val m = array.getJSONObject(i)
            if (
                m.optString("text") == text &&
                m.optString("date") == date &&
                m.optString("from") == from
            ) return true
        }
        return false
    }
}
