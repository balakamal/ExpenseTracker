package com.innovatex.expensetracker.ingestion.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.innovatex.expensetracker.domain.model.SourceChannel
import com.innovatex.expensetracker.ingestion.IngestionPipeline
import com.innovatex.expensetracker.ingestion.parser.EmailParserEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class EmailNotificationListenerService : NotificationListenerService() {

    @Inject
    lateinit var emailParserEngine: EmailParserEngine

    @Inject
    lateinit var ingestionPipeline: IngestionPipeline

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return
        if (!emailParserEngine.isEmailPackage(packageName)) return

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val timestamp = sbn.postTime

        val candidate = emailParserEngine.parseEmailNotification(
            packageName = packageName,
            title = title,
            text = text,
            bigText = bigText,
            timestamp = timestamp
        ) ?: return

        val combinedContent = buildString {
            if (!title.isNullOrBlank()) append(title).append(". ")
            if (!bigText.isNullOrBlank()) append(bigText)
            else if (!text.isNullOrBlank()) append(text)
        }

        serviceScope.launch {
            ingestionPipeline.processIncomingMessage(
                body = combinedContent,
                timestamp = timestamp,
                source = SourceChannel.EMAIL,
                sender = title
            )
        }
    }
}
