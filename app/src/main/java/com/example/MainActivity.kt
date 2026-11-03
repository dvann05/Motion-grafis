package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.ApiConnectionState
import com.example.model.WorkspaceTab
import com.example.ui.components.MultiTrackTimelineStrip
import com.example.ui.components.VideoPreviewMonitorCard
import com.example.ui.screens.ApiKeySettingsPanel
import com.example.ui.screens.AudioStudioPanel
import com.example.ui.screens.ExportMp4Panel
import com.example.ui.screens.SceneEditorPanel
import com.example.ui.screens.StoryAndStylePanel
import com.example.ui.theme.*
import com.example.viewmodel.MotionLabViewModel
import com.example.viewmodel.StudioUiState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MotionLabViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            MotionLabTheme(accentOption = uiState.accentTheme) {
                MotionLabStudioApp(
                    uiState = uiState,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun MotionLabStudioApp(
    uiState: StudioUiState,
    viewModel: MotionLabViewModel
) {
    val context = LocalContext.current

    // BackHandler on sub-tabs returns to primary Studio workspace
    BackHandler(enabled = uiState.activeTab != WorkspaceTab.STUDIO) {
        viewModel.selectTab(WorkspaceTab.STUDIO)
    }

    val saveMp4Launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("video/mp4")
    ) { uri ->
        if (uri != null) {
            viewModel.saveExportedVideoToUri(uri)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = StudioBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            StudioTopHeaderBar(
                uiState = uiState,
                onClickApiStatus = { viewModel.selectTab(WorkspaceTab.API_SETTINGS) },
                onClickQuickExport = { viewModel.selectTab(WorkspaceTab.EXPORT) }
            )
        },
        bottomBar = {
            StudioBottomNavigationBar(
                activeTab = uiState.activeTab,
                onSelectTab = viewModel::selectTab
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isWideScreen = maxWidth >= 720.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Dismissible Notification Toast Banner
                AnimatedVisibility(
                    visible = uiState.statusBannerMessage != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    uiState.statusBannerMessage?.let { msg ->
                        Surface(
                            color = StudioSurfaceElevated,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .testTag("studio_status_banner")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = viewModel::dismissBanner,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Tutup Notifikasi",
                                        tint = StudioTextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (isWideScreen) {
                    // Tablet / Wide layout: Preview & Timeline on Left, Active Inspector Panel on Right
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(0.45f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            VideoPreviewMonitorCard(
                                project = uiState.project,
                                currentTimeSec = uiState.currentTimeSec,
                                isPlaying = uiState.isPlaying,
                                selectedSceneIndex = uiState.selectedSceneIndex,
                                onTogglePlay = viewModel::togglePlayPause,
                                onStepScene = viewModel::stepScene,
                                onSeek = viewModel::seekToTime
                            )
                            MultiTrackTimelineStrip(
                                project = uiState.project,
                                selectedSceneIndex = uiState.selectedSceneIndex,
                                currentTimeSec = uiState.currentTimeSec,
                                onSelectScene = viewModel::selectScene,
                                onAddScene = viewModel::addNewScene
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(0.55f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                        ) {
                            ActiveWorkspacePanelContent(
                                uiState = uiState,
                                viewModel = viewModel,
                                isCompactMode = false,
                                onSaveToDevice = {
                                    val cleanName = uiState.project.title
                                        .replace(Regex("[^a-zA-Z0-9]+"), "_")
                                        .ifBlank { "motion_lab_ai" }
                                    saveMp4Launcher.launch("${cleanName}_1080p.mp4")
                                },
                                onShareVideo = { viewModel.shareExportedVideo(context) }
                            )
                        }
                    }
                } else {
                    // Handheld Android Portrait layout: Smooth AnimatedContent transitions between tabs
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Show 9:16 Video Preview & Timeline persistently at top/bottom on STUDIO & SCENE_EDITOR tabs,
                        // or a compact mini-preview bar on other tabs
                        if (uiState.activeTab == WorkspaceTab.STUDIO || uiState.activeTab == WorkspaceTab.SCENE_EDITOR) {
                            VideoPreviewMonitorCard(
                                project = uiState.project,
                                currentTimeSec = uiState.currentTimeSec,
                                isPlaying = uiState.isPlaying,
                                selectedSceneIndex = uiState.selectedSceneIndex,
                                onTogglePlay = viewModel::togglePlayPause,
                                onStepScene = viewModel::stepScene,
                                onSeek = viewModel::seekToTime
                            )
                            MultiTrackTimelineStrip(
                                project = uiState.project,
                                selectedSceneIndex = uiState.selectedSceneIndex,
                                currentTimeSec = uiState.currentTimeSec,
                                onSelectScene = viewModel::selectScene,
                                onAddScene = viewModel::addNewScene
                            )
                        }

                        AnimatedContent(
                            targetState = uiState.activeTab,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(220)) +
                                    slideInHorizontally(animationSpec = tween(220)) { it / 10 })
                                    .togetherWith(fadeOut(animationSpec = tween(160)))
                            },
                            label = "workspaceTabTransition"
                        ) { targetTab ->
                            ActiveWorkspacePanelContent(
                                uiState = uiState.copy(activeTab = targetTab),
                                viewModel = viewModel,
                                isCompactMode = true,
                                onSaveToDevice = {
                                    val cleanName = uiState.project.title
                                        .replace(Regex("[^a-zA-Z0-9]+"), "_")
                                        .ifBlank { "motion_lab_ai" }
                                    saveMp4Launcher.launch("${cleanName}_1080p.mp4")
                                },
                                onShareVideo = { viewModel.shareExportedVideo(context) }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveWorkspacePanelContent(
    uiState: StudioUiState,
    viewModel: MotionLabViewModel,
    isCompactMode: Boolean,
    onSaveToDevice: () -> Unit,
    onShareVideo: () -> Unit
) {
    when (uiState.activeTab) {
        WorkspaceTab.STUDIO, WorkspaceTab.STORY_AI -> {
            StoryAndStylePanel(
                uiState = uiState,
                onUpdateTitle = viewModel::updateProjectTitle,
                onUpdatePrompt = viewModel::updateStoryPrompt,
                onSelectTheme = viewModel::updateStoryTheme,
                onSelectLanguage = viewModel::updateStoryLanguage,
                onSelectDuration = viewModel::updateTargetDuration,
                onSelectStyle = viewModel::updateAnimationStyle,
                onSparkIdea = viewModel::sparkStoryIdeaWithAi,
                onGenerateScenes = viewModel::generateFullStoryAndScenes,
                onOpenSceneEditor = { idx ->
                    viewModel.selectScene(idx)
                    viewModel.selectTab(WorkspaceTab.SCENE_EDITOR)
                },
                onOpenApiSettings = { viewModel.selectTab(WorkspaceTab.API_SETTINGS) }
            )
        }
        WorkspaceTab.SCENE_EDITOR -> {
            SceneEditorPanel(
                scene = uiState.selectedScene,
                sceneIndex = uiState.selectedSceneIndex,
                totalScenes = uiState.project.scenes.size,
                onUpdateScene = viewModel::updateSelectedScene,
                onAddScene = viewModel::addNewScene,
                onDuplicateScene = viewModel::duplicateSelectedScene,
                onDeleteScene = viewModel::deleteSelectedScene
            )
        }
        WorkspaceTab.AUDIO -> {
            AudioStudioPanel(
                project = uiState.project,
                selectedScene = uiState.selectedScene,
                onToggleVoice = viewModel::updateVoiceEnabled,
                onUpdatePitch = viewModel::updateVoicePitch,
                onUpdateSpeed = viewModel::updateVoiceSpeed,
                onPreviewVoice = viewModel::previewCurrentSceneVoice,
                onToggleSubtitles = viewModel::updateAutoSubtitles,
                onSelectBgm = viewModel::updateBgmTrack,
                onUpdateBgmVolume = viewModel::updateBgmVolume,
                onSelectSceneSfx = { sfx ->
                    viewModel.updateSelectedScene { it.copy(sfxOnEnter = sfx) }
                },
                onTriggerSfx = viewModel::triggerSfxPreview
            )
        }
        WorkspaceTab.EXPORT -> {
            ExportMp4Panel(
                project = uiState.project,
                exportState = uiState.exportState,
                onStartExport = viewModel::startMp4Export,
                onSaveToDevice = onSaveToDevice,
                onShareVideo = onShareVideo
            )
        }
        WorkspaceTab.API_SETTINGS -> {
            ApiKeySettingsPanel(
                uiState = uiState,
                onApiKeyInputChange = viewModel::updateApiKeyInput,
                onToggleVisibility = viewModel::toggleApiKeyVisibility,
                onSaveKey = viewModel::saveApiKey,
                onTestConnection = viewModel::testApiConnection,
                onDeleteKey = viewModel::deleteApiKey,
                onSelectAccentTheme = viewModel::setAccentTheme
            )
        }
    }
}

@Composable
private fun StudioTopHeaderBar(
    uiState: StudioUiState,
    onClickApiStatus: () -> Unit,
    onClickQuickExport: () -> Unit
) {
    val isConnected = uiState.apiConnectionState == ApiConnectionState.CONNECTED
    val dotColor = when {
        isConnected -> StatusConnectedGreen
        uiState.hasSavedApiKey -> MaterialTheme.colorScheme.primary
        else -> StatusErrorRed
    }

    Surface(
        color = StudioSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .size(38.dp)
                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayCircleFilled,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = "MOTION LAB AI",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${uiState.project.animationStyle.displayName} • ${uiState.project.totalDurationSeconds}s",
                        style = MaterialTheme.typography.labelSmall,
                        color = StudioTextSecondary,
                        maxLines = 1
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Live API Connection Status Badge
                Surface(
                    color = StudioSurfaceVariant,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .border(1.dp, StudioBorder, RoundedCornerShape(20.dp))
                        .clickable { onClickApiStatus() }
                        .testTag("top_bar_api_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                        Text(
                            text = if (isConnected) "API Connected" else "API Key",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }

                // Quick Export MP4 Button
                FilledTonalButton(
                    onClick = onClickQuickExport,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("top_bar_export_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Export MP4",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("MP4", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun StudioBottomNavigationBar(
    activeTab: WorkspaceTab,
    onSelectTab: (WorkspaceTab) -> Unit
) {
    NavigationBar(
        containerColor = StudioSurface,
        tonalElevation = 8.dp
    ) {
        val items = listOf(
            Triple(WorkspaceTab.STUDIO, "Studio", Icons.Default.Dashboard),
            Triple(WorkspaceTab.SCENE_EDITOR, "Scenes", Icons.Default.Tune),
            Triple(WorkspaceTab.AUDIO, "Audio", Icons.Default.GraphicEq),
            Triple(WorkspaceTab.EXPORT, "Export", Icons.Default.MovieCreation),
            Triple(WorkspaceTab.API_SETTINGS, "Settings", Icons.Default.Settings)
        )

        items.forEach { (tab, label, icon) ->
            val selected = activeTab == tab || (tab == WorkspaceTab.STUDIO && activeTab == WorkspaceTab.STORY_AI)
            NavigationBarItem(
                selected = selected,
                onClick = { onSelectTab(tab) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label
                    )
                },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                },
                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
            )
        }
    }
}
