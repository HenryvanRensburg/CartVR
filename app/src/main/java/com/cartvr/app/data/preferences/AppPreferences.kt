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
        }
    }
}
