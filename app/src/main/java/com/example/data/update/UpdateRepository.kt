package com.example.data.update

import com.example.data.api.PublicApiService
import com.example.data.api.RetrofitClient
import com.example.data.model.AppUpdate
import com.example.data.model.GitHubRelease
import kotlinx.coroutines.CancellationException

sealed interface UpdateCheckResult {
    data class Available(val update: AppUpdate) : UpdateCheckResult
    data object NoUpdate : UpdateCheckResult
    data object Unavailable : UpdateCheckResult
}

class UpdateRepository(
    private val api: PublicApiService = RetrofitClient.publicApi
) {
    suspend fun check(currentVersion: String): UpdateCheckResult {
        return try {
            val response = api.fetchLatestGitHubRelease()
            if (response.code() == 404) return UpdateCheckResult.NoUpdate
            if (!response.isSuccessful) return UpdateCheckResult.Unavailable
            response.body()?.toUpdateCheckResult(currentVersion) ?: UpdateCheckResult.Unavailable
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            UpdateCheckResult.Unavailable
        }
    }
}

internal fun GitHubRelease.toUpdateCheckResult(currentVersion: String): UpdateCheckResult {
    if (draft || prerelease) return UpdateCheckResult.NoUpdate

    val releaseVersion = SemanticVersion.parse(tagName) ?: return UpdateCheckResult.NoUpdate
    val installedVersion = SemanticVersion.parse(currentVersion) ?: return UpdateCheckResult.Unavailable
    if (releaseVersion <= installedVersion) return UpdateCheckResult.NoUpdate

    val version = releaseVersion.toString()
    val expectedAssetName = "聚合智讯_ver$version.apk"
    val apk = assets.firstOrNull {
        it.name == expectedAssetName &&
            it.size > 0 &&
            it.browserDownloadUrl.startsWith("https://")
    } ?: return UpdateCheckResult.NoUpdate

    return UpdateCheckResult.Available(
        AppUpdate(
            version = version,
            title = name?.takeIf { it.isNotBlank() } ?: "聚合智讯 $version",
            notes = body?.trim().orEmpty(),
            downloadUrl = apk.browserDownloadUrl,
            releasePageUrl = htmlUrl
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
        private val stableVersionPattern = Regex("^v?(\\d+)\\.(\\d+)\\.(\\d+)$")

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
