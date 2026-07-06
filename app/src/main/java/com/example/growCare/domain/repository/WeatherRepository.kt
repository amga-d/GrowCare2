package com.example.growCare.domain.repository

import com.example.growCare.domain.model.WeatherData
import com.example.growCare.domain.model.WeatherForecast
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for weather data operations
 * Handles fetching and caching weather information
 */
interface WeatherRepository {
    
    /**
     * Get current weather data for user's location
     * @return Flow of WeatherData, updates periodically
     */
    fun getCurrentWeather(): Flow<WeatherData>
    
    /**
     * Get weather data for specific coordinates
     * @param latitude Latitude coordinate
     * @param longitude Longitude coordinate
     * @return Result with current WeatherData
     */
    suspend fun getWeatherByCoordinates(
        latitude: Double,
        longitude: Double
    ): Result<WeatherData>
    
    /**
     * Get weather data for a specific location by name
     * @param locationName City or location name
     * @return Result with current WeatherData
     */
    suspend fun getWeatherByLocation(locationName: String): Result<WeatherData>
    
    /**
     * Get weather forecast for next 7 days
     * @return Result with WeatherForecast
     */
    suspend fun getWeatherForecast(
        latitude: Double,
        longitude: Double
    ): Result<WeatherForecast>
    
    /**
     * Get cached weather data (for offline use)
     * @return Last cached WeatherData or null
     */
    suspend fun getCachedWeather(): WeatherData?
    
    /**
     * Refresh weather data from remote source
     */
    suspend fun refreshWeather(): Result<Unit>
    
    /**
     * Get farming advice based on current weather
     * Uses AI to generate contextual advice
     */
    suspend fun getFarmingAdvice(
        weather: WeatherData,
        cropType: String
    ): Result<String>
}
