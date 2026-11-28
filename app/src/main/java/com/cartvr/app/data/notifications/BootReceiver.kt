package com.cartvr.app.data.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.cartvr.app.CartVRApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val validActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED
        )
        if (intent.action !in validActions) return

        val pendingResult = goAsync()
        val app = context.applicationContext as? CartVRApplication
        if (app == null) {
            pendingResult.finish()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
                    app.preferences.setLiveMode(enabled = false, stopAtMs = 0L)
                }
                if (app.preferences.notificationsEnabled.first()) {
                    ListSyncWorker.WorkScheduler.schedule(context)
                }
                ReminderScheduler.rescheduleAll(
                    context = context,
                    organizerDao = app.database.organizerDao(),
                    prefs = app.preferences
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
