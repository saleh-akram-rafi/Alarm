package com.discipline.alarm.data.repository

import com.discipline.alarm.data.local.dao.MissionDao
import com.discipline.alarm.data.mapper.toDomain
import com.discipline.alarm.data.mapper.toEntity
import com.discipline.alarm.domain.model.Mission
import com.discipline.alarm.domain.repository.MissionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MissionRepositoryImpl @Inject constructor(
    private val missionDao: MissionDao
) : MissionRepository {

    override fun getAllMissions(): Flow<List<Mission>> {
        return missionDao.getAllMissions().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getMissionById(id: Long): Mission? {
        return missionDao.getMissionById(id)?.toDomain()
    }

    override suspend fun insertMission(mission: Mission): Long {
        return missionDao.insertMission(mission.toEntity())
    }

    override suspend fun updateMission(mission: Mission) {
        missionDao.updateMission(mission.toEntity())
    }

    override suspend fun deleteMission(mission: Mission) {
        missionDao.deleteMission(mission.toEntity())
    }
}
