package com.discipline.alarm.data.mapper

import com.discipline.alarm.data.local.entity.MissionEntity
import com.discipline.alarm.domain.model.Mission

fun MissionEntity.toDomain(): Mission {
    return Mission(
        id = id,
        name = name,
        image1Path = image1Path,
        image2Path = image2Path,
        image3Path = image3Path,
        embedding1 = embedding1,
        embedding2 = embedding2,
        embedding3 = embedding3,
        threshold = threshold,
        createdAt = createdAt
    )
}

fun Mission.toEntity(): MissionEntity {
    return MissionEntity(
        id = id,
        name = name,
        image1Path = image1Path,
        image2Path = image2Path,
        image3Path = image3Path,
        embedding1 = embedding1,
        embedding2 = embedding2,
        embedding3 = embedding3,
        threshold = threshold,
        createdAt = createdAt
    )
}
