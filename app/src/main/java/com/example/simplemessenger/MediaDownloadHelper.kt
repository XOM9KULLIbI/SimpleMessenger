package com.example.simplemessenger

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast

object MediaDownloadHelper {
    fun downloadFile(context: Context, url: String?, type: String) {
        if (url.isNullOrEmpty()) return
        
        try {
            val fileName = generateFileName(url, type)
            val destinationDir = when (type) {
                "image" -> Environment.DIRECTORY_PICTURES
                "video" -> Environment.DIRECTORY_MOVIES
                else -> Environment.DIRECTORY_DOWNLOADS
            }
            
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(fileName)
                .setDescription("Загрузка $type из SimpleMessenger")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(destinationDir, "SimpleMessenger/$fileName")
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
            
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            Toast.makeText(context, "Загрузка $type начата...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка при загрузке: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun generateFileName(url: String?, type: String): String {
        val originalFileName = url?.substringAfterLast('/') ?: "media_file"
        val timestamp = System.currentTimeMillis()
        
        // Если файл не имеет расширения, добавляем соответствующее
        return if (originalFileName.contains('.')) {
            "${timestamp}_$originalFileName"
        } else {
            val extension = when (type) {
                "image" -> "jpg"
                "video" -> "mp4"
                else -> "bin"
            }
            "${timestamp}_$originalFileName.$extension"
        }
    }
} 