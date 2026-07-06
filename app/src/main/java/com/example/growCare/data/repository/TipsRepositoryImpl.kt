package com.example.growCare.data.repository

import com.example.growCare.data.local.database.entity.AITipEntity
import com.example.growCare.data.local.datasource.AITipLocalDataSource
import com.example.growCare.domain.model.WeatherData
import com.example.growCare.domain.repository.TipsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of TipsRepository
 * Handles AI tip generation with intelligent caching strategy:
 * 
 * Cache Strategy:
 * 1. Check local cache first for valid (non-expired) tips
 * 2. If cache is valid, return cached tips immediately
 * 3. If cache is expired or missing, generate new tips with AI
 * 4. Save generated tips to cache with 24-hour expiration
 * 5. Clean up expired tips periodically
 */
@Singleton
class TipsRepositoryImpl @Inject constructor(
    private val localDataSource: AITipLocalDataSource
) : TipsRepository {

    companion object {
        // Cache expiration time: 24 hours in milliseconds
        private const val CACHE_EXPIRATION_MS = 24 * 60 * 60 * 1000L
    }

    override fun generateAITips(weather: WeatherData): Flow<List<Pair<String, String>>> = flow {
        // Create a cache key based on weather conditions
        val weatherKey = createWeatherKey(weather)
        
        // Step 1: Check cache first
        val cachedTips = localDataSource.getTipsForWeather(weatherKey)
        
        if (cachedTips.isNotEmpty() && !isCacheExpired(cachedTips.first())) {
            // Cache hit and still valid - return immediately
            emit(cachedTips.map { it.title to it.description })
            return@flow
        }
        
        // Step 2: Cache miss or expired - generate new tips
        try {
            val tips = getDefaultTips(weather)
            
            // Step 3: Save to cache for future use
            if (tips.isNotEmpty()) {
                saveTipsToCache(tips, weatherKey)
            }
            
            // Step 4: Clean up old expired tips
            localDataSource.cleanupExpiredTips()
            
            // Emit the tips
            emit(tips)
            
        } catch (e: Exception) {
            // On error, try to return expired cache or fallback to defaults
            if (cachedTips.isNotEmpty()) {
                emit(cachedTips.map { it.title to it.description })
            } else {
                emit(getDefaultTips(weather))
            }
        }
    }

    /**
     * Create a stable cache key based on weather conditions
     * Groups similar weather to maximize cache hits
     */
    private fun createWeatherKey(weather: WeatherData): String {
        // Round temperature to nearest 5 degrees for better cache hits
        val tempRange = (weather.temperature.toInt() / 5) * 5
        
        // Round humidity to nearest 10%
        val humidityRange = (weather.humidity / 10) * 10
        
        // Simplified weather description
        val weatherType = when {
            weather.description.contains("rain", ignoreCase = true) -> "rainy"
            weather.description.contains("clear", ignoreCase = true) -> "clear"
            weather.description.contains("cloud", ignoreCase = true) -> "cloudy"
            else -> "normal"
        }
        
        // Get current season
        val month = Calendar.getInstance().get(Calendar.MONTH)
        val season = when (month) {
            0, 1, 11 -> "winter"
            2, 3, 4 -> "spring"
            5, 6, 7 -> "summer"
            else -> "fall"
        }
        
        return "${weatherType}_${tempRange}_${humidityRange}_${season}"
    }

    /**
     * Check if cached tip has expired
     */
    private fun isCacheExpired(tip: AITipEntity): Boolean {
        return System.currentTimeMillis() > tip.expiresAt
    }

    /**
     * Save tips to local cache with expiration
     */
    private suspend fun saveTipsToCache(tips: List<Pair<String, String>>, weatherKey: String) {
        val currentTime = System.currentTimeMillis()
        val expirationTime = currentTime + CACHE_EXPIRATION_MS
        
        val entities = tips.map { (title, description) ->
            AITipEntity(
                id = UUID.randomUUID().toString(),
                title = title,
                description = description,
                weatherConditions = weatherKey,
                timestamp = currentTime,
                expiresAt = expirationTime
            )
        }
        
        localDataSource.saveTips(entities)
    }

    /**
     * Get default tips as fallback when AI is unavailable
     */
    private fun getDefaultTips(weather: WeatherData): List<Pair<String, String>> {
        val tip1 = when {
            weather.description.contains("rain", ignoreCase = true) -> 
                "Rainy Weather" to "Avoid fertilizer application and check drainage systems."
            weather.temperature > 30 -> 
                "Hot Weather" to "Increase irrigation and monitor crops for heat stress."
            weather.temperature < 10 -> 
                "Cold Protection" to "Protect sensitive crops from frost damage."
            else -> 
                "Optimal Conditions" to "Good weather for general farming activities."
        }
        
        val tip2 = when (Calendar.getInstance().get(Calendar.MONTH)) {
            0, 1, 11 -> "Winter Care" to "Focus on soil preparation and equipment maintenance."
            2, 3, 4 -> "Spring Planting" to "Ideal time for sowing cool-season crops."
            5, 6, 7 -> "Summer Watch" to "Monitor irrigation and watch for pests."
            else -> "Fall Harvest" to "Time to harvest and prepare for winter crops."
        }
        
        return listOf(tip1, tip2)
    }
}
