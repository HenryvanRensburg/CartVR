package com.cartvr.app.ui.home

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cartvr.app.data.local.entity.CalendarEventEntity
import com.cartvr.app.data.local.entity.HouseholdProfileEntity
import com.cartvr.app.data.local.entity.ShoppingItemEntity
import com.cartvr.app.data.local.entity.ShoppingListEntity
import com.cartvr.app.data.local.entity.TaskEntity
import com.cartvr.app.data.local.entity.TaskListEntity
import com.cartvr.app.data.preferences.ThemeMode
import com.cartvr.app.data.sync.SyncState
import com.cartvr.app.domain.CalendarRecurrence
import com.cartvr.app.ui.calendar.CalendarVisuals
import com.cartvr.app.ui.components.ListVisuals
import com.cartvr.app.ui.components.SyncStatusPill
import com.cartvr.app.ui.tasks.TaskVisuals
import com.cartvr.app.ui.tasks.sortTasksSmartly
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun FamilyDashboardScreen(
    displayName: String,
    syncState: SyncState,
    themeMode: ThemeMode,
    lists: List<ShoppingListEntity>,
    allShoppingItems: List<ShoppingItemEntity>,
    taskLists: List<TaskListEntity>,
    tasks: List<TaskEntity>,
    calendarEvents: List<CalendarEventEntity>,
    profiles: List<HouseholdProfileEntity>,
    onToggleTheme: (Boolean) -> Unit,
    onSyncClick: () -> Unit,
    onOpenShoppingTab: () -> Unit,
    onOpenShoppingList: (ShoppingListEntity) -> Unit,
    onOpenTasksTab: () -> Unit,
    onToggleTaskDone: (TaskEntity) -> Unit,
    onOpenTask: (TaskEntity) -> Unit,
    onOpenCalendarTab: () -> Unit,
    onOpenEvent: (CalendarEventEntity, LocalDate) -> Unit
) {
    val isCurrentlyDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val today = remember { LocalDate.now() }
    val todayStr = remember(today) { today.toString() }
    val headerDate = remember(today) {
        today.format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault()))
    }

    val dueTodayOrOverdue = remember(tasks, todayStr) {
        sortTasksSmartly(
            tasks.filter {
                it.status != "done" && !it.dueDate.isNullOrBlank() && it.dueDate <= todayStr
            }
        )
    }
    val overdueCount = remember(dueTodayOrOverdue, todayStr) {
        dueTodayOrOverdue.count { (it.dueDate ?: "") < todayStr }
    }
    val taskFolderMap = remember(taskLists) { taskLists.associateBy { it.id } }

    val upcomingEvents = remember(calendarEvents, today) {
        CalendarRecurrence.expandEventsInRange(calendarEvents, today, today.plusDays(7))
    }

    val activeListsSorted = remember(lists) { lists.sortedBy { it.name.lowercase() } }
    val remainingByList = remember(allShoppingItems) {
        allShoppingItems.filter { !it.isChecked }.groupBy { it.listId }.mapValues { it.value.size }
    }
    val totalToBuy = remember(remainingByList) { remainingByList.values.sum() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Bar: Greeting, Sync Pill, Light/Dark Toggle
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
                        text = headerDate,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

        // Card 1: Today's & Overdue Tasks
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                imageVector = Icons.Rounded.TaskAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Today's Tasks",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (overdueCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Text(
                                        text = "$overdueCount overdue",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        TextButton(onClick = onOpenTasksTab) {
                            Text("See all")
                        }
                    }

                    if (dueTodayOrOverdue.isEmpty()) {
                        Text(
                            text = "All caught up for today! Tap + to add a task.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        dueTodayOrOverdue.take(5).forEachIndexed { idx, task ->
                            if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            val dueInfo = TaskVisuals.formatDueLabel(task.dueDate, task.dueTime, isDone = false)
                            val folderName = taskFolderMap[task.taskListId]?.name
                            val assigneeColor = TaskVisuals.memberColor(task.assignedTo, profiles)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenTask(task) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = false,
                                    onCheckedChange = { onToggleTaskDone(task) }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = task.title,
                                            style = MaterialTheme.typography.bodyLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (task.priority == "urgent") {
                                            Icon(
                                                Icons.Rounded.Flag,
                                                contentDescription = "Urgent",
                                                tint = TaskVisuals.priorityColor("urgent"),
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (dueInfo != null) {
                                            Text(
                                                text = dueInfo.first,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (dueInfo.second) {
                                                    MaterialTheme.colorScheme.error
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                        }
                                        if (!folderName.isNullOrBlank()) {
                                            Text(
                                                text = "• $folderName",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (task.assignedTo != null) {
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .clip(CircleShape)
                                                    .background(assigneeColor)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (dueTodayOrOverdue.size > 5) {
                            Text(
                                text = "+ ${dueTodayOrOverdue.size - 5} more due tasks",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable(onClick = onOpenTasksTab)
                            )
                        }
                    }
                }
            }
        }

        // Card 2: Today's & Upcoming Calendar Events
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                imageVector = Icons.Rounded.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Upcoming Calendar",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        TextButton(onClick = onOpenCalendarTab) {
                            Text("See all")
                        }
                    }

                    if (upcomingEvents.isEmpty()) {
                        Text(
                            text = "No events in the next 7 days.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val dayFmt = remember { DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()) }
                        upcomingEvents.take(4).forEach { occ ->
                            val accent = CalendarVisuals.resolveEventColor(occ.event, profiles)
                            val assignee = CalendarVisuals.resolveAssigneeLabel(occ.event, profiles)
                            val dayText = when (occ.occurrenceDate) {
                                today -> "Today"
                                today.plusDays(1) -> "Tomorrow"
                                else -> occ.occurrenceDate.format(dayFmt)
                            }
                            val timeText = CalendarVisuals.formatEventTimeRange(occ)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onOpenEvent(occ.event, occ.occurrenceDate) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(accent)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = occ.event.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "$dayText • $timeText",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = accent.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = assignee,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = accent,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Card 3: Shopping Overview & Quick Store Shortcuts
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                imageVector = Icons.Rounded.ShoppingCart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "Shopping Overview",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (totalToBuy == 0) {
                                        "Nothing left to buy"
                                    } else {
                                        "$totalToBuy item${if (totalToBuy == 1) "" else "s"} left across ${activeListsSorted.size} list${if (activeListsSorted.size == 1) "" else "s"}"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        TextButton(onClick = onOpenShoppingTab) {
                            Text("See all")
                        }
                    }

                    if (activeListsSorted.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            activeListsSorted.forEach { list ->
                                val remaining = remainingByList[list.id] ?: 0
                                val accent = ListVisuals.parseColor(list.colorHex)
                                val icon = ListVisuals.iconFor(list.iconKey)

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = accent.copy(alpha = 0.12f),
                                    modifier = Modifier.clickable { onOpenShoppingList(list) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = accent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = list.name,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (remaining > 0) {
                                            Badge(containerColor = accent, contentColor = Color.White) {
                                                Text(
                                                    text = remaining.toString(),
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickAddShoppingItemDialog(
    shoppingLists: List<ShoppingListEntity>,
    onDismiss: () -> Unit,
    onAdd: (listId: String, rawInput: String) -> Unit
) {
    var rawText by remember { mutableStateOf("") }
    var selectedListId by remember {
        mutableStateOf(
            shoppingLists.firstOrNull { it.isShared }?.id
                ?: shoppingLists.firstOrNull()?.id
                ?: ""
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick Add Shopping Item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    label = { Text("Item (e.g. 2L Milk, Woolies coffee)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (rawText.isNotBlank() && selectedListId.isNotBlank()) {
                                onAdd(selectedListId, rawText.trim())
                            }
                        }
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Text(
                    text = "Shopping list",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    shoppingLists.sortedBy { it.name.lowercase() }.forEach { list ->
                        FilterChip(
                            selected = list.id == selectedListId,
                            onClick = { selectedListId = list.id },
                            label = { Text(list.name) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(selectedListId, rawText.trim()) },
                enabled = rawText.isNotBlank() && selectedListId.isNotBlank()
            ) {
                Text("Add Item")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
