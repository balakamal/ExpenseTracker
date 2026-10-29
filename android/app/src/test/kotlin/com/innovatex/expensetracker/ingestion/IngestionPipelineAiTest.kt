package com.innovatex.expensetracker.ingestion

import android.content.Context
import com.innovatex.expensetracker.ai.LocalAiClassifier
import com.innovatex.expensetracker.data.local.AppSettings
import com.innovatex.expensetracker.data.local.PreferencesDataStore
import com.innovatex.expensetracker.domain.model.AiClassificationResult
import com.innovatex.expensetracker.domain.model.AuthenticityLabel
import com.innovatex.expensetracker.domain.model.MerchantRule
import com.innovatex.expensetracker.domain.model.SourceChannel
import com.innovatex.expensetracker.domain.model.Transaction
import com.innovatex.expensetracker.domain.model.TransactionType
import com.innovatex.expensetracker.domain.repository.CategoryRepository
import com.innovatex.expensetracker.domain.usecase.DeduplicateTransactionUseCase
import com.innovatex.expensetracker.domain.usecase.DeduplicationResult
import com.innovatex.expensetracker.domain.usecase.ParseTransactionUseCase
import com.innovatex.expensetracker.widget.ExpenseWidgetSyncManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class IngestionPipelineAiTest {

    private lateinit var mockContext: Context
    private lateinit var mockParseUseCase: ParseTransactionUseCase
    private lateinit var mockDeduplicateUseCase: DeduplicateTransactionUseCase
    private lateinit var mockPreferencesDataStore: PreferencesDataStore
    private lateinit var mockWidgetSyncManager: ExpenseWidgetSyncManager
    private lateinit var mockAiClassifier: LocalAiClassifier
    private lateinit var mockCategoryRepository: CategoryRepository

    private lateinit var pipeline: IngestionPipeline

    private val sampleCandidate = Transaction(
        id = "test_1",
        amount = 450.0,
        currency = "INR",
        type = TransactionType.DEBIT,
        timestamp = 1700000000000L,
        monthKey = "2023-11",
        merchantRaw = "Swiggy Bangalore",
        merchantClean = "Swiggy",
        categoryId = "food_dining",
        sources = listOf(SourceChannel.SMS)
    )

    @BeforeEach
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockParseUseCase = mockk()
        mockDeduplicateUseCase = mockk()
        mockPreferencesDataStore = mockk()
        mockWidgetSyncManager = mockk(relaxed = true)
        mockAiClassifier = mockk()
        mockCategoryRepository = mockk()

        every { mockPreferencesDataStore.appSettingsFlow } returns flowOf(
            AppSettings(
                smsTrackingEnabled = true,
                emailTrackingEnabled = true,
                deduplicationWindowMinutes = 10,
                aiClassificationEnabled = true,
                aiSpamDetectionEnabled = true
            )
        )
        coEvery { mockPreferencesDataStore.isSpamSender(any()) } returns false
        coEvery { mockCategoryRepository.getAllRules() } returns flowOf(emptyList())

        pipeline = IngestionPipeline(
            context = mockContext,
            parseTransactionUseCase = mockParseUseCase,
            deduplicateTransactionUseCase = mockDeduplicateUseCase,
            preferencesDataStore = mockPreferencesDataStore,
            widgetSyncManager = mockWidgetSyncManager,
            localAiClassifier = mockAiClassifier,
            categoryRepository = mockCategoryRepository
        )
    }

    @Test
    fun `test pipeline retains promotional alerts in ledger tagged as SPAM_PROMO with isValidTransaction false`() = runTest {
        val sms = "Congratulations! Instant pre-approved loan of Rs 50000! Apply bit.ly"
        coEvery { mockParseUseCase.parse(sms, any(), SourceChannel.SMS, any()) } returns sampleCandidate
        coEvery { mockAiClassifier.classify(sms, any(), any(), any()) } returns AiClassificationResult(
            authenticityLabel = AuthenticityLabel.SPAM_PROMO,
            authenticityConfidence = 0.95f,
            categoryId = null,
            categoryConfidence = null
        )
        coEvery { mockDeduplicateUseCase(any(), any()) } answers {
            val txn = firstArg<Transaction>()
            DeduplicationResult.Inserted(txn)
        }

        val result = pipeline.processIncomingMessage(sms, 1700000000000L, SourceChannel.SMS)
        assertNotNull(result)
        val inserted = (result as DeduplicationResult.Inserted).transaction
        assertEquals(AuthenticityLabel.SPAM_PROMO, inserted.aiClassification?.authenticityLabel)
        assertFalse(inserted.isValidTransaction)
    }

    @Test
    fun `test pipeline retains payment reminders in ledger tagged as PAYMENT_REMINDER with isValidTransaction false`() = runTest {
        val sms = "Your postpaid bill of Rs 499 is due on 28-Oct. Pay now."
        coEvery { mockParseUseCase.parse(sms, any(), SourceChannel.SMS, any()) } returns sampleCandidate
        coEvery { mockAiClassifier.classify(sms, any(), any(), any()) } returns AiClassificationResult(
            authenticityLabel = AuthenticityLabel.PAYMENT_REMINDER,
            authenticityConfidence = 0.90f,
            categoryId = null,
            categoryConfidence = null
        )
        coEvery { mockDeduplicateUseCase(any(), any()) } answers {
            val txn = firstArg<Transaction>()
            DeduplicationResult.Inserted(txn)
        }

        val result = pipeline.processIncomingMessage(sms, 1700000000000L, SourceChannel.SMS)
        assertNotNull(result)
        val inserted = (result as DeduplicationResult.Inserted).transaction
        assertEquals(AuthenticityLabel.PAYMENT_REMINDER, inserted.aiClassification?.authenticityLabel)
        assertFalse(inserted.isValidTransaction)
    }

    @Test
    fun `AI category may enrich transaction but model implied direction cannot override parser`() = runTest {
        val sms = "Salary of INR 85,000 credited to account XX1234 on 28-Oct."
        val unclassifiedCandidate = sampleCandidate.copy(
            amount = 85000.0,
            categoryId = "general",
            type = TransactionType.DEBIT
        )
        coEvery { mockParseUseCase.parse(sms, any(), SourceChannel.SMS, any()) } returns unclassifiedCandidate
        coEvery { mockAiClassifier.classify(sms, any(), any(), any()) } returns AiClassificationResult(
            authenticityLabel = AuthenticityLabel.REAL_TRANSACTION,
            authenticityConfidence = 0.98f,
            categoryId = "income",
            categoryConfidence = 0.92f,
            impliedTransactionType = TransactionType.CREDIT
        )
        coEvery { mockDeduplicateUseCase(any(), any()) } answers {
            val txn = firstArg<Transaction>()
            DeduplicationResult.Inserted(txn)
        }

        val result = pipeline.processIncomingMessage(sms, 1700000000000L, SourceChannel.SMS)
        assertNotNull(result)
        val inserted = (result as DeduplicationResult.Inserted).transaction
        assertEquals("income", inserted.categoryId)
        assertEquals(TransactionType.DEBIT, inserted.type)
        assertTrue(inserted.isValidTransaction)
    }

    @Test
    fun `test user merchant rules take precedence over AI category prediction`() = runTest {
        val sms = "Paid Rs 300 to Star Supermarket on 28-Oct."
        val userRule = MerchantRule(
            pattern = "Star Supermarket",
            categoryId = "custom_supermarket",
            isRegex = false,
            priority = 100
        )
        coEvery { mockCategoryRepository.getAllRules() } returns flowOf(listOf(userRule))

        val candidate = sampleCandidate.copy(
            merchantClean = "Star Supermarket",
            categoryId = "general"
        )
        coEvery { mockParseUseCase.parse(sms, any(), SourceChannel.SMS, any()) } returns candidate
        coEvery { mockAiClassifier.classify(sms, any(), any(), any()) } returns AiClassificationResult(
            authenticityLabel = AuthenticityLabel.REAL_TRANSACTION,
            authenticityConfidence = 0.95f,
            categoryId = "groceries",
            categoryConfidence = 0.99f,
            impliedTransactionType = TransactionType.DEBIT
        )
        coEvery { mockDeduplicateUseCase(any(), any()) } answers {
            val txn = firstArg<Transaction>()
            DeduplicationResult.Inserted(txn)
        }

        val result = pipeline.processIncomingMessage(sms, 1700000000000L, SourceChannel.SMS)
        assertNotNull(result)
        val inserted = (result as DeduplicationResult.Inserted).transaction
        // AI category "groceries" must be ignored because user rule matches
        assertEquals("general", inserted.categoryId)
    }

    @Test
    fun `uncertain classification is retained outside financial totals`() = runTest {
        val sms = "Paid Rs 250 to Uber on 28-Oct."
        coEvery { mockParseUseCase.parse(sms, any(), SourceChannel.SMS, any()) } returns sampleCandidate
        coEvery { mockAiClassifier.classify(sms, any(), any(), any()) } returns AiClassificationResult(
            authenticityLabel = AuthenticityLabel.UNCLASSIFIED,
            authenticityConfidence = 0f,
            categoryId = null,
            categoryConfidence = null,
            ranByAi = false
        )
        coEvery { mockDeduplicateUseCase(any(), any()) } answers {
            val txn = firstArg<Transaction>()
            DeduplicationResult.Inserted(txn)
        }

        val result = pipeline.processIncomingMessage(sms, 1700000000000L, SourceChannel.SMS)
        assertNotNull(result)
        val inserted = (result as DeduplicationResult.Inserted).transaction
        assertEquals("food_dining", inserted.categoryId)
        assertEquals(AuthenticityLabel.UNCLASSIFIED, inserted.aiClassification?.authenticityLabel)
        assertFalse(inserted.isValidTransaction)
    }

    @Test
    fun `test user override on promotional alert makes it valid`() {
        val promoTxn = sampleCandidate.copy(
            aiClassification = AiClassificationResult(
                authenticityLabel = AuthenticityLabel.SPAM_PROMO,
                authenticityConfidence = 0.95f,
                categoryId = null,
                categoryConfidence = null
            )
        )
        assertFalse(promoTxn.isValidTransaction)

        val overriddenTxn = promoTxn.copy(userOverrideValid = true)
        assertTrue(overriddenTxn.isValidTransaction)
    }
}
