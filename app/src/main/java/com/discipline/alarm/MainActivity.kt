package com.discipline.alarm

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.discipline.alarm.presentation.navigation.NavGraph
import com.discipline.alarm.presentation.theme.DarkBackground
import com.discipline.alarm.presentation.theme.DisciplineAlarmTheme
import com.discipline.alarm.util.PermissionUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // After runtime permissions are answered, ensure exact alarm permission is granted
        if (!PermissionUtils.hasExactAlarmPermission(this)) {
            PermissionUtils.openExactAlarmSettings(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestEssentialRuntimePermissions()

        setContent {
            DisciplineAlarmTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    val navController = rememberNavController()
                    NavGraph(navController = navController)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // If an alarm is currently ringing and has NOT been dismissed by scanning the mission object,
        // prevent bypassing the alarm via MainActivity and immediately launch AlarmActivity!
        val activeAlarmId = com.discipline.alarm.util.ActiveAlarmStore.getActiveAlarmId(this)
        if (activeAlarmId > 0L) {
            val serviceIntent = android.content.Intent(
                this,
                com.discipline.alarm.service.AlarmForegroundService::class.java
            ).apply {
                action = com.discipline.alarm.service.AlarmForegroundService.ACTION_START_ALARM
                putExtra(com.discipline.alarm.service.AlarmForegroundService.EXTRA_ALARM_ID, activeAlarmId)
            }
            androidx.core.content.ContextCompat.startForegroundService(this, serviceIntent)

            val triggerIntent = android.content.Intent(
                this,
                com.discipline.alarm.presentation.trigger.AlarmActivity::class.java
            ).apply {
                putExtra(com.discipline.alarm.presentation.trigger.AlarmActivity.EXTRA_ALARM_ID, activeAlarmId)
                addFlags(
                    android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                        android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            }
            startActivity(triggerIntent)
        }
    }

    private fun requestEssentialRuntimePermissions() {
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !PermissionUtils.hasNotificationPermission(this)
        ) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (!PermissionUtils.hasCameraPermission(this)) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        } else if (!PermissionUtils.hasExactAlarmPermission(this)) {
            PermissionUtils.openExactAlarmSettings(this)
        }
    }
}
