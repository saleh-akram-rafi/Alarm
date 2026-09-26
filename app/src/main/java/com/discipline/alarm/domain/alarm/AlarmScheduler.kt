package com.discipline.alarm.domain.alarm

import com.discipline.alarm.domain.model.Alarm

interface AlarmScheduler {
    fun schedule(alarm: Alarm)
    fun cancel(alarm: Alarm)
    fun scheduleSnooze(alarm: Alarm)
    fun canScheduleExactAlarms(): Boolean
}
