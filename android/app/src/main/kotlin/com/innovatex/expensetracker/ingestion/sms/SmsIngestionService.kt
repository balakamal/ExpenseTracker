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
import com.innovatex.expensetracker.ingestion.IngestionPipeline
import com.innovatex.expensetracker.widget.ExpenseWidgetSyncManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@AndroidEntryPoint
class SmsBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var ingestionPipeline: IngestionPipeline

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
                ingestionPipeline.processIncomingMessage(
                    body = fullBody,
                    timestamp = timestamp,
                    source = SourceChannel.SMS,
                    sender = sender
                )
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
    private val ingestionPipeline: IngestionPipeline,
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
                            try {
                                val body = cursor.getString(bodyIndex) ?: continue
                                val date = cursor.getLong(dateIndex)
                                val sender = if (addressIndex >= 0) cursor.getString(addressIndex) else null
                                if (preferencesDataStore.isSpamSender(sender)) {
                                    continue
                                }

                                val res = ingestionPipeline.processIncomingMessage(
                                    body = body,
                                    timestamp = date,
                                    source = SourceChannel.SMS,
                                    sender = sender
                                )
                                if (res != null) {
                                    scanned++
                                }
                            } catch (msgErr: Exception) {
                                Log.e("HistoricalSmsScanner", "Error processing individual historical SMS message", msgErr)
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
        val mergedDuplicateCount = transactionRepository.purgeDuplicates()
        val invalidatedDuplicateCount = transactionRepository.markLikelyDuplicateTransactionsInvalid()

        preferencesDataStore.setHistoricalScanCompleted(true)
        if (count > 0 || mergedDuplicateCount + invalidatedDuplicateCount > 0) {
            widgetSyncManager.notifyDataChanged(context)
        }
        count
    }
}
