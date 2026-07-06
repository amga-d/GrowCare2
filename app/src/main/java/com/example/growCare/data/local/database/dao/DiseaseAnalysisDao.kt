package com.example.growCare.data.local.database.dao

import androidx.room.*
import com.example.growCare.data.local.database.entity.DiseaseAnalysisEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for disease analyses
 * Provides methods for CRUD operations on disease analysis records
 */
@Dao
interface DiseaseAnalysisDao {
    
    /**
     * Get all disease analyses for a specific user
     * Returns a Flow for reactive updates
     */
    @Query("SELECT * FROM disease_analyses WHERE userId = :userId ORDER BY timestamp DESC")
    fun getUserAnalyses(userId: String): Flow<List<DiseaseAnalysisEntity>>
    
    /**
     * Get a specific analysis by ID
     */
    @Query("SELECT * FROM disease_analyses WHERE id = :analysisId")
    suspend fun getAnalysisById(analysisId: String): DiseaseAnalysisEntity?
    
    /**
     * Get analyses by disease name
     */
    @Query("SELECT * FROM disease_analyses WHERE userId = :userId AND diseaseName = :diseaseName ORDER BY timestamp DESC")
    fun getAnalysesByDisease(userId: String, diseaseName: String): Flow<List<DiseaseAnalysisEntity>>
    
    /**
     * Get analyses by severity
     */
    @Query("SELECT * FROM disease_analyses WHERE userId = :userId AND severity = :severity ORDER BY timestamp DESC")
    fun getAnalysesBySeverity(userId: String, severity: String): Flow<List<DiseaseAnalysisEntity>>
    
    /**
     * Get analyses by crop name
     */
    @Query("SELECT * FROM disease_analyses WHERE userId = :userId AND cropName = :cropName ORDER BY timestamp DESC")
    fun getAnalysesByCrop(userId: String, cropName: String): Flow<List<DiseaseAnalysisEntity>>
    
    /**
     * Get recent analyses (last N days)
     */
    @Query("SELECT * FROM disease_analyses WHERE userId = :userId AND timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    fun getRecentAnalyses(userId: String, sinceTimestamp: Long): Flow<List<DiseaseAnalysisEntity>>
    
    /**
     * Insert a single analysis
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalysis(analysis: DiseaseAnalysisEntity)
    
    /**
     * Insert multiple analyses
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalyses(analyses: List<DiseaseAnalysisEntity>)
    
    /**
     * Update an analysis
     */
    @Update
    suspend fun updateAnalysis(analysis: DiseaseAnalysisEntity)
    
    /**
     * Delete an analysis
     */
    @Delete
    suspend fun deleteAnalysis(analysis: DiseaseAnalysisEntity)
    
    /**
     * Delete all analyses for a user
     */
    @Query("DELETE FROM disease_analyses WHERE userId = :userId")
    suspend fun deleteUserAnalyses(userId: String)
    
    /**
     * Get total count of analyses for a user
     */
    @Query("SELECT COUNT(*) FROM disease_analyses WHERE userId = :userId")
    suspend fun getAnalysisCount(userId: String): Int
}
