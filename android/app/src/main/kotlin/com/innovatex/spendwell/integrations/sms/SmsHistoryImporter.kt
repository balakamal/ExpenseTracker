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
    suspend fun importRecentHistory(maxMessages: Int = 5_000): SmsHistoryImportResult =
        withContext(Dispatchers.IO) {
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

            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} ASC"
            )?.use { cursor ->
                val addressIndex = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIndex = cursor.getColumnIndex(Telephony.Sms.BODY)
                val dateIndex = cursor.getColumnIndex(Telephony.Sms.DATE)

                if (addressIndex < 0 || bodyIndex < 0 || dateIndex < 0) {
                    throw IllegalStateException("The SMS provider returned an unsupported result.")
                }

                while (cursor.moveToNext() && scanned < maxMessages) {
                    val sender = cursor.getString(addressIndex).orEmpty()
                    val body = cursor.getString(bodyIndex).orEmpty()
                    val timestamp = cursor.getLong(dateIndex)
                    scanned++

                    if (sender.isBlank() || body.isBlank()) {
                        ignored++
                        continue
                    }

                    when (
                        transactionEngine.processSignal(
                            RawSignal(
                                sourceType = SourceType.SMS,
                                senderAddress = sender,
                                rawText = body,
                                timestamp = timestamp
                            )
                        )
                    ) {
                        is EngineResult.Success -> imported++
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
