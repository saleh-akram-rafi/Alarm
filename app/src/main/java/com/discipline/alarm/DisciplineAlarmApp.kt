package com.discipline.alarm

import android.app.Application
import com.discipline.alarm.service.NotificationHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DisciplineAlarmApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize Notification Channels early
        NotificationHelper.createNotificationChannels(this)
    }
}
