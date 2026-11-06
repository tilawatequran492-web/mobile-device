package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioDspEngine
import com.example.audio.AudioExportManager
import com.example.audio.AudioFileImporter
import com.example.audio.AudioRecorderHelper
import com.example.audio.DemoAudioProvider
import com.example.audio.RealtimeAudioPlayer
import com.example.model.AudioData
import com.example.model.SavedAudioItem
import com.example.model.VoiceCategory
import com.example.model.VoiceEffect
import com.example.model.VoiceModifierParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class MainTab {
    STUDIO,
    EFFECTS,
    SAVED
}

data class UiMessage(val message: String, val isError: Boolean = false)

class VoiceChangerViewModel(application: Application) : AndroidViewModel(application) {

    private val player = RealtimeAudioPlayer(viewModelScope)
    private val recorder = AudioRecorderHelper(viewModelScope)

    val isPlaying: StateFlow<Boolean> = player.isPlaying
    val progress: StateFlow<Float> = player.progress
    val playbackPositionMs: StateFlow<Long> = player.playbackPositionMs
    val durationMs: StateFlow<Long> = player.durationMs
    val currentRms: StateFlow<Float> = player.currentRms

    val isRecording: StateFlow<Boolean> = recorder.isRecording
    val recordingDurationMs: StateFlow<Long> = recorder.recordingDurationMs
    val liveRecordingAmplitude: StateFlow<Float> = recorder.liveAmplitude

    private val _currentTab = MutableStateFlow(MainTab.STUDIO)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    private val _selectedCategory = MutableStateFlow(VoiceCategory.ALL)
    val selectedCategory: StateFlow<VoiceCategory> = _selectedCategory.asStateFlow()

    private val _rawAudio = MutableStateFlow<AudioData?>(null)
    val rawAudio: StateFlow<AudioData?> = _rawAudio.asStateFlow()

    private val _processedAudio = MutableStateFlow<AudioData?>(null)
    val processedAudio: StateFlow<AudioData?> = _processedAudio.asStateFlow()

    private val _waveformPeaks = MutableStateFlow(FloatArray(64) { 0.12f })
    val waveformPeaks: StateFlow<FloatArray> = _waveformPeaks.asStateFlow()

    private val _params = MutableStateFlow(VoiceModifierParams())
    val params: StateFlow<VoiceModifierParams> = _params.asStateFlow()

    private val _selectedEffect = MutableStateFlow(VoiceEffect.ORIGINAL)
    val selectedEffect: StateFlow<VoiceEffect> = _selectedEffect.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _isLooping = MutableStateFlow(false)
    val isLooping: StateFlow<Boolean> = _isLooping.asStateFlow()

    private val _savedAudios = MutableStateFlow<List<SavedAudioItem>>(emptyList())
    val savedAudios: StateFlow<List<SavedAudioItem>> = _savedAudios.asStateFlow()

    private val _showFineTuneSheet = MutableStateFlow(false)
    val showFineTuneSheet: StateFlow<Boolean> = _showFineTuneSheet.asStateFlow()

    private val _showRecordDialog = MutableStateFlow(false)
    val showRecordDialog: StateFlow<Boolean> = _showRecordDialog.asStateFlow()

    private val _showExportDialog = MutableStateFlow(false)
    val showExportDialog: StateFlow<Boolean> = _showExportDialog.asStateFlow()

    private val _toastEvents = MutableSharedFlow<UiMessage>()
    val toastEvents: SharedFlow<UiMessage> = _toastEvents.asSharedFlow()

    private var dspDebounceJob: Job? = null

    init {
        // Load default speech demo on startup so the app is immediately interactive!
        loadDefaultDemo()
        refreshSavedAudios()
    }

    fun setTab(tab: MainTab) {
        _currentTab.value = tab
    }

    fun setCategory(cat: VoiceCategory) {
        _selectedCategory.value = cat
    }

    fun setShowFineTuneSheet(show: Boolean) {
        _showFineTuneSheet.value = show
    }

    fun setShowRecordDialog(show: Boolean) {
        _showRecordDialog.value = show
    }

    fun setShowExportDialog(show: Boolean) {
        _showExportDialog.value = show
    }

    fun loadDefaultDemo() {
        val demo = DemoAudioProvider.getSpeechDemo()
        setRawAudio(demo)
    }

    fun loadNoisyDemo() {
        val noisy = DemoAudioProvider.getSpeechWithNoiseDemo()
        setRawAudio(noisy)
        sendToast("Loaded Voice with Background Noise Demo")
    }

