package com.cartvr.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "household_profiles")
data class HouseholdProfileEntity(
    @PrimaryKey val userId: String,
    val displayName: String,
    val email: String,
    val accentColor: String = "#0D9488"
)

@Entity(
    tableName = "task_lists",
    indices = [Index("householdId")]
)
data class TaskListEntity(
    @PrimaryKey val id: String,
    val householdId: String,
    val ownerId: String,
    val name: String,
    val iconKey: String = "checklist",
    val colorHex: String = "#0D9488",
    val isShared: Boolean = true,
    val defaultView: String = "list",
    val sortOrder: Int = 0,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null,
    val syncStatus: String = SyncStatus.SYNCED
)

@Entity(
    tableName = "tasks",
    indices = [
        Index("taskListId"),
        Index("dueDate"),
        Index("assignedTo")
    ]
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val householdId: String,
    val taskListId: String,
    val ownerId: String,
    val assignedTo: String? = null,
    val title: String,
    val notes: String? = null,
    val status: String = "todo",
    val priority: String = "normal",
    val isShared: Boolean = true,
    val dueDate: String? = null,
    val dueTime: String? = null,
    val reminderMinutesBefore: Int? = null,
    val recurrenceRule: String? = null,
    val recurrenceSourceId: String? = null,
    val completedAt: String? = null,
    val updatedBy: String? = null,
    val sortOrder: Int = 0,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null,
    val syncStatus: String = SyncStatus.SYNCED
)

@Entity(
    tableName = "task_subtasks",
    indices = [Index("taskId")]
)
data class TaskSubtaskEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val householdId: String,
    val ownerId: String,
    val isShared: Boolean = true,
    val title: String,
    val isCompleted: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null,
    val syncStatus: String = SyncStatus.SYNCED
)

@Entity(
    tableName = "calendar_events",
    indices = [
        Index("householdId"),
        Index("startAt")
    ]
)
data class CalendarEventEntity(
    @PrimaryKey val id: String,
    val householdId: String,
    val ownerId: String,
    val title: String,
    val location: String? = null,
    val notes: String? = null,
    val startAt: String,
    val endAt: String,
    val isAllDay: Boolean = false,
    val isShared: Boolean = true,
    val assignedMemberIdsCsv: String = "",
    val colorHex: String? = null,
    val recurrenceRule: String? = null,
    val reminderMinutesBefore: Int? = null,
    val externalProvider: String? = null,
    val externalCalendarId: String? = null,
    val externalEventId: String? = null,
    val externalEtag: String? = null,
    val updatedBy: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null,
    val syncStatus: String = SyncStatus.SYNCED
)
