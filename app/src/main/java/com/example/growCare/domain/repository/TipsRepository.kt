package com.example.growCare.domain.repository

import com.example.growCare.domain.model.WeatherData
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for agricultural tips
 * 
 * Defines contract for getting farming tips and advice.
 * Implementation in data layer handles AI generation and fallback logic.
 */
interface TipsRepository {
    
    /**
     * Generate AI-powered agricultural tips based on weather and season
     * 
     * @param weather Current weather data including temperature, humidity, etc.
     * @return Flow emitting list of tips (title to description pairs)
     */
    fun generateAITips(weather: WeatherData): Flow<List<Pair<String, String>>>
}
