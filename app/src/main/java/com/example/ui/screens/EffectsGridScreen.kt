package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VoiceCategory
import com.example.model.VoiceEffect
import com.example.ui.components.AudioEffectCard
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceHighlight
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun EffectsGridScreen(
    selectedCategory: VoiceCategory,
    selectedEffect: VoiceEffect,
    isPlaying: Boolean,
    onSelectCategory: (VoiceCategory) -> Unit,
    onSelectEffect: (VoiceEffect) -> Unit,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allEffects = VoiceEffect.entries.filter { it != VoiceEffect.CUSTOM }
    val filteredEffects = if (selectedCategory == VoiceCategory.ALL) {
        allEffects
    } else {
        allEffects.filter { it.category == selectedCategory }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Category Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 6.dp)
        ) {
            items(VoiceCategory.entries) { category ->
                val isSelected = category == selectedCategory
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) NeonViolet else DarkSurfaceVariant,
                    border = BorderStroke(1.dp, if (isSelected) NeonViolet else DarkSurfaceHighlight),
                    modifier = Modifier
                        .clickable { onSelectCategory(category) }
                        .testTag("filter_chip_${category.name.lowercase()}")
                ) {
                    Text(
                        text = category.displayName,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSelected) Color.White else TextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Grid of Effects
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(filteredEffects, key = { it.id }) { effect ->
                AudioEffectCard(
                    effect = effect,
                    isSelected = selectedEffect == effect,
                    onSelect = {
                        onSelectEffect(effect)
                        if (!isPlaying) {
                            onTogglePlay()
                        }
                    },
                    compact = false
                )
            }
        }

        // Floating Bottom Active Banner
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(16.dp)),
            color = DarkSurface,
            border = BorderStroke(1.dp, NeonViolet.copy(alpha = 0.5f)),
            tonalElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(selectedEffect.accentColor.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = selectedEffect.icon,
                            contentDescription = null,
                            tint = selectedEffect.accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = selectedEffect.displayName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = if (isPlaying) "Playing real-time preview..." else "Tap to hear preview",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isPlaying) NeonCyan else TextMuted
                        )
                    }
                }

                IconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(NeonViolet)
                        .testTag("effects_grid_play_pause")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Preview",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
