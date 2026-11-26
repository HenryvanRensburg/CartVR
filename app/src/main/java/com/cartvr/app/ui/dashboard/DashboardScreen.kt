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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cartvr.app.data.local.entity.ListMemberEntity
import com.cartvr.app.data.local.entity.ShoppingItemEntity
import com.cartvr.app.data.local.entity.ShoppingListEntity
import com.cartvr.app.data.preferences.ThemeMode
import com.cartvr.app.data.sync.SyncState
import com.cartvr.app.ui.components.ListVisuals
import com.cartvr.app.ui.theme.StatusErrorRed
import com.cartvr.app.ui.theme.StatusOfflineAmber
import com.cartvr.app.ui.theme.StatusSyncedGreen
import com.cartvr.app.ui.theme.StatusSyncingBlue

private enum class ListFilter(val label: String) {
    ALL("All"),
    SHARED("Shared"),
    PRIVATE("Private")
}

@Composable
fun DashboardScreen(
    displayName: String,
    currentUserId: String?,
    syncState: SyncState,
    themeMode: ThemeMode,
    lists: List<ShoppingListEntity>,
    listMembers: List<ListMemberEntity>,
    allItems: List<ShoppingItemEntity>,
    onToggleTheme: (Boolean) -> Unit,
    onSyncClick: () -> Unit,
    onOpenJoinDialog: () -> Unit,
    onListClick: (ShoppingListEntity) -> Unit,
    onEditList: (ShoppingListEntity) -> Unit,
    onShareList: (ShoppingListEntity) -> Unit,
    onMakePrivate: (ShoppingListEntity) -> Unit,
    onDeleteList: (ShoppingListEntity) -> Unit,
    onLeaveList: (ShoppingListEntity) -> Unit
) {
    val isCurrentlyDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    var selectedFilter by remember { mutableStateOf(ListFilter.ALL) }
    var confirmDeleteTarget by remember { mutableStateOf<ShoppingListEntity?>(null) }
    var confirmMakePrivateTarget by remember { mutableStateOf<ShoppingListEntity?>(null) }
    var confirmLeaveTarget by remember { mutableStateOf<ShoppingListEntity?>(null) }

    val filteredLists = remember(lists, selectedFilter) {
        when (selectedFilter) {
            ListFilter.ALL -> lists
            ListFilter.SHARED -> lists.filter { it.isShared }
            ListFilter.PRIVATE -> lists.filter { !it.isShared }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Compact Header
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
                        text = "Your shopping lists",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    SyncStatusPill(syncState = syncState, onClick = onSyncClick)
                    IconButton(onClick = onOpenJoinDialog) {
                        Icon(
                            imageVector = Icons.Rounded.GroupAdd,
                            contentDescription = "Join shared list",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
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

        // Filter Chips: All | Shared | Private
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ListFilter.entries.forEach { filter ->
                    val count = when (filter) {
                        ListFilter.ALL -> lists.size
                        ListFilter.SHARED -> lists.count { it.isShared }
                        ListFilter.PRIVATE -> lists.count { !it.isShared }
                    }
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text("${filter.label} ($count)") }
                    )
                }
            }
        }

        if (filteredLists.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = when (selectedFilter) {
                                ListFilter.ALL -> "No shopping lists yet"
                                ListFilter.SHARED -> "No shared lists"
                                ListFilter.PRIVATE -> "No private lists"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap + New List below to create a list, or tap the join icon above to join one via code.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Full-width List Cards
        items(filteredLists, key = { it.id }) { list ->
            val listItems = allItems.filter { it.listId == list.id }
            val remaining = listItems.count { !it.isChecked }
            val completed = listItems.count { it.isChecked }
            val memberCount = listMembers.count { it.listId == list.id }.coerceAtLeast(1)
            val isOwner = list.ownerId == currentUserId

            ShoppingListCard(
                list = list,
                isOwner = isOwner,
                memberCount = memberCount,
                remainingCount = remaining,
                completedCount = completed,
                onClick = { onListClick(list) },
                onRename = { onEditList(list) },
                onShare = { onShareList(list) },
                onRequestMakePrivate = { confirmMakePrivateTarget = list },
                onRequestDelete = { confirmDeleteTarget = list },
                onRequestLeave = { confirmLeaveTarget = list }
            )
        }
    }

    // Delete confirmation
    confirmDeleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmDeleteTarget = null },
            title = { Text("Delete \"${target.name}\"?") },
            text = {
                Text(
                    if (target.isShared) {
                        "This will permanently delete this shared list and all its items for everyone."
                    } else {
                        "This will permanently delete this private list and all its items."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteList(target)
                        confirmDeleteTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Make Private confirmation
    confirmMakePrivateTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmMakePrivateTarget = null },
            title = { Text("Make \"${target.name}\" Private?") },
            text = {
                Text("Other members will lose access and this list will only be visible on your phone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onMakePrivate(target)
                        confirmMakePrivateTarget = null
                    }
                ) {
                    Text("Make Private")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmMakePrivateTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Leave Shared List confirmation
    confirmLeaveTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmLeaveTarget = null },
            title = { Text("Leave \"${target.name}\"?") },
            text = {
                Text("The list will be removed from your phone, but other members will still keep it. You'll need a new invite link to rejoin.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onLeaveList(target)
                        confirmLeaveTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Leave List")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmLeaveTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ShoppingListCard(
    list: ShoppingListEntity,
    isOwner: Boolean,
    memberCount: Int,
    remainingCount: Int,
    completedCount: Int,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onRequestMakePrivate: () -> Unit,
    onRequestDelete: () -> Unit,
    onRequestLeave: () -> Unit
) {
    val accent = ListVisuals.parseColor(list.colorHex)
    val icon = ListVisuals.iconFor(list.iconKey)
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 6.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: High-contrast icon badge
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = accent.copy(alpha = 0.16f),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.padding(11.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Middle: Title + Private/Shared pill
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = list.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VisibilityPill(isShared = list.isShared, memberCount = memberCount)
                    if (completedCount > 0 && remainingCount == 0) {
                        Text(
                            text = "All $completedCount in trolley",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (completedCount > 0) {
                        Text(
                            text = "$completedCount done",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Right: Remaining count badge + overflow menu
            if (remainingCount > 0) {
                Badge(
                    containerColor = accent,
                    contentColor = androidx.compose.ui.graphics.Color.White
                ) {
                    Text(
                        text = remainingCount.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "All done",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(22.dp)
                )
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "List options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    if (isOwner) {
                        DropdownMenuItem(
                            text = { Text("Rename / Style") },
                            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onRename()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (list.isShared) "Send Invite Link" else "Share List") },
                            leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onShare()
                            }
                        )
                        if (list.isShared) {
                            DropdownMenuItem(
                                text = { Text("Make Private") },
                                leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onRequestMakePrivate()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Delete List", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.DeleteOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onRequestDelete()
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Send Invite Link") },
                            leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onShare()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Leave List", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.ExitToApp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onRequestLeave()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VisibilityPill(isShared: Boolean, memberCount: Int) {
    val containerColor = if (isShared) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (isShared) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (isShared) Icons.Rounded.Group else Icons.Rounded.Lock,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = if (isShared) "Shared ($memberCount)" else "Private",
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                fontWeight = FontWeight.Medium
            )
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
