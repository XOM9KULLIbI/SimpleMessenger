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
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatDelegate
import com.example.simplemessenger.ui.ChatListAdapter
import com.example.simplemessenger.ui.ChatListItem
import org.json.JSONArray
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.util.Log
import com.example.simplemessenger.ThemeManager

class MainActivity : AppCompatActivity() {
    private lateinit var userName: String
    private lateinit var prefs: SharedPreferences
    private lateinit var listView: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private val chatList = mutableSetOf<String>()  // хранит всех собеседников
    private lateinit var chatAdapter: ChatListAdapter
    private var chatItems: MutableList<ChatListItem> = mutableListOf()
    private var chatListHandler: android.os.Handler? = null
    private var chatListRunnable: Runnable? = null
    private val chatListUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("MainActivity", "Получен broadcast для обновления списка чатов")
            loadChatsWithUnread()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Применить сохранённую тему ДО setContentView
        val currentTheme = ThemeManager.getCurrentTheme(this)
        ThemeManager.applyTheme(this, currentTheme)
        
        setContentView(R.layout.activity_main)
        
        prefs = getSharedPreferences("simplemessenger_prefs", MODE_PRIVATE)
        userName = prefs.getString("username", "") ?: ""
        
        listView = findViewById(R.id.chatListView)
        chatAdapter = ChatListAdapter(this, chatItems)
        listView.adapter = chatAdapter

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

        // Запускаем автообновление только если пользователь зарегистрирован
        if (userName.isNotEmpty()) {
            startAutoUpdate()
        }

        if (userName.isEmpty()) {
            askForName()
        } else {
            setupUI()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(chatListUpdateReceiver, IntentFilter("com.example.simplemessenger.ACTION_CHAT_LIST_UPDATED"), Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(chatListUpdateReceiver, IntentFilter("com.example.simplemessenger.ACTION_CHAT_LIST_UPDATED"))
        }
    }

    private fun startAutoUpdate() {
        if (chatListHandler == null) {
            Log.d("MainActivity", "Запуск автообновления чатов")
            chatListHandler = android.os.Handler(mainLooper)
            chatListRunnable = object : Runnable {
                override fun run() {
                    if (!isFinishing && !isDestroyed) {
                        Log.d("MainActivity", "Автообновление чатов...")
                        loadChatsWithUnread()
                        chatListHandler?.postDelayed(this, 5000)
                    }
                }
            }
            chatListHandler?.post(chatListRunnable!!)
        }
    }

    private fun stopAutoUpdate() {
        chatListHandler?.removeCallbacksAndMessages(null)
        chatListHandler = null
        chatListRunnable = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAutoUpdate()
        unregisterReceiver(chatListUpdateReceiver)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_theme, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_theme_light -> {
                ThemeManager.saveTheme(this, ThemeManager.THEME_LIGHT)
                recreate()
                true
            }
            R.id.action_theme_dark -> {
                ThemeManager.saveTheme(this, ThemeManager.THEME_DARK)
                recreate()
                true
            }
            R.id.action_theme_colorful -> {
                ThemeManager.saveTheme(this, ThemeManager.THEME_COLORFUL)
                recreate()
                true
            }
            else -> super.onOptionsItemSelected(item)
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
                                startAutoUpdate() // Запускаем автообновление после регистрации
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
        chatAdapter = ChatListAdapter(this, chatItems)
        listView.adapter = chatAdapter
        loadChatsWithUnread()

        val newChatButton = findViewById<Button>(R.id.newChatButton)
        newChatButton.setOnClickListener {
            val input = EditText(this)
            AlertDialog.Builder(this)
                .setTitle("Кому написать?")
                .setView(input)
                .setPositiveButton("OK") { _, _ ->
                    val recipient = input.text.toString().trim()
                    if (recipient.isNotEmpty() && recipient != userName) {
                        openChat(recipient)
                    } else {
                        Toast.makeText(this, "Некорректное имя", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Отмена", null)
                .show()
        }

        val refreshButton = findViewById<Button>(R.id.refreshButton)
        refreshButton.setOnClickListener {
            Log.d("MainActivity", "Ручное обновление списка чатов")
            loadChatsWithUnread()
            Toast.makeText(this, "Обновление...", Toast.LENGTH_SHORT).show()
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            val chat = chatItems[position]
            openChat(chat.name)
        }
    }

    private fun loadChatsWithUnread() {
        Log.d("MainActivity", "Загрузка чатов с непрочитанными сообщениями")
        // Загружаем список чатов из памяти
        val chatNames = prefs.getStringSet("chatList", emptySet()) ?: emptySet()
        Log.d("MainActivity", "Найдено чатов в памяти: ${chatNames.size}")
        val items = mutableListOf<ChatListItem>()
        val client = OkHttpClient()
        val url = "http://37.113.99.158:5000/messages"
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("MainActivity", "Ошибка загрузки сообщений: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Ошибка загрузки сообщений", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                Log.d("MainActivity", "Получен ответ от сервера: ${response.code}")
                val arr = JSONArray(response.body?.string() ?: "[]")
                Log.d("MainActivity", "Получено сообщений: ${arr.length()}")
                for (chatName in chatNames) {
                    var lastMsg: String? = null
                    var lastTime: String? = null
                    var unread = 0
                    for (i in 0 until arr.length()) {
                        val msg = arr.getJSONObject(i)
                        val isToMe = msg.optString("to") == userName && msg.optString("from") == chatName
                        val isFromMe = msg.optString("from") == userName && msg.optString("to") == chatName
                        if (isToMe || isFromMe) {
                            lastMsg = msg.optString("text", msg.optString("type", ""))
                            lastTime = msg.optString("date", "")
                        }
                        if (isToMe && !msg.optBoolean("read", true)) {
                            unread++
                        }
                    }
                    items.add(ChatListItem(chatName, lastMsg, lastTime, unread))
                }
                runOnUiThread {
                    Log.d("MainActivity", "Обновление списка чатов: ${items.size} чатов")
                    chatItems.clear()
                    chatItems.addAll(items)
                    chatAdapter.updateData(chatItems)
                }
            }
        })
    }

    private fun openChat(recipient: String) {
        // Отметить сообщения как прочитанные
        val client = OkHttpClient()
        val url = "http://37.113.99.158:5000/mark_read"
        val json = JSONObject().apply {
            put("from", recipient)
            put("to", userName)
        }
        val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder().url(url).post(body).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // ничего
            }
            override fun onResponse(call: Call, response: Response) {
                // После отметки обновить список чатов
                loadChatsWithUnread()
            }
        })
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("userName", userName)
        intent.putExtra("recipient", recipient)
        startActivity(intent)
    }

    // TODO: Consider adopting ViewBinding for safer UI code
}
