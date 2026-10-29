package com.innovatex.expensetracker.domain.usecase

import com.innovatex.expensetracker.domain.model.Category
import com.innovatex.expensetracker.domain.model.MonthlySummary
import com.innovatex.expensetracker.domain.model.SourceChannel
import com.innovatex.expensetracker.domain.model.SuspectedSpamItem
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
    val monthKey: String = "ALL",
    val displayMonth: String = "Overall Months",
    val totalDebit: Double = 0.0,
    val totalCredit: Double = 0.0,
    val netBalance: Double = 0.0,
    val budgetLimit: Double = 50000.0,
    val budgetProgressPercent: Int = 0,
    val remainingBudget: Double = 50000.0,
    val creditTransactionCount: Int = 0,
    val debitTransactionCount: Int = 0,
    val categoryTotals: List<CategorySpendItem> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val allTransactions: List<Transaction> = emptyList(),
    val availableMonths: List<String> = emptyList(),
    val startDateMillis: Long? = null,
    val endDateMillis: Long? = null,
    val selectedDateMillis: Long? = startDateMillis,
    val selectedDateDisplay: String? = null,
    val isOverallView: Boolean = true,
    val isLoading: Boolean = false
)

data class CategorySpendItem(
    val category: Category,
    val totalSpent: Double,
    val percentageOfTotal: Float,
    val transactionCount: Int = 0
)

class GetMonthlyDashboardUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) {
    operator fun invoke(
        monthKey: String? = null,
        startDateMillis: Long? = null,
        endDateMillis: Long? = null
    ): Flow<MonthlyDashboardUiState> {
        val allTransactionsFlow = transactionRepository.getAllTransactions()
        val categoriesFlow = categoryRepository.getAllCategories()
        val monthsFlow = transactionRepository.getAvailableMonths()

        return combine(
            allTransactionsFlow,
            categoriesFlow,
            monthsFlow
        ) { allTransactions, categories, availableMonths ->
            val categoryMap = categories.associateBy { it.id }

            // Determine filtered list based on date range or month selection
            val (filteredTransactions, displayTitle, isOverall) = when {
                startDateMillis != null -> {
                    val endMillis = endDateMillis ?: startDateMillis
                    val (startOfRange, _) = getDayRange(startDateMillis)
                    val (_, endOfRange) = getDayRange(endMillis)
                    val list = allTransactions.filter { it.timestamp in startOfRange..endOfRange }

                    val title = if (isSameDay(startDateMillis, endMillis)) {
                        SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(startDateMillis))
                    } else {
                        val startFmt = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(startDateMillis))
                        val endFmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(endMillis))
                        "$startFmt – $endFmt"
                    }
                    Triple(list, title, false)
                }
                monthKey != null && monthKey != "ALL" -> {
                    val list = allTransactions.filter { it.monthKey == monthKey }
                    Triple(list, formatMonthDisplay(monthKey), false)
                }
                else -> {
                    Triple(allTransactions, "Overall Ledger", true)
                }
            }

            var totalDebit = 0.0
            var totalCredit = 0.0
            var creditCount = 0
            var debitCount = 0
            val categorySpendMap = mutableMapOf<String, Double>()
            val categoryCountMap = mutableMapOf<String, Int>()

            for (txn in filteredTransactions) {
                // Only include genuine transactions in the financial totals -
                // promotional/spam alerts (isValidTransaction == false) are shown in
                // the ledger for transparency but excluded from all summary calculations.
                if (!txn.isValidTransaction) continue

                when (txn.type) {
                    TransactionType.DEBIT -> {
                        totalDebit += txn.amount
                        debitCount++
                        categorySpendMap[txn.categoryId] = (categorySpendMap[txn.categoryId] ?: 0.0) + txn.amount
                        categoryCountMap[txn.categoryId] = (categoryCountMap[txn.categoryId] ?: 0) + 1
                    }
                    TransactionType.CREDIT -> {
                        totalCredit += txn.amount
                        creditCount++
                        // Also include income categories in the breakdown
                        categorySpendMap[txn.categoryId] = (categorySpendMap[txn.categoryId] ?: 0.0) + txn.amount
                        categoryCountMap[txn.categoryId] = (categoryCountMap[txn.categoryId] ?: 0) + 1
                    }
                }
            }

            val totalVolume = totalDebit + totalCredit
            val spendItems = categorySpendMap.mapNotNull { (categoryId, amount) ->
                val category = categoryMap[categoryId] ?: Category(
                    id = categoryId,
                    name = categoryId.replaceFirstChar { it.titlecase() },
                    iconName = "more_horizontal",
                    colorHex = "#79B6E3"
                )
                val percent = if (totalVolume > 0) ((amount / totalVolume) * 100).toFloat() else 0f
                CategorySpendItem(
                    category = category,
                    totalSpent = amount,
                    percentageOfTotal = percent,
                    transactionCount = categoryCountMap[categoryId] ?: 0
                )
            }.sortedByDescending { it.totalSpent }

            val budgetLimit = 50000.0
            val progressPercent = if (budgetLimit > 0) ((totalDebit / budgetLimit) * 100).toInt() else 0

            MonthlyDashboardUiState(
                monthKey = monthKey ?: "ALL",
                displayMonth = displayTitle,
                totalDebit = totalDebit,
                totalCredit = totalCredit,
                netBalance = totalCredit - totalDebit,
                budgetLimit = budgetLimit,
                budgetProgressPercent = progressPercent,
                remainingBudget = maxOf(0.0, budgetLimit - totalDebit),
                creditTransactionCount = creditCount,
                debitTransactionCount = debitCount,
                categoryTotals = spendItems,
                transactions = filteredTransactions,
                allTransactions = allTransactions,
                availableMonths = availableMonths,
                startDateMillis = startDateMillis,
                endDateMillis = endDateMillis ?: startDateMillis,
                selectedDateMillis = startDateMillis,
                selectedDateDisplay = if (startDateMillis != null) displayTitle else null,
                isOverallView = isOverall,
                isLoading = false
            )
        }
    }

    private fun isSameDay(timeA: Long, timeB: Long): Boolean {
        val calA = java.util.Calendar.getInstance().apply { timeInMillis = timeA }
        val calB = java.util.Calendar.getInstance().apply { timeInMillis = timeB }
        return calA.get(java.util.Calendar.YEAR) == calB.get(java.util.Calendar.YEAR) &&
            calA.get(java.util.Calendar.DAY_OF_YEAR) == calB.get(java.util.Calendar.DAY_OF_YEAR)
    }

    private fun getDayRange(timestampMillis: Long): Pair<Long, Long> {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = timestampMillis
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis

        cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
        cal.set(java.util.Calendar.MINUTE, 59)
        cal.set(java.util.Calendar.SECOND, 59)
        cal.set(java.util.Calendar.MILLISECOND, 999)
        val endOfDay = cal.timeInMillis

        return Pair(startOfDay, endOfDay)
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
        source: SourceChannel = SourceChannel.SMS,
        sender: String? = null
    ): Transaction? {
        val candidate = parserEngine.parse(body, timestamp, source, sender) ?: return null
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
            rawSnippet = candidate.rawSnippet,
            notes = sender
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

class GetSuspectedSpamUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(): List<SuspectedSpamItem> =
        transactionRepository.getSuspectedSpamItems()
}

class ConfirmSpamSenderUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(sender: String, transactionIds: List<String>) =
        transactionRepository.confirmSpam(sender, transactionIds)
}

class TrustSenderUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(sender: String) =
        transactionRepository.trustSender(sender)
}

class DeleteTransactionsBySenderUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(sender: String): Int =
        transactionRepository.deleteTransactionsBySender(sender)
}
