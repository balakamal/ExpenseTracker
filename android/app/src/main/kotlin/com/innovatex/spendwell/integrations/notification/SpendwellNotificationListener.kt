package com.innovatex.spendwell.integrations.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.innovatex.spendwell.domain.model.SourceType
import com.innovatex.spendwell.transactionengine.TransactionEngine
import com.innovatex.spendwell.transactionengine.model.RawSignal
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SpendwellNotificationListener : NotificationListenerService() {

    @Inject
    lateinit var transactionEngine: TransactionEngine

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private val MONITORED_PACKAGES = setOf(
            "com.google.android.apps.nbu.paisa.user", // Google Pay
            "com.phonepe.app",                        // PhonePe
            "net.one97.paytm",                        // Paytm
            "com.dreamplug.androidapp",               // CRED
            "com.hdfcbank.payzapp",                   // PayZapp
            "com.mgs.indusmobile",                    // IndusInd
            "com.msf.kbank.mobile",                   // Kotak
            "com.csam.icici.bank.imobile",            // iMobile
            "com.sbi.lotusintouch"                    // YONO SBI
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName
        if (!MONITORED_PACKAGES.contains(pkg)) return

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

        val combinedMessage = when {
            bigText.isNotBlank() -> "$title. $bigText"
            text.isNotBlank() -> "$title. $text"
            else -> title
        }

        if (combinedMessage.isBlank()) return

        serviceScope.launch {
            try {
                val signal = RawSignal(
                    sourceType = SourceType.NOTIFICATION,
                    senderAddress = pkg,
                    rawText = combinedMessage,
                    timestamp = sbn.postTime
                )
                transactionEngine.processSignal(signal)
            } catch (e: Exception) {
                // Safeguard against background crashes
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
