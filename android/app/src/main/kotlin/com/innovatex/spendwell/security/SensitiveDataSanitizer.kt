package com.innovatex.spendwell.security

import java.util.regex.Pattern

/**
 * Sanitizes sensitive banking, authentication, and personal identity data before logging
 * or internal processing to maintain strict privacy and zero telemetry leakage.
 */
object SensitiveDataSanitizer {

    private val OTP_PATTERN = Pattern.compile("(?i)\\b(?:otp|one time password|code|verification pin)\\s*(?:is|:)?\\s*([0-9]{4,8})\\b")
    private val CARD_NUMBER_PATTERN = Pattern.compile("\\b(?:[0-9]{4}[-\\s]?){3}([0-9]{4})\\b")
    private val ACCOUNT_NUMBER_PATTERN = Pattern.compile("(?i)(?:a/c|acct|account)(?:\\s*no\\.?)?\\s*[:#]?\\s*([0-9]{5,18})")
    private val UPI_ID_PATTERN = Pattern.compile("([a-zA-Z0-9._-]+)@([a-zA-Z0-9]+)")
    private val PHONE_NUMBER_PATTERN = Pattern.compile("\\b(?:\\+91[- ]?)?[6-9][0-9]{9}\\b")

    /**
     * Obfuscates any OTP, password, or sensitive account digits from a raw log or trace string.
     */
    fun sanitizeForLogging(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        var sanitized = raw

        // Mask OTP
        sanitized = OTP_PATTERN.matcher(sanitized).replaceAll("OTP [REDACTED]")

        // Mask 16-digit cards, keeping only last 4
        val cardMatcher = CARD_NUMBER_PATTERN.matcher(sanitized)
        val sbCard = StringBuffer()
        while (cardMatcher.find()) {
            val last4 = cardMatcher.group(1)
            cardMatcher.appendReplacement(sbCard, "••••-••••-••••-$last4")
        }
        cardMatcher.appendTail(sbCard)
        sanitized = sbCard.toString()

        // Mask long account numbers, keeping only last 4
        val acctMatcher = ACCOUNT_NUMBER_PATTERN.matcher(sanitized)
        val sbAcct = StringBuffer()
        while (acctMatcher.find()) {
            val acctNum = acctMatcher.group(1)
            val last4 = if (acctNum.length >= 4) acctNum.takeLast(4) else acctNum
            acctMatcher.appendReplacement(sbAcct, "Acct ••••$last4")
        }
        acctMatcher.appendTail(sbAcct)
        sanitized = sbAcct.toString()

        // Mask phone numbers
        sanitized = PHONE_NUMBER_PATTERN.matcher(sanitized).replaceAll("••••••[PHONE]")

        return sanitized
    }

    /**
     * Checks if a text snippet looks strictly like an authentication OTP or password reset
     * rather than an actual financial ledger transaction.
     */
    fun isAuthenticationOrOtpMessage(text: String): Boolean {
        val lower = text.lowercase()
        val hasOtpKeyword = lower.contains("otp") ||
                lower.contains("one time password") ||
                lower.contains("verification code") ||
                lower.contains("secret code") ||
                lower.contains("do not share")
        val hasLoginKeyword = lower.contains("login") ||
                lower.contains("sign in") ||
                lower.contains("reset password") ||
                lower.contains("device registered")

        val hasMonetaryTransfer = lower.contains("debited") ||
                lower.contains("credited") ||
                lower.contains("paid to") ||
                lower.contains("spent on") ||
                lower.contains("sent rs") ||
                lower.contains("received rs")

        // If it explicitly asks not to share OTP and doesn't mention money debit/credit
        return (hasOtpKeyword || hasLoginKeyword) && !hasMonetaryTransfer
    }
}
