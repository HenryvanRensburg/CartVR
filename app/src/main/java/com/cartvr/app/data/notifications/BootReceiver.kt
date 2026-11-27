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
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        val app = context.applicationContext as? CartVRApplication
        if (app == null) {
            pendingResult.finish()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.preferences.setLiveMode(enabled = false, stopAtMs = 0L)
                if (app.preferences.notificationsEnabled.first()) {
                    ListSyncWorker.WorkScheduler.schedule(context)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
