package com.discipline.alarm.domain.usecase.alarm

import com.discipline.alarm.domain.model.Alarm
import com.discipline.alarm.domain.repository.AlarmRepository
import javax.inject.Inject

class SaveAlarmUseCase @Inject constructor(
    private val alarmRepository: AlarmRepository
) {
    suspend operator fun invoke(alarm: Alarm): Long {
        return if (alarm.id == 0L) {
            alarmRepository.insertAlarm(alarm)
        } else {
            alarmRepository.updateAlarm(alarm)
            alarm.id
        }
    }
}
