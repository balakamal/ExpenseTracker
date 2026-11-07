package com.innovatex.spendwell.features.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innovatex.spendwell.analytics.FinancialAnalyticsEngine
import com.innovatex.spendwell.analytics.MonthlyFinancialSummary
import com.innovatex.spendwell.common.Money
import com.innovatex.spendwell.data.preferences.UserPreferencesRepository
import com.innovatex.spendwell.domain.model.Account
import com.innovatex.spendwell.domain.model.Transaction
import com.innovatex.spendwell.domain.repository.AccountRepository
import com.innovatex.spendwell.domain.repository.TransactionRepository
import com.innovatex.spendwell.integrations.sms.SmsHistoryImportResult
import com.innovatex.spendwell.integrations.sms.SmsHistoryImporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val netBalance: Money = Money.ZERO,
    val isBalanceObfuscated: Boolean = false,
    val monthlySummary: MonthlyFinancialSummary? = null,
    val recentTransactions: List<Transaction> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val pendingReviewCount: Int = 0,
    val isImporting: Boolean = false,
    val importMessage: String? = null,
    val userName: String = "",
    val error: String? = null
)

private data class DashboardRepoData(
    val transactions: List<Transaction>,
    val accounts: List<Account>,
    val pendingReviewCount: Int
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val smsHistoryImporter: SmsHistoryImporter,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val isObfuscated = MutableStateFlow(false)
    private val isImportingFlow = MutableStateFlow(false)
    private val importMessageFlow = MutableStateFlow<String?>(null)

    private val repositoryDataFlow = combine(
        transactionRepository.observeAllTransactions().catch { e ->
            Log.e("DashboardViewModel", "Error observing all transactions", e)
            emit(emptyList())
        },
        accountRepository.observeAccounts().catch { e ->
            Log.e("DashboardViewModel", "Error observing accounts", e)
            emit(emptyList())
        },
        transactionRepository.observePendingReviewCount().catch { e ->
            Log.e("DashboardViewModel", "Error observing pending review count", e)
            emit(0)
        }
    ) { transactions, accounts, pendingCount ->
        DashboardRepoData(transactions, accounts, pendingCount)
    }

    private val userPrefsFlow = combine(
        userPreferencesRepository.cycleStartDay,
        userPreferencesRepository.userName
    ) { cycleStartDay, userName ->
        Pair(cycleStartDay, userName)
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        repositoryDataFlow,
        isObfuscated,
        isImportingFlow,
        importMessageFlow,
        userPrefsFlow
    ) { repoData, obfuscated, importing, importMsg, userPrefs ->
        val (cycleStartDay, userName) = userPrefs
        try {
            var totalBalance = Money.ZERO
            repoData.accounts.forEach { totalBalance += it.currentBalance }

            val cal = Calendar.getInstance()
            val summary = FinancialAnalyticsEngine.analyze(
                transactions = repoData.transactions,
                year = cal.get(Calendar.YEAR),
                monthZeroIndexed = cal.get(Calendar.MONTH),
                cycleStartDay = cycleStartDay
            )

            DashboardUiState(
                isLoading = false,
                netBalance = totalBalance,
                isBalanceObfuscated = obfuscated,
                monthlySummary = summary,
                recentTransactions = repoData.transactions.take(5),
                accounts = repoData.accounts,
                pendingReviewCount = repoData.pendingReviewCount,
                isImporting = importing,
                importMessage = importMsg,
                userName = userName
            )
        } catch (e: Throwable) {
            Log.e("DashboardViewModel", "Error processing dashboard calculation", e)
            DashboardUiState(
                isLoading = false,
                netBalance = Money.ZERO,
                error = e.stackTraceToString()
            )
        }
    }.catch { e ->
        Log.e("DashboardViewModel", "Fatal error in uiState flow", e)
        emit(
            DashboardUiState(
                isLoading = false,
                netBalance = Money.ZERO,
                error = e.stackTraceToString()
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(isLoading = true)
    )

    fun toggleBalanceObfuscation() {
        isObfuscated.value = !isObfuscated.value
    }

    fun saveTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.saveTransaction(transaction)
        }
    }

    fun importSmsHistory(
        onComplete: ((SmsHistoryImportResult) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        if (isImportingFlow.value) return
        viewModelScope.launch {
            isImportingFlow.value = true
            importMessageFlow.value = null
            try {
                val result = smsHistoryImporter.importRecentHistory(days = 30)
                val cal = Calendar.getInstance()
                val monthKey = "%04d-%02d".format(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
                userPreferencesRepository.addSyncedMonth(monthKey)
                importMessageFlow.value = "Imported ${result.imported} transactions from SMS"
                onComplete?.invoke(result)
            } catch (e: Exception) {
                val msg = e.message ?: "Failed to import SMS history"
                importMessageFlow.value = msg
                onError?.invoke(msg)
            } finally {
                isImportingFlow.value = false
            }
        }
    }
}
