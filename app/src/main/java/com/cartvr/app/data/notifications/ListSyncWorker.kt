package com.cartvr.app.data.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.cartvr.app.CartVRApplication
import kotlinx.coroutines.flow.first
import java.io.IOException
import java.util.concurrent.TimeUnit

class ListSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? CartVRApplication ?: return Result.success()
        val enabled = app.preferences.notificationsEnabled.first()
        if (!enabled) return Result.success()

        if (LiveSyncService.isRunning.value) {
            app.repository.ensureRealtimeSubscribed()
            return Result.success()
        }

        return try {
            app.repository.pollAndNotifyBackgroundChanges()
            Result.success()
        } catch (_: IOException) {
            if (runAttemptCount < 2) Result.retry() else Result.success()
        } catch (_: Throwable) {
            Result.success()
        }
    }

    companion object WorkScheduler {
        private const val UNIQUE_WORK_NAME = "cartvr_periodic_list_sync"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<ListSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context.applicationContext).cancelUniqueWork(UNIQUE_WORK_NAME)
        }
    }
}
