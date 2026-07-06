package com.example.growCare.domain.model

/**
 * Domain model representing weather data
 */
data class WeatherData(
    val location: String,
    val latitude: Double,
    val longitude: Double,
    val temperature: Double, // in Celsius
    val feelsLike: Double,
    val minTemperature: Double,
    val maxTemperature: Double,
    val humidity: Int, // percentage
    val pressure: Int, // hPa
    val windSpeed: Double, // m/s
    val windDirection: Int, // degrees
    val cloudiness: Int, // percentage
    val description: String, // e.g., "Clear sky", "Light rain"
    val icon: String, // weather icon code
    val visibility: Int, // meters
    val uvIndex: Double? = null,
    val precipitation: Double? = null, // mm
    val sunrise: Long,
    val sunset: Long,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Weather forecast for future days
 */
data class WeatherForecast(
    val location: String,
    val forecasts: List<DailyForecast>
)

/**
 * Daily weather forecast
 */
data class DailyForecast(
    val date: Long,
    val tempMin: Double,
    val tempMax: Double,
    val description: String,
    val icon: String,
    val humidity: Int,
    val windSpeed: Double,
    val precipitation: Double? = null,
    val precipitationProbability: Int? = null // percentage
)

/**
 * Helper functions for weather conditions
 */
fun WeatherData.isRainy(): Boolean {
    return description.contains("rain", ignoreCase = true)
}

fun WeatherData.isSunny(): Boolean {
    return description.contains("clear", ignoreCase = true) ||
           description.contains("sunny", ignoreCase = true)
}

fun WeatherData.isCloudy(): Boolean {
    return cloudiness > 50
}

fun WeatherData.isHot(): Boolean {
    return temperature > 30.0
}

fun WeatherData.isCold(): Boolean {
    return temperature < 10.0
}

fun WeatherData.isGoodForFarming(): Boolean {
    return !isRainy() && temperature in 15.0..30.0 && humidity in 40..70
}

/**
 * Get farming advice based on weather
 */
fun WeatherData.getFarmingAdvice(): String {
    return when {
        isRainy() -> "Avoid irrigation today. Good time for indoor tasks. Delay pesticide application."
        isHot() && humidity < 30 -> "Increase irrigation frequency. Monitor plants for heat stress. Apply mulch."
        isCold() -> "Protect sensitive crops from cold. Delay planting if temperature drops further."
        isGoodForFarming() -> "Ideal conditions for most farming activities. Good for spraying and planting."
        else -> "Monitor weather changes. Adjust farming activities accordingly."
    }
}

/**
 * Convert temperature to Fahrenheit
 */
fun Double.toFahrenheit(): Double {
    return (this * 9/5) + 32
}

/**
 * Convert wind speed from m/s to km/h
 */
fun Double.toKmh(): Double {
    return this * 3.6
}
