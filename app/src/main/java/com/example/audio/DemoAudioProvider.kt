package com.example.audio

import com.example.model.AudioData
import kotlin.math.PI
import kotlin.math.sin

object DemoAudioProvider {

    /**
     * Synthesizes a vocalized demo track imitating human speech phonemes and cadences.
     */
    fun getSpeechDemo(): AudioData {
        val sampleRate = 44100
        val durationSecs = 5.5
        val totalSamples = (sampleRate * durationSecs).toInt()
        val samples = ShortArray(totalSamples)

        // Syllable rhythm timings (vocal bursts imitating speech words)
        val words = listOf(
            Triple(0.2, 0.6, 210.0), // "Wel-"
            Triple(0.65, 0.95, 195.0), // "come"
            Triple(1.1, 1.35, 230.0), // "to"
            Triple(1.4, 1.7, 240.0), // "A"
            Triple(1.75, 2.1, 260.0), // "I"
            Triple(2.3, 2.7, 220.0), // "Voice"
            Triple(2.8, 3.2, 200.0), // "Changer"
            Triple(3.4, 3.75, 240.0), // "Magic"
            Triple(3.85, 4.3, 220.0), // "Studio"
            Triple(4.5, 5.0, 190.0)  // "Enjoy"
        )

        for (w in words) {
            val startSample = (w.first * sampleRate).toInt()
            val endSample = (w.second * sampleRate).toInt().coerceAtMost(totalSamples)
            val baseFreq = w.third
            val len = (endSample - startSample).coerceAtLeast(1)

            for (i in startSample until endSample) {
                val rel = (i - startSample).toDouble() / len
                // Hanning envelope for syllable
                val env = 0.5 * (1.0 - Math.cos(2.0 * PI * rel))
                val t = (i - startSample).toDouble() / sampleRate

                // Vocal harmonics (formant simulation: F0, F1 ~ 700Hz, F2 ~ 1400Hz, F3 ~ 2400Hz)
                val fundamental = sin(2.0 * PI * baseFreq * t)
                val h2 = 0.5 * sin(2.0 * PI * (baseFreq * 2.0) * t)
                val h3 = 0.3 * sin(2.0 * PI * (baseFreq * 3.0) * t)
                val formant1 = 0.4 * sin(2.0 * PI * 750.0 * t)
                val formant2 = 0.25 * sin(2.0 * PI * 1350.0 * t)

                val vocalSample = (fundamental + h2 + h3 + formant1 + formant2) * env * 0.7
                samples[i] = (vocalSample * 28000.0).toInt().coerceIn(-32768, 32767).toShort()
            }
        }

        return AudioData(
            samples = samples,
            sampleRate = sampleRate,
            sourceTitle = "Studio Voice Demo"
        )
    }

    /**
     * Synthesizes a demo with clear vocal speech plus background air conditioning / hum noise,
     * perfect for testing the background noise reduction slider!
     */
    fun getSpeechWithNoiseDemo(): AudioData {
        val clean = getSpeechDemo()
        val noisySamples = ShortArray(clean.samples.size)
        val rand = java.util.Random(42)

        for (i in clean.samples.indices) {
            val cleanVal = clean.samples[i].toInt()
            // Low-frequency room hum (60Hz + 120Hz) + white noise hiss
            val hum = (sin(2.0 * PI * 60.0 * i / 44100.0) * 1200.0).toInt()
            val hum2 = (sin(2.0 * PI * 120.0 * i / 44100.0) * 800.0).toInt()
            val hiss = (rand.nextGaussian() * 600.0).toInt()
            val total = cleanVal + hum + hum2 + hiss
            noisySamples[i] = total.coerceIn(-32768, 32767).toShort()
        }

        return AudioData(
            samples = noisySamples,
            sampleRate = 44100,
            sourceTitle = "Voice with Background Noise"
        )
    }
}
