package com.cartvr.app

import android.app.Application
import com.cartvr.app.data.local.CartVrDatabase
import com.cartvr.app.data.notifications.ListSyncWorker
import com.cartvr.app.data.notifications.LiveSyncService
import com.cartvr.app.data.notifications.NotificationBundler
import com.cartvr.app.data.notifications.NotificationHelper
import com.cartvr.app.data.preferences.AppPreferences
import com.cartvr.app.data.sync.CartVrRepository
import com.cartvr.app.data.sync.NetworkMonitor
import com.cartvr.app.data.updater.GitHubUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CartVRApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var preferences: AppPreferences
        private set

    lateinit var database: CartVrDatabase
        private set

    lateinit var networkMonitor: NetworkMonitor
        private set

    lateinit var notificationBundler: NotificationBundler
        private set

    lateinit var repository: CartVrRepository
        private set

    lateinit var updater: GitHubUpdater
        private set

    override fun onCreate() {
        super.onCreate()
        preferences = AppPreferences(this)
        database = CartVrDatabase.getInstance(this)
        networkMonitor = NetworkMonitor(this)
        NotificationHelper.ensureChannels(this)
        notificationBundler = NotificationBundler(this, preferences)
        repository = CartVrRepository(
            dao = database.cartVrDao(),
            organizerDao = database.organizerDao(),
            prefs = preferences,
            networkMonitor = networkMonitor,
            notificationBundler = notificationBundler
        )
        updater = GitHubUpdater(this)

        appScope.launch {
            if (preferences.notificationsEnabled.first()) {
                ListSyncWorker.WorkScheduler.schedule(this@CartVRApplication)
            }
            val liveSaved = preferences.liveModeEnabled.first()
            val stopAt = preferences.liveModeStopAtMs.first()
            if (liveSaved && !LiveSyncService.isRunning.value) {
                if (stopAt > System.currentTimeMillis()) {
                    val remainingHours = ((stopAt - System.currentTimeMillis() + 3_599_999L) / 3_600_000L)
                        .toInt()
                        .coerceIn(1, 4)
                    LiveSyncService.start(this@CartVRApplication, remainingHours)
                } else {
                    preferences.setLiveMode(enabled = false, stopAtMs = 0L)
                }
            }
        }
    }
}
