package com.innovatex.spendwell.integrations.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.innovatex.spendwell.domain.model.SourceType
import com.innovatex.spendwell.transactionengine.TransactionEngine
import com.innovatex.spendwell.transactionengine.model.RawSignal
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SmsBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var transactionEngine: TransactionEngine

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val pendingResult = goAsync()

        receiverScope.launch {
            try {
                // Group multipart SMS messages by originating address
                val combinedBySender = messages.groupBy { it.displayOriginatingAddress ?: it.originatingAddress ?: "UNKNOWN" }

                for ((sender, smsList) in combinedBySender) {
                    val fullBody = smsList.joinToString("") { it.displayMessageBody ?: it.messageBody ?: "" }
                    val timestamp = smsList.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()

                    val signal = RawSignal(
                        sourceType = SourceType.SMS,
                        senderAddress = sender,
                        rawText = fullBody,
                        timestamp = timestamp
                    )

                    transactionEngine.processSignal(signal)
                }
            } catch (e: Exception) {
                // Securely swallowed in production to prevent crashes
            } finally {
                pendingResult.finish()
            }
        }
    }
}
