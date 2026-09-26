package com.discipline.alarm.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.discipline.alarm.R
import com.discipline.alarm.domain.model.Alarm
import com.discipline.alarm.presentation.trigger.AlarmActivity
import com.discipline.alarm.receiver.AlarmReceiver

object NotificationHelper {

    const val CHANNEL_ID = "discipline_alarm_channel"
    const val NOTIFICATION_ID = 1001

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.alarm_channel_name)
            val descriptionText = context.getString(R.string.alarm_channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH

            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 1000)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
                setSound(null, null) // Sound is handled directly by ExoPlayer for precise volume and looping
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun buildAlarmNotification(
        context: Context,
        alarm: Alarm?,
        fallbackAlarmId: Long = -1L
    ): Notification {
        val alarmId = alarm?.id ?: fallbackAlarmId
        val alarmLabel = alarm?.label?.ifBlank { "Discipline Alarm" } ?: "Discipline Alarm"
        val missionName = alarm?.mission?.name ?: "Physical Object Scan"

        // Full Screen Intent to AlarmActivity
        val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
            putExtra(AlarmActivity.EXTRA_ALARM_ID, alarmId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            alarmId.toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Scan Mission
        val scanIntent = Intent(context, AlarmActivity::class.java).apply {
            putExtra(AlarmActivity.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmActivity.EXTRA_OPEN_SCANNER, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val scanPendingIntent = PendingIntent.getActivity(
            context,
            (alarmId + 1000).toInt(),
            scanIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("WAKE UP — $alarmLabel")
            .setContentText("Mission: $missionName required to dismiss!")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_camera,
                context.getString(R.string.action_scan_mission),
                scanPendingIntent
            )

        // If snooze is permitted and limit not reached
        if (alarm != null && alarm.snoozeEnabled && alarm.snoozeCount < alarm.maxSnoozeCount) {
            val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_SNOOZE_ALARM
                putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            }
            val snoozePendingIntent = PendingIntent.getBroadcast(
                context,
                (alarmId + 2000).toInt(),
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                android.R.drawable.ic_popup_reminder,
                context.getString(R.string.action_snooze),
                snoozePendingIntent
            )
        }

        return builder.build()
    }
}
