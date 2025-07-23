package com.example.simplemessenger

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.simplemessenger.R
import com.example.simplemessenger.ThemeManager

class FullscreenMediaActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Применить сохранённую тему ДО setContentView
        val currentTheme = ThemeManager.getCurrentTheme(this)
        ThemeManager.applyTheme(this, currentTheme)
        
        setContentView(R.layout.activity_fullscreen_media)

        val imageView = findViewById<ImageView>(R.id.fullscreenImageView)
        val videoView = findViewById<VideoView>(R.id.fullscreenVideoView)
        val downloadButton = findViewById<ImageButton>(R.id.downloadButton)
        val closeButton = findViewById<ImageButton>(R.id.closeButton)

        val mediaUrl = intent.getStringExtra("media_url") ?: return finish()
        val mediaType = intent.getStringExtra("media_type") ?: "image"

        if (mediaType == "image") {
            imageView.visibility = ImageView.VISIBLE
            videoView.visibility = VideoView.GONE
            Glide.with(this).load(mediaUrl).into(imageView)
        } else {
            imageView.visibility = ImageView.GONE
            videoView.visibility = VideoView.VISIBLE
            videoView.setVideoURI(Uri.parse(mediaUrl))
            videoView.setOnPreparedListener { mediaPlayer ->
                // Показываем стандартные элементы управления видео
                val mediaController = android.widget.MediaController(this)
                mediaController.setAnchorView(videoView)
                videoView.setMediaController(mediaController)
                mediaPlayer.isLooping = false
                videoView.requestFocus()
                videoView.start()
            }
            // Обработка ошибок
            videoView.setOnErrorListener { _, what, extra ->
                Toast.makeText(this, "Ошибка воспроизведения видео", Toast.LENGTH_SHORT).show()
                false
            }
        }

        downloadButton.setOnClickListener {
            MediaDownloadHelper.downloadFile(this, mediaUrl, mediaType)
        }
        closeButton.setOnClickListener { finish() }
    }
} 