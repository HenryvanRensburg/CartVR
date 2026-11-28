package com.cartvr.app.data.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.cartvr.app.data.local.dao.OrganizerDao
import com.cartvr.app.data.preferences.AppPreferences
import com.cartvr.app.domain.CalendarRecurrence
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object ReminderScheduler {

    private const val DAILY_SUMMARY_REQUEST_CODE = 7001
    private const val MAX_ITEM_ALARMS = 40
    private val scheduleMutex = Mutex()
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

    suspend fun rescheduleAll(
        context: Context,
        organizerDao: OrganizerDao,
        prefs: AppPreferences
    ) {
        scheduleMutex.withLock {
            val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
            cancelTrackedAlarmsLocked(context, alarmManager, prefs)

            val nowMs = System.currentTimeMillis()
            val zone = ZoneId.systemDefault()
            val scheduledCodes = mutableSetOf<Int>()

            // 1. Daily Morning Summary
            if (prefs.morningSummaryEnabled.first()) {
                val hour = prefs.morningSummaryHour.first()
                val minute = prefs.morningSummaryMinute.first()
                val today = LocalDate.now(zone)
                var triggerDateTime = LocalDateTime.of(today, LocalTime.of(hour, minute))
                if (triggerDateTime.atZone(zone).toInstant().toEpochMilli() <= nowMs + 5_000L) {
                    triggerDateTime = triggerDateTime.plusDays(1)
                }
                val triggerAtMs = triggerDateTime.atZone(zone).toInstant().toEpochMilli()
                val pi = buildPendingIntent(
                    context = context,
                    requestCode = DAILY_SUMMARY_REQUEST_CODE,
                    type = ReminderReceiver.TYPE_DAILY_SUMMARY,
                    title = "Morning Summary",
                    body = "",
                    itemId = "",
                    occurrenceDate = ""
                )
                scheduleAlarmSafe(context, alarmManager, triggerAtMs, pi)
                scheduledCodes += DAILY_SUMMARY_REQUEST_CODE
            }

            // 2. Upcoming timed tasks & calendar events (next 14 days)
            val today = LocalDate.now(zone)
            val horizon = today.plusDays(14)
            val defaultEventLead = prefs.defaultEventReminderMinutes.first()

            data class CandidateAlarm(
                val requestCode: Int,
                val triggerAtMs: Long,
                val type: String,
                val title: String,
                val body: String,
                val itemId: String,
                val occurrenceDate: String
            )

            val candidates = mutableListOf<CandidateAlarm>()

            val activeTasks = organizerDao.observeActiveTasks().first()
            for (task in activeTasks) {
                if (task.status == "done" || task.deletedAt != null) continue
                val dateStr = task.dueDate?.takeIf { it.isNotBlank() } ?: continue
                val timeStr = task.dueTime?.takeIf { it.isNotBlank() } ?: continue
                val lead = task.reminderMinutesBefore ?: 0
                if (lead < 0) continue

                val localDate = runCatching { LocalDate.parse(dateStr) }.getOrNull() ?: continue
                if (localDate.isBefore(today) || localDate.isAfter(horizon)) continue
                val localTime = runCatching { LocalTime.parse(timeStr) }.getOrNull() ?: continue

                val triggerMs = LocalDateTime.of(localDate, localTime)
                    .minusMinutes(lead.toLong())
                    .atZone(zone)
                    .toInstant()
                    .toEpochMilli()
                if (triggerMs <= nowMs + 5_000L) continue

                val reqCode = 100000 + (task.id.hashCode() and 0x0FFFFFFF)
                candidates += CandidateAlarm(
                    requestCode = reqCode,
                    triggerAtMs = triggerMs,
                    type = ReminderReceiver.TYPE_TASK_REMINDER,
                    title = "Task due: ${task.title}",
                    body = if (lead > 0) "Due in $lead min at $timeStr" else "Due now ($timeStr)",
                    itemId = task.id,
                    occurrenceDate = dateStr
                )
            }

            val activeEvents = organizerDao.observeActiveCalendarEvents().first()
            val expanded = CalendarRecurrence.expandEventsInRange(activeEvents, today, horizon, zone)
            for (occ in expanded) {
                val lead = occ.event.reminderMinutesBefore ?: defaultEventLead
                if (lead < 0) continue

                val anchorTime = if (occ.event.isAllDay || occ.startTime == null) {
                    LocalTime.of(8, 0)
                } else {
                    occ.startTime
                }

                val triggerMs = LocalDateTime.of(occ.occurrenceDate, anchorTime)
                    .minusMinutes(lead.toLong())
                    .atZone(zone)
                    .toInstant()
                    .toEpochMilli()
                if (triggerMs <= nowMs + 5_000L) continue

                val reqCode = 200000 + ("${occ.event.id}:${occ.occurrenceDate}".hashCode() and 0x0FFFFFFF)
                val timeLabel = if (occ.event.isAllDay || occ.startTime == null) {
                    "All day"
                } else {
                    occ.startTime.format(timeFormatter)
                }
                val locationSuffix = occ.event.location?.takeIf { it.isNotBlank() }?.let { " • $it" }.orEmpty()

                candidates += CandidateAlarm(
                    requestCode = reqCode,
                    triggerAtMs = triggerMs,
                    type = ReminderReceiver.TYPE_EVENT_REMINDER,
                    title = occ.event.title,
                    body = "$timeLabel$locationSuffix",
                    itemId = occ.event.id,
                    occurrenceDate = occ.occurrenceDate.toString()
                )
            }

            candidates.sortedBy { it.triggerAtMs }
                .take(MAX_ITEM_ALARMS)
                .forEach { candidate ->
                    val pi = buildPendingIntent(
                        context = context,
                        requestCode = candidate.requestCode,
                        type = candidate.type,
                        title = candidate.title,
                        body = candidate.body,
                        itemId = candidate.itemId,
                        occurrenceDate = candidate.occurrenceDate
                    )
                    scheduleAlarmSafe(context, alarmManager, candidate.triggerAtMs, pi)
                    scheduledCodes += candidate.requestCode
                }

            prefs.setScheduledAlarmIds(scheduledCodes)
        }
    }

    suspend fun cancelAll(context: Context, prefs: AppPreferences) {
        scheduleMutex.withLock {
            val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
            cancelTrackedAlarmsLocked(context, alarmManager, prefs)
            prefs.setScheduledAlarmIds(emptySet())
        }
    }

    private suspend fun cancelTrackedAlarmsLocked(
        context: Context,
        alarmManager: AlarmManager,
        prefs: AppPreferences
    ) {
        val codes = prefs.scheduledAlarmIds.first() + DAILY_SUMMARY_REQUEST_CODE
        codes.forEach { code ->
            val pi = PendingIntent.getBroadcast(
                context,
                code,
                Intent(context, ReminderReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pi != null) {
                alarmManager.cancel(pi)
                pi.cancel()
            }
        }
    }

    private fun buildPendingIntent(
        context: Context,
        requestCode: Int,
        type: String,
        title: String,
        body: String,
        itemId: String,
        occurrenceDate: String
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_REMINDER_TYPE, type)
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_BODY, body)
            putExtra(ReminderReceiver.EXTRA_ITEM_ID, itemId)
            putExtra(ReminderReceiver.EXTRA_OCCURRENCE_DATE, occurrenceDate)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun scheduleAlarmSafe(
        context: Context,
        alarmManager: AlarmManager,
        triggerAtMs: Long,
        pendingIntent: PendingIntent
    ) {
        if (NotificationHelper.canScheduleExactAlarms(context)) {
            val exactOk = runCatching {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
            }.isSuccess
            if (exactOk) return
        }
        runCatching {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
        }
    }
}
