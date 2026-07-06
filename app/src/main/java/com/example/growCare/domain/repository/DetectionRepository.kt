package com.example.growCare.domain.repository

import android.net.Uri
import com.example.growCare.domain.model.DiseaseAnalysis
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for disease detection and seed quality analysis
 * Handles AI-powered image analysis for plant health
 */
interface DetectionRepository {
    
    /**
     * Analyze plant image for disease detection
     * @param imageUri URI of the plant image
     * @param cropName Optional crop name for context
     * @return Result with DiseaseAnalysis
     */
    suspend fun analyzePlantDisease(
        imageUri: Uri,
        cropName: String? = null
    ): Result<DiseaseAnalysis>
    
    /**
     * Get disease scan history
     * @return Flow of disease analyses, sorted by timestamp
     */
    fun getDiseaseHistory(): Flow<List<DiseaseAnalysis>>
    
    /**
     * Get a specific disease analysis by ID
     */
    suspend fun getDiseaseAnalysisById(analysisId: String): Result<DiseaseAnalysis?>
    
    /**
     * Delete a disease analysis record
     */
    suspend fun deleteDiseaseAnalysis(analysisId: String): Result<Unit>
    
    /**
     * Save disease analysis to history
     */
    suspend fun saveDiseaseAnalysis(analysis: DiseaseAnalysis): Result<Unit>
    
    /**
     * Get disease analyses by severity
     */
    fun getDiseasesBySeverity(
        severity: com.example.growCare.domain.model.DiseaseSeverity
    ): Flow<List<DiseaseAnalysis>>
}
