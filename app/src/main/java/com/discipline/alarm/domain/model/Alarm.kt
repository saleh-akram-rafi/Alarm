package com.discipline.alarm.domain.model

import java.util.Locale

data class Alarm(
    val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "Morning Discipline",
    val enabled: Boolean = true,
    val repeatDays: Set<DayOfWeek> = emptySet(),
    val soundUri: String = "",
    val snoozeEnabled: Boolean = false,
    val snoozeMinutes: Int = 5,
    val snoozeCount: Int = 0,
    val maxSnoozeCount: Int = 1,
    val missionId: Long? = null,
    val mission: Mission? = null,
    val threshold: Float = 0.75f,
    val vibrate: Boolean = true,
    val volume: Float = 1.0f,
    val createdAt: Long = System.currentTimeMillis()
) {
    val formattedTime: String
        get() {
            val isPm = hour >= 12
            val displayHour = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            val amPm = if (isPm) "PM" else "AM"
            return String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, minute, amPm)
        }

    val repeatSummary: String
        get() {
            if (repeatDays.isEmpty()) return "Once"
            if (repeatDays.size == 7) return "Every day"
            val weekdays = setOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY
            )
            val weekend = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
            if (repeatDays == weekdays) return "Weekdays"
            if (repeatDays == weekend) return "Weekends"
            return repeatDays.sortedBy { it.ordinal }.joinToString(", ") { it.shortName }
        }

    val isRepeating: Boolean
        get() = repeatDays.isNotEmpty()

    val hasMission: Boolean
        get() = missionId != null && missionId > 0
}
