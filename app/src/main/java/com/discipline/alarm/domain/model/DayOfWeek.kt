package com.discipline.alarm.domain.model

import java.util.Calendar

enum class DayOfWeek(val calendarDay: Int, val shortName: String, val bit: Int) {
    MONDAY(Calendar.MONDAY, "Mon", 1),
    TUESDAY(Calendar.TUESDAY, "Tue", 2),
    WEDNESDAY(Calendar.WEDNESDAY, "Wed", 4),
    THURSDAY(Calendar.THURSDAY, "Thu", 8),
    FRIDAY(Calendar.FRIDAY, "Fri", 16),
    SATURDAY(Calendar.SATURDAY, "Sat", 32),
    SUNDAY(Calendar.SUNDAY, "Sun", 64);

    companion object {
        fun fromCalendarDay(calendarDay: Int): DayOfWeek? {
            return entries.firstOrNull { it.calendarDay == calendarDay }
        }

        fun fromStringList(days: List<String>): Set<DayOfWeek> {
            return days.mapNotNull { name ->
                entries.firstOrNull { it.name.equals(name, ignoreCase = true) || it.shortName.equals(name, ignoreCase = true) }
            }.toSet()
        }

        fun toCommaSeparated(days: Set<DayOfWeek>): String {
            return days.sortedBy { it.ordinal }.joinToString(",") { it.name }
        }

        fun fromCommaSeparated(raw: String): Set<DayOfWeek> {
            if (raw.isBlank()) return emptySet()
            return raw.split(",")
                .map { it.trim() }
                .mapNotNull { name ->
                    entries.firstOrNull { it.name.equals(name, ignoreCase = true) || it.shortName.equals(name, ignoreCase = true) }
                }
                .toSet()
        }
    }
}
