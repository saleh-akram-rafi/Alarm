package com.discipline.alarm.presentation.permissions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.discipline.alarm.presentation.theme.DarkBackground
import com.discipline.alarm.presentation.theme.DarkBorder
import com.discipline.alarm.presentation.theme.DarkSurface
import com.discipline.alarm.presentation.theme.DarkSurfaceVariant
import com.discipline.alarm.presentation.theme.DisciplineAccent
import com.discipline.alarm.presentation.theme.DisciplineError
import com.discipline.alarm.presentation.theme.DisciplinePrimary
import com.discipline.alarm.presentation.theme.DisciplineSuccess
import com.discipline.alarm.presentation.theme.TextMuted
import com.discipline.alarm.presentation.theme.TextPrimary
import com.discipline.alarm.presentation.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    viewModel: PermissionsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.checkPermissions()
        if (!granted) {
            viewModel.openAppSettings()
        }
    }

    val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.checkPermissions()
        if (!granted) {
            viewModel.requestNotificationSettings()
        }
    }

    // Re-check permissions when returning from system settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "System Permissions",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "For Discipline Alarm to guarantee reliable wakeups and object scanning, grant the permissions below:",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            // 1. Exact Alarm
            PermissionItemCard(
                title = "Exact Alarm Permission",
                description = "Required to wake your phone at the exact second scheduled.",
                isGranted = state.hasExactAlarm,
                icon = Icons.Default.Alarm,
                onRequest = { viewModel.requestExactAlarmPermission() }
            )

            // 2. Notification
            PermissionItemCard(
                title = "Notification Permission",
                description = "Required to display the alarm and full-screen wakeup interface.",
                isGranted = state.hasNotification,
                icon = Icons.Default.Notifications,
                onRequest = {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.requestNotificationSettings()
                    }
                }
            )

            // 3. Camera
            PermissionItemCard(
                title = "Camera Permission",
                description = "Required to scan and verify your registered mission target.",
                isGranted = state.hasCamera,
                icon = Icons.Default.CameraAlt,
                onRequest = {
                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                }
            )

            // 4. Battery Optimization
            PermissionItemCard(
                title = "Battery Optimization",
                description = "Prevents OEM aggressive task killers from killing the alarm service.",
                isGranted = state.isIgnoringBattery,
                icon = Icons.Default.BatteryAlert,
                onRequest = { viewModel.requestBatteryOptimization() }
            )

            Spacer(modifier = Modifier.weight(1f))

            if (state.allGranted) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DisciplineSuccess),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = DisciplineSuccess,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Text(
                            text = "All required permissions are granted! Discipline Alarm is fully operational.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionItemCard(
    title: String,
    description: String,
    isGranted: Boolean,
    icon: ImageVector,
    onRequest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) DisciplineSuccess else DisciplineAccent,
                    modifier = Modifier
                        .size(32.dp)
                        .padding(end = 12.dp)
                )
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            if (isGranted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Granted",
                    tint = DisciplineSuccess,
                    modifier = Modifier
                        .size(28.dp)
                        .padding(start = 8.dp)
                )
            } else {
                OutlinedButton(
                    onClick = onRequest,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DisciplineAccent),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DisciplineAccent),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text("Grant", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
