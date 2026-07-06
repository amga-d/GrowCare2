package com.example.growCare.data.local.database.dao

import androidx.room.*
import com.example.growCare.data.local.database.entity.AITipEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for AI Tips
 * Handles CRUD operations for cached agricultural tips
 */
@Dao
interface AITipDao {
    
    /**
     * Get all non-expired tips for specific weather conditions
     * 
     * @param weatherConditions Serialized weather conditions
     * @param currentTime Current timestamp to filter expired tips
     * @return Flow of valid tips
     */
    @Query("SELECT * FROM ai_tips WHERE weatherConditions = :weatherConditions AND expiresAt > :currentTime ORDER BY timestamp DESC")
    fun getTipsForWeather(weatherConditions: String, currentTime: Long): Flow<List<AITipEntity>>
    
    /**
     * Get all tips (including expired) for specific weather conditions
     * 
     * @param weatherConditions Serialized weather conditions
     * @return List of all tips
     */
    @Query("SELECT * FROM ai_tips WHERE weatherConditions = :weatherConditions ORDER BY timestamp DESC LIMIT 2")
    suspend fun getTipsByWeather(weatherConditions: String): List<AITipEntity>
    
    /**
     * Insert a new tip or replace if exists
     * 
     * @param tip The tip to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTip(tip: AITipEntity)
    
    /**
     * Insert multiple tips
     * 
     * @param tips List of tips to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTips(tips: List<AITipEntity>)
    
    /**
     * Delete expired tips to keep database clean
     * 
     * @param currentTime Current timestamp
     * @return Number of deleted rows
     */
    @Query("DELETE FROM ai_tips WHERE expiresAt < :currentTime")
    suspend fun deleteExpiredTips(currentTime: Long): Int
    
    /**
     * Delete all tips (for testing or manual refresh)
     */
    @Query("DELETE FROM ai_tips")
    suspend fun deleteAllTips()
    
    /**
     * Get total count of cached tips
     */
    @Query("SELECT COUNT(*) FROM ai_tips")
    suspend fun getTipCount(): Int
}
