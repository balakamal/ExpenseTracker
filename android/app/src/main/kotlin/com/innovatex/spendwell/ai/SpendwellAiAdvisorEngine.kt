package com.innovatex.spendwell.ai

import android.content.Context
import android.util.Log
import com.innovatex.spendwell.ai.model.ModelDownloadManager
import com.innovatex.spendwell.analytics.FinancialAnalyticsEngine
import com.innovatex.spendwell.analytics.MonthlyFinancialSummary
import com.innovatex.spendwell.common.Money
import com.innovatex.spendwell.core.DispatcherProvider
import com.innovatex.spendwell.data.preferences.UserPreferencesRepository
import com.innovatex.spendwell.database.dao.ChatDao
import com.innovatex.spendwell.database.entity.ChatMessageEntity
import com.innovatex.spendwell.database.entity.ChatSessionEntity
import com.innovatex.spendwell.domain.model.Account
import com.innovatex.spendwell.domain.model.RecurringPayment
import com.innovatex.spendwell.domain.model.Transaction
import com.innovatex.spendwell.domain.model.TransactionType
import com.innovatex.spendwell.domain.repository.AccountRepository
import com.innovatex.spendwell.domain.repository.BudgetRepository
import com.innovatex.spendwell.domain.repository.CategoryRepository
import com.innovatex.spendwell.domain.repository.RecurringPaymentRepository
import com.innovatex.spendwell.domain.repository.TransactionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
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

data class AiRefinementResult(
    val transactionId: String,
    val originalMerchant: String,
    val cleanedMerchant: String,
    val suggestedCategoryId: String,
    val suggestedCategoryName: String,
    val confidence: Float,
    val tags: List<String>
)

data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

