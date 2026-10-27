package com.innovatex.expensetracker.ingestion.parser

import com.innovatex.expensetracker.domain.model.SourceChannel
import java.util.regex.Pattern
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

    // Marketing, delivery updates, cart abandonment and non-financial email patterns
    private val emailMarketingPattern = Pattern.compile(
        "(?i)\\b(?:newsletter|weekly\\s+digest|top\\s+deals|deal\\s+of\\s+the\\s+day|flash\\s+sale|\\d+%\\s+off|coupon|voucher|we\\s+miss\\s+you|cart\\s+is\\s+waiting|items\\s+in\\s+your\\s+cart|abandoned\\s+cart|out\\s+for\\s+delivery|arriving\\s+today|arriving\\s+tomorrow|package\\s+delivered|track\\s+your\\s+(?:shipment|package|order)|unsubscribe|view\\s+in\\s+browser|job\\s+alert|promotions?)\\b"
    )

    // Positive indicators of financial transaction emails
    private val emailFinancialIndicator = Pattern.compile(
        "(?i)\\b(?:transaction\\s+alert|debit\\s+alert|credit\\s+alert|alert:\\s+transaction|payment\\s+receipt|payment\\s+confirmed|payment\\s+successful|order\\s+confirmed|tax\\s+invoice|e-receipt|statement\\s+summary|receipt\\s+for|has\\s+been\\s+debited|has\\s+been\\s+credited|you've\\s+spent|paid\\s+to|charged\\s+usd|charged\\s+inr)\\b"
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

        val header = title?.trim() ?: ""
        val bodyContent = (if (!bigText.isNullOrBlank()) bigText else text)?.trim() ?: ""

        val combinedContent = buildString {
            if (header.isNotBlank()) append(header).append(". ")
            if (bodyContent.isNotBlank()) append(bodyContent)
        }.trim()

        if (combinedContent.isBlank()) return null

        // 1. Immediately drop marketing, newsletter, and delivery tracking emails
        if (emailMarketingPattern.matcher(header).find() || emailMarketingPattern.matcher(combinedContent).find()) {
            return null
        }

        // 2. Reject if neither the header nor body shows any financial transaction indicators
        val hasFinancialIndicator = emailFinancialIndicator.matcher(header).find() ||
            emailFinancialIndicator.matcher(combinedContent).find()

        // If no explicit email financial keyword is found, verify via smsParserEngine's financial alert check
        if (!hasFinancialIndicator && !smsParserEngine.isFinancialAlert(combinedContent, header)) {
            return null
        }

        return smsParserEngine.parse(
            body = combinedContent,
            receivedTimestamp = timestamp,
            sourceChannel = SourceChannel.EMAIL,
            sender = header
        )
    }
}
