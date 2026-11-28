package com.cartvr.app.domain

import com.cartvr.app.data.local.entity.CalendarEventEntity
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class CalendarEventOccurrence(
    val event: CalendarEventEntity,
    val occurrenceDate: LocalDate,
    val startTime: LocalTime?,
    val endTime: LocalTime?
)

object CalendarRecurrence {
    const val DAILY = "DAILY"
    const val WEEKLY = "WEEKLY"
    const val MONTHLY = "MONTHLY"
    const val YEARLY = "YEARLY"

    private val rruleDateFormatter = DateTimeFormatter.BASIC_ISO_DATE
    private val displayDateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())

    fun buildRRule(freq: String?, untilDate: LocalDate? = null): String? {
        val cleanFreq = freq?.trim()?.uppercase()?.takeIf {
            it == DAILY || it == WEEKLY || it == MONTHLY || it == YEARLY
        } ?: return null

        return buildString {
            append("FREQ=").append(cleanFreq).append(";INTERVAL=1")
            if (untilDate != null) {
                append(";UNTIL=").append(untilDate.format(rruleDateFormatter))
            }
        }
    }

    fun parseFrequency(rule: String?): String? {
        if (rule.isNullOrBlank()) return null
        val match = Regex("""FREQ=(DAILY|WEEKLY|MONTHLY|YEARLY)""", RegexOption.IGNORE_CASE)
            .find(rule)
        return match?.groupValues?.getOrNull(1)?.uppercase()
    }

    fun parseUntilDate(rule: String?): LocalDate? {
        if (rule.isNullOrBlank()) return null
        val match = Regex("""UNTIL=(\d{8})""", RegexOption.IGNORE_CASE).find(rule) ?: return null
        val raw = match.groupValues.getOrNull(1) ?: return null
        return runCatching { LocalDate.parse(raw, rruleDateFormatter) }.getOrNull()
    }

    fun withUntilDate(rule: String?, untilDate: LocalDate): String? {
        val freq = parseFrequency(rule) ?: return null
        return buildRRule(freq, untilDate)
    }

    fun formatLabel(rule: String?): String {
        val freq = parseFrequency(rule) ?: return "Does not repeat"
        val base = when (freq) {
            DAILY -> "Daily"
            WEEKLY -> "Weekly"
            MONTHLY -> "Monthly"
            YEARLY -> "Yearly"
            else -> "Repeats"
        }
        val until = parseUntilDate(rule)
        return if (until != null) {
            "$base until ${until.format(displayDateFormatter)}"
        } else {
            base
        }
    }

    fun expandEventsInRange(
        events: List<CalendarEventEntity>,
        rangeStart: LocalDate,
        rangeEnd: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<CalendarEventOccurrence> {
        if (rangeEnd.isBefore(rangeStart) || events.isEmpty()) return emptyList()
        val results = mutableListOf<CalendarEventOccurrence>()

        for (event in events) {
            if (event.deletedAt != null) continue
            val startInstant = runCatching { Instant.parse(event.startAt) }.getOrNull() ?: continue
            val endInstant = runCatching { Instant.parse(event.endAt) }.getOrNull() ?: startInstant

            val startZoned = startInstant.atZone(zoneId)
            val endZoned = endInstant.atZone(zoneId)
            val eventStartDate = startZoned.toLocalDate()
            val eventEndDate = maxOf(eventStartDate, endZoned.toLocalDate())
            val startTime = if (event.isAllDay) null else startZoned.toLocalTime()
            val endTime = if (event.isAllDay) null else endZoned.toLocalTime()

            val freq = parseFrequency(event.recurrenceRule)
            if (freq == null) {
                var day = maxOf(eventStartDate, rangeStart)
                val limit = minOf(eventEndDate, rangeEnd, eventStartDate.plusDays(60))
                while (!day.isAfter(limit)) {
                    results += CalendarEventOccurrence(
                        event = event,
                        occurrenceDate = day,
                        startTime = if (day == eventStartDate) startTime else null,
                        endTime = if (day == eventEndDate) endTime else null
                    )
                    day = day.plusDays(1)
                }
            } else {
                val untilDate = parseUntilDate(event.recurrenceRule)
                val effectiveEnd = if (untilDate != null && untilDate.isBefore(rangeEnd)) untilDate else rangeEnd
                if (effectiveEnd.isBefore(eventStartDate)) continue

                var cursor = eventStartDate
                var guard = 0
                while (!cursor.isAfter(effectiveEnd) && guard < 500) {
                    if (!cursor.isBefore(rangeStart)) {
                        results += CalendarEventOccurrence(
                            event = event,
                            occurrenceDate = cursor,
                            startTime = startTime,
                            endTime = endTime
                        )
                    }
                    cursor = when (freq) {
                        DAILY -> cursor.plusDays(1)
                        WEEKLY -> cursor.plusWeeks(1)
                        MONTHLY -> cursor.plusMonths(1)
                        YEARLY -> cursor.plusYears(1)
                        else -> break
                    }
                    guard++
                }
            }
        }

        return results.sortedWith(
            compareBy<CalendarEventOccurrence>(
                { it.occurrenceDate },
                { !it.event.isAllDay },
                { it.startTime ?: LocalTime.MIN },
                { it.event.title.lowercase() }
            )
        )
    }
}
