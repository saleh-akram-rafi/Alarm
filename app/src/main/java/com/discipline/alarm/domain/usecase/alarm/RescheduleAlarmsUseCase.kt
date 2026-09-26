package com.discipline.alarm.domain.usecase.alarm

import com.discipline.alarm.domain.alarm.AlarmScheduler
import com.discipline.alarm.domain.repository.AlarmRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class RescheduleAlarmsUseCase @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke() {
        val alarms = alarmRepository.getAllAlarms().first()
        val enabledAlarms = alarms.filter { it.enabled }
        enabledAlarms.forEach { alarm ->
            alarmScheduler.schedule(alarm)
        }
    }
}
