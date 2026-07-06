package com.example.growCare.data.local.datasource

import com.example.growCare.data.local.database.dao.DiseaseAnalysisDao
import com.example.growCare.data.local.database.entity.DiseaseAnalysisEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local data source for disease analyses
 * Handles all database operations for disease detection history
 */
@Singleton
class DiseaseAnalysisLocalDataSource @Inject constructor(
    private val diseaseAnalysisDao: DiseaseAnalysisDao
) {
    /**
     * Get all analyses for a user
     */
    fun getUserAnalyses(userId: String): Flow<List<DiseaseAnalysisEntity>> {
        return diseaseAnalysisDao.getUserAnalyses(userId)
    }
    
    /**
     * Get a specific analysis by ID
     */
    suspend fun getAnalysisById(analysisId: String): DiseaseAnalysisEntity? {
        return diseaseAnalysisDao.getAnalysisById(analysisId)
    }
    
    /**
     * Get analyses by disease name
     */
    fun getAnalysesByDisease(userId: String, diseaseName: String): Flow<List<DiseaseAnalysisEntity>> {
        return diseaseAnalysisDao.getAnalysesByDisease(userId, diseaseName)
    }
    
    /**
     * Get analyses by severity
     */
    fun getAnalysesBySeverity(userId: String, severity: String): Flow<List<DiseaseAnalysisEntity>> {
        return diseaseAnalysisDao.getAnalysesBySeverity(userId, severity)
    }
    
    /**
     * Get analyses by crop
     */
    fun getAnalysesByCrop(userId: String, cropName: String): Flow<List<DiseaseAnalysisEntity>> {
        return diseaseAnalysisDao.getAnalysesByCrop(userId, cropName)
    }
    
    /**
     * Get recent analyses
     */
    fun getRecentAnalyses(userId: String, sinceTimestamp: Long): Flow<List<DiseaseAnalysisEntity>> {
        return diseaseAnalysisDao.getRecentAnalyses(userId, sinceTimestamp)
    }
    
    /**
     * Save an analysis
     */
    suspend fun saveAnalysis(analysis: DiseaseAnalysisEntity) {
        diseaseAnalysisDao.insertAnalysis(analysis)
    }
    
    /**
     * Save multiple analyses
     */
    suspend fun saveAnalyses(analyses: List<DiseaseAnalysisEntity>) {
        diseaseAnalysisDao.insertAnalyses(analyses)
    }
    
    /**
     * Update an analysis
     */
    suspend fun updateAnalysis(analysis: DiseaseAnalysisEntity) {
        diseaseAnalysisDao.updateAnalysis(analysis)
    }
    
    /**
     * Delete an analysis
     */
    suspend fun deleteAnalysis(analysis: DiseaseAnalysisEntity) {
        diseaseAnalysisDao.deleteAnalysis(analysis)
    }
    
    /**
     * Delete all analyses for a user
     */
    suspend fun deleteUserAnalyses(userId: String) {
        diseaseAnalysisDao.deleteUserAnalyses(userId)
    }
    
    /**
     * Get total analysis count
     */
    suspend fun getAnalysisCount(userId: String): Int {
        return diseaseAnalysisDao.getAnalysisCount(userId)
    }
}
