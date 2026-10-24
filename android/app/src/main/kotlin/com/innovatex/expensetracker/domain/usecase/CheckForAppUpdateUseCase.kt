package com.innovatex.expensetracker.domain.usecase

import com.innovatex.expensetracker.domain.model.AppUpdateInfo
import com.innovatex.expensetracker.domain.repository.AppUpdateRepository
import javax.inject.Inject

/**
 * UseCase for orchestrating application update checks against GitHub Releases.
 */
class CheckForAppUpdateUseCase @Inject constructor(
    private val appUpdateRepository: AppUpdateRepository
) {
    suspend operator fun invoke(currentVersion: String): Result<AppUpdateInfo?> {
        return appUpdateRepository.checkForUpdate(currentVersion)
    }
}
