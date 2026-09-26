package com.discipline.alarm.presentation.permissions

import android.content.Context
import androidx.lifecycle.ViewModel
import com.discipline.alarm.util.PermissionUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class PermissionsState(
    val hasExactAlarm: Boolean = false,
    val hasNotification: Boolean = false,
    val hasCamera: Boolean = false,
    val isIgnoringBattery: Boolean = false
) {
    val allGranted: Boolean
        get() = hasExactAlarm && hasNotification && hasCamera && isIgnoringBattery
}

@HiltViewModel
class PermissionsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(PermissionsState())
    val state: StateFlow<PermissionsState> = _state.asStateFlow()

    init {
        checkPermissions()
    }

    fun checkPermissions() {
        _state.value = PermissionsState(
            hasExactAlarm = PermissionUtils.hasExactAlarmPermission(context),
            hasNotification = PermissionUtils.hasNotificationPermission(context),
            hasCamera = PermissionUtils.hasCameraPermission(context),
            isIgnoringBattery = PermissionUtils.isIgnoringBatteryOptimizations(context)
        )
    }

    fun requestExactAlarmPermission() {
        PermissionUtils.openExactAlarmSettings(context)
    }

    fun requestBatteryOptimization() {
        PermissionUtils.openBatteryOptimizationSettings(context)
    }

    fun requestNotificationSettings() {
        PermissionUtils.openNotificationSettings(context)
    }

    fun openAppSettings() {
        PermissionUtils.openAppSettings(context)
    }
}
