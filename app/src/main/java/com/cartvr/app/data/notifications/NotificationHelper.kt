package com.cartvr.app.data.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.cartvr.app.MainActivity
import com.cartvr.app.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object NotificationHelper {

    const val CHANNEL_LIST_UPDATES = "cartvr_list_updates_v1"
    const val CHANNEL_LIVE_SYNC = "cartvr_live_sync"
    const val LIVE_SYNC_NOTIFICATION_ID = 4100
    const val EXTRA_LIST_ID = "extra_list_id"

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val chimeUri = Uri.parse("android.resource://${context.packageName}/${R.raw.cartvr_chime}")
        val soundAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val updatesChannel = NotificationChannel(
            CHANNEL_LIST_UPDATES,
            "Shared List Updates",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Chime alerts when another family member updates a shared shopping list"
            setSound(chimeUri, soundAttrs)
            enableVibration(true)
        }

        val liveChannel = NotificationChannel(
            CHANNEL_LIVE_SYNC,
            "Live Shopping Mode",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Quiet indicator while instant shopping alerts are active"
            setSound(null, null)
            enableVibration(false)
        }

        manager.createNotificationChannel(updatesChannel)
        manager.createNotificationChannel(liveChannel)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun postListUpdateNotification(
        context: Context,
        listId: String,
        title: String,
        body: String,
        expandedLines: List<String>
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_LIST_ID, listId)
        }
        val notificationId = 5000 + (listId.hashCode() and 0x7FFF)
        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val chimeUri = Uri.parse("android.resource://${context.packageName}/${R.raw.cartvr_chime}")
        val builder = NotificationCompat.Builder(context, CHANNEL_LIST_UPDATES)
            .setSmallIcon(R.drawable.ic_stat_cartvr)
            .setContentTitle(title)
            .setContentText(body)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(chimeUri)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setContentIntent(contentIntent)

        if (expandedLines.size > 1) {
            val inbox = NotificationCompat.InboxStyle().setBigContentTitle(body)
            expandedLines.forEach { inbox.addLine(it) }
            builder.setStyle(inbox)
        } else {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(body))
        }

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }

    fun buildLiveSyncNotification(context: Context, stopAtMs: Long): Notification {
        ensureChannels(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            LIVE_SYNC_NOTIFICATION_ID,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(context, LiveSyncService::class.java).apply {
            action = LiveSyncService.ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            context,
            LIVE_SYNC_NOTIFICATION_ID + 1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val untilTime = Instant.ofEpochMilli(stopAtMs)
            .atZone(ZoneId.systemDefault())
            .format(timeFormatter)

        return NotificationCompat.Builder(context, CHANNEL_LIVE_SYNC)
            .setSmallIcon(R.drawable.ic_stat_cartvr)
            .setContentTitle("CartVR Live Shopping Mode")
            .setContentText("Instant list alerts active until $untilTime")
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(contentIntent)
            .addAction(0, "Stop", stopPendingIntent)
            .build()
    }

    fun previewChime(context: Context) {
        runCatching {
            val afd = context.resources.openRawResourceFd(R.raw.cartvr_chime) ?: return
            val player = MediaPlayer()
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            player.setOnCompletionListener { mp ->
                runCatching { mp.release() }
            }
            player.setOnErrorListener { mp, _, _ ->
                runCatching { mp.release() }
                true
            }
            player.prepare()
            player.start()
        }
    }

    fun openChannelSoundSettings(context: Context) {
        ensureChannels(context)
        val channelIntent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, CHANNEL_LIST_UPDATES)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val fallbackIntent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            context.startActivity(channelIntent)
        }.onFailure {
            runCatching { context.startActivity(fallbackIntent) }
        }
    }
}
