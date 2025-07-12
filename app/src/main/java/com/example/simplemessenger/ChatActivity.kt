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
<<<<<<< Updated upstream
=======
import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.result.ActivityResultLauncher
import okhttp3.MultipartBody
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.simplemessenger.ui.Message
import com.example.simplemessenger.ui.MessageAdapter
import android.util.Log
import androidx.activity.viewModels
>>>>>>> Stashed changes

class ChatActivity : AppCompatActivity() {
    private val client = OkHttpClient()
    private val serverUrl = "http://37.113.99.158:5000"

    private var lastMessageCount = 0

    private lateinit var userName: String
    private lateinit var recipient: String
    private lateinit var messagesView: TextView
    private lateinit var chatFile: File
<<<<<<< Updated upstream

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

=======
    private val viewModel: MainViewModel by viewModels()

    private lateinit var messagesRecyclerView: RecyclerView
    private var messageList: MutableList<Message> = mutableListOf()
    private lateinit var messageAdapter: MessageAdapter

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    companion object {
        const val ACTION_CHAT_LIST_UPDATED = "com.example.simplemessenger.ACTION_CHAT_LIST_UPDATED"
    }

>>>>>>> Stashed changes
    // Регистрация запроса разрешения
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Разрешение на уведомления не получено", Toast.LENGTH_SHORT).show()
        }
    }

    private lateinit var pickMediaLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
<<<<<<< Updated upstream

        userName = intent.getStringExtra("userName")!!
        recipient = intent.getStringExtra("recipient")!!
=======
        // Safe intent extra handling
        userName = intent.getStringExtra("userName") ?: ""
        recipient = intent.getStringExtra("recipient") ?: ""
        if (userName.isEmpty() || recipient.isEmpty()) {
            Toast.makeText(this, "User or recipient missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
>>>>>>> Stashed changes
        title = "Чат с $recipient"

        val messageEdit = findViewById<EditText>(R.id.messageEdit)
        val sendButton = findViewById<ImageButton>(R.id.sendButton)
        val attachButton = findViewById<ImageButton>(R.id.attachButton)
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView)

        messageAdapter = MessageAdapter(this, messageList, userName, serverUrl)
        messagesRecyclerView.layoutManager = LinearLayoutManager(this)
        messagesRecyclerView.adapter = messageAdapter

        val chatFileName = "chat_${userName}_$recipient.json"
        chatFile = File(filesDir, chatFileName)

        // Register media picker
        pickMediaLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                sendMedia(uri)
            }
        }

        attachButton.setOnClickListener {
            pickMediaLauncher.launch("image/* video/*")
        }

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

    private fun sendMedia(uri: Uri) {
        val contentResolver = contentResolver
        val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
        val fileName = getFileName(uri)
        contentResolver.openInputStream(uri)?.use { inputStream ->
            val fileBytes = inputStream.readBytes()
            val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("from", userName)
                .addFormDataPart("to", recipient)
                .addFormDataPart("file", fileName, fileBytes.toRequestBody(mimeType.toMediaTypeOrNull()))
                .build()
            val request = Request.Builder()
                .url("$serverUrl/send_media")
                .post(requestBody)
                .build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(this@ChatActivity, "Ошибка отправки файла: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onResponse(call: Call, response: Response) {
                    runOnUiThread {
                        if (response.isSuccessful) {
                            Toast.makeText(this@ChatActivity, "Файл отправлен", Toast.LENGTH_SHORT).show()
                            fetchMessages()
                        } else {
                            Toast.makeText(this@ChatActivity, "Ошибка: ${response.code}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
        } ?: run {
            Toast.makeText(this, "Не удалось открыть файл", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFileName(uri: Uri): String {
        var name = "file"
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && it.moveToFirst()) {
                name = it.getString(nameIndex)
            }
        }
        return name
    }

    private fun fetchMessages() {
        val request = Request.Builder()
            .url("$serverUrl/messages?from=$userName&to=$recipient")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    // Optionally show error in UI
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
                updateMessageList(decryptedArr)
                runOnUiThread {
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
        updateMessageList(decryptedArr)
        lastMessageCount = decryptedArr.length()
    }

    private fun updateMessageList(arr: org.json.JSONArray) {
        messageList.clear()
        for (i in 0 until arr.length()) {
            val m = arr.getJSONObject(i)
            val type = m.optString("type", "text")
            val mediaUrl = m.optString("mediaUrl", null)
            messageList.add(
                Message(
                    from = m.getString("from"),
                    text = m.optString("text", null),
                    date = m.optString("date", null),
                    type = type,
                    mediaUrl = mediaUrl
                )
            )
        }
        runOnUiThread {
            messageAdapter.notifyDataSetChanged()
            messagesRecyclerView.scrollToPosition(messageList.size - 1)
        }
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
            val type = msg.optString("type", "text")
            val newMsg = JSONObject()
            newMsg.put("from", msg.optString("from", "unknown"))
            newMsg.put("date", msg.optString("date", ""))
            if (type == "text") {
                val encryptedText = msg.optString("text", null)
                val decryptedText = if (encryptedText != null) {
                    try {
                        CryptoHelper.decrypt(encryptedText)
                    } catch (e: Exception) {
                        Log.e("CryptoHelper", "Decryption failed", e)
                        "[ошибка расшифровки]"
                    }
                } else {
                    ""
                }
                newMsg.put("text", decryptedText)
            } else {
                newMsg.put("type", type)
                newMsg.put("mediaUrl", msg.optString("mediaUrl", null))
                newMsg.put("text", msg.optString("text", ""))
            }
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

    // TODO: Consider adopting ViewBinding for safer UI code
}
