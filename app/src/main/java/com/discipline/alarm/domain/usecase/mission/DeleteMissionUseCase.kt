package com.discipline.alarm.domain.usecase.mission

import com.discipline.alarm.domain.model.Mission
import com.discipline.alarm.domain.repository.MissionRepository
import com.discipline.alarm.util.FileStorageManager
import javax.inject.Inject

class DeleteMissionUseCase @Inject constructor(
    private val missionRepository: MissionRepository,
    private val fileStorageManager: FileStorageManager
) {
    suspend operator fun invoke(mission: Mission) {
        missionRepository.deleteMission(mission)
        fileStorageManager.deleteImages(mission.imagePaths)
    }
}
