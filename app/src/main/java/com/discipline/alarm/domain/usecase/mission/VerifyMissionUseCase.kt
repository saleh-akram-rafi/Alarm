package com.discipline.alarm.domain.usecase.mission

import android.graphics.Bitmap
import com.discipline.alarm.domain.model.Mission
import com.discipline.alarm.domain.model.VerificationResult
import com.discipline.alarm.ml.VerificationEngine
import javax.inject.Inject

class VerifyMissionUseCase @Inject constructor(
    private val verificationEngine: VerificationEngine
) {
    suspend operator fun invoke(
        liveBitmap: Bitmap,
        mission: Mission,
        effectiveThreshold: Float = mission.threshold
    ): VerificationResult {
        return verificationEngine.verify(liveBitmap, mission, effectiveThreshold)
    }
}
