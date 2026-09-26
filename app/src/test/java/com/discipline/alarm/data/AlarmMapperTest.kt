package com.discipline.alarm.data

import com.discipline.alarm.data.local.entity.AlarmEntity
import com.discipline.alarm.data.mapper.toDomain
import com.discipline.alarm.data.mapper.toEntity
import com.discipline.alarm.domain.model.Alarm
import com.discipline.alarm.domain.model.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmMapperTest {

    @Test
    fun testAlarmEntityToDomainAndBack() {
        val entity = AlarmEntity(
            id = 42L,
            hour = 6,
            minute = 30,
            label = "Morning Discipline",
            enabled = true,
            repeatDays = "MONDAY,WEDNESDAY,FRIDAY",
            soundUri = "content://media/internal/audio/media/1",
            snoozeEnabled = false,
            snoozeMinutes = 5,
            snoozeCount = 0,
            maxSnoozeCount = 1,
            missionId = 10L,
            threshold = 0.80f,
            vibrate = true,
            volume = 1.0f,
            createdAt = 123456789L
        )

        val domain = entity.toDomain()

        assertEquals(42L, domain.id)
        assertEquals(6, domain.hour)
        assertEquals(30, domain.minute)
        assertEquals("Morning Discipline", domain.label)
        assertTrue(domain.enabled)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), domain.repeatDays)
        assertEquals("06:30 AM", domain.formattedTime)

        val mappedBack = domain.toEntity()
        assertEquals(entity.id, mappedBack.id)
        assertEquals(entity.hour, mappedBack.hour)
        assertEquals(entity.minute, mappedBack.minute)
        assertEquals(entity.repeatDays, mappedBack.repeatDays)
        assertEquals(entity.threshold, mappedBack.threshold, 0.001f)
    }
}
