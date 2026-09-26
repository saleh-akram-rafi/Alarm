package com.discipline.alarm.domain.usecase.alarm

import com.discipline.alarm.domain.model.Alarm
import com.discipline.alarm.domain.repository.AlarmRepository
import javax.inject.Inject

class GetAlarmByIdUseCase @Inject constructor(
    private val alarmRepository: AlarmRepository
) {
    suspend operator fun invoke(id: Long): Alarm? {
        return alarmRepository.getAlarmById(id)
    }
}
