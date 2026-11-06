package com.example.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import com.example.model.AudioData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object AudioFileImporter {

    suspend fun decodeAudioUri(
        context: Context,
        uri: Uri,
        displayName: String = "Imported Audio"
    ): Result<AudioData> = withContext(Dispatchers.IO) {
        try {
            // Check if it's a standard PCM WAV first
            val wavResult = tryParsePcmWav(context, uri, displayName)
            if (wavResult != null) {
                return@withContext Result.success(wavResult)
            }

            // Otherwise use MediaExtractor + MediaCodec to decode
            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(context, uri, null)
            } catch (e: Exception) {
                // Try copying to temporary file if direct URI content fails
                val tempFile = java.io.File.createTempFile("imported_audio", ".tmp", context.cacheDir)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                extractor.setDataSource(tempFile.absolutePath)
            }

            var audioTrackIndex = -1
            var format: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                val mime = f.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    format = f
                    break
                }
            }

            if (audioTrackIndex < 0 || format == null) {
                extractor.release()
                return@withContext Result.failure(Exception("No audio track found in file."))
            }

            extractor.selectTrack(audioTrackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            val sampleRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } else 44100
            val channelCount = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } else 1

            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val pcmOut = ByteArrayOutputStream()
            val bufferInfo = MediaCodec.BufferInfo()
            var isEos = false
            val timeoutUs = 5000L

            while (!isEos) {
                val inIndex = codec.dequeueInputBuffer(timeoutUs)
                if (inIndex >= 0) {
                    val inBuffer = codec.getInputBuffer(inIndex)
                    if (inBuffer != null) {
                        val sampleSize = extractor.readSampleData(inBuffer, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            isEos = true
                        } else {
                            codec.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                var outIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)
                while (outIndex >= 0) {
                    val outBuffer = codec.getOutputBuffer(outIndex)
                    if (outBuffer != null && bufferInfo.size > 0) {
                        val chunk = ByteArray(bufferInfo.size)
                        outBuffer.position(bufferInfo.offset)
                        outBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        outBuffer.get(chunk)
                        pcmOut.write(chunk)
                    }
                    codec.releaseOutputBuffer(outIndex, false)
                    outIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)
                }
            }

            codec.stop()
            codec.release()
            extractor.release()

            val rawBytes = pcmOut.toByteArray()
            if (rawBytes.isEmpty()) {
                return@withContext Result.failure(Exception("Decoded audio is empty."))
            }

            val shortCount = rawBytes.size / 2
            val rawShorts = ShortArray(shortCount)
            ByteBuffer.wrap(rawBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(rawShorts)

            // Convert to Mono if multi-channel
            val monoShorts = if (channelCount > 1) {
                val monoSize = shortCount / channelCount
                val mono = ShortArray(monoSize)
                for (i in 0 until monoSize) {
                    var sum = 0
                    for (c in 0 until channelCount) {
                        sum += rawShorts[i * channelCount + c]
                    }
                    mono[i] = (sum / channelCount).toShort()
                }
                mono
            } else {
                rawShorts
            }

            // Resample to 44100 if necessary
            val finalShorts = if (sampleRate != 44100 && sampleRate > 8000) {
                resampleLinear(monoShorts, sampleRate, 44100)
            } else {
                monoShorts
            }

            Result.success(
                AudioData(
                    samples = finalShorts,
                    sampleRate = 44100,
                    sourceTitle = displayName
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun tryParsePcmWav(context: Context, uri: Uri, title: String): AudioData? {
        return try {
            val input: InputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bytes = input.readBytes()
            input.close()

            if (bytes.size < 44) return null
            // Check "RIFF" and "WAVE"
            if (bytes[0] != 'R'.code.toByte() || bytes[1] != 'I'.code.toByte() ||
                bytes[2] != 'F'.code.toByte() || bytes[3] != 'F'.code.toByte() ||
                bytes[8] != 'W'.code.toByte() || bytes[9] != 'A'.code.toByte() ||
                bytes[10] != 'V'.code.toByte() || bytes[11] != 'E'.code.toByte()
            ) {
                return null
            }

            // Find "data" chunk
            var offset = 12
            var dataOffset = -1
            var dataSize = -1

            while (offset + 8 <= bytes.size) {
                val chunkId = String(bytes, offset, 4)
                val chunkSize = ByteBuffer.wrap(bytes, offset + 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
                if (chunkId == "data") {
                    dataOffset = offset + 8
                    dataSize = chunkSize
                    break
                }
                offset += 8 + chunkSize
            }

            if (dataOffset < 0 || dataOffset + dataSize > bytes.size) {
                dataOffset = 44
                dataSize = bytes.size - 44
            }

            val pcmLength = dataSize / 2
            val shorts = ShortArray(pcmLength)
            ByteBuffer.wrap(bytes, dataOffset, pcmLength * 2).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)

            AudioData(
                samples = shorts,
                sampleRate = 44100,
                sourceTitle = title
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun resampleLinear(input: ShortArray, fromRate: Int, toRate: Int): ShortArray {
        val ratio = toRate.toDouble() / fromRate
        val newLength = (input.size * ratio).toInt()
        val output = ShortArray(newLength)

        for (i in 0 until newLength) {
            val srcPos = i / ratio
            val srcIdx = srcPos.toInt()
            val frac = (srcPos - srcIdx).toFloat()

            val s0 = if (srcIdx < input.size) input[srcIdx].toFloat() else 0f
            val s1 = if (srcIdx + 1 < input.size) input[srcIdx + 1].toFloat() else s0
            val interpolated = s0 + frac * (s1 - s0)
            output[i] = interpolated.toInt().coerceIn(-32768, 32767).toShort()
        }
        return output
    }
}
