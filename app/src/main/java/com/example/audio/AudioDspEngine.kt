package com.example.audio

import com.example.model.VoiceModifierParams
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

object AudioDspEngine {

    /**
     * Applies the complete DSP chain to 16-bit PCM samples according to parameters.
     */
    fun process(
        input: ShortArray,
        params: VoiceModifierParams,
        sampleRate: Int = 44100
    ): ShortArray {
        if (input.isEmpty()) return ShortArray(0)

        // Convert 16-bit PCM to normalized float buffer [-1.0, 1.0]
        var buffer = FloatArray(input.size) { input[it] / 32768.0f }

        // 1. Noise Reduction / Gate
        if (params.noiseReduction > 0.05f) {
            buffer = applyNoiseReduction(buffer, params.noiseReduction, sampleRate)
        }

        // 2. High-pass filter (cut rumble or device effect)
        val hpCutoff = if (params.highPassHz > 20f) params.highPassHz else 80f
        buffer = applyHighPassFilter(buffer, hpCutoff, sampleRate)

        // 3. Low-pass filter (for telephone, underwater, walkie-talkie)
        if (params.lowPassHz in 200f..18000f) {
            buffer = applyLowPassFilter(buffer, params.lowPassHz, sampleRate)
        }

        // 4. Pitch & Speed stretching (SOLA / Granular Synthesizer)
        val pitchFactor = Math.pow(2.0, params.pitchSemitones.toDouble() / 12.0).toFloat()
        val speedRate = params.speedRate.coerceIn(0.5f, 2.0f)
        if (abs(pitchFactor - 1.0f) > 0.03f || abs(speedRate - 1.0f) > 0.03f) {
            buffer = applyPitchAndSpeed(buffer, pitchFactor, speedRate, sampleRate)
        }

        // 5. Modulation (Robot / Alien / Underwater LFO)
        if (params.modulationHz > 0.5f) {
            buffer = applyRingModulation(buffer, params.modulationHz, sampleRate, depth = 0.85f)
        }

        // 6. Distortion / Overdrive (Megaphone / Demon / Walkie-talkie)
        if (params.distortion > 0.05f) {
            buffer = applyDistortion(buffer, params.distortion)
        }

        // 7. Echo / Delay line
        if (params.echoDelayMs > 20 && params.echoFeedback > 0.05f) {
            buffer = applyEcho(buffer, params.echoDelayMs, params.echoFeedback, sampleRate)
        }

        // 8. Reverb (Schroeder Reverberator)
        if (params.reverb > 0.05f) {
            buffer = applyReverb(buffer, params.reverb, sampleRate)
        }

        // 9. Normalize & convert back to 16-bit PCM
        return floatToPcm16(buffer)
    }

    /**
     * Noise gate and spectral floor attenuation to suppress ambient background noise.
     */
    private fun applyNoiseReduction(input: FloatArray, level: Float, sampleRate: Int): FloatArray {
        val output = FloatArray(input.size)
        // Adaptive threshold based on level
        val threshold = 0.015f * level
        val attack = 0.05f
        val release = 0.002f
        var envelope = 0.0f

        for (i in input.indices) {
            val sample = input[i]
            val absVal = abs(sample)
            if (absVal > envelope) {
                envelope += attack * (absVal - envelope)
            } else {
                envelope -= release * envelope
            }

            // Gain multiplier based on envelope vs threshold
            val gain = if (envelope > threshold) {
                1.0f
            } else {
                val ratio = (envelope / threshold).coerceIn(0.0f, 1.0f)
                // Smooth cubic taper into floor
                val softFloor = (1.0f - level).coerceAtLeast(0.05f)
                softFloor + (1.0f - softFloor) * (ratio * ratio * (3f - 2f * ratio))
            }
            output[i] = sample * gain
        }
        return output
    }

    /**
     * High-pass IIR Butterworth filter to cut frequencies below cutoff.
     */
    private fun applyHighPassFilter(input: FloatArray, cutoff: Float, sampleRate: Int): FloatArray {
        val output = FloatArray(input.size)
        val dt = 1.0f / sampleRate
        val rc = 1.0f / (2.0f * PI.toFloat() * cutoff)
        val alpha = rc / (rc + dt)

        var prevIn = input[0]
        var prevOut = input[0]

        for (i in input.indices) {
            val currIn = input[i]
            val currOut = alpha * (prevOut + currIn - prevIn)
            output[i] = currOut
            prevIn = currIn
            prevOut = currOut
        }
        return output
    }

