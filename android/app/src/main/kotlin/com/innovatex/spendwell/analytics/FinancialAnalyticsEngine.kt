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

enum class TimeGranularity(val title: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    CUSTOM("Custom")
}

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

data class TimeBlockSpending(
    val label: String,            // "Morning (6AM - 12PM)", "Afternoon (12PM - 5PM)", "Evening (5PM - 9PM)", "Night (9PM - 6AM)"
    val amount: Money,
    val transactionCount: Int,
    val percentageOfTotal: Float
)

data class DailyFinancialSummary(
    val dateLabel: String,                 // "Today, 09 Nov 2026", "Yesterday", "05 Nov 2026"
    val dayTimestamp: Long,
    val totalIncome: Money,
    val totalExpenses: Money,
    val netCashFlow: Money,
    val transactionCount: Int,
    val timeBlocks: List<TimeBlockSpending>,
    val microTxnCount: Int,                // Transactions <= ₹100
    val microTxnTotal: Money,
    val averageTicketSize: Money,
    val comparisonTo30DayAvgPct: Float,    // +/- % vs 30d average
    val categoryBreakdown: List<CategorySpending>,
    val topMerchants: List<MerchantSpending>
)

data class DaySpendBar(
    val dayLabel: String,                  // "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"
    val fullDateLabel: String,             // "03 Nov"
    val amount: Money,
    val isWeekend: Boolean,
    val isToday: Boolean,
    val percentageOfMax: Float
)

data class WeeklyFinancialSummary(
    val weekLabel: String,                 // "03 Nov – 09 Nov 2026"
    val startTime: Long,
    val endTime: Long,
    val totalIncome: Money,
    val totalExpenses: Money,
    val netSavings: Money,
    val averageDailySpend: Money,
    val dailyBars: List<DaySpendBar>,
    val weekdayTotal: Money,
    val weekendTotal: Money,
    val weekendVariancePercentage: Float,  // e.g. +28.5% on weekends
    val weekOverWeekPercentage: Float,     // +/- % vs previous week
    val peakDayLabel: String,              // "Saturday (₹4,250)"
    val categoryBreakdown: List<CategorySpending>,
    val topMerchants: List<MerchantSpending>
)

data class CumulativeSpendPoint(
    val dayIndex: Int,                  // 1, 2, ... totalDays
    val timestamp: Long,                // Start of that day
    val dateLabel: String,              // "09 Nov"
    val daySpend: Money,                // Spent on this specific day
    val cumulativeSpend: Money,         // Cumulative sum from day 1 up to this day
    val isPastOrToday: Boolean          // true if day <= today
)

data class MonthlyFinancialSummary(
    val totalIncome: Money,
    val totalExpenses: Money,
    val netSavings: Money,
    val savingsRatePercentage: Float,
    val averageDailySpend: Money,
    val projectedMonthEndSpend: Money,
    val monthOverMonthPercentage: Float,   // +/- % vs previous month
    val categoryBreakdown: List<CategorySpending>,
    val topMerchants: List<MerchantSpending>,
    val anomalies: List<SpendingAnomaly>,
    val cycleWindow: FinancialCycleWindow? = null,
    val cumulativeSpendPoints: List<CumulativeSpendPoint> = emptyList()
)

data class CustomRangeFinancialSummary(
    val rangeLabel: String,                // "15 Oct 2026 – 05 Nov 2026 (22 Days)"
    val startTime: Long,
    val endTime: Long,
    val totalDays: Int,
    val totalIncome: Money,
    val totalExpenses: Money,
    val netSavings: Money,
    val averageDailySpend: Money,
    val periodOverPeriodPercentage: Float, // Compared to previous equivalent duration
    val categoryBreakdown: List<CategorySpending>,
    val topMerchants: List<MerchantSpending>,
    val anomalies: List<SpendingAnomaly>,
    val cumulativeSpendPoints: List<CumulativeSpendPoint> = emptyList()
)

object FinancialAnalyticsEngine {

    fun filterValidTransactions(transactions: List<Transaction>): List<Transaction> {
        return transactions.filter {
            it.status != TransactionStatus.NEEDS_REVIEW &&
                    it.status != TransactionStatus.FAILED &&
                    it.status != TransactionStatus.REVERSED
        }
    }

