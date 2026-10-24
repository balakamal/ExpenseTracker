package com.innovatex.expensetracker.presentation.dashboard

import androidx.compose.runtime.Immutable
import com.innovatex.expensetracker.domain.model.AppUpdateInfo

@Immutable
sealed interface UpdateCheckState {
    data object Idle : UpdateCheckState
    data object Checking : UpdateCheckState
    data class Available(val updateInfo: AppUpdateInfo) : UpdateCheckState
    data class UpToDate(val currentVersion: String) : UpdateCheckState
    data class Error(val message: String) : UpdateCheckState
}
