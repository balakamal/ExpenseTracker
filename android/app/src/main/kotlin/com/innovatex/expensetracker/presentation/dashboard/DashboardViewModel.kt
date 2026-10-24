package com.innovatex.expensetracker.presentation.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innovatex.expensetracker.BuildConfig
import com.innovatex.expensetracker.data.local.AppSettings
import com.innovatex.expensetracker.data.local.PreferencesDataStore
import com.innovatex.expensetracker.domain.model.MerchantRule
import com.innovatex.expensetracker.domain.repository.CategoryRepository
import com.innovatex.expensetracker.domain.repository.TransactionRepository
import com.innovatex.expensetracker.domain.usecase.CheckForAppUpdateUseCase
import com.innovatex.expensetracker.domain.usecase.GetMonthlyDashboardUseCase
import com.innovatex.expensetracker.domain.usecase.MonthlyDashboardUiState
import com.innovatex.expensetracker.ingestion.sms.HistoricalSmsScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getMonthlyDashboardUseCase: GetMonthlyDashboardUseCase,
    private val historicalSmsScanner: HistoricalSmsScanner,
    private val preferencesDataStore: PreferencesDataStore,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val checkForAppUpdateUseCase: CheckForAppUpdateUseCase
) : ViewModel() {

    private val currentMonthKey = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
    private val _selectedMonth = MutableStateFlow(currentMonthKey)
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    private val _updateState = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)
    val updateState: StateFlow<UpdateCheckState> = _updateState.asStateFlow()

    private val _showUpdateBanner = MutableStateFlow(true)
    val showUpdateBanner: StateFlow<Boolean> = _showUpdateBanner.asStateFlow()

    init {
        viewModelScope.launch {
            transactionRepository.purgeDuplicates()
            val currentSettings = preferencesDataStore.appSettingsFlow.first()
            if (currentSettings.autoCheckUpdates) {
                checkForUpdates(manual = false)
            }
        }
    }

    val settings: StateFlow<AppSettings> = preferencesDataStore.appSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val uiState: StateFlow<MonthlyDashboardUiState> = _selectedMonth
        .flatMapLatest { monthKey ->
            getMonthlyDashboardUseCase(monthKey)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            MonthlyDashboardUiState(monthKey = currentMonthKey, displayMonth = "Loading...", isLoading = true)
        )

    fun selectMonth(monthKey: String) {
        _selectedMonth.value = monthKey
    }

    fun triggerHistoricalScan(context: Context) {
        viewModelScope.launch {
            historicalSmsScanner.scanLast30Days(context)
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(id)
        }
    }

    fun reassignCategory(transactionId: String, newCategoryId: String, createMerchantRule: Boolean) {
        viewModelScope.launch {
            val txn = transactionRepository.getTransactionById(transactionId) ?: return@launch
            val updated = txn.copy(categoryId = newCategoryId)
            transactionRepository.updateTransaction(updated)

            if (createMerchantRule && txn.merchantClean.isNotBlank()) {
                categoryRepository.saveRule(
                    MerchantRule(
                        pattern = txn.merchantClean,
                        categoryId = newCategoryId,
                        isRegex = false,
                        priority = 100
                    )
                )
            }
        }
    }

    fun toggleSms(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataStore.setSmsTrackingEnabled(enabled)
        }
    }

    fun toggleEmail(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataStore.setEmailTrackingEnabled(enabled)
        }
    }

    fun checkForUpdates(manual: Boolean = false) {
        viewModelScope.launch {
            _updateState.value = UpdateCheckState.Checking
            val currentVersion = BuildConfig.VERSION_NAME
            checkForAppUpdateUseCase(currentVersion)
                .onSuccess { updateInfo ->
                    if (updateInfo != null && updateInfo.isUpdateAvailable) {
                        _updateState.value = UpdateCheckState.Available(updateInfo)
                        _showUpdateBanner.value = true
                    } else {
                        _updateState.value = UpdateCheckState.UpToDate(currentVersion)
                    }
                }
                .onFailure { error ->
                    if (manual) {
                        _updateState.value = UpdateCheckState.Error(
                            error.localizedMessage ?: "Failed to reach GitHub Releases"
                        )
                    } else {
                        // Silent fallback for automatic background startup checks
                        _updateState.value = UpdateCheckState.Idle
                    }
                }
        }
    }

    fun dismissUpdateBanner() {
        _showUpdateBanner.value = false
    }

    fun toggleAutoCheckUpdates(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataStore.setAutoCheckUpdates(enabled)
            if (enabled && _updateState.value is UpdateCheckState.Idle) {
                checkForUpdates(manual = false)
            }
        }
    }
}
