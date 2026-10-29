package com.innovatex.expensetracker.ai

import com.innovatex.expensetracker.data.local.AppSettings
import com.innovatex.expensetracker.data.local.PreferencesDataStore
import com.innovatex.expensetracker.data.local.dao.UserFeedbackDao
import com.innovatex.expensetracker.domain.model.AiClassificationResult
import com.innovatex.expensetracker.domain.model.AuthenticityLabel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LocalAiClassifierTest {

    private lateinit var mockModelStore: AiModelStore
    private lateinit var mockUserFeedbackDao: UserFeedbackDao
    private lateinit var mockPreferencesDataStore: PreferencesDataStore
    private lateinit var mockLlmClassifierEngine: LlmClassifierEngine

    private lateinit var classifier: LocalAiClassifier

    @BeforeEach
    fun setup() {
        mockModelStore = mockk(relaxed = true)
        mockUserFeedbackDao = mockk(relaxed = true)
        mockPreferencesDataStore = mockk(relaxed = true)
        mockLlmClassifierEngine = mockk(relaxed = true)

        every { mockPreferencesDataStore.appSettingsFlow } returns flowOf(AppSettings())
        every { mockModelStore.isGenerativeLlmReady() } returns false
        coEvery { mockUserFeedbackDao.getAllExemplars() } returns emptyList()

        classifier = LocalAiClassifier(
            modelStore = mockModelStore,
            userFeedbackDao = mockUserFeedbackDao,
            preferencesDataStore = mockPreferencesDataStore,
            llmClassifierEngine = mockLlmClassifierEngine,
            ioDispatcher = Dispatchers.Unconfined
        )
    }

    @Test
    fun `test fallback classifies standard real debit transaction without crashing`() = runTest {
        val result = classifier.classify(
            text = "Rs 350.00 debited from A/c XX1234 at Swiggy on 29-Oct-26",
            merchant = "Swiggy",
            sender = "VM-HDFCBK"
        )

        assertNotNull(result)
        assertEquals(AuthenticityLabel.REAL_TRANSACTION, result?.authenticityLabel)
        assertEquals("food_dining", result?.categoryId)
        assertTrue((result?.categoryConfidence ?: 0f) >= 0.75f)
        assertFalse(result?.ranByAi ?: true)
    }

    @Test
    fun `test fallback classifies promotional loan spam accurately`() = runTest {
        val result = classifier.classify(
            text = "Congratulations! You have pre-approved personal loan of Rs 500,000. Apply now at bit.ly/loan",
            merchant = "LoanOffer",
            sender = "DM-HDFCBK"
        )

        assertNotNull(result)
        assertEquals(AuthenticityLabel.SPAM_PROMO, result?.authenticityLabel)
    }

    @Test
    fun `test fallback classifies upcoming bill reminder accurately`() = runTest {
        val result = classifier.classify(
            text = "Reminder: Your credit card bill of Rs 12,450 is due on 05-Nov-26. Pay before due date to avoid charges.",
            merchant = "CreditCard",
            sender = "AD-ICICIB"
        )

        assertNotNull(result)
        assertEquals(AuthenticityLabel.PAYMENT_REMINDER, result?.authenticityLabel)
    }

    @Test
    fun `test transport merchant inferred accurately in fallback`() = runTest {
        val result = classifier.classify(
            text = "Paid Rs 240 to Uber India via UPI",
            merchant = "Uber",
            sender = "UPI"
        )

        assertNotNull(result)
        assertEquals("transport", result?.categoryId)
    }

    @Test
    fun `test utilities merchant inferred accurately in fallback`() = runTest {
        val result = classifier.classify(
            text = "Electricity bill payment of Rs 1,420 to BESCOM successful",
            merchant = "BESCOM",
            sender = "BESCOM"
        )

        assertNotNull(result)
        assertEquals("utilities", result?.categoryId)
    }

    @Test
    fun `ambiguous message remains unclassified when model is unavailable`() = runTest {
        val result = classifier.classify(
            text = "Your account update is ready. Please review the details.",
            merchant = "Bank",
            sender = "VM-BANK"
        )
        assertEquals(AuthenticityLabel.UNCLASSIFIED, result.authenticityLabel)
        assertEquals(0f, result.authenticityConfidence)
        assertFalse(result.ranByAi)
    }

    @Test
    fun `language model cannot promote ambiguous parsed message into financial totals`() = runTest {
        every { mockModelStore.isGenerativeLlmReady() } returns true
        coEvery { mockLlmClassifierEngine.analyze(any(), any()) } returns AiClassificationResult(
            authenticityLabel = AuthenticityLabel.REAL_TRANSACTION,
            authenticityConfidence = 0.70f,
            categoryId = null,
            categoryConfidence = null,
            ranByAi = true
        )

        val result = classifier.classify(
            text = "Aapka bank account update processed.",
            merchant = "Bank",
            sender = "VM-BANK"
        )

        assertEquals(AuthenticityLabel.UNCLASSIFIED, result.authenticityLabel)
        assertFalse(result.authenticityLabel == AuthenticityLabel.REAL_TRANSACTION)
    }
}

