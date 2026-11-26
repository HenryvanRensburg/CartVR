package com.cartvr.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
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

    suspend fun clearSessionData() {
        context.dataStore.edit {
            it.remove(Keys.HOUSEHOLD_ID)
            it.remove(Keys.HOUSEHOLD_NAME)
            it.remove(Keys.LAST_SYNCED_AT)
            it.remove(Keys.USER_ROLE)
            it.remove(Keys.USER_DISPLAY_NAME)
        }
    }
}
