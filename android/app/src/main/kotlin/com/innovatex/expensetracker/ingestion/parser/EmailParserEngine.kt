package com.innovatex.expensetracker.ingestion.parser

import com.innovatex.expensetracker.domain.model.SourceChannel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmailParserEngine @Inject constructor(
    private val smsParserEngine: SmsParserEngine
) {
    private val supportedEmailPackages = setOf(
        "com.google.android.gm",                 // Gmail
        "com.microsoft.office.outlook",          // Outlook
        "com.yahoo.mobile.client.android.mail",  // Yahoo Mail
        "com.android.email",                     // Stock AOSP Email
        "com.samsung.android.email.provider"     // Samsung Email
    )

    fun isEmailPackage(packageName: String): Boolean =
        supportedEmailPackages.contains(packageName)

    fun parseEmailNotification(
        packageName: String,
        title: String?,
        text: String?,
        bigText: String?,
        timestamp: Long = System.currentTimeMillis()
    ): ParsedTransactionCandidate? {
        if (!isEmailPackage(packageName)) return null

        // Combine title and body text for holistic parsing
        val combinedContent = buildString {
            if (!title.isNullOrBlank()) append(title).append(". ")
            if (!bigText.isNullOrBlank()) append(bigText)
            else if (!text.isNullOrBlank()) append(text)
        }

        if (combinedContent.isBlank()) return null

        return smsParserEngine.parse(
            body = combinedContent,
            receivedTimestamp = timestamp,
            sourceChannel = SourceChannel.EMAIL
        )
    }
}
