package com.discipline.alarm.domain.usecase.alarm

import com.discipline.alarm.domain.alarm.AlarmScheduler
import com.discipline.alarm.domain.model.Alarm
import com.discipline.alarm.domain.repository.AlarmRepository
import javax.inject.Inject

class DismissAlarmUseCase @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke(alarm: Alarm) {
        alarmRepository.resetSnoozeCount(alarm.id)

        if (alarm.isRepeating) {
            // Re-schedule for the next repeating day
            alarmScheduler.schedule(alarm)
        } else {
            // One-shot alarm: disable it
            alarmRepository.setAlarmEnabled(alarm.id, false)
            alarmScheduler.cancel(alarm)
        }
    }
}
