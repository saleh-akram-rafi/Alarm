package com.discipline.alarm.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class AlarmWithMission(
    @Embedded
    val alarm: AlarmEntity,

    @Relation(
        parentColumn = "missionId",
        entityColumn = "id"
    )
    val mission: MissionEntity? = null
)
