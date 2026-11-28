package com.cartvr.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cartvr_prefs")

class AppPreferences(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val HOUSEHOLD_ID = stringPreferencesKey("household_id")
        val HOUSEHOLD_NAME = stringPreferencesKey("household_name")
        val LAST_SYNCED_AT = stringPreferencesKey("last_synced_at")
        val USER_ROLE = stringPreferencesKey("user_role")
        val USER_DISPLAY_NAME = stringPreferencesKey("user_display_name")

        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val NOTIFY_ON_CHECKED = booleanPreferencesKey("notify_on_checked")
        val LIVE_MODE_ENABLED = booleanPreferencesKey("live_mode_enabled")
        val LIVE_MODE_DURATION_HOURS = intPreferencesKey("live_mode_duration_hours")
        val LIVE_MODE_STOP_AT_MS = longPreferencesKey("live_mode_stop_at_ms")
        val LAST_NOTIFIED_AT = stringPreferencesKey("last_notified_at")

        val MORNING_SUMMARY_ENABLED = booleanPreferencesKey("morning_summary_enabled")
        val MORNING_SUMMARY_HOUR = intPreferencesKey("morning_summary_hour")
        val MORNING_SUMMARY_MINUTE = intPreferencesKey("morning_summary_minute")
        val DEFAULT_EVENT_REMINDER_MINUTES = intPreferencesKey("default_event_reminder_minutes")
        val NOTIFY_TASK_ASSIGNMENTS = booleanPreferencesKey("notify_task_assignments")
        val NOTIFY_SHARED_CALENDAR = booleanPreferencesKey("notify_shared_calendar")
        val SCHEDULED_ALARM_IDS = stringPreferencesKey("scheduled_alarm_ids")

        val KEEP_SCREEN_AWAKE_ON_LIST = booleanPreferencesKey("keep_screen_awake_on_list")
        val KEEP_SCREEN_AWAKE_IN_LIVE_MODE = booleanPreferencesKey("keep_screen_awake_in_live_mode")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        runCatching { ThemeMode.valueOf(raw) }.getOrDefault(ThemeMode.SYSTEM)
    }

    val householdId: Flow<String?> = context.dataStore.data.map { it[Keys.HOUSEHOLD_ID] }
    val householdName: Flow<String?> = context.dataStore.data.map { it[Keys.HOUSEHOLD_NAME] }
    val lastSyncedAt: Flow<String?> = context.dataStore.data.map { it[Keys.LAST_SYNCED_AT] }
    val isAdmin: Flow<Boolean> = context.dataStore.data.map { it[Keys.USER_ROLE] == "admin" }
    val displayName: Flow<String?> = context.dataStore.data.map { it[Keys.USER_DISPLAY_NAME] }

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.NOTIFICATIONS_ENABLED] ?: false }
    val notifyOnChecked: Flow<Boolean> = context.dataStore.data.map { it[Keys.NOTIFY_ON_CHECKED] ?: false }
    val liveModeEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.LIVE_MODE_ENABLED] ?: false }
    val liveModeDurationHours: Flow<Int> = context.dataStore.data.map { it[Keys.LIVE_MODE_DURATION_HOURS] ?: 2 }
    val liveModeStopAtMs: Flow<Long> = context.dataStore.data.map { it[Keys.LIVE_MODE_STOP_AT_MS] ?: 0L }
    val lastNotifiedAt: Flow<String?> = context.dataStore.data.map { it[Keys.LAST_NOTIFIED_AT] }

    val morningSummaryEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.MORNING_SUMMARY_ENABLED] ?: false }
    val morningSummaryHour: Flow<Int> = context.dataStore.data.map { it[Keys.MORNING_SUMMARY_HOUR] ?: 7 }
    val morningSummaryMinute: Flow<Int> = context.dataStore.data.map { it[Keys.MORNING_SUMMARY_MINUTE] ?: 0 }
    val defaultEventReminderMinutes: Flow<Int> = context.dataStore.data.map { it[Keys.DEFAULT_EVENT_REMINDER_MINUTES] ?: 30 }
    val notifyTaskAssignments: Flow<Boolean> = context.dataStore.data.map { it[Keys.NOTIFY_TASK_ASSIGNMENTS] ?: true }
    val notifySharedCalendar: Flow<Boolean> = context.dataStore.data.map { it[Keys.NOTIFY_SHARED_CALENDAR] ?: true }
    val scheduledAlarmIds: Flow<Set<Int>> = context.dataStore.data.map { prefs ->
        (prefs[Keys.SCHEDULED_ALARM_IDS] ?: "")
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet()
    }

    val keepScreenAwakeOnList: Flow<Boolean> = context.dataStore.data.map {
        it[Keys.KEEP_SCREEN_AWAKE_ON_LIST] ?: false
    }
    val keepScreenAwakeInLiveMode: Flow<Boolean> = context.dataStore.data.map {
        it[Keys.KEEP_SCREEN_AWAKE_IN_LIVE_MODE] ?: true
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setHousehold(id: String, name: String) {
        context.dataStore.edit {
            it[Keys.HOUSEHOLD_ID] = id
            it[Keys.HOUSEHOLD_NAME] = name
        }
    }

    suspend fun setUserProfile(displayName: String, role: String) {
        context.dataStore.edit {
            it[Keys.USER_DISPLAY_NAME] = displayName
            it[Keys.USER_ROLE] = role
        }
    }

    suspend fun setLastSyncedAt(isoTimestamp: String) {
        context.dataStore.edit { it[Keys.LAST_SYNCED_AT] = isoTimestamp }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setNotifyOnChecked(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFY_ON_CHECKED] = enabled }
    }

    suspend fun setLiveMode(enabled: Boolean, stopAtMs: Long) {
        context.dataStore.edit {
            it[Keys.LIVE_MODE_ENABLED] = enabled
            it[Keys.LIVE_MODE_STOP_AT_MS] = stopAtMs
        }
    }

    suspend fun setLiveModeDurationHours(hours: Int) {
        context.dataStore.edit { it[Keys.LIVE_MODE_DURATION_HOURS] = hours }
    }

    suspend fun setLastNotifiedAt(isoTimestamp: String) {
        context.dataStore.edit { it[Keys.LAST_NOTIFIED_AT] = isoTimestamp }
    }

    suspend fun setMorningSummaryEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.MORNING_SUMMARY_ENABLED] = enabled }
    }

    suspend fun setMorningSummaryTime(hour: Int, minute: Int) {
        context.dataStore.edit {
            it[Keys.MORNING_SUMMARY_HOUR] = hour.coerceIn(0, 23)
            it[Keys.MORNING_SUMMARY_MINUTE] = minute.coerceIn(0, 59)
        }
    }

    suspend fun setDefaultEventReminderMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.DEFAULT_EVENT_REMINDER_MINUTES] = minutes }
    }

    suspend fun setNotifyTaskAssignments(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFY_TASK_ASSIGNMENTS] = enabled }
    }

    suspend fun setNotifySharedCalendar(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFY_SHARED_CALENDAR] = enabled }
    }

    suspend fun setScheduledAlarmIds(ids: Set<Int>) {
        context.dataStore.edit { it[Keys.SCHEDULED_ALARM_IDS] = ids.joinToString(",") }
    }

    suspend fun setKeepScreenAwakeOnList(enabled: Boolean) {
        context.dataStore.edit { it[Keys.KEEP_SCREEN_AWAKE_ON_LIST] = enabled }
    }

    suspend fun setKeepScreenAwakeInLiveMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.KEEP_SCREEN_AWAKE_IN_LIVE_MODE] = enabled }
    }

    suspend fun clearSessionData() {
        context.dataStore.edit {
            it.remove(Keys.HOUSEHOLD_ID)
            it.remove(Keys.HOUSEHOLD_NAME)
            it.remove(Keys.LAST_SYNCED_AT)
            it.remove(Keys.USER_ROLE)
            it.remove(Keys.USER_DISPLAY_NAME)
            it.remove(Keys.LIVE_MODE_ENABLED)
            it.remove(Keys.LIVE_MODE_STOP_AT_MS)
            it.remove(Keys.LAST_NOTIFIED_AT)
            it.remove(Keys.SCHEDULED_ALARM_IDS)
        }
    }
}
