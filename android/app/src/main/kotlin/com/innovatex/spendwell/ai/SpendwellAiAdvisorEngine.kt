package com.innovatex.spendwell.ai

import com.innovatex.spendwell.analytics.FinancialAnalyticsEngine
import com.innovatex.spendwell.common.Money
import com.innovatex.spendwell.core.DispatcherProvider
import com.innovatex.spendwell.domain.model.Transaction
import com.innovatex.spendwell.domain.repository.AccountRepository
import com.innovatex.spendwell.domain.repository.BudgetRepository
import com.innovatex.spendwell.domain.repository.RecurringPaymentRepository
import com.innovatex.spendwell.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class AiChatMessage(
    val id: String,
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageSender {
    USER,
    ADVISOR
}

@Singleton
class SpendwellAiAdvisorEngine @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val budgetRepository: BudgetRepository,
    private val recurringPaymentRepository: RecurringPaymentRepository,
    private val dispatchers: DispatcherProvider
) {

    /**
     * Executes a user query using predefined, read-only financial tools.
     * Guarantees zero ledger modification capability.
     */
    suspend fun answerQuery(query: String): String = withContext(dispatchers.io) {
        val lower = query.lowercase(Locale.ROOT)
        val allTx = transactionRepository.observeAllTransactions().first()

        val cal = Calendar.getInstance()
        val summary = FinancialAnalyticsEngine.analyze(
            transactions = allTx,
            year = cal.get(Calendar.YEAR),
            monthZeroIndexed = cal.get(Calendar.MONTH)
        )

        return@withContext when {
            lower.contains("spent") || lower.contains("expense") || lower.contains("outflow") -> {
                val totalExpenses = summary.totalExpenses.formatInr(true)
                val avgDaily = summary.averageDailySpend.formatInr(true)
                "You have spent $totalExpenses so far this month, averaging $avgDaily per day. Your projected month-end spend is ${summary.projectedMonthEndSpend.formatInr(true)}."
            }

            lower.contains("income") || lower.contains("earned") || lower.contains("salary") -> {
                val totalIncome = summary.totalIncome.formatInr(true)
                val savings = summary.netSavings.formatInr(true)
                "Your total income recorded this month is $totalIncome. Your current net savings is $savings (Savings rate: ${String.format(Locale.ENGLISH, "%.1f", summary.savingsRatePercentage)}%)."
            }

            lower.contains("category") || lower.contains("top category") || lower.contains("where") -> {
                if (summary.categoryBreakdown.isEmpty()) {
                    "You don't have any categorised consumption expenses recorded yet."
                } else {
                    val top3 = summary.categoryBreakdown.take(3).joinToString("\n") {
                        "• ${it.categoryName}: ${it.amount.formatInr(true)} (${String.format(Locale.ENGLISH, "%.1f", it.percentageOfTotal)}%)"
                    }
                    "Your top spending categories this month:\n$top3"
                }
            }

            lower.contains("subscription") || lower.contains("recurring") || lower.contains("bills") -> {
                val recurring = recurringPaymentRepository.observeActiveRecurring().first()
                if (recurring.isEmpty()) {
                    "No active recurring subscriptions or bills were found in your ledger."
                } else {
                    var total = Money.ZERO
                    val items = recurring.joinToString("\n") {
                        total += it.amount
                        "• ${it.title}: ${it.amount.formatInr(true)} (${it.cadence.lowercase()})"
                    }
                    "Active recurring commitments:\n$items\n\nTotal commitment: ${total.formatInr(true)}"
                }
            }

            lower.contains("cut") || lower.contains("save") || lower.contains("recommend") || lower.contains("advice") -> {
                val topCat = summary.categoryBreakdown.firstOrNull()
                if (topCat != null) {
                    "Based on your ledger, '${topCat.categoryName}' accounts for ${String.format(Locale.ENGLISH, "%.1f", topCat.percentageOfTotal)}% of your expenses (${topCat.amount.formatInr(true)}). Setting a monthly budget on this category could save you up to 15% next month."
                } else {
                    "Maintain your spending discipline! Track all incoming alerts and verify any flagged duplicate items in the Review Center."
                }
            }

            else -> {
                "I am your Spendwell On-Device AI Advisor running locally. You can ask me:\n• 'How much did I spend this month?'\n• 'What are my top spending categories?'\n• 'List my recurring subscriptions'\n• 'How can I save more money?'"
            }
        }
    }
}
