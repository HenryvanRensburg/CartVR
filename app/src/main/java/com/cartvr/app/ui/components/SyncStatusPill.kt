package com.cartvr.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cartvr.app.data.sync.SyncState
import com.cartvr.app.ui.theme.StatusErrorRed
import com.cartvr.app.ui.theme.StatusOfflineAmber
import com.cartvr.app.ui.theme.StatusSyncedGreen
import com.cartvr.app.ui.theme.StatusSyncingBlue

@Composable
fun SyncStatusPill(
    syncState: SyncState,
    onClick: () -> Unit
) {
    val (label, color) = when (syncState) {
        SyncState.SYNCED -> "Synced" to StatusSyncedGreen
        SyncState.SYNCING -> "Syncing" to StatusSyncingBlue
        SyncState.OFFLINE -> "Offline" to StatusOfflineAmber
        SyncState.ERROR -> "Retry" to StatusErrorRed
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.14f),
        modifier = Modifier.clickable(onClick = onClick)
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
                    .background(color)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
