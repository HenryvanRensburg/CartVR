package com.cartvr.app.domain

import java.time.LocalDate

object TaskRecurrence {
    const val DAILY = "DAILY"
    const val WEEKLY = "WEEKLY"
    const val MONTHLY = "MONTHLY"
    const val YEARLY = "YEARLY"
    const val AFTER_COMPLETION_PREFIX = "AFTER_COMPLETION:"

    fun afterCompletionRule(days: Int): String =
        "$AFTER_COMPLETION_PREFIX${days.coerceIn(1, 3650)}"

    fun parseAfterCompletionDays(rule: String?): Int? {
        if (rule.isNullOrBlank()) return null
        if (!rule.startsWith(AFTER_COMPLETION_PREFIX, ignoreCase = true)) return null
        return rule.substringAfter(AFTER_COMPLETION_PREFIX).trim().toIntOrNull()?.coerceIn(1, 3650)
    }

    fun computeNextDueDate(
        rule: String?,
        currentDueDateIso: String?,
        completedDate: LocalDate = LocalDate.now()
    ): String? {
        if (rule.isNullOrBlank()) return null

        val afterDays = parseAfterCompletionDays(rule)
        if (afterDays != null) {
            return completedDate.plusDays(afterDays.toLong()).toString()
        }

        var next = currentDueDateIso
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: completedDate

        when (rule.trim().uppercase()) {
            DAILY -> {
                next = if (next.isBefore(completedDate)) completedDate else next
                next = next.plusDays(1)
            }
            WEEKLY -> {
                next = next.plusWeeks(1)
                while (!next.isAfter(completedDate)) {
                    next = next.plusWeeks(1)
                }
            }
            MONTHLY -> {
                next = next.plusMonths(1)
                while (!next.isAfter(completedDate)) {
                    next = next.plusMonths(1)
                }
            }
            YEARLY -> {
                next = next.plusYears(1)
                while (!next.isAfter(completedDate)) {
                    next = next.plusYears(1)
                }
            }
            else -> return null
        }

        return next.toString()
    }

    fun formatLabel(rule: String?): String {
        if (rule.isNullOrBlank()) return "Does not repeat"
        val afterDays = parseAfterCompletionDays(rule)
        if (afterDays != null) {
            return if (afterDays == 1) "1 day after completion" else "$afterDays days after completion"
        }
        return when (rule.trim().uppercase()) {
            DAILY -> "Daily"
            WEEKLY -> "Weekly"
            MONTHLY -> "Monthly"
            YEARLY -> "Yearly"
            else -> "Repeats"
        }
    }
}
