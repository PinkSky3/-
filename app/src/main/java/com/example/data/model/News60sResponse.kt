package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class News60sRootResponse(
    val code: Int? = null,
    val message: String? = null,
    val msg: String? = null,
    val data: News60sDataResponse? = null,
    val api_source: String? = null
)

@JsonClass(generateAdapter = true)
data class News60sDataResponse(
    val date: String? = null,
    val news: List<String>? = null,
    val updated: String? = null,
    val api_updated: String? = null
)
