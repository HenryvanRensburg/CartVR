package com.cartvr.app.data.remote.dto

import com.cartvr.app.data.local.entity.CalendarEventEntity
import com.cartvr.app.data.local.entity.HouseholdProfileEntity
import com.cartvr.app.data.local.entity.SyncStatus
import com.cartvr.app.data.local.entity.TaskEntity
import com.cartvr.app.data.local.entity.TaskListEntity
import com.cartvr.app.data.local.entity.TaskSubtaskEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TaskListDto(
    val id: String,
    @SerialName("household_id") val householdId: String,
    @SerialName("owner_id") val ownerId: String,
    val name: String,
    @SerialName("icon_key") val iconKey: String = "checklist",
    @SerialName("color_hex") val colorHex: String = "#0D9488",
    @SerialName("is_shared") val isShared: Boolean = true,
    @SerialName("default_view") val defaultView: String = "list",
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null
)

@Serializable
data class TaskDto(
    val id: String,
    @SerialName("household_id") val householdId: String,
    @SerialName("task_list_id") val taskListId: String,
    @SerialName("owner_id") val ownerId: String,
    @SerialName("assigned_to") val assignedTo: String? = null,
    val title: String,
    val notes: String? = null,
    val status: String = "todo",
    val priority: String = "normal",
    @SerialName("is_shared") val isShared: Boolean = true,
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("due_time") val dueTime: String? = null,
    @SerialName("reminder_minutes_before") val reminderMinutesBefore: Int? = null,
    @SerialName("recurrence_rule") val recurrenceRule: String? = null,
    @SerialName("recurrence_source_id") val recurrenceSourceId: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("updated_by") val updatedBy: String? = null,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null
)

@Serializable
data class TaskSubtaskDto(
    val id: String,
    @SerialName("task_id") val taskId: String,
    @SerialName("household_id") val householdId: String,
    @SerialName("owner_id") val ownerId: String,
    @SerialName("is_shared") val isShared: Boolean = true,
    val title: String,
    @SerialName("is_completed") val isCompleted: Boolean = false,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null
)

@Serializable
data class CalendarEventDto(
    val id: String,
    @SerialName("household_id") val householdId: String,
    @SerialName("owner_id") val ownerId: String,
    val title: String,
    val location: String? = null,
    val notes: String? = null,
    @SerialName("start_at") val startAt: String,
    @SerialName("end_at") val endAt: String,
    @SerialName("is_all_day") val isAllDay: Boolean = false,
    @SerialName("is_shared") val isShared: Boolean = true,
    @SerialName("assigned_member_ids") val assignedMemberIds: List<String> = emptyList(),
    @SerialName("color_hex") val colorHex: String? = null,
    @SerialName("recurrence_rule") val recurrenceRule: String? = null,
    @SerialName("reminder_minutes_before") val reminderMinutesBefore: Int? = null,
    @SerialName("external_provider") val externalProvider: String? = null,
    @SerialName("external_calendar_id") val externalCalendarId: String? = null,
    @SerialName("external_event_id") val externalEventId: String? = null,
    @SerialName("external_etag") val externalEtag: String? = null,
    @SerialName("updated_by") val updatedBy: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null
)

fun ProfileDto.toEntity() = HouseholdProfileEntity(
    userId = userId,
    displayName = displayName,
    email = email,
    accentColor = accentColor
)

fun TaskListDto.toEntity() = TaskListEntity(
    id = id,
    householdId = householdId,
    ownerId = ownerId,
    name = name,
    iconKey = iconKey,
    colorHex = colorHex,
    isShared = isShared,
    defaultView = defaultView,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncStatus = SyncStatus.SYNCED
)

fun TaskListEntity.toDto() = TaskListDto(
    id = id,
    householdId = householdId,
    ownerId = ownerId,
    name = name,
    iconKey = iconKey,
    colorHex = colorHex,
    isShared = isShared,
    defaultView = defaultView,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

fun TaskDto.toEntity() = TaskEntity(
    id = id,
    householdId = householdId,
    taskListId = taskListId,
    ownerId = ownerId,
    assignedTo = assignedTo,
    title = title,
    notes = notes,
    status = status,
    priority = priority,
    isShared = isShared,
    dueDate = dueDate,
    dueTime = dueTime,
    reminderMinutesBefore = reminderMinutesBefore,
    recurrenceRule = recurrenceRule,
    recurrenceSourceId = recurrenceSourceId,
    completedAt = completedAt,
    updatedBy = updatedBy,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncStatus = SyncStatus.SYNCED
)

fun TaskEntity.toDto() = TaskDto(
    id = id,
    householdId = householdId,
    taskListId = taskListId,
    ownerId = ownerId,
    assignedTo = assignedTo,
    title = title,
    notes = notes,
    status = status,
    priority = priority,
    isShared = isShared,
    dueDate = dueDate,
    dueTime = dueTime,
    reminderMinutesBefore = reminderMinutesBefore,
    recurrenceRule = recurrenceRule,
    recurrenceSourceId = recurrenceSourceId,
    completedAt = completedAt,
    updatedBy = updatedBy,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

fun TaskSubtaskDto.toEntity() = TaskSubtaskEntity(
    id = id,
    taskId = taskId,
    householdId = householdId,
    ownerId = ownerId,
    isShared = isShared,
    title = title,
    isCompleted = isCompleted,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncStatus = SyncStatus.SYNCED
)

fun TaskSubtaskEntity.toDto() = TaskSubtaskDto(
    id = id,
    taskId = taskId,
    householdId = householdId,
    ownerId = ownerId,
    isShared = isShared,
    title = title,
    isCompleted = isCompleted,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

fun CalendarEventDto.toEntity() = CalendarEventEntity(
    id = id,
    householdId = householdId,
    ownerId = ownerId,
    title = title,
    location = location,
    notes = notes,
    startAt = startAt,
    endAt = endAt,
    isAllDay = isAllDay,
    isShared = isShared,
    assignedMemberIdsCsv = assignedMemberIds.filter { it.isNotBlank() }.joinToString(","),
    colorHex = colorHex,
    recurrenceRule = recurrenceRule,
    reminderMinutesBefore = reminderMinutesBefore,
    externalProvider = externalProvider,
    externalCalendarId = externalCalendarId,
    externalEventId = externalEventId,
    externalEtag = externalEtag,
    updatedBy = updatedBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncStatus = SyncStatus.SYNCED
)

fun CalendarEventEntity.toDto() = CalendarEventDto(
    id = id,
    householdId = householdId,
    ownerId = ownerId,
    title = title,
    location = location,
    notes = notes,
    startAt = startAt,
    endAt = endAt,
    isAllDay = isAllDay,
    isShared = isShared,
    assignedMemberIds = assignedMemberIdsCsv.split(",").filter { it.isNotBlank() },
    colorHex = colorHex,
    recurrenceRule = recurrenceRule,
    reminderMinutesBefore = reminderMinutesBefore,
    externalProvider = externalProvider,
    externalCalendarId = externalCalendarId,
    externalEventId = externalEventId,
    externalEtag = externalEtag,
    updatedBy = updatedBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)
