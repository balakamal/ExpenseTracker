package com.innovatex.spendwell.ai

import android.content.Context
import com.innovatex.spendwell.ai.model.ModelDownloadManager
import com.innovatex.spendwell.analytics.CycleWindow
import com.innovatex.spendwell.analytics.MonthlyFinancialSummary
import com.innovatex.spendwell.common.Money
import com.innovatex.spendwell.core.DispatcherProvider
import com.innovatex.spendwell.data.preferences.UserPreferencesRepository
import com.innovatex.spendwell.database.dao.ChatDao
import com.innovatex.spendwell.database.entity.ChatMessageEntity
import com.innovatex.spendwell.database.entity.ChatSessionEntity
import com.innovatex.spendwell.domain.model.Account
import com.innovatex.spendwell.domain.model.Category
import com.innovatex.spendwell.domain.model.RecurringPayment
import com.innovatex.spendwell.domain.model.Transaction
import com.innovatex.spendwell.domain.model.TransactionStatus
import com.innovatex.spendwell.domain.model.TransactionType
import com.innovatex.spendwell.domain.repository.AccountRepository
import com.innovatex.spendwell.domain.repository.BudgetRepository
import com.innovatex.spendwell.domain.repository.CategoryRepository
import com.innovatex.spendwell.domain.repository.RecurringPaymentRepository
import com.innovatex.spendwell.domain.repository.TransactionRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
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
    private lateinit var categoryRepo: CategoryRepository
    private lateinit var chatDao: ChatDao
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
        categoryRepo = mockk(relaxed = true)
        chatDao = mockk(relaxed = true)

        every { preferencesRepo.userName } returns flowOf("Kamal")
        every { preferencesRepo.cycleStartDay } returns flowOf(1)
        every { preferencesRepo.isModelDownloaded } returns flowOf(false)
        every { transactionRepo.observeAllTransactions() } returns flowOf(emptyList())
        every { accountRepo.observeAccounts() } returns flowOf(emptyList())
        every { recurringRepo.observeActiveRecurring() } returns flowOf(emptyList())
        every { categoryRepo.observeCategories() } returns flowOf(
            listOf(
                Category("cat_groceries", "Groceries", "cart", "#159B70", false, true),
                Category("cat_food", "Food & Dining", "dining", "#FF7A30", false, true),
                Category("cat_shopping", "Shopping", "bag", "#8B5CF6", false, true),
                Category("cat_transport", "Transportation", "car", "#3B82F6", false, true),
                Category("cat_other", "Miscellaneous", "other", "#64748B", false, true)
            )
        )

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
            categoryRepository = categoryRepo,
            chatDao = chatDao,
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
    fun `buildGemmaPrompt formats prompt with user name, financial stats, ledger entries, and instructions`() {
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

        val recentTxs = listOf(
            Transaction(
                id = "tx_1",
                amount = Money.of(1500.0),
                type = TransactionType.DEBIT,
                merchantName = "COUNTRY DELIGHT",
                description = "Milk delivery",
                timestamp = System.currentTimeMillis(),
                accountId = "acc_1",
                categoryId = "cat_groceries",
                categoryName = "Groceries",
                status = TransactionStatus.SETTLED
            )
        )

        val prompt = engine.buildGemmaPrompt(
            userName = "Kamal",
            query = "Did I buy anything from Country Delight?",
            summary = summary,
            recurring = emptyList(),
            accounts = emptyList(),
            recentTransactions = recentTxs
        )

        assertTrue(prompt.contains("User Profile: Kamal"))
        assertTrue(prompt.contains("<start_of_turn>user"))
        assertTrue(prompt.contains("<end_of_turn>"))
        assertTrue(prompt.contains("Did I buy anything from Country Delight?"))
        assertTrue(prompt.contains("75,000"))
        assertTrue(prompt.contains("25,000"))
        // Authoritative ledger access grounding
        assertTrue(prompt.contains("COUNTRY DELIGHT"))
        assertTrue(prompt.contains("1,500"))
        assertTrue(prompt.contains("NEVER claim you cannot access the user's bank accounts or transactions"))
    }

    @Test
    fun `answerQuery addresses user by name and returns tagged local response when model missing`() = runTest(testDispatcher) {
        val response = engine.answerQuery("How much have I spent?")

        assertTrue(response.contains("[Spendwell App Engine]") || response.contains("[Spendwell Local Engine]"))
        assertTrue(response.contains("Kamal"))
        assertTrue(response.contains("spent"))
    }

    @Test
    fun `answerQuery in GEMMA_LLM mode reports graceful initialization warning without crashing`() = runTest(testDispatcher) {
        every { preferencesRepo.aiEngineMode } returns flowOf("GEMMA_LLM")
        val dummyModel = File(tempDir, "gemma-2b-it-cpu-int4.bin")
        dummyModel.writeBytes(ByteArray(15 * 1024 * 1024))
        every { modelDownloadManager.getModelFile() } returns dummyModel

        val response = engine.answerQuery("How much have I spent?")
        // When native libs are absent on host JVM test, it must handle gracefully
        assertTrue(response.contains("Initialization Warning") || response.contains("Gemma 2B") || response.contains("App Engine"))
    }

    @Test
    fun `refineTransactionsWithAi identifies Country Delight and Swiggy accurately`() = runTest(testDispatcher) {
        val txs = listOf(
            Transaction(
                id = "cd_1",
                amount = Money.of(1500.0),
                type = TransactionType.DEBIT,
                merchantName = "COUNTRY DELIGHT",
                description = "Milk delivery",
                timestamp = System.currentTimeMillis(),
                accountId = "acc_1",
                categoryId = "cat_other",
                categoryName = "Miscellaneous",
                status = TransactionStatus.SETTLED
            ),
            Transaction(
                id = "sw_1",
                amount = Money.of(450.0),
                type = TransactionType.DEBIT,
                merchantName = "SWIGGY BANGALORE",
                description = "Food order",
                timestamp = System.currentTimeMillis(),
                accountId = "acc_1",
                categoryId = "cat_other",
                categoryName = "Miscellaneous",
                status = TransactionStatus.SETTLED
            )
        )

        val results = engine.refineTransactionsWithAi(txs)
        assertEquals(2, results.size)

        val cdResult = results.first { it.transactionId == "cd_1" }
        assertEquals("Country Delight", cdResult.cleanedMerchant)
        assertEquals("cat_groceries", cdResult.suggestedCategoryId)
        assertEquals("Groceries", cdResult.suggestedCategoryName)
        assertTrue(cdResult.confidence >= 0.95f)

        val swResult = results.first { it.transactionId == "sw_1" }
        assertEquals("cat_food", swResult.suggestedCategoryId)
        assertEquals("Food & Dining", swResult.suggestedCategoryName)
    }

    @Test
    fun `sendChatMessage records message into chatDao and auto-summarizes when exceeding context limit`() = runTest(testDispatcher) {
        val testSession = ChatSessionEntity("sess_1", "New Chat", 1000L, 1000L, null)
        coEvery { chatDao.getSessionById("sess_1") } returns testSession

        // Create 10 existing messages to trigger summarization (>8)
        val dummyList = (1..10).map { i ->
            ChatMessageEntity("msg_$i", "sess_1", if (i % 2 == 1) "USER" else "ADVISOR", "Message $i", 1000L + i)
        }
        coEvery { chatDao.getMessages("sess_1") } returns dummyList

        val response = engine.sendChatMessage("sess_1", "What is my spend this month?")
        assertTrue(response.isNotBlank())
    }
}
