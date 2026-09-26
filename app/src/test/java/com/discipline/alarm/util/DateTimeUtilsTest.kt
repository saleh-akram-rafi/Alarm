package com.discipline.alarm.util

import com.discipline.alarm.domain.model.Alarm
import com.discipline.alarm.domain.model.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DateTimeUtilsTest {

    @Test
    fun testOneShotAlarm_ScheduledLaterToday() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 24, 8, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Alarm is set for 9:30 AM today
        val alarm = Alarm(hour = 9, minute = 30, repeatDays = emptySet())
        val nextTrigger = DateTimeUtils.calculateNextTriggerMillis(alarm, now)

        val expected = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 24, 9, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        assertEquals(expected, nextTrigger)
    }

    @Test
    fun testOneShotAlarm_TimePassed_ScheduledTomorrow() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 24, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Alarm set for 6:00 AM (already passed today)
        val alarm = Alarm(hour = 6, minute = 0, repeatDays = emptySet())
        val nextTrigger = DateTimeUtils.calculateNextTriggerMillis(alarm, now)

        val expected = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 25, 6, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        assertEquals(expected, nextTrigger)
    }

    @Test
    fun testRepeatingAlarm_NearestDay() {
        // Thursday Sep 24, 2026
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 24, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Repeat on Friday and Monday at 7:00 AM
        val alarm = Alarm(
            hour = 7,
            minute = 0,
            repeatDays = setOf(DayOfWeek.FRIDAY, DayOfWeek.MONDAY)
        )
        val nextTrigger = DateTimeUtils.calculateNextTriggerMillis(alarm, now)

        // Nearest should be Friday Sep 25, 2026 at 7:00 AM
        val expected = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 25, 7, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        assertEquals(expected, nextTrigger)
    }

    @Test
    fun testCountdownString() {
        val now = 1000000000L
        val trigger = now + (2 * 3600 * 1000L) + (15 * 60 * 1000L) // 2 hr 15 min
        val countdown = DateTimeUtils.getCountdownString(trigger, now)

        assertTrue(countdown.contains("2 hr 15 min"))
    }
}
