package com.discipline.alarm.presentation.alarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.discipline.alarm.domain.model.Alarm
import com.discipline.alarm.domain.model.DayOfWeek
import com.discipline.alarm.domain.model.Mission
import com.discipline.alarm.domain.usecase.alarm.DeleteAlarmUseCase
import com.discipline.alarm.domain.usecase.alarm.GetAlarmByIdUseCase
import com.discipline.alarm.domain.usecase.alarm.ScheduleAlarmUseCase
import com.discipline.alarm.domain.usecase.mission.GetMissionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlarmEditState(
    val id: Long = 0,
    val hour: Int = 6,
    val minute: Int = 0,
    val label: String = "Morning Discipline",
    val enabled: Boolean = true,
    val repeatDays: Set<DayOfWeek> = emptySet(),
    val soundUri: String = "Kafir.mp3",
    val snoozeEnabled: Boolean = false,
    val snoozeMinutes: Int = 5,
    val maxSnoozeCount: Int = 1,
    val missionId: Long? = null,
    val threshold: Float = 0.75f,
    val vibrate: Boolean = true,
    val volume: Float = 1.0f,
    val isPreviewPlaying: Boolean = false,
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val isInitialized: Boolean = false
)

@HiltViewModel
class AlarmEditViewModel @Inject constructor(
    private val getAlarmByIdUseCase: GetAlarmByIdUseCase,
    private val scheduleAlarmUseCase: ScheduleAlarmUseCase,
    private val deleteAlarmUseCase: DeleteAlarmUseCase,
    private val fileStorageManager: com.discipline.alarm.util.FileStorageManager,
    private val alarmAudioPlayer: com.discipline.alarm.data.audio.AlarmAudioPlayer,
    getMissionsUseCase: GetMissionsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(AlarmEditState())
    val state: StateFlow<AlarmEditState> = _state.asStateFlow()

    val availableMissions: StateFlow<List<Mission>> = getMissionsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _saveSuccessEvent = MutableSharedFlow<Unit>()
    val saveSuccessEvent: SharedFlow<Unit> = _saveSuccessEvent.asSharedFlow()

    private var hasLoadedOnce = false
    private var userExplicitlyClearedMission = false

    init {
        // Automatically select the newest mission if none is selected yet
        viewModelScope.launch {
            availableMissions.collectLatest { missions ->
                if (missions.isNotEmpty() && _state.value.missionId == null && !userExplicitlyClearedMission) {
                    val newest = missions.first()
                    _state.value = _state.value.copy(
                        missionId = newest.id,
                        threshold = newest.threshold
                    )
                }
            }
        }
    }

    fun loadAlarm(alarmId: Long?) {
        if (hasLoadedOnce) return
        hasLoadedOnce = true

        if (alarmId == null || alarmId <= 0) {
            val defaultMission = availableMissions.value.firstOrNull()
            val cal = java.util.Calendar.getInstance().apply {
                add(java.util.Calendar.MINUTE, 1)
            }
            _state.value = AlarmEditState(
                hour = cal.get(java.util.Calendar.HOUR_OF_DAY),
                minute = cal.get(java.util.Calendar.MINUTE),
                missionId = defaultMission?.id,
                threshold = defaultMission?.threshold ?: 0.75f,
                isEditMode = false,
                isInitialized = true
            )
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val alarm = getAlarmByIdUseCase(alarmId)
            if (alarm != null) {
                if (alarm.missionId == null) {
                    userExplicitlyClearedMission = true
                }
                _state.value = AlarmEditState(
                    id = alarm.id,
                    hour = alarm.hour,
                    minute = alarm.minute,
                    label = alarm.label,
                    enabled = alarm.enabled,
                    repeatDays = alarm.repeatDays,
                    soundUri = alarm.soundUri,
                    snoozeEnabled = alarm.snoozeEnabled,
                    snoozeMinutes = alarm.snoozeMinutes,
                    maxSnoozeCount = alarm.maxSnoozeCount,
                    missionId = alarm.missionId,
                    threshold = alarm.threshold,
                    vibrate = alarm.vibrate,
                    volume = alarm.volume,
                    isEditMode = true,
                    isLoading = false,
                    isInitialized = true
                )
            } else {
                _state.value = _state.value.copy(isLoading = false, isInitialized = true)
            }
        }
    }

    fun setTime(hour: Int, minute: Int) {
        _state.value = _state.value.copy(hour = hour, minute = minute)
    }

    fun setLabel(label: String) {
        _state.value = _state.value.copy(label = label)
    }

    fun toggleRepeatDay(day: DayOfWeek) {
        val currentDays = _state.value.repeatDays.toMutableSet()
        if (currentDays.contains(day)) {
            currentDays.remove(day)
        } else {
            currentDays.add(day)
        }
        _state.value = _state.value.copy(repeatDays = currentDays)
    }

    fun setMissionId(missionId: Long?) {
        userExplicitlyClearedMission = (missionId == null)
        _state.value = _state.value.copy(missionId = missionId)
    }

    fun setThreshold(threshold: Float) {
        _state.value = _state.value.copy(threshold = threshold.coerceIn(0.50f, 0.95f))
    }

    fun setSnoozeEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(snoozeEnabled = enabled)
    }

    fun setSnoozeMinutes(minutes: Int) {
        _state.value = _state.value.copy(snoozeMinutes = minutes)
    }

    fun setSoundUri(soundUri: String) {
        stopPreview()
        _state.value = _state.value.copy(soundUri = soundUri)
    }

    fun selectCustomDeviceAudio(uri: android.net.Uri) {
        stopPreview()
        viewModelScope.launch {
            val savedInternalUriString = fileStorageManager.saveCustomAudioTone(uri)
            _state.value = _state.value.copy(soundUri = savedInternalUriString)
        }
    }

    fun selectSystemRingtone(uri: android.net.Uri) {
        stopPreview()
        _state.value = _state.value.copy(soundUri = uri.toString())
    }

    fun togglePreview() {
        if (_state.value.isPreviewPlaying) {
            stopPreview()
        } else {
            alarmAudioPlayer.playPreview(_state.value.soundUri, _state.value.volume)
            _state.value = _state.value.copy(isPreviewPlaying = true)
        }
    }

    fun stopPreview() {
        alarmAudioPlayer.stopPreview()
        if (_state.value.isPreviewPlaying) {
            _state.value = _state.value.copy(isPreviewPlaying = false)
        }
    }

    fun setVibrate(vibrate: Boolean) {
        _state.value = _state.value.copy(vibrate = vibrate)
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _state.value = _state.value.copy(volume = clamped)
        if (_state.value.isPreviewPlaying) {
            alarmAudioPlayer.playPreview(_state.value.soundUri, clamped)
        }
    }

    fun saveAlarm() {
        stopPreview()
        val s = _state.value
        val alarm = Alarm(
            id = s.id,
            hour = s.hour,
            minute = s.minute,
            label = s.label,
            enabled = s.enabled,
            repeatDays = s.repeatDays,
            soundUri = s.soundUri,
            snoozeEnabled = s.snoozeEnabled,
            snoozeMinutes = s.snoozeMinutes,
            maxSnoozeCount = s.maxSnoozeCount,
            missionId = s.missionId,
            threshold = s.threshold,
            vibrate = s.vibrate,
            volume = s.volume
        )

        viewModelScope.launch {
            scheduleAlarmUseCase(alarm)
            _saveSuccessEvent.emit(Unit)
        }
    }

    fun deleteAlarm() {
        stopPreview()
        if (!_state.value.isEditMode) return
        val s = _state.value
        val alarm = Alarm(
            id = s.id,
            hour = s.hour,
            minute = s.minute,
            enabled = s.enabled
        )
        viewModelScope.launch {
            deleteAlarmUseCase(alarm)
            _saveSuccessEvent.emit(Unit)
        }
    }

    override fun onCleared() {
        stopPreview()
        super.onCleared()
    }
}
