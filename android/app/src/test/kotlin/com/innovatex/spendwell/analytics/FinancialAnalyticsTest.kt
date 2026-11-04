package com.innovatex.spendwell.analytics

import com.innovatex.spendwell.common.Money
import com.innovatex.spendwell.domain.model.FinancialNature
import com.innovatex.spendwell.domain.model.Transaction
import com.innovatex.spendwell.domain.model.TransactionStatus
import com.innovatex.spendwell.domain.model.TransactionType
import org.junit.jupiter.api.Assertions.assertEquals
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
}
