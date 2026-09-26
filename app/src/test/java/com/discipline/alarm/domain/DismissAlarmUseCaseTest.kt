package com.discipline.alarm.domain

import com.discipline.alarm.domain.alarm.AlarmScheduler
import com.discipline.alarm.domain.model.Alarm
import com.discipline.alarm.domain.model.DayOfWeek
import com.discipline.alarm.domain.repository.AlarmRepository
import com.discipline.alarm.domain.usecase.alarm.DismissAlarmUseCase
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class DismissAlarmUseCaseTest {

    private lateinit var alarmRepository: AlarmRepository
    private lateinit var alarmScheduler: AlarmScheduler
    private lateinit var useCase: DismissAlarmUseCase

    @Before
    fun setup() {
        alarmRepository = mockk(relaxed = true)
        alarmScheduler = mockk(relaxed = true)
        useCase = DismissAlarmUseCase(alarmRepository, alarmScheduler)
    }

    @Test
    fun testDismissOneShotAlarm_DisablesAndCancels() = runTest {
        val alarm = Alarm(
            id = 20L,
            hour = 6,
            minute = 0,
            repeatDays = emptySet()
        )

        useCase(alarm)

        coVerify(exactly = 1) { alarmRepository.resetSnoozeCount(20L) }
        coVerify(exactly = 1) { alarmRepository.setAlarmEnabled(20L, false) }
        verify(exactly = 1) { alarmScheduler.cancel(alarm) }
    }

    @Test
    fun testDismissRepeatingAlarm_ReschedulesNextOccurrence() = runTest {
        val alarm = Alarm(
            id = 21L,
            hour = 6,
            minute = 0,
            repeatDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        )

        useCase(alarm)

        coVerify(exactly = 1) { alarmRepository.resetSnoozeCount(21L) }
        coVerify(exactly = 0) { alarmRepository.setAlarmEnabled(any(), any()) }
        verify(exactly = 1) { alarmScheduler.schedule(alarm) }
    }
}
