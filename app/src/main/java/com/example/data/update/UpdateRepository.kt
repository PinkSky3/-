package com.example.data.update

import com.example.data.api.FallbackFetchResult
import com.example.data.api.PublicApiService
import com.example.data.api.RetrofitClient
import com.example.data.api.fetchFirstParsed
import com.example.data.model.AppUpdate
import com.example.data.model.UpdateManifest

sealed interface UpdateCheckResult {
    data class Available(val update: AppUpdate) : UpdateCheckResult
    data object NoUpdate : UpdateCheckResult
    data object Unavailable : UpdateCheckResult
}

class UpdateRepository(
    private val api: PublicApiService = RetrofitClient.publicApi
) {
    private val manifestAdapter = RetrofitClient.moshi.adapter(UpdateManifest::class.java)
    private val manifestUrls = listOf(
        "https://cdn.jsdelivr.net/gh/PinkSky3/DailyHot-Android@main/update.json",
        "https://raw.githubusercontent.com/PinkSky3/DailyHot-Android/main/update.json"
    )

    suspend fun check(currentVersion: String): UpdateCheckResult {
        return when (val result = api.fetchFirstParsed(
            urls = manifestUrls,
            parse = { _, body -> runCatching { manifestAdapter.fromJson(body) }.getOrNull() }
        )) {
            is FallbackFetchResult.Success -> result.value.toUpdateCheckResult(currentVersion)
            is FallbackFetchResult.Failure -> UpdateCheckResult.Unavailable
        }
    }
}

internal fun UpdateManifest.toUpdateCheckResult(currentVersion: String): UpdateCheckResult {
    if (channel != "stable") return UpdateCheckResult.NoUpdate

    val releaseVersion = SemanticVersion.parse(version) ?: return UpdateCheckResult.NoUpdate
    val installedVersion = SemanticVersion.parse(currentVersion) ?: return UpdateCheckResult.Unavailable
    if (releaseVersion <= installedVersion) return UpdateCheckResult.NoUpdate

    val normalizedVersion = releaseVersion.toString()
    val expectedReleasePage =
        "https://github.com/PinkSky3/DailyHot-Android/releases/tag/v$normalizedVersion"
    val expectedDownloadUrl =
        "https://github.com/PinkSky3/DailyHot-Android/releases/download/v$normalizedVersion/" +
            "JuHeZhiXun_ver$normalizedVersion.apk"
    if (releasePageUrl != expectedReleasePage) return UpdateCheckResult.NoUpdate
    if (downloadUrl != expectedDownloadUrl) return UpdateCheckResult.NoUpdate

    return UpdateCheckResult.Available(
        AppUpdate(
            version = normalizedVersion,
            title = title.ifBlank { "聚合智讯 $normalizedVersion" },
            notes = notes.trim(),
            downloadUrl = downloadUrl,
            releasePageUrl = releasePageUrl
        )
    )
}

internal data class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int
) : Comparable<SemanticVersion> {
    override fun compareTo(other: SemanticVersion): Int =
        compareValuesBy(this, other, SemanticVersion::major, SemanticVersion::minor, SemanticVersion::patch)

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        private val stableVersionPattern = Regex("^(\\d+)\\.(\\d+)\\.(\\d+)$")

        fun parse(value: String): SemanticVersion? {
            val match = stableVersionPattern.matchEntire(value.trim()) ?: return null
            return SemanticVersion(
                major = match.groupValues[1].toIntOrNull() ?: return null,
                minor = match.groupValues[2].toIntOrNull() ?: return null,
                patch = match.groupValues[3].toIntOrNull() ?: return null
            )
        }
    }
}
