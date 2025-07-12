package com.example.simplemessenger

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs: SharedPreferences = application.getSharedPreferences("simplemessenger_prefs", 0)
    private val _chatList = MutableStateFlow<Set<String>>(prefs.getStringSet("chatList", emptySet()) ?: emptySet())
    val chatList: StateFlow<Set<String>> = _chatList.asStateFlow()

    fun loadChatList() {
        _chatList.value = prefs.getStringSet("chatList", emptySet()) ?: emptySet()
    }

    fun addChat(name: String) {
        val current = prefs.getStringSet("chatList", emptySet())?.toMutableSet() ?: mutableSetOf()
        if (current.add(name)) {
            prefs.edit().putStringSet("chatList", current).apply()
            _chatList.value = current
        }
    }
} 