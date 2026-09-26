package com.discipline.alarm.domain

import com.discipline.alarm.domain.alarm.AlarmScheduler
import com.discipline.alarm.domain.model.Alarm
import com.discipline.alarm.domain.repository.AlarmRepository
import com.discipline.alarm.domain.usecase.alarm.SnoozeAlarmUseCase
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SnoozeAlarmUseCaseTest {

    private lateinit var alarmRepository: AlarmRepository
    private lateinit var alarmScheduler: AlarmScheduler
    private lateinit var useCase: SnoozeAlarmUseCase

    @Before
    fun setup() {
        alarmRepository = mockk(relaxed = true)
        alarmScheduler = mockk(relaxed = true)
        useCase = SnoozeAlarmUseCase(alarmRepository, alarmScheduler)
    }

    @Test
    fun testSnoozeAllowed_IncrementsCountAndSchedules() = runTest {
        val alarm = Alarm(
            id = 12L,
            hour = 6,
            minute = 0,
            snoozeEnabled = true,
            snoozeCount = 0,
            maxSnoozeCount = 1
        )

        val result = useCase(alarm)

        assertTrue(result)
        coVerify(exactly = 1) { alarmRepository.incrementSnoozeCount(12L) }
        verify(exactly = 1) { alarmScheduler.scheduleSnooze(alarm) }
    }

    @Test
    fun testSnoozeDisabled_ReturnsFalse() = runTest {
        val alarm = Alarm(
            id = 12L,
            hour = 6,
            minute = 0,
            snoozeEnabled = false
        )

        val result = useCase(alarm)

        assertFalse(result)
        coVerify(exactly = 0) { alarmRepository.incrementSnoozeCount(any()) }
        verify(exactly = 0) { alarmScheduler.scheduleSnooze(any()) }
    }

    @Test
    fun testSnoozeLimitReached_ReturnsFalse() = runTest {
        val alarm = Alarm(
            id = 12L,
            hour = 6,
            minute = 0,
            snoozeEnabled = true,
            snoozeCount = 1,
            maxSnoozeCount = 1
        )

        val result = useCase(alarm)

        assertFalse(result)
        coVerify(exactly = 0) { alarmRepository.incrementSnoozeCount(any()) }
        verify(exactly = 0) { alarmScheduler.scheduleSnooze(any()) }
    }
}
