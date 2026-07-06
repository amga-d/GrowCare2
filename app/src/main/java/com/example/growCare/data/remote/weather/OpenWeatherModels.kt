package com.example.growCare.data.remote.weather

import com.google.gson.annotations.SerializedName

/**
 * OpenWeatherMap API Response Model
 * Represents the JSON response from OpenWeatherMap current weather API
 */
data class OpenWeatherResponse(
    @SerializedName("coord")
    val coord: Coordinates,

    @SerializedName("weather")
    val weather: List<Weather>,

    @SerializedName("base")
    val base: String,

    @SerializedName("main")
    val main: Main,

    @SerializedName("visibility")
    val visibility: Int,

    @SerializedName("wind")
    val wind: Wind,

    @SerializedName("clouds")
    val clouds: Clouds,

    @SerializedName("dt")
    val dt: Long,

    @SerializedName("sys")
    val sys: Sys,

    @SerializedName("timezone")
    val timezone: Int,

    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("cod")
    val cod: Int
)

/**
 * Coordinates data
 */
data class Coordinates(
    @SerializedName("lon")
    val lon: Double,

    @SerializedName("lat")
    val lat: Double
)

/**
 * Weather condition data
 */
data class Weather(
    @SerializedName("id")
    val id: Int,

    @SerializedName("main")
    val main: String,

    @SerializedName("description")
    val description: String,

    @SerializedName("icon")
    val icon: String
)

/**
 * Main weather parameters
 */
data class Main(
    @SerializedName("temp")
    val temp: Double,

    @SerializedName("feels_like")
    val feelsLike: Double,

    @SerializedName("temp_min")
    val tempMin: Double,

    @SerializedName("temp_max")
    val tempMax: Double,

    @SerializedName("pressure")
    val pressure: Int,

    @SerializedName("humidity")
    val humidity: Int,

    @SerializedName("sea_level")
    val seaLevel: Int? = null,

    @SerializedName("grnd_level")
    val grndLevel: Int? = null
)

/**
 * Wind data
 */
data class Wind(
    @SerializedName("speed")
    val speed: Double,

    @SerializedName("deg")
    val deg: Int,

    @SerializedName("gust")
    val gust: Double? = null
)

/**
 * Clouds data
 */
data class Clouds(
    @SerializedName("all")
    val all: Int
)

/**
 * System data (sunrise/sunset)
 */
data class Sys(
    @SerializedName("type")
    val type: Int? = null,

    @SerializedName("id")
    val id: Int? = null,

    @SerializedName("country")
    val country: String,

    @SerializedName("sunrise")
    val sunrise: Long,

    @SerializedName("sunset")
    val sunset: Long
)

/**
 * Forecast response model
 */
data class OpenWeatherForecastResponse(
    @SerializedName("cod")
    val cod: String,

    @SerializedName("message")
    val message: Int,

    @SerializedName("cnt")
    val cnt: Int,

    @SerializedName("list")
    val list: List<ForecastItem>,

    @SerializedName("city")
    val city: City
)

/**
 * Forecast item data
 */
data class ForecastItem(
    @SerializedName("dt")
    val dt: Long,

    @SerializedName("main")
    val main: Main,

    @SerializedName("weather")
    val weather: List<Weather>,

    @SerializedName("clouds")
    val clouds: Clouds,

    @SerializedName("wind")
    val wind: Wind,

    @SerializedName("visibility")
    val visibility: Int,

    @SerializedName("pop")
    val pop: Double,

    @SerializedName("sys")
    val sys: ForecastSys,

    @SerializedName("dt_txt")
    val dtTxt: String
)

/**
 * Forecast system data
 */
data class ForecastSys(
    @SerializedName("pod")
    val pod: String
)

/**
 * City data in forecast
 */
data class City(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("coord")
    val coord: Coordinates,

    @SerializedName("country")
    val country: String,

    @SerializedName("population")
    val population: Int,

    @SerializedName("timezone")
    val timezone: Int,

    @SerializedName("sunrise")
    val sunrise: Long,

    @SerializedName("sunset")
    val sunset: Long
)

