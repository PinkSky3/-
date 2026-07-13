package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GitHubRelease(
    @Json(name = "tag_name") val tagName: String,
    val name: String? = null,
    val body: String? = null,
    @Json(name = "html_url") val htmlUrl: String,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<GitHubReleaseAsset> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GitHubReleaseAsset(
    val name: String,
    val label: String? = null,
    @Json(name = "browser_download_url") val browserDownloadUrl: String,
    val size: Long = 0
)

data class AppUpdate(
    val version: String,
    val title: String,
    val notes: String,
    val downloadUrl: String,
    val releasePageUrl: String
)
