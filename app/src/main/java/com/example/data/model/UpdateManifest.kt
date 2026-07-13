package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UpdateManifest(
    val channel: String,
    val version: String,
    val title: String,
    val notes: String,
    val downloadUrl: String,
    val releasePageUrl: String
)

data class AppUpdate(
    val version: String,
    val title: String,
    val notes: String,
    val downloadUrl: String,
    val releasePageUrl: String
)
