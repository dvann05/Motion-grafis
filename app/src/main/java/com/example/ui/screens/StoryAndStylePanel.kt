package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.example.viewmodel.StudioUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StoryAndStylePanel(
    uiState: StudioUiState,
    onUpdateTitle: (String) -> Unit,
    onUpdatePrompt: (String) -> Unit,
    onSelectTheme: (StoryTheme) -> Unit,
    onSelectLanguage: (StoryLanguage) -> Unit,
    onSelectDuration: (Int) -> Unit,
    onSelectStyle: (AnimationStyle) -> Unit,
    onSparkIdea: () -> Unit,
    onGenerateScenes: () -> Unit,
    onOpenSceneEditor: (Int) -> Unit,
    onOpenApiSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val project = uiState.project

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // API Connection Quick Status Card
        Surface(
            color = StudioSurface,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StudioBorder, RoundedCornerShape(14.dp))
                .clickable { onOpenApiSettings() }
                .testTag("dashboard_api_status_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (uiState.apiConnectionState == ApiConnectionState.CONNECTED || uiState.hasSavedApiKey) {
                            Icons.Default.VerifiedUser
                        } else {
                            Icons.Default.Key
                        },
                        contentDescription = null,
                        tint = if (uiState.apiConnectionState == ApiConnectionState.CONNECTED) {
                            StatusConnectedGreen
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                    Column {
                        Text(
                            text = if (uiState.apiConnectionState == ApiConnectionState.CONNECTED) {
                                "Gemini AI Engine: Connected"
                            } else if (uiState.hasSavedApiKey) {
                                "Gemini API Key Tersimpan (Siap Digunakan)"
                            } else {
                                "Konfigurasi Gemini API Key"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White
                        )
                        Text(
                            text = uiState.apiStatusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = StudioTextSecondary,
                            maxLines = 2
                        )
                    }
                }
                TextButton(onClick = onOpenApiSettings) {
                    Text("Settings", color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // 1. STORY GENERATOR CARD
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "AI STORY & SCRIPT GENERATOR",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }

                    FilledTonalButton(
                        onClick = onSparkIdea,
                        enabled = !uiState.isGeneratingIdea,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_spark_story_idea")
                    ) {
                        if (uiState.isGeneratingIdea) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ide AI", style = MaterialTheme.typography.labelLarge)
                    }
                }

                OutlinedTextField(
                    value = project.title,
                    onValueChange = onUpdateTitle,
                    label = { Text("Judul Proyek Video") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_project_title")
                )

                OutlinedTextField(
                    value = project.storyPrompt,
                    onValueChange = onUpdatePrompt,
                    label = { Text("Masukkan Ide Cerita / Topik Video") },
                    placeholder = { Text("Contoh: Ceritakan bagaimana AI membantu kreator membuat video animasi...") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_story_prompt")
                )

                // Pilihan Bahasa (Indonesia & Inggris)
                Text(
                    text = "Bahasa Naskah & Suara (Language)",
                    style = MaterialTheme.typography.labelLarge,
                    color = StudioTextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StoryLanguage.entries.forEach { lang ->
                        val selected = project.language == lang
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectLanguage(lang) },
                            label = { Text(lang.displayName) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Translate,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chip_lang_${lang.code}")
                        )
                    }
                }

                // Pilihan Durasi (30, 45, atau 60 detik)
                Text(
                    text = "Target Durasi Video",
                    style = MaterialTheme.typography.labelLarge,
                    color = StudioTextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(30, 45, 60).forEach { seconds ->
                        val selected = project.targetDurationSeconds == seconds
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectDuration(seconds) },
                            label = { Text("$seconds Detik") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chip_duration_$seconds")
                        )
                    }
                }

                // Pilihan Tema Cerita
                Text(
                    text = "Pilih Tema Cerita",
                    style = MaterialTheme.typography.labelLarge,
                    color = StudioTextSecondary
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StoryTheme.entries.forEach { theme ->
                        val selected = project.theme == theme
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectTheme(theme) },
                            label = {
                                Text(
                                    if (project.language == StoryLanguage.INDONESIAN) theme.labelId else theme.labelEn
                                )
                            },
                            modifier = Modifier.testTag("chip_theme_${theme.id}")
                        )
                    }
                }

                Button(
                    onClick = onGenerateScenes,
                    enabled = !uiState.isGeneratingStory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_generate_story_scenes"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (uiState.isGeneratingStory) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Merancang Naskah & Scene dengan AI...")
                    } else {
                        Icon(imageVector = Icons.Default.MovieFilter, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Generate Naskah & Scene Otomatis (AI)",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2. ANIMATION STYLE GENERATOR CARD (6 Styles)
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
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "ANIMATION STYLE GENERATOR (6 GAYA)",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }

                AnimationStyle.entries.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pair.forEach { style ->
                            val isSelected = project.animationStyle == style
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) StudioSurfaceElevated else StudioSurfaceVariant)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else StudioBorder,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onSelectStyle(style) }
                                    .padding(10.dp)
                                    .testTag("style_card_${style.id}"),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = style.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = style.badgeTag,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = style.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StudioTextSecondary,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. GENERATED SCENE STORYBOARD BREAKDOWN
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
                    Text(
                        text = "DAFTAR SCENE & NASKAH (${project.scenes.size} ADEGAN)",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = "Total ${project.totalDurationSeconds}s",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                project.scenes.forEachIndexed { idx, scene ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(StudioSurfaceVariant)
                            .border(
                                1.dp,
                                if (idx == uiState.selectedSceneIndex) MaterialTheme.colorScheme.primary else StudioBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { onOpenSceneEditor(idx) }
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SCENE ${idx + 1} (${scene.durationSeconds}s)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "• ${scene.characterType.label} (${scene.characterMotion.label})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StudioTextSecondary
                                )
                            }
                            Text(
                                text = scene.headlineText,
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White
                            )
                            Text(
                                text = "\"${scene.narrationText}\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = StudioTextSecondary
                            )
                        }
                        IconButton(onClick = { onOpenSceneEditor(idx) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Scene",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
