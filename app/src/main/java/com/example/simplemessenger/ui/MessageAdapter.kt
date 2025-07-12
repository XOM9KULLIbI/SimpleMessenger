package com.example.simplemessenger.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.VideoView
import androidx.recyclerview.widget.RecyclerView
import com.example.simplemessenger.R
import com.bumptech.glide.Glide
import android.graphics.drawable.GradientDrawable
import androidx.core.content.ContextCompat
import android.view.animation.AlphaAnimation
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

class MessageAdapter(private val context: Context, private val messages: List<Message>, private val currentUser: String, private val serverUrl: String) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        private const val TYPE_TEXT = 0
        private const val TYPE_IMAGE = 1
        private const val TYPE_VIDEO = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (messages[position].type) {
            "image" -> TYPE_IMAGE
            "video" -> TYPE_VIDEO
            else -> TYPE_TEXT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_IMAGE -> {
                val view = inflater.inflate(R.layout.item_message_image, parent, false)
                ImageViewHolder(view)
            }
            TYPE_VIDEO -> {
                val view = inflater.inflate(R.layout.item_message_video, parent, false)
                VideoViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_message_text, parent, false)
                TextViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        val isSent = message.from == currentUser
        when (holder) {
            is TextViewHolder -> {
                holder.bind(message, currentUser, context)
                // Alignment
                val params = holder.itemView.layoutParams as RecyclerView.LayoutParams
                params.marginStart = if (isSent) 80 else 0
                params.marginEnd = if (isSent) 0 else 80
                holder.itemView.layoutParams = params
                holder.itemView.layoutDirection = if (isSent) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
                // Animation
                val anim = AlphaAnimation(0.0f, 1.0f)
                anim.duration = 400
                holder.itemView.startAnimation(anim)
            }
            is ImageViewHolder -> {
                holder.bind(message, currentUser, context, serverUrl)
                val params = holder.itemView.layoutParams as RecyclerView.LayoutParams
                params.marginStart = if (isSent) 80 else 0
                params.marginEnd = if (isSent) 0 else 80
                holder.itemView.layoutParams = params
                holder.itemView.layoutDirection = if (isSent) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
                val anim = AlphaAnimation(0.0f, 1.0f)
                anim.duration = 400
                holder.itemView.startAnimation(anim)
            }
            is VideoViewHolder -> {
                holder.bind(message, currentUser, context, serverUrl)
                val params = holder.itemView.layoutParams as RecyclerView.LayoutParams
                params.marginStart = if (isSent) 80 else 0
                params.marginEnd = if (isSent) 0 else 80
                holder.itemView.layoutParams = params
                holder.itemView.layoutDirection = if (isSent) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
                val anim = AlphaAnimation(0.0f, 1.0f)
                anim.duration = 400
                holder.itemView.startAnimation(anim)
            }
        }
    }

    override fun getItemCount(): Int = messages.size

    class TextViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.textMessage)
        private val authorView: TextView = itemView.findViewById(R.id.authorMessage)
        private val dateView: TextView = itemView.findViewById(R.id.dateMessage)
        private val avatarView: ImageView = itemView.findViewById(R.id.avatarView)
        private val avatarInitials: TextView = itemView.findViewById(R.id.avatarInitials)
        private val messageCard: View = itemView.findViewById(R.id.messageCard)
        fun bind(message: Message, currentUser: String, context: Context) {
            textView.text = message.text
            authorView.text = message.from
            dateView.text = formatDate(message.date)
            // Avatar initials
            val firstLetter = message.from.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            avatarInitials.text = firstLetter
            avatarView.setImageResource(R.drawable.ic_avatar_placeholder)
            // Bubble background
            val isSent = message.from == currentUser
            val bgColor = if (isSent) R.color.message_sent_bg else R.color.message_received_bg
            messageCard.setBackgroundTintList(ContextCompat.getColorStateList(context, bgColor))
        }
        private fun formatDate(dateStr: String?): String {
            if (dateStr.isNullOrEmpty()) return ""
            val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm")
            val now = Calendar.getInstance()
            val msgDate = Calendar.getInstance()
            try {
                msgDate.time = sdf.parse(dateStr) ?: Date()
            } catch (e: Exception) {
                return dateStr
            }
            val today = Calendar.getInstance()
            val yesterday = Calendar.getInstance()
            yesterday.add(Calendar.DATE, -1)
            return when {
                msgDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                msgDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) ->
                    "Today, " + SimpleDateFormat("HH:mm").format(msgDate.time)
                msgDate.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                msgDate.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) ->
                    "Yesterday, " + SimpleDateFormat("HH:mm").format(msgDate.time)
                else -> dateStr
            }
        }
    }

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.imageMessage)
        private val authorView: TextView = itemView.findViewById(R.id.authorMessage)
        private val dateView: TextView = itemView.findViewById(R.id.dateMessage)
        private val avatarView: ImageView = itemView.findViewById(R.id.avatarView)
        private val messageCard: View = itemView.findViewById(R.id.messageCard)
        fun bind(message: Message, currentUser: String, context: Context, serverUrl: String) {
            val mediaUrl = message.mediaUrl
            val fullUrl = if (mediaUrl != null && !mediaUrl.startsWith("http")) "$serverUrl/$mediaUrl" else mediaUrl
            Glide.with(context).load(fullUrl).into(imageView)
            authorView.text = message.from
            dateView.text = message.date
            avatarView.setImageResource(R.drawable.ic_avatar_placeholder)
            val isSent = message.from == currentUser
            val bgColor = if (isSent) R.color.message_sent_bg else R.color.message_received_bg
            messageCard.setBackgroundTintList(ContextCompat.getColorStateList(context, bgColor))
        }
    }

    class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val videoView: VideoView = itemView.findViewById(R.id.videoMessage)
        private val authorView: TextView = itemView.findViewById(R.id.authorMessage)
        private val dateView: TextView = itemView.findViewById(R.id.dateMessage)
        private val avatarView: ImageView = itemView.findViewById(R.id.avatarView)
        private val messageCard: View = itemView.findViewById(R.id.messageCard)
        fun bind(message: Message, currentUser: String, context: Context, serverUrl: String) {
            val mediaUrl = message.mediaUrl
            val fullUrl = if (mediaUrl != null && !mediaUrl.startsWith("http")) "$serverUrl/$mediaUrl" else mediaUrl
            videoView.setVideoPath(fullUrl)
            videoView.seekTo(100)
            authorView.text = message.from
            dateView.text = message.date
            avatarView.setImageResource(R.drawable.ic_avatar_placeholder)
            val isSent = message.from == currentUser
            val bgColor = if (isSent) R.color.message_sent_bg else R.color.message_received_bg
            messageCard.setBackgroundTintList(ContextCompat.getColorStateList(context, bgColor))
        }
    }
} 