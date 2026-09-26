package com.discipline.alarm.domain.repository

import com.discipline.alarm.domain.model.Alarm
import kotlinx.coroutines.flow.Flow

interface AlarmRepository {
    fun getAllAlarms(): Flow<List<Alarm>>
    suspend fun getAlarmById(id: Long): Alarm?
    suspend fun getNextActiveAlarm(): Alarm?
    suspend fun insertAlarm(alarm: Alarm): Long
    suspend fun updateAlarm(alarm: Alarm)
    suspend fun deleteAlarm(alarm: Alarm)
    suspend fun setAlarmEnabled(id: Long, enabled: Boolean)
    suspend fun incrementSnoozeCount(id: Long)
    suspend fun resetSnoozeCount(id: Long)
}
