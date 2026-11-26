package com.cartvr.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cartvr.app.ui.UpdateUiState

@Composable
fun UpdateDialog(
    state: UpdateUiState,
    onUpdateNow: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    val update = state.availableUpdate ?: return

    AlertDialog(
        onDismissRequest = { if (!state.isDownloading) onDismiss() },
        title = {
            Text(
                text = "Update Available (v${update.versionName})",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = update.releaseNotes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (state.isDownloading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { state.downloadProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Downloading CartVR.apk (${(state.downloadProgress * 100).toInt()}%)...",
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                if (state.needsUnknownSourcesPermission) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Please allow CartVR to install unknown apps in Android Settings, then tap Update Now again.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (!state.errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = state.errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            if (state.needsUnknownSourcesPermission) {
                Button(onClick = onOpenSettings) {
                    Text("Open Settings")
                }
            } else {
                Button(
                    onClick = onUpdateNow,
                    enabled = !state.isDownloading
                ) {
                    Text(if (state.isDownloading) "Downloading..." else "Update Now")
                }
            }
        },
        dismissButton = {
            if (!state.isDownloading) {
                TextButton(onClick = onDismiss) {
                    Text("Later")
                }
            }
        }
    )
}
