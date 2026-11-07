package com.innovatex.spendwell.analytics

import com.innovatex.spendwell.common.Money
import com.innovatex.spendwell.domain.model.FinancialNature
import com.innovatex.spendwell.domain.model.Transaction
import com.innovatex.spendwell.domain.model.TransactionStatus
import com.innovatex.spendwell.domain.model.TransactionType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Calendar

class FinancialAnalyticsTest {

    @Test
    fun `test credit card bill payment and investment are excluded from consumption expenses`() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.OCTOBER, 15, 12, 0, 0)
        val timestamp = cal.timeInMillis

        val txList = listOf(
            // Actual consumption expense (₹ 1,500)
            Transaction(
                id = "1",
                amount = Money.fromString("1500.00"),
                type = TransactionType.DEBIT,
                status = TransactionStatus.SUCCESSFUL,
                financialNature = FinancialNature.CONSUMPTION_EXPENSE,
                timestamp = timestamp,
                accountId = "acc_1",
                description = "Swiggy",
                merchantName = "Swiggy",
                categoryId = "cat_food"
            ),
            // Credit card bill payment (₹ 15,000) - must NOT be counted as expense
            Transaction(
                id = "2",
                amount = Money.fromString("15000.00"),
                type = TransactionType.DEBIT,
                status = TransactionStatus.SUCCESSFUL,
                financialNature = FinancialNature.CREDIT_CARD_BILL_PAYMENT,
                timestamp = timestamp,
                accountId = "acc_1",
                description = "HDFC Card Payment"
            ),
            // Investment transfer (₹ 25,000) - must NOT be counted as expense
            Transaction(
                id = "3",
                amount = Money.fromString("25000.00"),
                type = TransactionType.DEBIT,
                status = TransactionStatus.SUCCESSFUL,
                financialNature = FinancialNature.INVESTMENT_TRANSFER,
                timestamp = timestamp,
                accountId = "acc_1",
                description = "Zerodha Broking"
            ),
            // Internal transfer (₹ 10,000) - must NOT be counted as expense
            Transaction(
                id = "3b",
                amount = Money.fromString("10000.00"),
                type = TransactionType.DEBIT,
                status = TransactionStatus.SUCCESSFUL,
                financialNature = FinancialNature.INTERNAL_TRANSFER,
                timestamp = timestamp,
                accountId = "acc_1",
                description = "Self Transfer"
            ),
            // Salary Inflow (₹ 80,000)
            Transaction(
                id = "4",
                amount = Money.fromString("80000.00"),
                type = TransactionType.CREDIT,
                status = TransactionStatus.SUCCESSFUL,
                financialNature = FinancialNature.INCOME,
                timestamp = timestamp,
                accountId = "acc_1",
                description = "Company Payroll"
            )
        )

        val summary = FinancialAnalyticsEngine.analyze(txList, 2026, Calendar.OCTOBER)

        assertEquals("₹ 80,000.00", summary.totalIncome.formatInr(true))
        // Total consumption expense should only be ₹ 1,500.00
        assertEquals("₹ 1,500.00", summary.totalExpenses.formatInr(true))
        // Net savings = 80,000 - 1,500 = 78,500
        assertEquals("₹ 78,500.00", summary.netSavings.formatInr(true))
    }

    @Test
    fun `test needs_review and failed transactions are excluded from totals`() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.OCTOBER, 15, 12, 0, 0)
        val timestamp = cal.timeInMillis

        val txList = listOf(
            Transaction(
                id = "1",
                amount = Money.fromString("1000.00"),
                type = TransactionType.DEBIT,
                status = TransactionStatus.SUCCESSFUL,
                financialNature = FinancialNature.CONSUMPTION_EXPENSE,
                timestamp = timestamp,
                accountId = "acc_1",
                description = "Grocery"
            ),
            Transaction(
                id = "2",
                amount = Money.fromString("50000.00"),
                type = TransactionType.DEBIT,
                status = TransactionStatus.NEEDS_REVIEW, // Suspicious or duplicate candidate
                financialNature = FinancialNature.CONSUMPTION_EXPENSE,
                timestamp = timestamp,
                accountId = "acc_1",
                description = "Pending Review Spike"
            ),
            Transaction(
                id = "3",
                amount = Money.fromString("2000.00"),
                type = TransactionType.DEBIT,
                status = TransactionStatus.FAILED,
                financialNature = FinancialNature.CONSUMPTION_EXPENSE,
                timestamp = timestamp,
                accountId = "acc_1",
                description = "Failed Debit"
            )
        )

        val summary = FinancialAnalyticsEngine.analyze(txList, 2026, Calendar.OCTOBER)
        // Only the successful ₹ 1,000 transaction should be counted
        assertEquals("₹ 1,000.00", summary.totalExpenses.formatInr(true))
    }

    @Test
    fun `test custom salary cycle window calculation`() {
        // Test cycle starting on the 10th of October 2026
        val cycle10th = FinancialAnalyticsEngine.calculateCycle(2026, Calendar.OCTOBER, 10)
        assertEquals("10 Oct – 09 Nov 2026", cycle10th.label)
        assertEquals(31, cycle10th.totalDaysInCycle)

        // Test cycle starting on the 1st (standard calendar)
        val cycle1st = FinancialAnalyticsEngine.calculateCycle(2026, Calendar.OCTOBER, 1)
        assertEquals("01 Oct – 31 Oct 2026", cycle1st.label)
        assertEquals(31, cycle1st.totalDaysInCycle)

        // Test cycle on last day of month (-1)
        val cycleLastDay = FinancialAnalyticsEngine.calculateCycle(2026, Calendar.OCTOBER, -1)
        assertEquals("31 Oct – 29 Nov 2026", cycleLastDay.label)
    }

    @Test
    fun `test custom cycle filtering partitions transactions accurately`() {
        // Salary cycle starts on the 10th of every month
        val cal = Calendar.getInstance()

        // Tx on 5th October (belongs to 10 Sep - 09 Oct cycle, NOT current cycle)
        cal.set(2026, Calendar.OCTOBER, 5, 12, 0, 0)
        val txBeforeCycle = Transaction(
            id = "tx_before",
            amount = Money.fromString("3000.00"),
            type = TransactionType.DEBIT,
            status = TransactionStatus.SUCCESSFUL,
            financialNature = FinancialNature.CONSUMPTION_EXPENSE,
            timestamp = cal.timeInMillis,
            accountId = "acc_1",
            description = "Previous Cycle Rent"
        )

        // Tx on 12th October (belongs to current 10 Oct - 09 Nov cycle)
        cal.set(2026, Calendar.OCTOBER, 12, 12, 0, 0)
        val txInCycle = Transaction(
            id = "tx_current",
            amount = Money.fromString("750.00"),
            type = TransactionType.DEBIT,
            status = TransactionStatus.SUCCESSFUL,
            financialNature = FinancialNature.CONSUMPTION_EXPENSE,
            timestamp = cal.timeInMillis,
            accountId = "acc_1",
            description = "Current Cycle Swiggy"
        )

        val summary = FinancialAnalyticsEngine.analyze(
            transactions = listOf(txBeforeCycle, txInCycle),
            year = 2026,
            monthZeroIndexed = Calendar.OCTOBER,
            cycleStartDay = 10
        )

        // Should strictly only include txInCycle (₹ 750.00)
        assertEquals("₹ 750.00", summary.totalExpenses.formatInr(true))
        assertEquals("10 Oct – 09 Nov 2026", summary.cycleWindow.label)
    }

    @Test
    fun `test refund reduces total expenses`() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.OCTOBER, 10, 10, 0, 0)
        val timestamp = cal.timeInMillis

        val txList = listOf(
            Transaction(
                id = "1",
                amount = Money.fromString("2000.00"),
                type = TransactionType.DEBIT,
                status = TransactionStatus.SUCCESSFUL,
                financialNature = FinancialNature.CONSUMPTION_EXPENSE,
                timestamp = timestamp,
                accountId = "acc_1",
                description = "Amazon Purchase"
            ),
            Transaction(
                id = "2",
                amount = Money.fromString("500.00"),
                type = TransactionType.CREDIT,
                status = TransactionStatus.REFUNDED,
                financialNature = FinancialNature.REFUND,
                timestamp = timestamp + 10000,
                accountId = "acc_1",
                description = "Amazon Refund"
            )
        )

        val summary = FinancialAnalyticsEngine.analyze(txList, 2026, Calendar.OCTOBER)
        // 2000 - 500 = 1500 net expense
        assertEquals("₹ 1,500.00", summary.totalExpenses.formatInr(true))
    }

    @Test
    fun `test reconciliation matching engine`() {
        val ledgerTx = listOf(
            Transaction(
                id = "tx1",
                amount = Money.fromString("450.00"),
                type = TransactionType.DEBIT,
                status = TransactionStatus.SUCCESSFUL,
                financialNature = FinancialNature.CONSUMPTION_EXPENSE,
                timestamp = 1000000L,
                accountId = "acc_1",
                description = "Swiggy",
                referenceId = "REF990011"
            )
        )

        val statementTx = listOf(
            Transaction(
                id = "st1",
                amount = Money.fromString("450.00"),
                type = TransactionType.DEBIT,
                status = TransactionStatus.SUCCESSFUL,
                financialNature = FinancialNature.CONSUMPTION_EXPENSE,
                timestamp = 1000050L,
                accountId = "acc_1",
                description = "UPI/SWIGGY",
                referenceId = "REF990011"
            )
        )

        val result = ReconciliationEngine.reconcile(ledgerTx, statementTx)
        assertEquals(1, result.matchedTransactions.size)
        assertTrue(result.unmatchedStatementTransactions.isEmpty())
        assertTrue(result.unmatchedLedgerTransactions.isEmpty())
        assertTrue(result.netDiscrepancy.isZero)
    }

    @Test
    fun `test last working day calculation adjusts for weekends`() {
        val cal = Calendar.getInstance()

        // October 2026: 31st is Saturday -> Last working day must be Friday 30th
        cal.set(2026, Calendar.OCTOBER, 1)
        assertEquals(30, FinancialAnalyticsEngine.getLastWorkingDay(cal))

        // May 2026: 31st is Sunday -> Last working day must be Friday 29th
        cal.set(2026, Calendar.MAY, 1)
        assertEquals(29, FinancialAnalyticsEngine.getLastWorkingDay(cal))

        // November 2026: 30th is Monday -> Last working day is 30th
        cal.set(2026, Calendar.NOVEMBER, 1)
        assertEquals(30, FinancialAnalyticsEngine.getLastWorkingDay(cal))

        // December 2026: 31st is Thursday -> Last working day is 31st
        cal.set(2026, Calendar.DECEMBER, 1)
        assertEquals(31, FinancialAnalyticsEngine.getLastWorkingDay(cal))
    }

    @Test
    fun `test cycle calculation for last working day cycleStartDay -2`() {
        // November 2026 cycle with cycleStartDay = -2 (Last Working Day)
        // Previous month: October 2026 (last working day is 30 Oct)
        // Current month: November 2026 (last working day is 30 Nov, so cycle ends 29 Nov)
        val cycle = FinancialAnalyticsEngine.calculateCycle(2026, Calendar.NOVEMBER, -2)

        val calStart = Calendar.getInstance().apply { timeInMillis = cycle.startTime }
        val calEnd = Calendar.getInstance().apply { timeInMillis = cycle.endTime }

        assertEquals(2026, calStart.get(Calendar.YEAR))
        assertEquals(Calendar.OCTOBER, calStart.get(Calendar.MONTH))
        assertEquals(30, calStart.get(Calendar.DAY_OF_MONTH))

        assertEquals(2026, calEnd.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, calEnd.get(Calendar.MONTH))
        assertEquals(29, calEnd.get(Calendar.DAY_OF_MONTH))

        assertEquals("30 Oct – 29 Nov", cycle.label)
        assertEquals("November 2026", cycle.monthLabel)
    }

    @Test
    fun `test cycle calculation for last day cycleStartDay -1`() {
        // November 2026 cycle with cycleStartDay = -1 (Last Day)
        // Previous month: October 2026 (last day is 31 Oct)
        // Current month: November 2026 (last day is 30 Nov, so cycle ends 29 Nov)
        val cycle = FinancialAnalyticsEngine.calculateCycle(2026, Calendar.NOVEMBER, -1)

        val calStart = Calendar.getInstance().apply { timeInMillis = cycle.startTime }
        val calEnd = Calendar.getInstance().apply { timeInMillis = cycle.endTime }

        assertEquals(31, calStart.get(Calendar.DAY_OF_MONTH))
        assertEquals(29, calEnd.get(Calendar.DAY_OF_MONTH))
        assertEquals("31 Oct – 29 Nov", cycle.label)
    }
}
