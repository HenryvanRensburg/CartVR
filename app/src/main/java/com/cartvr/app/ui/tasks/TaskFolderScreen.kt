package com.cartvr.app.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material.icons.rounded.ViewWeek
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.cartvr.app.ui.components.ListVisuals

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFolderScreen(
    folder: TaskListEntity,
    tasks: List<TaskEntity>,
    subtasks: List<TaskSubtaskEntity>,
    profiles: List<HouseholdProfileEntity>,
    onBack: () -> Unit,
    onToggleViewMode: (String) -> Unit,
    onEditFolder: () -> Unit,
    onDeleteFolder: () -> Unit,
    onCreateTask: (initialStatus: String) -> Unit,
    onEditTask: (TaskEntity) -> Unit,
    onSetTaskStatus: (TaskEntity, String) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var confirmDeleteFolder by remember { mutableStateOf(false) }
    var showCompletedInList by remember { mutableStateOf(false) }

    val accent = ListVisuals.parseColor(folder.colorHex)
    val subtasksByTask = remember(subtasks) { subtasks.groupBy { it.taskId } }
    val openTasks = remember(tasks) { sortTasksSmartly(tasks.filter { it.status != "done" }) }
    val doneTasks = remember(tasks) { tasks.filter { it.status == "done" }.sortedByDescending { it.completedAt ?: it.updatedAt } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = folder.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (folder.isShared) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
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
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "Folder menu")
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
                                    onEditFolder()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Folder", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(Icons.Rounded.DeleteOutline, null, tint = MaterialTheme.colorScheme.error)
                                },
                                onClick = {
                                    menuExpanded = false
                                    confirmDeleteFolder = true
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onCreateTask("todo") },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("New Task") },
                containerColor = accent,
                contentColor = Color.White
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // List <-> Project Board View Toggle
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                SegmentedButton(
                    selected = folder.defaultView != "board",
                    onClick = { onToggleViewMode("list") },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = { Icon(Icons.Rounded.ViewList, null, Modifier.size(18.dp)) }
                ) { Text("List View") }

                SegmentedButton(
                    selected = folder.defaultView == "board",
                    onClick = { onToggleViewMode("board") },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = { Icon(Icons.Rounded.ViewWeek, null, Modifier.size(18.dp)) }
                ) { Text("Project Board") }
            }

            if (folder.defaultView == "board") {
                val todoTasks = remember(tasks) { sortTasksSmartly(tasks.filter { it.status == "todo" }) }
                val inProgressTasks = remember(tasks) { sortTasksSmartly(tasks.filter { it.status == "in_progress" }) }

                LazyRow(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        BoardColumn(
                            title = "To Do",
                            dotColor = accent,
                            tasks = todoTasks,
                            subtasksByTask = subtasksByTask,
                            profiles = profiles,
                            onAddClick = { onCreateTask("todo") },
                            onTaskClick = onEditTask,
                            onSetStatus = onSetTaskStatus
                        )
                    }
                    item {
                        BoardColumn(
                            title = "In Progress",
                            dotColor = Color(0xFFD97706),
                            tasks = inProgressTasks,
                            subtasksByTask = subtasksByTask,
                            profiles = profiles,
                            onAddClick = { onCreateTask("in_progress") },
                            onTaskClick = onEditTask,
                            onSetStatus = onSetTaskStatus
                        )
                    }
                    item {
                        BoardColumn(
                            title = "Done",
                            dotColor = Color(0xFF10B981),
                            tasks = doneTasks,
                            subtasksByTask = subtasksByTask,
                            profiles = profiles,
                            onAddClick = null,
                            onTaskClick = onEditTask,
                            onSetStatus = onSetTaskStatus
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (openTasks.isEmpty() && doneTasks.isEmpty()) {
                        item {
                            EmptyTasksCard(
                                title = "No tasks in ${folder.name}",
                                subtitle = "Tap + New Task below to add your first item."
                            )
                        }
                    }

                    items(openTasks, key = { it.id }) { task ->
                        TaskRowCard(
                            task = task,
                            folderName = null,
                            profiles = profiles,
                            subtasks = subtasksByTask[task.id].orEmpty(),
                            onToggleDone = { onSetTaskStatus(task, "done") },
                            onClick = { onEditTask(task) }
                        )
                    }

                    if (doneTasks.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showCompletedInList = !showCompletedInList }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Completed (${doneTasks.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Icon(
                                    imageVector = if (showCompletedInList) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (showCompletedInList) {
                            items(doneTasks, key = { it.id }) { task ->
                                TaskRowCard(
                                    task = task,
                                    folderName = null,
                                    profiles = profiles,
                                    subtasks = subtasksByTask[task.id].orEmpty(),
                                    onToggleDone = { onSetTaskStatus(task, "todo") },
                                    onClick = { onEditTask(task) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmDeleteFolder) {
        AlertDialog(
            onDismissRequest = { confirmDeleteFolder = false },
            title = { Text("Delete \"${folder.name}\"?") },
            text = { Text("This will delete this folder and all tasks inside it.") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDeleteFolder = false
                        onDeleteFolder()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteFolder = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun BoardColumn(
    title: String,
    dotColor: Color,
    tasks: List<TaskEntity>,
    subtasksByTask: Map<String, List<TaskSubtaskEntity>>,
    profiles: List<HouseholdProfileEntity>,
    onAddClick: (() -> Unit)?,
    onTaskClick: (TaskEntity) -> Unit,
    onSetStatus: (TaskEntity, String) -> Unit
) {
    Surface(
        modifier = Modifier
            .width(292.dp)
            .fillMaxHeight(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Badge(
                    containerColor = dotColor.copy(alpha = 0.18f),
                    contentColor = dotColor
                ) {
                    Text(
                        text = tasks.size.toString(),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    BoardTaskCard(
                        task = task,
                        subtasks = subtasksByTask[task.id].orEmpty(),
                        profiles = profiles,
                        onClick = { onTaskClick(task) },
                        onSetStatus = onSetStatus
                    )
                }

                if (onAddClick != null) {
                    item {
                        FilledTonalButton(
                            onClick = onAddClick,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Rounded.Add, null, Modifier.size(18.dp))
                            Text(" Add card")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoardTaskCard(
    task: TaskEntity,
    subtasks: List<TaskSubtaskEntity>,
    profiles: List<HouseholdProfileEntity>,
    onClick: () -> Unit,
    onSetStatus: (TaskEntity, String) -> Unit
) {
    val isDone = task.status == "done"
    val dueInfo = remember(task.dueDate, task.dueTime, isDone) {
        TaskVisuals.formatDueLabel(task.dueDate, task.dueTime, isDone)
    }
    val assigneeColor = TaskVisuals.memberColor(task.assignedTo, profiles)
    val assigneeName = TaskVisuals.memberName(task.assignedTo, profiles)
    val doneSubs = subtasks.count { it.isCompleted }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (task.priority == "urgent" && !isDone) {
                    Icon(
                        imageVector = Icons.Rounded.Flag,
                        contentDescription = "Urgent",
                        tint = TaskVisuals.priorityColor("urgent"),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (dueInfo != null || task.assignedTo != null || subtasks.isNotEmpty() || !task.recurrenceRule.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
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
                            Text(
                                text = assigneeName,
                                style = MaterialTheme.typography.labelSmall,
                                color = assigneeColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (subtasks.isNotEmpty()) {
                        Text(
                            text = "$doneSubs/${subtasks.size}",
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

            // One-tap status moves
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                when (task.status) {
                    "todo" -> {
                        TextButton(
                            onClick = { onSetStatus(task, "in_progress") },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, null, Modifier.size(15.dp))
                            Text(" Start", style = MaterialTheme.typography.labelMedium)
                        }
                        TextButton(
                            onClick = { onSetStatus(task, "done") },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Rounded.Check, null, Modifier.size(15.dp))
                            Text(" Done", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    "in_progress" -> {
                        TextButton(
                            onClick = { onSetStatus(task, "todo") },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Rounded.Replay, null, Modifier.size(15.dp))
                            Text(" To Do", style = MaterialTheme.typography.labelMedium)
                        }
                        TextButton(
                            onClick = { onSetStatus(task, "done") },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Rounded.Check, null, Modifier.size(15.dp))
                            Text(" Done", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    else -> {
                        TextButton(
                            onClick = { onSetStatus(task, "todo") },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Rounded.Replay, null, Modifier.size(15.dp))
                            Text(" Reopen", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}
