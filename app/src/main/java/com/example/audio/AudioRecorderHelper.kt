package com.example.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.example.model.AudioData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioRecorderHelper(
    private val scope: CoroutineScope
) {
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDurationMs = MutableStateFlow(0L)
    val recordingDurationMs: StateFlow<Long> = _recordingDurationMs.asStateFlow()

    private val _liveAmplitude = MutableStateFlow(0f)
    val liveAmplitude: StateFlow<Float> = _liveAmplitude.asStateFlow()

    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    @SuppressLint("MissingPermission")
    fun startRecording(onFailed: (String) -> Unit): Boolean {
        if (_isRecording.value) return true

        val minBuf = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (minBuf == AudioRecord.ERROR || minBuf == AudioRecord.ERROR_BAD_VALUE) {
            onFailed("Audio recording hardware not supported.")
            return false
        }
        val bufferSize = (minBuf * 2).coerceAtLeast(4096)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                onFailed("Could not initialize microphone.")
                audioRecord?.release()
                audioRecord = null
                return false
            }

            audioRecord?.startRecording()
            _isRecording.value = true
            _recordingDurationMs.value = 0L

            return true
        } catch (e: Exception) {
            onFailed("Failed to access microphone: ${e.message}")
            return false
        }
    }

    fun recordLoop(onFinished: (AudioData?) -> Unit) {
        recordingJob?.cancel()
        recordingJob = scope.launch(Dispatchers.IO) {
            val record = audioRecord ?: return@launch
            val buffer = ShortArray(1024)
            val byteOut = ByteArrayOutputStream()
            val startTime = System.currentTimeMillis()

            try {
                while (isActive && _isRecording.value) {
                    val read = record.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        // Calculate RMS for live visualizer
                        var sumSq = 0.0
                        val byteBuffer = ByteBuffer.allocate(read * 2).order(ByteOrder.LITTLE_ENDIAN)
                        for (i in 0 until read) {
                            val sample = buffer[i]
                            byteBuffer.putShort(sample)
                            sumSq += (sample * sample).toDouble()
                        }
                        byteOut.write(byteBuffer.array())

                        val rms = Math.sqrt(sumSq / read) / 32768.0
                        _liveAmplitude.value = rms.toFloat().coerceIn(0f, 1f)
                        _recordingDurationMs.value = System.currentTimeMillis() - startTime
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    record.stop()
                    record.release()
                } catch (_: Exception) {}
                audioRecord = null
                _isRecording.value = false
                _liveAmplitude.value = 0f

                val recordedBytes = byteOut.toByteArray()
                if (recordedBytes.isNotEmpty()) {
                    val shorts = ShortArray(recordedBytes.size / 2)
                    ByteBuffer.wrap(recordedBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)
                    val audioData = AudioData(
                        samples = shorts,
                        sampleRate = sampleRate,
                        sourceTitle = "Voice Recording"
                    )
                    launch(Dispatchers.Main) {
                        onFinished(audioData)
                    }
                } else {
                    launch(Dispatchers.Main) {
                        onFinished(null)
                    }
                }
            }
        }
    }

    fun stopRecording() {
        _isRecording.value = false
    }

    fun cancelRecording() {
        _isRecording.value = false
        recordingJob?.cancel()
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
        _liveAmplitude.value = 0f
        _recordingDurationMs.value = 0L
    }
}
