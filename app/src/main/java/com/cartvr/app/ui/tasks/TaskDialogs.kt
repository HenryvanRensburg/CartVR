package com.cartvr.app.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material.icons.rounded.ViewWeek
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.cartvr.app.data.local.entity.HouseholdProfileEntity
import com.cartvr.app.data.local.entity.ShoppingListEntity
import com.cartvr.app.data.local.entity.TaskEntity
import com.cartvr.app.data.local.entity.TaskListEntity
import com.cartvr.app.data.local.entity.TaskSubtaskEntity
import com.cartvr.app.domain.TaskRecurrence
import com.cartvr.app.ui.components.ListVisuals
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFolderSheet(
    editingFolder: TaskListEntity? = null,
    canEditPrivacy: Boolean = true,
    onDismiss: () -> Unit,
    onSave: (name: String, iconKey: String, colorHex: String, isShared: Boolean, defaultView: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember { mutableStateOf(editingFolder?.name ?: "") }
    var selectedIconKey by remember { mutableStateOf(editingFolder?.iconKey ?: "checklist") }
    var selectedColorHex by remember { mutableStateOf(editingFolder?.colorHex ?: "#0D9488") }
    var isShared by remember { mutableStateOf(editingFolder?.isShared ?: true) }
    var defaultView by remember { mutableStateOf(editingFolder?.defaultView ?: "list") }

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
                text = if (editingFolder == null) "New Task Folder" else "Edit Task Folder",
                style = MaterialTheme.typography.titleLarge
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Folder name (e.g. Garden, School, Car)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            if (canEditPrivacy) {
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
                    Text(
                        text = if (!isShared) {
                            "Only visible on your phone. All tasks & subtasks inside stay private."
                        } else {
                            "Shared across the family. All tasks & subtasks inside stay shared."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Default Layout",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = defaultView == "list",
                        onClick = { defaultView = "list" },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        icon = { Icon(Icons.Rounded.ViewList, null, Modifier.size(18.dp)) }
                    ) { Text("List View") }

                    SegmentedButton(
                        selected = defaultView == "board",
                        onClick = { defaultView = "board" },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        icon = { Icon(Icons.Rounded.ViewWeek, null, Modifier.size(18.dp)) }
                    ) { Text("Project Board") }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Icon",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val accent = ListVisuals.parseColor(selectedColorHex)
                    TaskVisuals.iconOptions.forEach { option ->
                        val selected = option.key == selectedIconKey
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) accent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .size(48.dp)
                                .then(
                                    if (selected) Modifier.border(2.dp, accent, RoundedCornerShape(14.dp))
                                    else Modifier
                                )
                                .clickable { selectedIconKey = option.key }
                        ) {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = option.label,
                                tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(11.dp)
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Color",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ListVisuals.colorHexOptions.forEach { hex ->
                        val color = ListVisuals.parseColor(hex)
                        val selected = hex == selectedColorHex
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColorHex = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { onSave(name.trim(), selectedIconKey, selectedColorHex, isShared, defaultView) },
                enabled = name.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(if (editingFolder == null) "Create Folder" else "Save Folder")
            }
        }
    }
}

