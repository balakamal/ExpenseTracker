package com.innovatex.expensetracker.ingestion

import android.content.Context
import com.innovatex.expensetracker.ai.LocalAiClassifier
import com.innovatex.expensetracker.data.local.PreferencesDataStore
import com.innovatex.expensetracker.domain.model.AuthenticityLabel
import com.innovatex.expensetracker.domain.model.SourceChannel
import com.innovatex.expensetracker.domain.repository.CategoryRepository
import com.innovatex.expensetracker.domain.usecase.DeduplicateTransactionUseCase
import com.innovatex.expensetracker.domain.usecase.DeduplicationResult
import com.innovatex.expensetracker.domain.usecase.ParseTransactionUseCase
import com.innovatex.expensetracker.widget.ExpenseWidgetSyncManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IngestionPipeline @Inject constructor(
    @ApplicationContext private val context: Context,
    private val parseTransactionUseCase: ParseTransactionUseCase,
    private val deduplicateTransactionUseCase: DeduplicateTransactionUseCase,
    private val preferencesDataStore: PreferencesDataStore,
    private val widgetSyncManager: ExpenseWidgetSyncManager,
    private val localAiClassifier: LocalAiClassifier,
    private val categoryRepository: CategoryRepository
) {
    suspend fun processIncomingMessage(
        body: String,
        timestamp: Long,
        source: SourceChannel,
        sender: String? = null
    ): DeduplicationResult? {
        val settings = preferencesDataStore.appSettingsFlow.first()
        if (source == SourceChannel.SMS && !settings.smsTrackingEnabled) return null
        if (source == SourceChannel.EMAIL && !settings.emailTrackingEnabled) return null

        // If sender is marked as spam by the user, ignore immediately
        if (preferencesDataStore.isSpamSender(sender)) {
            return null
        }

        // Step 1: Regex parse as hard safety gate
        val candidate = parseTransactionUseCase.parse(body, timestamp, source, sender) ?: return null

        // Step 2: AI Classification (runs if model downloaded and enabled)
        val aiResult = if (settings.aiClassificationEnabled) {
            localAiClassifier.classify(body, candidate.merchantClean)
        } else {
            null
        }

        // Step 3: Handle Authenticity (AI-based vs Local-based spam filtering)
        if (settings.aiSpamDetectionEnabled && aiResult != null) {
            if (aiResult.authenticityLabel == AuthenticityLabel.SPAM_PROMO &&
                aiResult.authenticityConfidence >= AI_SPAM_DROP_THRESHOLD
            ) {
                return null // High confidence promotional/spam alert dropped by AI
            }
            if (aiResult.authenticityLabel == AuthenticityLabel.PAYMENT_REMINDER &&
                aiResult.authenticityConfidence >= AI_REMINDER_DROP_THRESHOLD
            ) {
                return null // Prospective payment reminder dropped by AI
            }
        }

        // Step 4: Resolve Category and Type via Priority Chain:
        // Priority 1: User's explicit MerchantRule
        // Priority 2: AI Category (if confidence >= 0.75)
        // Priority 3: Regex keyword heuristics (already in candidate)
        var finalCategory = candidate.categoryId
        var finalType = candidate.type

        val userRules = runCatching { categoryRepository.getAllRules().first() }.getOrDefault(emptyList())
        val hasUserRule = userRules.any { rule ->
            if (rule.isRegex) {
                Regex(rule.pattern, RegexOption.IGNORE_CASE).containsMatchIn(candidate.merchantClean)
            } else {
                candidate.merchantClean.contains(rule.pattern, ignoreCase = true)
            }
        }

        if (!hasUserRule && aiResult?.categoryId != null &&
            (aiResult.categoryConfidence ?: 0f) >= AI_CATEGORY_CONFIDENCE_THRESHOLD
        ) {
            finalCategory = aiResult.categoryId
            finalType = aiResult.impliedTransactionType ?: candidate.type
        }

        val enrichedTransaction = candidate.copy(
            categoryId = finalCategory,
            type = finalType,
            aiClassification = aiResult
        )

        val result = deduplicateTransactionUseCase(
            candidate = enrichedTransaction,
            toleranceWindowMs = settings.deduplicationWindowMinutes * 60 * 1000L
        )

        widgetSyncManager.notifyDataChanged(context)
        return result
    }

    companion object {
        const val AI_SPAM_DROP_THRESHOLD = 0.85f
        const val AI_REMINDER_DROP_THRESHOLD = 0.80f
        const val AI_CATEGORY_CONFIDENCE_THRESHOLD = 0.75f
    }
}
