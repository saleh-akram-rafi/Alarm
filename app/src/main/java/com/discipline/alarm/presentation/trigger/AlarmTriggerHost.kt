package com.discipline.alarm.presentation.trigger

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.discipline.alarm.presentation.verify.VerificationScreen
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AlarmTriggerHost(
    viewModel: AlarmTriggerViewModel,
    onAlarmDismissed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.alarmDismissedEvent.collectLatest {
            onAlarmDismissed()
        }
    }

    if (state.isScannerVisible && state.alarm?.mission != null) {
        VerificationScreen(
            mission = state.alarm?.mission,
            isAnalyzing = state.isAnalyzing,
            verificationResult = state.verificationResult,
            onCapture = { bitmap ->
                viewModel.verifyCapturedImage(bitmap)
            },
            onDismissConfirmed = {
                viewModel.confirmDismissal()
            },
            onRetry = {
                viewModel.retryVerification()
            },
            onCloseScanner = {
                viewModel.closeScanner()
            },
            modifier = modifier
        )
    } else {
        AlarmTriggerScreen(
            alarm = state.alarm,
            currentTimeString = state.currentTimeString,
            canSnooze = state.canSnooze,
            onScanMissionClick = {
                viewModel.openScanner()
            },
            onSnoozeClick = {
                viewModel.snooze()
            },
            onDismissClick = {
                viewModel.confirmDismissal()
            },
            modifier = modifier
        )
    }
}
