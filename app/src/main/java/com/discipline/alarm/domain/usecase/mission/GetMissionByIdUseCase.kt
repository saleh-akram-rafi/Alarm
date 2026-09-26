package com.discipline.alarm.domain.usecase.mission

import com.discipline.alarm.domain.model.Mission
import com.discipline.alarm.domain.repository.MissionRepository
import javax.inject.Inject

class GetMissionByIdUseCase @Inject constructor(
    private val missionRepository: MissionRepository
) {
    suspend operator fun invoke(id: Long): Mission? {
        return missionRepository.getMissionById(id)
    }
}
