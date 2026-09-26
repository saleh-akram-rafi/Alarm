package com.discipline.alarm.presentation.trigger

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.discipline.alarm.presentation.theme.DisciplineAlarmTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AlarmActivity : ComponentActivity() {

    companion object {
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_OPEN_SCANNER = "extra_open_scanner"
    }

    private val viewModel: AlarmTriggerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupLockScreenFlags()

        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        val openScannerInitially = intent.getBooleanExtra(EXTRA_OPEN_SCANNER, false)
        viewModel.initialize(alarmId, openScannerInitially)

        // Block accidental dismissal via Back button
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.state.value.isScannerVisible) {
                    viewModel.closeScanner()
                }
                // When in trigger screen, back button does nothing (must scan to dismiss!)
            }
        })

        setContent {
            DisciplineAlarmTheme {
                AlarmTriggerHost(
                    viewModel = viewModel,
                    onAlarmDismissed = {
                        finish()
                    }
                )
            }
        }
    }

    private fun setupLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onDestroy() {
        val activeAlarmId = com.discipline.alarm.util.ActiveAlarmStore.getActiveAlarmId(this)
        if (activeAlarmId > 0L && !viewModel.state.value.isDismissed) {
            // User swiped AlarmActivity away from Recents or OS destroyed the activity before scanning!
            // Keep the foreground audio service alive in the background and schedule immediate revival.
            com.discipline.alarm.util.ActiveAlarmStore.scheduleImmediateRevival(this, activeAlarmId)
            try {
                val serviceIntent = android.content.Intent(
                    this,
                    com.discipline.alarm.service.AlarmForegroundService::class.java
                ).apply {
                    action = com.discipline.alarm.service.AlarmForegroundService.ACTION_START_ALARM
                    putExtra(com.discipline.alarm.service.AlarmForegroundService.EXTRA_ALARM_ID, activeAlarmId)
                }
                androidx.core.content.ContextCompat.startForegroundService(this, serviceIntent)
            } catch (_: Exception) {
            }
        }
        super.onDestroy()
    }
}
