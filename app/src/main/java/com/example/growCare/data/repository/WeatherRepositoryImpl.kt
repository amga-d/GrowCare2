package com.example.growCare.data.repository

import android.location.Location
import android.util.Log
import com.example.growCare.BuildConfig
import com.example.growCare.data.local.location.LocationService
import com.example.growCare.data.remote.weather.WeatherApiService
import com.example.growCare.domain.model.WeatherData
import com.example.growCare.domain.model.WeatherForecast
import com.example.growCare.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of WeatherRepository
 * Handles weather data retrieval from OpenWeatherMap API
 */
@Singleton
class WeatherRepositoryImpl @Inject constructor(
    private val weatherApiService: WeatherApiService,
    private val locationService: LocationService
) : WeatherRepository {

    companion object {
        private const val TAG = "WeatherRepository"
    }

    init {
        // Log API key status (masked for security)
        val apiKey = BuildConfig.WEATHER_API_KEY
        if (apiKey.isEmpty()) {
            Log.e(TAG, "WEATHER_API_KEY is EMPTY! Check local.properties")
        } else {
            val maskedKey = apiKey.take(8) + "..." + apiKey.takeLast(4)
            Log.d(TAG, "Weather API Key loaded: $maskedKey (length: ${apiKey.length})")
        }
    }

    // Default location (Jakarta, Indonesia) as fallback
    private val defaultLat = -6.2088
    private val defaultLon = 106.8456
    private val defaultLocation = "Jakarta, ID"

    // Cache for weather data
    private var cachedWeather: WeatherData? = null
    private var lastFetchTime: Long = 0
    private val cacheValidityDuration = 10 * 60 * 1000L // 10 minutes

    override fun getCurrentWeather(): Flow<WeatherData> = flow {
        Log.d(TAG, "getCurrentWeather: Starting weather fetch")

        // Check cache first
        if (isCacheValid()) {
            Log.d(TAG, "getCurrentWeather: Using cached weather data")
            cachedWeather?.let { emit(it) }
            return@flow
        }

        try {
            // Check location permission
            if (!locationService.hasLocationPermission()) {
                Log.w(TAG, "getCurrentWeather: No location permission, using default location")
                val weatherData = fetchWeatherForDefaultLocation()
                cachedWeather = weatherData
                lastFetchTime = System.currentTimeMillis()
                emit(weatherData)
                return@flow
            }

            // Check if location services are enabled
            if (!locationService.isLocationEnabled()) {
                Log.w(TAG, "getCurrentWeather: Location services disabled, using default location")
                val weatherData = fetchWeatherForDefaultLocation()
                cachedWeather = weatherData
                lastFetchTime = System.currentTimeMillis()
                emit(weatherData)
                return@flow
            }

            // Get user's current location
            Log.d(TAG, "getCurrentWeather: Getting user location")
            val location = locationService.getCurrentLocation()

            val weatherData = if (location != null) {
                Log.d(TAG, "getCurrentWeather: Got location: ${location.latitude}, ${location.longitude}")
                // Fetch weather for user's location
                fetchWeatherForLocation(location)
            } else {
                Log.w(TAG, "getCurrentWeather: Location is null, using default location")
                // Use default location as fallback
                fetchWeatherForDefaultLocation()
            }

            // Update cache
            cachedWeather = weatherData
            lastFetchTime = System.currentTimeMillis()
            Log.d(TAG, "getCurrentWeather: Successfully fetched weather for ${weatherData.location}")

            emit(weatherData)
        } catch (e: Exception) {
            Log.e(TAG, "getCurrentWeather: Error fetching weather", e)
            // If cached data exists, emit it instead of throwing
            if (cachedWeather != null) {
                Log.d(TAG, "getCurrentWeather: Emitting cached data after error")
                emit(cachedWeather!!)
            } else {
                throw Exception("Failed to fetch weather data: ${e.message}")
            }
        }
    }

    override suspend fun getWeatherByCoordinates(
        latitude: Double,
        longitude: Double
    ): Result<WeatherData> {
        return try {
            val response = weatherApiService.getCurrentWeather(
                lat = latitude,
                lon = longitude,
                apiKey = BuildConfig.WEATHER_API_KEY
            )

            val weatherData = WeatherData(
                location = "${response.name}, ${response.sys.country}",
                latitude = response.coord.lat,
                longitude = response.coord.lon,
                temperature = response.main.temp,
                feelsLike = response.main.feelsLike,
                minTemperature = response.main.tempMin,
                maxTemperature = response.main.tempMax,
                humidity = response.main.humidity,
                pressure = response.main.pressure,
                windSpeed = response.wind.speed,
                windDirection = response.wind.deg,
                cloudiness = response.clouds.all,
                description = response.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: "Unknown",
                icon = response.weather.firstOrNull()?.icon ?: "01d",
                visibility = response.visibility,
                sunrise = response.sys.sunrise,
                sunset = response.sys.sunset,
                timestamp = System.currentTimeMillis()
            )

            Result.success(weatherData)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to get weather: ${e.message}"))
        }
    }

    override suspend fun getWeatherByLocation(locationName: String): Result<WeatherData> {
        return try {
            val response = weatherApiService.getCurrentWeatherByCity(
                city = locationName,
                apiKey = BuildConfig.WEATHER_API_KEY
            )

            val weatherData = WeatherData(
                location = "${response.name}, ${response.sys.country}",
                latitude = response.coord.lat,
                longitude = response.coord.lon,
                temperature = response.main.temp,
                feelsLike = response.main.feelsLike,
                minTemperature = response.main.tempMin,
                maxTemperature = response.main.tempMax,
                humidity = response.main.humidity,
                pressure = response.main.pressure,
                windSpeed = response.wind.speed,
                windDirection = response.wind.deg,
                cloudiness = response.clouds.all,
                description = response.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: "Unknown",
                icon = response.weather.firstOrNull()?.icon ?: "01d",
                visibility = response.visibility,
                sunrise = response.sys.sunrise,
                sunset = response.sys.sunset,
                timestamp = System.currentTimeMillis()
            )

            Result.success(weatherData)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to get weather: ${e.message}"))
        }
    }

    override suspend fun getWeatherForecast(
        latitude: Double,
        longitude: Double
    ): Result<WeatherForecast> {
        // TODO: Implement forecast functionality
        return Result.failure(Exception("Forecast not yet implemented"))
    }

    override suspend fun getCachedWeather(): WeatherData? {
        return if (isCacheValid()) cachedWeather else null
    }

    override suspend fun refreshWeather(): Result<Unit> {
        return try {
            // Invalidate cache
            cachedWeather = null
            lastFetchTime = 0

            // Fetch fresh data
            val location = locationService.getCurrentLocation()
            val weatherData = if (location != null) {
                fetchWeatherForLocation(location)
            } else {
                fetchWeatherForDefaultLocation()
            }

            // Update cache
            cachedWeather = weatherData
            lastFetchTime = System.currentTimeMillis()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to refresh weather: ${e.message}"))
        }
    }

    override suspend fun getFarmingAdvice(
        weather: WeatherData,
        cropType: String
    ): Result<String> {
        return try {
            val advice = when {
                weather.description.contains("rain", ignoreCase = true) ->
                    "Avoid irrigation for $cropType today. Good time for indoor tasks. Delay pesticide application."
                weather.temperature > 30.0 && weather.humidity < 30 ->
                    "Increase irrigation frequency for $cropType. Monitor plants for heat stress. Apply mulch."
                weather.temperature < 10.0 ->
                    "Protect $cropType from cold. Delay planting if temperature drops further."
                weather.temperature in 15.0..30.0 && weather.humidity in 40..70 ->
                    "Ideal conditions for $cropType cultivation. Good for spraying and planting."
                else ->
                    "Monitor weather changes for $cropType cultivation. Adjust farming activities accordingly."
            }
            Result.success(advice)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to get farming advice: ${e.message}"))
        }
    }

    /**
     * Check if cached data is still valid
     */
    private fun isCacheValid(): Boolean {
        return cachedWeather != null &&
                (System.currentTimeMillis() - lastFetchTime) < cacheValidityDuration
    }

    /**
     * Fetch weather for user's location
     */
    private suspend fun fetchWeatherForLocation(location: Location): WeatherData {
        Log.d(TAG, "fetchWeatherForLocation: Fetching for ${location.latitude}, ${location.longitude}")
        try {
            val response = weatherApiService.getCurrentWeather(
                lat = location.latitude,
                lon = location.longitude,
                apiKey = BuildConfig.WEATHER_API_KEY
            )

            Log.d(TAG, "fetchWeatherForLocation: Success - ${response.name}, ${response.sys.country}")

            return WeatherData(
                location = "${response.name}, ${response.sys.country}",
                latitude = response.coord.lat,
                longitude = response.coord.lon,
                temperature = response.main.temp,
                feelsLike = response.main.feelsLike,
                minTemperature = response.main.tempMin,
                maxTemperature = response.main.tempMax,
                humidity = response.main.humidity,
                pressure = response.main.pressure,
                windSpeed = response.wind.speed,
                windDirection = response.wind.deg,
                cloudiness = response.clouds.all,
                description = response.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: "Unknown",
                icon = response.weather.firstOrNull()?.icon ?: "01d",
                visibility = response.visibility,
                sunrise = response.sys.sunrise,
                sunset = response.sys.sunset,
                timestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e(TAG, "fetchWeatherForLocation: Error", e)
            throw e
        }
    }

    /**
     * Fetch weather for default location (fallback)
     */
    private suspend fun fetchWeatherForDefaultLocation(): WeatherData {
        Log.d(TAG, "fetchWeatherForDefaultLocation: Using default location $defaultLocation")
        try {
            val response = weatherApiService.getCurrentWeather(
                lat = defaultLat,
                lon = defaultLon,
                apiKey = BuildConfig.WEATHER_API_KEY
            )

            Log.d(TAG, "fetchWeatherForDefaultLocation: Success - ${response.name}, ${response.sys.country}")

            return WeatherData(
                location = defaultLocation,
                latitude = response.coord.lat,
                longitude = response.coord.lon,
                temperature = response.main.temp,
                feelsLike = response.main.feelsLike,
                minTemperature = response.main.tempMin,
                maxTemperature = response.main.tempMax,
                humidity = response.main.humidity,
                pressure = response.main.pressure,
                windSpeed = response.wind.speed,
                windDirection = response.wind.deg,
                cloudiness = response.clouds.all,
                description = response.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: "Unknown",
                icon = response.weather.firstOrNull()?.icon ?: "01d",
                visibility = response.visibility,
                sunrise = response.sys.sunrise,
                sunset = response.sys.sunset,
                timestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e(TAG, "fetchWeatherForDefaultLocation: Error", e)
            throw e
        }
    }
}

