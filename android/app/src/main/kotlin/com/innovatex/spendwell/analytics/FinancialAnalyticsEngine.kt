package com.innovatex.spendwell.analytics

import com.innovatex.spendwell.common.Money
import com.innovatex.spendwell.domain.model.FinancialNature
import com.innovatex.spendwell.domain.model.Transaction
import com.innovatex.spendwell.domain.model.TransactionStatus
import com.innovatex.spendwell.domain.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CategorySpending(
    val categoryId: String,
    val categoryName: String,
    val amount: Money,
    val percentageOfTotal: Float,
    val colorHex: String,
    val transactionCount: Int
)

data class MerchantSpending(
    val merchantName: String,
    val amount: Money,
    val transactionCount: Int
)

data class SpendingAnomaly(
    val transaction: Transaction,
    val reason: String,
    val deviationMultiple: Float
)

data class FinancialCycleWindow(
    val label: String,            // e.g. "10 Oct – 09 Nov 2026" or "November 2026"
    val monthLabel: String,       // e.g. "November 2026"
    val startTime: Long,
    val endTime: Long,
    val daysRemaining: Int,
    val currentDayIndex: Int,
    val totalDays: Int
)

data class MonthlyFinancialSummary(
    val totalIncome: Money,
    val totalExpenses: Money,
    val netSavings: Money,
    val savingsRatePercentage: Float,
    val averageDailySpend: Money,
    val projectedMonthEndSpend: Money,
    val categoryBreakdown: List<CategorySpending>,
    val topMerchants: List<MerchantSpending>,
    val anomalies: List<SpendingAnomaly>,
    val cycleWindow: FinancialCycleWindow? = null
)

object FinancialAnalyticsEngine {

    fun calculateCycle(year: Int, monthZeroIndexed: Int, cycleStartDay: Int = 1): FinancialCycleWindow {
        val calStart = Calendar.getInstance()
        val calEnd = Calendar.getInstance()
        val now = System.currentTimeMillis()

        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH)
        val shortDateFormat = SimpleDateFormat("dd MMM", Locale.ENGLISH)