    /**
     * Low-pass IIR single-pole filter.
     */
    private fun applyLowPassFilter(input: FloatArray, cutoff: Float, sampleRate: Int): FloatArray {
        val output = FloatArray(input.size)
        val dt = 1.0f / sampleRate
        val rc = 1.0f / (2.0f * PI.toFloat() * cutoff)
        val alpha = dt / (rc + dt)

        var prevOut = 0.0f
        for (i in input.indices) {
            prevOut += alpha * (input[i] - prevOut)
            output[i] = prevOut
        }
        return output
    }

    /**
     * Time-domain SOLA / Granular pitch and speed transformation.
     * Shifts pitch by pitchFactor without changing duration, and stretches duration by 1 / speedRate.
     */
    private fun applyPitchAndSpeed(
        input: FloatArray,
        pitchFactor: Float,
        speedRate: Float,
        sampleRate: Int
    ): FloatArray {
        if (input.size < 512) return input

        // Window size ~45ms
        val grainSize = (sampleRate * 0.045f).toInt()
        val halfGrain = grainSize / 2

        // Precompute Hanning window
        val window = FloatArray(grainSize) { i ->
            (0.5f * (1.0f - cos(2.0f * PI.toFloat() * i / (grainSize - 1)))).toFloat()
        }

        // Resample step for pitch change
        // pitchFactor > 1: pitch higher (resample step > 1)
        // pitchFactor < 1: pitch lower (resample step < 1)
        val effectiveResampleStep = pitchFactor

        // Hop sizes:
        val synthHop = halfGrain
        // Analysis hop determines output playback rate vs speed
        val analysisHop = (synthHop * effectiveResampleStep / speedRate).toInt().coerceAtLeast(1)

        val estimatedOutputSize = ((input.size.toDouble() / speedRate) * 1.1).toInt() + grainSize
        val output = FloatArray(estimatedOutputSize)
        val normWeight = FloatArray(estimatedOutputSize)

        var inPos = 0.0
        var outPos = 0

        while (inPos + grainSize * effectiveResampleStep < input.size && outPos + grainSize < estimatedOutputSize) {
            for (i in 0 until grainSize) {
                val readIndex = inPos + i * effectiveResampleStep
                val i0 = readIndex.toInt()
                val frac = (readIndex - i0).toFloat()
                val s0 = if (i0 < input.size) input[i0] else 0f
                val s1 = if (i0 + 1 < input.size) input[i0 + 1] else s0
                val interp = s0 + frac * (s1 - s0)

                val w = window[i]
                output[outPos + i] += interp * w
                normWeight[outPos + i] += w
            }

            inPos += analysisHop
            outPos += synthHop
        }

        // Normalize overlapping window gains
        val finalLength = outPos.coerceAtLeast(1)
        val finalOutput = FloatArray(finalLength)
        for (i in 0 until finalLength) {
            val weight = normWeight[i]
            finalOutput[i] = if (weight > 0.01f) (output[i] / weight) else output[i]
        }

        return finalOutput
    }

    /**
     * Ring modulation with sine carrier for robotic / alien tones.
     */
    private fun applyRingModulation(
        input: FloatArray,
        freqHz: Float,
        sampleRate: Int,
        depth: Float
    ): FloatArray {
        val output = FloatArray(input.size)
        val twoPiF = 2.0 * PI * freqHz / sampleRate

        // Also add metallic comb feedback
        val combDelaySamples = (sampleRate * 0.007).toInt() // 7ms delay
        val combFeedback = 0.45f
        val delayBuf = FloatArray(combDelaySamples)
        var delayIdx = 0

        for (i in input.indices) {
            val carrier = sin(twoPiF * i).toFloat()
            val modulated = input[i] * carrier

            val delayed = delayBuf[delayIdx]
            val combSample = modulated + delayed * combFeedback
            delayBuf[delayIdx] = combSample
            delayIdx = (delayIdx + 1) % combDelaySamples

            output[i] = input[i] * (1.0f - depth) + combSample * depth
        }
        return output
    }

