package com.innovatex.expensetracker.ingestion

import android.content.Context
import com.innovatex.expensetracker.data.local.PreferencesDataStore
import com.innovatex.expensetracker.domain.model.SourceChannel
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
    private val widgetSyncManager: ExpenseWidgetSyncManager
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

        val candidate = parseTransactionUseCase.parse(body, timestamp, source, sender) ?: return null

        val result = deduplicateTransactionUseCase(
            candidate = candidate,
            toleranceWindowMs = settings.deduplicationWindowMinutes * 60 * 1000L
        )

        widgetSyncManager.notifyDataChanged(context)
        return result
    }
}
