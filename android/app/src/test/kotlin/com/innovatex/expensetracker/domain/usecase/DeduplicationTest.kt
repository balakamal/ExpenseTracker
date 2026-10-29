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
}