    /**
     * Soft saturation distortion with tanh curve.
     */
    private fun applyDistortion(input: FloatArray, amount: Float): FloatArray {
        val output = FloatArray(input.size)
        val gain = 1.0f + amount * 6.0f
        val comp = 1.0f / tanh(gain.toDouble()).toFloat()

        for (i in input.indices) {
            val boosted = input[i] * gain
            output[i] = tanh(boosted.toDouble()).toFloat() * comp
        }
        return output
    }

    /**
     * Stereo/mono feedback delay echo.
     */
    private fun applyEcho(
        input: FloatArray,
        delayMs: Int,
        feedback: Float,
        sampleRate: Int
    ): FloatArray {
        val delaySamples = (sampleRate * (delayMs / 1000.0f)).toInt().coerceAtLeast(1)
        val extraLength = (sampleRate * 0.8).toInt()
        val totalLength = input.size + extraLength
        val output = FloatArray(totalLength)

        val delayBuffer = FloatArray(delaySamples)
        var bufIndex = 0

        for (i in 0 until totalLength) {
            val inSample = if (i < input.size) input[i] else 0f
            val delayedSample = delayBuffer[bufIndex]
            val outSample = inSample + delayedSample * feedback

            delayBuffer[bufIndex] = inSample + delayedSample * (feedback * 0.75f)
            bufIndex = (bufIndex + 1) % delaySamples

            output[i] = outSample
        }
        return output
    }

    /**
     * 4-comb, 2-allpass Schroeder Reverberator for lush acoustic space simulation.
     */
    private fun applyReverb(input: FloatArray, mix: Float, sampleRate: Int): FloatArray {
        val wetMix = (mix * 0.85f).coerceIn(0f, 0.9f)
        val dryMix = 1.0f - wetMix * 0.5f

        // Tuned comb filter delays (primes around 30-45ms)
        val combDelays = intArrayOf(
            (sampleRate * 0.0297).toInt(),
            (sampleRate * 0.0371).toInt(),
            (sampleRate * 0.0411).toInt(),
            (sampleRate * 0.0437).toInt()
        )
        val combFeedback = 0.78f

        val combs = Array(4) { FloatArray(combDelays[it]) }
        val combIndices = IntArray(4)

        // Allpass diffuser delays
        val apDelay1 = (sampleRate * 0.005).toInt()
        val apDelay2 = (sampleRate * 0.0017).toInt()
        val apBuf1 = FloatArray(apDelay1)
        val apBuf2 = FloatArray(apDelay2)
        var apIdx1 = 0
        var apIdx2 = 0
        val apGain = 0.5f

        val extraLength = (sampleRate * 0.5).toInt()
        val totalLength = input.size + extraLength
        val output = FloatArray(totalLength)

        for (i in 0 until totalLength) {
            val inSample = if (i < input.size) input[i] else 0f

            // Comb sum
            var combSum = 0f
            for (c in 0 until 4) {
                val idx = combIndices[c]
                val dVal = combs[c][idx]
                combSum += dVal
                combs[c][idx] = inSample + dVal * combFeedback
                combIndices[c] = (idx + 1) % combDelays[c]
            }
            combSum *= 0.25f

            // Allpass 1
            val ap1Out = -apGain * combSum + apBuf1[apIdx1]
            apBuf1[apIdx1] = combSum + apGain * ap1Out
            apIdx1 = (apIdx1 + 1) % apDelay1

            // Allpass 2
            val ap2Out = -apGain * ap1Out + apBuf2[apIdx2]
            apBuf2[apIdx2] = ap1Out + apGain * ap2Out
            apIdx2 = (apIdx2 + 1) % apDelay2

            output[i] = inSample * dryMix + ap2Out * wetMix
        }

        return output
    }

    /**
     * Converts normalized float buffer back to 16-bit PCM with soft peak limiting.
     */
    private fun floatToPcm16(buffer: FloatArray): ShortArray {
        val result = ShortArray(buffer.size)
        // Find peak for soft AGC limiter
        var maxPeak = 0.0f
        for (f in buffer) {
            val absVal = abs(f)
            if (absVal > maxPeak) maxPeak = absVal
        }

        val scale = if (maxPeak > 0.95f) (0.95f / maxPeak) else 1.0f

        for (i in buffer.indices) {
            val sample = (buffer[i] * scale).coerceIn(-1.0f, 1.0f)
            result[i] = (sample * 32767.0f).toInt().toShort()
        }
        return result
    }
}
