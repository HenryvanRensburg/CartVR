package com.cartvr.app.data.notifications

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.cartvr.app.CartVRApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LiveSyncService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var monitorJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            deactivateAndStop()
            return START_NOT_STICKY
        }

        val durationHours = intent?.getIntExtra(EXTRA_DURATION_HOURS, DEFAULT_DURATION_HOURS)
            ?.coerceIn(1, 4) ?: DEFAULT_DURATION_HOURS
        val stopAtMs = System.currentTimeMillis() + durationHours * 3_600_000L

        val notification = NotificationHelper.buildLiveSyncNotification(this, stopAtMs)
        val fgType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            0
        }
        ServiceCompat.startForeground(
            this,
            NotificationHelper.LIVE_SYNC_NOTIFICATION_ID,
            notification,
            fgType
        )

        _isRunning.value = true

        val app = applicationContext as CartVRApplication
        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            app.preferences.setLiveMode(enabled = true, stopAtMs = stopAtMs)
            app.repository.awaitAuthReady()
            app.repository.syncAll(force = true)
            app.repository.ensureRealtimeSubscribed(forceReconnect = true)

            while (System.currentTimeMillis() < stopAtMs) {
                delay(60_000L)
                if (System.currentTimeMillis() >= stopAtMs) break
                app.repository.ensureRealtimeSubscribed()
            }

            deactivateAndStop()
        }

        return START_NOT_STICKY
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        deactivateAndStop()
    }

    private fun deactivateAndStop() {
        monitorJob?.cancel()
        _isRunning.value = false
        val app = applicationContext as? CartVRApplication
        serviceScope.launch {
            app?.preferences?.setLiveMode(enabled = false, stopAtMs = 0L)
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        _isRunning.value = false
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.cartvr.app.action.START_LIVE_SYNC"
        const val ACTION_STOP = "com.cartvr.app.action.STOP_LIVE_SYNC"
        const val EXTRA_DURATION_HOURS = "extra_duration_hours"
        const val DEFAULT_DURATION_HOURS = 2

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        fun start(context: Context, durationHours: Int = DEFAULT_DURATION_HOURS) {
            val intent = Intent(context.applicationContext, LiveSyncService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_DURATION_HOURS, durationHours)
            }
            ContextCompat.startForegroundService(context.applicationContext, intent)
        }

        fun stop(context: Context) {
            val appContext = context.applicationContext
            if (!_isRunning.value) return
            val intent = Intent(appContext, LiveSyncService::class.java).apply {
                action = ACTION_STOP
            }
            runCatching {
                appContext.startService(intent)
            }.onFailure {
                appContext.stopService(Intent(appContext, LiveSyncService::class.java))
            }
        }
    }
}
