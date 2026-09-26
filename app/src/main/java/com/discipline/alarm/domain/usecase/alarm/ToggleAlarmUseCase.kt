package com.discipline.alarm.domain.usecase.alarm

import com.discipline.alarm.domain.repository.AlarmRepository
import javax.inject.Inject

class ToggleAlarmUseCase @Inject constructor(
    private val alarmRepository: AlarmRepository
) {
    suspend operator fun invoke(id: Long, enabled: Boolean) {
        alarmRepository.setAlarmEnabled(id, enabled)
    }
}