    fun importAudioUri(context: Context, uri: Uri, title: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            val result = AudioFileImporter.decodeAudioUri(context, uri, title)
            _isProcessing.value = false
            result.onSuccess { audio ->
                setRawAudio(audio)
                sendToast("Imported: ${audio.sourceTitle}")
            }.onFailure { err ->
                sendToast("Failed to load audio: ${err.localizedMessage ?: "Unknown error"}", isError = true)
            }
        }
    }

    fun startRecording(onFailed: (String) -> Unit) {
        player.pause()
        val success = recorder.startRecording(onFailed)
        if (success) {
            recorder.recordLoop { recordedAudio ->
                if (recordedAudio != null && recordedAudio.samples.isNotEmpty()) {
                    setRawAudio(recordedAudio)
                    sendToast("Recording captured successfully!")
                }
            }
        }
    }

    fun stopRecording() {
        recorder.stopRecording()
        _showRecordDialog.value = false
    }

    fun cancelRecording() {
        recorder.cancelRecording()
        _showRecordDialog.value = false
    }

    private fun setRawAudio(audio: AudioData) {
        _rawAudio.value = audio
        _waveformPeaks.value = audio.extractWaveformPeaks(64)
        reprocessAudio(immediate = true)
    }

    fun selectEffect(effect: VoiceEffect) {
        _selectedEffect.value = effect
        val newParams = effect.toParams()
        _params.value = newParams
        reprocessAudio(immediate = true)
    }

    fun updatePitch(semitones: Float) {
        _selectedEffect.value = VoiceEffect.CUSTOM
        _params.update { it.copy(effect = VoiceEffect.CUSTOM, pitchSemitones = semitones) }
        reprocessAudio(immediate = false)
    }

    fun updateSpeed(speed: Float) {
        _selectedEffect.value = VoiceEffect.CUSTOM
        _params.update { it.copy(effect = VoiceEffect.CUSTOM, speedRate = speed) }
        reprocessAudio(immediate = false)
    }

    fun updateNoiseReduction(level: Float) {
        _params.update { it.copy(noiseReduction = level) }
        reprocessAudio(immediate = false)
    }

    fun updateReverb(reverb: Float) {
        _selectedEffect.value = VoiceEffect.CUSTOM
        _params.update { it.copy(effect = VoiceEffect.CUSTOM, reverb = reverb) }
        reprocessAudio(immediate = false)
    }

    fun updateDistortion(distortion: Float) {
        _selectedEffect.value = VoiceEffect.CUSTOM
        _params.update { it.copy(effect = VoiceEffect.CUSTOM, distortion = distortion) }
        reprocessAudio(immediate = false)
    }

    fun updateEcho(delayMs: Int, feedback: Float) {
        _selectedEffect.value = VoiceEffect.CUSTOM
        _params.update { it.copy(effect = VoiceEffect.CUSTOM, echoDelayMs = delayMs, echoFeedback = feedback) }
        reprocessAudio(immediate = false)
    }

    fun resetToOriginal() {
        selectEffect(VoiceEffect.ORIGINAL)
    }

    private fun reprocessAudio(immediate: Boolean = false) {
        dspDebounceJob?.cancel()
        val raw = _rawAudio.value ?: return
        val currentP = _params.value

        dspDebounceJob = viewModelScope.launch(Dispatchers.Default) {
            if (!immediate) {
                delay(80) // smooth slider dragging debounce
            }
            _isProcessing.value = true

            val processedSamples = AudioDspEngine.process(
                input = raw.samples,
                params = currentP,
                sampleRate = raw.sampleRate
            )

            val processed = AudioData(
                samples = processedSamples,
                sampleRate = raw.sampleRate,
                sourceTitle = "${raw.sourceTitle} (${currentP.effect.displayName})"
            )

            withContext(Dispatchers.Main) {
                _processedAudio.value = processed
                _waveformPeaks.value = processed.extractWaveformPeaks(64)
                _isProcessing.value = false

                // Real-time audio player update: preserve position if already playing
                if (player.isPlaying.value) {
                    player.updateSamplesPreservingPosition(processedSamples)
                } else {
                    player.loadAudio(processedSamples, raw.sampleRate, resetPosition = false)
                }
            }
        }
    }

    fun togglePlayPause() {
        val currentP = _processedAudio.value
        if (currentP == null || currentP.samples.isEmpty()) {
            return
        }
        player.togglePlayPause()
    }

    fun seekTo(progress: Float) {
        player.seekTo(progress)
    }

    fun toggleLoop() {
        val next = !_isLooping.value
        _isLooping.value = next
        player.isLooping = next
    }

    fun exportCurrentAudio(context: Context, customTitle: String) {
        val audio = _processedAudio.value ?: return
        val effectName = _selectedEffect.value.displayName

        viewModelScope.launch {
            _isProcessing.value = true
            val title = customTitle.ifBlank { audio.sourceTitle }
            val result = AudioExportManager.exportAudio(
                context = context,
                samples = audio.samples,
                sampleRate = audio.sampleRate,
                title = title,
                effectName = effectName
            )
            _isProcessing.value = false
            _showExportDialog.value = false

            result.onSuccess { item ->
                sendToast("Exported: ${item.file.name}")
                refreshSavedAudios()
                // Offer 1-tap share immediately
                AudioExportManager.shareAudio(context, item.file, item.title)
            }.onFailure { err ->
                sendToast("Export failed: ${err.message}", isError = true)
            }
        }
    }

    fun refreshSavedAudios() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val list = AudioExportManager.getSavedAudios(context)
            _savedAudios.value = list
        }
    }

    fun deleteSavedAudio(item: SavedAudioItem) {
        viewModelScope.launch {
            val deleted = AudioExportManager.deleteAudio(item.file)
            if (deleted) {
                refreshSavedAudios()
                sendToast("Deleted ${item.title}")
            }
        }
    }

    fun playSavedAudio(item: SavedAudioItem) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val uri = Uri.fromFile(item.file)
            val result = AudioFileImporter.decodeAudioUri(context, uri, item.title)
            result.onSuccess { audio ->
                setRawAudio(audio)
                setTab(MainTab.STUDIO)
                player.play()
                sendToast("Playing ${item.title}")
            }.onFailure {
                sendToast("Could not load file", isError = true)
            }
        }
    }

    fun shareSavedAudio(context: Context, item: SavedAudioItem) {
        AudioExportManager.shareAudio(context, item.file, item.title)
    }

    private fun sendToast(msg: String, isError: Boolean = false) {
        viewModelScope.launch {
            _toastEvents.emit(UiMessage(msg, isError))
        }
    }

    override fun onCleared() {
        super.onCleared()
        player.release()
        recorder.cancelRecording()
    }
}
