package com.discipline.alarm.domain.usecase.alarm

import com.discipline.alarm.domain.alarm.AlarmScheduler
import com.discipline.alarm.domain.model.Alarm
import com.discipline.alarm.domain.repository.AlarmRepository
import javax.inject.Inject

class ScheduleAlarmUseCase @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke(alarm: Alarm): Long {
        val id = if (alarm.id == 0L) {
            alarmRepository.insertAlarm(alarm)
        } else {
            alarmRepository.updateAlarm(alarm)
            alarm.id
        }

        val updatedAlarm = alarm.copy(id = id)
        if (updatedAlarm.enabled) {
            alarmScheduler.schedule(updatedAlarm)
        } else {
            alarmScheduler.cancel(updatedAlarm)
        }

        return id
    }
}
