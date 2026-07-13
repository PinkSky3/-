package com.example.data.update

import com.example.data.model.GitHubRelease
import com.example.data.model.GitHubReleaseAsset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateRepositoryTest {
    @Test
    fun `newer stable release with matching apk is available`() {
        val result = release(version = "1.4.0").toUpdateCheckResult("1.3.1")

        assertTrue(result is UpdateCheckResult.Available)
        assertEquals("1.4.0", (result as UpdateCheckResult.Available).update.version)
    }

    @Test
    fun `draft and prerelease never become updates`() {
        assertEquals(
            UpdateCheckResult.NoUpdate,
            release(version = "1.4.0", draft = true).toUpdateCheckResult("1.3.1")
        )
        assertEquals(
            UpdateCheckResult.NoUpdate,
            release(version = "1.4.0", prerelease = true).toUpdateCheckResult("1.3.1")
        )
    }

    @Test
    fun `action style or mismatched apk is ignored`() {
        val release = release(
            version = "1.4.0",
            assetName = "app-release.apk"
        )

        assertEquals(UpdateCheckResult.NoUpdate, release.toUpdateCheckResult("1.3.1"))
    }

    @Test
    fun `preview tags and older versions are ignored`() {
        assertEquals(
            UpdateCheckResult.NoUpdate,
            release(version = "1.4.0-beta").toUpdateCheckResult("1.3.1")
        )
        assertEquals(
            UpdateCheckResult.NoUpdate,
            release(version = "1.3.0").toUpdateCheckResult("1.3.1")
        )
    }

    private fun release(
        version: String,
        draft: Boolean = false,
        prerelease: Boolean = false,
        assetName: String = "聚合智讯_ver$version.apk"
    ) = GitHubRelease(
        tagName = "v$version",
        name = "聚合智讯 $version",
        body = "更新内容",
        htmlUrl = "https://github.com/PinkSky3/DailyHot-Android/releases/tag/v$version",
        draft = draft,
        prerelease = prerelease,
        assets = listOf(
            GitHubReleaseAsset(
                name = assetName,
                browserDownloadUrl = "https://github.com/PinkSky3/DailyHot-Android/releases/download/v$version/$assetName",
                size = 1024
            )
        )
    )
}
