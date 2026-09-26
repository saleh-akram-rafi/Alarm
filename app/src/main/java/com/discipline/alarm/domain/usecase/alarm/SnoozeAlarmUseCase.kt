package com.discipline.alarm.domain.usecase.alarm

import com.discipline.alarm.domain.alarm.AlarmScheduler
import com.discipline.alarm.domain.model.Alarm
import com.discipline.alarm.domain.repository.AlarmRepository
import javax.inject.Inject

class SnoozeAlarmUseCase @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke(alarm: Alarm): Boolean {
        if (!alarm.snoozeEnabled || alarm.snoozeCount >= alarm.maxSnoozeCount) {
            return false
        }
        alarmRepository.incrementSnoozeCount(alarm.id)
        alarmScheduler.scheduleSnooze(alarm)
        return true
    }
}
