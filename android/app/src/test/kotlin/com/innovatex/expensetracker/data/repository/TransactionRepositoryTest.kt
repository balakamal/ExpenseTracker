package com.innovatex.expensetracker.data.repository

import com.innovatex.expensetracker.data.local.dao.TransactionDao
import com.innovatex.expensetracker.data.local.entity.TransactionEntity
import com.innovatex.expensetracker.domain.model.SourceChannel
import com.innovatex.expensetracker.domain.model.Transaction
import com.innovatex.expensetracker.domain.model.TransactionType
import com.innovatex.expensetracker.ingestion.parser.SmsParserEngine
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TransactionRepositoryTest {

    private val transactionDao: TransactionDao = mockk(relaxed = true)
    private val smsParserEngine = SmsParserEngine()
    private lateinit var repository: TransactionRepositoryImpl

    @BeforeEach
    fun setup() {
        repository = TransactionRepositoryImpl(transactionDao, smsParserEngine, Dispatchers.Unconfined)
    }

    @Test
    fun `test saveTransaction calls dao and returns id`() = runTest {
        val txn = Transaction(
            id = "txn_123",
            amount = 45.0,
            currency = "USD",
            type = TransactionType.DEBIT,
            timestamp = 1729570000000L,
            monthKey = "2026-10",
            merchantRaw = "Whole Foods",
            merchantClean = "Whole Foods"
        )

        coEvery { transactionDao.insertTransaction(any()) } returns 1L

        val returnedId = repository.saveTransaction(txn)
        assertEquals("txn_123", returnedId)
        coVerify(exactly = 1) { transactionDao.insertTransaction(any()) }
    }

    @Test
    fun `test getMonthlySummary aggregates debit and credit properly`() = runTest {
        val list = listOf(
            TransactionEntity(
                id = "1",
                amount = 100.0,
                currency = "USD",
                type = TransactionType.DEBIT,
                timestamp = 1000L,
                monthKey = "2026-10",
                merchantRaw = "Target",
                merchantClean = "Target",
                categoryId = "shopping",
                sources = listOf(SourceChannel.SMS)
            ),
            TransactionEntity(
                id = "2",
                amount = 50.0,
                currency = "USD",
                type = TransactionType.DEBIT,
                timestamp = 2000L,
                monthKey = "2026-10",
                merchantRaw = "Starbucks",
                merchantClean = "Starbucks",
                categoryId = "food_dining",
                sources = listOf(SourceChannel.SMS)
            ),
            TransactionEntity(
                id = "3",
                amount = 500.0,
                currency = "USD",
                type = TransactionType.CREDIT,
                timestamp = 3000L,
                monthKey = "2026-10",
                merchantRaw = "Payroll",
                merchantClean = "Payroll",
                categoryId = "income",
                sources = listOf(SourceChannel.SMS)
            )
        )

        every { transactionDao.getTransactionsForMonth("2026-10") } returns flowOf(list)

        val summary = repository.getMonthlySummary("2026-10").first()
        assertEquals(150.0, summary.totalDebit)
        assertEquals(500.0, summary.totalCredit)
        assertEquals(350.0, summary.netBalance)
        assertEquals(3, summary.transactionCount)
        assertEquals(100.0, summary.categoryTotals["shopping"])
        assertEquals(50.0, summary.categoryTotals["food_dining"])
    }

    @Test
    fun `test purgeSpamTransactions identifies and removes spam items`() = runTest {
        val validDebit = TransactionEntity(
            id = "valid_1",
            amount = 150.0,
            currency = "INR",
            type = TransactionType.DEBIT,
            timestamp = 1000L,
            monthKey = "2026-10",
            merchantRaw = "Swiggy",
            merchantClean = "Swiggy",
            rawSnippet = "Dear SBI UPI User, your A/c ...4321 debited by Rs.150.00 on 22Oct24 transfer to Swiggy. UPI Ref 429618291029.",
            sources = listOf(SourceChannel.SMS)
        )

        val spamLoan = TransactionEntity(
            id = "spam_loan",
            amount = 500000.0,
            currency = "INR",
            type = TransactionType.CREDIT,
            timestamp = 2000L,
            monthKey = "2026-10",
            merchantRaw = "Loan Approved",
            merchantClean = "Pre-approved Loan",
            rawSnippet = "Congratulations! You are eligible for pre-approved personal loan of Rs 5,00,000. Apply now.",
            sources = listOf(SourceChannel.SMS)
        )

        val spamBillDue = TransactionEntity(
            id = "spam_bill",
            amount = 1450.0,
            currency = "INR",
            type = TransactionType.DEBIT,
            timestamp = 3000L,
            monthKey = "2026-10",
            merchantRaw = "Electricity Bill Due",
            merchantClean = "Bill Due",
            rawSnippet = "Your electricity bill of Rs. 1,450 is due on 28-Oct. Pay now to avoid late fee.",
            sources = listOf(SourceChannel.SMS)
        )

        coEvery { transactionDao.getAllTransactions() } returns listOf(validDebit, spamLoan, spamBillDue)
        coEvery { transactionDao.deleteTransactionsByIds(any()) } returns Unit

        val purgedCount = repository.purgeSpamTransactions()
        assertEquals(2, purgedCount)
        coVerify(exactly = 1) {
            transactionDao.deleteTransactionsByIds(listOf("spam_loan", "spam_bill"))
        }
    }
}
