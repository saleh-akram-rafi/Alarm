package com.discipline.alarm.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.discipline.alarm.MainActivity
import com.discipline.alarm.receiver.AlarmReceiver

/**
 * Persists the currently ringing alarm state in SharedPreferences synchronously
 * so that if the user swipes the app away from Recent Tasks or the OS kills the process,
 * the alarm knows it has NOT been verified/dismissed yet and immediately revives itself.
 */
object ActiveAlarmStore {
    private const val TAG = "ActiveAlarmStore"
    private const val PREFS_NAME = "discipline_active_alarm_prefs"
    private const val KEY_ACTIVE_ALARM_ID = "active_alarm_id"
    private const val KEY_IS_RINGING = "is_alarm_ringing"

    fun setActiveAlarm(context: Context, alarmId: Long) {
        if (alarmId <= 0L) return
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_ACTIVE_ALARM_ID, alarmId)
            .putBoolean(KEY_IS_RINGING, true)
            .commit()
    }

    fun clearActiveAlarm(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_ACTIVE_ALARM_ID)
            .putBoolean(KEY_IS_RINGING, false)
            .commit()
        cancelWatchdogRevival(context)
    }

    fun isAlarmRinging(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_RINGING, false) && prefs.getLong(KEY_ACTIVE_ALARM_ID, -1L) > 0L
    }

    fun getActiveAlarmId(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return if (prefs.getBoolean(KEY_IS_RINGING, false)) {
            prefs.getLong(KEY_ACTIVE_ALARM_ID, -1L)
        } else {
            -1L
        }
    }

    /**
     * Schedules an immediate (800ms) high-priority AlarmClock wakeup to revive
     * AlarmForegroundService if the user swipes the app from Recent Tasks.
     */
    fun scheduleImmediateRevival(context: Context, alarmId: Long) {
        if (alarmId <= 0L) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerAt = System.currentTimeMillis() + 800L
        val pendingIntent = createRevivalPendingIntent(context, alarmId)
        val showIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAt, showIntent),
                pendingIntent
            )
            Log.d(TAG, "Scheduled immediate 800ms anti-kill revival for alarmId=$alarmId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule revival AlarmClock, trying setExactAndAllowWhileIdle", e)
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            } catch (_: Exception) {
            }
        }
    }

    fun cancelWatchdogRevival(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER_ALARM
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            999_999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun createRevivalPendingIntent(context: Context, alarmId: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER_ALARM
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        }
        return PendingIntent.getBroadcast(
            context,
            999_999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
