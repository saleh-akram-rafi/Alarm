package com.discipline.alarm.domain

import com.discipline.alarm.domain.model.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DayOfWeekTest {

    @Test
    fun testFromCalendarDay() {
        assertEquals(DayOfWeek.MONDAY, DayOfWeek.fromCalendarDay(Calendar.MONDAY))
        assertEquals(DayOfWeek.SUNDAY, DayOfWeek.fromCalendarDay(Calendar.SUNDAY))
        assertEquals(DayOfWeek.FRIDAY, DayOfWeek.fromCalendarDay(Calendar.FRIDAY))
    }

    @Test
    fun testCommaSeparatedConversion() {
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        val raw = DayOfWeek.toCommaSeparated(days)
        val parsed = DayOfWeek.fromCommaSeparated(raw)

        assertEquals(days, parsed)
    }

    @Test
    fun testEmptyCommaSeparatedConversion() {
        val parsed = DayOfWeek.fromCommaSeparated("")
        assertTrue(parsed.isEmpty())
    }
}
