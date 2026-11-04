package com.innovatex.spendwell.analytics

import com.innovatex.spendwell.common.Money
import com.innovatex.spendwell.domain.model.FinancialNature
import com.innovatex.spendwell.domain.model.Transaction
import com.innovatex.spendwell.domain.model.TransactionType
import java.util.Calendar

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

data class MonthlyFinancialSummary(
    val totalIncome: Money,
    val totalExpenses: Money,
    val netSavings: Money,
    val savingsRatePercentage: Float,
    val averageDailySpend: Money,
    val projectedMonthEndSpend: Money,
    val categoryBreakdown: List<CategorySpending>,
    val topMerchants: List<MerchantSpending>,
    val anomalies: List<SpendingAnomaly>
)

object FinancialAnalyticsEngine {

    fun analyze(transactions: List<Transaction>, year: Int, monthZeroIndexed: Int): MonthlyFinancialSummary {
        val cal = Calendar.getInstance()
        cal.set(year, monthZeroIndexed, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val monthStart = cal.timeInMillis

        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        cal.set(year, monthZeroIndexed, daysInMonth, 23, 59, 59)
        val monthEnd = cal.timeInMillis

        val monthTx = transactions.filter { it.timestamp in monthStart..monthEnd }

        // Filter income vs consumption expenses
        var totalIncome = Money.ZERO
        var totalExpenses = Money.ZERO

        for (tx in monthTx) {
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

        // Velocity
        val currentDay = Calendar.getInstance().let {
            if (it.get(Calendar.YEAR) == year && it.get(Calendar.MONTH) == monthZeroIndexed) {
                it.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
            } else {
                daysInMonth
            }
        }

        val avgDaily = Money.fromDouble(totalExpenses.toDisplayDouble() / currentDay)
        val projected = Money.fromDouble(avgDaily.toDisplayDouble() * daysInMonth)

        // Category breakdown
        val expenseTx = monthTx.filter { it.financialNature == FinancialNature.CONSUMPTION_EXPENSE }
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
            anomalies = anomalies
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
