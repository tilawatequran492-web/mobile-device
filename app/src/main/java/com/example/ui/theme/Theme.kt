package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonVioletLight,
    onPrimary = Color.White,
    primaryContainer = NeonViolet.copy(alpha = 0.25f),
    onPrimaryContainer = NeonCyanLight,
    secondary = NeonCyan,
    onSecondary = DarkBackground,
    secondaryContainer = NeonCyan.copy(alpha = 0.2f),
    onSecondaryContainer = Color.White,
    tertiary = NeonPink,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    surfaceTint = NeonVioletLight,
    outline = DarkSurfaceHighlight,
    outlineVariant = Color(0xFF1E283E),
    error = RecordingRed,
    onError = Color.White
)

@Composable
fun VoiceChangerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
