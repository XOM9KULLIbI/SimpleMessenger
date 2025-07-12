package com.example.simplemessenger.ui

data class Message(
    val from: String,
    val text: String? = null,
    val date: String? = null,
    val type: String = "text", // "text", "image", "video"
    val mediaUrl: String? = null,
    val read: Boolean? = null
) 