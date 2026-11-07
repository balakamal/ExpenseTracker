package com.innovatex.spendwell.ai

import android.content.Context
import android.util.Log
import com.innovatex.spendwell.ai.model.ModelDownloadManager
import com.innovatex.spendwell.analytics.FinancialAnalyticsEngine
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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class AiChatMessage(
    val id: String,
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageSender {
    USER,
    ADVISOR
}

@Singleton
class SpendwellAiAdvisorEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val budgetRepository: BudgetRepository,
    private val recurringPaymentRepository: RecurringPaymentRepository,
    private val modelDownloadManager: ModelDownloadManager,
    private val preferencesRepository: UserPreferencesRepository,
    private val dispatchers: DispatcherProvider
) {
    companion object {
        private const val TAG = "SpendwellAiAdvisor"
    }

    val isModelDownloaded: Flow<Boolean> = preferencesRepository.isModelDownloaded

    fun isGemmaModelReady(): Boolean {
        val file = modelDownloadManager.getModelFile()
        return file.exists() && file.length() > 50_000_000L
    }

    @Volatile
    private var llmInferenceInstance: Any? = null
    @Volatile
    private var lastLoadedPath: String? = null

    private fun getOrInitLlmInference(modelFile: File): Any? {
        if (llmInferenceInstance != null && lastLoadedPath == modelFile.absolutePath) {
            return llmInferenceInstance
        }
        return try {
            val optionsClass = Class.forName("com.google.mediapipe.tasks.genai.llminference.LlmInference\$LlmInferenceOptions")
            val builderMethod = optionsClass.getMethod("builder")
            val builder = builderMethod.invoke(null)
            builder.javaClass.getMethod("setModelPath", String::class.java).invoke(builder, modelFile.absolutePath)
            builder.javaClass.getMethod("setMaxTokens", Int::class.javaPrimitiveType).invoke(builder, 512)
            val options = builder.javaClass.getMethod("build").invoke(builder)

            val llmClass = Class.forName("com.google.mediapipe.tasks.genai.llminference.LlmInference")
            val createMethod = llmClass.getMethod("createFromOptions", Context::class.java, optionsClass)
            val instance = createMethod.invoke(null, context, options)
            llmInferenceInstance = instance
            lastLoadedPath = modelFile.absolutePath
            Log.i(TAG, "Gemma 2B LiteRT on-device inference successfully initialized from ${modelFile.absolutePath}")
            instance
        } catch (e: Throwable) {
            Log.w(TAG, "LlmInference initialization bypassed or unavailable: ${e.message}")
            null
        }
    }

    private fun runGemmaInference(instance: Any, prompt: String): String? {
        return try {
            val generateMethod = instance.javaClass.getMethod("generateResponse", String::class.java)
            val response = generateMethod.invoke(instance, prompt) as? String
            response?.trim()
        } catch (e: Throwable) {
            Log.e(TAG, "Gemma inference generation failed", e)
            null
        }
    }

    fun buildGemmaPrompt(
        userName: String,
        query: String,
        summary: MonthlyFinancialSummary,
        recurring: List<RecurringPayment>,
        accounts: List<Account>
    ): String {
        val name = userName.ifBlank { "User" }
        val topCats = summary.categoryBreakdown.take(4).joinToString(", ") {
            "${it.categoryName}: ${it.amount.formatInr(true)} (${String.format(Locale.ENGLISH, "%.1f", it.percentageOfTotal)}%)"
        }
        val subs = recurring.take(4).joinToString(", ") { "${it.title}: ${it.amount.formatInr(true)}" }
        val accts = accounts.take(4).joinToString(", ") { "${it.bankName} ${it.accountName}: ${it.currentBalance.formatInr(true)}" }

        return """
<start_of_turn>user
You are Spendwell AI, an expert personal financial advisor operating 100% locally and privately on the user's phone.
User Profile: $name
Current Financial Ledger Summary for this month:
- Total Income: ${summary.totalIncome.formatInr(true)}
- Total Expenses: ${summary.totalExpenses.formatInr(true)}
- Net Savings: ${summary.netSavings.formatInr(true)} (${String.format(Locale.ENGLISH, "%.1f", summary.savingsRatePercentage)}% savings rate)
- Average Daily Spend: ${summary.averageDailySpend.formatInr(true)}
- Projected Month-End Spend: ${summary.projectedMonthEndSpend.formatInr(true)}
- Top Spending Categories: ${if (topCats.isBlank()) "None recorded" else topCats}
- Active Recurring Commitments: ${if (subs.isBlank()) "None" else subs}
- Accounts: ${if (accts.isBlank()) "None" else accts}

User Question: $query

Instructions: Answer the user's question directly, concisely, and warmly. Address $name by name if appropriate. Ground all financial numbers accurately in the ledger data provided above.
<end_of_turn>
<start_of_turn>model
""".trimIndent()
    }

    /**
     * Executes a user query using on-device Gemma 2B LLM with prompt grounding
     * or context-aware heuristic fallback engine.
     * Guarantees zero ledger modification capability.
     */
    suspend fun answerQuery(query: String): String = withContext(dispatchers.io) {
        val userName = preferencesRepository.userName.first()
        val allTx = transactionRepository.observeAllTransactions().first()
        val cal = Calendar.getInstance()
        val cycleDay = preferencesRepository.cycleStartDay.first()
        val summary = FinancialAnalyticsEngine.analyze(
            transactions = allTx,
            year = cal.get(Calendar.YEAR),
            monthZeroIndexed = cal.get(Calendar.MONTH),
            cycleStartDay = cycleDay
        )
        val recurring = recurringPaymentRepository.observeActiveRecurring().first()
        val accounts = accountRepository.observeAccounts().first()

        val modelFile = modelDownloadManager.getModelFile()
        val isModelReady = modelFile.exists() && modelFile.length() > 50_000_000L

        if (isModelReady) {
            val llm = getOrInitLlmInference(modelFile)
            if (llm != null) {
                val prompt = buildGemmaPrompt(userName, query, summary, recurring, accounts)
                val gemmaResponse = runGemmaInference(llm, prompt)
                if (!gemmaResponse.isNullOrBlank()) {
                    return@withContext "[Gemma 2B Local LLM]\n\n$gemmaResponse"
                }
            }
        }

        // Context-aware heuristic fallback engine
        val nameGreeting = if (userName.isNotBlank()) "Hello $userName! " else ""
        val lower = query.lowercase(Locale.ROOT)
        val responseText = when {
            lower.contains("spent") || lower.contains("expense") || lower.contains("outflow") -> {
                val totalExpenses = summary.totalExpenses.formatInr(true)
                val avgDaily = summary.averageDailySpend.formatInr(true)
                "${nameGreeting}You have spent $totalExpenses so far this month, averaging $avgDaily per day. Your projected month-end spend is ${summary.projectedMonthEndSpend.formatInr(true)}."
            }

            lower.contains("income") || lower.contains("earned") || lower.contains("salary") -> {
                val totalIncome = summary.totalIncome.formatInr(true)
                val savings = summary.netSavings.formatInr(true)
                "${nameGreeting}Your total income recorded this month is $totalIncome. Your current net savings is $savings (Savings rate: ${String.format(Locale.ENGLISH, "%.1f", summary.savingsRatePercentage)}%)."
            }

            lower.contains("category") || lower.contains("top category") || lower.contains("where") -> {
                if (summary.categoryBreakdown.isEmpty()) {
                    "${nameGreeting}You don't have any categorised consumption expenses recorded yet."
                } else {
                    val top3 = summary.categoryBreakdown.take(3).joinToString("\n") {
                        "• ${it.categoryName}: ${it.amount.formatInr(true)} (${String.format(Locale.ENGLISH, "%.1f", it.percentageOfTotal)}%)"
                    }
                    "${nameGreeting}Your top spending categories this month:\n$top3"
                }
            }

            lower.contains("subscription") || lower.contains("recurring") || lower.contains("bills") -> {
                if (recurring.isEmpty()) {
                    "${nameGreeting}No active recurring subscriptions or bills were found in your ledger."
                } else {
                    var total = Money.ZERO
                    val items = recurring.joinToString("\n") {
                        total += it.amount
                        "• ${it.title}: ${it.amount.formatInr(true)} (${it.cadence.lowercase()})"
                    }
                    "${nameGreeting}Active recurring commitments:\n$items\n\nTotal commitment: ${total.formatInr(true)}"
                }
            }

            lower.contains("cut") || lower.contains("save") || lower.contains("recommend") || lower.contains("advice") -> {
                val topCat = summary.categoryBreakdown.firstOrNull()
                if (topCat != null) {
                    "${nameGreeting}Based on your ledger, '${topCat.categoryName}' accounts for ${String.format(Locale.ENGLISH, "%.1f", topCat.percentageOfTotal)}% of your expenses (${topCat.amount.formatInr(true)}). Setting a monthly budget on this category could save you up to 15% next month."
                } else {
                    "${nameGreeting}Maintain your spending discipline! Track all incoming alerts and verify any flagged duplicate items in the Review Center."
                }
            }

            else -> {
                "${nameGreeting}I am your Spendwell On-Device AI Advisor running locally. You can ask me:\n• 'How much did I spend this month?'\n• 'What are my top spending categories?'\n• 'List my recurring subscriptions'\n• 'How can I save more money?'\n\n(Tip: Download Gemma 2B in Settings for advanced open-ended reasoning)."
            }
        }

        val tag = if (isModelReady) "[Gemma 2B Fallback Engine]\n\n" else "[Spendwell Local Engine]\n\n"
        tag + responseText
    }
}
