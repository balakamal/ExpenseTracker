package com.innovatex.spendwell.integrations.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.innovatex.spendwell.domain.model.SourceType
import com.innovatex.spendwell.transactionengine.TransactionEngine
import com.innovatex.spendwell.transactionengine.model.EngineResult
import com.innovatex.spendwell.transactionengine.model.RawSignal
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class SmsHistoryImportResult(
    val scanned: Int,
    val imported: Int,
    val needsReview: Int,
    val alreadyPresentOrIgnored: Int
)

@Singleton
class SmsHistoryImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transactionEngine: TransactionEngine
) {
    /**
     * Initial sync: imports only recent transactions (default 30 days) to prevent
     * importing years of stale promotional clutter and ensure instant startup.
     */
    suspend fun importRecentHistory(days: Int = 30): SmsHistoryImportResult {
        val now = System.currentTimeMillis()
        val startTime = now - (days.toLong() * 24 * 60 * 60 * 1000L)
        return importHistoryForRange(startTime, now)
    }

    /**
     * Lazy loading on demand: queries Telephony SMS ContentProvider for a targeted
     * time window [startTime, endTime] when user selects or scrolls to an older month.
     */
    suspend fun importHistoryForRange(
        startTime: Long,
        endTime: Long,
        maxMessages: Int = 1_000
    ): SmsHistoryImportResult = withContext(Dispatchers.IO) {
        check(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
                    PackageManager.PERMISSION_GRANTED
        ) { "SMS read permission has not been granted." }

        var scanned = 0
        var imported = 0
        var needsReview = 0
        var ignored = 0

        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        val selection = "${Telephony.Sms.DATE} BETWEEN ? AND ?"
        val selectionArgs = arrayOf(startTime.toString(), endTime.toString())

        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${Telephony.Sms.DATE} DESC"
        )?.use { cursor ->
            val addressIndex = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIndex = cursor.getColumnIndex(Telephony.Sms.BODY)
            val dateIndex = cursor.getColumnIndex(Telephony.Sms.DATE)

            if (addressIndex < 0 || bodyIndex < 0 || dateIndex < 0) {
                throw IllegalStateException("The SMS provider returned an unsupported result.")
            }

            val recentMessages = mutableListOf<Triple<String, String, Long>>()
            while (cursor.moveToNext() && recentMessages.size < maxMessages) {
                recentMessages += Triple(
                    cursor.getString(addressIndex).orEmpty(),
                    cursor.getString(bodyIndex).orEmpty(),
                    cursor.getLong(dateIndex)
                )
            }

            // Process oldest-to-newest so refund/reversal linking sees prior transactions.
            for ((sender, body, timestamp) in recentMessages.asReversed()) {
                scanned++
                if (sender.isBlank() || body.isBlank()) {
                    ignored++
                    continue
                }
                when (
                    val result = transactionEngine.processSignal(
                        RawSignal(
                            sourceType = SourceType.SMS,
                            senderAddress = sender,
                            rawText = body,
                            timestamp = timestamp
                        )
                    )
                ) {
                    is EngineResult.Success -> if (result.isNewTransaction) imported++ else ignored++
                    is EngineResult.FlaggedForReview -> needsReview++
                    is EngineResult.Dropped -> ignored++
                }
            }
        } ?: throw IllegalStateException("Unable to read SMS history on this device.")

        SmsHistoryImportResult(
            scanned = scanned,
            imported = imported,
            needsReview = needsReview,
            alreadyPresentOrIgnored = ignored
        )
    }
}
