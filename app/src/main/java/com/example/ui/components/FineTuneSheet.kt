package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VoiceModifierParams
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceHighlight
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FineTuneSheet(
    params: VoiceModifierParams,
    onPitchChange: (Float) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onNoiseReductionChange: (Float) -> Unit,
    onReverbChange: (Float) -> Unit,
    onDistortionChange: (Float) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(DarkSurfaceHighlight, RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = NeonViolet,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Vocal Fine-Tuning Lab",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_fine_tune_sheet")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextMuted
                    )
                }
            }

            Text(
                text = "Adjust pitch, playback tempo, noise suppression, and acoustics in real-time.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 1. Pitch Shifter
            val pitchLabel = when {
                params.pitchSemitones > 0.5f -> "+${String.format(Locale.getDefault(), "%.1f", params.pitchSemitones)} st (Higher)"
                params.pitchSemitones < -0.5f -> "${String.format(Locale.getDefault(), "%.1f", params.pitchSemitones)} st (Deeper)"
                else -> "0.0 st (Natural Pitch)"
            }
            TuningSliderCard(
                title = "Pitch Shift",
                valueLabel = pitchLabel,
                icon = Icons.Default.Hearing,
                accentColor = NeonCyan,
                value = params.pitchSemitones,
                range = -12f..12f,
                testTag = "slider_pitch",
                onValueChange = onPitchChange
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Speed / Tempo
            val speedLabel = String.format(Locale.getDefault(), "%.2fx", params.speedRate)
            TuningSliderCard(
                title = "Playback Speed",
                valueLabel = speedLabel,
                icon = Icons.Default.Speed,
                accentColor = NeonViolet,
                value = params.speedRate,
                range = 0.5f..2.0f,
                testTag = "slider_speed",
                onValueChange = onSpeedChange
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Background Noise Removal
            val noisePct = (params.noiseReduction * 100).toInt()
            val noiseLabel = when {
                noisePct == 0 -> "Off (Bypass)"
                noisePct < 30 -> "$noisePct% (Subtle Gate)"
                noisePct < 70 -> "$noisePct% (Optimal Denoise)"
                else -> "$noisePct% (Max Silence)"
            }
            TuningSliderCard(
                title = "Background Noise Removal",
                valueLabel = noiseLabel,
                icon = Icons.Default.VolumeDown,
                accentColor = Color(0xFF10B981),
                value = params.noiseReduction,
                range = 0.0f..1.0f,
                testTag = "slider_noise_reduction",
                onValueChange = onNoiseReductionChange
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Reverb Space
            val reverbPct = (params.reverb * 100).toInt()
            val reverbLabel = if (reverbPct == 0) "Dry (None)" else "$reverbPct% Wet"
            TuningSliderCard(
                title = "Reverb & Atmosphere",
                valueLabel = reverbLabel,
                icon = Icons.Default.Waves,
                accentColor = Color(0xFF6366F1),
                value = params.reverb,
                range = 0.0f..1.0f,
                testTag = "slider_reverb",
                onValueChange = onReverbChange
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Distortion / Drive
            val distPct = (params.distortion * 100).toInt()
            val distLabel = if (distPct == 0) "Clean" else "$distPct% Saturation"
            TuningSliderCard(
                title = "Distortion & Overdrive",
                valueLabel = distLabel,
                icon = Icons.Default.GraphicEq,
                accentColor = NeonPink,
                value = params.distortion,
                range = 0.0f..1.0f,
                testTag = "slider_distortion",
                onValueChange = onDistortionChange
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Reset Button
            OutlinedButton(
                onClick = onReset,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reset_params_button"),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextPrimary
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceHighlight),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset All to Default")
            }
        }
    }
}

@Composable
fun TuningSliderCard(
    title: String,
    valueLabel: String,
    icon: ImageVector,
    accentColor: Color,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    testTag: String,
    onValueChange: (Float) -> Unit
) {
    Surface(
        color = DarkSurfaceVariant,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceHighlight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )
                }

                Surface(
                    color = DarkBackground,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = valueLabel,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = accentColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = range,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(testTag),
                colors = SliderDefaults.colors(
                    thumbColor = accentColor,
                    activeTrackColor = accentColor,
                    inactiveTrackColor = DarkSurfaceHighlight
                )
            )
        }
    }
}
