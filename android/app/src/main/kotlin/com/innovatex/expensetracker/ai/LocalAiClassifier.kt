package com.innovatex.expensetracker.ai

import com.innovatex.expensetracker.data.local.PreferencesDataStore
import com.innovatex.expensetracker.data.local.dao.UserFeedbackDao
import com.innovatex.expensetracker.di.IoDispatcher
import com.innovatex.expensetracker.domain.model.AiClassificationResult
import com.innovatex.expensetracker.domain.model.AuthenticityLabel
import com.innovatex.expensetracker.domain.model.TransactionType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Deterministic English/Hinglish screening first; LiteRT-LM runs only for ambiguous messages. */
@Singleton
class LocalAiClassifier @Inject constructor(
    private val modelStore: AiModelStore,
    private val userFeedbackDao: UserFeedbackDao,
    private val preferencesDataStore: PreferencesDataStore,
    private val llmClassifierEngine: LlmClassifierEngine,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val mutex = Mutex()

    suspend fun classify(text: String, merchant: String): AiClassificationResult? = classify(text, merchant, null)

    suspend fun classify(
        text: String,
        merchant: String,
        sender: String?,
        allowLanguageModel: Boolean = true
    ): AiClassificationResult = withContext(ioDispatcher) {
        mutex.withLock {
            val rulesResult = deterministicClassification(text, merchant)
            if (rulesResult.authenticityConfidence >= CERTAIN_RULE_CONFIDENCE) return@withLock rulesResult

            val textTokens = tokenize(text)
            val exemplars = runCatching { userFeedbackDao.getAllExemplars() }.getOrDefault(emptyList())
            val matchingFeedback = exemplars.mapNotNull { exemplar ->
                val snippet = exemplar.rawSnippet ?: return@mapNotNull null
                val similarity = jaccard(textTokens, tokenize(snippet))
                if (similarity >= FEEDBACK_SIMILARITY_THRESHOLD) similarity to exemplar.assignedLabel else null
            }.maxByOrNull { it.first }
            if (matchingFeedback != null) {
                return@withLock AiClassificationResult(
                    authenticityLabel = matchingFeedback.second,
                    authenticityConfidence = 0.80f,
                    categoryId = null,
                    categoryConfidence = null,
                    ranByAi = false
                )
            }

            val settings = runCatching { preferencesDataStore.appSettingsFlow.first() }.getOrNull()
            val llmAllowed = allowLanguageModel && settings?.aiClassificationEnabled != false
            if (llmAllowed && modelStore.isGenerativeLlmReady()) {
                llmClassifierEngine.analyze(text, merchant)?.let { modelResult ->
                    // The model may reject a candidate, but cannot promote an ambiguous message into totals.
                    if (modelResult.authenticityLabel != AuthenticityLabel.REAL_TRANSACTION) return@withLock modelResult
                }
            }
            rulesResult
        }
    }

    private fun deterministicClassification(text: String, merchant: String): AiClassificationResult {
        val lower = "$text $merchant".lowercase()
        val reminder = lower.containsAny(
            "reminder", "due on", "due date", "payment due", "amount due", "minimum due", "min due",
            "total due", "overdue", "bill generated", "upcoming payment", "will be debited", "will be deducted",
            "to be debited", "to be paid", "is scheduled", "scheduled for", "autopay scheduled", "mandate scheduled",
            "payment yaad", "kal tak pay", "jama karna hai", "due date hai", "bharne ki last date"
        )
        val promotion = lower.containsAny(
            "apply now", "pre-approved", "pre approved", "loan offer", "win cash", "voucher", "lucky winner",
            "claim reward", "exclusive offer", "limited time offer", "shop now", "flat 50%", "cashback up to",
            "get instant loan", "jeeto", "inaam", "offer sirf", "abhi apply", "loan paaye", "cashback paaye"
        )
        val completed = lower.containsAny(
            "debited", "credited", "withdrawn", "deposited", "spent", "purchase of", "paid rs", "paid inr",
            "payment of rs", "payment of inr", "transaction successful", "txn successful", "sent rs", "received rs",
            "upi txn", "upi transaction", "card ending", "a/c xx", "account xx", "charged rs", "refunded",
            "debit ho gaya", "credit ho gaya", "paise kat gaye", "payment ho gaya", "transaction hua",
            "paise mil gaye", "jama ho gaye"
        )
        val label = when {
            reminder -> AuthenticityLabel.PAYMENT_REMINDER
            promotion -> AuthenticityLabel.SPAM_PROMO
            completed -> AuthenticityLabel.REAL_TRANSACTION
            else -> AuthenticityLabel.UNCLASSIFIED
        }
        val confidence = when {
            reminder || promotion -> 0.94f
            completed -> 0.86f
            else -> 0.0f
        }
        val category = categoryFor(lower)
        return AiClassificationResult(
            authenticityLabel = label,
            authenticityConfidence = confidence,
            categoryId = category,
            categoryConfidence = if (category == "general") 0.45f else 0.78f,
            impliedTransactionType = when {
                lower.containsAny("credited", "received rs", "deposited", "salary", "refund") -> TransactionType.CREDIT
                completed -> TransactionType.DEBIT
                else -> null
            },
            ranByAi = false
        )
    }

    private fun categoryFor(text: String): String = when {
        text.containsAny("swiggy", "zomato", "mcdonald", "kfc", "domino", "starbucks", "cafe", "coffee", "restaurant", "food") -> "food_dining"
        text.containsAny("blinkit", "zepto", "instamart", "bigbasket", "supermarket", "grocery", "dmart") -> "groceries"
        text.containsAny("uber", "ola", "rapido", "metro", "railway", "irctc", "petrol", "fuel", "fastag") -> "transport"
        text.containsAny("amazon", "flipkart", "myntra", "ajio", "nykaa", "meesho", "retail", "croma") -> "shopping"
        text.containsAny("bescom", "electricity", "water bill", "airtel", "jio", "broadband", "indane", "recharge") -> "utilities"
        text.containsAny("bookmyshow", "pvr", "inox", "netflix", "spotify", "prime video", "hotstar") -> "entertainment"
        text.containsAny("apollo", "pharmacy", "medplus", "pharmeasy", "hospital", "clinic", "doctor") -> "health"
        text.containsAny("salary", "payroll", "dividend", "interest credited", "refund") -> "income"
        else -> "general"
    }

    private fun tokenize(text: String): Set<String> = text.lowercase().split(Regex("[^\\p{L}\\p{N}]+")).filter { it.length >= 3 }.toSet()
    private fun jaccard(a: Set<String>, b: Set<String>): Float =
        if (a.isEmpty() || b.isEmpty()) 0f else a.intersect(b).size.toFloat() / a.union(b).size
    private fun String.containsAny(vararg values: String): Boolean = values.any { contains(it, ignoreCase = true) }

    companion object {
        private const val CERTAIN_RULE_CONFIDENCE = 0.85f
        private const val FEEDBACK_SIMILARITY_THRESHOLD = 0.78f
    }
}
