package com.cartvr.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cartvr.app.data.local.entity.CalendarEventEntity
import com.cartvr.app.data.local.entity.HouseholdProfileEntity
import com.cartvr.app.data.local.entity.TaskEntity
import com.cartvr.app.data.local.entity.TaskListEntity
import com.cartvr.app.data.local.entity.TaskSubtaskEntity
import com.cartvr.app.data.preferences.ThemeMode
import com.cartvr.app.data.sync.SyncState
import com.cartvr.app.domain.CalendarEventOccurrence
import com.cartvr.app.domain.CalendarRecurrence
import com.cartvr.app.ui.components.ListVisuals
import com.cartvr.app.ui.components.SyncStatusPill
import com.cartvr.app.ui.tasks.EmptyTasksCard
import com.cartvr.app.ui.tasks.TaskRowCard
import com.cartvr.app.ui.tasks.TaskVisuals
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

enum class CalendarViewMode(val label: String) {
    MONTH("Month"),
    WEEK("Week"),
    AGENDA("Agenda")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    syncState: SyncState,
    themeMode: ThemeMode,
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    events: List<CalendarEventEntity>,
    tasks: List<TaskEntity>,
    taskLists: List<TaskListEntity>,
    subtasks: List<TaskSubtaskEntity>,
    profiles: List<HouseholdProfileEntity>,
    onToggleTheme: (Boolean) -> Unit,
    onSyncClick: () -> Unit,
    onEventClick: (CalendarEventEntity, LocalDate) -> Unit,
    onTaskClick: (TaskEntity) -> Unit,
    onToggleTaskDone: (TaskEntity) -> Unit
) {
    val isCurrentlyDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    var viewMode by rememberSaveable { mutableStateOf(CalendarViewMode.MONTH) }
    var showTasks by rememberSaveable { mutableStateOf(true) }

    val today = remember { LocalDate.now() }
    val selectedMonth = remember(selectedDate) { YearMonth.from(selectedDate) }
    val weekStart = remember(selectedDate) {
        selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }

    val (rangeStart, rangeEnd) = remember(viewMode, selectedMonth, weekStart, today) {
        when (viewMode) {
            CalendarViewMode.MONTH -> selectedMonth.atDay(1) to selectedMonth.atEndOfMonth()
            CalendarViewMode.WEEK -> weekStart to weekStart.plusDays(6)
            CalendarViewMode.AGENDA -> today to today.plusDays(30)
        }
    }

    val occurrencesInRange = remember(events, rangeStart, rangeEnd) {
        CalendarRecurrence.expandEventsInRange(events, rangeStart, rangeEnd)
    }
    val occurrencesByDate = remember(occurrencesInRange) {
        occurrencesInRange.groupBy { it.occurrenceDate }
    }

    val tasksByDate = remember(tasks, showTasks, rangeStart, rangeEnd) {
        if (!showTasks) {
            emptyMap()
        } else {
            tasks.filter { task ->
                val d = task.dueDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                d != null && !d.isBefore(rangeStart) && !d.isAfter(rangeEnd)
            }.groupBy { LocalDate.parse(it.dueDate!!) }
        }
    }

    val folderMap = remember(taskLists) { taskLists.associateBy { it.id } }
    val subtasksByTask = remember(subtasks) { subtasks.groupBy { it.taskId } }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Calendar",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = selectedDate.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault())),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                SyncStatusPill(syncState = syncState, onClick = onSyncClick)
                TextButton(
                    onClick = { onSelectDate(today) },
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text("Today", fontWeight = FontWeight.SemiBold)
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

        // Month | Week | Agenda Switcher
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            CalendarViewMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = viewMode == mode,
                    onClick = { viewMode = mode },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = CalendarViewMode.entries.size)
                ) {
                    Text(mode.label)
                }
            }
        }

        // Show/Hide Tasks chip + Member color legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilterChip(
                selected = showTasks,
                onClick = { showTasks = !showTasks },
                leadingIcon = {
                    Icon(Icons.Rounded.TaskAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                label = { Text(if (showTasks) "Tasks shown" else "Tasks hidden") }
            )

            profiles.forEach { profile ->
                val color = ListVisuals.parseColor(profile.accentColor)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Text(
                        text = profile.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(ListVisuals.parseColor(CalendarVisuals.FAMILY_COLOR_HEX))
                )
                Text(
                    text = "Family",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        when (viewMode) {
            CalendarViewMode.MONTH -> {
                val monthLabel = remember(selectedMonth) {
                    selectedMonth.atDay(1).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
                }
                val dayEvents = occurrencesByDate[selectedDate].orEmpty()
                val dayTasks = tasksByDate[selectedDate].orEmpty()

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                // Month navigation row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { onSelectDate(selectedMonth.minusMonths(1).atDay(1)) }) {
                                        Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous month")
                                    }
                                    Text(
                                        text = monthLabel,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    IconButton(onClick = { onSelectDate(selectedMonth.plusMonths(1).atDay(1)) }) {
                                        Icon(Icons.Rounded.ChevronRight, contentDescription = "Next month")
                                    }
                                }

                                // Mon - Sun header
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    listOf("M", "T", "W", "T", "F", "S", "S").forEach { dayName ->
                                        Text(
                                            text = dayName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                val firstDayOffset = selectedMonth.atDay(1).dayOfWeek.value - 1
                                val totalDays = selectedMonth.lengthOfMonth()
                                val totalCells = ((firstDayOffset + totalDays + 6) / 7) * 7

                                for (rowStart in 0 until totalCells step 7) {
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        for (col in 0..6) {
                                            val cellIndex = rowStart + col
                                            val dayNum = cellIndex - firstDayOffset + 1
                                            if (dayNum in 1..totalDays) {
                                                val date = selectedMonth.atDay(dayNum)
                                                val isSelected = date == selectedDate
                                                val isToday = date == today
                                                val dotColors = buildDotColorsForDay(
                                                    events = occurrencesByDate[date].orEmpty(),
                                                    tasks = tasksByDate[date].orEmpty(),
                                                    profiles = profiles
                                                )

                                                DayGridCell(
                                                    dayNumber = dayNum,
                                                    isSelected = isSelected,
                                                    isToday = isToday,
                                                    dotColors = dotColors,
                                                    onClick = { onSelectDate(date) },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            } else {
                                                Spacer(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(44.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        val headerText = if (selectedDate == today) {
                            "Today • ${selectedDate.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))}"
                        } else {
                            selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault()))
                        }
                        Text(
                            text = headerText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    if (dayEvents.isEmpty() && dayTasks.isEmpty()) {
                        item {
                            EmptyTasksCard(
                                title = "Nothing scheduled",
                                subtitle = "Tap + New Event below to add an event on this day."
                            )
                        }
                    } else {
                        items(dayEvents, key = { "ev_${it.event.id}_${it.occurrenceDate}" }) { occ ->
                            CalendarEventCard(
                                occurrence = occ,
                                profiles = profiles,
                                onClick = { onEventClick(occ.event, occ.occurrenceDate) }
                            )
                        }
                        items(dayTasks, key = { "task_${it.id}" }) { task ->
                            TaskRowCard(
                                task = task,
                                folderName = folderMap[task.taskListId]?.name,
                                profiles = profiles,
                                subtasks = subtasksByTask[task.id].orEmpty(),
                                onToggleDone = { onToggleTaskDone(task) },
                                onClick = { onTaskClick(task) }
                            )
                        }
                    }
                }
            }

            CalendarViewMode.WEEK -> {
                val weekEnd = weekStart.plusDays(6)
                val rangeLabel = remember(weekStart, weekEnd) {
                    val fmt = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
                    "${weekStart.format(fmt)} – ${weekEnd.format(fmt)}"
                }
                val weekDays = remember(weekStart) { (0..6).map { weekStart.plusDays(it.toLong()) } }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { onSelectDate(weekStart.minusWeeks(1)) }) {
                                        Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous week")
                                    }
                                    Text(
                                        text = rangeLabel,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    IconButton(onClick = { onSelectDate(weekStart.plusWeeks(1)) }) {
                                        Icon(Icons.Rounded.ChevronRight, contentDescription = "Next week")
                                    }
                                }

                                Row(modifier = Modifier.fillMaxWidth()) {
                                    weekDays.forEach { date ->
                                        val dotColors = buildDotColorsForDay(
                                            events = occurrencesByDate[date].orEmpty(),
                                            tasks = tasksByDate[date].orEmpty(),
                                            profiles = profiles
                                        )
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = date.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault())),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            DayGridCell(
                                                dayNumber = date.dayOfMonth,
                                                isSelected = date == selectedDate,
                                                isToday = date == today,
                                                dotColors = dotColors,
                                                onClick = { onSelectDate(date) },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    weekDays.forEach { date ->
                        val dayEvents = occurrencesByDate[date].orEmpty()
                        val dayTasks = tasksByDate[date].orEmpty()
                        if (dayEvents.isNotEmpty() || dayTasks.isNotEmpty() || date == selectedDate) {
                            item(key = "hdr_$date") {
                                Text(
                                    text = formatDayHeading(date, today),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (date == selectedDate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            if (dayEvents.isEmpty() && dayTasks.isEmpty()) {
                                item(key = "empty_$date") {
                                    Text(
                                        text = "Nothing scheduled",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                items(dayEvents, key = { "wk_ev_${it.event.id}_${it.occurrenceDate}" }) { occ ->
                                    CalendarEventCard(
                                        occurrence = occ,
                                        profiles = profiles,
                                        onClick = { onEventClick(occ.event, occ.occurrenceDate) }
                                    )
                                }
                                items(dayTasks, key = { "wk_task_${it.id}" }) { task ->
                                    TaskRowCard(
                                        task = task,
                                        folderName = folderMap[task.taskListId]?.name,
                                        profiles = profiles,
                                        subtasks = subtasksByTask[task.id].orEmpty(),
                                        onToggleDone = { onToggleTaskDone(task) },
                                        onClick = { onTaskClick(task) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            CalendarViewMode.AGENDA -> {
                val agendaDates = remember(occurrencesByDate, tasksByDate, today) {
                    (occurrencesByDate.keys + tasksByDate.keys + today).distinct().sorted()
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    agendaDates.forEach { date ->
                        val dayEvents = occurrencesByDate[date].orEmpty()
                        val dayTasks = tasksByDate[date].orEmpty()
                        item(key = "ag_hdr_$date") {
                            Text(
                                text = formatDayHeading(date, today),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (date == today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                        if (dayEvents.isEmpty() && dayTasks.isEmpty()) {
                            item(key = "ag_empty_$date") {
                                EmptyTasksCard(
                                    title = "Free day today",
                                    subtitle = "No events or due tasks scheduled for today."
                                )
                            }
                        } else {
                            items(dayEvents, key = { "ag_ev_${it.event.id}_${it.occurrenceDate}" }) { occ ->
                                CalendarEventCard(
                                    occurrence = occ,
                                    profiles = profiles,
                                    onClick = { onEventClick(occ.event, occ.occurrenceDate) }
                                )
                            }
                            items(dayTasks, key = { "ag_task_${it.id}" }) { task ->
                                TaskRowCard(
                                    task = task,
                                    folderName = folderMap[task.taskListId]?.name,
                                    profiles = profiles,
                                    subtasks = subtasksByTask[task.id].orEmpty(),
                                    onToggleDone = { onToggleTaskDone(task) },
                                    onClick = { onTaskClick(task) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayGridCell(
    dayNumber: Int,
    isSelected: Boolean,
    isToday: Boolean,
    dotColors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .then(
                    when {
                        isSelected -> Modifier.background(MaterialTheme.colorScheme.primary)
                        isToday -> Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        else -> Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = dayNumber.toString(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    isToday -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(6.dp)
        ) {
            dotColors.take(3).forEach { color ->
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
    }
}

@Composable
fun CalendarEventCard(
    occurrence: CalendarEventOccurrence,
    profiles: List<HouseholdProfileEntity>,
    onClick: () -> Unit
) {
    val event = occurrence.event
    val accent = CalendarVisuals.resolveEventColor(event, profiles)
    val assigneeLabel = CalendarVisuals.resolveAssigneeLabel(event, profiles)
    val timeRange = CalendarVisuals.formatEventTimeRange(occurrence)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(accent)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (!event.recurrenceRule.isNullOrBlank()) {
                        Icon(
                            imageVector = Icons.Rounded.Repeat,
                            contentDescription = "Repeating",
                            tint = accent,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = timeRange,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = accent.copy(alpha = 0.16f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (event.isShared) Icons.Rounded.Group else Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = assigneeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = accent
                            )
                        }
                    }

                    if (!event.location.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = event.location,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun buildDotColorsForDay(
    events: List<CalendarEventOccurrence>,
    tasks: List<TaskEntity>,
    profiles: List<HouseholdProfileEntity>
): List<Color> = buildList {
    events.forEach { add(CalendarVisuals.resolveEventColor(it.event, profiles)) }
    tasks.filter { it.status != "done" }.forEach { add(TaskVisuals.memberColor(it.assignedTo, profiles)) }
}

private fun formatDayHeading(date: LocalDate, today: LocalDate): String = when (date) {
    today -> "Today • ${date.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()))}"
    today.plusDays(1) -> "Tomorrow • ${date.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()))}"
    else -> date.format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault()))
}
