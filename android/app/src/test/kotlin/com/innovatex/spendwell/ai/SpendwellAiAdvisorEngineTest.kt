package com.innovatex.spendwell.ai

import android.content.Context
import com.innovatex.spendwell.ai.model.ModelDownloadManager
import com.innovatex.spendwell.analytics.CycleWindow
import com.innovatex.spendwell.analytics.MonthlyFinancialSummary
import com.innovatex.spendwell.common.Money
import com.innovatex.spendwell.core.DispatcherProvider
import com.innovatex.spendwell.data.preferences.UserPreferencesRepository
import com.innovatex.spendwell.domain.model.Account
import com.innovatex.spendwell.domain.model.RecurringPayment
import com.innovatex.spendwell.domain.model.Transaction
import com.innovatex.spendwell.domain.repository.AccountRepository
import com.innovatex.spendwell.domain.repository.BudgetRepository
import com.innovatex.spendwell.domain.repository.RecurringPaymentRepository
import com.innovatex.spendwell.domain.repository.TransactionRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class SpendwellAiAdvisorEngineTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var context: Context
    private lateinit var transactionRepo: TransactionRepository
    private lateinit var accountRepo: AccountRepository
    private lateinit var budgetRepo: BudgetRepository
    private lateinit var recurringRepo: RecurringPaymentRepository
    private lateinit var modelDownloadManager: ModelDownloadManager
    private lateinit var preferencesRepo: UserPreferencesRepository
    private lateinit var testDispatchers: DispatcherProvider
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var engine: SpendwellAiAdvisorEngine

    @BeforeEach
    fun setup() {
        context = mockk(relaxed = true)
        transactionRepo = mockk(relaxed = true)
        accountRepo = mockk(relaxed = true)
        budgetRepo = mockk(relaxed = true)
        recurringRepo = mockk(relaxed = true)
        modelDownloadManager = mockk(relaxed = true)
        preferencesRepo = mockk(relaxed = true)

        every { preferencesRepo.userName } returns flowOf("Kamal")
        every { preferencesRepo.cycleStartDay } returns flowOf(1)
        every { preferencesRepo.isModelDownloaded } returns flowOf(false)
        every { transactionRepo.observeAllTransactions() } returns flowOf(emptyList())
        every { accountRepo.observeAccounts() } returns flowOf(emptyList())
        every { recurringRepo.observeActiveRecurring() } returns flowOf(emptyList())

        val nonExistentModel = File(tempDir, "missing_gemma.bin")
        every { modelDownloadManager.getModelFile() } returns nonExistentModel

        testDispatchers = object : DispatcherProvider {
            override val main: CoroutineDispatcher = testDispatcher
            override val io: CoroutineDispatcher = testDispatcher
            override val default: CoroutineDispatcher = testDispatcher
            override val unconfined: CoroutineDispatcher = testDispatcher
        }

        engine = SpendwellAiAdvisorEngine(
            context = context,
            transactionRepository = transactionRepo,
            accountRepository = accountRepo,
            budgetRepository = budgetRepo,
            recurringPaymentRepository = recurringRepo,
            modelDownloadManager = modelDownloadManager,
            preferencesRepository = preferencesRepo,
            dispatchers = testDispatchers
        )
    }

    @Test
    fun `isGemmaModelReady returns false when model file is missing`() {
        assertFalse(engine.isGemmaModelReady())
    }

    @Test
    fun `isGemmaModelReady returns true when model file exceeds threshold`() {
        val dummyModel = File(tempDir, "gemma-2b-it-cpu-int4.bin")
        val chunk = ByteArray(1024 * 1024)
        dummyModel.outputStream().use { fos ->
            repeat(55) {
                fos.write(chunk)
            }
        }
        every { modelDownloadManager.getModelFile() } returns dummyModel

        assertTrue(engine.isGemmaModelReady())
    }

    @Test
    fun `buildGemmaPrompt formats prompt with user name, financial stats, and query`() {
        val summary = MonthlyFinancialSummary(
            totalIncome = Money.of(75000.0),
            totalExpenses = Money.of(25000.0),
            netSavings = Money.of(50000.0),
            savingsRatePercentage = 66.7,
            averageDailySpend = Money.of(1000.0),
            projectedMonthEndSpend = Money.of(30000.0),
            safeToSpendPerDay = Money.of(1500.0),
            totalPaisaSpentOnRecurring = 500000L,
            discretionarySpendPercentage = 30.0,
            categoryBreakdown = emptyList(),
            cycleWindow = CycleWindow("November 2026", 1, 30, 20)
        )

        val prompt = engine.buildGemmaPrompt(
            userName = "Kamal",
            query = "How much can I spend this weekend?",
            summary = summary,
            recurring = emptyList(),
            accounts = emptyList()
        )

        assertTrue(prompt.contains("User Profile: Kamal"))
        assertTrue(prompt.contains("<start_of_turn>user"))
        assertTrue(prompt.contains("<end_of_turn>"))
        assertTrue(prompt.contains("How much can I spend this weekend?"))
        assertTrue(prompt.contains("75,000"))
        assertTrue(prompt.contains("25,000"))
    }

    @Test
    fun `answerQuery addresses user by name and returns tagged local response when model missing`() = runTest(testDispatcher) {
        val response = engine.answerQuery("How much have I spent?")

        assertTrue(response.contains("[Spendwell Local Engine]"))
        assertTrue(response.contains("Kamal"))
        assertTrue(response.contains("spent"))
    }
}
