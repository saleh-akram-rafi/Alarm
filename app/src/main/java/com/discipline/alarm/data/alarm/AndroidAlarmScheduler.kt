package com.discipline.alarm.data.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.discipline.alarm.MainActivity
import com.discipline.alarm.domain.alarm.AlarmScheduler
import com.discipline.alarm.domain.model.Alarm
import com.discipline.alarm.receiver.AlarmReceiver
import com.discipline.alarm.util.DateTimeUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) : AlarmScheduler {

    companion object {
        private const val TAG = "AndroidAlarmScheduler"
    }

    private val alarmManager: AlarmManager? =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    override fun schedule(alarm: Alarm) {
        if (!alarm.enabled) {
            cancel(alarm)
            return
        }

        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager not available")
            return
        }

        val triggerMillis = DateTimeUtils.calculateNextTriggerMillis(alarm)
        scheduleExactAlarmClock(alarm.id, triggerMillis, "regular alarm")
    }

    override fun cancel(alarm: Alarm) {
        if (alarmManager == null) return
        val pendingIntent = createAlarmPendingIntent(alarm.id)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        Log.d(TAG, "Alarm cancelled for id=${alarm.id}")
    }

    override fun scheduleSnooze(alarm: Alarm) {
        if (alarmManager == null) return

        val triggerMillis = DateTimeUtils.calculateSnoozeTriggerMillis(alarm.snoozeMinutes)
        scheduleExactAlarmClock(alarm.id, triggerMillis, "snooze (${alarm.snoozeMinutes}m)")
    }

    private fun scheduleExactAlarmClock(alarmId: Long, triggerMillis: Long, reason: String) {
        val manager = alarmManager ?: return
        val pendingIntent = createAlarmPendingIntent(alarmId)
        val showIntent = createShowIntent()

        // Cancel any previous alarm with the same PendingIntent before setting the new one
        manager.cancel(pendingIntent)

        try {
            // setAlarmClock is the highest-priority OS wakeup mechanism on Android:
            // It wakes the CPU out of Deep Doze at the exact second (0ms batching delay)
            // and grants background activity launch + foreground service exemptions.
            manager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerMillis, showIntent),
                pendingIntent
            )
            Log.d(
                TAG,
                "Exact AlarmClock scheduled ($reason) for id=$alarmId at $triggerMillis (${DateTimeUtils.getCountdownString(triggerMillis)})"
            )
        } catch (se: SecurityException) {
            Log.e(TAG, "SecurityException in setAlarmClock for id=$alarmId, trying setExactAndAllowWhileIdle", se)
            try {
                manager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            } catch (e2: Exception) {
                Log.e(TAG, "Fallback setExactAndAllowWhileIdle failed for id=$alarmId", e2)
                manager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            }
        }
    }

    override fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager?.canScheduleExactAlarms() ?: false
        } else {
            true
        }
    }

    private fun createAlarmPendingIntent(alarmId: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER_ALARM
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            // Crucial: Forces Android to deliver this broadcast in the real-time
            // foreground broadcast queue instead of delaying it in the background queue!
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        }
        return PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createShowIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
