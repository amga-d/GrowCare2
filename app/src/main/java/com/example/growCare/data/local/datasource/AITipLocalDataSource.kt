package com.example.growCare.data.local.datasource

import com.example.growCare.data.local.database.dao.AITipDao
import com.example.growCare.data.local.database.entity.AITipEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local data source for AI Tips
 * Abstracts Room DAO operations for tips caching
 */
@Singleton
class AITipLocalDataSource @Inject constructor(
    private val aiTipDao: AITipDao
) {
    
    /**
     * Get cached tips for specific weather conditions that haven't expired
     * 
     * @param weatherConditions Serialized weather key
     * @return Flow of valid cached tips
     */
    fun getValidTipsForWeather(weatherConditions: String): Flow<List<AITipEntity>> {
        val currentTime = System.currentTimeMillis()
        return aiTipDao.getTipsForWeather(weatherConditions, currentTime)
    }
    
    /**
     * Get any tips for weather (even expired) for immediate display
     * 
     * @param weatherConditions Serialized weather key
     * @return List of tips
     */
    suspend fun getTipsForWeather(weatherConditions: String): List<AITipEntity> {
        return aiTipDao.getTipsByWeather(weatherConditions)
    }
    
    /**
     * Save tips to cache
     * 
     * @param tips List of tips to cache
     */
    suspend fun saveTips(tips: List<AITipEntity>) {
        aiTipDao.insertTips(tips)
    }
    
    /**
     * Save a single tip
     * 
     * @param tip Tip to save
     */
    suspend fun saveTip(tip: AITipEntity) {
        aiTipDao.insertTip(tip)
    }
    
    /**
     * Clean up expired tips from database
     * 
     * @return Number of tips removed
     */
    suspend fun cleanupExpiredTips(): Int {
        val currentTime = System.currentTimeMillis()
        return aiTipDao.deleteExpiredTips(currentTime)
    }
    
    /**
     * Clear all cached tips
     */
    suspend fun clearAllTips() {
        aiTipDao.deleteAllTips()
    }
    
    /**
     * Get count of cached tips
     */
    suspend fun getCacheSize(): Int {
        return aiTipDao.getTipCount()
    }
}
