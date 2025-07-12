package com.example.simplemessenger.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.example.simplemessenger.R

// Данные о чате
data class ChatListItem(
    val name: String,
    val lastMessage: String?,
    val time: String?,
    val unreadCount: Int
)

class ChatListAdapter(private val context: Context, private var chats: List<ChatListItem>) : BaseAdapter() {
    override fun getCount(): Int = chats.size
    override fun getItem(position: Int): Any = chats[position]
    override fun getItemId(position: Int): Long = position.toLong()

    fun updateData(newChats: List<ChatListItem>) {
        chats = newChats
        notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_chat, parent, false)
        val chat = chats[position]
        val nameView = view.findViewById<TextView>(R.id.chatName)
        val lastMsgView = view.findViewById<TextView>(R.id.lastMessage)
        val timeView = view.findViewById<TextView>(R.id.timeView)
        val unreadBadge = view.findViewById<TextView>(R.id.unreadBadge)
        val avatarView = view.findViewById<ImageView>(R.id.avatarView)

        nameView.text = chat.name
        lastMsgView.text = chat.lastMessage ?: ""
        timeView.text = chat.time ?: ""
        avatarView.setImageResource(R.drawable.ic_avatar_placeholder)

        if (chat.unreadCount > 0) {
            unreadBadge.visibility = View.VISIBLE
            unreadBadge.text = chat.unreadCount.toString()
        } else {
            unreadBadge.visibility = View.GONE
        }
        return view
    }
} 