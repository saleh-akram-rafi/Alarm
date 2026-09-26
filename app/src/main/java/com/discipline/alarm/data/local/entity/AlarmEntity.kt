package com.discipline.alarm.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "alarms",
    foreignKeys = [
        ForeignKey(
            entity = MissionEntity::class,
            parentColumns = ["id"],
            childColumns = ["missionId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["missionId"])]
)
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val label: String,
    val enabled: Boolean = true,
    val repeatDays: String = "",
    val soundUri: String = "",
    val snoozeEnabled: Boolean = false,
    val snoozeMinutes: Int = 5,
    val snoozeCount: Int = 0,
    val maxSnoozeCount: Int = 1,
    val missionId: Long? = null,
    val threshold: Float = 0.75f,
    val vibrate: Boolean = true,
    val volume: Float = 1.0f,
    val createdAt: Long = System.currentTimeMillis()
)
