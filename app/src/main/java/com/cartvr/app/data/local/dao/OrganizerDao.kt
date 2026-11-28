package com.cartvr.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cartvr.app.data.local.entity.CalendarEventEntity
import com.cartvr.app.data.local.entity.HouseholdProfileEntity
import com.cartvr.app.data.local.entity.SyncStatus
import com.cartvr.app.data.local.entity.TaskEntity
import com.cartvr.app.data.local.entity.TaskListEntity
import com.cartvr.app.data.local.entity.TaskSubtaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrganizerDao {

    // --- Household Profiles ---
    @Query("SELECT * FROM household_profiles ORDER BY displayName COLLATE NOCASE ASC")
    fun observeHouseholdProfiles(): Flow<List<HouseholdProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfiles(profiles: List<HouseholdProfileEntity>)

    @Query("UPDATE household_profiles SET accentColor = :accentColor WHERE userId = :userId")
    suspend fun updateProfileAccentColor(userId: String, accentColor: String)

    @Query("DELETE FROM household_profiles")
    suspend fun clearProfiles()

    // --- Task Lists ---
    @Query("SELECT * FROM task_lists WHERE deletedAt IS NULL ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    fun observeActiveTaskLists(): Flow<List<TaskListEntity>>

    @Query("SELECT * FROM task_lists WHERE id = :id LIMIT 1")
    suspend fun getTaskListById(id: String): TaskListEntity?

    @Query("SELECT * FROM task_lists WHERE syncStatus != '${SyncStatus.SYNCED}'")
    suspend fun getPendingTaskLists(): List<TaskListEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTaskLists(lists: List<TaskListEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTaskList(list: TaskListEntity)

    @Query("DELETE FROM task_lists WHERE id = :id")
    suspend fun hardDeleteTaskList(id: String)

    @Query("DELETE FROM task_lists WHERE syncStatus = '${SyncStatus.SYNCED}' AND id NOT IN (:activeIds)")
    suspend fun pruneSyncedTaskListsNotIn(activeIds: List<String>)

    @Query("DELETE FROM task_lists WHERE syncStatus = '${SyncStatus.SYNCED}'")
    suspend fun pruneAllSyncedTaskLists()

    // --- Tasks ---
    @Query("SELECT * FROM tasks WHERE deletedAt IS NULL ORDER BY sortOrder ASC, createdAt DESC")
    fun observeActiveTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: String): TaskEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM tasks WHERE recurrenceSourceId = :sourceId AND deletedAt IS NULL)")
    suspend fun hasTaskWithRecurrenceSource(sourceId: String): Boolean

    @Query("SELECT * FROM tasks WHERE taskListId = :taskListId AND deletedAt IS NULL")
    suspend fun getActiveTasksForList(taskListId: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE syncStatus != '${SyncStatus.SYNCED}'")
    suspend fun getPendingTasks(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTasks(tasks: List<TaskEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun hardDeleteTask(id: String)

    @Query("DELETE FROM tasks WHERE taskListId = :taskListId")
    suspend fun deleteTasksByListId(taskListId: String)

    @Query("UPDATE tasks SET isShared = :isShared, updatedAt = :updatedAt, syncStatus = '${SyncStatus.PENDING_UPSERT}' WHERE taskListId = :taskListId AND isShared != :isShared")
    suspend fun updateTasksSharedForList(taskListId: String, isShared: Boolean, updatedAt: String)

    @Query("DELETE FROM tasks WHERE syncStatus = '${SyncStatus.SYNCED}' AND id NOT IN (:activeIds)")
    suspend fun pruneSyncedTasksNotIn(activeIds: List<String>)

    @Query("DELETE FROM tasks WHERE syncStatus = '${SyncStatus.SYNCED}'")
    suspend fun pruneAllSyncedTasks()

    // --- Task Subtasks ---
    @Query("SELECT * FROM task_subtasks WHERE deletedAt IS NULL ORDER BY sortOrder ASC, createdAt ASC")
    fun observeActiveSubtasks(): Flow<List<TaskSubtaskEntity>>

    @Query("SELECT * FROM task_subtasks WHERE taskId = :taskId AND deletedAt IS NULL ORDER BY sortOrder ASC, createdAt ASC")
    suspend fun getActiveSubtasksForTask(taskId: String): List<TaskSubtaskEntity>

    @Query("SELECT * FROM task_subtasks WHERE syncStatus != '${SyncStatus.SYNCED}'")
    suspend fun getPendingSubtasks(): List<TaskSubtaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSubtasks(subtasks: List<TaskSubtaskEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSubtask(subtask: TaskSubtaskEntity)

    @Query("DELETE FROM task_subtasks WHERE id = :id")
    suspend fun hardDeleteSubtask(id: String)

    @Query("DELETE FROM task_subtasks WHERE taskId = :taskId")
    suspend fun deleteSubtasksByTaskId(taskId: String)

    @Query("DELETE FROM task_subtasks WHERE taskId IN (SELECT id FROM tasks WHERE taskListId = :taskListId)")
    suspend fun deleteSubtasksByListId(taskListId: String)

    @Query("UPDATE task_subtasks SET isShared = :isShared, updatedAt = :updatedAt, syncStatus = '${SyncStatus.PENDING_UPSERT}' WHERE taskId = :taskId AND isShared != :isShared")
    suspend fun updateSubtasksSharedForTask(taskId: String, isShared: Boolean, updatedAt: String)

    @Query("UPDATE task_subtasks SET isShared = :isShared, updatedAt = :updatedAt, syncStatus = '${SyncStatus.PENDING_UPSERT}' WHERE taskId IN (SELECT id FROM tasks WHERE taskListId = :taskListId) AND isShared != :isShared")
    suspend fun updateSubtasksSharedForList(taskListId: String, isShared: Boolean, updatedAt: String)

    @Query("DELETE FROM task_subtasks WHERE syncStatus = '${SyncStatus.SYNCED}' AND id NOT IN (:activeIds)")
    suspend fun pruneSyncedSubtasksNotIn(activeIds: List<String>)

    @Query("DELETE FROM task_subtasks WHERE syncStatus = '${SyncStatus.SYNCED}'")
    suspend fun pruneAllSyncedSubtasks()

    // --- Calendar Events ---
    @Query("SELECT * FROM calendar_events WHERE deletedAt IS NULL ORDER BY startAt ASC")
    fun observeActiveCalendarEvents(): Flow<List<CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events WHERE id = :id LIMIT 1")
    suspend fun getCalendarEventById(id: String): CalendarEventEntity?

    @Query("SELECT * FROM calendar_events WHERE syncStatus != '${SyncStatus.SYNCED}'")
    suspend fun getPendingCalendarEvents(): List<CalendarEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCalendarEvents(events: List<CalendarEventEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCalendarEvent(event: CalendarEventEntity)

    @Query("DELETE FROM calendar_events WHERE id = :id")
    suspend fun hardDeleteCalendarEvent(id: String)

    @Query("DELETE FROM calendar_events WHERE syncStatus = '${SyncStatus.SYNCED}' AND id NOT IN (:activeIds)")
    suspend fun pruneSyncedCalendarEventsNotIn(activeIds: List<String>)

    @Query("DELETE FROM calendar_events WHERE syncStatus = '${SyncStatus.SYNCED}'")
    suspend fun pruneAllSyncedCalendarEvents()

    // --- Cleanup ---
    @Query("DELETE FROM task_subtasks")
    suspend fun clearSubtasks()

    @Query("DELETE FROM tasks")
    suspend fun clearTasks()

    @Query("DELETE FROM task_lists")
    suspend fun clearTaskLists()

    @Query("DELETE FROM calendar_events")
    suspend fun clearCalendarEvents()

    @Transaction
    suspend fun clearAll() {
        clearSubtasks()
        clearTasks()
        clearTaskLists()
        clearCalendarEvents()
        clearProfiles()
    }
}
