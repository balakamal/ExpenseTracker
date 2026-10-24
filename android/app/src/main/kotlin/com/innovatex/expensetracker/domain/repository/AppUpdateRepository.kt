package com.innovatex.expensetracker.domain.repository

import com.innovatex.expensetracker.domain.model.AppUpdateInfo

/**
 * Domain repository contract for checking available application updates from GitHub releases.
 */
interface AppUpdateRepository {
    suspend fun checkForUpdate(currentVersion: String): Result<AppUpdateInfo?>
}
