package com.example.simplemessenger.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.simplemessenger.R
import com.bumptech.glide.Glide
import android.graphics.drawable.GradientDrawable
import androidx.core.content.ContextCompat
import android.view.animation.AlphaAnimation
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import com.example.simplemessenger.MediaDownloadHelper

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
                if (isSent) {
                    params.marginStart = 80
                    params.marginEnd = 0
                    holder.itemView.layoutDirection = View.LAYOUT_DIRECTION_RTL
                } else {
                    params.marginStart = 0
                    params.marginEnd = 80
                    holder.itemView.layoutDirection = View.LAYOUT_DIRECTION_LTR
                }
                holder.itemView.layoutParams = params
                // Animation
                val anim = AlphaAnimation(0.0f, 1.0f)
                anim.duration = 300
                holder.itemView.startAnimation(anim)
            }
            is ImageViewHolder -> {
                holder.bind(message, currentUser, context, serverUrl)
                val params = holder.itemView.layoutParams as RecyclerView.LayoutParams
                if (isSent) {
                    params.marginStart = 80
                    params.marginEnd = 0
                    holder.itemView.layoutDirection = View.LAYOUT_DIRECTION_RTL
                } else {
                    params.marginStart = 0
                    params.marginEnd = 80
                    holder.itemView.layoutDirection = View.LAYOUT_DIRECTION_LTR
                }
                holder.itemView.layoutParams = params
                val anim = AlphaAnimation(0.0f, 1.0f)
                anim.duration = 300
                holder.itemView.startAnimation(anim)
            }
            is VideoViewHolder -> {
                holder.bind(message, currentUser, context, serverUrl)
                val params = holder.itemView.layoutParams as RecyclerView.LayoutParams
                if (isSent) {
                    params.marginStart = 80
                    params.marginEnd = 0
                    holder.itemView.layoutDirection = View.LAYOUT_DIRECTION_RTL
                } else {
                    params.marginStart = 0
                    params.marginEnd = 80
                    holder.itemView.layoutDirection = View.LAYOUT_DIRECTION_LTR
                }
                holder.itemView.layoutParams = params
                val anim = AlphaAnimation(0.0f, 1.0f)
                anim.duration = 300
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
        private val readStatusView: TextView? = itemView.findViewById(R.id.readStatusView)
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
            // Галочки
            if (isSent && readStatusView != null) {
                readStatusView.visibility = View.VISIBLE
                readStatusView.text = if (message.read == true) "✓✓" else "✓"
            } else if (readStatusView != null) {
                readStatusView.visibility = View.GONE
            }
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
                    "Сегодня, " + SimpleDateFormat("HH:mm").format(msgDate.time)
                msgDate.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                msgDate.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) ->
                    "Вчера, " + SimpleDateFormat("HH:mm").format(msgDate.time)
                else -> SimpleDateFormat("dd.MM HH:mm").format(msgDate.time)
            }
        }
    }

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.imageMessage)
        private val authorView: TextView = itemView.findViewById(R.id.authorMessage)
        private val dateView: TextView = itemView.findViewById(R.id.dateMessage)
        private val avatarView: ImageView = itemView.findViewById(R.id.avatarView)
        private val messageCard: View = itemView.findViewById(R.id.messageCard)
        private val downloadButton: ImageView = itemView.findViewById(R.id.downloadImageButton)
        private val readStatusView: TextView? = itemView.findViewById(R.id.readStatusView)
        fun bind(message: Message, currentUser: String, context: Context, serverUrl: String) {
            val mediaUrl = message.mediaUrl
            val fullUrl = if (mediaUrl != null && !mediaUrl.startsWith("http")) "$serverUrl/$mediaUrl" else mediaUrl
            Glide.with(context).load(fullUrl).into(imageView)
            authorView.text = message.from
            dateView.text = formatDate(message.date)
            avatarView.setImageResource(R.drawable.ic_avatar_placeholder)
            val isSent = message.from == currentUser
            val bgColor = if (isSent) R.color.message_sent_bg else R.color.message_received_bg
            messageCard.setBackgroundTintList(ContextCompat.getColorStateList(context, bgColor))
            // Галочки
            if (isSent && readStatusView != null) {
                readStatusView.visibility = View.VISIBLE
                readStatusView.text = if (message.read == true) "✓✓" else "✓"
            } else if (readStatusView != null) {
                readStatusView.visibility = View.GONE
            }
            // Открытие в полноэкранном режиме
            imageView.setOnClickListener {
                val intent = android.content.Intent(context, com.example.simplemessenger.FullscreenMediaActivity::class.java)
                intent.putExtra("media_url", fullUrl)
                intent.putExtra("media_type", "image")
                context.startActivity(intent)
            }
            // Скачивание
            downloadButton.setOnClickListener {
                MediaDownloadHelper.downloadFile(context, fullUrl, "image")
            }
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
                    "Сегодня, " + SimpleDateFormat("HH:mm").format(msgDate.time)
                msgDate.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                msgDate.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) ->
                    "Вчера, " + SimpleDateFormat("HH:mm").format(msgDate.time)
                else -> SimpleDateFormat("dd.MM HH:mm").format(msgDate.time)
            }
        }
    }

    class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val videoThumbnail: ImageView = itemView.findViewById(R.id.videoThumbnail)
        private val playIcon: ImageView = itemView.findViewById(R.id.playIcon)
        private val loadingIndicator: View = itemView.findViewById(R.id.loadingIndicator)
        private val authorView: TextView = itemView.findViewById(R.id.authorMessage)
        private val dateView: TextView = itemView.findViewById(R.id.dateMessage)
        private val avatarView: ImageView = itemView.findViewById(R.id.avatarView)
        private val messageCard: View = itemView.findViewById(R.id.messageCard)
        private val downloadButton: ImageView = itemView.findViewById(R.id.downloadVideoButton)
        private val readStatusView: TextView? = itemView.findViewById(R.id.readStatusView)
        fun bind(message: Message, currentUser: String, context: Context, serverUrl: String) {
            val mediaUrl = message.mediaUrl
            val fullUrl = if (mediaUrl != null && !mediaUrl.startsWith("http")) "$serverUrl/$mediaUrl" else mediaUrl
            
            // Загружаем превью видео с помощью Glide
            loadingIndicator.visibility = View.VISIBLE
            playIcon.visibility = View.GONE
            
            Glide.with(context)
                .load(fullUrl)
                .placeholder(R.drawable.video_placeholder)
                .error(R.drawable.video_placeholder)
                .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(
                        e: com.bumptech.glide.load.engine.GlideException?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        loadingIndicator.visibility = View.GONE
                        playIcon.visibility = View.VISIBLE
                        return false
                    }
                    
                    override fun onResourceReady(
                        resource: android.graphics.drawable.Drawable,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                        dataSource: com.bumptech.glide.load.DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        loadingIndicator.visibility = View.GONE
                        playIcon.visibility = View.VISIBLE
                        return false
                    }
                })
                .into(videoThumbnail)
            
            authorView.text = message.from
            dateView.text = formatDate(message.date)
            avatarView.setImageResource(R.drawable.ic_avatar_placeholder)
            val isSent = message.from == currentUser
            val bgColor = if (isSent) R.color.message_sent_bg else R.color.message_received_bg
            messageCard.setBackgroundTintList(ContextCompat.getColorStateList(context, bgColor))
            // Галочки
            if (isSent && readStatusView != null) {
                readStatusView.visibility = View.VISIBLE
                readStatusView.text = if (message.read == true) "✓✓" else "✓"
            } else if (readStatusView != null) {
                readStatusView.visibility = View.GONE
            }
            // Открытие в полноэкранном режиме при клике на превью
            videoThumbnail.setOnClickListener {
                val intent = android.content.Intent(context, com.example.simplemessenger.FullscreenMediaActivity::class.java)
                intent.putExtra("media_url", fullUrl)
                intent.putExtra("media_type", "video")
                context.startActivity(intent)
            }
            // Скачивание
            downloadButton.setOnClickListener {
                MediaDownloadHelper.downloadFile(context, fullUrl, "video")
            }
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
                    "Сегодня, " + SimpleDateFormat("HH:mm").format(msgDate.time)
                msgDate.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                msgDate.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) ->
                    "Вчера, " + SimpleDateFormat("HH:mm").format(msgDate.time)
                else -> SimpleDateFormat("dd.MM HH:mm").format(msgDate.time)
            }
        }
    }
} 