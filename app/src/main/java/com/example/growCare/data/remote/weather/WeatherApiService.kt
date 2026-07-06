package com.example.growCare.data.remote.weather

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit API service for OpenWeatherMap API
 * Handles weather data retrieval
 */
interface WeatherApiService {

    /**
     * Get current weather by coordinates
     * @param lat Latitude
     * @param lon Longitude
     * @param apiKey OpenWeatherMap API key
     * @param units Units of measurement (metric, imperial, standard)
     */
    @GET("weather")
    suspend fun getCurrentWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): OpenWeatherResponse

    /**
     * Get current weather by city name
     * @param city City name
     * @param apiKey OpenWeatherMap API key
     * @param units Units of measurement
     */
    @GET("weather")
    suspend fun getCurrentWeatherByCity(
        @Query("q") city: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): OpenWeatherResponse

    /**
     * Get 5-day weather forecast
     * @param lat Latitude
     * @param lon Longitude
     * @param apiKey OpenWeatherMap API key
     * @param units Units of measurement
     */
    @GET("forecast")
    suspend fun getForecast(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): OpenWeatherForecastResponse
}

// Note: All data classes are defined in OpenWeatherModels.kt

