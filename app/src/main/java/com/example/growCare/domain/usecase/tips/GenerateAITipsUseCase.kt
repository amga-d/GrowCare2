package com.example.growCare.domain.usecase.tips

import com.example.growCare.domain.model.WeatherData
import com.example.growCare.domain.repository.TipsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for generating AI-powered agricultural tips
 * 
 * This use case generates personalized farming tips based on:
 * - Current weather conditions
 * - Season
 * - Location
 * 
 * Business Logic:
 * 1. Takes weather data as input
 * 2. Requests AI-generated tips from repository
 * 3. Returns list of tip pairs (title, description)
 */
class GenerateAITipsUseCase @Inject constructor(
    private val repository: TipsRepository
) {
    /**
     * Generate AI tips based on weather and season
     * 
     * @param weather Current weather data
     * @return Flow of list of tips (title to description pairs)
     */
    operator fun invoke(weather: WeatherData): Flow<List<Pair<String, String>>> {
        return repository.generateAITips(weather)
    }
}
