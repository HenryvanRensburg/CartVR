package com.cartvr.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.cartvr.app.data.local.entity.CalendarEventEntity
import com.cartvr.app.data.local.entity.HouseholdProfileEntity
import com.cartvr.app.domain.CalendarRecurrence
import com.cartvr.app.ui.components.ListVisuals
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditorSheet(
    editingEvent: CalendarEventEntity?,
    initialDate: LocalDate,
    occurrenceDate: LocalDate,
    profiles: List<HouseholdProfileEntity>,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        location: String?,
        notes: String?,
        date: LocalDate,
        isAllDay: Boolean,
        startTime: LocalTime,
        endTime: LocalTime,
        isShared: Boolean,
        assignedMemberIds: List<String>,
        colorHex: String?,
        recurrenceRule: String?,
        reminderMinutesBefore: Int?
    ) -> Unit,
    onDeleteSeries: (CalendarEventEntity) -> Unit,
    onStopRepeatingFromDate: (CalendarEventEntity, LocalDate) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val zone = remember { ZoneId.systemDefault() }

    val initialZonedStart = remember(editingEvent) {
        editingEvent?.startAt?.let { runCatching { Instant.parse(it).atZone(zone) }.getOrNull() }
    }
    val initialZonedEnd = remember(editingEvent) {
        editingEvent?.endAt?.let { runCatching { Instant.parse(it).atZone(zone) }.getOrNull() }
    }

    var title by remember { mutableStateOf(editingEvent?.title ?: "") }
    var location by remember { mutableStateOf(editingEvent?.location ?: "") }
    var notes by remember { mutableStateOf(editingEvent?.notes ?: "") }
    var eventDate by remember {
        mutableStateOf(initialZonedStart?.toLocalDate() ?: initialDate)
    }
    var isAllDay by remember { mutableStateOf(editingEvent?.isAllDay ?: false) }
    var startTime by remember {
        mutableStateOf(initialZonedStart?.toLocalTime() ?: LocalTime.of(9, 0))
    }
    var endTime by remember {
        mutableStateOf(initialZonedEnd?.toLocalTime() ?: LocalTime.of(10, 0))
    }
    var isShared by remember { mutableStateOf(editingEvent?.isShared ?: true) }
    var selectedMembers by remember {
        mutableStateOf(editingEvent?.let { CalendarVisuals.assignedIds(it) } ?: emptyList())
    }
    var repeatFreq by remember {
        mutableStateOf(CalendarRecurrence.parseFrequency(editingEvent?.recurrenceRule))
    }
    var repeatUntilDate by remember {
        mutableStateOf(CalendarRecurrence.parseUntilDate(editingEvent?.recurrenceRule))
    }
    var reminderMinutes by remember {
        mutableStateOf(editingEvent?.reminderMinutesBefore)
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showUntilPicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault()) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (editingEvent == null) "New Calendar Event" else "Edit Event",
                style = MaterialTheme.typography.titleLarge
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Event title (e.g. Dentist, Braai, Soccer)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            // Shared vs Private
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Visibility",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !isShared,
                        onClick = { isShared = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        icon = { Icon(Icons.Rounded.Lock, null, Modifier.size(18.dp)) }
                    ) { Text("Private") }

                    SegmentedButton(
                        selected = isShared,
                        onClick = { isShared = true },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        icon = { Icon(Icons.Rounded.Group, null, Modifier.size(18.dp)) }
                    ) { Text("Shared") }
                }
            }

            // Member assignment (when Shared)
            if (isShared) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Who is this for?",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedMembers.isEmpty(),
                            onClick = { selectedMembers = emptyList() },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(ListVisuals.parseColor(CalendarVisuals.FAMILY_COLOR_HEX))
                                )
                            },
                            label = { Text("Family") }
                        )

                        profiles.forEach { profile ->
                            val selected = profile.userId in selectedMembers
                            val accent = ListVisuals.parseColor(profile.accentColor)
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    selectedMembers = if (selected) {
                                        selectedMembers - profile.userId
                                    } else {
                                        selectedMembers + profile.userId
                                    }
                                },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(accent)
                                    )
                                },
                                label = { Text(profile.displayName) }
                            )
                        }
                    }
                }
            }

            // Date & All-Day
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.Today, null, Modifier.size(18.dp))
                    Text("  ${eventDate.format(dateFormatter)}")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("All-day", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = isAllDay,
                        onCheckedChange = { isAllDay = it }
                    )
                }
            }

            if (!isAllDay) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showStartTimePicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.Schedule, null, Modifier.size(18.dp))
                        Text(" Start: ${startTime.format(timeFormatter)}")
                    }
                    OutlinedButton(
                        onClick = { showEndTimePicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.Schedule, null, Modifier.size(18.dp))
                        Text(" End: ${endTime.format(timeFormatter)}")
                    }
                }
            }

            // Location
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location (optional)") },
                leadingIcon = { Icon(Icons.Rounded.LocationOn, null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            // Repeat
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Rounded.Repeat, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "Repeat",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        null to "None",
                        CalendarRecurrence.DAILY to "Daily",
                        CalendarRecurrence.WEEKLY to "Weekly",
                        CalendarRecurrence.MONTHLY to "Monthly",
                        CalendarRecurrence.YEARLY to "Yearly"
                    ).forEach { (freq, label) ->
                        FilterChip(
                            selected = repeatFreq == freq,
                            onClick = {
                                repeatFreq = freq
                                if (freq == null) repeatUntilDate = null
                            },
                            label = { Text(label) }
                        )
                    }
                }

                if (repeatFreq != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showUntilPicker = true },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                repeatUntilDate?.let { "Until ${it.format(dateFormatter)}" }
                                    ?: "Add end date (optional)"
                            )
                        }
                        if (repeatUntilDate != null) {
                            IconButton(onClick = { repeatUntilDate = null }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear end date")
                            }
                        }
                    }
                }
            }

            // Reminder Lead Time
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Rounded.Notifications, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "Reminder",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CalendarVisuals.reminderOptions.forEach { (minutes, label) ->
                        FilterChip(
                            selected = reminderMinutes == minutes,
                            onClick = { reminderMinutes = minutes },
                            label = { Text(label) }
                        )
                    }
                }
            }

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (editingEvent != null) {
                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete")
                    }
                }
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onSave(
                                title.trim(),
                                location.trim().ifBlank { null },
                                notes.trim().ifBlank { null },
                                eventDate,
                                isAllDay,
                                startTime,
                                endTime,
                                isShared,
                                selectedMembers,
                                editingEvent?.colorHex,
                                CalendarRecurrence.buildRRule(repeatFreq, repeatUntilDate),
                                reminderMinutes
                            )
                        }
                    },
                    enabled = title.isNotBlank(),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(if (editingEvent == null) "Save Event" else "Save Changes")
                }
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = remember(eventDate) {
            eventDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }
        val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { millis ->
                            eventDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = state) }
    }

    if (showUntilPicker) {
        val initialMillis = remember(repeatUntilDate, eventDate) {
            (repeatUntilDate ?: eventDate.plusMonths(3))
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }
        val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showUntilPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { millis ->
                            repeatUntilDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        }
                        showUntilPicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showUntilPicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = state) }
    }

    if (showStartTimePicker) {
        val state = rememberTimePickerState(
            initialHour = startTime.hour,
            initialMinute = startTime.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showStartTimePicker = false },
            title = { Text("Start Time") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val picked = LocalTime.of(state.hour, state.minute)
                        startTime = picked
                        if (!endTime.isAfter(picked)) {
                            endTime = picked.plusHours(1)
                        }
                        showStartTimePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartTimePicker = false }) { Text("Cancel") }
            }
        )
    }

    if (showEndTimePicker) {
        val state = rememberTimePickerState(
            initialHour = endTime.hour,
            initialMinute = endTime.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showEndTimePicker = false },
            title = { Text("End Time") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(
                    onClick = {
                        endTime = LocalTime.of(state.hour, state.minute)
                        showEndTimePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndTimePicker = false }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteDialog && editingEvent != null) {
        val isRepeating = !editingEvent.recurrenceRule.isNullOrBlank()
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(if (isRepeating) "Delete repeating event?" else "Delete \"${editingEvent.title}\"?")
            },
            text = {
                Text(
                    if (isRepeating) {
                        "Stop repeating from ${occurrenceDate.format(dateFormatter)} onwards, or delete the entire series?"
                    } else {
                        "This event will be removed from the calendar."
                    }
                )
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isRepeating) {
                        TextButton(
                            onClick = {
                                showDeleteDialog = false
                                onStopRepeatingFromDate(editingEvent, occurrenceDate)
                            }
                        ) { Text("This & following") }
                    }
                    Button(
                        onClick = {
                            showDeleteDialog = false
                            onDeleteSeries(editingEvent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text(if (isRepeating) "Entire series" else "Delete") }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}