    private fun computeIncomeAndExpenses(txList: List<Transaction>): Pair<Money, Money> {
        var totalIncome = Money.ZERO
        var totalExpenses = Money.ZERO

        for (tx in txList) {
            when (tx.financialNature) {
                FinancialNature.INCOME -> {
                    totalIncome += tx.amount
                }
                FinancialNature.CONSUMPTION_EXPENSE -> {
                    totalExpenses += tx.amount
                }
                FinancialNature.REFUND -> {
                    totalExpenses = (totalExpenses - tx.amount).let { if (it.isNegative) Money.ZERO else it }
                }
                // INTERNAL_TRANSFER, CREDIT_CARD_BILL_PAYMENT, INVESTMENT_TRANSFER, CASH_WITHDRAWAL, LOAN_DISBURSEMENT
                // are strictly excluded from consumption expenses to prevent double counting
                else -> {}
            }
        }
        return Pair(totalIncome, totalExpenses)
    }

    private fun buildCategoryBreakdown(expenseTx: List<Transaction>, totalExpenses: Money): List<CategorySpending> {
        val byCategory = expenseTx.groupBy { it.categoryId ?: "cat_misc" }
        return byCategory.map { (catId, txs) ->
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
    }

    private fun buildTopMerchants(expenseTx: List<Transaction>): List<MerchantSpending> {
        val byMerchant = expenseTx.groupBy { it.merchantName.ifBlank { "Other" } }
        return byMerchant.map { (merch, txs) ->
            var sum = Money.ZERO
            txs.forEach { sum += it.amount }
            MerchantSpending(merch, sum, txs.size)
        }.sortedByDescending { it.amount }.take(5)
    }

    // -------------------------------------------------------------
    // DAILY STATS
    // -------------------------------------------------------------
    fun analyzeDaily(
        transactions: List<Transaction>,
        targetTimestamp: Long = System.currentTimeMillis()
    ): DailyFinancialSummary {
        val validTransactions = filterValidTransactions(transactions)

        val cal = Calendar.getInstance().apply {
            timeInMillis = targetTimestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayStart = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val dayEnd = cal.timeInMillis

        val dayTx = validTransactions.filter { it.timestamp in dayStart..dayEnd }
        val (totalIncome, totalExpenses) = computeIncomeAndExpenses(dayTx)
        val netCashFlow = totalIncome - totalExpenses

        val expenseTx = dayTx.filter { it.financialNature == FinancialNature.CONSUMPTION_EXPENSE }

        // Time of Day distribution
        var morningSum = Money.ZERO
        var morningCount = 0
        var afternoonSum = Money.ZERO
        var afternoonCount = 0
        var eveningSum = Money.ZERO
        var eveningCount = 0
        var nightSum = Money.ZERO
        var nightCount = 0

        val calItem = Calendar.getInstance()
        for (tx in expenseTx) {
            calItem.timeInMillis = tx.timestamp
            when (calItem.get(Calendar.HOUR_OF_DAY)) {
                in 6..11 -> {
                    morningSum += tx.amount
                    morningCount++
                }
                in 12..16 -> {
                    afternoonSum += tx.amount
                    afternoonCount++
                }
                in 17..20 -> {
                    eveningSum += tx.amount
                    eveningCount++
                }
                else -> {
                    nightSum += tx.amount
                    nightCount++
                }
            }
        }

        val totalExpDouble = totalExpenses.toDisplayDouble()
        val timeBlocks = listOf(
            TimeBlockSpending("Morning (6 AM - 12 PM)", morningSum, morningCount, if (totalExpDouble > 0) ((morningSum.toDisplayDouble() / totalExpDouble) * 100).toFloat() else 0f),
            TimeBlockSpending("Afternoon (12 PM - 5 PM)", afternoonSum, afternoonCount, if (totalExpDouble > 0) ((afternoonSum.toDisplayDouble() / totalExpDouble) * 100).toFloat() else 0f),
            TimeBlockSpending("Evening (5 PM - 9 PM)", eveningSum, eveningCount, if (totalExpDouble > 0) ((eveningSum.toDisplayDouble() / totalExpDouble) * 100).toFloat() else 0f),
            TimeBlockSpending("Night (9 PM - 6 AM)", nightSum, nightCount, if (totalExpDouble > 0) ((nightSum.toDisplayDouble() / totalExpDouble) * 100).toFloat() else 0f)
        )

        // Micro-transactions (<= ₹100)
        var microCount = 0
        var microTotal = Money.ZERO
        for (tx in expenseTx) {
            if (tx.amount.toDisplayDouble() <= 100.0) {
                microCount++
                microTotal += tx.amount
            }
        }

        val avgTicket = if (expenseTx.isNotEmpty()) {
            Money.fromDouble(totalExpenses.toDisplayDouble() / expenseTx.size)
        } else Money.ZERO

        // Comparison against 30-day baseline average
        val thirtyDaysAgo = dayStart - (30L * 24 * 60 * 60 * 1000L)
        val prior30DaysTx = validTransactions.filter { it.timestamp in thirtyDaysAgo until dayStart && it.financialNature == FinancialNature.CONSUMPTION_EXPENSE }
        var thirtyDaysTotal = Money.ZERO
        prior30DaysTx.forEach { thirtyDaysTotal += it.amount }
        val thirtyDayDailyAvg = thirtyDaysTotal.toDisplayDouble() / 30.0

        val comparisonPct = if (thirtyDayDailyAvg > 0) {
            (((totalExpenses.toDisplayDouble() - thirtyDayDailyAvg) / thirtyDayDailyAvg) * 100).toFloat()
        } else 0f

        // Friendly Date Label
        val now = System.currentTimeMillis()
        val isToday = android.text.format.DateUtils.isToday(dayStart)
        val isYesterday = android.text.format.DateUtils.isToday(dayStart + 86400_000L)
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
        val label = when {
            isToday -> "Today, ${SimpleDateFormat("dd MMM", Locale.ENGLISH).format(Date(dayStart))}"
            isYesterday -> "Yesterday, ${SimpleDateFormat("dd MMM", Locale.ENGLISH).format(Date(dayStart))}"
            else -> dateFormat.format(Date(dayStart))
        }

        return DailyFinancialSummary(
            dateLabel = label,
            dayTimestamp = dayStart,
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            netCashFlow = netCashFlow,
            transactionCount = dayTx.size,
            timeBlocks = timeBlocks,
            microTxnCount = microCount,
            microTxnTotal = microTotal,
            averageTicketSize = avgTicket,
            comparisonTo30DayAvgPct = comparisonPct,
            categoryBreakdown = buildCategoryBreakdown(expenseTx, totalExpenses),
            topMerchants = buildTopMerchants(expenseTx)
        )
    }

    // -------------------------------------------------------------
    // WEEKLY STATS
    // -------------------------------------------------------------
    fun analyzeWeekly(
        transactions: List<Transaction>,
        weekOffset: Int = 0 // 0 = current week, 1 = previous week, etc.
    ): WeeklyFinancialSummary {
        val validTransactions = filterValidTransactions(transactions)

        val cal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            add(Calendar.WEEK_OF_YEAR, -weekOffset)
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val weekStart = cal.timeInMillis

        val calEnd = (cal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, 6)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val weekEnd = calEnd.timeInMillis

        val weekTx = validTransactions.filter { it.timestamp in weekStart..weekEnd }
        val (totalIncome, totalExpenses) = computeIncomeAndExpenses(weekTx)
        val netSavings = totalIncome - totalExpenses

        val expenseTx = weekTx.filter { it.financialNature == FinancialNature.CONSUMPTION_EXPENSE }

        // Day by day distribution (7 days: Mon-Sun)
        val dailyBars = mutableListOf<DaySpendBar>()
        val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        var maxDaySpend = 1.0

        val daySums = DoubleArray(7)
        val dayFullLabels = mutableListOf<String>()
        val dayIsWeekend = BooleanArray(7)
        val dayIsToday = BooleanArray(7)

        val tempCal = (cal.clone() as Calendar)
        val shortDate = SimpleDateFormat("dd MMM", Locale.ENGLISH)
        for (i in 0..6) {
            val dStart = tempCal.timeInMillis
            val dEnd = (tempCal.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }.timeInMillis

            val dayExpenses = expenseTx.filter { it.timestamp in dStart..dEnd }
            var sum = 0.0
            dayExpenses.forEach { sum += it.amount.toDisplayDouble() }
            daySums[i] = sum
            if (sum > maxDaySpend) maxDaySpend = sum

            dayFullLabels.add(shortDate.format(tempCal.time))
            val dow = tempCal.get(Calendar.DAY_OF_WEEK)
            dayIsWeekend[i] = (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY)
            dayIsToday[i] = android.text.format.DateUtils.isToday(tempCal.timeInMillis)

            tempCal.add(Calendar.DAY_OF_MONTH, 1)
        }

        var peakDayIdx = 0
        var peakDayAmount = 0.0
        var weekdayTotalDouble = 0.0
        var weekendTotalDouble = 0.0

        for (i in 0..6) {
            if (daySums[i] > peakDayAmount) {
                peakDayAmount = daySums[i]
                peakDayIdx = i
            }
            if (dayIsWeekend[i]) {
                weekendTotalDouble += daySums[i]
            } else {
                weekdayTotalDouble += daySums[i]
            }

            dailyBars.add(
                DaySpendBar(
                    dayLabel = dayLabels[i],
                    fullDateLabel = dayFullLabels[i],
                    amount = Money.fromDouble(daySums[i]),
                    isWeekend = dayIsWeekend[i],
                    isToday = dayIsToday[i],
                    percentageOfMax = (daySums[i] / maxDaySpend).toFloat().coerceIn(0.05f, 1f)
                )
            )
        }

        val weekdayTotal = Money.fromDouble(weekdayTotalDouble)
        val weekendTotal = Money.fromDouble(weekendTotalDouble)

        // Weekend vs Weekday variance
        val weekdayDailyAvg = weekdayTotalDouble / 5.0
        val weekendDailyAvg = weekendTotalDouble / 2.0
        val weekendVariance = if (weekdayDailyAvg > 0) {
            (((weekendDailyAvg - weekdayDailyAvg) / weekdayDailyAvg) * 100).toFloat()
        } else 0f

        // Previous week comparison (WoW)
        val prevWeekStart = weekStart - (7L * 24 * 60 * 60 * 1000L)
        val prevWeekEnd = weekStart - 1L
        val prevWeekTx = validTransactions.filter { it.timestamp in prevWeekStart..prevWeekEnd && it.financialNature == FinancialNature.CONSUMPTION_EXPENSE }
        var prevWeekExpenses = 0.0
        prevWeekTx.forEach { prevWeekExpenses += it.amount.toDisplayDouble() }

        val wowPercentage = if (prevWeekExpenses > 0) {
            (((totalExpenses.toDisplayDouble() - prevWeekExpenses) / prevWeekExpenses) * 100).toFloat()
        } else 0f

        // Days elapsed in current week
        val now = System.currentTimeMillis()
        val daysElapsed = if (now in weekStart..weekEnd) {
            val c = Calendar.getInstance().apply { timeInMillis = now }
            var d = c.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 1
            if (d <= 0) d += 7
            d.coerceIn(1, 7)
        } else 7

        val avgDailySpend = Money.fromDouble(totalExpenses.toDisplayDouble() / daysElapsed)
        val peakDayLabel = "${dayLabels[peakDayIdx]} (${Money.fromDouble(peakDayAmount).formatInr(true)})"

        val weekLabel = if (weekOffset == 0) {
            "This Week (${shortDate.format(Date(weekStart))} – ${shortDate.format(Date(weekEnd))})"
        } else {
            "${shortDate.format(Date(weekStart))} – ${shortDate.format(Date(weekEnd))}"
        }

        return WeeklyFinancialSummary(
            weekLabel = weekLabel,
            startTime = weekStart,
            endTime = weekEnd,
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            netSavings = netSavings,
            averageDailySpend = avgDailySpend,
            dailyBars = dailyBars,
            weekdayTotal = weekdayTotal,
            weekendTotal = weekendTotal,
            weekendVariancePercentage = weekendVariance,
            weekOverWeekPercentage = wowPercentage,
            peakDayLabel = peakDayLabel,
            categoryBreakdown = buildCategoryBreakdown(expenseTx, totalExpenses),
            topMerchants = buildTopMerchants(expenseTx)
        )
    }

    // -------------------------------------------------------------
    // MONTHLY STATS
    // -------------------------------------------------------------
    fun getLastWorkingDay(cal: Calendar): Int {
        val c = cal.clone() as Calendar
        val maxDay = c.getActualMaximum(Calendar.DAY_OF_MONTH)
        c.set(Calendar.DAY_OF_MONTH, maxDay)
        while (c.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || c.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            c.add(Calendar.DAY_OF_MONTH, -1)
        }
        return c.get(Calendar.DAY_OF_MONTH)
    }

    fun calculateCycle(year: Int, monthZeroIndexed: Int, cycleStartDay: Int = 1): FinancialCycleWindow {
        val calStart = Calendar.getInstance()
        val calEnd = Calendar.getInstance()
        val now = System.currentTimeMillis()

        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH)
        val shortDateFormat = SimpleDateFormat("dd MMM", Locale.ENGLISH)

        when {
            cycleStartDay == -1 -> {
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

            cycleStartDay == -2 -> {
                calStart.set(year, monthZeroIndexed - 1, 1, 0, 0, 0)
                calStart.set(Calendar.MILLISECOND, 0)
                val lastWorkDayPrev = getLastWorkingDay(calStart)
                calStart.set(Calendar.DAY_OF_MONTH, lastWorkDayPrev)

                calEnd.set(year, monthZeroIndexed, 1, 23, 59, 59)
                calEnd.set(Calendar.MILLISECOND, 999)
                val lastWorkDayCurr = getLastWorkingDay(calEnd)
                calEnd.set(Calendar.DAY_OF_MONTH, (lastWorkDayCurr - 1).coerceAtLeast(1))

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

            else -> {
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
        val validTransactions = filterValidTransactions(transactions)

        val cycleTx = validTransactions.filter { it.timestamp in cycle.startTime..cycle.endTime }
        val (totalIncome, totalExpenses) = computeIncomeAndExpenses(cycleTx)

        val netSavings = totalIncome - totalExpenses
        val savingsRate = if (totalIncome.isPositive) {
            ((netSavings.toDisplayDouble() / totalIncome.toDisplayDouble()) * 100).toFloat().coerceIn(-100f, 100f)
        } else 0f

        val avgDaily = Money.fromDouble(totalExpenses.toDisplayDouble() / cycle.currentDayIndex)
        val projected = Money.fromDouble(avgDaily.toDisplayDouble() * cycle.totalDays)

        val expenseTx = cycleTx.filter { it.financialNature == FinancialNature.CONSUMPTION_EXPENSE }

        // Previous month comparison (MoM)
        val prevMonthCycle = calculateCycle(if (monthZeroIndexed == 0) year - 1 else year, if (monthZeroIndexed == 0) 11 else monthZeroIndexed - 1, cycleStartDay)
        val prevMonthTx = validTransactions.filter { it.timestamp in prevMonthCycle.startTime..prevMonthCycle.endTime && it.financialNature == FinancialNature.CONSUMPTION_EXPENSE }
        var prevMonthExpenses = 0.0
        prevMonthTx.forEach { prevMonthExpenses += it.amount.toDisplayDouble() }

        val momPercentage = if (prevMonthExpenses > 0) {
            (((totalExpenses.toDisplayDouble() - prevMonthExpenses) / prevMonthExpenses) * 100).toFloat()
        } else 0f

        // Anomalies (transactions > 3x mean daily spend and > ₹1500)
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

        // Calculate cumulative spend trajectory across the cycle
        val cumulativePoints = mutableListOf<CumulativeSpendPoint>()
        var runningCumulative = 0.0
        val dayCal = Calendar.getInstance().apply { timeInMillis = cycle.startTime }
        val now = System.currentTimeMillis()
        val shortDate = SimpleDateFormat("dd MMM", Locale.ENGLISH)

        for (dayIdx in 1..cycle.totalDays) {
            val dStart = dayCal.timeInMillis
            val dEnd = (dayCal.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            val dayTxs = expenseTx.filter { it.timestamp in dStart..dEnd }
            var dayTotal = 0.0
            dayTxs.forEach { dayTotal += it.amount.toDisplayDouble() }
            runningCumulative += dayTotal

            cumulativePoints.add(
                CumulativeSpendPoint(
                    dayIndex = dayIdx,
                    timestamp = dStart,
                    dateLabel = shortDate.format(Date(dStart)),
                    daySpend = Money.fromDouble(dayTotal),
                    cumulativeSpend = Money.fromDouble(runningCumulative),
                    isPastOrToday = dStart <= now
                )
            )
            dayCal.add(Calendar.DAY_OF_MONTH, 1)
        }

        return MonthlyFinancialSummary(
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            netSavings = netSavings,
            savingsRatePercentage = savingsRate,
            averageDailySpend = avgDaily,
            projectedMonthEndSpend = projected,
            monthOverMonthPercentage = momPercentage,
            categoryBreakdown = buildCategoryBreakdown(expenseTx, totalExpenses),
            topMerchants = buildTopMerchants(expenseTx),
            anomalies = anomalies,
            cycleWindow = cycle,
            cumulativeSpendPoints = cumulativePoints
        )
    }

    // -------------------------------------------------------------
    // CUSTOM RANGE STATS
    // -------------------------------------------------------------
    fun analyzeCustomRange(
        transactions: List<Transaction>,
        startTime: Long,
        endTime: Long
    ): CustomRangeFinancialSummary {
        val validTransactions = filterValidTransactions(transactions)

        val cleanStart = startTime.coerceAtMost(endTime)
        val cleanEnd = endTime.coerceAtLeast(startTime)

        val rangeTx = validTransactions.filter { it.timestamp in cleanStart..cleanEnd }
        val (totalIncome, totalExpenses) = computeIncomeAndExpenses(rangeTx)
        val netSavings = totalIncome - totalExpenses

        val diffMillis = (cleanEnd - cleanStart).coerceAtLeast(1L)
        val totalDays = ((diffMillis / (24 * 60 * 60 * 1000L)) + 1).toInt().coerceAtLeast(1)
        val avgDaily = Money.fromDouble(totalExpenses.toDisplayDouble() / totalDays)

        val expenseTx = rangeTx.filter { it.financialNature == FinancialNature.CONSUMPTION_EXPENSE }

        // Comparison against preceding equivalent period
        val prevStart = (cleanStart - (diffMillis + 1)).coerceAtLeast(0L)
        val prevEnd = cleanStart - 1L
        val prevTx = validTransactions.filter { it.timestamp in prevStart..prevEnd && it.financialNature == FinancialNature.CONSUMPTION_EXPENSE }
        var prevPeriodExpenses = 0.0
        prevTx.forEach { prevPeriodExpenses += it.amount.toDisplayDouble() }

        val popPercentage = if (prevPeriodExpenses > 0) {
            (((totalExpenses.toDisplayDouble() - prevPeriodExpenses) / prevPeriodExpenses) * 100).toFloat()
        } else 0f

        val shortDate = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
        val rangeLabel = "${shortDate.format(Date(cleanStart))} – ${shortDate.format(Date(cleanEnd))} ($totalDays Days)"

        // Cumulative points for custom duration
        val customCumulativePoints = mutableListOf<CumulativeSpendPoint>()
        var customRunning = 0.0
        val cCal = Calendar.getInstance().apply { timeInMillis = cleanStart }
        val now = System.currentTimeMillis()
        val cShortDate = SimpleDateFormat("dd MMM", Locale.ENGLISH)

        for (dayIdx in 1..totalDays) {
            val dStart = cCal.timeInMillis
            val dEnd = (cCal.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            val dayTxs = expenseTx.filter { it.timestamp in dStart..dEnd }
            var dayTotal = 0.0
            dayTxs.forEach { dayTotal += it.amount.toDisplayDouble() }
            customRunning += dayTotal

            customCumulativePoints.add(
                CumulativeSpendPoint(
                    dayIndex = dayIdx,
                    timestamp = dStart,
                    dateLabel = cShortDate.format(Date(dStart)),
                    daySpend = Money.fromDouble(dayTotal),
                    cumulativeSpend = Money.fromDouble(customRunning),
                    isPastOrToday = dStart <= now
                )
            )
            cCal.add(Calendar.DAY_OF_MONTH, 1)
        }

        // Anomalies
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
                            reason = "Exceeds 3x average daily spend for selected period",
                            deviationMultiple = multiple
                        )
                    )
                }
            }
        }

        return CustomRangeFinancialSummary(
            rangeLabel = rangeLabel,
            startTime = cleanStart,
            endTime = cleanEnd,
            totalDays = totalDays,
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            netSavings = netSavings,
            averageDailySpend = avgDaily,
            periodOverPeriodPercentage = popPercentage,
            categoryBreakdown = buildCategoryBreakdown(expenseTx, totalExpenses),
            topMerchants = buildTopMerchants(expenseTx),
            anomalies = anomalies,
            cumulativeSpendPoints = customCumulativePoints
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
