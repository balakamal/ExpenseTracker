package com.innovatex.expensetracker.ingestion.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import com.innovatex.expensetracker.data.local.PreferencesDataStore
import com.innovatex.expensetracker.domain.model.SourceChannel
import com.innovatex.expensetracker.domain.repository.TransactionRepository
import com.innovatex.expensetracker.domain.usecase.DeduplicateTransactionUseCase
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
    lateinit var deduplicateTransactionUseCase: DeduplicateTransactionUseCase

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
        val sender = messages[0].originatingAddress

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = preferencesDataStore.appSettingsFlow.first()
                if (settings.smsTrackingEnabled) {
                    if (preferencesDataStore.isSpamSender(sender)) {
                        return@launch
                    }
                    val parsedTxn = parseTransactionUseCase.parse(
                        body = fullBody,
                        timestamp = timestamp,
                        source = SourceChannel.SMS,
                        sender = sender
                    )
                    if (parsedTxn != null) {
                        deduplicateTransactionUseCase(parsedTxn)
                        widgetSyncManager.notifyDataChanged(context)
                    }
                }
            } catch (e: Exception) {
                Log.e("SmsReceiver", "Error processing incoming SMS", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

@Singleton
class HistoricalSmsScanner @Inject constructor(
    private val parseTransactionUseCase: ParseTransactionUseCase,
    private val deduplicateTransactionUseCase: DeduplicateTransactionUseCase,
    private val transactionRepository: TransactionRepository,
    private val preferencesDataStore: PreferencesDataStore,
    private val widgetSyncManager: ExpenseWidgetSyncManager
) {
    suspend fun scanLast30Days(context: Context): Int = withContext(Dispatchers.IO) {
        // Expand scan window to 90 days to capture complete expense history
        val scanWindowDays = 90L
        val scanStartTimestamp = System.currentTimeMillis() - (scanWindowDays * 24 * 60 * 60 * 1000L)
        val projection = arrayOf("_id", "address", "body", "date")

        var count = 0

        // Helper function to query a given URI
        suspend fun querySmsUri(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
            var scanned = 0
            try {
                context.contentResolver.query(
                    uri,
                    projection,
                    selection,
                    selectionArgs,
                    "date DESC"
                )?.use { cursor ->
                    val bodyIndex = cursor.getColumnIndex("body")
                    val dateIndex = cursor.getColumnIndex("date")
                    val addressIndex = cursor.getColumnIndex("address")

                    if (bodyIndex >= 0 && dateIndex >= 0) {
                        while (cursor.moveToNext()) {
                            val body = cursor.getString(bodyIndex) ?: continue
                            val date = cursor.getLong(dateIndex)
                            val sender = if (addressIndex >= 0) cursor.getString(addressIndex) else null
                            if (preferencesDataStore.isSpamSender(sender)) {
                                continue
                            }

                            val parsed = parseTransactionUseCase.parse(
                                body = body,
                                timestamp = date,
                                source = SourceChannel.SMS,
                                sender = sender
                            )
                            if (parsed != null) {
                                deduplicateTransactionUseCase(parsed)
                                scanned++
                            }
                        }
                    }
                }
            } catch (err: Exception) {
                Log.e("HistoricalSmsScanner", "Error querying SMS uri: $uri", err)
            }
            return scanned
        }

        // Try primary inbox URI first
        count += querySmsUri(
            uri = Uri.parse("content://sms/inbox"),
            selection = "date >= ?",
            selectionArgs = arrayOf(scanStartTimestamp.toString())
        )

        // Fallback to general sms content provider if inbox was empty
        if (count == 0) {
            count += querySmsUri(
                uri = Telephony.Sms.CONTENT_URI,
                selection = "type = 1 AND date >= ?",
                selectionArgs = arrayOf(scanStartTimestamp.toString())
            )
        }

        // Clean up any historical duplicate entries in the database
        transactionRepository.purgeDuplicates()

        preferencesDataStore.setHistoricalScanCompleted(true)
        if (count > 0) {
            widgetSyncManager.notifyDataChanged(context)
        }
        count
    }
}
