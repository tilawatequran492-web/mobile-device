package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceHighlight
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.RecordingRed
import com.example.ui.theme.RecordingRedGlow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import java.util.Locale
import kotlin.math.sin

@Composable
fun RecordDialog(
    isRecording: Boolean,
    durationMs: Long,
    liveAmplitude: Float,
    onStartRecording: (onFailed: (String) -> Unit) -> Unit,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDeniedMessage by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            permissionDeniedMessage = null
            onStartRecording { err -> permissionDeniedMessage = err }
        } else {
            permissionDeniedMessage = "Microphone permission is required to record audio."
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else if (!isRecording) {
            onStartRecording { err -> permissionDeniedMessage = err }
        }
    }

    val pulseScale = remember { Animatable(1f) }
    LaunchedEffect(isRecording) {
        if (isRecording) {
            pulseScale.animateTo(
                targetValue = 1.25f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        }
    }

    val formattedDuration = remember(durationMs) {
        val totalSecs = (durationMs / 1000).toInt()
        val minutes = totalSecs / 60
        val seconds = totalSecs % 60
        val tenths = ((durationMs % 1000) / 100).toInt()
        String.format(Locale.getDefault(), "%02d:%02d.%d", minutes, seconds, tenths)
    }

    Dialog(
        onDismissRequest = {
            if (isRecording) onCancelRecording() else onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, DarkSurfaceHighlight, RoundedCornerShape(24.dp)),
            color = DarkSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Voice Recorder",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    IconButton(
                        onClick = {
                            if (isRecording) onCancelRecording() else onDismiss()
                        },
                        modifier = Modifier.testTag("record_dialog_close")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (permissionDeniedMessage != null) {
                    Text(
                        text = permissionDeniedMessage!!,
                        color = RecordingRed,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        modifier = Modifier.testTag("grant_permission_button")
                    ) {
                        Text("Grant Permission")
                    }
                } else {
                    // Animated Mic Recording Orb
                    Box(
                        modifier = Modifier.size(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Background concentric ripple waves
                        Canvas(modifier = Modifier.size(160.dp)) {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            if (isRecording) {
                                val dynamicAmp = liveAmplitude * 30.dp.toPx()
                                drawCircle(
                                    color = RecordingRedGlow,
                                    radius = (60.dp.toPx() * pulseScale.value) + dynamicAmp,
                                    center = center
                                )
                                drawCircle(
                                    color = NeonViolet.copy(alpha = 0.15f),
                                    radius = 75.dp.toPx() + dynamicAmp * 0.5f,
                                    center = center
                                )
                            }
                        }

                        // Central Orb
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isRecording) {
                                        Brush.linearGradient(listOf(RecordingRed, Color(0xFFB91C1C)))
                                    } else {
                                        Brush.linearGradient(listOf(NeonViolet, NeonCyan))
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isRecording) Icons.Default.Mic else Icons.Default.Stop,
                                contentDescription = "Recording Icon",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Timer
                    Text(
                        text = formattedDuration,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp
                        ),
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (isRecording) "Listening & capturing voice..." else "Ready",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isRecording) NeonCyan else TextMuted
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Live Waveform visualizer in recording
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(DarkBackground, RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxWidth().height(36.dp)) {
                            val bars = 28
                            val barW = 4.dp.toPx()
                            val totalWidth = size.width
                            val spacing = (totalWidth - (bars * barW)) / (bars - 1)
                            val midY = size.height / 2f

                            for (i in 0 until bars) {
                                val x = i * (barW + spacing)
                                val wave = sin((i * 0.4) + (durationMs * 0.01)).toFloat()
                                val amp = if (isRecording) {
                                    (liveAmplitude * (size.height * 0.8f) * (0.6f + 0.4f * wave)).coerceAtLeast(3.dp.toPx())
                                } else {
                                    3.dp.toPx()
                                }
                                drawRoundRect(
                                    color = if (isRecording) RecordingRed else TextMuted,
                                    topLeft = Offset(x, midY - amp / 2f),
                                    size = androidx.compose.ui.geometry.Size(barW, amp),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCancelRecording,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("record_cancel_button"),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceHighlight)
                        ) {
                            Text("Discard", color = TextMuted)
                        }

                        Button(
                            onClick = onStopRecording,
                            modifier = Modifier
                                .weight(1.5f)
                                .height(50.dp)
                                .testTag("record_stop_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonViolet
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save & Apply", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
