package com.discipline.alarm.presentation.trigger

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.discipline.alarm.domain.model.Alarm
import com.discipline.alarm.domain.model.VerificationResult
import com.discipline.alarm.domain.usecase.alarm.DismissAlarmUseCase
import com.discipline.alarm.domain.usecase.alarm.GetAlarmByIdUseCase
import com.discipline.alarm.domain.usecase.alarm.SnoozeAlarmUseCase
import com.discipline.alarm.domain.usecase.mission.VerifyMissionUseCase
import com.discipline.alarm.service.AlarmForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class AlarmTriggerState(
    val alarm: Alarm? = null,
    val currentTimeString: String = "",
    val isScannerVisible: Boolean = false,
    val isAnalyzing: Boolean = false,
    val verificationResult: VerificationResult? = null,
    val isDismissed: Boolean = false,
    val canSnooze: Boolean = false
)

@HiltViewModel
class AlarmTriggerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getAlarmByIdUseCase: GetAlarmByIdUseCase,
    private val dismissAlarmUseCase: DismissAlarmUseCase,
    private val snoozeAlarmUseCase: SnoozeAlarmUseCase,
    private val verifyMissionUseCase: VerifyMissionUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "AlarmTriggerViewModel"
    }

    private val _state = MutableStateFlow(AlarmTriggerState())
    val state: StateFlow<AlarmTriggerState> = _state.asStateFlow()

    private val _alarmDismissedEvent = MutableSharedFlow<Unit>()
    val alarmDismissedEvent: SharedFlow<Unit> = _alarmDismissedEvent.asSharedFlow()

    private var clockJob: Job? = null

    fun initialize(alarmId: Long, openScannerInitially: Boolean) {
        startClock()

        viewModelScope.launch {
            val alarm = if (alarmId != -1L) getAlarmByIdUseCase(alarmId) else null
            val canSnooze = alarm != null && alarm.snoozeEnabled && alarm.snoozeCount < alarm.maxSnoozeCount

            _state.value = _state.value.copy(
                alarm = alarm,
                isScannerVisible = openScannerInitially || (alarm?.hasMission == true && openScannerInitially),
                canSnooze = canSnooze
            )
        }
    }

    private fun startClock() {
        clockJob?.cancel()
        clockJob = viewModelScope.launch {
            val formatter = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
            while (isActive) {
                _state.value = _state.value.copy(currentTimeString = formatter.format(Date()))
                delay(1000L)
            }
        }
    }

    fun openScanner() {
        _state.value = _state.value.copy(isScannerVisible = true, verificationResult = null)
    }

    fun closeScanner() {
        _state.value = _state.value.copy(isScannerVisible = false, verificationResult = null)
    }

    fun verifyCapturedImage(bitmap: Bitmap) {
        val alarm = _state.value.alarm ?: return
        val mission = alarm.mission

        if (mission == null) {
            // Alarm has no mission configured; dismiss immediately
            completeDismissal()
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isAnalyzing = true)

            try {
                val result = verifyMissionUseCase(
                    liveBitmap = bitmap,
                    mission = mission,
                    effectiveThreshold = alarm.threshold
                )

                _state.value = _state.value.copy(
                    isAnalyzing = false,
                    verificationResult = result
                )

                if (result.isSuccess) {
                    Log.d(TAG, "Mission verified successfully! Dismissing alarm.")
                    // Stop audio service immediately
                    stopAlarmService()
                } else {
                    Log.d(TAG, "Mission verification failed: score=${result.similarityScore}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during image verification", e)
                _state.value = _state.value.copy(
                    isAnalyzing = false,
                    verificationResult = VerificationResult(
                        isSuccess = false,
                        similarityScore = 0f,
                        threshold = alarm.threshold,
                        failureReason = "Processing error: ${e.localizedMessage}",
                        guidanceTip = "Please try again."
                    )
                )
            }
        }
    }

    fun retryVerification() {
        _state.value = _state.value.copy(verificationResult = null)
    }

    fun confirmDismissal() {
        completeDismissal()
    }

    fun snooze() {
        val alarm = _state.value.alarm ?: return
        viewModelScope.launch {
            snoozeAlarmUseCase(alarm)
            stopAlarmService()
            _alarmDismissedEvent.emit(Unit)
        }
    }

    private fun completeDismissal() {
        val alarm = _state.value.alarm
        viewModelScope.launch {
            if (alarm != null) {
                dismissAlarmUseCase(alarm)
            }
            stopAlarmService()
            _state.value = _state.value.copy(isDismissed = true)
            _alarmDismissedEvent.emit(Unit)
        }
    }

    private fun stopAlarmService() {
        com.discipline.alarm.util.ActiveAlarmStore.clearActiveAlarm(context)
        val serviceIntent = Intent(context, AlarmForegroundService::class.java).apply {
            action = AlarmForegroundService.ACTION_STOP_ALARM
            putExtra(AlarmForegroundService.EXTRA_VERIFIED_DISMISS, true)
        }
        try {
            context.startService(serviceIntent)
        } catch (e: Exception) {
            context.stopService(serviceIntent)
        }
    }

    override fun onCleared() {
        super.onCleared()
        clockJob?.cancel()
    }
}
