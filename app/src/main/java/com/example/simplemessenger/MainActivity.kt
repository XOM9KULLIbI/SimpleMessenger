package com.example.simplemessenger

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import okhttp3.*

class MainActivity : AppCompatActivity() {
    private lateinit var userName: String
    private lateinit var prefs: SharedPreferences
    private lateinit var listView: ListView
<<<<<<< Updated upstream
    private val chatList = mutableSetOf<String>()  // хранит всех собеседников
=======
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var prefs: SharedPreferences
    private var userName: String = ""
>>>>>>> Stashed changes

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = getSharedPreferences("simplemessenger_prefs", Context.MODE_PRIVATE)
        userName = prefs.getString("username", "") ?: ""
        if (userName.isEmpty()) {
            askForName()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Messages"
            val descriptionText = "Уведомления о новых сообщениях"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("messages_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }


        prefs = getSharedPreferences("messenger_prefs", MODE_PRIVATE)
        userName = prefs.getString("username", null) ?: ""

        if (userName.isEmpty()) {
            askForName()
        } else {
            setupUI()
        }
    }

    private fun askForName() {
        val input = EditText(this)
        AlertDialog.Builder(this)
            .setTitle("Введите имя (не изменить потом)")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("OK") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isEmpty()) {
                    askForName()
                    return@setPositiveButton
                }

                // Отправка запроса на сервер для регистрации
                val json = JSONObject().apply {
                    put("username", name)
                }
                val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
                val request = Request.Builder()
                    .url("http://37.113.99.158:5000/register")  // Убедись, что сервер доступен
                    .post(body)
                    .build()

                OkHttpClient().newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Ошибка подключения", Toast.LENGTH_LONG).show()
                            askForName()
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (response.code == 409) {
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, "Имя уже занято", Toast.LENGTH_LONG).show()
                                askForName()
                            }
                        } else if (response.isSuccessful) {
                            prefs.edit().putString("username", name).apply()
                            userName = name
                            runOnUiThread {
                                setupUI()
                            }
                        } else {
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, "Ошибка: ${response.code}", Toast.LENGTH_LONG).show()
                                askForName()
                            }
                        }
                    }
                })
            }
            .show()
    }

    private fun setupUI() {
        listView = findViewById(R.id.chatListView)

        // Загружаем список чатов из памяти
        chatList.addAll(prefs.getStringSet("chatList", emptySet()) ?: emptySet())
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, chatList.toMutableList())
        listView.adapter = adapter

        val newChatButton = findViewById<Button>(R.id.newChatButton)
        newChatButton.setOnClickListener {
            val input = EditText(this)
            AlertDialog.Builder(this)
                .setTitle("Кому написать?")
                .setView(input)
                .setPositiveButton("OK") { _, _ ->
                    val recipient = input.text.toString().trim()
                    if (recipient.isNotEmpty() && recipient != userName) {
                        chatList.add(recipient)

                        // Обновляем сохранённый список
                        prefs.edit().putStringSet("chatList", chatList).apply()

                        // Обновляем адаптер
                        adapter.clear()
                        adapter.addAll(chatList)
                        adapter.notifyDataSetChanged()

                        // Переход в чат
                        openChat(recipient)
                    } else {
                        Toast.makeText(this, "Некорректное имя", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Отмена", null)
                .show()
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            val recipient = adapter.getItem(position)
            if (recipient != null) {
                openChat(recipient)
            }
        }


    // Загрузка ранее сохранённого списка чатов
        chatList.addAll(prefs.getStringSet("chatList", emptySet()) ?: emptySet())

        listView.setOnItemClickListener { _, _, position, _ ->
            val recipient = adapter.getItem(position)
            openChat(recipient!!)
        }
    }

    private fun openChat(recipient: String) {
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("userName", userName)
        intent.putExtra("recipient", recipient)
        startActivity(intent)
    }

    // TODO: Consider adopting ViewBinding for safer UI code
}