private enum class RepeatOption(val label: String) {
    NONE("None"),
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly"),
    AFTER_COMPLETION("After done")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorSheet(
    editingTask: TaskEntity?,
    initialFolderId: String,
    initialStatus: String = "todo",
    folders: List<TaskListEntity>,
    profiles: List<HouseholdProfileEntity>,
    existingSubtasks: List<TaskSubtaskEntity>,
    shoppingLists: List<ShoppingListEntity>,
    onDismiss: () -> Unit,
    onSave: (
        taskListId: String,
        title: String,
        notes: String?,
        assignedTo: String?,
        status: String,
        priority: String,
        dueDate: String?,
        dueTime: String?,
        recurrenceRule: String?,
        initialSubtasks: List<String>
    ) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit,
    onAddSubtask: (TaskEntity, String) -> Unit,
    onToggleSubtask: (TaskSubtaskEntity) -> Unit,
    onDeleteSubtask: (TaskSubtaskEntity) -> Unit,
    onSendToShoppingList: (listId: String, itemName: String, qty: String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf(editingTask?.title ?: "") }
    var notes by remember { mutableStateOf(editingTask?.notes ?: "") }
    var selectedFolderId by remember {
        mutableStateOf(
            editingTask?.taskListId
                ?: folders.firstOrNull { it.id == initialFolderId }?.id
                ?: folders.firstOrNull()?.id
                ?: ""
        )
    }
    var selectedStatus by remember { mutableStateOf(editingTask?.status ?: initialStatus) }
    var selectedAssignee by remember { mutableStateOf(editingTask?.assignedTo) }
    var selectedPriority by remember { mutableStateOf(editingTask?.priority ?: "normal") }
    var dueDate by remember { mutableStateOf(editingTask?.dueDate) }
    var dueTime by remember { mutableStateOf(editingTask?.dueTime) }

    val initialAfterDays = remember(editingTask) {
        TaskRecurrence.parseAfterCompletionDays(editingTask?.recurrenceRule)
    }
    var repeatOption by remember {
        mutableStateOf(
            when {
                editingTask?.recurrenceRule.isNullOrBlank() -> RepeatOption.NONE
                initialAfterDays != null -> RepeatOption.AFTER_COMPLETION
                editingTask?.recurrenceRule == TaskRecurrence.DAILY -> RepeatOption.DAILY
                editingTask?.recurrenceRule == TaskRecurrence.WEEKLY -> RepeatOption.WEEKLY
                editingTask?.recurrenceRule == TaskRecurrence.MONTHLY -> RepeatOption.MONTHLY
                editingTask?.recurrenceRule == TaskRecurrence.YEARLY -> RepeatOption.YEARLY
                else -> RepeatOption.NONE
            }
        )
    }
    var afterDaysText by remember { mutableStateOf((initialAfterDays ?: 14).toString()) }

    val draftSubtasks = remember { mutableStateListOf<String>() }
    var newSubtaskText by remember { mutableStateOf("") }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showSendToShopping by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val selectedFolder = folders.firstOrNull { it.id == selectedFolderId }
    val today = remember { LocalDate.now() }

    fun resolvedRecurrenceRule(): String? = when (repeatOption) {
        RepeatOption.NONE -> null
        RepeatOption.DAILY -> TaskRecurrence.DAILY
        RepeatOption.WEEKLY -> TaskRecurrence.WEEKLY
        RepeatOption.MONTHLY -> TaskRecurrence.MONTHLY
        RepeatOption.YEARLY -> TaskRecurrence.YEARLY
        RepeatOption.AFTER_COMPLETION -> {
            val days = afterDaysText.trim().toIntOrNull()?.coerceIn(1, 3650) ?: 14
            TaskRecurrence.afterCompletionRule(days)
        }
    }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editingTask == null) "New Task" else "Edit Task",
                    style = MaterialTheme.typography.titleLarge
                )
                if (selectedFolder != null) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (selectedFolder.isShared) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (selectedFolder.isShared) Icons.Rounded.Group else Icons.Rounded.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (selectedFolder.isShared) "Shared" else "Private",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("What needs to get done?") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            // Folder picker
            if (folders.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Folder",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        folders.forEach { folder ->
                            FilterChip(
                                selected = folder.id == selectedFolderId,
                                onClick = { selectedFolderId = folder.id },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (folder.isShared) TaskVisuals.iconFor(folder.iconKey) else Icons.Rounded.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text(folder.name) }
                            )
                        }
                    }
                }
            }

            // Board Status
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Status",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val statuses = listOf("todo" to "To Do", "in_progress" to "In Progress", "done" to "Done")
                    statuses.forEachIndexed { idx, (key, label) ->
                        SegmentedButton(
                            selected = selectedStatus == key,
                            onClick = { selectedStatus = key },
                            shape = SegmentedButtonDefaults.itemShape(index = idx, count = statuses.size)
                        ) { Text(label) }
                    }
                }
            }

            // Assignee
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Assigned To",
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
                        selected = selectedAssignee == null,
                        onClick = { selectedAssignee = null },
                        label = { Text("Anyone") }
                    )
                    profiles.forEach { profile ->
                        val accent = ListVisuals.parseColor(profile.accentColor)
                        FilterChip(
                            selected = selectedAssignee == profile.userId,
                            onClick = { selectedAssignee = profile.userId },
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

            // Priority
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Priority",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("low" to "Low", "normal" to "Normal", "urgent" to "Urgent").forEach { (key, label) ->
                        FilterChip(
                            selected = selectedPriority == key,
                            onClick = { selectedPriority = key },
                            label = { Text(label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Due Date & Optional Time
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Due Date & Time",
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
                        selected = dueDate == null,
                        onClick = {
                            dueDate = null
                            dueTime = null
                        },
                        label = { Text("No due date") }
                    )
                    FilterChip(
                        selected = dueDate == today.toString(),
                        onClick = { dueDate = today.toString() },
                        label = { Text("Today") }
                    )
                    FilterChip(
                        selected = dueDate == today.plusDays(1).toString(),
                        onClick = { dueDate = today.plusDays(1).toString() },
                        label = { Text("Tomorrow") }
                    )
                    FilterChip(
                        selected = dueDate == today.plusWeeks(1).toString(),
                        onClick = { dueDate = today.plusWeeks(1).toString() },
                        label = { Text("Next week") }
                    )
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Today, null, Modifier.size(16.dp))
                        val customLabel = dueDate?.let {
                            TaskVisuals.formatDueLabel(it, null, isDone = true)?.first ?: it
                        } ?: "Pick date"
                        Text(" $customLabel")
                    }
                }

                if (dueDate != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showTimePicker = true },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.Schedule, null, Modifier.size(16.dp))
                            Text(if (dueTime != null) " $dueTime" else " Add optional time")
                        }
                        if (dueTime != null) {
                            IconButton(onClick = { dueTime = null }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear time")
                            }
                        }
                    }
                }
            }

            // Smart Repeating Rules
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Repeat",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RepeatOption.entries.forEach { opt ->
                        FilterChip(
                            selected = repeatOption == opt,
                            onClick = { repeatOption = opt },
                            label = { Text(opt.label) }
                        )
                    }
                }

                if (repeatOption == RepeatOption.AFTER_COMPLETION) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(7, 14, 30).forEach { preset ->
                            FilterChip(
                                selected = afterDaysText.trim() == preset.toString(),
                                onClick = { afterDaysText = preset.toString() },
                                label = { Text("${preset}d") }
                            )
                        }
                        OutlinedTextField(
                            value = afterDaysText,
                            onValueChange = { raw ->
                                afterDaysText = raw.filter { it.isDigit() }.take(4)
                            },
                            label = { Text("Days") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(96.dp),
                            shape = RoundedCornerShape(12.dp)
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

            // Subtasks Checklist
            HorizontalDivider()
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Subtasks",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (editingTask != null) {
                    existingSubtasks.forEach { sub ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = sub.isCompleted,
                                onCheckedChange = { onToggleSubtask(sub) }
                            )
                            Text(
                                text = sub.title,
                                style = MaterialTheme.typography.bodyMedium,
                                textDecoration = if (sub.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                color = if (sub.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { onDeleteSubtask(sub) }) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "Remove subtask",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                } else {
                    draftSubtasks.forEachIndexed { index, subTitle ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "•  $subTitle",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp)
                            )
                            IconButton(onClick = { draftSubtasks.removeAt(index) }) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "Remove subtask",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newSubtaskText,
                        onValueChange = { newSubtaskText = it },
                        placeholder = { Text("Add subtask step...") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                val clean = newSubtaskText.trim()
                                if (clean.isNotEmpty()) {
                                    if (editingTask != null) {
                                        onAddSubtask(editingTask, clean)
                                    } else {
                                        draftSubtasks.add(clean)
                                    }
                                    newSubtaskText = ""
                                }
                            }
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    IconButton(
                        onClick = {
                            val clean = newSubtaskText.trim()
                            if (clean.isNotEmpty()) {
                                if (editingTask != null) {
                                    onAddSubtask(editingTask, clean)
                                } else {
                                    draftSubtasks.add(clean)
                                }
                                newSubtaskText = ""
                            }
                        }
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add subtask")
                    }
                }
            }

            // Shopping List Shortcut
            if (shoppingLists.isNotEmpty()) {
                FilledTonalButton(
                    onClick = { showSendToShopping = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  Send Item to Shopping List")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (editingTask != null) {
                    OutlinedButton(
                        onClick = { confirmDelete = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete")
                    }
                }
                Button(
                    onClick = {
                        if (selectedFolderId.isNotBlank() && title.isNotBlank()) {
                            onSave(
                                selectedFolderId,
                                title.trim(),
                                notes.trim().ifBlank { null },
                                selectedAssignee,
                                selectedStatus,
                                selectedPriority,
                                dueDate,
                                dueTime,
                                resolvedRecurrenceRule(),
                                draftSubtasks.toList()
                            )
                        }
                    },
                    enabled = title.isNotBlank() && selectedFolderId.isNotBlank(),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(if (editingTask == null) "Add Task" else "Save Changes")
                }
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = remember(dueDate) {
            dueDate?.let {
                runCatching { LocalDate.parse(it).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }.getOrNull()
            } ?: LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            dueDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                                .toString()
                        }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val parts = dueTime?.split(":")
        val initialHour = parts?.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 9
        val initialMin = parts?.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        val timeState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMin,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Pick Due Time") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        dueTime = String.format(Locale.US, "%02d:%02d", timeState.hour, timeState.minute)
                        showTimePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }

    if (showSendToShopping && shoppingLists.isNotEmpty()) {
        SendToShoppingDialog(
            initialItemName = title.ifBlank { editingTask?.title ?: "" },
            shoppingLists = shoppingLists,
            onDismiss = { showSendToShopping = false },
            onSend = { listId, itemName, qty ->
                onSendToShoppingList(listId, itemName, qty)
                showSendToShopping = false
            }
        )
    }

    if (confirmDelete && editingTask != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete \"${editingTask.title}\"?") },
            text = { Text("This task and its subtasks will be removed.") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDelete = false
                        onDeleteTask(editingTask)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun SendToShoppingDialog(
    initialItemName: String,
    shoppingLists: List<ShoppingListEntity>,
    onDismiss: () -> Unit,
    onSend: (listId: String, itemName: String, qty: String?) -> Unit
) {
    var itemName by remember { mutableStateOf(initialItemName) }
    var quantity by remember { mutableStateOf("") }
    var selectedListId by remember {
        mutableStateOf(
            shoppingLists.firstOrNull { it.isShared }?.id
                ?: shoppingLists.firstOrNull()?.id
                ?: ""
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Send to Shopping List") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("Item needed (e.g. Outdoor LED bulb)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Text(
                    text = "Choose list",
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
                onClick = { onSend(selectedListId, itemName.trim(), quantity.trim().ifBlank { null }) },
                enabled = itemName.isNotBlank() && selectedListId.isNotBlank()
            ) {
                Text("Add to List")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
