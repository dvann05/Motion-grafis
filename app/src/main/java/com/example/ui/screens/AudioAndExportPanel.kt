package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AudioStudioPanel(
    project: MotionProject,
    selectedScene: MotionScene,
    onToggleVoice: (Boolean) -> Unit,
    onUpdatePitch: (Float) -> Unit,
    onUpdateSpeed: (Float) -> Unit,
    onPreviewVoice: () -> Unit,
    onToggleSubtitles: (Boolean) -> Unit,
    onSelectBgm: (BgmTrack) -> Unit,
    onUpdateBgmVolume: (Float) -> Unit,
    onSelectSceneSfx: (SfxType) -> Unit,
    onTriggerSfx: (SfxType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Voiceover Narration & Auto Subtitle Card
        Card(
            colors = CardDefaults.cardColors(containerColor = StudioSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StudioBorder, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "NARASI SUARA (AI TTS VOICEOVER)",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                text = "Bahasa Aktif: ${project.language.displayName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = StudioTextSecondary
                            )
                        }
                    }
                    Switch(
                        checked = project.voiceEnabled,
                        onCheckedChange = onToggleVoice,
                        modifier = Modifier.testTag("switch_voiceover")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pitch Suara: ${String.format(Locale.US, "%.2fx", project.voicePitch)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = StudioTextSecondary
                    )
                    Text(
                        text = "Kecepatan: ${String.format(Locale.US, "%.2fx", project.voiceSpeed)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = StudioTextSecondary
                    )
                }

                Slider(
                    value = project.voicePitch,
                    onValueChange = onUpdatePitch,
                    valueRange = 0.7f..1.4f,
                    modifier = Modifier.fillMaxWidth()
                )

                Slider(
                    value = project.voiceSpeed,
                    onValueChange = onUpdateSpeed,
                    valueRange = 0.75f..1.45f,
                    modifier = Modifier.fillMaxWidth()
                )

                FilledTonalButton(
                    onClick = onPreviewVoice,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_preview_voiceover")
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tes Suara Narasi Scene Aktif")
                }

                HorizontalDivider(color = StudioBorder)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Subtitle Otomatis Karaoke (Auto-Synced)",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White
                        )
                        Text(
                            text = "Menampilkan highlight kata demi kata di layar video berdasarkan naskah",
                            style = MaterialTheme.typography.bodySmall,
                            color = StudioTextSecondary
                        )
                    }
                    Switch(
                        checked = project.autoSubtitles,
                        onCheckedChange = onToggleSubtitles,
                        modifier = Modifier.testTag("switch_auto_subtitles")
                    )
                }
            }
        }

        // 2. Background Music Synthesizer Card
        Card(
            colors = CardDefaults.cardColors(containerColor = StudioSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StudioBorder, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LibraryMusic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "BACKGROUND MUSIC (SYNTH ENGINE)",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BgmTrack.entries.forEach { track ->
                        FilterChip(
                            selected = project.bgmTrack == track,
                            onClick = { onSelectBgm(track) },
                            label = {
                                Text(if (track.bpm > 0) "${track.label} (${track.bpm} BPM)" else track.label)
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Volume Musik Latar", style = MaterialTheme.typography.labelLarge, color = StudioTextSecondary)
                    Text("${(project.bgmVolume * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = project.bgmVolume,
                    onValueChange = onUpdateBgmVolume,
                    valueRange = 0f..1f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 3. Sound Effects (SFX) Card
        Card(
            colors = CardDefaults.cardColors(containerColor = StudioSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StudioBorder, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "SOUND EFFECTS (SFX TRANSISI SCENE)",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SfxType.entries.forEach { sfx ->
                        FilterChip(
                            selected = selectedScene.sfxOnEnter == sfx,
                            onClick = {
                                onSelectSceneSfx(sfx)
                                onTriggerSfx(sfx)
                            },
                            label = { Text(sfx.label) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExportMp4Panel(
    project: MotionProject,
    exportState: ExportProgressState,
    onStartExport: () -> Unit,
    onSaveToDevice: () -> Unit,
    onShareVideo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalFrames = project.totalDurationSeconds * 30

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = StudioSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StudioBorder, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HighQuality,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            text = "EXPORT FULL HD MP4 VIDEO",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Text(
                            text = "Hardware H.264 / AVC Encoder • MediaMuxer MP4",
                            style = MaterialTheme.typography.bodySmall,
                            color = StudioTextSecondary
                        )
                    }
                }

                // Technical Specs Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SpecBadgeCard("FORMAT", "MP4 (H.264)", Modifier.weight(1f))
                    SpecBadgeCard("RESOLUSI", "1080 × 1920", Modifier.weight(1f))
                    SpecBadgeCard("FRAME RATE", "30 FPS", Modifier.weight(1f))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SpecBadgeCard("RASIO", "9:16 Vertical", Modifier.weight(1f))
                    SpecBadgeCard("DURASI", "${project.totalDurationSeconds} Detik", Modifier.weight(1f))
                    SpecBadgeCard("TOTAL FRAME", "$totalFrames Frames", Modifier.weight(1f))
                }

                if (exportState.isExporting) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(StudioSurfaceVariant)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = exportState.statusText,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )
                            Text(
                                text = "${(exportState.progressFraction * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        LinearProgressIndicator(
                            progress = { exportState.progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }
                }

                Button(
                    onClick = onStartExport,
                    enabled = !exportState.isExporting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("btn_render_export_mp4"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.MovieCreation, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (exportState.isExporting) {
                            "Sedang Merender MP4 (${exportState.currentFrame}/${exportState.totalFrames})..."
                        } else {
                            "Render & Export Video MP4 (1080×1920)"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }

                if (exportState.exportedFilePath != null && !exportState.isExporting) {
                    Surface(
                        color = StatusConnectedGreen.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, StatusConnectedGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = StatusConnectedGreen
                                )
                                Text(
                                    text = exportState.statusText,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onSaveToDevice,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_download_mp4_device")
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Unduh MP4")
                                }

                                OutlinedButton(
                                    onClick = onShareVideo,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_share_mp4_video")
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Bagikan")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecBadgeCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(StudioSurfaceVariant)
            .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary)
        Text(text = value, style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
    }
}
