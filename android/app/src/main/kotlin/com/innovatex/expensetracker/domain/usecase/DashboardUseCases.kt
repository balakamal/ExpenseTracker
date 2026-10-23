package com.innovatex.expensetracker.domain.usecase

import com.innovatex.expensetracker.domain.model.Category
import com.innovatex.expensetracker.domain.model.MonthlySummary
import com.innovatex.expensetracker.domain.model.SourceChannel
import com.innovatex.expensetracker.domain.model.Transaction
import com.innovatex.expensetracker.domain.model.TransactionType
import com.innovatex.expensetracker.domain.repository.CategoryRepository
import com.innovatex.expensetracker.domain.repository.TransactionRepository
import com.innovatex.expensetracker.ingestion.parser.ParsedTransactionCandidate
import com.innovatex.expensetracker.ingestion.parser.SmsParserEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

data class MonthlyDashboardUiState(
    val monthKey: String,
    val displayMonth: String,
    val totalDebit: Double = 0.0,
    val totalCredit: Double = 0.0,
    val netBalance: Double = 0.0,
    val budgetLimit: Double = 2500.0,
    val budgetProgressPercent: Int = 0,
    val remainingBudget: Double = 2500.0,
    val categoryTotals: List<CategorySpendItem> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val availableMonths: List<String> = emptyList(),
    val isLoading: Boolean = false
)

data class CategorySpendItem(
    val category: Category,
    val totalSpent: Double,
    val percentageOfTotal: Float
)

class GetMonthlyDashboardUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) {
    operator fun invoke(monthKey: String): Flow<MonthlyDashboardUiState> {
        val summaryFlow = transactionRepository.getMonthlySummary(monthKey)
        val transactionsFlow = transactionRepository.getTransactionsForMonth(monthKey)
        val categoriesFlow = categoryRepository.getAllCategories()
        val monthsFlow = transactionRepository.getAvailableMonths()

        return combine(
            summaryFlow,
            transactionsFlow,
            categoriesFlow,
            monthsFlow
        ) { summary, transactions, categories, availableMonths ->
            val categoryMap = categories.associateBy { it.id }
            val totalDebit = summary.totalDebit

            val spendItems = summary.categoryTotals.mapNotNull { (categoryId, amount) ->
                val category = categoryMap[categoryId] ?: Category(
                    id = categoryId,
                    name = categoryId.replaceFirstChar { it.titlecase() },
                    iconName = "more_horizontal",
                    colorHex = "#64748B"
                )
                val percent = if (totalDebit > 0) ((amount / totalDebit) * 100).toFloat() else 0f
                CategorySpendItem(
                    category = category,
                    totalSpent = amount,
                    percentageOfTotal = percent
                )
            }.sortedByDescending { it.totalSpent }

            MonthlyDashboardUiState(
                monthKey = monthKey,
                displayMonth = formatMonthDisplay(monthKey),
                totalDebit = summary.totalDebit,
                totalCredit = summary.totalCredit,
                netBalance = summary.netBalance,
                budgetLimit = summary.budgetLimit,
                budgetProgressPercent = summary.budgetProgressPercent,
                remainingBudget = summary.remainingBudget,
                categoryTotals = spendItems,
                transactions = transactions,
                availableMonths = if (availableMonths.isEmpty()) listOf(monthKey) else availableMonths,
                isLoading = false
            )
        }
    }

    private fun formatMonthDisplay(monthKey: String): String {
        return runCatching {
            val parser = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val formatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            val date = parser.parse(monthKey)
            if (date != null) formatter.format(date) else monthKey
        }.getOrDefault(monthKey)
    }
}

