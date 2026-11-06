package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

class RealtimeAudioPlayer(
    private val scope: CoroutineScope
) {
    private var audioTrack: AudioTrack? = null
    private var currentSamples: ShortArray = ShortArray(0)
    private var sampleRate: Int = 44100
    private var playbackIndex = 0

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _playbackPositionMs = MutableStateFlow(0L)
    val playbackPositionMs: StateFlow<Long> = _playbackPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _currentRms = MutableStateFlow(0f)
    val currentRms: StateFlow<Float> = _currentRms.asStateFlow()

    var isLooping: Boolean = false

    private var playbackJob: Job? = null
    private val lock = Any()

    fun loadAudio(samples: ShortArray, rate: Int = 44100, resetPosition: Boolean = true) {
        synchronized(lock) {
            currentSamples = samples
            sampleRate = rate
            val dur = if (rate > 0) ((samples.size.toLong() * 1000L) / rate) else 0L
            _durationMs.value = dur

            if (resetPosition || currentSamples.isEmpty()) {
                playbackIndex = 0
                _progress.value = 0f
                _playbackPositionMs.value = 0L
            } else {
                if (playbackIndex >= currentSamples.size) {
                    playbackIndex = 0
                }
                updateProgressValues()
            }
        }
        recreateAudioTrack()
    }

    fun updateSamplesPreservingPosition(newSamples: ShortArray) {
        synchronized(lock) {
            val oldProgress = _progress.value
            currentSamples = newSamples
            val dur = if (sampleRate > 0) ((newSamples.size.toLong() * 1000L) / sampleRate) else 0L
            _durationMs.value = dur
            playbackIndex = (oldProgress * newSamples.size).toInt().coerceIn(0, (newSamples.size - 1).coerceAtLeast(0))
            updateProgressValues()
        }
    }

    private fun recreateAudioTrack() {
        try {
            audioTrack?.apply {
                if (playState == AudioTrack.PLAYSTATE_PLAYING) {
                    stop()
                }
                release()
            }
        } catch (_: Exception) {}

        if (sampleRate <= 0) return

        val minBufSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = (minBufSize * 2).coerceAtLeast(4096)

        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun play() {
        if (currentSamples.isEmpty()) return
        if (_isPlaying.value) return

        if (audioTrack == null) {
            recreateAudioTrack()
        }

        try {
            audioTrack?.play()
        } catch (e: Exception) {
            recreateAudioTrack()
            audioTrack?.play()
        }

        _isPlaying.value = true
        startPlaybackLoop()
    }

    fun pause() {
        _isPlaying.value = false
        playbackJob?.cancel()
        try {
            audioTrack?.pause()
            audioTrack?.flush()
        } catch (_: Exception) {}
        _currentRms.value = 0f
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            play()
        }
    }

    fun seekTo(newProgress: Float) {
        synchronized(lock) {
            val clamped = newProgress.coerceIn(0f, 1f)
            playbackIndex = (clamped * currentSamples.size).toInt().coerceIn(0, currentSamples.size)
            updateProgressValues()
        }
        try {
            audioTrack?.flush()
        } catch (_: Exception) {}
    }

    private fun updateProgressValues() {
        val total = currentSamples.size
        if (total > 0) {
            val prog = (playbackIndex.toFloat() / total).coerceIn(0f, 1f)
            _progress.value = prog
            _playbackPositionMs.value = if (sampleRate > 0) ((playbackIndex.toLong() * 1000L) / sampleRate) else 0L
        } else {
            _progress.value = 0f
            _playbackPositionMs.value = 0L
        }
    }

    private fun startPlaybackLoop() {
        playbackJob?.cancel()
        playbackJob = scope.launch(Dispatchers.IO) {
            val chunkSize = 2048
            val chunk = ShortArray(chunkSize)

            while (isActive && _isPlaying.value) {
                var samplesWritten = 0
                var chunkRmsSum = 0.0

                synchronized(lock) {
                    if (currentSamples.isEmpty()) return@synchronized

                    if (playbackIndex >= currentSamples.size) {
                        if (isLooping) {
                            playbackIndex = 0
                        } else {
                            // Completed playback
                            launch(Dispatchers.Main) {
                                pause()
                                seekTo(0f)
                            }
                            return@synchronized
                        }
                    }

                    val remaining = currentSamples.size - playbackIndex
                    val toCopy = minOf(chunkSize, remaining)
                    System.arraycopy(currentSamples, playbackIndex, chunk, 0, toCopy)
                    playbackIndex += toCopy
                    samplesWritten = toCopy

                    for (i in 0 until toCopy) {
                        val v = chunk[i]
                        chunkRmsSum += (v * v).toDouble()
                    }

                    val curProg = (playbackIndex.toFloat() / currentSamples.size).coerceIn(0f, 1f)
                    val curPos = if (sampleRate > 0) ((playbackIndex.toLong() * 1000L) / sampleRate) else 0L

                    _progress.value = curProg
                    _playbackPositionMs.value = curPos
                }

                if (samplesWritten > 0) {
                    val rms = Math.sqrt(chunkRmsSum / samplesWritten) / 32768.0
                    _currentRms.value = rms.toFloat().coerceIn(0f, 1f)

                    audioTrack?.write(chunk, 0, samplesWritten)
                }
            }
        }
    }

    fun release() {
        pause()
        try {
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }
}
