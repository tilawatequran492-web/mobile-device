package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ExportDialog
import com.example.ui.components.FineTuneSheet
import com.example.ui.components.RecordDialog
import com.example.ui.screens.EffectsGridScreen
import com.example.ui.screens.SavedAudioScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceHighlight
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.viewmodel.MainTab
import com.example.viewmodel.VoiceChangerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceChangerMainApp(
    viewModel: VoiceChangerViewModel
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val rawAudio by viewModel.rawAudio.collectAsStateWithLifecycle()
    val processedAudio by viewModel.processedAudio.collectAsStateWithLifecycle()
    val waveformPeaks by viewModel.waveformPeaks.collectAsStateWithLifecycle()
    val params by viewModel.params.collectAsStateWithLifecycle()
    val selectedEffect by viewModel.selectedEffect.collectAsStateWithLifecycle()

    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val playbackPositionMs by viewModel.playbackPositionMs.collectAsStateWithLifecycle()
    val durationMs by viewModel.durationMs.collectAsStateWithLifecycle()
    val currentRms by viewModel.currentRms.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val isLooping by viewModel.isLooping.collectAsStateWithLifecycle()

    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val recordingDurationMs by viewModel.recordingDurationMs.collectAsStateWithLifecycle()
    val liveRecordingAmplitude by viewModel.liveRecordingAmplitude.collectAsStateWithLifecycle()

    val savedAudios by viewModel.savedAudios.collectAsStateWithLifecycle()
    val showFineTuneSheet by viewModel.showFineTuneSheet.collectAsStateWithLifecycle()
    val showRecordDialog by viewModel.showRecordDialog.collectAsStateWithLifecycle()
    val showExportDialog by viewModel.showExportDialog.collectAsStateWithLifecycle()

    // Listen to toasts
    LaunchedEffect(Unit) {
        viewModel.toastEvents.collect { uiMsg ->
            Toast.makeText(context, uiMsg.message, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NeonViolet.copy(alpha = 0.2f),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(18.dp)
                            )
                        }
                        Text(
                            text = "AI Voice Changer",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.setShowFineTuneSheet(true) },
                        modifier = Modifier.testTag("topbar_fine_tune_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Fine-Tune",
                            tint = NeonViolet
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                // Tab 1: Studio
                NavigationBarItem(
                    selected = currentTab == MainTab.STUDIO,
                    onClick = { viewModel.setTab(MainTab.STUDIO) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == MainTab.STUDIO) Icons.Default.GraphicEq else Icons.Outlined.GraphicEq,
                            contentDescription = "Studio",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Studio",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = NeonCyan,
                        indicatorColor = NeonViolet,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("tab_studio")
                )

                // Tab 2: Effects Catalogue
                NavigationBarItem(
                    selected = currentTab == MainTab.EFFECTS,
                    onClick = { viewModel.setTab(MainTab.EFFECTS) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == MainTab.EFFECTS) Icons.Default.AutoAwesome else Icons.Outlined.AutoAwesome,
                            contentDescription = "Effects",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Effects",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = NeonCyan,
                        indicatorColor = NeonViolet,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("tab_effects")
                )

                // Tab 3: Saved Library
                NavigationBarItem(
                    selected = currentTab == MainTab.SAVED,
                    onClick = { viewModel.setTab(MainTab.SAVED) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == MainTab.SAVED) Icons.Default.Folder else Icons.Outlined.Folder,
                            contentDescription = "Saved Clips",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Saved",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = NeonCyan,
                        indicatorColor = NeonViolet,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("tab_saved")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                MainTab.STUDIO -> {
                    StudioScreen(
                        rawAudio = rawAudio,
                        processedAudio = processedAudio,
                        waveformPeaks = waveformPeaks,
                        params = params,
                        selectedEffect = selectedEffect,
                        isPlaying = isPlaying,
                        progress = progress,
                        playbackPositionMs = playbackPositionMs,
                        durationMs = durationMs,
                        currentRms = currentRms,
                        isProcessing = isProcessing,
                        isLooping = isLooping,
                        onTogglePlay = { viewModel.togglePlayPause() },
                        onSeek = { viewModel.seekTo(it) },
                        onToggleLoop = { viewModel.toggleLoop() },
                        onSelectEffect = { viewModel.selectEffect(it) },
                        onPitchChange = { viewModel.updatePitch(it) },
                        onSpeedChange = { viewModel.updateSpeed(it) },
                        onNoiseReductionChange = { viewModel.updateNoiseReduction(it) },
                        onOpenFineTune = { viewModel.setShowFineTuneSheet(true) },
                        onOpenRecord = { viewModel.setShowRecordDialog(true) },
                        onOpenExport = { viewModel.setShowExportDialog(true) },
                        onLoadDefaultDemo = { viewModel.loadDefaultDemo() },
                        onLoadNoisyDemo = { viewModel.loadNoisyDemo() },
                        onImportAudio = { uri, name -> viewModel.importAudioUri(context, uri, name) },
                        onNavigateToEffects = { viewModel.setTab(MainTab.EFFECTS) }
                    )
                }

                MainTab.EFFECTS -> {
                    EffectsGridScreen(
                        selectedCategory = selectedCategory,
                        selectedEffect = selectedEffect,
                        isPlaying = isPlaying,
                        onSelectCategory = { viewModel.setCategory(it) },
                        onSelectEffect = { viewModel.selectEffect(it) },
                        onTogglePlay = { viewModel.togglePlayPause() }
                    )
                }

                MainTab.SAVED -> {
                    SavedAudioScreen(
                        savedAudios = savedAudios,
                        onPlayAudio = { viewModel.playSavedAudio(it) },
                        onShareAudio = { viewModel.shareSavedAudio(context, it) },
                        onDeleteAudio = { viewModel.deleteSavedAudio(it) },
                        onRefresh = { viewModel.refreshSavedAudios() },
                        onRecordNew = {
                            viewModel.setTab(MainTab.STUDIO)
                            viewModel.setShowRecordDialog(true)
                        }
                    )
                }
            }
        }
    }

    // Modal: Fine-Tuning Sheet
    if (showFineTuneSheet) {
        FineTuneSheet(
            params = params,
            onPitchChange = { viewModel.updatePitch(it) },
            onSpeedChange = { viewModel.updateSpeed(it) },
            onNoiseReductionChange = { viewModel.updateNoiseReduction(it) },
            onReverbChange = { viewModel.updateReverb(it) },
            onDistortionChange = { viewModel.updateDistortion(it) },
            onReset = { viewModel.resetToOriginal() },
            onDismiss = { viewModel.setShowFineTuneSheet(false) }
        )
    }

    // Modal: Audio Recorder
    if (showRecordDialog) {
        RecordDialog(
            isRecording = isRecording,
            durationMs = recordingDurationMs,
            liveAmplitude = liveRecordingAmplitude,
            onStartRecording = { onFailed -> viewModel.startRecording(onFailed) },
            onStopRecording = { viewModel.stopRecording() },
            onCancelRecording = { viewModel.cancelRecording() },
            onDismiss = { viewModel.setShowRecordDialog(false) }
        )
    }

    // Modal: Export Dialog
    if (showExportDialog) {
        ExportDialog(
            effect = selectedEffect,
            defaultTitle = rawAudio?.sourceTitle ?: "My Voice Clip",
            onConfirmExport = { title -> viewModel.exportCurrentAudio(context, title) },
            onDismiss = { viewModel.setShowExportDialog(false) }
        )
    }
}
