package com.innovatex.expensetracker.ai

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.innovatex.expensetracker.di.IoDispatcher
import com.innovatex.expensetracker.domain.model.AiClassificationResult
import com.innovatex.expensetracker.domain.model.AuthenticityLabel
import com.innovatex.expensetracker.domain.model.TransactionType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LlmClassifierEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelStore: AiModelStore,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private var llmInference: LlmInference? = null
    private val mutex = Mutex()
    private var isEngineFailed = false

    private fun getOrInitLlm(): LlmInference? {
        if (isEngineFailed || !modelStore.isGenerativeLlmReady()) return null
        if (llmInference != null) return llmInference

        val modelFile = modelStore.generativeLlmFile
        if (!modelStore.isValidMediaPipeBundle(modelFile)) {
            Log.e("LlmClassifierEngine", "Model file is not a valid MediaPipe GenAI bundle. Aborting native init to prevent crash.")
            isEngineFailed = true
            return null
        }

        return try {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .setMaxTokens(256)
                .build()
            val instance = LlmInference.createFromOptions(context, options)
            llmInference = instance
            instance
        } catch (t: Throwable) {
            Log.e("LlmClassifierEngine", "Failed to initialize MediaPipe LlmInference", t)
            isEngineFailed = true
            null
        }
    }

    suspend fun analyze(text: String, candidateMerchant: String? = null): AiClassificationResult? =
        withContext(ioDispatcher) {
            mutex.withLock {
                val engine = getOrInitLlm() ?: return@withContext null
                val prompt = buildPrompt(text)

                val response: String? = runCatching {
                    engine.generateResponse(prompt)
                }.onFailure { err ->
                    Log.e("LlmClassifierEngine", "Error generating response from LLM", err)
                }.getOrNull()

                if (response.isNullOrBlank()) return@withContext null

                parseLlmResponse(response, candidateMerchant)
            }
        }

    private fun buildPrompt(body: String): String {
        return """<|im_start|>system
You are an on-device personal financial assistant. Analyze notification messages to classify whether they represent an actual completed transaction, a payment reminder, or promotional spam. Always respond in valid JSON format only.<|im_end|>
<|im_start|>user
Analyze the following notification message:
"${body.trim()}"

Determine if this message represents money that was ALREADY debited or credited (an actual completed transaction), or if it is an upcoming bill, payment reminder, mandate schedule, loan offer, or promotional spam.

Output ONLY a valid JSON object in this exact format:
{
  "is_actual_transaction": true,
  "is_reminder": false,
  "transaction_type": "DEBIT",
  "merchant": "Merchant Name",
  "category": "food_dining",
  "reason": "Brief explanation"
}<|im_end|>
<|im_start|>assistant
"""
    }

    private fun parseLlmResponse(
        rawResponse: String,
        fallbackMerchant: String?
    ): AiClassificationResult? {
        return runCatching {
            // Find opening and closing curly braces to extract valid JSON
            val startIdx = rawResponse.indexOf('{')
            val endIdx = rawResponse.lastIndexOf('}')
            if (startIdx == -1 || endIdx == -1 || startIdx >= endIdx) {
                return null
            }

            val jsonStr = rawResponse.substring(startIdx, endIdx + 1)
            val json = JSONObject(jsonStr)

            val isActual = json.optBoolean("is_actual_transaction", false)
            val isReminder = json.optBoolean("is_reminder", false)
            val rawType = json.optString("transaction_type", "DEBIT").uppercase()
            val merchant = json.optString("merchant", "").ifBlank { fallbackMerchant ?: "Unknown Merchant" }
            val rawCategory = json.optString("category", "general").lowercase()

            val authLabel = when {
                isReminder -> AuthenticityLabel.PAYMENT_REMINDER
                isActual -> AuthenticityLabel.REAL_TRANSACTION
                else -> AuthenticityLabel.SPAM_PROMO
            }

            val transactionType = when (rawType) {
                "CREDIT" -> TransactionType.CREDIT
                else -> TransactionType.DEBIT
            }

            val normalizedCategory = when (rawCategory) {
                "food", "dining", "food_dining", "restaurant" -> "food_dining"
                "groceries", "grocery", "supermarket" -> "groceries"
                "transport", "transit", "fuel", "travel" -> "transport"
                "shopping", "retail", "ecommerce" -> "shopping"
                "utilities", "bill", "electricity", "water", "internet", "recharge" -> "utilities"
                "entertainment", "movie", "ott", "streaming" -> "entertainment"
                "health", "medical", "pharmacy" -> "health"
                "income", "salary", "refund" -> "income"
                else -> "general"
            }

            AiClassificationResult(
                authenticityLabel = authLabel,
                authenticityConfidence = 0.98f,
                categoryId = normalizedCategory,
                categoryConfidence = 0.95f,
                impliedTransactionType = transactionType,
                ranByAi = true
            )
        }.onFailure { err ->
            Log.w("LlmClassifierEngine", "Failed to parse LLM JSON: $rawResponse", err)
        }.getOrNull()
    }

    fun release() {
        runCatching {
            llmInference?.close()
            llmInference = null
            isEngineFailed = false
        }
    }
}
