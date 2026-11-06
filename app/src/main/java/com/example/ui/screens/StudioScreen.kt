package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioData
import com.example.model.VoiceEffect
import com.example.model.VoiceModifierParams
import com.example.ui.components.AudioEffectCard
import com.example.ui.components.WaveformVisualizer
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceHighlight
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import java.util.Locale

@Composable
fun StudioScreen(
    rawAudio: AudioData?,
    processedAudio: AudioData?,
    waveformPeaks: FloatArray,
    params: VoiceModifierParams,
    selectedEffect: VoiceEffect,
    isPlaying: Boolean,
    progress: Float,
    playbackPositionMs: Long,
    durationMs: Long,
    currentRms: Float,
    isProcessing: Boolean,
    isLooping: Boolean,
    onTogglePlay: () -> Unit,
    onSeek: (Float) -> Unit,
    onToggleLoop: () -> Unit,
    onSelectEffect: (VoiceEffect) -> Unit,
    onPitchChange: (Float) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onNoiseReductionChange: (Float) -> Unit,
    onOpenFineTune: () -> Unit,
    onOpenRecord: () -> Unit,
    onOpenExport: () -> Unit,
    onLoadDefaultDemo: () -> Unit,
    onLoadNoisyDemo: () -> Unit,
    onImportAudio: (Uri, String) -> Unit,
    onNavigateToEffects: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Audio file picker
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val name = it.lastPathSegment?.substringAfterLast('/') ?: "Uploaded Audio"
            onImportAudio(it, name)
        }
    }

    val formattedCurrentTime = remember(playbackPositionMs) {
        val sec = (playbackPositionMs / 1000).toInt()
        String.format(Locale.getDefault(), "%02d:%02d", sec / 60, sec % 60)
    }

    val formattedTotalTime = remember(durationMs) {
        val sec = (durationMs / 1000).toInt()
        String.format(Locale.getDefault(), "%02d:%02d", sec / 60, sec % 60)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Quick Action Bar: Record & Upload buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Record Audio Button
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onOpenRecord)
                    .testTag("action_record_audio"),
                shape = RoundedCornerShape(16.dp),
                color = DarkSurface,
                border = BorderStroke(1.dp, NeonViolet.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(NeonViolet.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Record",
                            tint = NeonViolet,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Record Voice",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "Tap to speak",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
            }

            // Upload Audio Button
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { audioPickerLauncher.launch("audio/*") }
                    .testTag("action_upload_audio"),
                shape = RoundedCornerShape(16.dp),
                color = DarkSurface,
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(NeonCyan.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = "Upload",
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Upload Audio",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "MP3, M4A, WAV",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Demo sample shortcuts
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Instant Demos:",
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted
            )

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = DarkSurfaceVariant,
                border = BorderStroke(1.dp, DarkSurfaceHighlight),
                modifier = Modifier.clickable(onClick = onLoadDefaultDemo).testTag("load_demo_speech")
            ) {
                Text(
                    text = "Studio Voice",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = NeonCyan
                )
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = DarkSurfaceVariant,
                border = BorderStroke(1.dp, DarkSurfaceHighlight),
                modifier = Modifier.clickable(onClick = onLoadNoisyDemo).testTag("load_demo_noisy")
            ) {
                Text(
                    text = "Voice + Noise",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = NeonEmerald
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Studio Hero Waveform Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .border(1.dp, DarkSurfaceHighlight, RoundedCornerShape(22.dp)),
            color = DarkSurface,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header of loaded track
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = rawAudio?.sourceTitle ?: "No audio loaded",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = "Effect: ",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                            Text(
                                text = selectedEffect.displayName,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = selectedEffect.accentColor
                            )
                            if (isProcessing) {
                                Spacer(modifier = Modifier.width(8.dp))
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    color = NeonCyan,
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }

                    // Loop button
                    IconButton(
                        onClick = onToggleLoop,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("toggle_loop_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = "Loop",
                            tint = if (isLooping) NeonCyan else TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Interactive Waveform Visualizer
                WaveformVisualizer(
                    peaks = waveformPeaks,
                    progress = progress,
                    isPlaying = isPlaying,
                    currentRms = currentRms,
                    onSeek = onSeek,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Timing & Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$formattedCurrentTime / $formattedTotalTime",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum"
                        ),
                        color = TextMuted
                    )

                    // Large Play / Pause FAB
                    Surface(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onTogglePlay)
                            .testTag("main_play_pause_button"),
                        shape = CircleShape,
                        color = NeonViolet,
                        tonalElevation = 8.dp
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(NeonViolet, Color(0xFF7C3AED))
                                    )
                                )
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    // Export Button
                    Button(
                        onClick = onOpenExport,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                        modifier = Modifier.testTag("open_export_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Export",
                            color = NeonCyan,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quick Controls Card: Pitch, Speed & Noise Reduction
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, DarkSurfaceHighlight, RoundedCornerShape(20.dp)),
            color = DarkSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Quick Audio Tuning",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }

                    Text(
                        text = "More FX",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = NeonViolet
                        ),
                        modifier = Modifier
                            .clickable(onClick = onOpenFineTune)
                            .testTag("open_fine_tune_text")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Pitch Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Hearing,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Pitch",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = TextPrimary,
                        modifier = Modifier.width(42.dp)
                    )

                    Slider(
                        value = params.pitchSemitones,
                        onValueChange = onPitchChange,
                        valueRange = -12f..12f,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_slider_pitch"),
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = DarkSurfaceHighlight
                        )
                    )

                    Text(
                        text = if (params.pitchSemitones >= 0) "+${params.pitchSemitones.toInt()}st" else "${params.pitchSemitones.toInt()}st",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = NeonCyan,
                        modifier = Modifier.width(42.dp)
                    )
                }

                // Speed Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = NeonViolet,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Speed",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = TextPrimary,
                        modifier = Modifier.width(42.dp)
                    )

                    Slider(
                        value = params.speedRate,
                        onValueChange = onSpeedChange,
                        valueRange = 0.5f..2.0f,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_slider_speed"),
                        colors = SliderDefaults.colors(
                            thumbColor = NeonViolet,
                            activeTrackColor = NeonViolet,
                            inactiveTrackColor = DarkSurfaceHighlight
                        )
                    )

                    Text(
                        text = String.format(Locale.getDefault(), "%.1fx", params.speedRate),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = NeonViolet,
                        modifier = Modifier.width(42.dp)
                    )
                }

                // Noise Gate Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeDown,
                        contentDescription = null,
                        tint = NeonEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Noise",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = TextPrimary,
                        modifier = Modifier.width(42.dp)
                    )

                    Slider(
                        value = params.noiseReduction,
                        onValueChange = onNoiseReductionChange,
                        valueRange = 0.0f..1.0f,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_slider_noise"),
                        colors = SliderDefaults.colors(
                            thumbColor = NeonEmerald,
                            activeTrackColor = NeonEmerald,
                            inactiveTrackColor = DarkSurfaceHighlight
                        )
                    )

                    Text(
                        text = "${(params.noiseReduction * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = NeonEmerald,
                        modifier = Modifier.width(42.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Effects Carousel Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = NeonPink,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Voice Presets",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable(onClick = onNavigateToEffects)
                    .testTag("see_all_effects_button")
            ) {
                Text(
                    text = "See All (${VoiceEffect.entries.size})",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal Carousel of Presets
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(VoiceEffect.entries.filter { it != VoiceEffect.CUSTOM }) { effect ->
                AudioEffectCard(
                    effect = effect,
                    isSelected = selectedEffect == effect,
                    onSelect = { onSelectEffect(effect) },
                    compact = true
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
