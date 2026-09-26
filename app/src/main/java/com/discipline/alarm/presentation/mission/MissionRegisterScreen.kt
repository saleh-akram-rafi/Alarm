package com.discipline.alarm.presentation.mission

import android.graphics.Bitmap
import androidx.camera.view.PreviewView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.discipline.alarm.camera.CameraManager
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
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionRegisterScreen(
    viewModel: MissionRegisterViewModel,
    onNavigateBack: () -> Unit,
    onMissionCreated: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraManager = remember { CameraManager(context) }

    DisposableEffect(Unit) {
        onDispose {
            cameraManager.shutdown()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.saveSuccessEvent.collectLatest { missionId ->
            onMissionCreated(missionId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Register Mission Target",
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mission Name Input
            OutlinedTextField(
                value = state.missionName,
                onValueChange = { viewModel.setMissionName(it) },
                label = { Text("Target Name (e.g. Bathroom Sink, Mirror)") },
                placeholder = { Text("Bathroom Sink") },
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

            // Instruction Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceVariant)
                    .padding(12.dp)
            ) {
                Text(
                    text = "Capture 3 reference photos of the object from different angles. In the morning, you must scan this object to dismiss your alarm.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            // Camera Viewfinder Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurface)
                    .border(2.dp, DisciplineAccent, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            lifecycleOwner.lifecycleScope.launch {
                                cameraManager.startCamera(lifecycleOwner, this@apply)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Reticle overlay
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .border(2.dp, DisciplineAccent.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                )

                // Torch toggle on camera
                IconButton(
                    onClick = {
                        cameraManager.toggleTorch { isNowOn ->
                            viewModel.setTorch(isNowOn)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(DarkBackground.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (state.isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Torch",
                        tint = if (state.isTorchOn) DisciplineAccent else TextPrimary
                    )
                }
            }

            // Slot Thumbnails Row
            Text(
                text = "Reference Angles (Capture 3/3):",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val slots = listOf(
                    Triple(0, "Angle 1 (Front)", state.slot1Bitmap),
                    Triple(1, "Angle 2 (Side)", state.slot2Bitmap),
                    Triple(2, "Angle 3 (Close)", state.slot3Bitmap)
                )

                slots.forEach { (index, title, bitmap) ->
                    val isCurrent = state.activeSlotIndex == index
                    val isDone = bitmap != null

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurface)
                            .border(
                                width = if (isCurrent) 2.dp else 1.dp,
                                color = if (isCurrent) DisciplinePrimary else if (isDone) DisciplineSuccess else DarkBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.setActiveSlot(index) }
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(18.dp)
                                        .background(DisciplineSuccess, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = DarkBackground,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = if (isCurrent) DisciplinePrimary else TextMuted
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = if (isCurrent) DisciplinePrimary else TextSecondary,
                            maxLines = 1
                        )
                    }
                }
            }

            // Capture Shutter Button
            Button(
                onClick = {
                    cameraManager.capturePhoto(
                        onSuccess = { bitmap ->
                            viewModel.onPhotoCaptured(bitmap)
                        },
                        onError = {
                            // Error handling
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DisciplinePrimary)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Capture Angle ${state.activeSlotIndex + 1}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage ?: "",
                    color = DisciplineError,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Save Mission Button
            Button(
                onClick = { viewModel.saveMission() },
                enabled = state.canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DisciplineSuccess,
                    disabledContainerColor = DarkSurfaceVariant
                )
            ) {
                if (state.isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = DarkBackground,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Extracting Embeddings...",
                        color = DarkBackground,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = if (state.canSave) DarkBackground else TextMuted,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Save Mission Target",
                        color = if (state.canSave) DarkBackground else TextMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
