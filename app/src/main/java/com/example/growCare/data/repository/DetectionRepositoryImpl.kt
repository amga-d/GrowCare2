package com.example.growCare.data.repository

import android.net.Uri
import com.example.growCare.data.local.datasource.DiseaseAnalysisLocalDataSource
import com.example.growCare.data.local.inference.LocalDiseaseInference
import com.example.growCare.data.mapper.DiseaseAnalysisMapper
import com.example.growCare.domain.model.DiseaseAnalysis
import com.example.growCare.domain.model.DiseaseSeverity
import com.example.growCare.domain.repository.DetectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of DetectionRepository
 * Handles disease detection and seed quality analysis using Gemini AI
 */
@Singleton
class DetectionRepositoryImpl @Inject constructor(
    private val localDiseaseInference: LocalDiseaseInference,
    private val diseaseAnalysisLocalDataSource: DiseaseAnalysisLocalDataSource,
    private val diseaseAnalysisMapper: DiseaseAnalysisMapper,
) : DetectionRepository {

    private fun getCurrentUserId(): String {
        return "local-user"
    }

    override suspend fun analyzePlantDisease(
        imageUri: Uri,
        cropName: String?,
    ): Result<DiseaseAnalysis> {
        return try {
            val userId = getCurrentUserId()
            val result = localDiseaseInference.detectDisease(imageUri, cropName)

            val diseaseAnalysis = DiseaseAnalysis(
                id = UUID.randomUUID().toString(),
                userId = userId,
                cropName = cropName,
                imageUrl = imageUri.toString(),
                diseaseName = result.diseaseName,
                confidence = result.confidence,
                symptoms = result.symptoms,
                severity = when {
                    result.confidence >= 80 -> DiseaseSeverity.SEVERE
                    result.confidence >= 50 -> DiseaseSeverity.MODERATE
                    else -> DiseaseSeverity.MILD
                },
                treatment = result.treatment,
                prevention = result.prevention,
                additionalNotes = result.additionalNotes,
                timestamp = System.currentTimeMillis(),
            )

            saveDiseaseAnalysis(diseaseAnalysis)
            
            Result.success(diseaseAnalysis)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getDiseaseHistory(): Flow<List<DiseaseAnalysis>> = flow {
        try {
            val userId = getCurrentUserId()
            diseaseAnalysisLocalDataSource.getUserAnalyses(userId).collect { entities ->
                val analyses = diseaseAnalysisMapper.toDomainList(entities)
                emit(analyses)
            }
        } catch (_: Exception) {
            emit(emptyList())
        }
    }

    override suspend fun getDiseaseAnalysisById(analysisId: String): Result<DiseaseAnalysis?> {
        return try {
            val entity = diseaseAnalysisLocalDataSource.getAnalysisById(analysisId)
            Result.success(entity?.let { diseaseAnalysisMapper.toDomain(it) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteDiseaseAnalysis(analysisId: String): Result<Unit> {
        return try {
            val existing = diseaseAnalysisLocalDataSource.getAnalysisById(analysisId)
            existing?.let { diseaseAnalysisLocalDataSource.deleteAnalysis(it) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveDiseaseAnalysis(analysis: DiseaseAnalysis): Result<Unit> {
        return try {
            diseaseAnalysisLocalDataSource.saveAnalysis(
                diseaseAnalysisMapper.toEntity(analysis)
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getDiseasesBySeverity(severity: DiseaseSeverity): Flow<List<DiseaseAnalysis>> = flow {
        getDiseaseHistory().collect { analyses ->
            emit(analyses.filter { it.severity == severity })
        }
    }
}

