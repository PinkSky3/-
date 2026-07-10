package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WeatherAlertResponse(
    @Json(name = "province") val province: String? = null,
    @Json(name = "city") val city: String? = null,
    @Json(name = "district") val district: String? = null,
    @Json(name = "weather") val weather: String? = null,
    @Json(name = "temperature") val temperature: Double? = null,
    @Json(name = "report_time") val reportTime: String? = null,
    @Json(name = "alerts") val alerts: List<WeatherAlertItem>? = null,
    @Json(name = "forecast") val forecast: List<WeatherForecastDay>? = null,
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
    @Json(name = "precip") val precip: Double? = null
)

@JsonClass(generateAdapter = true)
data class MinutelyForecast(
    @Json(name = "summary") val summary: String? = null,
    @Json(name = "update_time") val updateTime: String? = null
)
