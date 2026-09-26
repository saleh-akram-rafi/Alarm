package com.discipline.alarm.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.ServiceCompat
import com.discipline.alarm.data.audio.AlarmAudioPlayer
import com.discipline.alarm.domain.model.Alarm
import com.discipline.alarm.domain.usecase.alarm.GetAlarmByIdUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AlarmForegroundService : Service() {

    companion object {
        private const val TAG = "AlarmForegroundService"
        const val ACTION_START_ALARM = "com.discipline.alarm.ACTION_START_ALARM"
        const val ACTION_STOP_ALARM = "com.discipline.alarm.ACTION_STOP_ALARM"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_VERIFIED_DISMISS = "extra_verified_dismiss"

        private const val WAKE_LOCK_TIMEOUT_MS = 15 * 60 * 1000L // 15 minutes safety cutoff

        private val _isAlarmActive = MutableStateFlow(false)
        val isAlarmActive: StateFlow<Boolean> = _isAlarmActive.asStateFlow()

        private val _activeAlarm = MutableStateFlow<Alarm?>(null)
        val activeAlarm: StateFlow<Alarm?> = _activeAlarm.asStateFlow()
    }

    @Inject
    lateinit var alarmAudioPlayer: AlarmAudioPlayer

    @Inject
    lateinit var getAlarmByIdUseCase: GetAlarmByIdUseCase

    private var wakeLock: PowerManager.WakeLock? = null
    private var safetyTimeoutJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var currentAlarmId: Long = -1L
    private var isExplicitlyDismissed: Boolean = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "DisciplineAlarm:ForegroundServiceWakeLock"
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val intentAlarmId = intent?.getLongExtra(EXTRA_ALARM_ID, -1L) ?: -1L
        val savedAlarmId = com.discipline.alarm.util.ActiveAlarmStore.getActiveAlarmId(this)
        val resolvedAlarmId = if (intentAlarmId > 0L) intentAlarmId else savedAlarmId

        Log.d(TAG, "onStartCommand: action=$action, intentAlarmId=$intentAlarmId, savedAlarmId=$savedAlarmId")

        when (action) {
            ACTION_START_ALARM, null -> {
                // Note: action == null happens when Android OS automatically restarts START_STICKY service
                // after the user swipes the app away from Recent Tasks!
                if (resolvedAlarmId > 0L) {
                    isExplicitlyDismissed = false
                    currentAlarmId = resolvedAlarmId
                    com.discipline.alarm.util.ActiveAlarmStore.setActiveAlarm(this, resolvedAlarmId)
                    handleStartAlarm(resolvedAlarmId)
                }
            }
            ACTION_STOP_ALARM -> {
                val isVerified = intent.getBooleanExtra(EXTRA_VERIFIED_DISMISS, false)
                if (isVerified) {
                    isExplicitlyDismissed = true
                    com.discipline.alarm.util.ActiveAlarmStore.clearActiveAlarm(this)
                    handleStopAlarm()
                } else {
                    Log.w(TAG, "Ignored unverified stop attempt! Mission object must be scanned to stop alarm.")
                }
            }
            else -> {
                if (resolvedAlarmId > 0L && !isExplicitlyDismissed) {
                    handleStartAlarm(resolvedAlarmId)
                }
            }
        }

        // START_STICKY guarantees that if the system kills the service after a task swipe,
        // Android automatically recreates the service in the background.
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val activeId = if (currentAlarmId > 0L) {
            currentAlarmId
        } else {
            com.discipline.alarm.util.ActiveAlarmStore.getActiveAlarmId(this)
        }

        if (!isExplicitlyDismissed && activeId > 0L) {
            Log.w(TAG, "Task removed from Recents while alarm ($activeId) is unverified! Keeping alarm alive in background.")
            com.discipline.alarm.util.ActiveAlarmStore.setActiveAlarm(this, activeId)
            // Schedule an immediate 800ms AlarmClock revival in case the OEM task manager kills the process
            com.discipline.alarm.util.ActiveAlarmStore.scheduleImmediateRevival(this, activeId)

            // Re-launch AlarmActivity so the user is brought back to the scanner screen
            try {
                val relaunchIntent = Intent(
                    this,
                    com.discipline.alarm.presentation.trigger.AlarmActivity::class.java
                ).apply {
                    putExtra(com.discipline.alarm.presentation.trigger.AlarmActivity.EXTRA_ALARM_ID, activeId)
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_NO_USER_ACTION
                    )
                }
                startActivity(relaunchIntent)
            } catch (e: Exception) {
                Log.w(TAG, "Could not directly relaunch AlarmActivity onTaskRemoved; FullScreenIntent notification remains active", e)
            }
        }
    }

    private fun handleStartAlarm(alarmId: Long) {
        acquireWakeLock()
        _isAlarmActive.value = true

        // 1. Call startForeground SYNCHRONOUSLY inside onStartCommand before any async coroutine
        // so Android OS never throttles or delays the foreground service startup.
        val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else {
            0
        }
        val initialNotification = NotificationHelper.buildAlarmNotification(
            this@AlarmForegroundService,
            alarm = _activeAlarm.value,
            fallbackAlarmId = alarmId
        )
        ServiceCompat.startForeground(
            this@AlarmForegroundService,
            NotificationHelper.NOTIFICATION_ID,
            initialNotification,
            foregroundServiceType
        )

        serviceScope.launch {
            val alarm = if (alarmId != -1L) getAlarmByIdUseCase(alarmId) else null
            _activeAlarm.value = alarm

            // Update notification with full alarm details (label, mission name, snooze button)
            if (alarm != null) {
                val updatedNotification = NotificationHelper.buildAlarmNotification(
                    this@AlarmForegroundService,
                    alarm = alarm,
                    fallbackAlarmId = alarmId
                )
                ServiceCompat.startForeground(
                    this@AlarmForegroundService,
                    NotificationHelper.NOTIFICATION_ID,
                    updatedNotification,
                    foregroundServiceType
                )
            }

            // Start audio and vibration immediately (continues seamlessly if already playing)
            alarmAudioPlayer.playAlarm(
                soundUriString = alarm?.soundUri,
                targetVolume = alarm?.volume ?: 1.0f,
                enableVibration = alarm?.vibrate ?: true
            )

            // Schedule safety timeout cutoff
            safetyTimeoutJob?.cancel()
            safetyTimeoutJob = launch {
                delay(WAKE_LOCK_TIMEOUT_MS)
                Log.w(TAG, "Safety cutoff reached (15 min). Auto-stopping alarm.")
                isExplicitlyDismissed = true
                com.discipline.alarm.util.ActiveAlarmStore.clearActiveAlarm(this@AlarmForegroundService)
                handleStopAlarm()
            }
        }
    }

    private fun handleStopAlarm() {
        Log.d(TAG, "Stopping foreground alarm service (verified=$isExplicitlyDismissed)")
        safetyTimeoutJob?.cancel()
        safetyTimeoutJob = null

        alarmAudioPlayer.stopAlarm()
        releaseWakeLock()

        _isAlarmActive.value = false
        _activeAlarm.value = null
        currentAlarmId = -1L

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun acquireWakeLock() {
        wakeLock?.let {
            if (!it.isHeld) {
                it.acquire(WAKE_LOCK_TIMEOUT_MS)
                Log.d(TAG, "WakeLock acquired with 15m timeout")
            }
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                Log.d(TAG, "WakeLock released")
            }
        }
    }

    override fun onDestroy() {
        val activeId = if (currentAlarmId > 0L) {
            currentAlarmId
        } else {
            com.discipline.alarm.util.ActiveAlarmStore.getActiveAlarmId(this)
        }

        if (!isExplicitlyDismissed && activeId > 0L) {
            Log.w(TAG, "Service destroyed while alarm ($activeId) is STILL UNVERIFIED! Scheduling instant revival.")
            com.discipline.alarm.util.ActiveAlarmStore.scheduleImmediateRevival(this, activeId)
        } else {
            handleStopAlarm()
        }
        super.onDestroy()
    }
}
