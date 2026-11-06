package com.example.model

data class VoiceModifierParams(
    val effect: VoiceEffect = VoiceEffect.ORIGINAL,
    val pitchSemitones: Float = 0f, // -12.0 to +12.0
    val speedRate: Float = 1.0f, // 0.5 to 2.0
    val noiseReduction: Float = 0.15f, // 0.0 to 1.0
    val reverb: Float = 0.0f, // 0.0 to 1.0
    val echoDelayMs: Int = 0, // 0 to 600 ms
    val echoFeedback: Float = 0.0f, // 0.0 to 0.8
    val distortion: Float = 0.0f, // 0.0 to 1.0
    val modulationHz: Float = 0.0f, // 0 to 100 Hz
    val lowPassHz: Float = 0.0f, // 0 = disabled
    val highPassHz: Float = 0.0f // 0 = disabled
) {
    val isDefault: Boolean
        get() = pitchSemitones == 0f &&
                speedRate == 1.0f &&
                reverb == 0f &&
                echoDelayMs == 0 &&
                distortion == 0f &&
                modulationHz == 0f &&
                lowPassHz == 0f &&
                highPassHz == 0f
}
