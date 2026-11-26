package com.cartvr.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LocalGroceryStore
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cartvr.app.data.local.entity.RegularItemEntity
import com.cartvr.app.data.local.entity.ShoppingItemEntity
import com.cartvr.app.data.local.entity.StoreEntity
import com.cartvr.app.data.preferences.ThemeMode
import com.cartvr.app.data.sync.SyncState
import com.cartvr.app.ui.theme.StatusErrorRed
import com.cartvr.app.ui.theme.StatusOfflineAmber
import com.cartvr.app.ui.theme.StatusSyncedGreen
import com.cartvr.app.ui.theme.StatusSyncingBlue

@Composable
fun DashboardScreen(
    displayName: String,
    householdName: String,
    syncState: SyncState,
    themeMode: ThemeMode,
    stores: List<StoreEntity>,
    allItems: List<ShoppingItemEntity>,
    regulars: List<RegularItemEntity>,
    onToggleTheme: (Boolean) -> Unit,
    onSyncClick: () -> Unit,
    onStoreClick: (StoreEntity) -> Unit,
    onQuickAddRegular: (RegularItemEntity) -> Unit
) {
    val isCurrentlyDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val totalCount = allItems.size
    val checkedCount = allItems.count { it.isChecked }
    val remainingCount = totalCount - checkedCount
    val progress = if (totalCount > 0) checkedCount.toFloat() / totalCount.toFloat() else 0f

    val storePairs = stores.chunked(2)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hi, $displayName",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = householdName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SyncStatusPill(syncState = syncState, onClick = onSyncClick)
                    IconButton(onClick = { onToggleTheme(isCurrentlyDark) }) {
                        Icon(
                            imageVector = if (isCurrentlyDark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                            contentDescription = "Toggle theme",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }

        // Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "$remainingCount item${if (remainingCount == 1) "" else "s"} left to buy",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$checkedCount of $totalCount in trolley",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ShoppingCart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        // Quick-Add Regulars Strip
        if (regulars.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Quick-Add Regulars",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(regulars, key = { it.id }) { regular ->
                            AssistChip(
                                onClick = { onQuickAddRegular(regular) },
                                label = {
                                    val labelText = if (!regular.defaultQuantity.isNullOrBlank()) {
                                        "${regular.defaultQuantity} ${regular.name}"
                                    } else {
                                        regular.name
                                    }
                                    Text(labelText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }
        }

        // Store Grid Header
        item {
            Text(
                text = "Your Stores",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // 2-Column Store Cards
        items(storePairs) { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                pair.forEach { store ->
                    val storeRemaining = allItems.count { it.storeId == store.id && !it.isChecked }
                    StoreCard(
                        store = store,
                        remainingCount = storeRemaining,
                        onClick = { onStoreClick(store) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SyncStatusPill(
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

@Composable
private fun StoreCard(
    store: StoreEntity,
    remainingCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = storeIconFor(store.name)
    val accent = storeAccentFor(store.name)

    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accent.copy(alpha = 0.14f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.padding(9.dp)
                    )
                }

                if (remainingCount > 0) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Text(
                            text = remainingCount.toString(),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Column {
                Text(
                    text = store.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (remainingCount == 0) "All done" else "$remainingCount to buy",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun storeIconFor(name: String): ImageVector = when {
    name.contains("Checkers", ignoreCase = true) -> Icons.Rounded.LocalGroceryStore
    name.contains("Woolworths", ignoreCase = true) -> Icons.Rounded.ShoppingBag
    name.contains("Pick n Pay", ignoreCase = true) -> Icons.Rounded.ShoppingCart
    name.contains("Makro", ignoreCase = true) -> Icons.Rounded.Inventory2
    name.contains("Hardware", ignoreCase = true) -> Icons.Rounded.Build
    else -> Icons.Rounded.Storefront
}

private fun storeAccentFor(name: String): Color = when {
    name.contains("Checkers", ignoreCase = true) -> Color(0xFF0D9488)
    name.contains("Woolworths", ignoreCase = true) -> Color(0xFF1F2937)
    name.contains("Pick n Pay", ignoreCase = true) -> Color(0xFF2563EB)
    name.contains("Makro", ignoreCase = true) -> Color(0xFFD97706)
    name.contains("Hardware", ignoreCase = true) -> Color(0xFFDC2626)
    else -> Color(0xFF7C3AED)
}
