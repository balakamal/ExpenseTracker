package com.innovatex.expensetracker.ai

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OnnxValue
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import com.innovatex.expensetracker.data.local.dao.UserFeedbackDao
import com.innovatex.expensetracker.di.IoDispatcher
import com.innovatex.expensetracker.domain.model.AiClassificationResult
import com.innovatex.expensetracker.domain.model.AuthenticityLabel
import com.innovatex.expensetracker.domain.model.TransactionType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
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

        // --- Layer 3: ONNX Neural Model Inference ---
        val session = getOrLoadSession() ?: return@withContext heuristicFallback(text, merchant)
        val tok = tokenizer ?: return@withContext heuristicFallback(text, merchant)

        val combinedInput = if (merchant.isNotBlank() && merchant != "Unknown Merchant") {
            "$text $merchant"
        } else {
            text
        }

        val (inputIds, attentionMask) = tok.encode(combinedInput)
        val shape = longArrayOf(1, 128)

        var inputIdsTensor: OnnxTensor? = null
        var attentionMaskTensor: OnnxTensor? = null

        try {
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
                // Head 1: Authenticity
                val authLogits = extractLogits(result, "authenticity_logits", 0)
                val authProbs = softmax(authLogits)
                val topAuthIdx = authProbs.indices.maxByOrNull { authProbs[it] } ?: 0
                val authLabel = authenticityLabels.getOrElse(topAuthIdx) { AuthenticityLabel.REAL_TRANSACTION }
                val authConfidence = authProbs.getOrElse(topAuthIdx) { 0f }

                // Head 2: Category
                val catLogits = extractLogits(result, "category_logits", 1)
                val catProbs = softmax(catLogits)
                val topCatIdx = catProbs.indices.maxByOrNull { catProbs[it] } ?: (categoryLabels.size - 1)
                val catLabel = categoryLabels.getOrElse(topCatIdx) { "general" }
                val catConfidence = catProbs.getOrElse(topCatIdx) { 0f }

                val impliedType = if (catLabel == "income") TransactionType.CREDIT else TransactionType.DEBIT

                val lower = text.lowercase()
                val finalAuthLabel = when {
                    lower.containsAny("loan", "pre-approved", "apply now", "credit limit", "congratulations", "win cash", "voucher", "cashback of", "flat rs", "flat ₹") ->
                        AuthenticityLabel.SPAM_PROMO
                    lower.containsAny("due on", "bill generated", "minimum due", "pay before", "payment due") ->
                        AuthenticityLabel.PAYMENT_REMINDER
                    else -> authLabel
                }

                AiClassificationResult(
                    authenticityLabel = finalAuthLabel,
                    authenticityConfidence = if (finalAuthLabel != authLabel) 0.90f else authConfidence,
                    categoryId = catLabel,
                    categoryConfidence = catConfidence,
                    impliedTransactionType = impliedType,
                    ranByAi = true
                )
            }
        } catch (e: Exception) {
            heuristicFallback(text, merchant)
        } finally {
            runCatching { inputIdsTensor?.close() }
            runCatching { attentionMaskTensor?.close() }
        }
    }

    private fun heuristicFallback(text: String, merchant: String): AiClassificationResult {
        val lower = text.lowercase()
        val authLabel = when {
            lower.containsAny("loan", "pre-approved", "apply now", "credit limit", "congratulations", "win cash", "voucher", "cashback of", "flat rs", "flat ₹") ->
                AuthenticityLabel.SPAM_PROMO
            lower.containsAny("due on", "bill generated", "minimum due", "pay before", "payment due") ->
                AuthenticityLabel.PAYMENT_REMINDER
            else -> AuthenticityLabel.REAL_TRANSACTION
        }

        return AiClassificationResult(
            authenticityLabel = authLabel,
            authenticityConfidence = 0.88f,
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
            val floatBuffer = runCatching { onnxVal.floatBuffer }.getOrNull()
            if (floatBuffer != null) {
                val array = FloatArray(floatBuffer.remaining())
                floatBuffer.get(array)
                return array
            }
            val raw = runCatching { onnxVal.getValue() }.getOrNull()
            if (raw is Array<*>) {
                val first = raw.firstOrNull()
                if (first is FloatArray) return first
            } else if (raw is FloatArray) {
                return raw
            }
        }
        return FloatArray(10) { 0f }
    }

    private suspend fun getOrLoadSession(): OrtSession? = mutex.withLock {
        if (ortSession != null) return ortSession
        if (!modelStore.isModelReady()) return null

        try {
            val sessionOptions = OrtSession.SessionOptions().apply {
                try {
                    addNnapi()
                } catch (_: Exception) {
                    setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
                }
            }

            ortSession = ortEnv.createSession(modelStore.modelFile.absolutePath, sessionOptions)
            tokenizer = WordPieceTokenizer(modelStore.vocabFile)
            ortSession
        } catch (e: Exception) {
            null
        }
    }

    private fun softmax(logits: FloatArray): FloatArray {
        if (logits.isEmpty()) return FloatArray(0)
        val maxLogit = logits.maxOrNull() ?: 0f
        val expScores = logits.map { exp((it - maxLogit).toDouble()).toFloat() }
        val sumExp = expScores.sum().coerceAtLeast(1e-6f)
        return expScores.map { it / sumExp }.toFloatArray()
    }

    fun release() {
        ortSession?.close()
        ortSession = null
        tokenizer = null
    }
}
