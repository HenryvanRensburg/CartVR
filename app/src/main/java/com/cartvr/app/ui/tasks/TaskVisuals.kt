package com.cartvr.app.ui.tasks

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.cartvr.app.data.local.entity.HouseholdProfileEntity
import com.cartvr.app.ui.components.ListIconOption
import com.cartvr.app.ui.components.ListVisuals
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object TaskVisuals {
    val iconOptions = listOf(
        ListIconOption("checklist", "Tasks", Icons.Rounded.Checklist),
        ListIconOption("home", "Home", Icons.Rounded.Home),
        ListIconOption("receipt", "Bills", Icons.Rounded.ReceiptLong),
        ListIconOption("directions_car", "Errands", Icons.Rounded.DirectionsCar),
        ListIconOption("family", "Family", Icons.Rounded.FamilyRestroom),
        ListIconOption("person", "Personal", Icons.Rounded.Person),
        ListIconOption("build", "Repair", Icons.Rounded.Build),
        ListIconOption("work", "Admin", Icons.Rounded.Work),
        ListIconOption("star", "Goals", Icons.Rounded.Star),
        ListIconOption("event", "Projects", Icons.Rounded.Event)
    )

    private val shortDateFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

    fun iconFor(key: String): ImageVector =
        iconOptions.firstOrNull { it.key == key }?.icon ?: Icons.Rounded.Checklist

    fun priorityLabel(priority: String): String = when (priority.lowercase()) {
        "urgent" -> "Urgent"
        "low" -> "Low"
        else -> "Normal"
    }

    fun priorityColor(priority: String): Color = when (priority.lowercase()) {
        "urgent" -> Color(0xFFEF4444)
        "low" -> Color(0xFF64748B)
        else -> Color(0xFF0D9488)
    }

    fun statusLabel(status: String): String = when (status.lowercase()) {
        "in_progress" -> "In Progress"
        "done" -> "Done"
        else -> "To Do"
    }

    fun memberName(userId: String?, profiles: List<HouseholdProfileEntity>): String {
        if (userId.isNullOrBlank()) return "Anyone"
        return profiles.firstOrNull { it.userId == userId }?.displayName ?: when (userId) {
            "862590df-507f-4c03-9417-f3703b8327c2" -> "Henry"
            "fc1b039d-1b1a-4dce-bbc4-a4f8dd5a94d2" -> "Jolene"
            else -> "Family"
        }
    }

    fun memberColor(userId: String?, profiles: List<HouseholdProfileEntity>): Color {
        if (userId.isNullOrBlank()) return Color(0xFF2563EB)
        val hex = profiles.firstOrNull { it.userId == userId }?.accentColor
            ?: when (userId) {
                "862590df-507f-4c03-9417-f3703b8327c2" -> "#0D9488"
                "fc1b039d-1b1a-4dce-bbc4-a4f8dd5a94d2" -> "#9333EA"
                else -> "#2563EB"
            }
        return ListVisuals.parseColor(hex)
    }

    fun formatDueLabel(
        dueDate: String?,
        dueTime: String?,
        isDone: Boolean = false
    ): Pair<String, Boolean>? {
        if (dueDate.isNullOrBlank()) return null
        val parsed = runCatching { LocalDate.parse(dueDate) }.getOrNull() ?: return null
        val today = LocalDate.now()
        val isOverdue = !isDone && parsed.isBefore(today)
        val dateText = when {
            parsed == today -> "Today"
            parsed == today.plusDays(1) -> "Tomorrow"
            parsed == today.minusDays(1) -> "Yesterday"
            else -> parsed.format(shortDateFormatter)
        }
        val withTime = if (!dueTime.isNullOrBlank()) "$dateText • $dueTime" else dateText
        val finalLabel = if (isOverdue) "Overdue • $withTime" else withTime
        return finalLabel to isOverdue
    }
}
