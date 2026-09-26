package com.discipline.alarm.domain.usecase.mission

import com.discipline.alarm.domain.model.Mission
import com.discipline.alarm.domain.repository.MissionRepository
import javax.inject.Inject

class SaveMissionUseCase @Inject constructor(
    private val missionRepository: MissionRepository
) {
    suspend operator fun invoke(mission: Mission): Long {
        return if (mission.id == 0L) {
            missionRepository.insertMission(mission)
        } else {
            missionRepository.updateMission(mission)
            mission.id
        }
    }
}
