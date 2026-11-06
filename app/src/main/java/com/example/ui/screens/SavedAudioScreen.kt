package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SavedAudioItem
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceHighlight
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.RecordingRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun SavedAudioScreen(
    savedAudios: List<SavedAudioItem>,
    onPlayAudio: (SavedAudioItem) -> Unit,
    onShareAudio: (SavedAudioItem) -> Unit,
    onDeleteAudio: (SavedAudioItem) -> Unit,
    onRefresh: () -> Unit,
    onRecordNew: () -> Unit,
    modifier: Modifier = Modifier
) {
    var itemToDelete by remember { mutableStateOf<SavedAudioItem?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Saved Audio Clips",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "${savedAudios.size} exported recordings in library",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }

            IconButton(
                onClick = onRefresh,
                modifier = Modifier.testTag("refresh_saved_audios_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = NeonCyan
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (savedAudios.isEmpty()) {
            // Empty State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant)
                            .border(1.dp, DarkSurfaceHighlight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = NeonViolet,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "No Exported Clips Yet",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Record or upload audio, apply your favorite voice effect, and export your clip to listen or share anytime.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onRecordNew,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
                        modifier = Modifier.testTag("empty_state_record_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Audiotrack,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create First Voice Clip", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(savedAudios, key = { it.id }) { item ->
                    SavedAudioItemCard(
                        item = item,
                        onPlay = { onPlayAudio(item) },
                        onShare = { onShareAudio(item) },
                        onDelete = { itemToDelete = item }
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = {
                Text(text = "Delete Clip?", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete \"${item.title}\"?",
                    color = TextPrimary
                )
            },
            containerColor = DarkSurface,
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteAudio(item)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RecordingRed),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { itemToDelete = null },
                    modifier = Modifier.testTag("cancel_delete_button")
                ) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }
}

@Composable
fun SavedAudioItemCard(
    item: SavedAudioItem,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onPlay)
            .testTag("saved_audio_item_${item.id}"),
        color = DarkSurface,
        border = BorderStroke(1.dp, DarkSurfaceHighlight),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play Button
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(NeonViolet.copy(alpha = 0.2f))
                    .clickable(onClick = onPlay),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = NeonViolet,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = NeonCyan.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = item.effectName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = NeonCyan,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "${item.formattedDuration} • ${item.formattedSize}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = item.formattedDate,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = TextMuted
                )
            }

            // Share Button
            IconButton(
                onClick = onShare,
                modifier = Modifier.size(38.dp).testTag("share_item_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = NeonCyan,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Delete Button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(38.dp).testTag("delete_item_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
