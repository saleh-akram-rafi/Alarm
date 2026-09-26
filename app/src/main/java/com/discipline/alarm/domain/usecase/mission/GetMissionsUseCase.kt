package com.discipline.alarm.domain.usecase.mission

import com.discipline.alarm.domain.model.Mission
import com.discipline.alarm.domain.repository.MissionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMissionsUseCase @Inject constructor(
    private val missionRepository: MissionRepository
) {
    operator fun invoke(): Flow<List<Mission>> {
        return missionRepository.getAllMissions()
    }
}
