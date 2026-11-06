package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.WaveformUnplayed
import kotlin.math.sin

@Composable
fun WaveformVisualizer(
    peaks: FloatArray,
    progress: Float,
    isPlaying: Boolean,
    currentRms: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // Subtle breathing shimmer animation while playing
    val shimmerPhase = remember { Animatable(0f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            shimmerPhase.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        } else {
            shimmerPhase.snapTo(0f)
        }
    }

    Box(
        modifier = modifier
            .height(130.dp)
            .testTag("waveform_visualizer")
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val frac = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeek(frac)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val frac = (change.position.x / size.width).coerceIn(0f, 1f)
                    onSeek(frac)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerY = height / 2f

            if (peaks.isEmpty()) return@Canvas

            val count = peaks.size
            val barSpacing = 3.dp.toPx()
            val totalSpacing = barSpacing * (count - 1)
            val barWidth = ((width - totalSpacing) / count).coerceAtLeast(2.dp.toPx())

            val playedBrush = Brush.verticalGradient(
                colors = listOf(NeonCyan, NeonViolet),
                startY = 0f,
                endY = height
            )

            val unplayedColor = WaveformUnplayed
            val progressX = width * progress

            for (i in 0 until count) {
                val x = i * (barWidth + barSpacing)
                val isPlayed = (x + barWidth / 2f) <= progressX

                // Base peak height
                var barHeight = peaks[i] * (height * 0.85f)

                // Dynamic pulse if active and playing
                if (isPlaying) {
                    val distFromHead = Math.abs(x - progressX)
                    if (distFromHead < barWidth * 6) {
                        val pulse = (1f + currentRms * 1.5f + (sin((shimmerPhase.value * 2 * Math.PI) + i * 0.3).toFloat() * 0.15f))
                        barHeight = (barHeight * pulse).coerceAtMost(height * 0.95f)
                    }
                }
                barHeight = barHeight.coerceAtLeast(4.dp.toPx())

                val top = centerY - (barHeight / 2f)

                drawRoundRect(
                    brush = if (isPlayed) playedBrush else Brush.linearGradient(listOf(unplayedColor, unplayedColor)),
                    topLeft = Offset(x, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )

                // Glowing cursor line indicator at current progress
                if (Math.abs(x - progressX) <= (barWidth + barSpacing)) {
                    drawCircle(
                        color = NeonCyan,
                        radius = 4.dp.toPx(),
                        center = Offset(progressX, centerY)
                    )
                }
            }

            // Playhead vertical line
            drawLine(
                color = Color.White.copy(alpha = 0.9f),
                start = Offset(progressX, 4.dp.toPx()),
                end = Offset(progressX, height - 4.dp.toPx()),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}
