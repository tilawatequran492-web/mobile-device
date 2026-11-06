package com.example.audio

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.model.SavedAudioItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

object AudioExportManager {

    private fun getExportsDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(null) ?: context.filesDir, "VoiceChangerExports")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    suspend fun exportAudio(
        context: Context,
        samples: ShortArray,
        sampleRate: Int,
        title: String,
        effectName: String
    ): Result<SavedAudioItem> = withContext(Dispatchers.IO) {
        try {
            val dir = getExportsDir(context)
            val cleanTitle = title.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "VoiceClip" }
            val timestamp = System.currentTimeMillis()
            val fileName = "${cleanTitle}_${effectName.lowercase().replace(" ", "_")}_${timestamp % 10000}.wav"
            val file = File(dir, fileName)

            writeWavFile(file, samples, sampleRate)

            val durationMs = if (sampleRate > 0) ((samples.size.toLong() * 1000L) / sampleRate) else 0L
            val item = SavedAudioItem(
                id = UUID.randomUUID().toString(),
                title = title,
                effectName = effectName,
                durationMs = durationMs,
                file = file,
                createdAt = timestamp,
                sizeBytes = file.length()
            )
            Result.success(item)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun getSavedAudios(context: Context): List<SavedAudioItem> = withContext(Dispatchers.IO) {
        val dir = getExportsDir(context)
        val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".wav", ignoreCase = true) } ?: emptyArray()

        files.sortedByDescending { it.lastModified() }.map { file ->
            val size = file.length()
            // In 44.1kHz 16-bit mono PCM: 2 bytes per sample = 88,200 bytes per second
            val durationMs = if (size > 44) ((size - 44) * 1000L / (44100 * 2)) else 0L

            // Parse effect name from filename if present
            val parts = file.nameWithoutExtension.split("_")
            val effectName = if (parts.size >= 2) parts[parts.size - 2].replaceFirstChar { it.uppercase() } else "Effect"

            SavedAudioItem(
                id = file.name,
                title = file.nameWithoutExtension,
                effectName = effectName,
                durationMs = durationMs,
                file = file,
                createdAt = file.lastModified(),
                sizeBytes = size
            )
        }
    }

    suspend fun deleteAudio(file: File): Boolean = withContext(Dispatchers.IO) {
        try {
            file.delete()
        } catch (_: Exception) {
            false
        }
    }

    fun shareAudio(context: Context, file: File, title: String) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/wav"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, "Listen to my modified voice clip ($title) created with AI Voice Changer!")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Audio Clip")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun writeWavFile(file: File, samples: ShortArray, sampleRate: Int) {
        val channels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val dataSize = samples.size * 2
        val totalDataLen = dataSize + 36

        FileOutputStream(file).use { out ->
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            // RIFF header
            header.put('R'.code.toByte())
            header.put('I'.code.toByte())
            header.put('F'.code.toByte())
            header.put('F'.code.toByte())
            header.putInt(totalDataLen)
            header.put('W'.code.toByte())
            header.put('A'.code.toByte())
            header.put('V'.code.toByte())
            header.put('E'.code.toByte())

            // Format chunk
            header.put('f'.code.toByte())
            header.put('m'.code.toByte())
            header.put('t'.code.toByte())
            header.put(' '.code.toByte())
            header.putInt(16) // Subchunk1Size for PCM
            header.putShort(1.toShort()) // AudioFormat 1 = PCM
            header.putShort(channels.toShort())
            header.putInt(sampleRate)
            header.putInt(byteRate)
            header.putShort(blockAlign.toShort())
            header.putShort(bitsPerSample.toShort())

            // Data chunk
            header.put('d'.code.toByte())
            header.put('a'.code.toByte())
            header.put('t'.code.toByte())
            header.put('a'.code.toByte())
            header.putInt(dataSize)

            out.write(header.array())

            // Write PCM samples
            val pcmBuffer = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            for (s in samples) {
                pcmBuffer.putShort(s)
            }
            out.write(pcmBuffer.array())
        }
    }
}
