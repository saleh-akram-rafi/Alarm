package com.discipline.alarm.util

import com.discipline.alarm.domain.model.Alarm
import com.discipline.alarm.domain.model.DayOfWeek
import java.util.Calendar

object DateTimeUtils {

    /**
     * Calculates the exact epoch timestamp in milliseconds when the alarm should next fire.
     */
    fun calculateNextTriggerMillis(alarm: Alarm, fromMillis: Long = System.currentTimeMillis()): Long {
        val nowCal = Calendar.getInstance().apply { timeInMillis = fromMillis }

        if (alarm.repeatDays.isEmpty()) {
            // One-shot alarm
            val targetCal = Calendar.getInstance().apply {
                timeInMillis = fromMillis
                set(Calendar.HOUR_OF_DAY, alarm.hour)
                set(Calendar.MINUTE, alarm.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (targetCal.timeInMillis <= nowCal.timeInMillis) {
                // Time already passed today, schedule for tomorrow
                targetCal.add(Calendar.DAY_OF_YEAR, 1)
            }
            return targetCal.timeInMillis
        }

        // Repeating alarm
        var minTriggerMillis = Long.MAX_VALUE

        for (day in alarm.repeatDays) {
            val candidateCal = Calendar.getInstance().apply {
                timeInMillis = fromMillis
                set(Calendar.HOUR_OF_DAY, alarm.hour)
                set(Calendar.MINUTE, alarm.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val currentDayOfWeek = candidateCal.get(Calendar.DAY_OF_WEEK)
            var daysToAdd = (day.calendarDay - currentDayOfWeek) % 7
            if (daysToAdd < 0) {
                daysToAdd += 7
            }

            candidateCal.add(Calendar.DAY_OF_YEAR, daysToAdd)

            // If today is a match, but the target time has already passed, schedule for next week
            if (candidateCal.timeInMillis <= nowCal.timeInMillis) {
                candidateCal.add(Calendar.DAY_OF_YEAR, 7)
            }

            if (candidateCal.timeInMillis < minTriggerMillis) {
                minTriggerMillis = candidateCal.timeInMillis
            }
        }

        return minTriggerMillis
    }

    /**
     * Calculates epoch timestamp for a snooze alarm.
     */
    fun calculateSnoozeTriggerMillis(snoozeMinutes: Int, fromMillis: Long = System.currentTimeMillis()): Long {
        return fromMillis + (snoozeMinutes * 60 * 1000L)
    }

    /**
     * Produces a human-readable countdown string, e.g. "Alarm in 5 hours 12 minutes".
     */
    fun getCountdownString(triggerMillis: Long, fromMillis: Long = System.currentTimeMillis()): String {
        val diffMillis = triggerMillis - fromMillis
        if (diffMillis <= 0) return "Alarm is due now"

        val totalMinutes = diffMillis / (1000 * 60)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        val days = hours / 24
        val remHours = hours % 24

        return when {
            days > 0 -> "Alarm in $days d $remHours hr $minutes min"
            hours > 0 -> "Alarm in $hours hr $minutes min"
            minutes > 0 -> "Alarm in $minutes min"
            else -> "Alarm in less than a minute"
        }
    }
}
