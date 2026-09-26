package com.discipline.alarm.data.repository

import com.discipline.alarm.data.local.dao.AlarmDao
import com.discipline.alarm.data.mapper.toDomain
import com.discipline.alarm.data.mapper.toEntity
import com.discipline.alarm.domain.model.Alarm
import com.discipline.alarm.domain.repository.AlarmRepository
import com.discipline.alarm.util.DateTimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmRepositoryImpl @Inject constructor(
    private val alarmDao: AlarmDao
) : AlarmRepository {

    override fun getAllAlarms(): Flow<List<Alarm>> {
        return alarmDao.getAllAlarms().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getAlarmById(id: Long): Alarm? {
        return alarmDao.getAlarmById(id)?.toDomain()
    }

    override suspend fun getNextActiveAlarm(): Alarm? {
        val alarms = getAllAlarms().first().filter { it.enabled }
        if (alarms.isEmpty()) return null

        val now = System.currentTimeMillis()
        return alarms.minByOrNull { alarm ->
            DateTimeUtils.calculateNextTriggerMillis(alarm, now)
        }
    }

    override suspend fun insertAlarm(alarm: Alarm): Long {
        return alarmDao.insertAlarm(alarm.toEntity())
    }

    override suspend fun updateAlarm(alarm: Alarm) {
        alarmDao.updateAlarm(alarm.toEntity())
    }

    override suspend fun deleteAlarm(alarm: Alarm) {
        alarmDao.deleteAlarm(alarm.toEntity())
    }

    override suspend fun setAlarmEnabled(id: Long, enabled: Boolean) {
        alarmDao.setAlarmEnabled(id, enabled)
    }

    override suspend fun incrementSnoozeCount(id: Long) {
        alarmDao.incrementSnoozeCount(id)
    }

    override suspend fun resetSnoozeCount(id: Long) {
        alarmDao.resetSnoozeCount(id)
    }
}
