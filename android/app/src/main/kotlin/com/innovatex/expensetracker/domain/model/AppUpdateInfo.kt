package com.innovatex.expensetracker.domain.model

/**
 * Pure Kotlin domain model representing release update information fetched from GitHub.
 */
data class AppUpdateInfo(
    val isUpdateAvailable: Boolean,
    val currentVersion: String,
    val latestVersion: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val releasePageUrl: String,
    val downloadApkUrl: String?,
    val publishedAt: String = ""
)
