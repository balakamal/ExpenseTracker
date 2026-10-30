package com.innovatex.expensetracker.data.remote

import com.innovatex.expensetracker.di.IoDispatcher
import com.innovatex.expensetracker.data.local.NetworkCallLogger
import com.innovatex.expensetracker.domain.model.AppUpdateInfo
import com.innovatex.expensetracker.domain.repository.AppUpdateRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GithubReleaseChecker @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val networkCallLogger: NetworkCallLogger
) : AppUpdateRepository {

    companion object {
        private const val GITHUB_REPO_OWNER = "balakamal"
        private const val GITHUB_REPO_NAME = "ExpenseTracker"
        private const val LATEST_RELEASE_API =
            "https://api.github.com/repos/$GITHUB_REPO_OWNER/$GITHUB_REPO_NAME/releases/latest"
        private const val TIMEOUT_MS = 12000
    }

    override suspend fun checkForUpdate(currentVersion: String): Result<AppUpdateInfo?> = withContext(ioDispatcher) {
        val startedAt = System.currentTimeMillis()
        var callStatus = "network error"
        runCatching {
            val url = URL(LATEST_RELEASE_API)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "ApexExpenseTracker-Android")
            }

            try {
                val responseCode = connection.responseCode
                callStatus = responseCode.toString()
                if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    // No GitHub releases published yet
                    return@runCatching null
                }

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw IllegalStateException("GitHub API returned HTTP code: $responseCode")
                }

                val responseBody = connection.inputStream.use { stream ->
                    BufferedReader(InputStreamReader(stream)).readText()
                }

                val json = JSONObject(responseBody)
                val rawTag = json.optString("tag_name", "").trim()
                val latestCleanVersion = rawTag.removePrefix("v").removePrefix("V")
                val localCleanVersion = currentVersion.trim().removePrefix("v").removePrefix("V")

                val title = json.optString("name", "v$latestCleanVersion")
                val body = json.optString("body", "Bug fixes and performance enhancements.")
                val htmlUrl = json.optString("html_url", "https://github.com/$GITHUB_REPO_OWNER/$GITHUB_REPO_NAME/releases")
                val publishedAt = json.optString("published_at", "")

                // Find attached APK download URL from release assets
                var apkDownloadUrl: String? = null
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    // Priority 1: Match APK containing "ExpenseTracker" or "release"
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val assetName = asset.optString("name", "")
                        if (assetName.endsWith(".apk", ignoreCase = true) &&
                            (assetName.contains("ExpenseTracker", ignoreCase = true) || assetName.contains("release", ignoreCase = true))
                        ) {
                            apkDownloadUrl = asset.optString("browser_download_url", null)
                            break
                        }
                    }
                    // Priority 2: Fallback to any attached .apk
                    if (apkDownloadUrl == null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val assetName = asset.optString("name", "")
                            if (assetName.endsWith(".apk", ignoreCase = true)) {
                                apkDownloadUrl = asset.optString("browser_download_url", null)
                                break
                            }
                        }
                    }
                }

                val hasUpdate = isNewerVersion(remote = latestCleanVersion, local = localCleanVersion)

                AppUpdateInfo(
                    isUpdateAvailable = hasUpdate,
                    currentVersion = currentVersion,
                    latestVersion = latestCleanVersion,
                    releaseTitle = title,
                    releaseNotes = body,
                    releasePageUrl = htmlUrl,
                    downloadApkUrl = apkDownloadUrl ?: htmlUrl,
                    publishedAt = publishedAt
                )
            } finally {
                connection.disconnect()
            }
        }.also { result ->
            networkCallLogger.record("GET", LATEST_RELEASE_API, callStatus, System.currentTimeMillis() - startedAt)
        }
    }

    /**
     * Compares semantic versioning strings (e.g., "1.0.1" vs "1.0.0").
     * Returns true if remote is strictly greater than local.
     */
    fun isNewerVersion(remote: String, local: String): Boolean {
        if (remote.isBlank() || local.isBlank()) return false
        val remoteParts = remote.split("-")[0].split(".").map { it.toIntOrNull() ?: 0 }
        val localParts = local.split("-")[0].split(".").map { it.toIntOrNull() ?: 0 }

        val maxLen = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }
}
