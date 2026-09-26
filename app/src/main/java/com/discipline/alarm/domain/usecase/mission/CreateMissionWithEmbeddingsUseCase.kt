package com.discipline.alarm.domain.usecase.mission

import android.graphics.Bitmap
import com.discipline.alarm.data.local.converter.Converters
import com.discipline.alarm.domain.model.Mission
import com.discipline.alarm.domain.repository.MissionRepository
import com.discipline.alarm.ml.FeatureExtractor
import com.discipline.alarm.util.FileStorageManager
import javax.inject.Inject

class CreateMissionWithEmbeddingsUseCase @Inject constructor(
    private val missionRepository: MissionRepository,
    private val featureExtractor: FeatureExtractor,
    private val fileStorageManager: FileStorageManager
) {
    suspend operator fun invoke(
        missionName: String,
        bitmap1: Bitmap,
        bitmap2: Bitmap,
        bitmap3: Bitmap,
        threshold: Float = 0.75f
    ): Long {
        // 1. Save bitmaps into internal storage
        val path1 = fileStorageManager.saveMissionImage(bitmap1, missionName, 1)
        val path2 = fileStorageManager.saveMissionImage(bitmap2, missionName, 2)
        val path3 = fileStorageManager.saveMissionImage(bitmap3, missionName, 3)

        // 2. Precompute feature embedding vectors
        val vector1 = featureExtractor.extractFeatures(bitmap1)
        val vector2 = featureExtractor.extractFeatures(bitmap2)
        val vector3 = featureExtractor.extractFeatures(bitmap3)

        // 3. Serialize vectors for Room BLOB caching
        val bytes1 = Converters.floatArrayToByteArray(vector1)
        val bytes2 = Converters.floatArrayToByteArray(vector2)
        val bytes3 = Converters.floatArrayToByteArray(vector3)

        // 4. Save Mission
        val mission = Mission(
            name = missionName,
            image1Path = path1,
            image2Path = path2,
            image3Path = path3,
            embedding1 = bytes1,
            embedding2 = bytes2,
            embedding3 = bytes3,
            threshold = threshold
        )

        return missionRepository.insertMission(mission)
    }
}
