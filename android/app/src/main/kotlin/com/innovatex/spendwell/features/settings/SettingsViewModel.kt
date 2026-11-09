package com.innovatex.spendwell.features.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innovatex.spendwell.data.preferences.UserPreferencesRepository
import com.innovatex.spendwell.data.seeder.SampleFinancialDataSeeder
import com.innovatex.spendwell.database.SpendwellDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val database: SpendwellDatabase,
    @ApplicationContext private val context: Context
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

    val aiEngineMode: StateFlow<String> = userPreferencesRepository.aiEngineMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "AUTO")

    fun setAiEngineMode(mode: String) {
        viewModelScope.launch {
            userPreferencesRepository.setAiEngineMode(mode)
        }
    }

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

    fun seedRealisticDemoData(onCompleted: () -> Unit = {}) {
        viewModelScope.launch {
            SampleFinancialDataSeeder.seedDemoLedger(database, context)
            onCompleted()
        }
    }

    fun clearAllData(onCompleted: () -> Unit = {}) {
        viewModelScope.launch {
            SampleFinancialDataSeeder.clearAllData(database, context)
            onCompleted()
        }
    }
}
