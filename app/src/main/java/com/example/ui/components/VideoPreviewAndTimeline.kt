package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.engine.MotionCanvasRenderer
import com.example.model.MotionProject
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextSecondary
import java.util.Locale

@Composable
fun VideoPreviewMonitorCard(
    project: MotionProject,
    currentTimeSec: Float,
    isPlaying: Boolean,
    selectedSceneIndex: Int,
    onTogglePlay: () -> Unit,
    onStepScene: (Int) -> Unit,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalDuration = project.totalDurationSeconds.toFloat().coerceAtLeast(1f)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = StudioSurface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StudioBorder, RoundedCornerShape(18.dp))
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Monitor Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isPlaying) Color(0xFF10B981) else MaterialTheme.colorScheme.primary)
                    )
                    Text(
                        text = "PREVIEW 9:16 • 1080×1920",
                        style = MaterialTheme.typography.labelMedium,
                        color = StudioTextSecondary
                    )
                }

                Surface(
                    color = StudioSurfaceElevated,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${formatTimecode(currentTimeSec)} / ${formatTimecode(totalDuration)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 9:16 Aspect Ratio Live Motion Canvas
            Box(
                modifier = Modifier
                    .heightIn(min = 240.dp, max = 350.dp)
                    .aspectRatio(9f / 16f)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                    .background(Color(0xFF060911))
                    .clickable { onTogglePlay() }
                    .testTag("video_preview_canvas"),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    MotionCanvasRenderer.renderFrame(
                        canvas = drawContext.canvas.nativeCanvas,
                        width = size.width,
                        height = size.height,
                        project = project,
                        globalTimeSec = currentTimeSec
                    )
                }

                // Subtle Play indicator badge when paused
                if (!isPlaying) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.52f),
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play Preview",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Transport Scrubber Slider
            Slider(
                value = currentTimeSec.coerceIn(0f, totalDuration),
                onValueChange = onSeek,
                valueRange = 0f..totalDuration,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .testTag("preview_scrubber_slider"),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = StudioSurfaceElevated
                )
            )

            // Transport Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Scene ${selectedSceneIndex + 1}/${project.scenes.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = StudioTextSecondary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = { onSeek(0f) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("btn_restart_preview")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = "Restart Video",
                            tint = StudioTextSecondary
                        )
                    }

                    IconButton(
                        onClick = { onStepScene(-1) },
                        enabled = selectedSceneIndex > 0,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("btn_prev_scene")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous Scene",
                            tint = if (selectedSceneIndex > 0) Color.White else StudioTextMuted
                        )
                    }

                    FilledIconButton(
                        onClick = onTogglePlay,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("btn_play_pause"),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play"
                        )
                    }

                    IconButton(
                        onClick = { onStepScene(1) },
                        enabled = selectedSceneIndex < project.scenes.lastIndex,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("btn_next_scene")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Scene",
                            tint = if (selectedSceneIndex < project.scenes.lastIndex) Color.White else StudioTextMuted
                        )
                    }
                }

                Text(
                    text = "30 FPS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
fun MultiTrackTimelineStrip(
    project: MotionProject,
    selectedSceneIndex: Int,
    currentTimeSec: Float,
    onSelectScene: (Int) -> Unit,
    onAddScene: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = StudioSurfaceVariant,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StudioBorder, RoundedCornerShape(16.dp))
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewTimeline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "MULTI-TRACK TIMELINE (${project.totalDurationSeconds}s)",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }

                TextButton(
                    onClick = onAddScene,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 36.dp)
                        .testTag("timeline_add_scene_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Scene",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tambah Scene", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Horizontal Scrollable Scene Blocks + Audio & Motion Sub-Tracks
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                project.scenes.forEachIndexed { index, scene ->
                    val isSelected = index == selectedSceneIndex
                    val borderColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else StudioBorder,
                        label = "sceneBorder"
                    )
                    val cardWidth = (scene.durationSeconds * 16).coerceIn(115, 220).dp

                    Column(
                        modifier = Modifier
                            .width(cardWidth)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) StudioSurfaceElevated else StudioSurface)
                            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(10.dp))
                            .clickable { onSelectScene(index) }
                            .padding(8.dp)
                            .testTag("timeline_scene_card_$index"),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Video / Scene Track Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "S${index + 1} • ${scene.durationSeconds}s",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else StudioTextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = scene.transitionToNext.label.take(6),
                                style = MaterialTheme.typography.labelSmall,
                                color = StudioTextMuted
                            )
                        }

                        Text(
                            text = scene.headlineText.ifBlank { scene.title },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Motion Character + Object mini track pill
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${scene.characterType.label.take(8)} • ${scene.objectType.label.take(8)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1
                            )
                        }

                        // Voice + SFX mini track pill
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "VO + ${scene.sfxOnEnter.label.take(8)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

fun formatTimecode(seconds: Float): String {
    val totalSec = seconds.coerceAtLeast(0f)
    val mins = (totalSec / 60f).toInt()
    val secs = (totalSec % 60f).toInt()
    val tenths = ((totalSec - totalSec.toInt()) * 10f).toInt()
    return String.format(Locale.US, "%02d:%02d.%d", mins, secs, tenths)
}
