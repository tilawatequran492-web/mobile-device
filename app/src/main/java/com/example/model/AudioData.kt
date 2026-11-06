package com.example.model

import java.io.File

data class AudioData(
    val samples: ShortArray,
    val sampleRate: Int = 44100,
    val sourceTitle: String = "Recording",
    val sourceFile: File? = null
) {
    val durationMs: Long = if (sampleRate > 0) ((samples.size.toLong() * 1000L) / sampleRate) else 0L

    // Normalized amplitude peaks (0.0 to 1.0) sampled for waveform rendering (e.g. 60 bars)
    fun extractWaveformPeaks(barCount: Int = 64): FloatArray {
        if (samples.isEmpty()) return FloatArray(barCount) { 0.1f }
        val result = FloatArray(barCount)
        val chunkSize = (samples.size / barCount).coerceAtLeast(1)

        for (i in 0 until barCount) {
            val start = i * chunkSize
            val end = (start + chunkSize).coerceAtMost(samples.size)
            var maxVal = 0
            for (j in start until end) {
                val abs = Math.abs(samples[j].toInt())
                if (abs > maxVal) maxVal = abs
            }
            // Normalize with logarithmic perception curve
            val norm = (maxVal / 32768f).coerceIn(0f, 1f)
            result[i] = (Math.sqrt(norm.toDouble()).toFloat()).coerceIn(0.08f, 1.0f)
        }
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AudioData
        return samples.contentEquals(other.samples) && sampleRate == other.sampleRate && sourceTitle == other.sourceTitle
    }

    override fun hashCode(): Int {
        var result = samples.contentHashCode()
        result = 31 * result + sampleRate
        result = 31 * result + sourceTitle.hashCode()
        return result
    }
}
