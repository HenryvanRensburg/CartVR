package com.cartvr.app.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material.icons.rounded.ViewWeek
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cartvr.app.data.local.entity.HouseholdProfileEntity
import com.cartvr.app.data.local.entity.TaskEntity
import com.cartvr.app.data.local.entity.TaskListEntity
import com.cartvr.app.data.local.entity.TaskSubtaskEntity
import com.cartvr.app.data.preferences.ThemeMode
import com.cartvr.app.data.sync.SyncState
import com.cartvr.app.ui.components.ListVisuals
import com.cartvr.app.ui.components.SyncStatusPill
import java.time.LocalDate

enum class TaskSmartFilter(val label: String) {
    FOLDERS("Folders"),
    TODAY("Today"),
    UPCOMING("Upcoming"),
    ASSIGNED_TO_ME("Assigned to Me"),
    ALL_SHARED("All Shared"),
    PRIVATE("Private")
}

@Composable
fun TasksHubScreen(
    displayName: String,
    currentUserId: String?,
    syncState: SyncState,
    themeMode: ThemeMode,
    folders: List<TaskListEntity>,
    tasks: List<TaskEntity>,
    subtasks: List<TaskSubtaskEntity>,
    profiles: List<HouseholdProfileEntity>,
    onToggleTheme: (Boolean) -> Unit,
    onSyncClick: () -> Unit,
    onFolderClick: (TaskListEntity) -> Unit,
    onCreateFolder: () -> Unit,
    onEditFolder: (TaskListEntity) -> Unit,
    onToggleFolderPrivacy: (TaskListEntity) -> Unit,
    onDeleteFolder: (TaskListEntity) -> Unit,
    onToggleTaskDone: (TaskEntity) -> Unit,
    onTaskClick: (TaskEntity) -> Unit
) {
    val isCurrentlyDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    var selectedFilter by rememberSaveable { mutableStateOf(TaskSmartFilter.FOLDERS) }
    var confirmDeleteFolder by remember { mutableStateOf<TaskListEntity?>(null) }

    val todayStr = remember { LocalDate.now().toString() }
    val openTasks = remember(tasks) { tasks.filter { it.status != "done" } }
    val overdueCount = remember(openTasks, todayStr) {
        openTasks.count { !it.dueDate.isNullOrBlank() && it.dueDate < todayStr }
    }

    val folderMap = remember(folders) { folders.associateBy { it.id } }
    val subtasksByTask = remember(subtasks) { subtasks.groupBy { it.taskId } }

    val filteredTasks = remember(openTasks, selectedFilter, currentUserId, todayStr) {
        val list = when (selectedFilter) {
            TaskSmartFilter.FOLDERS -> emptyList()
            TaskSmartFilter.TODAY -> openTasks.filter { !it.dueDate.isNullOrBlank() && it.dueDate <= todayStr }
            TaskSmartFilter.UPCOMING -> openTasks.filter { !it.dueDate.isNullOrBlank() && it.dueDate > todayStr }
            TaskSmartFilter.ASSIGNED_TO_ME -> openTasks.filter { it.assignedTo == currentUserId }
            TaskSmartFilter.ALL_SHARED -> openTasks.filter { it.isShared }
            TaskSmartFilter.PRIVATE -> openTasks.filter { !it.isShared }
        }
        sortTasksSmartly(list)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tasks",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = buildString {
                            append("${openTasks.size} open")
                            if (overdueCount > 0) append(" • $overdueCount overdue")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (overdueCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
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

        // Smart Filter Pills
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaskSmartFilter.entries.forEach { filter ->
                    val count = when (filter) {
                        TaskSmartFilter.FOLDERS -> folders.size
                        TaskSmartFilter.TODAY -> openTasks.count { !it.dueDate.isNullOrBlank() && it.dueDate <= todayStr }
                        TaskSmartFilter.UPCOMING -> openTasks.count { !it.dueDate.isNullOrBlank() && it.dueDate > todayStr }
                        TaskSmartFilter.ASSIGNED_TO_ME -> openTasks.count { it.assignedTo == currentUserId }
                        TaskSmartFilter.ALL_SHARED -> openTasks.count { it.isShared }
                        TaskSmartFilter.PRIVATE -> openTasks.count { !it.isShared }
                    }
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text("${filter.label} ($count)") }
                    )
                }
            }
        }

        if (selectedFilter == TaskSmartFilter.FOLDERS) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Task Folders",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = onCreateFolder,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(" New Folder")
                    }
                }
            }

            if (folders.isEmpty()) {
                item {
                    EmptyTasksCard(
                        title = "No task folders yet",
                        subtitle = "Tap + New Folder to create one for home chores, admin, or personal tasks."
                    )
                }
            } else {
                items(folders, key = { it.id }) { folder ->
                    val folderTasks = tasks.filter { it.taskListId == folder.id }
                    val openCount = folderTasks.count { it.status != "done" }
                    val inProgressCount = folderTasks.count { it.status == "in_progress" }

                    TaskFolderCard(
                        folder = folder,
                        openCount = openCount,
                        inProgressCount = inProgressCount,
                        canEditPrivacy = folder.ownerId == currentUserId,
                        onClick = { onFolderClick(folder) },
                        onEdit = { onEditFolder(folder) },
                        onTogglePrivacy = { onToggleFolderPrivacy(folder) },
                        onDelete = { confirmDeleteFolder = folder }
                    )
                }
            }
        } else {
            if (filteredTasks.isEmpty()) {
                item {
                    EmptyTasksCard(
                        title = "Nothing in ${selectedFilter.label}",
                        subtitle = "Tap + New Task below or pick another filter."
                    )
                }
            } else {
                items(filteredTasks, key = { it.id }) { task ->
                    val folder = folderMap[task.taskListId]
                    val taskSubs = subtasksByTask[task.id].orEmpty()
                    TaskRowCard(
                        task = task,
                        folderName = folder?.name,
                        profiles = profiles,
                        subtasks = taskSubs,
                        onToggleDone = { onToggleTaskDone(task) },
                        onClick = { onTaskClick(task) }
                    )
                }
            }
        }
    }

    confirmDeleteFolder?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmDeleteFolder = null },
            title = { Text("Delete \"${target.name}\"?") },
            text = { Text("This will remove this folder and all tasks inside it.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteFolder(target)
                        confirmDeleteFolder = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteFolder = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun TaskFolderCard(
    folder: TaskListEntity,
    openCount: Int,
    inProgressCount: Int,
    canEditPrivacy: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onTogglePrivacy: () -> Unit,
    onDelete: () -> Unit
) {
    val accent = ListVisuals.parseColor(folder.colorHex)
    val icon = TaskVisuals.iconFor(folder.iconKey)
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

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (folder.isShared) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (folder.isShared) Icons.Rounded.Group else Icons.Rounded.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = if (folder.isShared) "Shared" else "Private",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (folder.defaultView == "board") Icons.Rounded.ViewWeek else Icons.Rounded.ViewList,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = if (folder.defaultView == "board") "Board" else "List",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    if (inProgressCount > 0) {
                        Text(
                            text = "$inProgressCount active",
                            style = MaterialTheme.typography.labelSmall,
                            color = accent
                        )
                    }
                }
            }

            if (openCount > 0) {
                Badge(containerColor = accent, contentColor = Color.White) {
                    Text(
                        text = openCount.toString(),
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
                        contentDescription = "Folder options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit Folder") },
                        leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        }
                    )
                    if (canEditPrivacy) {
                        DropdownMenuItem(
                            text = { Text(if (folder.isShared) "Make Private" else "Share with Family") },
                            leadingIcon = {
                                Icon(if (folder.isShared) Icons.Rounded.Lock else Icons.Rounded.Group, null)
                            },
                            onClick = {
                                menuExpanded = false
                                onTogglePrivacy()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Delete Folder", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(Icons.Rounded.DeleteOutline, null, tint = MaterialTheme.colorScheme.error)
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TaskRowCard(
    task: TaskEntity,
    folderName: String?,
    profiles: List<HouseholdProfileEntity>,
    subtasks: List<TaskSubtaskEntity>,
    onToggleDone: () -> Unit,
    onClick: () -> Unit
) {
    val isDone = task.status == "done"
    val dueInfo = remember(task.dueDate, task.dueTime, isDone) {
        TaskVisuals.formatDueLabel(task.dueDate, task.dueTime, isDone)
    }
    val assigneeName = TaskVisuals.memberName(task.assignedTo, profiles)
    val assigneeColor = TaskVisuals.memberColor(task.assignedTo, profiles)
    val priorityColor = TaskVisuals.priorityColor(task.priority)
    val doneSubs = subtasks.count { it.isCompleted }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDone) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDone) 0.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isDone,
                onCheckedChange = { onToggleDone() }
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (task.priority == "urgent" && !isDone) {
                        Icon(
                            imageVector = Icons.Rounded.Flag,
                            contentDescription = "Urgent",
                            tint = priorityColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (task.status == "in_progress") {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = "In Progress",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (!folderName.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = folderName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (dueInfo != null) {
                        val (label, overdue) = dueInfo
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (overdue) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (overdue) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (task.assignedTo != null) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = assigneeColor.copy(alpha = 0.16f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(assigneeColor)
                                )
                                Text(
                                    text = assigneeName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = assigneeColor
                                )
                            }
                        }
                    }

                    if (subtasks.isNotEmpty()) {
                        Text(
                            text = "Checklist $doneSubs/${subtasks.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (!task.recurrenceRule.isNullOrBlank()) {
                        Icon(
                            imageVector = Icons.Rounded.Repeat,
                            contentDescription = "Repeating",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyTasksCard(title: String, subtitle: String) {
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
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun sortTasksSmartly(tasks: List<TaskEntity>): List<TaskEntity> {
    val priorityWeight = mapOf("urgent" to 0, "normal" to 1, "low" to 2)
    return tasks.sortedWith(
        compareBy<TaskEntity>(
            { it.dueDate ?: "9999-12-31" },
            { priorityWeight[it.priority] ?: 1 },
            { it.title.lowercase() }
        )
    )
}
