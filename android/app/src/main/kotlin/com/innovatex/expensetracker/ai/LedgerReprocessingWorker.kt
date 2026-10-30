package com.innovatex.expensetracker.ai

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.innovatex.expensetracker.MainActivity
import com.innovatex.expensetracker.data.local.PreferencesDataStore
import com.innovatex.expensetracker.di.IoDispatcher
import com.innovatex.expensetracker.domain.model.MerchantRule
import com.innovatex.expensetracker.domain.model.AuthenticityLabel
import com.innovatex.expensetracker.domain.model.TransactionCursor
import com.innovatex.expensetracker.domain.repository.CategoryRepository
import com.innovatex.expensetracker.domain.repository.TransactionRepository
import com.innovatex.expensetracker.ingestion.IngestionPipeline
import com.innovatex.expensetracker.widget.ExpenseWidgetSyncManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/** Checkpointed ledger reclassification that survives the dashboard leaving the foreground. */
@HiltWorker
class LedgerReprocessingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val localAiClassifier: LocalAiClassifier,
    private val aiDuplicateReview: AiDuplicateReview,
    private val preferences: PreferencesDataStore,
    private val checkpointStore: LedgerReprocessingCheckpointStore,
    private val widgetSyncManager: ExpenseWidgetSyncManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(ioDispatcher) {
        try {
            val savedCheckpoint = checkpointStore.read(id.toString())
            var checkpoint: LedgerReprocessingCheckpoint
            if (savedCheckpoint == null) {
                val total = transactionRepository.countTransactionsForReprocessing()
                val through = transactionRepository.latestTransactionCursor()
                if (through == null || total == 0) {
                    return@withContext Result.success(workDataOf(KEY_PROCESSED to 0, KEY_UPDATED to 0, KEY_DEDUPLICATED to 0))
                }
                checkpoint = LedgerReprocessingCheckpoint(
                    workId = id.toString(),
                    total = total,
                    processed = 0,
                    updatedCount = 0,
                    lastCursor = null,
                    throughCursor = through,
                    deduplicating = false
                )
                checkpointStore.save(checkpoint)
            } else {
                checkpoint = savedCheckpoint
            }

            val userRules = categoryRepository.getAllRules().first().mapNotNull(::compileRule)
            updateForeground(
                checkpoint,
                if (checkpoint.deduplicating) STATUS_DEDUPLICATING else STATUS_REVERIFYING
            )

            while (!checkpoint.deduplicating) {
                ensureActive()
                val batch = transactionRepository.getTransactionsForReprocessing(
                    after = checkpoint.lastCursor,
                    through = checkpoint.throughCursor,
                    limit = BATCH_SIZE
                )
                if (batch.isEmpty()) {
                    checkpoint = checkpoint.copy(deduplicating = true)
                    checkpointStore.save(checkpoint)
                    updateForeground(checkpoint, STATUS_DEDUPLICATING)
                    break
                }

                for (transaction in batch) {
                    ensureActive()
                    val aiResult = try {
                        localAiClassifier.classify(
                            text = transaction.rawSnippet?.ifBlank { null } ?: transaction.merchantClean,
                            merchant = transaction.merchantClean,
                            sender = transaction.sender
                        )
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        null
                    }

                    val hasUserRule = userRules.any { it.matches(transaction.merchantClean) }
                    val newCategory = if (!hasUserRule && aiResult?.categoryId != null &&
                        (aiResult.categoryConfidence ?: 0f) >= IngestionPipeline.AI_CATEGORY_CONFIDENCE_THRESHOLD
                    ) aiResult.categoryId else transaction.categoryId

                    // Reclassification must not erase an explicit self-transfer label.
                    val classification = if (
                        (transaction.isSelfTransfer && aiResult?.authenticityLabel != AuthenticityLabel.SELF_TRANSFER) ||
                        (transaction.userOverrideValid == false &&
                            transaction.aiClassification?.authenticityLabel == AuthenticityLabel.DUPLICATE)
                    ) transaction.aiClassification else aiResult ?: transaction.aiClassification
                    val enriched = transaction.copy(
                        categoryId = newCategory,
                        aiClassification = classification
                    )
                    try {
                        transactionRepository.updateTransaction(enriched)
                        checkpoint = checkpoint.copy(updatedCount = checkpoint.updatedCount + 1)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        // Keep processing other rows; a later verification run can retry this record.
                    }
                    checkpoint = checkpoint.copy(
                        processed = (checkpoint.processed + 1).coerceAtMost(checkpoint.total),
                        lastCursor = TransactionCursor(transaction.timestamp, transaction.id)
                    )
                }

                // Persist the cursor per batch. A process interruption may repeat at most one batch;
                // updating the same transaction again is safe and keeps storage writes bounded.
                checkpointStore.save(checkpoint)
                updateForeground(checkpoint, STATUS_REVERIFYING)
            }

            ensureActive()
            val mergedCount = transactionRepository.purgeDuplicates()
            val markedInvalidCount = transactionRepository.markLikelyDuplicateTransactionsInvalid()
            val aiMarkedCount = aiDuplicateReview.reviewUncertainDuplicates()
            val deduplicatedCount = mergedCount + markedInvalidCount + aiMarkedCount
            checkpointStore.clear(id.toString())
            widgetSyncManager.notifyDataChanged()
            notifyCompletionIfAllowed()
            Result.success(
                workDataOf(
                    KEY_PROCESSED to checkpoint.processed,
                    KEY_UPDATED to checkpoint.updatedCount,
                    KEY_DEDUPLICATED to deduplicatedCount
                )
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            android.util.Log.e(TAG, "Ledger reprocessing failed; WorkManager will retry", error)
            Result.retry()
        }
    }

    private fun ensureActive() {
        if (isStopped) throw CancellationException("Ledger reprocessing was stopped")
    }

    private suspend fun updateForeground(checkpoint: LedgerReprocessingCheckpoint, status: String) {
        val notificationsEnabled = shouldShowNotifications()
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(NOTIFICATION_CHANNEL_ID, "AI and ledger processing", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val percent = if (checkpoint.total == 0) 0 else (checkpoint.processed * 100 / checkpoint.total).coerceIn(0, 99)
        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Apex Expense Tracker")
            .setContentText(if (notificationsEnabled) status else "Ledger verification is running in the background")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, if (notificationsEnabled) percent else 0, !notificationsEnabled)
            .setOngoing(true)
            .setSilent(true)
            .build()
        val foregroundInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
        setForeground(foregroundInfo)
        setProgress(
            workDataOf(
                KEY_PROCESSED to checkpoint.processed,
                KEY_TOTAL to checkpoint.total,
                KEY_STATUS to status,
                KEY_DEDUPLICATING to checkpoint.deduplicating
            )
        )
    }

    private suspend fun shouldShowNotifications(): Boolean {
        if (!preferences.appSettingsFlow.first().backgroundAiNotificationsEnabled) return false
        if (!NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (manager?.getNotificationChannel(NOTIFICATION_CHANNEL_ID)?.importance == NotificationManager.IMPORTANCE_NONE) return false
        }
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    private suspend fun notifyCompletionIfAllowed() {
        if (!shouldShowNotifications()) return
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            COMPLETION_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Ledger verification complete")
            .setContentText("Existing transactions were verified and likely duplicates were marked and excluded from totals.")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSilent(true)
            .build()
        runCatching {
            NotificationManagerCompat.from(applicationContext).notify(COMPLETION_NOTIFICATION_ID, notification)
        }
    }

    private fun compileRule(rule: MerchantRule): CompiledRule? = if (rule.isRegex) {
        runCatching { Regex(rule.pattern, RegexOption.IGNORE_CASE) }.getOrNull()?.let { CompiledRule(regex = it) }
    } else {
        CompiledRule(literal = rule.pattern)
    }

    private data class CompiledRule(val regex: Regex? = null, val literal: String? = null) {
        fun matches(merchant: String): Boolean = when {
            regex != null -> regex.containsMatchIn(merchant)
            literal != null -> merchant.contains(literal, ignoreCase = true)
            else -> false
        }
    }

    companion object {
        const val WORK_NAME = "ledger_reprocessing_worker"
        const val KEY_PROCESSED = "processed"
        const val KEY_TOTAL = "total"
        const val KEY_UPDATED = "updated"
        const val KEY_DEDUPLICATED = "deduplicated"
        const val KEY_STATUS = "status"
        const val KEY_DEDUPLICATING = "deduplicating"
        private const val TAG = "LedgerReprocessing"
        private const val NOTIFICATION_ID = 2011
        private const val COMPLETION_NOTIFICATION_ID = 2012
        private const val NOTIFICATION_CHANNEL_ID = "ai_model_download_channel"
        private const val BATCH_SIZE = 20
        private const val STATUS_REVERIFYING = "Re-verifying existing transactions"
        private const val STATUS_DEDUPLICATING = "Checking for duplicate transaction alerts"

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<LedgerReprocessingWorker>()
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .addTag(WORK_NAME)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
        }

        fun cancel(context: Context) = WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
