package com.cartvr.app.ui.calendar

import androidx.compose.ui.graphics.Color
import com.cartvr.app.data.local.entity.CalendarEventEntity
import com.cartvr.app.data.local.entity.HouseholdProfileEntity
import com.cartvr.app.domain.CalendarEventOccurrence
import com.cartvr.app.ui.components.ListVisuals
import java.time.format.DateTimeFormatter
import java.util.Locale

object CalendarVisuals {
    const val FAMILY_COLOR_HEX = "#2563EB"

    val memberColorOptions = listOf(
        "#0D9488", // Teal (Henry default)
        "#9333EA", // Purple (Jolene default)
        "#2563EB", // Blue (Family)
        "#E11D48", // Rose
        "#D97706", // Amber
        "#10B981", // Emerald
        "#6366F1", // Indigo
        "#0284C7"  // Sky
    )

    val reminderOptions: List<Pair<Int?, String>> = listOf(
        null to "Phone default",
        -1 to "None",
        15 to "15 min",
        30 to "30 min",
        60 to "1 hr",
        1440 to "1 day"
    )

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

    fun assignedIds(event: CalendarEventEntity): List<String> =
        event.assignedMemberIdsCsv.split(",").filter { it.isNotBlank() }

    fun resolveEventColor(
        event: CalendarEventEntity,
        profiles: List<HouseholdProfileEntity>
    ): Color {
        if (!event.colorHex.isNullOrBlank()) {
            return ListVisuals.parseColor(event.colorHex)
        }
        if (!event.isShared) {
            val ownerHex = profiles.firstOrNull { it.userId == event.ownerId }?.accentColor
            return ListVisuals.parseColor(ownerHex ?: "#0D9488")
        }
        val assigned = assignedIds(event)
        if (assigned.size == 1) {
            val memberHex = profiles.firstOrNull { it.userId == assigned.first() }?.accentColor
                ?: when (assigned.first()) {
                    "862590df-507f-4c03-9417-f3703b8327c2" -> "#0D9488"
                    "fc1b039d-1b1a-4dce-bbc4-a4f8dd5a94d2" -> "#9333EA"
                    else -> FAMILY_COLOR_HEX
                }
            return ListVisuals.parseColor(memberHex)
        }
        return ListVisuals.parseColor(FAMILY_COLOR_HEX)
    }

    fun resolveAssigneeLabel(
        event: CalendarEventEntity,
        profiles: List<HouseholdProfileEntity>
    ): String {
        if (!event.isShared) return "Private"
        val assigned = assignedIds(event)
        if (assigned.isEmpty() || (profiles.isNotEmpty() && assigned.size >= profiles.size)) {
            return "Family"
        }
        return assigned.mapNotNull { id ->
            profiles.firstOrNull { it.userId == id }?.displayName ?: when (id) {
                "862590df-507f-4c03-9417-f3703b8327c2" -> "Henry"
                "fc1b039d-1b1a-4dce-bbc4-a4f8dd5a94d2" -> "Jolene"
                else -> null
            }
        }.ifEmpty { listOf("Family") }.joinToString(" & ")
    }

    fun formatEventTimeRange(occ: CalendarEventOccurrence): String {
        if (occ.event.isAllDay || occ.startTime == null) return "All day"
        val startStr = occ.startTime.format(timeFormatter)
        val endStr = occ.endTime?.format(timeFormatter)
        return if (endStr != null && occ.endTime != occ.startTime) {
            "$startStr – $endStr"
        } else {
            startStr
        }
    }

    fun formatReminderLabel(minutes: Int?): String = when (minutes) {
        null -> "Phone default"
        -1 -> "No reminder"
        15 -> "15 min before"
        30 -> "30 min before"
        60 -> "1 hr before"
        1440 -> "1 day before"
        else -> "$minutes min before"
    }
}
