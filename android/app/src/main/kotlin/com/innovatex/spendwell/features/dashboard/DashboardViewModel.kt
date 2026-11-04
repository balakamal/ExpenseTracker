package com.innovatex.spendwell.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innovatex.spendwell.analytics.FinancialAnalyticsEngine
import com.innovatex.spendwell.analytics.MonthlyFinancialSummary
import com.innovatex.spendwell.common.Money
import com.innovatex.spendwell.domain.model.Account
import com.innovatex.spendwell.domain.model.Transaction
import com.innovatex.spendwell.domain.repository.AccountRepository
import com.innovatex.spendwell.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val isObfuscated = MutableStateFlow(false)

    val uiState: StateFlow<DashboardUiState> = combine(
        transactionRepository.observeAllTransactions(),
        accountRepository.observeAccounts(),
        transactionRepository.observePendingReviewCount(),
        isObfuscated
    ) { transactions, accounts, pendingCount, obfuscated ->
        var totalBalance = Money.ZERO
        accounts.forEach { totalBalance += it.currentBalance }

        val cal = Calendar.getInstance()
        val summary = FinancialAnalyticsEngine.analyze(
            transactions = transactions,
            year = cal.get(Calendar.YEAR),
            monthZeroIndexed = cal.get(Calendar.MONTH)
        )

        DashboardUiState(
            isLoading = false,
            netBalance = totalBalance,
            isBalanceObfuscated = obfuscated,
            monthlySummary = summary,
            recentTransactions = transactions.take(5),
            accounts = accounts,
            pendingReviewCount = pendingCount
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
}
