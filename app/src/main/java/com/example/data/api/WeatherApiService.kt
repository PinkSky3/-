package com.example.data.api

import com.example.data.model.WeatherAlertResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

interface WeatherApiService {
    @GET
    suspend fun fetch(@Url url: String): Response<WeatherAlertResponse>
}