class ParseTransactionUseCase @Inject constructor(
    private val parserEngine: SmsParserEngine,
    private val categoryRepository: CategoryRepository
) {
    suspend fun parse(
        body: String,
        timestamp: Long = System.currentTimeMillis(),
        source: SourceChannel = SourceChannel.SMS
    ): Transaction? {
        val candidate = parserEngine.parse(body, timestamp, source) ?: return null
        val categoryId = resolveCategory(candidate)
        val monthKey = formatMonthKey(candidate.transactionTimestamp)

        val stableId = if (!candidate.referenceId.isNullOrBlank()) {
            "ref_${candidate.referenceId.trim().lowercase()}"
        } else {
            val amountCents = (candidate.amount * 100).toLong()
            val timeBucket = candidate.transactionTimestamp / (60 * 1000L) // 1-minute bucket
            val merchantSlug = candidate.cleanMerchant.lowercase().replace(Regex("[^a-z0-9]"), "_").take(20)
            "sms_${timeBucket}_${amountCents}_${candidate.type.name}_$merchantSlug"
        }

        return Transaction(
            id = stableId,
            amount = candidate.amount,
            currency = candidate.currency,
            type = candidate.type,
            timestamp = candidate.transactionTimestamp,
            monthKey = monthKey,
            merchantRaw = candidate.rawMerchant,
            merchantClean = candidate.cleanMerchant,
            categoryId = categoryId,
            sources = listOf(candidate.sourceChannel),
            referenceId = candidate.referenceId,
            accountLast4 = candidate.accountLast4,
            rawSnippet = candidate.rawSnippet
        )
    }

    private suspend fun resolveCategory(candidate: ParsedTransactionCandidate): String {
        if (candidate.type == TransactionType.CREDIT) {
            return "income"
        }

        val rules = runCatching { categoryRepository.getAllRules().first() }.getOrDefault(emptyList())
        for (rule in rules) {
            if (rule.isRegex) {
                if (Regex(rule.pattern, RegexOption.IGNORE_CASE).containsMatchIn(candidate.cleanMerchant)) {
                    return rule.categoryId
                }
            } else {
                if (candidate.cleanMerchant.contains(rule.pattern, ignoreCase = true)) {
                    return rule.categoryId
                }
            }
        }

        // Standard heuristics including Indian merchants
        val merchantLower = candidate.cleanMerchant.lowercase()
        return when {
            merchantLower.containsAny("swiggy", "zomato", "mcdonald", "kfc", "domino", "starbucks", "cafe", "coffee", "restaurant", "burger", "pizza", "diner", "bakery", "chai", "tea") -> "food_dining"
            merchantLower.containsAny("blinkit", "zepto", "instamart", "bigbasket", "bbnow", "dmart", "spencer", "walmart", "whole foods", "supermarket", "grocery", "trader joe", "kroger") -> "groceries"
            merchantLower.containsAny("ola", "uber", "rapido", "irctc", "metro", "subway", "transit", "gas", "shell", "chevron", "petrol", "fuel", "hpcl", "bpcl", "iocl") -> "transport"
            merchantLower.containsAny("flipkart", "amazon", "myntra", "meesho", "ajio", "nykaa", "tata cliq", "apple", "target", "nike", "zara", "ebay", "store") -> "shopping"
            merchantLower.containsAny("electricity", "bescom", "tneb", "mseb", "water", "utility", "wifi", "broadband", "jio", "airtel", "vi", "bsnl", "verizon", "at&t") -> "utilities"
            merchantLower.containsAny("bookmyshow", "pvr", "inox", "netflix", "hotstar", "prime video", "spotify", "cinema", "hulu", "disney", "theater") -> "entertainment"
            merchantLower.containsAny("apollo", "pharmeasy", "1mg", "netmeds", "medplus", "pharmacy", "cvs", "walgreens", "hospital", "clinic", "dental") -> "health"
            else -> "general"
        }
    }

    private fun String.containsAny(vararg keywords: String): Boolean =
        keywords.any { this.contains(it, ignoreCase = true) }

    private fun formatMonthKey(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        return sdf.format(Date(timestamp))
    }
}
