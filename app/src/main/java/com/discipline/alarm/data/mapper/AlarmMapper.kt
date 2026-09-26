package com.discipline.alarm.data.mapper

import com.discipline.alarm.data.local.entity.AlarmEntity
import com.discipline.alarm.data.local.entity.AlarmWithMission
import com.discipline.alarm.domain.model.Alarm
import com.discipline.alarm.domain.model.DayOfWeek

fun AlarmEntity.toDomain(): Alarm {
    return Alarm(
        id = id,
        hour = hour,
        minute = minute,
        label = label,
        enabled = enabled,
        repeatDays = DayOfWeek.fromCommaSeparated(repeatDays),
        soundUri = soundUri,
        snoozeEnabled = snoozeEnabled,
        snoozeMinutes = snoozeMinutes,
        snoozeCount = snoozeCount,
        maxSnoozeCount = maxSnoozeCount,
        missionId = missionId,
        mission = null,
        threshold = threshold,
        vibrate = vibrate,
        volume = volume,
        createdAt = createdAt
    )
}

fun AlarmWithMission.toDomain(): Alarm {
    return Alarm(
        id = alarm.id,
        hour = alarm.hour,
        minute = alarm.minute,
        label = alarm.label,
        enabled = alarm.enabled,
        repeatDays = DayOfWeek.fromCommaSeparated(alarm.repeatDays),
        soundUri = alarm.soundUri,
        snoozeEnabled = alarm.snoozeEnabled,
        snoozeMinutes = alarm.snoozeMinutes,
        snoozeCount = alarm.snoozeCount,
        maxSnoozeCount = alarm.maxSnoozeCount,
        missionId = alarm.missionId,
        mission = mission?.toDomain(),
        threshold = alarm.threshold,
        vibrate = alarm.vibrate,
        volume = alarm.volume,
        createdAt = alarm.createdAt
    )
}

fun Alarm.toEntity(): AlarmEntity {
    return AlarmEntity(
        id = id,
        hour = hour,
        minute = minute,
        label = label,
        enabled = enabled,
        repeatDays = DayOfWeek.toCommaSeparated(repeatDays),
        soundUri = soundUri,
        snoozeEnabled = snoozeEnabled,
        snoozeMinutes = snoozeMinutes,
        snoozeCount = snoozeCount,
        maxSnoozeCount = maxSnoozeCount,
        missionId = missionId,
        threshold = threshold,
        vibrate = vibrate,
        volume = volume,
        createdAt = createdAt
    )
}
