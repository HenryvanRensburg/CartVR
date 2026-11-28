package com.cartvr.app.data.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.cartvr.app.CartVRApplication
import com.cartvr.app.domain.CalendarRecurrence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val app = context.applicationContext as? CartVRApplication
        if (app == null) {
            pendingResult.finish()
            return
        }

        val type = intent.getStringExtra(EXTRA_REMINDER_TYPE) ?: run {
            pendingResult.finish()
            return
        }
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val body = intent.getStringExtra(EXTRA_BODY).orEmpty()
        val itemId = intent.getStringExtra(EXTRA_ITEM_ID).orEmpty()
        val occurrenceDate = intent.getStringExtra(EXTRA_OCCURRENCE_DATE).orEmpty()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val organizerDao = app.database.organizerDao()
                when (type) {
                    TYPE_DAILY_SUMMARY -> {
                        if (app.preferences.morningSummaryEnabled.first()) {
                            val zone = ZoneId.systemDefault()
                            val today = LocalDate.now(zone)
                            val todayStr = today.toString()

                            val activeTasks = organizerDao.observeActiveTasks().first()
                            val dueTasks = activeTasks.filter {
                                it.status != "done" && !it.dueDate.isNullOrBlank() && it.dueDate <= todayStr
                            }
                            val overdueCount = dueTasks.count { (it.dueDate ?: "") < todayStr }

                            val activeEvents = organizerDao.observeActiveCalendarEvents().first()
                            val todayEvents = CalendarRecurrence.expandEventsInRange(activeEvents, today, today, zone)

                            val displayName = app.preferences.displayName.first() ?: "there"
                            val summaryTitle = "Good morning, $displayName"
                            val summaryBody = buildString {
                                append("${dueTasks.size} task")
                                if (dueTasks.size != 1) append("s")
                                if (overdueCount > 0) append(" ($overdueCount overdue)")
                                append(" • ${todayEvents.size} event")
                                if (todayEvents.size != 1) append("s")
                                append(" today")
                            }

                            val timeFmt = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
                            val lines = buildList {
                                todayEvents.take(4).forEach { occ ->
                                    val t = if (occ.event.isAllDay || occ.startTime == null) {
                                        "All day"
                                    } else {
                                        occ.startTime.format(timeFmt)
                                    }
                                    add("$t — ${occ.event.title}")
                                }
                                dueTasks.take((6 - size).coerceAtLeast(0)).forEach { task ->
                                    add("☐ ${task.title}")
                                }
                            }

                            NotificationHelper.postDailySummaryNotification(
                                context = context,
                                title = summaryTitle,
                                body = summaryBody,
                                lines = lines
                            )
                        }
                        ReminderScheduler.rescheduleAll(context, organizerDao, app.preferences)
                    }

                    TYPE_TASK_REMINDER -> {
                        val task = organizerDao.getTaskById(itemId)
                        if (task != null && task.deletedAt == null && task.status != "done") {
                            NotificationHelper.postTaskReminderNotification(
                                context = context,
                                taskId = task.id,
                                title = title.ifBlank { "Task due: ${task.title}" },
                                body = body.ifBlank { "Tap to view task details" }
                            )
                        }
                    }

                    TYPE_EVENT_REMINDER -> {
                        val event = organizerDao.getCalendarEventById(itemId)
                        if (event != null && event.deletedAt == null) {
                            NotificationHelper.postEventReminderNotification(
                                context = context,
                                eventId = event.id,
                                eventDateIso = occurrenceDate.ifBlank { LocalDate.now().toString() },
                                title = title.ifBlank { event.title },
                                body = body.ifBlank { "Upcoming calendar event" }
                            )
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_REMINDER_TYPE = "extra_reminder_type"
        const val TYPE_DAILY_SUMMARY = "daily_summary"
        const val TYPE_TASK_REMINDER = "task_reminder"
        const val TYPE_EVENT_REMINDER = "event_reminder"

        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_BODY = "extra_body"
        const val EXTRA_ITEM_ID = "extra_item_id"
        const val EXTRA_OCCURRENCE_DATE = "extra_occurrence_date"
    }
}
