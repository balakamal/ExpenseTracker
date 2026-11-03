package com.innovatex.expensetracker.domain.usecase

import com.innovatex.expensetracker.domain.model.SourceChannel
import com.innovatex.expensetracker.domain.model.Transaction
import com.innovatex.expensetracker.domain.model.TransactionType
import com.innovatex.expensetracker.domain.repository.TransactionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DeduplicationTest {

    private val repository: TransactionRepository = mockk(relaxed = true)
    private lateinit var useCase: DeduplicateTransactionUseCase

    @BeforeEach
    fun setup() {
        useCase = DeduplicateTransactionUseCase(repository)
    }

    @Test
    fun `test matching SMS and Email transaction merges into single record`() = runTest {
        val existingSms = Transaction(
            id = "sms_001",
            amount = 18.75,
            currency = "USD",
            type = TransactionType.DEBIT,
            timestamp = 1000000L,
            monthKey = "2026-10",
            merchantRaw = "Starbucks Coffee",
            merchantClean = "Starbucks",
            sources = listOf(SourceChannel.SMS),
            accountLast4 = "1234"
        )

        val incomingEmail = Transaction(
            id = "email_002",
            amount = 18.75,
            currency = "USD",
            type = TransactionType.DEBIT,
            timestamp = 1060000L, // 60 seconds later
            monthKey = "2026-10",
            merchantRaw = "Starbucks Store #12",
            merchantClean = "Starbucks",
            sources = listOf(SourceChannel.EMAIL),
            accountLast4 = "1234"
        )

        coEvery {
            repository.findPotentialDuplicates(any(), any(), 18.75, "USD")
        } returns listOf(existingSms)

        val result = useCase(incomingEmail)
        assertTrue(result is DeduplicationResult.Merged)
        val merged = (result as DeduplicationResult.Merged).updatedTransaction

        assertEquals("sms_001", merged.id)
        assertTrue(merged.isDeduplicated)
        assertTrue(merged.sources.contains(SourceChannel.SMS))
        assertTrue(merged.sources.contains(SourceChannel.EMAIL))

        coVerify(exactly = 1) { repository.updateTransaction(any()) }
        coVerify(exactly = 0) { repository.saveTransaction(incomingEmail) }
    }

    @Test
    fun `test non-duplicate purchase creates new transaction`() = runTest {
        val newTxn = Transaction(
            id = "txn_new",
            amount = 99.0,
            currency = "USD",
            type = TransactionType.DEBIT,
            timestamp = 2000000L,
            monthKey = "2026-10",
            merchantRaw = "Apple Store",
            merchantClean = "Apple Store",
            sources = listOf(SourceChannel.SMS)
        )

        coEvery {
            repository.findPotentialDuplicates(any(), any(), 99.0, "USD")
        } returns emptyList()

        val result = useCase(newTxn)
        assertTrue(result is DeduplicationResult.NewTransaction)
        coVerify(exactly = 1) { repository.saveTransaction(newTxn) }
    }

    @Test
    fun `test bank SMS and merchant order confirmation merge into single enriched transaction`() = runTest {
        val bankTxn = Transaction(
            id = "bank_001",
            amount = 450.0,
            currency = "INR",
            type = TransactionType.DEBIT,
            timestamp = 1700000000000L,
            monthKey = "2023-11",
            merchantRaw = "POS 43019283 BANGALORE",
            merchantClean = "General Purchase",
            sources = listOf(SourceChannel.SMS),
            accountLast4 = "1234",
            rawSnippet = "Sent Rs. 450 to SWIGGY from HDFC Bank A/c 1234",
            notes = "VK-HDFCBK"
        )

        val merchantTxn = Transaction(
            id = "merchant_002",
            amount = 450.0,
            currency = "INR",
            type = TransactionType.DEBIT,
            timestamp = 1700000180000L, // 3 minutes later
            monthKey = "2023-11",
            merchantRaw = "Swiggy Order #98214",
            merchantClean = "Swiggy",
            categoryId = "food_dining",
            sources = listOf(SourceChannel.SMS),
            notes = "VM-SWIGGY"
        )

        coEvery {
            repository.findPotentialDuplicates(any(), any(), 450.0, "INR")
        } returns listOf(bankTxn)

        val result = useCase(merchantTxn)
        assertTrue(result is DeduplicationResult.Merged)
        val merged = (result as DeduplicationResult.Merged).updatedTransaction

        assertEquals("bank_001", merged.id)
        assertTrue(merged.isDeduplicated)
        assertEquals("Swiggy", merged.merchantClean)
        assertEquals("food_dining", merged.categoryId)
        assertEquals("1234", merged.accountLast4)
    }

    @Test
    fun `test matching reference ID merges regardless of timestamp difference`() = runTest {
        val originalTxn = Transaction(
            id = "txn_ref_1",
            amount = 1200.0,
            currency = "INR",
            type = TransactionType.DEBIT,
            timestamp = 1000000L,
            monthKey = "2026-10",
            merchantRaw = "Amazon Pay",
            merchantClean = "Amazon",
            referenceId = "UPI99887766",
            sources = listOf(SourceChannel.SMS)
        )

        val duplicateTxn = Transaction(
            id = "txn_ref_2",
            amount = 1200.0,
            currency = "INR",
            type = TransactionType.DEBIT,
            timestamp = 1000000L + (12 * 60 * 1000L), // 12 minutes later
            monthKey = "2026-10",
            merchantRaw = "Amazon India",
            merchantClean = "Amazon",
            referenceId = "UPI99887766",
            sources = listOf(SourceChannel.EMAIL)
        )

        coEvery {
            repository.findPotentialDuplicates(any(), any(), 1200.0, "INR")
        } returns listOf(originalTxn)

        val result = useCase(duplicateTxn)
        assertTrue(result is DeduplicationResult.Merged)
        val merged = (result as DeduplicationResult.Merged).updatedTransaction

        assertEquals("txn_ref_1", merged.id)
        assertTrue(merged.isDeduplicated)
        assertTrue(merged.sources.contains(SourceChannel.SMS))
        assertTrue(merged.sources.contains(SourceChannel.EMAIL))
    }

    @Test
    fun `test UPI transaction arriving tomorrow (18 hours later) merges as duplicate`() = runTest {
        val upiToday = Transaction(
            id = "upi_today",
            amount = 350.0,
            currency = "INR",
            type = TransactionType.DEBIT,
            timestamp = 1700000000000L,
            monthKey = "2023-11",
            merchantRaw = "Swiggy UPI",
            merchantClean = "Swiggy",
            sources = listOf(SourceChannel.MANUAL),
            accountLast4 = "5678"
        )

        val bankSmsTomorrow = Transaction(
            id = "bank_tomorrow",
            amount = 350.0,
            currency = "INR",
            type = TransactionType.DEBIT,
            timestamp = 1700000000000L + (18 * 60 * 60 * 1000L), // 18 hours later (tomorrow morning)
            monthKey = "2023-11",
            merchantRaw = "UPI/33214567/SWIGGY BANGALORE",
            merchantClean = "General Purchase",
            rawSnippet = "INR 350.00 debited from A/c XX5678 on 02-Nov for UPI/Swiggy",
            sources = listOf(SourceChannel.SMS),
            accountLast4 = "5678"
        )

        coEvery {
            repository.findPotentialDuplicates(any(), any(), 350.0, "INR")
        } returns listOf(upiToday)

        val result = useCase(bankSmsTomorrow)
        assertTrue(result is DeduplicationResult.Merged)
        val merged = (result as DeduplicationResult.Merged).updatedTransaction

        assertEquals("upi_today", merged.id)
        assertTrue(merged.isDeduplicated)
        assertTrue(merged.sources.contains(SourceChannel.SMS))
        assertTrue(merged.sources.contains(SourceChannel.MANUAL))
        assertEquals("Swiggy", merged.merchantClean)
    }

    @Test
    fun `test credit card settlement confirmation 3 days later merges as duplicate`() = runTest {
        val bankPaymentDebit = Transaction(
            id = "bank_card_pay",
            amount = 15000.0,
            currency = "INR",
            type = TransactionType.DEBIT,
            timestamp = 1700000000000L,
            monthKey = "2023-11",
            merchantRaw = "HDFC Credit Card Bill",
            merchantClean = "Credit Card Payment",
            rawSnippet = "Rs 15000 debited towards Credit Card payment",
            sources = listOf(SourceChannel.SMS)
        )

        val cardSettlementConfirmation = Transaction(
            id = "card_confirm",
            amount = 15000.0,
            currency = "INR",
            type = TransactionType.CREDIT,
            timestamp = 1700000000000L + (3 * 24 * 60 * 60 * 1000L), // 3 days later
            monthKey = "2023-11",
            merchantRaw = "HDFC Bank Card",
            merchantClean = "Credit Card",
            rawSnippet = "Thank you for payment of Rs 15000 towards your credit card ending 1234. Payment received.",
            sources = listOf(SourceChannel.SMS),
            accountLast4 = "1234"
        )

        coEvery {
            repository.findPotentialDuplicates(any(), any(), 15000.0, "INR")
        } returns listOf(bankPaymentDebit)

        val result = useCase(cardSettlementConfirmation)
        assertTrue(result is DeduplicationResult.Merged)
        val merged = (result as DeduplicationResult.Merged).updatedTransaction

        assertEquals("bank_card_pay", merged.id)
        assertTrue(merged.isDeduplicated)
    }
}
