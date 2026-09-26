package com.discipline.alarm.domain.usecase.alarm

import com.discipline.alarm.domain.model.Alarm
import com.discipline.alarm.domain.repository.AlarmRepository
import javax.inject.Inject

class GetNextActiveAlarmUseCase @Inject constructor(
    private val alarmRepository: AlarmRepository
) {
    suspend operator fun invoke(): Alarm? {
        return alarmRepository.getNextActiveAlarm()
    }
}
