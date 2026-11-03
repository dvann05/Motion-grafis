package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SceneEditorPanel(
    scene: MotionScene,
    sceneIndex: Int,
    totalScenes: Int,
    onUpdateScene: ((MotionScene) -> MotionScene) -> Unit,
    onAddScene: () -> Unit,
    onDuplicateScene: () -> Unit,
    onDeleteScene: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Scene Management Bar
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
                    Column {
                        Text(
                            text = "EDITING SCENE ${sceneIndex + 1} OF $totalScenes",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = scene.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilledTonalIconButton(
                            onClick = onAddScene,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("scene_editor_add_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Tambah Scene")
                        }
                        FilledTonalIconButton(
                            onClick = onDuplicateScene,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("scene_editor_dup_btn")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Duplikat Scene")
                        }
                        FilledTonalIconButton(
                            onClick = onDeleteScene,
                            enabled = totalScenes > 1,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("scene_editor_del_btn")
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus Scene")
                        }
                    }
                }

                // Duration Slider (3s - 15s)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Durasi Scene",
                        style = MaterialTheme.typography.labelLarge,
                        color = StudioTextSecondary
                    )
                    Text(
                        text = "${scene.durationSeconds} Detik",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = scene.durationSeconds.toFloat(),
                    onValueChange = { v ->
                        onUpdateScene { it.copy(durationSeconds = v.roundToInt().coerceIn(3, 15)) }
                    },
                    valueRange = 3f..15f,
                    steps = 11,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("slider_scene_duration")
                )
            }
        }

        // Text, Narration & Animation Card
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
                        imageVector = Icons.Default.Title,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "TEKS ANIMASI & NARASI SCENE",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }

                OutlinedTextField(
                    value = scene.headlineText,
                    onValueChange = { txt -> onUpdateScene { it.copy(headlineText = txt) } },
                    label = { Text("Teks Utama (Kinetic Headline)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_scene_headline")
                )

                OutlinedTextField(
                    value = scene.narrationText,
                    onValueChange = { txt -> onUpdateScene { it.copy(narrationText = txt) } },
                    label = { Text("Naskah Narasi Suara & Subtitle Otomatis") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_scene_narration")
                )

                Text(
                    text = "Animasi Masuk Teks (Text Entrance)",
                    style = MaterialTheme.typography.labelLarge,
                    color = StudioTextSecondary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TextAnimIn.entries.forEach { animIn ->
                        FilterChip(
                            selected = scene.textAnimIn == animIn,
                            onClick = { onUpdateScene { it.copy(textAnimIn = animIn) } },
                            label = { Text(animIn.label) }
                        )
                    }
                }

                Text(
                    text = "Animasi Keluar Teks (Text Exit)",
                    style = MaterialTheme.typography.labelLarge,
                    color = StudioTextSecondary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TextAnimOut.entries.forEach { animOut ->
                        FilterChip(
                            selected = scene.textAnimOut == animOut,
                            onClick = { onUpdateScene { it.copy(textAnimOut = animOut) } },
                            label = { Text(animOut.label) }
                        )
                    }
                }
            }
        }

        // Character & Motion Card
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
                        imageVector = Icons.Default.DirectionsRun,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "KARAKTER & OBJEK BERGERAK",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }

                Text(
                    text = "Pilih Karakter 2D",
                    style = MaterialTheme.typography.labelLarge,
                    color = StudioTextSecondary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CharacterType.entries.forEach { charType ->
                        FilterChip(
                            selected = scene.characterType == charType,
                            onClick = { onUpdateScene { it.copy(characterType = charType) } },
                            label = { Text(charType.label) }
                        )
                    }
                }

                Text(
                    text = "Gerakan Karakter (Character Motion)",
                    style = MaterialTheme.typography.labelLarge,
                    color = StudioTextSecondary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CharacterMotion.entries.forEach { motion ->
                        FilterChip(
                            selected = scene.characterMotion == motion,
                            onClick = { onUpdateScene { it.copy(characterMotion = motion) } },
                            label = { Text(motion.label) }
                        )
                    }
                }

                HorizontalDivider(color = StudioBorder)

                Text(
                    text = "Pilih Objek Bergerak (Motion Props)",
                    style = MaterialTheme.typography.labelLarge,
                    color = StudioTextSecondary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MovingObjectType.entries.forEach { obj ->
                        FilterChip(
                            selected = scene.objectType == obj,
                            onClick = { onUpdateScene { it.copy(objectType = obj) } },
                            label = { Text(obj.label) }
                        )
                    }
                }

                Text(
                    text = "Lintasan Gerak Objek",
                    style = MaterialTheme.typography.labelLarge,
                    color = StudioTextSecondary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ObjectMotion.entries.forEach { om ->
                        FilterChip(
                            selected = scene.objectMotion == om,
                            onClick = { onUpdateScene { it.copy(objectMotion = om) } },
                            label = { Text(om.label) }
                        )
                    }
                }
            }
        }

        // Background & Scene Transition Card
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
                        imageVector = Icons.Default.Landscape,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "BACKGROUND & TRANSISI ANTAR-SCENE",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }

                Text(
                    text = "Background Environment",
                    style = MaterialTheme.typography.labelLarge,
                    color = StudioTextSecondary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BackgroundPreset.entries.forEach { bg ->
                        FilterChip(
                            selected = scene.backgroundPreset == bg,
                            onClick = {
                                onUpdateScene {
                                    it.copy(
                                        backgroundPreset = bg,
                                        primaryColorHex = bg.secondaryHex
                                    )
                                }
                            },
                            label = { Text(bg.label) }
                        )
                    }
                }

                // Quick Primary Accent Palette Swatches for Scene
                Text(
                    text = "Warna Aksen Visual Scene",
                    style = MaterialTheme.typography.labelLarge,
                    color = StudioTextSecondary
                )
                val colorHexOptions = listOf("#00E5FF", "#8B5CF6", "#10B981", "#F43F5E", "#F59E0B", "#EC4899")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    colorHexOptions.forEach { hex ->
                        val parsed = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = scene.primaryColorHex.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(parsed)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) Color.White else StudioBorder,
                                    shape = CircleShape
                                )
                                .clickable {
                                    onUpdateScene { it.copy(primaryColorHex = hex) }
                                }
                        )
                    }
                }

                Text(
                    text = "Transisi ke Scene Berikutnya",
                    style = MaterialTheme.typography.labelLarge,
                    color = StudioTextSecondary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SceneTransition.entries.forEach { trans ->
                        FilterChip(
                            selected = scene.transitionToNext == trans,
                            onClick = { onUpdateScene { it.copy(transitionToNext = trans) } },
                            label = { Text(trans.label) },
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    fontWeight: FontWeight = FontWeight.Normal
) {
    androidx.compose.material3.FilterChip(
        selected = selected,
        onClick = onClick,
        label = label
    )
}
