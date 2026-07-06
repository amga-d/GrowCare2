package com.example.growCare.domain.usecase.detection

import android.net.Uri
import com.example.growCare.domain.model.DiseaseAnalysis
import com.example.growCare.domain.repository.DetectionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Use case for analyzing plant images to detect diseases
 */
class AnalyzePlantDiseaseUseCase @Inject constructor(
    private val repository: DetectionRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend operator fun invoke(
        imageUri: Uri,
        cropName: String? = null
    ): Result<DiseaseAnalysis> = withContext(dispatcher) {
        try {
            repository.analyzePlantDisease(imageUri, cropName)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
