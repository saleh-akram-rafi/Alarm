package com.discipline.alarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.discipline.alarm.domain.usecase.alarm.DismissAlarmUseCase
import com.discipline.alarm.domain.usecase.alarm.GetAlarmByIdUseCase
import com.discipline.alarm.domain.usecase.alarm.SnoozeAlarmUseCase
import com.discipline.alarm.service.AlarmForegroundService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
        const val ACTION_TRIGGER_ALARM = "com.discipline.alarm.ACTION_TRIGGER_ALARM"
        const val ACTION_DISMISS_ALARM = "com.discipline.alarm.ACTION_DISMISS_ALARM"
        const val ACTION_SNOOZE_ALARM = "com.discipline.alarm.ACTION_SNOOZE_ALARM"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
    }

    @Inject
    lateinit var getAlarmByIdUseCase: GetAlarmByIdUseCase

    @Inject
    lateinit var dismissAlarmUseCase: DismissAlarmUseCase

    @Inject
    lateinit var snoozeAlarmUseCase: SnoozeAlarmUseCase

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        val action = intent.action
        Log.d(TAG, "onReceive: action=$action, alarmId=$alarmId")

        if (alarmId == -1L && action != ACTION_DISMISS_ALARM) {
            Log.e(TAG, "Invalid alarmId: -1")
            return
        }

        // Acquire a temporary wake lock (up to 10 seconds)
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "DisciplineAlarm:AlarmReceiverWakeLock"
        )?.apply {
            acquire(10_000L) // 10 seconds timeout
        }

        when (action) {
            ACTION_TRIGGER_ALARM -> {
                com.discipline.alarm.util.ActiveAlarmStore.setActiveAlarm(context, alarmId)

                // 1. Start Foreground Service immediately for audio + fullScreenIntent notification
                val serviceIntent = Intent(context, AlarmForegroundService::class.java).apply {
                    this.action = AlarmForegroundService.ACTION_START_ALARM
                    putExtra(AlarmForegroundService.EXTRA_ALARM_ID, alarmId)
                }
                ContextCompat.startForegroundService(context, serviceIntent)

                // 2. Also launch AlarmActivity directly while AlarmManager BAL exemption is active
                try {
                    val fullScreenIntent = Intent(
                        context,
                        com.discipline.alarm.presentation.trigger.AlarmActivity::class.java
                    ).apply {
                        putExtra(com.discipline.alarm.presentation.trigger.AlarmActivity.EXTRA_ALARM_ID, alarmId)
                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                Intent.FLAG_ACTIVITY_NO_USER_ACTION
                        )
                    }
                    context.startActivity(fullScreenIntent)
                } catch (e: Exception) {
                    Log.w(TAG, "Direct activity start deferred to FullScreenIntent notification", e)
                }

                // Do NOT release wakeLock here! It was acquired with a 10_000L (10s) timeout
                // so the CPU stays awake while AlarmForegroundService starts and acquires its own WakeLock.
                return
            }

            ACTION_DISMISS_ALARM -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val alarm = getAlarmByIdUseCase(alarmId)
                        if (alarm != null) {
                            dismissAlarmUseCase(alarm)
                        }
                        com.discipline.alarm.util.ActiveAlarmStore.clearActiveAlarm(context)
                        val serviceIntent = Intent(context, AlarmForegroundService::class.java).apply {
                            this.action = AlarmForegroundService.ACTION_STOP_ALARM
                            putExtra(AlarmForegroundService.EXTRA_VERIFIED_DISMISS, true)
                        }
                        context.startService(serviceIntent)
                    } finally {
                        wakeLock?.let { if (it.isHeld) it.release() }
                        pendingResult.finish()
                    }
                }
                return
            }

            ACTION_SNOOZE_ALARM -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val alarm = getAlarmByIdUseCase(alarmId)
                        if (alarm != null) {
                            snoozeAlarmUseCase(alarm)
                        }
                        com.discipline.alarm.util.ActiveAlarmStore.clearActiveAlarm(context)
                        val serviceIntent = Intent(context, AlarmForegroundService::class.java).apply {
                            this.action = AlarmForegroundService.ACTION_STOP_ALARM
                            putExtra(AlarmForegroundService.EXTRA_VERIFIED_DISMISS, true)
                        }
                        context.startService(serviceIntent)
                    } finally {
                        wakeLock?.let { if (it.isHeld) it.release() }
                        pendingResult.finish()
                    }
                }
                return
            }
        }

        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
    }
}
