package com.innovatex.expensetracker.ai

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OnnxValue
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import com.innovatex.expensetracker.data.local.AiEnginePreference
import com.innovatex.expensetracker.data.local.PreferencesDataStore
import com.innovatex.expensetracker.data.local.dao.UserFeedbackDao
import com.innovatex.expensetracker.di.IoDispatcher
import com.innovatex.expensetracker.domain.model.AiClassificationResult
import com.innovatex.expensetracker.domain.model.AuthenticityLabel
import com.innovatex.expensetracker.domain.model.TransactionType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.nio.LongBuffer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.exp

@Singleton
class LocalAiClassifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelStore: AiModelStore,
    private val userFeedbackDao: UserFeedbackDao,
    private val preferencesDataStore: PreferencesDataStore,
    private val llmClassifierEngine: LlmClassifierEngine,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val ortEnv: OrtEnvironment by lazy { OrtEnvironment.getEnvironment() }
    private var ortSession: OrtSession? = null
    private var tokenizer: WordPieceTokenizer? = null
    private val mutex = Mutex()

    val categoryLabels = listOf(
        "food_dining", "groceries", "transport", "shopping",
        "utilities", "entertainment", "health", "income", "general"
    )

    val authenticityLabels = listOf(
        AuthenticityLabel.REAL_TRANSACTION,
        AuthenticityLabel.SPAM_PROMO,
        AuthenticityLabel.PAYMENT_REMINDER
    )

    suspend fun classify(text: String, merchant: String): AiClassificationResult? =
        classify(text, merchant, null)

    suspend fun classify(
        text: String,
        merchant: String,
        sender: String?
    ): AiClassificationResult? = withContext(ioDispatcher) {
        // --- Layer 1: Exact User Feedback Adaptation (Learned Memory) ---
        if (!sender.isNullOrBlank()) {
            val senderExemplar = userFeedbackDao.findLatestForSender(sender.trim().uppercase())
            if (senderExemplar != null) {
                return@withContext AiClassificationResult(
                    authenticityLabel = senderExemplar.assignedLabel,
                    authenticityConfidence = 1.0f,
                    categoryId = null,
                    categoryConfidence = null,
                    impliedTransactionType = null,
                    ranByAi = true
                )
            }
        }

        if (merchant.isNotBlank() && merchant != "Unknown Merchant") {
            val merchantExemplar = userFeedbackDao.findLatestForMerchant(merchant.trim())
            if (merchantExemplar != null) {
                return@withContext AiClassificationResult(
                    authenticityLabel = merchantExemplar.assignedLabel,
                    authenticityConfidence = 1.0f,
                    categoryId = null,
                    categoryConfidence = null,
                    impliedTransactionType = null,
                    ranByAi = true
                )
            }
        }

        // --- Layer 2: Few-Shot Exemplar Similarity Adaptation ---
        val textTokens = tokenizeSimple(text)
        val allExemplars = runCatching { userFeedbackDao.getAllExemplars() }.getOrDefault(emptyList())
        var bestSimilarity = 0f
        var bestExemplarLabel: AuthenticityLabel? = null

        for (exemplar in allExemplars) {
            val exSnippet = exemplar.rawSnippet ?: continue
            val sim = computeJaccardSimilarity(textTokens, tokenizeSimple(exSnippet))
            if (sim > bestSimilarity) {
                bestSimilarity = sim
                bestExemplarLabel = exemplar.assignedLabel
            }
        }

        if (bestSimilarity >= 0.65f && bestExemplarLabel != null) {
            return@withContext AiClassificationResult(
                authenticityLabel = bestExemplarLabel,
                authenticityConfidence = (0.85f + (bestSimilarity * 0.15f)).coerceAtMost(0.98f),
                categoryId = null,
                categoryConfidence = null,
                impliedTransactionType = null,
                ranByAi = true
            )
        }

        // --- Layer 3: Engine Selection (Generative LLM vs Financial Transformer) ---
        val settings = runCatching { preferencesDataStore.appSettingsFlow.first() }.getOrNull()
        val preferredEngine = settings?.preferredAiEngine ?: AiEnginePreference.GENERATIVE_LLM

        // Case A: Generative SLM/LLM (Snapdragon 8 Gen 3 / Tensor / Dimensity)
        if (preferredEngine == AiEnginePreference.GENERATIVE_LLM && modelStore.isGenerativeLlmReady()) {
            val llmResult = llmClassifierEngine.analyze(text, merchant)
            if (llmResult != null) {
                return@withContext llmResult
            }
        }

        // Case B: High-Accuracy Dedicated Financial Transformer (ONNX Runtime)
        if (preferredEngine == AiEnginePreference.FINANCIAL_TRANSFORMER && modelStore.isTransformerReady()) {
            val onnxResult = runTransformerInference(text, merchant)
            if (onnxResult != null) {
                return@withContext onnxResult
            }
        }

        // Case C: Fallback to Intent & Tense Analyzer
        return@withContext intentFallback(text, merchant)
    }

    private suspend fun runTransformerInference(text: String, merchant: String): AiClassificationResult? {
        val session = getOrLoadSession() ?: return null
        val tok = tokenizer ?: return null

        val combinedInput = if (merchant.isNotBlank() && merchant != "Unknown Merchant") {
            "$text $merchant"
        } else {
            text
        }

        val (inputIds, attentionMask) = tok.encode(combinedInput)
        val shape = longArrayOf(1, 128)

        var inputIdsTensor: OnnxTensor? = null
        var attentionMaskTensor: OnnxTensor? = null

        return try {
            inputIdsTensor = OnnxTensor.createTensor(ortEnv, LongBuffer.wrap(inputIds), shape)
            attentionMaskTensor = OnnxTensor.createTensor(ortEnv, LongBuffer.wrap(attentionMask), shape)

            val inputNames = session.inputNames
            val firstInputName = inputNames.firstOrNull() ?: "input_ids"
            val secondInputName = inputNames.elementAtOrNull(1) ?: "attention_mask"

            val inputs = mapOf(
                firstInputName to inputIdsTensor,
                secondInputName to attentionMaskTensor
            )

            session.run(inputs).use { result ->
                val authLogits = extractLogits(result, "authenticity_logits", 0)
                val authProbs = softmax(authLogits)
                val topAuthIdx = authProbs.indices.maxByOrNull { authProbs[it] } ?: 0
                val authLabel = authenticityLabels.getOrElse(topAuthIdx) { AuthenticityLabel.REAL_TRANSACTION }
                val authConfidence = authProbs.getOrElse(topAuthIdx) { 0f }

                val catLogits = extractLogits(result, "category_logits", 1)
                val catProbs = softmax(catLogits)
                val topCatIdx = catProbs.indices.maxByOrNull { catProbs[it] } ?: (categoryLabels.size - 1)
                val catLabel = categoryLabels.getOrElse(topCatIdx) { "general" }
                val catConfidence = catProbs.getOrElse(topCatIdx) { 0f }

                val impliedType = if (catLabel == "income") TransactionType.CREDIT else TransactionType.DEBIT

                val fallback = intentFallback(text, merchant)
                val finalAuthLabel = if (fallback.authenticityLabel != AuthenticityLabel.REAL_TRANSACTION) {
                    fallback.authenticityLabel
                } else {
                    authLabel
                }

                AiClassificationResult(
                    authenticityLabel = finalAuthLabel,
                    authenticityConfidence = if (finalAuthLabel != authLabel) 0.95f else authConfidence,
                    categoryId = catLabel,
                    categoryConfidence = catConfidence,
                    impliedTransactionType = impliedType,
                    ranByAi = true
                )
            }
        } catch (e: Exception) {
            null
        } finally {
            runCatching { inputIdsTensor?.close() }
            runCatching { attentionMaskTensor?.close() }
        }
    }

    private fun intentFallback(text: String, merchant: String): AiClassificationResult {
        val lower = text.lowercase()

        val isReminder = lower.containsAny(
            "reminder", "due on", "due date", "payment due", "amount due", "minimum due",
            "min due", "total due", "overdue", "bill generated", "bill is due", "upcoming payment",
            "scheduled for", "will be debited", "will be deducted", "to be debited", "to be paid",
            "shall be debited", "is scheduled to", "maintain sufficient balance", "keep balance",
            "keep sufficient balance", "recharge now", "recharge with", "pack expiring",
            "validity expiring", "plan expires", "collect request", "requested money",
            "approve request", "mandate created", "mandate scheduled", "autopay scheduled",
            "standing instruction", "pay before", "pay by"
        )

        val isSpam = lower.containsAny(
            "pre-approved", "pre approved", "pre-qualified", "loan offer", "apply now",
            "credit limit", "congratulations", "win cash", "voucher", "cashback of", "flat rs",
            "flat ₹", "rummy", "dream11", "lucky winner", "claim reward", "bonus points"
        )

        val authLabel = when {
            isReminder -> AuthenticityLabel.PAYMENT_REMINDER
            isSpam -> AuthenticityLabel.SPAM_PROMO
            else -> AuthenticityLabel.REAL_TRANSACTION
        }

        return AiClassificationResult(
            authenticityLabel = authLabel,
            authenticityConfidence = 0.92f,
            categoryId = "general",
            categoryConfidence = 0.60f,
            impliedTransactionType = TransactionType.DEBIT,
            ranByAi = false
        )
    }

    private fun tokenizeSimple(text: String): Set<String> {
        return text.lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 3 }
            .toSet()
    }

    private fun computeJaccardSimilarity(set1: Set<String>, set2: Set<String>): Float {
        if (set1.isEmpty() || set2.isEmpty()) return 0f
        val intersection = set1.intersect(set2).size
        val union = set1.union(set2).size
        return if (union == 0) 0f else intersection.toFloat() / union.toFloat()
    }

    private fun String.containsAny(vararg words: String): Boolean =
        words.any { this.contains(it, ignoreCase = true) }

    private fun extractLogits(result: OrtSession.Result, preferredName: String, fallbackIndex: Int): FloatArray {
        val onnxVal: OnnxValue? = runCatching { result.get(preferredName).orElse(null) }.getOrNull()
            ?: runCatching { result.get(fallbackIndex) }.getOrNull()

        if (onnxVal is OnnxTensor) {
            val value = onnxVal.value
            if (value is Array<*> && value.isNotEmpty()) {
                val first = value[0]
                if (first is FloatArray) return first
                if (first is Array<*> && first.isNotEmpty()) {
                    val second = first[0]
                    if (second is FloatArray) return second
                }
            }
        }
        return FloatArray(3) { 0f }
    }

    private fun softmax(logits: FloatArray): FloatArray {
        if (logits.isEmpty()) return FloatArray(0)
        val maxLogit = logits.maxOrNull() ?: 0f
        val exps = logits.map { exp((it - maxLogit).toDouble()).toFloat() }
        val sumExp = exps.sum()
        return if (sumExp > 0f) exps.map { it / sumExp }.toFloatArray() else FloatArray(logits.size) { 1f / logits.size }
    }

    private suspend fun getOrLoadSession(): OrtSession? = withContext(ioDispatcher) {
        mutex.withLock {
            if (ortSession != null && tokenizer != null) {
                return@withLock ortSession
            }
            if (!modelStore.isTransformerReady()) {
                return@withLock null
            }
            try {
                val sessionOptions = OrtSession.SessionOptions().apply {
                    setOptimizationLevel(OrtSession.SessionOptions.OptLevel.BASIC_OPT)
                    setIntraOpNumThreads(2)
                }
                ortSession = ortEnv.createSession(modelStore.modelFile.absolutePath, sessionOptions)
                tokenizer = WordPieceTokenizer(modelStore.vocabFile)
                ortSession
            } catch (e: Exception) {
                ortSession = null
                tokenizer = null
                null
            }
        }
    }
}
