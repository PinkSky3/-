package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WeatherAlertResponse(
    @Json(name = "province") val province: String? = null,
    @Json(name = "city") val city: String? = null,
    @Json(name = "district") val district: String? = null,
    @Json(name = "adcode") val adcode: String? = null,
    @Json(name = "weather") val weather: String? = null,
    @Json(name = "weather_icon") val weatherIcon: String? = null,
    @Json(name = "temperature") val temperature: Double? = null,
    @Json(name = "wind_direction") val windDirection: String? = null,
    @Json(name = "wind_power") val windPower: String? = null,
    @Json(name = "humidity") val humidity: Int? = null,
    @Json(name = "report_time") val reportTime: String? = null,
    @Json(name = "feels_like") val feelsLike: Double? = null,
    @Json(name = "visibility") val visibility: Double? = null,
    @Json(name = "pressure") val pressure: Double? = null,
    @Json(name = "uv") val uv: Double? = null,
    @Json(name = "precipitation") val precipitation: Double? = null,
    @Json(name = "aqi") val aqi: Int? = null,
    @Json(name = "aqi_category") val aqiCategory: String? = null,
    @Json(name = "alerts") val alerts: List<WeatherAlertItem>? = null,
    @Json(name = "forecast") val forecast: List<WeatherForecastDay>? = null,
    @Json(name = "hourly_forecast") val hourlyForecast: List<HourlyWeatherItem>? = null,
    @Json(name = "minutely_precip") val minutelyPrecip: MinutelyForecast? = null,
    @Json(name = "minutely_forecast") val minutelyForecast: MinutelyForecast? = null
)

@JsonClass(generateAdapter = true)
data class WeatherAlertItem(
    @Json(name = "title") val title: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "level") val level: String? = null,
    @Json(name = "text") val text: String? = null,
    @Json(name = "publish_time") val publishTime: String? = null,
    @Json(name = "publisher") val publisher: String? = null,
    @Json(name = "guidance") val guidance: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class WeatherForecastDay(
    @Json(name = "date") val date: String? = null,
    @Json(name = "week") val week: String? = null,
    @Json(name = "temp_max") val tempMax: Double? = null,
    @Json(name = "temp_min") val tempMin: Double? = null,
    @Json(name = "weather_day") val weatherDay: String? = null,
    @Json(name = "weather_night") val weatherNight: String? = null,
    @Json(name = "wind_dir_day") val windDirDay: String? = null,
    @Json(name = "wind_scale_day") val windScaleDay: String? = null,
    @Json(name = "precip") val precip: Double? = null,
    @Json(name = "pop") val precipitationProbability: Int? = null,
    @Json(name = "uv_index") val uvIndex: Double? = null,
    @Json(name = "sunrise") val sunrise: String? = null,
    @Json(name = "sunset") val sunset: String? = null
)

@JsonClass(generateAdapter = true)
data class HourlyWeatherItem(
    @Json(name = "time") val time: String? = null,
    @Json(name = "temperature") val temperature: Double? = null,
    @Json(name = "weather") val weather: String? = null,
    @Json(name = "wind_direction") val windDirection: String? = null,
    @Json(name = "wind_speed") val windSpeed: Double? = null,
    @Json(name = "humidity") val humidity: Int? = null,
    @Json(name = "precip") val precip: Double? = null,
    @Json(name = "pop") val precipitationProbability: Int? = null
)

@JsonClass(generateAdapter = true)
data class MinutelyForecast(
    @Json(name = "summary") val summary: String? = null,
    @Json(name = "update_time") val updateTime: String? = null
)
