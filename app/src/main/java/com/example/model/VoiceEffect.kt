package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Waves
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class VoiceCategory(val displayName: String) {
    ALL("All"),
    CREATIVE("Creative"),
    ENVIRONMENT("Environment"),
    CHARACTER("Characters"),
    DEVICE("Devices")
}

enum class VoiceEffect(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val category: VoiceCategory,
    val icon: ImageVector,
    val accentColor: Color,
    val defaultPitch: Float = 0f, // semitones (-12 to +12)
    val defaultSpeed: Float = 1.0f, // 0.5 to 2.0
    val defaultNoiseReduction: Float = 0.2f, // 0.0 to 1.0
    val defaultReverb: Float = 0f, // 0.0 to 1.0
    val defaultEchoDelayMs: Int = 0,
    val defaultEchoFeedback: Float = 0f,
    val defaultDistortion: Float = 0f,
    val defaultModulationHz: Float = 0f,
    val lowPassHz: Float = 0f, // 0 = bypass
    val highPassHz: Float = 0f // 0 = bypass
) {
    ORIGINAL(
        id = "original",
        displayName = "Original",
        subtitle = "Clean natural voice",
        category = VoiceCategory.CREATIVE,
        icon = Icons.Default.Mic,
        accentColor = Color(0xFF38BDF8),
        defaultPitch = 0f,
        defaultSpeed = 1.0f,
        defaultNoiseReduction = 0.15f
    ),
    CHIPMUNK(
        id = "chipmunk",
        displayName = "Chipmunk",
        subtitle = "High-pitch comedic helium",
        category = VoiceCategory.CHARACTER,
        icon = Icons.Default.Pets,
        accentColor = Color(0xFFFBBF24),
        defaultPitch = 8.5f,
        defaultSpeed = 1.12f,
        defaultNoiseReduction = 0.2f
    ),
    DARK_DEMON(
        id = "dark_demon",
        displayName = "Dark Demon",
        subtitle = "Deep sinister subterranean rumble",
        category = VoiceCategory.CHARACTER,
        icon = Icons.Default.Face,
        accentColor = Color(0xFFEF4444),
        defaultPitch = -8.0f,
        defaultSpeed = 0.95f,
        defaultDistortion = 0.35f,
        defaultReverb = 0.3f
    ),
    CYBER_ROBOT(
        id = "cyber_robot",
        displayName = "Cyber Robot",
        subtitle = "Metallic modulated android",
        category = VoiceCategory.CREATIVE,
        icon = Icons.Default.SmartToy,
        accentColor = Color(0xFFA855F7),
        defaultPitch = -1.5f,
        defaultModulationHz = 65f,
        defaultDistortion = 0.15f
    ),
    ALIEN(
        id = "alien",
        displayName = "Alien",
        subtitle = "Wobbly extraterrestrial frequency",
        category = VoiceCategory.CREATIVE,
        icon = Icons.Default.RocketLaunch,
        accentColor = Color(0xFF10B981),
        defaultPitch = 4.0f,
        defaultModulationHz = 24f,
        defaultEchoDelayMs = 180,
        defaultEchoFeedback = 0.35f
    ),
    CATHEDRAL(
        id = "cathedral",
        displayName = "Cathedral",
        subtitle = "Expansive sacred hall reverb",
        category = VoiceCategory.ENVIRONMENT,
        icon = Icons.Default.Waves,
        accentColor = Color(0xFF6366F1),
        defaultPitch = 0f,
        defaultReverb = 0.7f
    ),
    SPACE_ECHO(
        id = "space_echo",
        displayName = "Space Echo",
        subtitle = "Pulsing repeating cosmic delay",
        category = VoiceCategory.ENVIRONMENT,
        icon = Icons.Default.GraphicEq,
        accentColor = Color(0xFFEC4899),
        defaultEchoDelayMs = 260,
        defaultEchoFeedback = 0.55f,
        defaultReverb = 0.25f
    ),
    WALKIE_TALKIE(
        id = "walkie_talkie",
        displayName = "Walkie Talkie",
        subtitle = "Vintage CB radio transmission",
        category = VoiceCategory.DEVICE,
        icon = Icons.Default.Radio,
        accentColor = Color(0xFFF97316),
        highPassHz = 450f,
        lowPassHz = 3200f,
        defaultDistortion = 0.3f
    ),
    TELEPHONE(
        id = "telephone",
        displayName = "Telephone",
        subtitle = "Narrowband phone receiver",
        category = VoiceCategory.DEVICE,
        icon = Icons.Default.PhoneInTalk,
        accentColor = Color(0xFF14B8A6),
        highPassHz = 350f,
        lowPassHz = 2700f
    ),
    UNDERWATER(
        id = "underwater",
        displayName = "Underwater",
        subtitle = "Submerged acoustic bubble",
        category = VoiceCategory.ENVIRONMENT,
        icon = Icons.Default.WaterDrop,
        accentColor = Color(0xFF0284C7),
        lowPassHz = 600f,
        defaultPitch = -1.5f,
        defaultModulationHz = 8f
    ),
    DARK_MASTER(
        id = "dark_master",
        displayName = "Dark Master",
        subtitle = "Heavy ominous villain breathing",
        category = VoiceCategory.CHARACTER,
        icon = Icons.Default.Shield,
        accentColor = Color(0xFF991B1B),
        defaultPitch = -6.5f,
        defaultDistortion = 0.25f,
        defaultReverb = 0.4f
    ),
    MEGAPHONE(
        id = "megaphone",
        displayName = "Megaphone",
        subtitle = "Overdriven public loudspeaker",
        category = VoiceCategory.DEVICE,
        icon = Icons.Default.Campaign,
        accentColor = Color(0xFFEAB308),
        highPassHz = 600f,
        lowPassHz = 4000f,
        defaultDistortion = 0.45f
    ),
    MALE_TO_FEMALE(
        id = "male_to_female",
        displayName = "Woman Pitch",
        subtitle = "Smooth higher vocal shift",
        category = VoiceCategory.CHARACTER,
        icon = Icons.Default.Hearing,
        accentColor = Color(0xFFD946EF),
        defaultPitch = 5.0f,
        defaultSpeed = 1.0f
    ),
    FEMALE_TO_MALE(
        id = "female_to_male",
        displayName = "Man Pitch",
        subtitle = "Resonant deeper baritone shift",
        category = VoiceCategory.CHARACTER,
        icon = Icons.Default.Hearing,
        accentColor = Color(0xFF3B82F6),
        defaultPitch = -5.0f,
        defaultSpeed = 1.0f
    ),
    CUSTOM(
        id = "custom",
        displayName = "Custom Lab",
        subtitle = "Full manual DSP control",
        category = VoiceCategory.ALL,
        icon = Icons.Default.Tune,
        accentColor = Color(0xFFA855F7)
    );

    fun toParams(): VoiceModifierParams {
        return VoiceModifierParams(
            effect = this,
            pitchSemitones = defaultPitch,
            speedRate = defaultSpeed,
            noiseReduction = defaultNoiseReduction,
            reverb = defaultReverb,
            echoDelayMs = defaultEchoDelayMs,
            echoFeedback = defaultEchoFeedback,
            distortion = defaultDistortion,
            modulationHz = defaultModulationHz,
            lowPassHz = lowPassHz,
            highPassHz = highPassHz
        )
    }
}
