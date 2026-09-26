package com.discipline.alarm.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.discipline.alarm.domain.alarm.AlarmScheduler
import com.discipline.alarm.domain.model.Alarm
import com.discipline.alarm.domain.usecase.alarm.DeleteAlarmUseCase
import com.discipline.alarm.domain.usecase.alarm.GetAlarmsUseCase
import com.discipline.alarm.domain.usecase.alarm.GetNextActiveAlarmUseCase
import com.discipline.alarm.domain.usecase.alarm.ToggleAlarmUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    getAlarmsUseCase: GetAlarmsUseCase,
    private val getNextActiveAlarmUseCase: GetNextActiveAlarmUseCase,
    private val toggleAlarmUseCase: ToggleAlarmUseCase,
    private val deleteAlarmUseCase: DeleteAlarmUseCase,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    val alarms: StateFlow<List<Alarm>> = getAlarmsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _nextAlarm = MutableStateFlow<Alarm?>(null)
    val nextAlarm: StateFlow<Alarm?> = _nextAlarm.asStateFlow()

    init {
        refreshNextAlarm()
    }

    fun refreshNextAlarm() {
        viewModelScope.launch {
            _nextAlarm.value = getNextActiveAlarmUseCase()
        }
    }

    fun toggleAlarm(alarm: Alarm, enabled: Boolean) {
        viewModelScope.launch {
            toggleAlarmUseCase(alarm.id, enabled)
            val updated = alarm.copy(enabled = enabled)
            if (enabled) {
                alarmScheduler.schedule(updated)
            } else {
                alarmScheduler.cancel(updated)
            }
            refreshNextAlarm()
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            alarmScheduler.cancel(alarm)
            deleteAlarmUseCase(alarm)
            refreshNextAlarm()
        }
    }
}
