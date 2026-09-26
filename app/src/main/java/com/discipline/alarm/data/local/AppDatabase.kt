package com.discipline.alarm.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.discipline.alarm.data.local.converter.Converters
import com.discipline.alarm.data.local.dao.AlarmDao
import com.discipline.alarm.data.local.dao.MissionDao
import com.discipline.alarm.data.local.entity.AlarmEntity
import com.discipline.alarm.data.local.entity.MissionEntity

@Database(
    entities = [AlarmEntity::class, MissionEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun missionDao(): MissionDao

    companion object {
        const val DATABASE_NAME = "discipline_alarm.db"
    }
}
