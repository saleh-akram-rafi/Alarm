package com.discipline.alarm.domain.repository

import com.discipline.alarm.domain.model.Mission
import kotlinx.coroutines.flow.Flow

interface MissionRepository {
    fun getAllMissions(): Flow<List<Mission>>
    suspend fun getMissionById(id: Long): Mission?
    suspend fun insertMission(mission: Mission): Long
    suspend fun updateMission(mission: Mission)
    suspend fun deleteMission(mission: Mission)
}
