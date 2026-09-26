package com.discipline.alarm.domain

import com.discipline.alarm.domain.alarm.AlarmScheduler
import com.discipline.alarm.domain.model.Alarm
import com.discipline.alarm.domain.repository.AlarmRepository
import com.discipline.alarm.domain.usecase.alarm.ScheduleAlarmUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ScheduleAlarmUseCaseTest {

    private lateinit var alarmRepository: AlarmRepository
    private lateinit var alarmScheduler: AlarmScheduler
    private lateinit var useCase: ScheduleAlarmUseCase

    @Before
    fun setup() {
        alarmRepository = mockk(relaxed = true)
        alarmScheduler = mockk(relaxed = true)
        useCase = ScheduleAlarmUseCase(alarmRepository, alarmScheduler)
    }

    @Test
    fun testScheduleNewAlarm_InsertsAndSchedulesExactAlarm() = runTest {
        val newAlarm = Alarm(
            id = 0L,
            hour = 6,
            minute = 30,
            label = "Morning Discipline",
            enabled = true
        )

        coEvery { alarmRepository.insertAlarm(newAlarm) } returns 101L

        val returnedId = useCase(newAlarm)

        assertEquals(101L, returnedId)
        coVerify(exactly = 1) { alarmRepository.insertAlarm(newAlarm) }
        verify(exactly = 1) { alarmScheduler.schedule(match { it.id == 101L && it.enabled }) }
    }

    @Test
    fun testScheduleDisabledAlarm_CancelsPendingIntent() = runTest {
        val disabledAlarm = Alarm(
            id = 55L,
            hour = 7,
            minute = 0,
            enabled = false
        )

        val returnedId = useCase(disabledAlarm)

        assertEquals(55L, returnedId)
        coVerify(exactly = 1) { alarmRepository.updateAlarm(disabledAlarm) }
        verify(exactly = 1) { alarmScheduler.cancel(match { it.id == 55L }) }
        verify(exactly = 0) { alarmScheduler.schedule(any()) }
    }
}
