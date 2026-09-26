package com.discipline.alarm.presentation.mission

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.discipline.alarm.domain.model.Mission
import com.discipline.alarm.domain.usecase.mission.CreateMissionWithEmbeddingsUseCase
import com.discipline.alarm.domain.usecase.mission.GetMissionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MissionRegisterState(
    val missionName: String = "",
    val slot1Bitmap: Bitmap? = null,
    val slot2Bitmap: Bitmap? = null,
    val slot3Bitmap: Bitmap? = null,
    val activeSlotIndex: Int = 0,
    val threshold: Float = 0.75f,
    val isTorchOn: Boolean = false,
    val isProcessing: Boolean = false,
    val errorMessage: String? = null
) {
    val canSave: Boolean
        get() = missionName.isNotBlank() &&
                slot1Bitmap != null &&
                slot2Bitmap != null &&
                slot3Bitmap != null &&
                !isProcessing
}

@HiltViewModel
class MissionRegisterViewModel @Inject constructor(
    private val createMissionWithEmbeddingsUseCase: CreateMissionWithEmbeddingsUseCase,
    getMissionsUseCase: GetMissionsUseCase
) : ViewModel() {

    val existingMissions: StateFlow<List<Mission>> = getMissionsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _state = MutableStateFlow(MissionRegisterState())
    val state: StateFlow<MissionRegisterState> = _state.asStateFlow()

    private val _saveSuccessEvent = MutableSharedFlow<Long>()
    val saveSuccessEvent: SharedFlow<Long> = _saveSuccessEvent.asSharedFlow()

    fun setMissionName(name: String) {
        _state.value = _state.value.copy(missionName = name)
    }

    fun setActiveSlot(slotIndex: Int) {
        _state.value = _state.value.copy(activeSlotIndex = slotIndex.coerceIn(0, 2))
    }

    fun setTorch(isTorchOn: Boolean) {
        _state.value = _state.value.copy(isTorchOn = isTorchOn)
    }

    fun onPhotoCaptured(bitmap: Bitmap) {
        val currentSlot = _state.value.activeSlotIndex
        when (currentSlot) {
            0 -> _state.value = _state.value.copy(slot1Bitmap = bitmap, activeSlotIndex = 1)
            1 -> _state.value = _state.value.copy(slot2Bitmap = bitmap, activeSlotIndex = 2)
            2 -> _state.value = _state.value.copy(slot3Bitmap = bitmap)
        }
    }

    fun saveMission() {
        val s = _state.value
        if (!s.canSave) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isProcessing = true, errorMessage = null)
            try {
                val missionId = createMissionWithEmbeddingsUseCase(
                    missionName = s.missionName.trim(),
                    bitmap1 = s.slot1Bitmap!!,
                    bitmap2 = s.slot2Bitmap!!,
                    bitmap3 = s.slot3Bitmap!!,
                    threshold = s.threshold
                )
                _saveSuccessEvent.emit(missionId)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isProcessing = false,
                    errorMessage = "Failed to extract embeddings: ${e.localizedMessage}"
                )
            }
        }
    }
}
