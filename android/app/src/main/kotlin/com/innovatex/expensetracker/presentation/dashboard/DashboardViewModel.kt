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

    private val _selectedMonth = MutableStateFlow("ALL")
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    private val _selectedDateRange = MutableStateFlow<Pair<Long, Long>?>(null)
    val selectedDateRange: StateFlow<Pair<Long, Long>?> = _selectedDateRange.asStateFlow()

    // Current displayed calendar month (timestamp millis of the 1st of that month)
    private val _calendarMonthMillis = MutableStateFlow(getStartOfCurrentMonth())
    val calendarMonthMillis: StateFlow<Long> = _calendarMonthMillis.asStateFlow()

    private val _updateState = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)
    val updateState: StateFlow<UpdateCheckState> = _updateState.asStateFlow()

    private val _showUpdateBanner = MutableStateFlow(true)
    val showUpdateBanner: StateFlow<Boolean> = _showUpdateBanner.asStateFlow()

    private val _cleanSpamStatus = MutableStateFlow<String?>(null)
    val cleanSpamStatus: StateFlow<String?> = _cleanSpamStatus.asStateFlow()

    init {
        viewModelScope.launch {
            transactionRepository.purgeDuplicates()
            val purged = transactionRepository.purgeSpamTransactions()
            if (purged > 0) {
                _cleanSpamStatus.value = "Automatically filtered $purged spam transactions."
            }
            val currentSettings = preferencesDataStore.appSettingsFlow.first()
            if (currentSettings.autoCheckUpdates) {
                checkForUpdates(manual = false)
            }
        }
    }

    val settings: StateFlow<AppSettings> = preferencesDataStore.appSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val uiState: StateFlow<MonthlyDashboardUiState> = kotlinx.coroutines.flow.combine(
        _selectedMonth,
        _selectedDateRange
    ) { monthKey, dateRange ->
        Pair(monthKey, dateRange)
    }.flatMapLatest { (monthKey, dateRange) ->
        getMonthlyDashboardUseCase(
            monthKey = if (monthKey == "ALL") null else monthKey,
            startDateMillis = dateRange?.first,
            endDateMillis = dateRange?.second
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        MonthlyDashboardUiState(monthKey = "ALL", displayMonth = "Overall Ledger", isLoading = true)
    )

    fun onDayTapped(timestampMillis: Long) {
        val current = _selectedDateRange.value
        if (current == null) {
            // First tap: start single day range [timestamp, timestamp]
            _selectedDateRange.value = Pair(timestampMillis, timestampMillis)
        } else {
            val (start, end) = current
            if (start == end && isSameDay(start, timestampMillis)) {
                // Tapping the same single day toggles selection off
                _selectedDateRange.value = null
            } else if (start == end) {
                // Second tap: expand single day into range
                if (timestampMillis >= start) {
                    _selectedDateRange.value = Pair(start, timestampMillis)
                } else {
                    _selectedDateRange.value = Pair(timestampMillis, start)
                }
            } else {
                // Already a range: tapping starts a fresh single-day selection
                _selectedDateRange.value = Pair(timestampMillis, timestampMillis)
            }
        }
    }

    fun selectDate(timestampMillis: Long) {
        onDayTapped(timestampMillis)
    }

    fun selectDateRange(startMillis: Long, endMillis: Long) {
        val sortedStart = minOf(startMillis, endMillis)
        val sortedEnd = maxOf(startMillis, endMillis)
        _selectedDateRange.value = Pair(sortedStart, sortedEnd)
    }

    fun clearDateFilter() {
        _selectedDateRange.value = null
    }

    fun selectPreset(preset: DateRangePreset) {
        val now = System.currentTimeMillis()
        when (preset) {
            DateRangePreset.ALL -> {
                _selectedDateRange.value = null
            }
            DateRangePreset.TODAY -> {
                _selectedDateRange.value = Pair(now, now)
            }
            DateRangePreset.LAST_7_DAYS -> {
                val cal = java.util.Calendar.getInstance().apply {
                    timeInMillis = now
                    add(java.util.Calendar.DAY_OF_YEAR, -6)
                }
                _selectedDateRange.value = Pair(cal.timeInMillis, now)
            }
            DateRangePreset.THIS_MONTH -> {
                val calStart = java.util.Calendar.getInstance().apply {
                    timeInMillis = now
                    set(java.util.Calendar.DAY_OF_MONTH, 1)
                }
                _selectedDateRange.value = Pair(calStart.timeInMillis, now)
            }
            DateRangePreset.LAST_30_DAYS -> {
                val cal = java.util.Calendar.getInstance().apply {
                    timeInMillis = now
                    add(java.util.Calendar.DAY_OF_YEAR, -29)
                }
                _selectedDateRange.value = Pair(cal.timeInMillis, now)
            }
        }
    }

    private fun isSameDay(timeA: Long, timeB: Long): Boolean {
        val calA = java.util.Calendar.getInstance().apply { timeInMillis = timeA }
        val calB = java.util.Calendar.getInstance().apply { timeInMillis = timeB }
        return calA.get(java.util.Calendar.YEAR) == calB.get(java.util.Calendar.YEAR) &&
            calA.get(java.util.Calendar.DAY_OF_YEAR) == calB.get(java.util.Calendar.DAY_OF_YEAR)
    }

    fun selectMonth(monthKey: String) {
        _selectedMonth.value = monthKey
        _selectedDateMillis.value = null
    }

    fun nextCalendarMonth() {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = _calendarMonthMillis.value
            add(java.util.Calendar.MONTH, 1)
        }
        _calendarMonthMillis.value = cal.timeInMillis
    }

    fun previousCalendarMonth() {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = _calendarMonthMillis.value
            add(java.util.Calendar.MONTH, -1)
        }
        _calendarMonthMillis.value = cal.timeInMillis
    }

    private fun getStartOfCurrentMonth(): Long {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.DAY_OF_MONTH, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
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
                    if (updateInfo != null) {
                        if (updateInfo.isUpdateAvailable) {
                            _updateState.value = UpdateCheckState.Available(updateInfo)
                            _showUpdateBanner.value = true
                        } else {
                            _updateState.value = UpdateCheckState.UpToDate(currentVersion)
                        }
                    } else {
                        if (manual) {
                            _updateState.value = UpdateCheckState.Error("No releases published yet on GitHub repository.")
                        } else {
                            _updateState.value = UpdateCheckState.Idle
                        }
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

    fun cleanSpamAndRecalculate() {
        viewModelScope.launch {
            val count = transactionRepository.purgeSpamTransactions()
            _cleanSpamStatus.value = if (count > 0) {
                "Cleaned $count spam items. Cards recalculated!"
            } else {
                "No spam detected. Ledger is clean."
            }
        }
    }

    fun clearCleanSpamStatus() {
        _cleanSpamStatus.value = null
    }
}

enum class DateRangePreset(val label: String) {
    ALL("All"),
    TODAY("Today"),
    LAST_7_DAYS("7D"),
    THIS_MONTH("This Month"),
    LAST_30_DAYS("30D")
}