        when {
            cycleStartDay <= 1 -> {
                calStart.set(year, monthZeroIndexed, 1, 0, 0, 0)
                calStart.set(Calendar.MILLISECOND, 0)
                val daysInMonth = calStart.getActualMaximum(Calendar.DAY_OF_MONTH)
                calEnd.set(year, monthZeroIndexed, daysInMonth, 23, 59, 59)
                calEnd.set(Calendar.MILLISECOND, 999)

                val mLabel = monthFormat.format(calStart.time)
                val currentDay = if (now in calStart.timeInMillis..calEnd.timeInMillis) {
                    val c = Calendar.getInstance().apply { timeInMillis = now }
                    c.get(Calendar.DAY_OF_MONTH)
                } else daysInMonth

                val remaining = ((calEnd.timeInMillis - now) / (24 * 60 * 60 * 1000L)).toInt().coerceAtLeast(0)

                return FinancialCycleWindow(
                    label = mLabel,
                    monthLabel = mLabel,
                    startTime = calStart.timeInMillis,
                    endTime = calEnd.timeInMillis,
                    daysRemaining = remaining,
                    currentDayIndex = currentDay.coerceAtLeast(1),
                    totalDays = daysInMonth
                )
            }

            cycleStartDay == -1 -> {
                // Last day of previous month to day before last day of current month
                calStart.set(year, monthZeroIndexed - 1, 1, 0, 0, 0)
                calStart.set(Calendar.MILLISECOND, 0)
                val lastDayPrev = calStart.getActualMaximum(Calendar.DAY_OF_MONTH)
                calStart.set(Calendar.DAY_OF_MONTH, lastDayPrev)

                calEnd.set(year, monthZeroIndexed, 1, 23, 59, 59)
                val lastDayCurr = calEnd.getActualMaximum(Calendar.DAY_OF_MONTH)
                calEnd.set(Calendar.DAY_OF_MONTH, (lastDayCurr - 1).coerceAtLeast(1))
                calEnd.set(Calendar.MILLISECOND, 999)

                val totalDays = (((calEnd.timeInMillis - calStart.timeInMillis) / (24 * 60 * 60 * 1000L)) + 1).toInt()
                val remaining = ((calEnd.timeInMillis - now) / (24 * 60 * 60 * 1000L)).toInt().coerceAtLeast(0)
                val currentDayIndex = (totalDays - remaining).coerceIn(1, totalDays)

                val mLabel = monthFormat.format(calEnd.time)
                val label = "${shortDateFormat.format(calStart.time)} – ${shortDateFormat.format(calEnd.time)}"

                return FinancialCycleWindow(
                    label = label,
                    monthLabel = mLabel,
                    startTime = calStart.timeInMillis,
                    endTime = calEnd.timeInMillis,
                    daysRemaining = remaining,
                    currentDayIndex = currentDayIndex,
                    totalDays = totalDays
                )
            }

            else -> {
                // cycleStartDay > 1 (e.g. 10th)
                // Cycle for month M starts on day `cycleStartDay` of M-1 and ends on day `cycleStartDay - 1` of M
                calStart.set(year, monthZeroIndexed - 1, 1, 0, 0, 0)
                calStart.set(Calendar.MILLISECOND, 0)
                val maxPrev = calStart.getActualMaximum(Calendar.DAY_OF_MONTH)
                calStart.set(Calendar.DAY_OF_MONTH, cycleStartDay.coerceAtMost(maxPrev))

                calEnd.set(year, monthZeroIndexed, 1, 23, 59, 59)
                calEnd.set(Calendar.MILLISECOND, 999)
                val maxCurr = calEnd.getActualMaximum(Calendar.DAY_OF_MONTH)
                calEnd.set(Calendar.DAY_OF_MONTH, (cycleStartDay - 1).coerceIn(1, maxCurr))

                val totalDays = (((calEnd.timeInMillis - calStart.timeInMillis) / (24 * 60 * 60 * 1000L)) + 1).toInt()
                val remaining = ((calEnd.timeInMillis - now) / (24 * 60 * 60 * 1000L)).toInt().coerceAtLeast(0)
                val currentDayIndex = (totalDays - remaining).coerceIn(1, totalDays)

                val mLabel = monthFormat.format(calEnd.time)
                val label = "${shortDateFormat.format(calStart.time)} – ${shortDateFormat.format(calEnd.time)}"

                return FinancialCycleWindow(
                    label = label,
                    monthLabel = mLabel,
                    startTime = calStart.timeInMillis,
                    endTime = calEnd.timeInMillis,
                    daysRemaining = remaining,
                    currentDayIndex = currentDayIndex,
                    totalDays = totalDays
                )
            }
        }
    }

    fun analyze(
        transactions: List<Transaction>,
        year: Int,
        monthZeroIndexed: Int,
        cycleStartDay: Int = 1
    ): MonthlyFinancialSummary {
        val cycle = calculateCycle(year, monthZeroIndexed, cycleStartDay)

        // Strictly exclude transactions pending review, failed, or reversed to prevent inaccurate totals
        val validTransactions = transactions.filter {
            it.status != TransactionStatus.NEEDS_REVIEW &&
                    it.status != TransactionStatus.FAILED &&
                    it.status != TransactionStatus.REVERSED
        }

        val cycleTx = validTransactions.filter { it.timestamp in cycle.startTime..cycle.endTime }

        // Filter income vs consumption expenses
        var totalIncome = Money.ZERO
        var totalExpenses = Money.ZERO

        for (tx in cycleTx) {
            when (tx.financialNature) {
                FinancialNature.INCOME -> {
                    totalIncome += tx.amount
                }
                FinancialNature.CONSUMPTION_EXPENSE -> {
                    totalExpenses += tx.amount
                }
                FinancialNature.REFUND -> {
                    // Reduces expense
                    totalExpenses = (totalExpenses - tx.amount).let { if (it.isNegative) Money.ZERO else it }
                }
                // INTERNAL_TRANSFER, CREDIT_CARD_BILL_PAYMENT, INVESTMENT_TRANSFER, CASH_WITHDRAWAL, LOAN_DISBURSEMENT
                // are strictly excluded from consumption expenses to prevent double counting
                else -> {}
            }
        }

        val netSavings = totalIncome - totalExpenses
        val savingsRate = if (totalIncome.isPositive) {
            ((netSavings.toDisplayDouble() / totalIncome.toDisplayDouble()) * 100).toFloat().coerceIn(-100f, 100f)
        } else 0f

        val avgDaily = Money.fromDouble(totalExpenses.toDisplayDouble() / cycle.currentDayIndex)
        val projected = Money.fromDouble(avgDaily.toDisplayDouble() * cycle.totalDays)

        // Category breakdown
        val expenseTx = cycleTx.filter { it.financialNature == FinancialNature.CONSUMPTION_EXPENSE }
        val byCategory = expenseTx.groupBy { it.categoryId ?: "cat_misc" }

        val categoryBreakdown = byCategory.map { (catId, txs) ->
            var sum = Money.ZERO
            txs.forEach { sum += it.amount }
            val catName = txs.firstOrNull()?.categoryName ?: "Miscellaneous"
            val pct = if (totalExpenses.isPositive) {
                ((sum.toDisplayDouble() / totalExpenses.toDisplayDouble()) * 100).toFloat()
            } else 0f

            CategorySpending(
                categoryId = catId,
                categoryName = catName,
                amount = sum,
                percentageOfTotal = pct,
                colorHex = getCategoryColor(catId),
                transactionCount = txs.size
            )
        }.sortedByDescending { it.amount }

        // Top merchants
        val byMerchant = expenseTx.groupBy { it.merchantName.ifBlank { "Other" } }
        val topMerchants = byMerchant.map { (merch, txs) ->
            var sum = Money.ZERO
            txs.forEach { sum += it.amount }
            MerchantSpending(merch, sum, txs.size)
        }.sortedByDescending { it.amount }.take(5)

        // Anomalies (transactions > 3x mean daily spend)
        val anomalies = mutableListOf<SpendingAnomaly>()
        if (avgDaily.isPositive) {
            val threshold = avgDaily.toDisplayDouble() * 3.0
            for (tx in expenseTx) {
                val amt = tx.amount.toDisplayDouble()
                if (amt > threshold && amt > 1500.0) {
                    val multiple = (amt / avgDaily.toDisplayDouble()).toFloat()
                    anomalies.add(
                        SpendingAnomaly(
                            transaction = tx,
                            reason = "Single purchase exceeds 3x your average daily spend",
                            deviationMultiple = multiple
                        )
                    )
                }
            }
        }

        return MonthlyFinancialSummary(
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            netSavings = netSavings,
            savingsRatePercentage = savingsRate,
            averageDailySpend = avgDaily,
            projectedMonthEndSpend = projected,
            categoryBreakdown = categoryBreakdown,
            topMerchants = topMerchants,
            anomalies = anomalies,
            cycleWindow = cycle
        )
    }

    private fun getCategoryColor(categoryId: String): String {
        return when (categoryId) {
            "cat_food" -> "#FF7A30"
            "cat_groceries" -> "#159B70"
            "cat_shopping" -> "#8B5CF6"
            "cat_transport" -> "#3B82F6"
            "cat_utilities" -> "#EAB308"
            "cat_housing" -> "#6366F1"
            "cat_entertainment" -> "#EC4899"
            "cat_health" -> "#EF4444"
            "cat_salary" -> "#10B981"
            "cat_investment" -> "#06B6D4"
            else -> "#94A3B8"
        }
    }
}
