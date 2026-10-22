package com.innovatex.expensetracker.ingestion.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Telephony
import com.innovatex.expensetracker.data.local.PreferencesDataStore
import com.innovatex.expensetracker.domain.model.SourceChannel
import com.innovatex.expensetracker.domain.repository.TransactionRepository
import com.innovatex.expensetracker.domain.usecase.ParseTransactionUseCase
import com.innovatex.expensetracker.widget.ExpenseWidgetSyncManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@AndroidEntryPoint
class SmsBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var parseTransactionUseCase: ParseTransactionUseCase

    @Inject
    lateinit var transactionRepository: TransactionRepository

    @Inject
    lateinit var preferencesDataStore: PreferencesDataStore

    @Inject
    lateinit var widgetSyncManager: ExpenseWidgetSyncManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        // Concatenate multipart SMS segments
        val fullBody = messages.joinToString("") { it.displayMessageBody ?: "" }
        val timestamp = messages[0].timestampMillis

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = preferencesDataStore.appSettingsFlow.first()
                if (settings.smsTrackingEnabled) {
                    val parsedTxn = parseTransactionUseCase.parse(
                        body = fullBody,
                        timestamp = timestamp,
                        source = SourceChannel.SMS
                    )
                    if (parsedTxn != null) {
                        transactionRepository.saveTransaction(parsedTxn)
                        widgetSyncManager.notifyDataChanged(context)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

@Singleton
class HistoricalSmsScanner @Inject constructor(
    private val parseTransactionUseCase: ParseTransactionUseCase,
    private val transactionRepository: TransactionRepository,
    private val preferencesDataStore: PreferencesDataStore,
    private val widgetSyncManager: ExpenseWidgetSyncManager
) {
    suspend fun scanLast30Days(context: Context): Int = withContext(Dispatchers.IO) {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        val uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf("_id", "address", "body", "date")
        val selection = "date >= ?"
        val selectionArgs = arrayOf(thirtyDaysAgo.toString())

        var count = 0
        runCatching {
            context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                "date DESC"
            )?.use { cursor ->
                val bodyIndex = cursor.getColumnIndex("body")
                val dateIndex = cursor.getColumnIndex("date")

                while (cursor.moveToNext()) {
                    val body = cursor.getString(bodyIndex) ?: continue
                    val date = cursor.getLong(dateIndex)

                    val parsed = parseTransactionUseCase.parse(
                        body = body,
                        timestamp = date,
                        source = SourceChannel.SMS
                    )
                    if (parsed != null) {
                        transactionRepository.saveTransaction(parsed)
                        count++
                    }
                }
            }
        }

        preferencesDataStore.setHistoricalScanCompleted(true)
        if (count > 0) {
            widgetSyncManager.notifyDataChanged(context)
        }
        count
    }
}
