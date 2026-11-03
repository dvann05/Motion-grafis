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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.model.ApiConnectionState
import com.example.ui.theme.*
import com.example.viewmodel.StudioUiState

@Composable
fun ApiKeySettingsPanel(
    uiState: StudioUiState,
    onApiKeyInputChange: (String) -> Unit,
    onToggleVisibility: () -> Unit,
    onSaveKey: () -> Unit,
    onTestConnection: () -> Unit,
    onDeleteKey: () -> Unit,
    onSelectAccentTheme: (AccentThemeOption) -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = uiState.apiConnectionState == ApiConnectionState.CONNECTED
    val isTesting = uiState.apiConnectionState == ApiConnectionState.TESTING

    val statusColor = when (uiState.apiConnectionState) {
        ApiConnectionState.CONNECTED -> StatusConnectedGreen
        ApiConnectionState.TESTING -> MaterialTheme.colorScheme.primary
        ApiConnectionState.QUOTA_EXCEEDED -> StatusWarningAmber
        ApiConnectionState.INVALID_KEY, ApiConnectionState.NETWORK_ERROR -> StatusErrorRed
        ApiConnectionState.DISCONNECTED -> StudioTextMuted
    }

    val statusLabel = when (uiState.apiConnectionState) {
        ApiConnectionState.CONNECTED -> "Connected"
        ApiConnectionState.TESTING -> "Testing..."
        ApiConnectionState.INVALID_KEY -> "Disconnected (API Key Invalid)"
        ApiConnectionState.QUOTA_EXCEEDED -> "Disconnected (Quota Exceeded)"
        ApiConnectionState.NETWORK_ERROR -> "Disconnected (Connection Failed)"
        ApiConnectionState.DISCONNECTED -> "Disconnected"
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. API KEY CONFIGURATION CARD
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
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "API CONFIGURATION MANAGER",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                text = "EncryptedSharedPreferences (AES-256 GCM)",
                                style = MaterialTheme.typography.labelSmall,
                                color = StudioTextSecondary
                            )
                        }
                    }

                    // Live Connection Status Pill
                    Surface(
                        color = statusColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .border(1.dp, statusColor.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                            .testTag("api_connection_status_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Text(
                                text = if (isConnected) "Connected" else "Disconnected",
                                style = MaterialTheme.typography.labelMedium,
                                color = statusColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Detailed Status Banner
                Surface(
                    color = StudioSurfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = when (uiState.apiConnectionState) {
                                ApiConnectionState.CONNECTED -> Icons.Default.CheckCircle
                                ApiConnectionState.QUOTA_EXCEEDED -> Icons.Default.Warning
                                ApiConnectionState.INVALID_KEY, ApiConnectionState.NETWORK_ERROR -> Icons.Default.ErrorOutline
                                else -> Icons.Default.Info
                            },
                            contentDescription = null,
                            tint = statusColor
                        )
                        Column {
                            Text(
                                text = "Status: $statusLabel",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                            Text(
                                text = uiState.apiStatusMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = StudioTextSecondary
                            )
                        }
                    }
                }

                // Input Field for Google Gemini API Key + Show/Hide Toggle
                OutlinedTextField(
                    value = uiState.apiKeyInput,
                    onValueChange = onApiKeyInputChange,
                    label = { Text("Google Gemini API Key") },
                    placeholder = { Text("AIzaSy...") },
                    singleLine = true,
                    visualTransformation = if (uiState.isApiKeyVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = onToggleVisibility,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("btn_show_hide_api_key")
                        ) {
                            Icon(
                                imageVector = if (uiState.isApiKeyVisible) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                                contentDescription = if (uiState.isApiKeyVisible) "Hide API Key" else "Show API Key"
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_gemini_api_key")
                )

                // Action Buttons: Save API Key & Test Connection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onSaveKey,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_save_api_key"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save API Key", fontWeight = FontWeight.SemiBold)
                    }

                    FilledTonalButton(
                        onClick = onTestConnection,
                        enabled = !isTesting,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_test_api_connection"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test Connection", fontWeight = FontWeight.SemiBold)
                    }
                }

                // Delete API Key Button
                OutlinedButton(
                    onClick = onDeleteKey,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_delete_api_key"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusErrorRed)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete API Key dari Perangkat")
                }
            }
        }

        // 2. THEME ACCENT CUSTOMIZATION CARD
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ColorLens,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "KUSTOMISASI WARNA AKSEN STUDIO",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = "Tema gelap minimalis dengan pilihan aksen warna personal",
                            style = MaterialTheme.typography.bodySmall,
                            color = StudioTextSecondary
                        )
                    }
                }

                AccentThemeOption.entries.chunked(3).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { option ->
                            val selected = uiState.accentTheme == option
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) StudioSurfaceElevated else StudioSurfaceVariant)
                                    .border(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) option.primary else StudioBorder,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onSelectAccentTheme(option) }
                                    .padding(horizontal = 10.dp, vertical = 10.dp)
                                    .testTag("accent_option_${option.id}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(option.primary)
                                )
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) Color.White else StudioTextSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. USER GUIDE FOR API KEY & SECURITY
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "PETUNJUK KONFIGURASI & KEAMANAN API KEY",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }

                Text(
                    text = "1. Buka Google AI Studio (aistudio.google.com/apikey) lalu buat Google Gemini API Key.\n" +
                        "2. Tempelkan API Key pada kolom di atas atau melalui panel Secrets AI Studio (GEMINI_API_KEY).\n" +
                        "3. Tekan Save API Key untuk mengenkripsi kunci secara lokal menggunakan Android Jetpack EncryptedSharedPreferences (AES-256 GCM).\n" +
                        "4. Tekan Test Connection untuk memverifikasi status koneksi dan kuota Gemini API secara langsung.\n" +
                        "5. API Key tidak pernah ditampilkan di log dan tetap tersimpan permanen meskipun aplikasi ditutup atau diperbarui.",
                    style = MaterialTheme.typography.bodySmall,
                    color = StudioTextSecondary
                )
            }
        }
    }
}