@Singleton
class SpendwellAiAdvisorEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val budgetRepository: BudgetRepository,
    private val recurringPaymentRepository: RecurringPaymentRepository,
    private val modelDownloadManager: ModelDownloadManager,
    private val preferencesRepository: UserPreferencesRepository,
    private val dispatchers: DispatcherProvider,
    private val chatDao: ChatDao,
    private val categoryRepository: CategoryRepository
) {
    companion object {
        private const val TAG = "SpendwellAiAdvisor"
    }

    val isModelDownloaded: Flow<Boolean> = preferencesRepository.isModelDownloaded
    val aiEngineMode: Flow<String> = preferencesRepository.aiEngineMode

    suspend fun setAiEngineMode(mode: String) {
        preferencesRepository.setAiEngineMode(mode)
    }

    fun isGemmaModelReady(): Boolean {
        val file = modelDownloadManager.getModelFile()
        return file.exists() && file.length() > 10_000_000L
    }

    @Volatile
    private var llmInferenceInstance: Any? = null
    @Volatile
    private var lastLoadedPath: String? = null

    @Volatile
    var lastInitErrorMessage: String? = null
        private set

    @Volatile
    var lastInferenceErrorMessage: String? = null
        private set

    private fun getOrInitLlmInference(modelFile: File): Any? {
        if (llmInferenceInstance != null && lastLoadedPath == modelFile.absolutePath) {
            return llmInferenceInstance
        }

        // 1. Try MediaPipe Tasks GenAI LlmInference
        try {
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
            lastInitErrorMessage = null
            Log.i(TAG, "Gemma 2B MediaPipe on-device inference successfully initialized from ${modelFile.absolutePath}")
            return instance
        } catch (e: Throwable) {
            val causeMsg = e.cause?.message ?: e.message ?: e.javaClass.simpleName
            Log.w(TAG, "MediaPipe LlmInference initialization attempt failed: $causeMsg")
            lastInitErrorMessage = "MediaPipe: $causeMsg"
        }

        // 2. Try LiteRT-LM Engine
        try {
            val engineConfigClass = Class.forName("com.google.ai.edge.litertlm.EngineConfig")
            val backendClass = Class.forName("com.google.ai.edge.litertlm.Backend\$CPU")
            val backendInstance = backendClass.getDeclaredConstructor().newInstance()

            // Find best matching constructor (supports 1-arg, 2-arg, or full multi-arg with default padding)
            val constructors = engineConfigClass.constructors
            val targetConstructor = constructors.firstOrNull { c ->
                val p = c.parameterTypes
                p.isNotEmpty() && p[0] == String::class.java && (p.size == 1 || (p.size == 2 && p[1].isAssignableFrom(backendClass)))
            } ?: constructors.firstOrNull { c ->
                val p = c.parameterTypes
                p.isNotEmpty() && p[0] == String::class.java
            }

            if (targetConstructor != null) {
                val paramTypes = targetConstructor.parameterTypes
                val paramCount = paramTypes.size
                val args = arrayOfNulls<Any>(paramCount)
                args[0] = modelFile.absolutePath
                if (paramCount > 1 && paramTypes[1].isAssignableFrom(backendClass)) {
                    args[1] = backendInstance
                }
                for (i in 2 until paramCount) {
                    val pType = paramTypes[i]
                    args[i] = when (pType) {
                        Int::class.javaPrimitiveType -> 0
                        Boolean::class.javaPrimitiveType -> false
                        Long::class.javaPrimitiveType -> 0L
                        Float::class.javaPrimitiveType -> 0.0f
                        Double::class.javaPrimitiveType -> 0.0
                        else -> null
                    }
                }

                val config = targetConstructor.newInstance(*args)
                if (config != null) {
                    val engineClass = Class.forName("com.google.ai.edge.litertlm.Engine")
                    val engineInstance = engineClass.getConstructor(engineConfigClass).newInstance(config)
                    val initMethod = engineClass.getMethod("initialize")
                    initMethod.invoke(engineInstance)

                    llmInferenceInstance = engineInstance
                    lastLoadedPath = modelFile.absolutePath
                    lastInitErrorMessage = null
                    Log.i(TAG, "LiteRT-LM Engine successfully initialized from ${modelFile.absolutePath}")
                    return engineInstance
                }
            }
        } catch (e: Throwable) {
            val causeMsg = e.cause?.message ?: e.message ?: e.javaClass.simpleName
            Log.w(TAG, "LiteRT-LM Engine initialization attempt failed: $causeMsg")
            lastInitErrorMessage = (lastInitErrorMessage?.let { "$it; LiteRT: $causeMsg" }) ?: "LiteRT: $causeMsg"
        }

        return null
    }

    private fun runGemmaInference(instance: Any, prompt: String): String? {
        lastInferenceErrorMessage = null
        // 1. Try generateResponse (MediaPipe LlmInference)
        try {
            val generateMethod = instance.javaClass.getMethod("generateResponse", String::class.java)
            val response = generateMethod.invoke(instance, prompt) as? String
            if (!response.isNullOrBlank()) return response.trim()
        } catch (e: Throwable) {
            lastInferenceErrorMessage = e.cause?.message ?: e.message
        }

        // 2. Try LiteRT-LM createConversation + sendMessage
        try {
            val createConvMethod = instance.javaClass.getMethod("createConversation")
            val conversation = createConvMethod.invoke(instance)
            if (conversation != null) {
                val sendMethod = conversation.javaClass.methods.firstOrNull {
                    it.name == "sendMessage" && it.parameterTypes.size == 1
                } ?: conversation.javaClass.methods.firstOrNull {
                    it.name == "sendMessageAsync" && it.parameterTypes.size == 1
                }
                if (sendMethod != null) {
                    val result = sendMethod.invoke(conversation, prompt)
                    if (result is String && result.isNotBlank()) {
                        return result.trim()
                    } else if (result != null) {
                        val textProp = result.javaClass.methods.firstOrNull {
                            it.name == "getText" || it.name == "getContent"
                        }
                        val str = (textProp?.invoke(result) as? String) ?: result.toString()
                        if (str.isNotBlank() && !str.startsWith(result.javaClass.name)) {
                            return str.trim()
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            lastInferenceErrorMessage = e.cause?.message ?: e.message
        }

        return null
    }

    fun buildGemmaPrompt(
        userName: String,
        query: String,
        summary: MonthlyFinancialSummary,
        recurring: List<RecurringPayment>,
        accounts: List<Account>,
        recentTransactions: List<Transaction> = emptyList(),
        priorSummary: String? = null,
        recentChatTurns: List<Pair<String, String>> = emptyList()
    ): String {
        val name = userName.ifBlank { "User" }
        val topCats = summary.categoryBreakdown.take(4).joinToString(", ") {
            "${it.categoryName}: ${it.amount.formatInr(true)} (${String.format(Locale.ENGLISH, "%.1f", it.percentageOfTotal)}%)"
        }
        val subs = recurring.take(4).joinToString(", ") { "${it.title}: ${it.amount.formatInr(true)}" }
        val accts = accounts.take(4).joinToString(", ") { "${it.bankName} ${it.accountName}: ${it.currentBalance.formatInr(true)}" }

        val acctMap = accounts.associateBy { it.id }
        val txListing = if (recentTransactions.isEmpty()) {
            "No individual transactions recorded yet."
        } else {
            recentTransactions.take(15).joinToString("\n") { tx ->
                val dateStr = SimpleDateFormat("dd MMM", Locale.ENGLISH).format(Date(tx.timestamp))
                val sign = if (tx.type == TransactionType.CREDIT) "+" else "−"
                val acct = tx.accountId?.let { acctMap[it] }
                val acctTag = if (acct != null) " (${acct.bankName} ••${acct.accountNumberLast4})" else ""
                "• $dateStr: ${tx.merchantName} ${sign}${tx.amount.formatInr(true)} [${tx.categoryName}]$acctTag"
            }
        }

        val priorContextBlock = if (!priorSummary.isNullOrBlank()) {
            "\n[Summary of Prior Discussion]:\n$priorSummary\n"
        } else ""

        val dialogueBlock = if (recentChatTurns.isNotEmpty()) {
            "\n[Recent Dialogue]:\n" + recentChatTurns.joinToString("\n") { (u, a) -> "User: $u\nAdvisor: $a" } + "\n"
        } else ""

        return """
<start_of_turn>user
CRITICAL OPERATING CONTEXT:
You are Spendwell AI, an expert, trustworthy personal financial advisor operating 100% locally and privately on the user's phone.
You HAVE full, authorized offline access to the user's actual financial ledger records, bank accounts, and transactions listed below.
NEVER state that you cannot access their bank account, financial records, or ledger—because their offline transaction history is explicitly provided right here in your context.
Answer the user's question directly, concisely, and warmly. Address $name by name if appropriate. Ground all financial numbers accurately in the ledger data and recent transactions provided below.

User Profile: $name
Current Financial Ledger Summary for this month:
- Total Income: ${summary.totalIncome.formatInr(true)}
- Total Expenses: ${summary.totalExpenses.formatInr(true)}
- Net Savings: ${summary.netSavings.formatInr(true)} (${String.format(Locale.ENGLISH, "%.1f", summary.savingsRatePercentage)}% savings rate)
- Average Daily Spend: ${summary.averageDailySpend.formatInr(true)}
- Projected Month-End Spend: ${summary.projectedMonthEndSpend.formatInr(true)}
- Top Spending Categories: ${if (topCats.isBlank()) "None recorded" else topCats}
- Active Recurring Commitments: ${if (subs.isBlank()) "None" else subs}
- Registered Accounts: ${if (accts.isBlank()) "None" else accts}

Recent Transactions (Offline Ledger):
$txListing
$priorContextBlock$dialogueBlock
User Question: $query
<end_of_turn>
<start_of_turn>model
""".trimIndent()
    }

    /**
     * Executes a user query using on-device Gemma 2B LLM or deterministic
     * Spendwell financial analytics engine, routed by selected engine mode.
     */
    suspend fun answerQuery(query: String, explicitEngineMode: String? = null): String = withContext(dispatchers.io) {
        answerQueryInternal(query = query, explicitEngineMode = explicitEngineMode)
    }

    private suspend fun answerQueryInternal(
        query: String,
        explicitEngineMode: String? = null,
        priorSummary: String? = null,
        recentChatTurns: List<Pair<String, String>> = emptyList()
    ): String = withContext(dispatchers.io) {
        val selectedEngineMode = explicitEngineMode ?: preferencesRepository.aiEngineMode.first()
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

        fun runDeterministicAppEngine(): String {
            val nameGreeting = if (userName.isNotBlank()) "Hello $userName! " else ""
            val lower = query.lowercase(Locale.ROOT)

            // 1. Check for specific merchant in user query (e.g. "Country Delight", "Swiggy", "Amazon")
            val matchingTx = allTx.filter { tx ->
                tx.merchantName.isNotBlank() && lower.contains(tx.merchantName.lowercase(Locale.ROOT))
            }
            if (matchingTx.isNotEmpty()) {
                val sample = matchingTx.first()
                val totalSpent = matchingTx.fold(Money.ZERO) { acc, t -> acc + t.amount }
                val txLines = matchingTx.take(3).joinToString("\n") { t ->
                    val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(Date(t.timestamp))
                    "• $dateStr: ${t.amount.formatInr(true)} (${t.categoryName})"
                }
                return "${nameGreeting}Yes! I found ${matchingTx.size} transaction(s) for '${sample.merchantName}' in your local ledger totaling ${totalSpent.formatInr(true)}:\n$txLines"
            }

            // 2. Check for "last transaction" or "latest transaction"
            if (lower.contains("last") || lower.contains("latest") || lower.contains("recent transaction")) {
                val latest = allTx.maxByOrNull { it.timestamp }
                if (latest != null) {
                    val dateStr = SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.ENGLISH).format(Date(latest.timestamp))
                    return "${nameGreeting}Your most recent recorded transaction was '${latest.merchantName}' for ${latest.amount.formatInr(true)} on $dateStr (${latest.categoryName})."
                } else {
                    return "${nameGreeting}You don't have any transactions recorded in your ledger yet."
                }
            }

            // 3. Check for bank / accounts / balance inquiry
            if (lower.contains("bank") || lower.contains("account") || lower.contains("balance")) {
                if (accounts.isEmpty()) {
                    return "${nameGreeting}No linked bank accounts or cards are currently registered in your local ledger."
                } else {
                    val acctLines = accounts.joinToString("\n") { "• ${it.bankName} (${it.accountName}): ${it.currentBalance.formatInr(true)}" }
                    return "${nameGreeting}Here are your registered accounts and recorded ledger balances:\n$acctLines"
                }
            }

            return when {
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
                    "${nameGreeting}I am your Spendwell On-Device AI Advisor running locally. You can ask me:\n• 'How much did I spend this month?'\n• 'What was my last transaction?'\n• 'Did I spend at Country Delight?'\n• 'What are my top spending categories?'\n• 'List my recurring subscriptions'\n• 'How can I save more money?'\n\n(Tip: Switch to Gemma 2B mode for open-ended generative insights)."
                }
            }
        }

        // Direct Route: App Engine selected
        if (selectedEngineMode == "APP_ENGINE") {
            return@withContext "[Spendwell App Engine]\n\n" + runDeterministicAppEngine()
        }

        val modelFile = modelDownloadManager.getModelFile()
        val isModelFileExisting = modelFile.exists() && modelFile.length() > 10_000_000L

        // Direct Route: Gemma LLM selected but file missing
        if (selectedEngineMode == "GEMMA_LLM" && !isModelFileExisting) {
            return@withContext "[Gemma 2B Mode: Model Not Downloaded]\n\n" +
                "The Gemma 2B model weights have not been downloaded to this device yet.\n\n" +
                "• Target file: ${modelFile.name}\n" +
                "• Expected size: ~1.42 GB (CPU INT4 Quantized)\n\n" +
                "Please download Gemma 2B in Settings > AI Engine or use the dropdown above to select 'Spendwell App Engine' for instant calculations."
        }

        // Attempt LLM execution
        if (isModelFileExisting) {
            val llm = getOrInitLlmInference(modelFile)
            if (llm != null) {
                val prompt = buildGemmaPrompt(
                    userName = userName,
                    query = query,
                    summary = summary,
                    recurring = recurring,
                    accounts = accounts,
                    recentTransactions = allTx.sortedByDescending { it.timestamp }.take(20),
                    priorSummary = priorSummary,
                    recentChatTurns = recentChatTurns
                )
                val gemmaResponse = runGemmaInference(llm, prompt)
                if (!gemmaResponse.isNullOrBlank()) {
                    return@withContext "[Gemma 2B Local LLM]\n\n$gemmaResponse"
                } else if (selectedEngineMode == "GEMMA_LLM") {
                    return@withContext "[Gemma 2B Mode: Generation Warning]\n\n" +
                        "The model initialized but returned an empty response (${lastInferenceErrorMessage ?: "Inference timed out"}).\n\n" +
                        "Answer from Spendwell App Engine:\n\n" + runDeterministicAppEngine()
                }
            } else if (selectedEngineMode == "GEMMA_LLM") {
                return@withContext "[Gemma 2B Mode: Initialization Warning]\n\n" +
                    "Model binary found (${modelFile.length() / (1024 * 1024)} MB), but the local inference runtime could not be started:\n" +
                    "${lastInitErrorMessage ?: "Memory allocation or native library error"}.\n\n" +
                    "Answer from Spendwell App Engine:\n\n" + runDeterministicAppEngine()
            }
        }

        // Default / Auto Fallback
        val tag = if (isModelFileExisting) "[Spendwell App Engine (Auto Fallback)]\n\n" else "[Spendwell App Engine]\n\n"
        tag + runDeterministicAppEngine()
    }

    // ---------------------------------------------------------
    // Professional Multi-Session Chat Harness Management (Item 4)
    // ---------------------------------------------------------

    fun observeAllSessions(): Flow<List<ChatSessionEntity>> = chatDao.observeAllSessions()

    fun observeMessages(sessionId: String): Flow<List<ChatMessageEntity>> = chatDao.observeMessages(sessionId)

    suspend fun getOrCreateActiveSession(): ChatSessionEntity = withContext(dispatchers.io) {
        val latest = chatDao.getLatestSession()
        if (latest != null) return@withContext latest
        val newSession = ChatSessionEntity(
            id = UUID.randomUUID().toString(),
            title = "New Chat",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        chatDao.insertSession(newSession)
        newSession
    }

    suspend fun createNewSession(title: String = "New Chat"): ChatSessionEntity = withContext(dispatchers.io) {
        val newSession = ChatSessionEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        chatDao.insertSession(newSession)
        newSession
    }

    suspend fun deleteSession(sessionId: String) = withContext(dispatchers.io) {
        chatDao.deleteSession(sessionId)
    }

    suspend fun renameSession(sessionId: String, newTitle: String) = withContext(dispatchers.io) {
        val session = chatDao.getSessionById(sessionId)
        if (session != null) {
            chatDao.updateSession(session.copy(title = newTitle, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun sendChatMessage(
        sessionId: String,
        userQuery: String,
        explicitEngineMode: String? = null
    ): String = withContext(dispatchers.io) {
        // 1. Record user message
        val userMsg = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            sender = "USER",
            text = userQuery.trim(),
            timestamp = System.currentTimeMillis()
        )
        chatDao.insertMessage(userMsg)

        // 2. Context Window Assessment & Compression
        val existingMessages = chatDao.getMessages(sessionId)
        val session = chatDao.getSessionById(sessionId)
        var priorSummary = session?.summary
        val recentTurns = mutableListOf<Pair<String, String>>()

        // When conversation exceeds 8 messages, summarize older turns
        if (existingMessages.size > 8) {
            val olderMessages = existingMessages.take(existingMessages.size - 6)
            val olderSummaryText = olderMessages.chunked(2).joinToString("; ") { chunk ->
                val u = chunk.firstOrNull { it.sender == "USER" }?.text?.take(45) ?: ""
                val a = chunk.firstOrNull { it.sender == "ADVISOR" }?.text?.take(45) ?: ""
                "Q: '$u' -> A: '$a'"
            }
            priorSummary = if (priorSummary.isNullOrBlank()) olderSummaryText else "$priorSummary; $olderSummaryText"
            session?.copy(summary = priorSummary, updatedAt = System.currentTimeMillis())?.let {
                chatDao.updateSession(it)
            }
        }

        // Build recent dialog turns (last 6 messages)
        val recentMsgs = existingMessages.takeLast(6)
        var idx = 0
        while (idx < recentMsgs.size - 1) {
            if (recentMsgs[idx].sender == "USER" && recentMsgs[idx + 1].sender == "ADVISOR") {
                recentTurns.add(Pair(recentMsgs[idx].text, recentMsgs[idx + 1].text))
                idx += 2
            } else {
                idx++
            }
        }

        // 3. Compute Response with full ledger grounding
        val response = answerQueryInternal(
            query = userQuery,
            explicitEngineMode = explicitEngineMode,
            priorSummary = priorSummary,
            recentChatTurns = recentTurns
        )

        // 4. Record Advisor Response
        val advisorMsg = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            sender = "ADVISOR",
            text = response,
            timestamp = System.currentTimeMillis()
        )
        chatDao.insertMessage(advisorMsg)

        // 5. Auto-title session if default "New Chat"
        if (session != null) {
            val updatedTitle = if (session.title == "New Chat") {
                userQuery.take(28).trim().replaceFirstChar { it.uppercase() }
            } else session.title
            chatDao.updateSession(session.copy(title = updatedTitle, updatedAt = System.currentTimeMillis()))
        }

        response
    }

    // ---------------------------------------------------------
    // AI Transaction Intelligence & Accuracy Processing (Item 5)
    // ---------------------------------------------------------

    suspend fun refineTransactionsWithAi(candidates: List<Transaction>): List<AiRefinementResult> = withContext(dispatchers.io) {
        val categories = categoryRepository.observeCategories().first()
        val catMap = categories.associateBy { it.id }

        candidates.map { tx ->
            val clean = tx.merchantName.trim().ifBlank { tx.description.trim() }
            val lower = clean.lowercase(Locale.ROOT)

            val (suggestedCatId, confidence, cleanMerchant, tags) = when {
                lower.contains("country delight") || lower.contains("countrydelight") ->
                    Tuple4("cat_groceries", 0.98f, "Country Delight", listOf("#Milk", "#Groceries", "#DailyNeeds"))
                lower.contains("swiggy") || lower.contains("zomato") || lower.contains("mcdonald") ->
                    Tuple4("cat_food", 0.95f, clean.replaceFirstChar { it.uppercase() }, listOf("#Food", "#DiningOut"))
                lower.contains("blinkit") || lower.contains("zepto") || lower.contains("instamart") || lower.contains("bigbasket") ->
                    Tuple4("cat_groceries", 0.95f, clean.replaceFirstChar { it.uppercase() }, listOf("#QuickCommerce", "#Groceries"))
                lower.contains("uber") || lower.contains("ola") || lower.contains("rapido") ->
                    Tuple4("cat_transport", 0.95f, clean.replaceFirstChar { it.uppercase() }, listOf("#Commute", "#Cabs"))
                lower.contains("amazon") || lower.contains("flipkart") || lower.contains("myntra") || lower.contains("ajio") ->
                    Tuple4("cat_shopping", 0.95f, clean.replaceFirstChar { it.uppercase() }, listOf("#Shopping", "#Online"))
                lower.contains("apollo") || lower.contains("1mg") || lower.contains("pharmeasy") ->
                    Tuple4("cat_health", 0.95f, clean.replaceFirstChar { it.uppercase() }, listOf("#Pharmacy", "#Health"))
                lower.contains("netflix") || lower.contains("spotify") || lower.contains("prime video") || lower.contains("hotstar") ->
                    Tuple4("cat_entertainment", 0.95f, clean.replaceFirstChar { it.uppercase() }, listOf("#Subscription", "#OTT"))
                lower.contains("airtel") || lower.contains("jio") || lower.contains("bescom") || lower.contains("electricity") ->
                    Tuple4("cat_utilities", 0.95f, clean.replaceFirstChar { it.uppercase() }, listOf("#Bills", "#Utilities"))
                lower.contains("zerodha") || lower.contains("groww") ->
                    Tuple4("cat_investment", 0.95f, clean.replaceFirstChar { it.uppercase() }, listOf("#Investments", "#Stocks"))
                else ->
                    Tuple4("cat_other", 0.75f, clean.replaceFirstChar { it.uppercase() }, listOf("#Expense"))
            }

            val categoryName = catMap[suggestedCatId]?.name ?: "Miscellaneous"
            AiRefinementResult(
                transactionId = tx.id,
                originalMerchant = tx.merchantName,
                cleanedMerchant = cleanMerchant,
                suggestedCategoryId = suggestedCatId,
                suggestedCategoryName = categoryName,
                confidence = confidence,
                tags = tags
            )
        }
    }
}
