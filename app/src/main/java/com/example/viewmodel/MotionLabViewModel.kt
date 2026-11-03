package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ApiTestResult
import com.example.data.GeminiStoryService
import com.example.data.SecurePreferencesManager
import com.example.engine.MotionCanvasRenderer
import com.example.engine.Mp4VideoExporter
import com.example.engine.StudioAudioEngine
import com.example.model.*
import com.example.ui.theme.AccentThemeOption
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class StudioUiState(
    val activeTab: WorkspaceTab = WorkspaceTab.STUDIO,
    val project: MotionProject = MotionProject(),
    val selectedSceneIndex: Int = 0,
    val isPlaying: Boolean = false,
    val currentTimeSec: Float = 0f,
    val isGeneratingStory: Boolean = false,
    val isGeneratingIdea: Boolean = false,
    val statusBannerMessage: String? = null,
    val apiKeyInput: String = "",
    val hasSavedApiKey: Boolean = false,
    val isUsingBuildConfigKey: Boolean = false,
    val isApiKeyVisible: Boolean = false,
    val apiConnectionState: ApiConnectionState = ApiConnectionState.DISCONNECTED,
    val apiStatusMessage: String = "Belum terhubung. Masukkan Google Gemini API Key lalu klik Test Connection.",
    val accentTheme: AccentThemeOption = AccentThemeOption.CYBER_CYAN,
    val exportState: ExportProgressState = ExportProgressState()
) {
    val selectedScene: MotionScene
        get() = project.scenes.getOrElse(selectedSceneIndex) {
            project.scenes.firstOrNull() ?: defaultSampleScenes().first()
        }
}

class MotionLabViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = SecurePreferencesManager(application)
    private val geminiService = GeminiStoryService()
    private val audioEngine = StudioAudioEngine(application)

    private val _uiState = MutableStateFlow(StudioUiState())
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    private var playbackJob: Job? = null
    private var exportJob: Job? = null
    private var lastSfxSceneId: String? = null

    init {
        val savedKey = prefs.getSavedApiKey()
        val usingBuildConfig = prefs.isUsingBuildConfigKey()
        val lastConn = prefs.getLastConnected() && savedKey.isNotEmpty()
        val lastMsg = prefs.getLastStatusMessage().ifBlank {
            if (savedKey.isNotEmpty()) {
                "API Key tersimpan di EncryptedSharedPreferences. Klik Test Connection untuk verifikasi."
            } else {
                "Disconnected • Belum ada API Key yang tersimpan."
            }
        }
        val savedAccent = prefs.getAccentTheme()
        val savedProject = prefs.loadProject() ?: MotionProject()

        _uiState.update {
            it.copy(
                project = savedProject,
                apiKeyInput = savedKey,
                hasSavedApiKey = savedKey.isNotEmpty(),
                isUsingBuildConfigKey = usingBuildConfig,
                apiConnectionState = if (lastConn) ApiConnectionState.CONNECTED else ApiConnectionState.DISCONNECTED,
                apiStatusMessage = lastMsg,
                accentTheme = savedAccent
            )
        }
    }

    fun selectTab(tab: WorkspaceTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun dismissBanner() {
        _uiState.update { it.copy(statusBannerMessage = null) }
    }

    fun setAccentTheme(option: AccentThemeOption) {
        prefs.saveAccentTheme(option)
        _uiState.update {
            it.copy(
                accentTheme = option,
                statusBannerMessage = "Tema aksen diubah ke ${option.label}"
            )
        }
    }

    // --- Playback & Timeline Controls ---

    fun togglePlayPause() {
        val currentlyPlaying = _uiState.value.isPlaying
        if (currentlyPlaying) {
            pausePlayback()
        } else {
            startPlayback()
        }
    }

    fun startPlayback() {
        playbackJob?.cancel()
        val state = _uiState.value
        val total = state.project.totalDurationSeconds.toFloat()
        if (state.currentTimeSec >= total - 0.1f) {
            _uiState.update { it.copy(currentTimeSec = 0f, selectedSceneIndex = 0) }
        }
        audioEngine.resetSceneVoiceTracker()
        lastSfxSceneId = null
        _uiState.update { it.copy(isPlaying = true) }

        audioEngine.startBgmLoop(state.project.bgmTrack, state.project.bgmVolume)

        playbackJob = viewModelScope.launch {
            val frameStepSec = 1f / 30f
            var lastTickMs = System.currentTimeMillis()
            while (isActive && _uiState.value.isPlaying) {
                delay(33L)
                val now = System.currentTimeMillis()
                val dt = ((now - lastTickMs) / 1000f).coerceIn(0.01f, 0.1f)
                lastTickMs = now

                val p = _uiState.value.project
                val maxDur = p.totalDurationSeconds.toFloat().coerceAtLeast(1f)
                val nextTime = _uiState.value.currentTimeSec + dt.coerceAtLeast(frameStepSec * 0.8f)

                if (nextTime >= maxDur) {
                    // Loop back to start and pause cleanly
                    _uiState.update {
                        it.copy(
                            isPlaying = false,
                            currentTimeSec = 0f,
                            selectedSceneIndex = 0
                        )
                    }
                    audioEngine.stopBgmLoop()
                    audioEngine.resetSceneVoiceTracker()
                    break
                } else {
                    val timing = MotionCanvasRenderer.resolveSceneAtTime(p, nextTime)
                    if (lastSfxSceneId != timing.scene.id) {
                        lastSfxSceneId = timing.scene.id
                        audioEngine.playSfx(timing.scene.sfxOnEnter)
                    }
                    audioEngine.speakSceneNarrationIfChanged(
                        sceneId = timing.scene.id,
                        narrationText = timing.scene.narrationText,
                        language = p.language,
                        enabled = p.voiceEnabled,
                        pitch = p.voicePitch,
                        speed = p.voiceSpeed
                    )
                    _uiState.update {
                        it.copy(
                            currentTimeSec = nextTime,
                            selectedSceneIndex = timing.sceneIndex
                        )
                    }
                }
            }
        }
    }

    fun pausePlayback() {
        playbackJob?.cancel()
        playbackJob = null
        audioEngine.stopBgmLoop()
        audioEngine.resetSceneVoiceTracker()
        _uiState.update { it.copy(isPlaying = false) }
    }

    fun seekToTime(seconds: Float) {
        val p = _uiState.value.project
        val clamped = seconds.coerceIn(0f, p.totalDurationSeconds.toFloat())
        val timing = MotionCanvasRenderer.resolveSceneAtTime(p, clamped)
        _uiState.update {
            it.copy(
                currentTimeSec = clamped,
                selectedSceneIndex = timing.sceneIndex
            )
        }
    }

    fun selectScene(index: Int) {
        val scenes = _uiState.value.project.scenes
        if (index !in scenes.indices) return
        val startTime = scenes.take(index).sumOf { it.durationSeconds }.toFloat()
        _uiState.update {
            it.copy(
                selectedSceneIndex = index,
                currentTimeSec = startTime
            )
        }
    }

    fun stepScene(delta: Int) {
        val scenes = _uiState.value.project.scenes
        val nextIdx = (_uiState.value.selectedSceneIndex + delta).coerceIn(0, scenes.lastIndex)
        selectScene(nextIdx)
    }

    // --- Story Generator & AI Integration ---

    fun updateStoryPrompt(prompt: String) {
        updateProject { it.copy(storyPrompt = prompt) }
    }

    fun updateProjectTitle(title: String) {
        updateProject { it.copy(title = title) }
    }

    fun updateStoryTheme(theme: StoryTheme) {
        updateProject { it.copy(theme = theme) }
    }

    fun updateStoryLanguage(language: StoryLanguage) {
        updateProject { it.copy(language = language) }
    }

    fun updateTargetDuration(durationSeconds: Int) {
        updateProject { it.copy(targetDurationSeconds = durationSeconds) }
    }

    fun updateAnimationStyle(style: AnimationStyle) {
        updateProject { it.copy(animationStyle = style) }
        _uiState.update {
            it.copy(statusBannerMessage = "Gaya visual diubah ke ${style.displayName}")
        }
    }

    fun sparkStoryIdeaWithAi() {
        if (_uiState.value.isGeneratingIdea) return
        val state = _uiState.value
        val activeKey = state.apiKeyInput.trim().ifEmpty { prefs.getSavedApiKey() }

        _uiState.update { it.copy(isGeneratingIdea = true) }
        viewModelScope.launch {
            val (idea, notice) = geminiService.generateStoryIdea(
                apiKey = activeKey,
                theme = state.project.theme,
                language = state.project.language
            )
            updateProject { it.copy(storyPrompt = idea) }
            _uiState.update {
                it.copy(
                    isGeneratingIdea = false,
                    statusBannerMessage = notice
                )
            }
        }
    }

    fun generateFullStoryAndScenes() {
        if (_uiState.value.isGeneratingStory) return
        pausePlayback()
        val state = _uiState.value
        val activeKey = state.apiKeyInput.trim().ifEmpty { prefs.getSavedApiKey() }

        _uiState.update { it.copy(isGeneratingStory = true) }
        viewModelScope.launch {
            val result = geminiService.generateFullScriptAndScenes(
                apiKey = activeKey,
                storyPrompt = state.project.storyPrompt,
                theme = state.project.theme,
                language = state.project.language,
                targetDurationSeconds = state.project.targetDurationSeconds,
                animationStyle = state.project.animationStyle
            )
            updateProject {
                it.copy(
                    title = result.projectTitle,
                    scenes = result.scenes
                )
            }
            _uiState.update {
                it.copy(
                    isGeneratingStory = false,
                    selectedSceneIndex = 0,
                    currentTimeSec = 0f,
                    statusBannerMessage = result.statusNotice,
                    apiConnectionState = if (result.usedAi) ApiConnectionState.CONNECTED else it.apiConnectionState
                )
            }
        }
    }

    // --- Scene Editor Operations ---

    fun updateSelectedScene(transform: (MotionScene) -> MotionScene) {
        val idx = _uiState.value.selectedSceneIndex
        updateProject { proj ->
            val mutable = proj.scenes.toMutableList()
            if (idx in mutable.indices) {
                mutable[idx] = transform(mutable[idx])
            }
            proj.copy(scenes = mutable)
        }
    }

    fun addNewScene() {
        updateProject { proj ->
            val nextNum = proj.scenes.size + 1
            val newScene = MotionScene(
                id = "scene_${System.currentTimeMillis()}",
                title = "Scene $nextNum • New Beat",
                headlineText = "ADEGAN BARU $nextNum",
                narrationText = "Tambahkan narasi cerita untuk adegan ke-$nextNum di sini.",
                visualDescription = "Karakter bergerak dinamis dengan partikel latar belakang.",
                durationSeconds = 6,
                backgroundPreset = BackgroundPreset.entries[nextNum % BackgroundPreset.entries.size]
            )
            proj.copy(scenes = proj.scenes + newScene)
        }
        val lastIdx = _uiState.value.project.scenes.lastIndex
        selectScene(lastIdx)
    }

    fun duplicateSelectedScene() {
        val current = _uiState.value.selectedScene
        val idx = _uiState.value.selectedSceneIndex
        updateProject { proj ->
            val copy = current.copy(
                id = "scene_dup_${System.currentTimeMillis()}",
                title = "${current.title} (Copy)"
            )
            val mutable = proj.scenes.toMutableList()
            val insertIdx = (idx + 1).coerceAtMost(mutable.size)
            mutable.add(insertIdx, copy)
            proj.copy(scenes = mutable)
        }
        selectScene(idx + 1)
    }

    fun deleteSelectedScene() {
        val scenes = _uiState.value.project.scenes
        if (scenes.size <= 1) {
            _uiState.update {
                it.copy(statusBannerMessage = "Proyek harus memiliki minimal 1 scene.")
            }
            return
        }
        val idx = _uiState.value.selectedSceneIndex
        updateProject { proj ->
            val mutable = proj.scenes.toMutableList()
            if (idx in mutable.indices) {
                mutable.removeAt(idx)
            }
            proj.copy(scenes = mutable)
        }
        val newIdx = (idx - 1).coerceAtLeast(0)
        selectScene(newIdx)
    }

    // --- Audio & Voiceover Controls ---

    fun updateBgmTrack(track: BgmTrack) {
        updateProject { it.copy(bgmTrack = track) }
        if (_uiState.value.isPlaying) {
            audioEngine.startBgmLoop(track, _uiState.value.project.bgmVolume)
        }
    }

    fun updateBgmVolume(volume: Float) {
        updateProject { it.copy(bgmVolume = volume.coerceIn(0f, 1f)) }
    }

    fun updateVoiceEnabled(enabled: Boolean) {
        updateProject { it.copy(voiceEnabled = enabled) }
    }

    fun updateVoicePitch(pitch: Float) {
        updateProject { it.copy(voicePitch = pitch.coerceIn(0.6f, 1.5f)) }
    }

    fun updateVoiceSpeed(speed: Float) {
        updateProject { it.copy(voiceSpeed = speed.coerceIn(0.6f, 1.6f)) }
    }

    fun updateAutoSubtitles(enabled: Boolean) {
        updateProject { it.copy(autoSubtitles = enabled) }
    }

    fun previewCurrentSceneVoice() {
        val p = _uiState.value.project
        val scene = _uiState.value.selectedScene
        audioEngine.previewVoiceSample(
            sampleText = scene.narrationText,
            language = p.language,
            pitch = p.voicePitch,
            speed = p.voiceSpeed
        )
    }

    fun triggerSfxPreview(sfx: SfxType) {
        audioEngine.playSfx(sfx)
    }

    // --- API Key Manager (EncryptedSharedPreferences) ---

    fun updateApiKeyInput(input: String) {
        _uiState.update { it.copy(apiKeyInput = input) }
    }

    fun toggleApiKeyVisibility() {
        _uiState.update { it.copy(isApiKeyVisible = !it.isApiKeyVisible) }
    }

    fun saveApiKey() {
        val raw = _uiState.value.apiKeyInput.trim()
        if (raw.isEmpty()) {
            _uiState.update {
                it.copy(
                    apiConnectionState = ApiConnectionState.INVALID_KEY,
                    apiStatusMessage = "Kolom API Key masih kosong. Masukkan Google Gemini API Key sebelum menyimpan.",
                    statusBannerMessage = "API Key tidak boleh kosong."
                )
            }
            return
        }
        prefs.saveApiKey(raw)
        prefs.saveLastConnectionStatus(false, "API Key berhasil disimpan secara terenkripsi. Klik Test Connection untuk menguji.")
        _uiState.update {
            it.copy(
                hasSavedApiKey = true,
                isUsingBuildConfigKey = false,
                apiStatusMessage = "API Key tersimpan aman di EncryptedSharedPreferences. Klik Test Connection untuk verifikasi.",
                statusBannerMessage = "API Key berhasil disimpan secara permanen!"
            )
        }
    }

    fun testApiConnection() {
        val keyToTest = _uiState.value.apiKeyInput.trim().ifEmpty { prefs.getSavedApiKey() }
        _uiState.update {
            it.copy(
                apiConnectionState = ApiConnectionState.TESTING,
                apiStatusMessage = "Menguji koneksi ke Google Gemini API..."
            )
        }
        viewModelScope.launch {
            when (val res = geminiService.testConnection(keyToTest)) {
                is ApiTestResult.Connected -> {
                    prefs.saveApiKey(keyToTest)
                    prefs.saveLastConnectionStatus(true, res.message)
                    _uiState.update {
                        it.copy(
                            hasSavedApiKey = true,
                            apiConnectionState = ApiConnectionState.CONNECTED,
                            apiStatusMessage = res.message,
                            statusBannerMessage = "Connected! ${res.message}"
                        )
                    }
                }
                is ApiTestResult.InvalidKey -> {
                    prefs.saveLastConnectionStatus(false, res.message)
                    _uiState.update {
                        it.copy(
                            apiConnectionState = ApiConnectionState.INVALID_KEY,
                            apiStatusMessage = res.message,
                            statusBannerMessage = res.message
                        )
                    }
                }
                is ApiTestResult.QuotaExceeded -> {
                    prefs.saveLastConnectionStatus(false, res.message)
                    _uiState.update {
                        it.copy(
                            apiConnectionState = ApiConnectionState.QUOTA_EXCEEDED,
                            apiStatusMessage = res.message,
                            statusBannerMessage = res.message
                        )
                    }
                }
                is ApiTestResult.NetworkError -> {
                    prefs.saveLastConnectionStatus(false, res.message)
                    _uiState.update {
                        it.copy(
                            apiConnectionState = ApiConnectionState.NETWORK_ERROR,
                            apiStatusMessage = res.message,
                            statusBannerMessage = res.message
                        )
                    }
                }
            }
        }
    }

    fun deleteApiKey() {
        prefs.deleteApiKey()
        _uiState.update {
            it.copy(
                apiKeyInput = "",
                hasSavedApiKey = false,
                isUsingBuildConfigKey = prefs.isUsingBuildConfigKey(),
                apiConnectionState = ApiConnectionState.DISCONNECTED,
                apiStatusMessage = "Disconnected • API Key telah dihapus dari penyimpanan lokal.",
                statusBannerMessage = "API Key berhasil dihapus."
            )
        }
    }

    // --- Export MP4 (1080x1920 @ 30 FPS) ---

    fun startMp4Export() {
        if (_uiState.value.exportState.isExporting) return
        pausePlayback()

        val project = _uiState.value.project
        val totalFrames = project.totalDurationSeconds.coerceIn(5, 90) * Mp4VideoExporter.VIDEO_FPS
        _uiState.update {
            it.copy(
                exportState = ExportProgressState(
                    isExporting = true,
                    currentFrame = 0,
                    totalFrames = totalFrames,
                    statusText = "Menyiapkan encoder H.264 1080x1920 @ 30 FPS..."
                )
            )
        }

        exportJob = viewModelScope.launch {
            try {
                val result = Mp4VideoExporter.exportProjectToMp4(
                    context = getApplication(),
                    project = project,
                    onProgress = { frame, total, status ->
                        _uiState.update { state ->
                            state.copy(
                                exportState = state.exportState.copy(
                                    currentFrame = frame,
                                    totalFrames = total,
                                    statusText = status
                                )
                            )
                        }
                    }
                )
                _uiState.update {
                    it.copy(
                        exportState = ExportProgressState(
                            isExporting = false,
                            currentFrame = totalFrames,
                            totalFrames = totalFrames,
                            statusText = "Export MP4 Selesai! (${result.fileSizeKb} KB • 1080x1920 @ 30 FPS)",
                            exportedFilePath = result.file.absolutePath,
                            exportedMediaStoreUri = result.mediaStoreUri,
                            fileSizeKb = result.fileSizeKb,
                            errorMessage = null
                        ),
                        statusBannerMessage = "Video MP4 1080x1920 berhasil dirender (${result.fileSizeKb} KB)!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        exportState = ExportProgressState(
                            isExporting = false,
                            statusText = "Gagal merender video MP4.",
                            errorMessage = e.localizedMessage ?: "Encoder error"
                        ),
                        statusBannerMessage = "Gagal mengekspor MP4: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun saveExportedVideoToUri(targetUri: Uri) {
        val path = _uiState.value.exportState.exportedFilePath ?: return
        val file = File(path)
        if (!file.exists()) return
        viewModelScope.launch {
            val ok = Mp4VideoExporter.copyFileToUri(getApplication(), file, targetUri)
            _uiState.update {
                it.copy(
                    statusBannerMessage = if (ok) {
                        "File MP4 berhasil diunduh & disimpan ke perangkat!"
                    } else {
                        "Gagal menyimpan file MP4 ke lokasi tersebut."
                    }
                )
            }
        }
    }

    fun shareExportedVideo(context: Context) {
        val path = _uiState.value.exportState.exportedFilePath ?: return
        val file = File(path)
        if (!file.exists()) return
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Bagikan Video MP4 Motion Lab AI"))
        } catch (e: Exception) {
            _uiState.update {
                it.copy(statusBannerMessage = "Tidak dapat membuka menu bagikan: ${e.localizedMessage}")
            }
        }
    }

    private fun updateProject(transform: (MotionProject) -> MotionProject) {
        _uiState.update { current ->
            val updated = transform(current.project)
            prefs.saveProject(updated)
            current.copy(project = updated)
        }
    }

    override fun onCleared() {
        super.onCleared()
        playbackJob?.cancel()
        exportJob?.cancel()
        audioEngine.release()
    }
}
