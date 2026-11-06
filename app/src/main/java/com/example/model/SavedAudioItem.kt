package com.example.model

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SavedAudioItem(
    val id: String,
    val title: String,
    val effectName: String,
    val durationMs: Long,
    val file: File,
    val createdAt: Long = System.currentTimeMillis(),
    val sizeBytes: Long = 0L
) {
    val formattedDuration: String
        get() {
            val totalSecs = (durationMs / 1000).toInt()
            val minutes = totalSecs / 60
            val seconds = totalSecs % 60
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }

    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
            return sdf.format(Date(createdAt))
        }

    val formattedSize: String
        get() {
            if (sizeBytes <= 0) return "0 KB"
            val kb = sizeBytes / 1024.0
            return if (kb > 1024) {
                String.format(Locale.getDefault(), "%.1f MB", kb / 1024.0)
            } else {
                String.format(Locale.getDefault(), "%.0f KB", kb)
            }
        }
}
