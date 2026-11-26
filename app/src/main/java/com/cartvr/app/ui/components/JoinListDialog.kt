package com.cartvr.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun JoinListDialog(
    isJoining: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onJoin: (String) -> Unit
) {
    var rawInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isJoining) onDismiss() },
        title = { Text("Join Shared List") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Paste the invite link or 6-character code sent to you on WhatsApp or SMS.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = rawInput,
                    onValueChange = { rawInput = it },
                    label = { Text("Invite code or link") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val extracted = extractShareCode(rawInput)
                    onJoin(extracted)
                },
                enabled = rawInput.isNotBlank() && !isJoining
            ) {
                Text(if (isJoining) "Joining..." else "Join List")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isJoining) {
                Text("Cancel")
            }
        }
    )
}

fun extractShareCode(input: String): String {
    val trimmed = input.trim()
    val codeParamMatch = Regex("""[?&]code=([A-Za-z0-9]+)""").find(trimmed)
    if (codeParamMatch != null) return codeParamMatch.groupValues[1].uppercase()
    val pathMatch = Regex("""/join/([A-Za-z0-9]+)""").find(trimmed)
    if (pathMatch != null) return pathMatch.groupValues[1].uppercase()
    return trimmed.uppercase()
}
