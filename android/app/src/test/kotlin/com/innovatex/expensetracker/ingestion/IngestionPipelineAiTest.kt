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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
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
    fun `test pipeline drops high-confidence spam identified by AI`() = runTest {
        val sms = "Congratulations! Instant pre-approved loan of Rs 50000! Apply bit.ly"
        coEvery { mockParseUseCase.parse(sms, any(), SourceChannel.SMS, any()) } returns sampleCandidate
        coEvery { mockAiClassifier.classify(sms, any()) } returns AiClassificationResult(
            authenticityLabel = AuthenticityLabel.SPAM_PROMO,
            authenticityConfidence = 0.95f,
            categoryId = null,
            categoryConfidence = null
        )

        val result = pipeline.processIncomingMessage(sms, 1700000000000L, SourceChannel.SMS)
        assertNull(result)
        coVerify(exactly = 0) { mockDeduplicateUseCase(any(), any()) }
    }

    @Test
    fun `test pipeline drops payment reminders identified by AI`() = runTest {
        val sms = "Your postpaid bill of Rs 499 is due on 28-Oct. Pay now."
        coEvery { mockParseUseCase.parse(sms, any(), SourceChannel.SMS, any()) } returns sampleCandidate
        coEvery { mockAiClassifier.classify(sms, any()) } returns AiClassificationResult(
            authenticityLabel = AuthenticityLabel.PAYMENT_REMINDER,
            authenticityConfidence = 0.90f,
            categoryId = null,
            categoryConfidence = null
        )

        val result = pipeline.processIncomingMessage(sms, 1700000000000L, SourceChannel.SMS)
        assertNull(result)
        coVerify(exactly = 0) { mockDeduplicateUseCase(any(), any()) }
    }

    @Test
    fun `test AI category and implied type enrich transaction when confidence is high`() = runTest {
        val sms = "Salary of INR 85,000 credited to account XX1234 on 28-Oct."
        val unclassifiedCandidate = sampleCandidate.copy(
            amount = 85000.0,
            categoryId = "general",
            type = TransactionType.DEBIT
        )
        coEvery { mockParseUseCase.parse(sms, any(), SourceChannel.SMS, any()) } returns unclassifiedCandidate
        coEvery { mockAiClassifier.classify(sms, any()) } returns AiClassificationResult(
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
        assertEquals(TransactionType.CREDIT, inserted.type)
    }

    @Test
    fun `test user explicit MerchantRule takes precedence over AI classification`() = runTest {
        val sms = "INR 1200 spent on Amazon India"
        coEvery { mockCategoryRepository.getAllRules() } returns flowOf(
            listOf(
                MerchantRule(
                    pattern = "Swiggy",
                    categoryId = "groceries", // User explicit custom category
                    isRegex = false
                )
            )
        )
        val userRuleCandidate = sampleCandidate.copy(categoryId = "groceries")
        coEvery { mockParseUseCase.parse(sms, any(), SourceChannel.SMS, any()) } returns userRuleCandidate
        coEvery { mockAiClassifier.classify(sms, any()) } returns AiClassificationResult(
            authenticityLabel = AuthenticityLabel.REAL_TRANSACTION,
            authenticityConfidence = 0.99f,
            categoryId = "food_dining", // AI says food_dining
            categoryConfidence = 0.95f
        )

        coEvery { mockDeduplicateUseCase(any(), any()) } answers {
            val txn = firstArg<Transaction>()
            DeduplicationResult.Inserted(txn)
        }

        val result = pipeline.processIncomingMessage(sms, 1700000000000L, SourceChannel.SMS)
        assertNotNull(result)
        val inserted = (result as DeduplicationResult.Inserted).transaction
        // Since user rule matched, candidate's rule category ("groceries") is preserved and not overwritten by AI ("food_dining")
        assertEquals("groceries", inserted.categoryId)
    }

    @Test
    fun `test pipeline gracefully falls back to regex when AI returns null or is unavailable`() = runTest {
        val sms = "INR 500 debited from A/c XX1234 at McDonald's"
        coEvery { mockParseUseCase.parse(sms, any(), SourceChannel.SMS, any()) } returns sampleCandidate
        coEvery { mockAiClassifier.classify(sms, any()) } returns null // AI model not ready

        coEvery { mockDeduplicateUseCase(any(), any()) } answers {
            val txn = firstArg<Transaction>()
            DeduplicationResult.Inserted(txn)
        }

        val result = pipeline.processIncomingMessage(sms, 1700000000000L, SourceChannel.SMS)
        assertNotNull(result)
        val inserted = (result as DeduplicationResult.Inserted).transaction
        assertEquals("food_dining", inserted.categoryId)
        assertNull(inserted.aiClassification)
    }

    @Test
    fun `test pipeline does not drop AI spam when aiSpamDetectionEnabled is false (Local mode)`() = runTest {
        every { mockPreferencesDataStore.appSettingsFlow } returns flowOf(
            AppSettings(
                smsTrackingEnabled = true,
                emailTrackingEnabled = true,
                deduplicationWindowMinutes = 10,
                aiClassificationEnabled = true,
                aiSpamDetectionEnabled = false
            )
        )
        val sms = "Instant loan approved! Apply bit.ly"
        coEvery { mockParseUseCase.parse(sms, any(), SourceChannel.SMS, any()) } returns sampleCandidate
        coEvery { mockAiClassifier.classify(sms, any()) } returns AiClassificationResult(
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
        coVerify(exactly = 1) { mockDeduplicateUseCase(any(), any()) }
    }
}

