package com.innovatex.spendwell.transactionengine

import com.innovatex.spendwell.core.DispatcherProvider
import com.innovatex.spendwell.database.SpendwellDatabase
import com.innovatex.spendwell.transactionengine.model.DropReason
import com.innovatex.spendwell.transactionengine.model.EngineResult
import com.innovatex.spendwell.transactionengine.model.RawSignal
import com.innovatex.spendwell.transactionengine.stages.DuplicateResolution
import com.innovatex.spendwell.transactionengine.stages.Stage10TransactionLifecycleResolution
import com.innovatex.spendwell.transactionengine.stages.Stage11CategoryClassification
import com.innovatex.spendwell.transactionengine.stages.Stage12LedgerPersistence
import com.innovatex.spendwell.transactionengine.stages.Stage3MessageNormalization
import com.innovatex.spendwell.transactionengine.stages.Stage4SpamAndReminderFilter
import com.innovatex.spendwell.transactionengine.stages.Stage5TransactionDetection
import com.innovatex.spendwell.transactionengine.stages.Stage6StructuredExtraction
import com.innovatex.spendwell.transactionengine.stages.Stage9DuplicateCandidateDetection
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionEngine @Inject constructor(
    private val database: SpendwellDatabase,
    private val dispatchers: DispatcherProvider
) {

    private val engineMutex = Mutex()

    /**
     * Executes the complete 12-Stage Transaction Processing Pipeline:
     * 1. Source Ingestion
     * 2. Source Identification
     * 3. Message Normalization
     * 4. Spam & Reminder Classification
     * 5. Transaction Detection
     * 6. Structured Information Extraction
     * 7. Amount & Direction Validation
     * 8. Merchant Normalization
     * 9. Duplicate Candidate Detection
     * 10. Transaction Lifecycle Resolution
     * 11. Category Classification
     * 12. Ledger Persistence
     */
    suspend fun processSignal(signal: RawSignal): EngineResult = withContext(dispatchers.io) {
        // Stage 1 & 3: Ingestion & Message Normalization (includes Stage 2 Sender Identification)
        val normalized = Stage3MessageNormalization.normalize(signal)

        // Stage 4: Spam and Payment Reminder Filter
        val dropReason = Stage4SpamAndReminderFilter.filter(normalized)
        if (dropReason != null) {
            return@withContext EngineResult.Dropped(
                dropReason = dropReason,
                message = "Filtered at Stage 4: $dropReason"
            )
        }

        // Stage 5: Transaction Detection
        if (!Stage5TransactionDetection.isTransaction(normalized.normalizedText)) {
            return@withContext EngineResult.Dropped(
                dropReason = DropReason.NON_FINANCIAL_MESSAGE,
                message = "No financial transaction event detected"
            )
        }

        // Stage 6, 7 & 8: Structured Extraction, Direction Validation & Merchant Normalization
        val extractedInfo = Stage6StructuredExtraction.extract(normalized)
            ?: return@withContext EngineResult.Dropped(
                dropReason = DropReason.INVALID_AMOUNT_OR_FORMAT,
                message = "Could not parse monetary amount or account structure"
            )

        // Concurrency Lock for deterministic deduplication and database persistence
        engineMutex.withLock {
            // Stage 9: Duplicate Candidate Detection
            val duplicateResolution = Stage9DuplicateCandidateDetection.evaluate(database, extractedInfo)

            // Stage 10: Transaction Lifecycle Resolution (Refund / Reversal linking)
            val lifecycleResolution = Stage10TransactionLifecycleResolution.resolveLifecycle(database, extractedInfo)

            // Stage 11: Category Classification
            val (categoryId, confidence) = Stage11CategoryClassification.classify(extractedInfo)

            // Stage 12: Ledger Persistence
            Stage12LedgerPersistence.persist(
                db = database,
                info = extractedInfo,
                normalized = normalized,
                duplicateResolution = duplicateResolution,
                lifecycleResolution = lifecycleResolution,
                categoryId = categoryId,
                confidence = confidence
            )
        }
    }
}
