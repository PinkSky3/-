package com.example.data.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

interface RawApiService {
    @GET
    suspend fun fetch(@Url url: String): Response<ResponseBody>
}
