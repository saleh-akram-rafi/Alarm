package com.discipline.alarm.presentation.alarm

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.discipline.alarm.domain.model.DayOfWeek
import com.discipline.alarm.presentation.theme.DarkBackground
import com.discipline.alarm.presentation.theme.DarkBorder
import com.discipline.alarm.presentation.theme.DarkSurface
import com.discipline.alarm.presentation.theme.DarkSurfaceVariant
import com.discipline.alarm.presentation.theme.DisciplineAccent
import com.discipline.alarm.presentation.theme.DisciplineError
import com.discipline.alarm.presentation.theme.DisciplinePrimary
import com.discipline.alarm.presentation.theme.TextMuted
import com.discipline.alarm.presentation.theme.TextPrimary
import com.discipline.alarm.presentation.theme.TextSecondary
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AlarmEditScreen(
    alarmId: Long?,
    viewModel: AlarmEditViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCreateMission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val availableMissions by viewModel.availableMissions.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            viewModel.stopPreview()
        }
    }

    val customAudioPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.selectCustomDeviceAudio(uri)
        }
    }

    val ringtonePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val pickedUri: android.net.Uri? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(
                    android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI,
                    android.net.Uri::class.java
                )
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            if (pickedUri != null) {
                viewModel.selectSystemRingtone(pickedUri)
            }
        }
    }

    LaunchedEffect(alarmId) {
        viewModel.loadAlarm(alarmId)
    }

    LaunchedEffect(Unit) {
        viewModel.saveSuccessEvent.collectLatest {
            onNavigateBack()
        }
    }

    if (!state.isInitialized) {
        return
    }

    val timePickerState = rememberTimePickerState(
        initialHour = state.hour,
        initialMinute = state.minute,
        is24Hour = false
    )

    LaunchedEffect(timePickerState.hour, timePickerState.minute) {
        viewModel.setTime(timePickerState.hour, timePickerState.minute)
    }

    var isMissionDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.isEditMode) "Edit Alarm" else "New Alarm",
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
                actions = {
                    TextButton(onClick = {
                        viewModel.setTime(timePickerState.hour, timePickerState.minute)
                        viewModel.saveAlarm()
                    }) {
                        Text(
                            text = "Save",
                            color = DisciplinePrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Material 3 Time Picker
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(
                        state = timePickerState,
                        colors = TimePickerDefaults.colors(
                            clockDialColor = DarkSurfaceVariant,
                            clockDialSelectedContentColor = DarkBackground,
                            clockDialUnselectedContentColor = TextPrimary,
                            selectorColor = DisciplinePrimary,
                            periodSelectorBorderColor = DarkBorder,
                            periodSelectorSelectedContainerColor = DisciplinePrimary,
                            periodSelectorUnselectedContainerColor = DarkSurfaceVariant,
                            periodSelectorSelectedContentColor = TextPrimary,
                            periodSelectorUnselectedContentColor = TextSecondary,
                            timeSelectorSelectedContainerColor = DisciplinePrimary.copy(alpha = 0.2f),
                            timeSelectorUnselectedContainerColor = DarkSurfaceVariant,
                            timeSelectorSelectedContentColor = DisciplinePrimary,
                            timeSelectorUnselectedContentColor = TextPrimary
                        )
                    )
                }
            }

            // Alarm Label
            OutlinedTextField(
                value = state.label,
                onValueChange = { viewModel.setLabel(it) },
                label = { Text("Alarm Label") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DisciplinePrimary,
                    unfocusedBorderColor = DarkBorder,
                    focusedLabelColor = DisciplinePrimary,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = DisciplinePrimary
                )
            )

            // Repeat Days
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Repeat Days",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DayOfWeek.entries.forEach { day ->
                            val isSelected = state.repeatDays.contains(day)
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.toggleRepeatDay(day) },
                                label = { Text(day.shortName) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DisciplinePrimary,
                                    selectedLabelColor = TextPrimary,
                                    containerColor = DarkSurfaceVariant,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                }
            }

            // Mission Selection Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = DisciplineAccent,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = "Mission Object",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(onClick = onNavigateToCreateMission) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Mission",
                                tint = DisciplineAccent
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val selectedMission = availableMissions.firstOrNull { it.id == state.missionId }

                    ExposedDropdownMenuBox(
                        expanded = isMissionDropdownExpanded,
                        onExpandedChange = { isMissionDropdownExpanded = !isMissionDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedMission?.name ?: "None (Standard Dismiss)",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isMissionDropdownExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DisciplineAccent,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        ExposedDropdownMenu(
                            expanded = isMissionDropdownExpanded,
                            onDismissRequest = { isMissionDropdownExpanded = false },
                            modifier = Modifier.background(DarkSurfaceVariant)
                        ) {
                            DropdownMenuItem(
                                text = { Text("None (Standard Dismiss)", color = TextSecondary) },
                                onClick = {
                                    viewModel.setMissionId(null)
                                    isMissionDropdownExpanded = false
                                }
                            )
                            availableMissions.forEach { mission ->
                                DropdownMenuItem(
                                    text = { Text(mission.name, color = TextPrimary, fontWeight = FontWeight.SemiBold) },
                                    onClick = {
                                        viewModel.setMissionId(mission.id)
                                        viewModel.setThreshold(mission.threshold)
                                        isMissionDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (selectedMission != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Verification Threshold: ${(state.threshold * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Slider(
                            value = state.threshold,
                            onValueChange = { viewModel.setThreshold(it) },
                            valueRange = 0.50f..0.95f,
                            steps = 8,
                            colors = SliderDefaults.colors(
                                thumbColor = DisciplineAccent,
                                activeTrackColor = DisciplineAccent,
                                inactiveTrackColor = DarkSurfaceVariant
                            )
                        )
                        Text(
                            text = "Higher threshold requires more precise matching of the physical object.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }

            // Snooze Controls
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = if (state.snoozeEnabled) DisciplineAccent else TextMuted,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = "Allow Snooze",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Switch(
                            checked = state.snoozeEnabled,
                            onCheckedChange = { viewModel.setSnoozeEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TextPrimary,
                                checkedTrackColor = DisciplineAccent,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DarkSurfaceVariant
                            )
                        )
                    }

                    if (state.snoozeEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Snooze Duration: ${state.snoozeMinutes} minutes (Max 1 time)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            listOf(3, 5, 10, 15).forEach { min ->
                                FilterChip(
                                    selected = state.snoozeMinutes == min,
                                    onClick = { viewModel.setSnoozeMinutes(min) },
                                    label = { Text("$min m") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = DisciplineAccent,
                                        selectedLabelColor = DarkBackground,
                                        containerColor = DarkSurfaceVariant,
                                        labelColor = TextSecondary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Sound and Vibration
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Vibrate",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )

                        Switch(
                            checked = state.vibrate,
                            onCheckedChange = { viewModel.setVibrate(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TextPrimary,
                                checkedTrackColor = DisciplinePrimary,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DarkSurfaceVariant
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val isKafirSelected = state.soundUri.equals("Kafir.mp3", ignoreCase = true) ||
                        state.soundUri.equals("kafir", ignoreCase = true) ||
                        state.soundUri.contains("raw/kafir")
                    val isSystemSelected = state.soundUri.equals("system", ignoreCase = true)
                    val isCustomSelected = !isKafirSelected && !isSystemSelected
                    val currentTuneName = remember(state.soundUri) {
                        com.discipline.alarm.util.FileStorageManager.getSoundDisplayName(context, state.soundUri)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Alarm Tone",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Selected: $currentTuneName",
                                style = MaterialTheme.typography.bodySmall,
                                color = DisciplineAccent,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        OutlinedButton(
                            onClick = { viewModel.togglePreview() },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (state.isPreviewPlaying) DisciplineError else DisciplineAccent
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (state.isPreviewPlaying) DisciplineError else DisciplineAccent
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (state.isPreviewPlaying) "Stop Test" else "Test Tune",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Built-in / Preset Options
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = isKafirSelected,
                            onClick = { viewModel.setSoundUri("Kafir.mp3") },
                            label = { Text("Kafir.mp3 (Discipline)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DisciplinePrimary,
                                selectedLabelColor = TextPrimary,
                                containerColor = DarkSurfaceVariant,
                                labelColor = TextSecondary
                            )
                        )
                        FilterChip(
                            selected = isSystemSelected,
                            onClick = { viewModel.setSoundUri("system") },
                            label = { Text("System Alarm") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DisciplineAccent,
                                selectedLabelColor = DarkBackground,
                                containerColor = DarkSurfaceVariant,
                                labelColor = TextSecondary
                            )
                        )
                        if (isCustomSelected) {
                            FilterChip(
                                selected = true,
                                onClick = { viewModel.togglePreview() },
                                label = { Text("Custom: $currentTuneName") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DisciplineAccent,
                                    selectedLabelColor = DarkBackground,
                                    containerColor = DarkSurfaceVariant,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Custom Device Audio & Ringtone Picker Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.stopPreview()
                                customAudioPickerLauncher.launch(arrayOf("audio/*"))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DisciplinePrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = DisciplinePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Device Audio (MP3)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.stopPreview()
                                val intent = android.content.Intent(android.media.RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                    putExtra(
                                        android.media.RingtoneManager.EXTRA_RINGTONE_TYPE,
                                        android.media.RingtoneManager.TYPE_ALARM or android.media.RingtoneManager.TYPE_RINGTONE
                                    )
                                    putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                    putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                    putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Tune")
                                }
                                ringtonePickerLauncher.launch(intent)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DisciplineAccent)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = DisciplineAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Phone Ringtones",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Alarm Volume: ${(state.volume * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Slider(
                        value = state.volume,
                        onValueChange = { viewModel.setVolume(it) },
                        colors = SliderDefaults.colors(
                            thumbColor = DisciplinePrimary,
                            activeTrackColor = DisciplinePrimary,
                            inactiveTrackColor = DarkSurfaceVariant
                        )
                    )
                }
            }

            // Delete Alarm Button (if edit mode)
            if (state.isEditMode) {
                OutlinedButton(
                    onClick = { viewModel.deleteAlarm() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = DisciplineError
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DisciplineError),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Delete Alarm", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
