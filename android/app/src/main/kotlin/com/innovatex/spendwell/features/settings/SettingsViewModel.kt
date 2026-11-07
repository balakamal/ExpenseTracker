package com.innovatex.spendwell.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innovatex.spendwell.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val cycleStartDay: StateFlow<Int> = userPreferencesRepository.cycleStartDay
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 1)

    val isPrivateMode: StateFlow<Boolean> = userPreferencesRepository.isPrivateMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val isModelDownloaded: StateFlow<Boolean> = userPreferencesRepository.isModelDownloaded
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val userName: StateFlow<String> = userPreferencesRepository.userName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val themeMode: StateFlow<String> = userPreferencesRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "SYSTEM")

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            userPreferencesRepository.setThemeMode(mode)
        }
    }

    fun setUserName(name: String) {
        viewModelScope.launch {
            userPreferencesRepository.setUserName(name)
        }
    }

    fun setCycleStartDay(day: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setCycleStartDay(day)
        }
    }

    fun setPrivateMode(isPrivate: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setPrivateMode(isPrivate)
        }
    }
}
