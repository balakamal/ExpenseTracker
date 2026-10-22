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
}
