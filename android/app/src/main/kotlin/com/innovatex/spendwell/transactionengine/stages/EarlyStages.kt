package com.innovatex.spendwell.transactionengine.stages

import com.innovatex.spendwell.security.SensitiveDataSanitizer
import com.innovatex.spendwell.transactionengine.model.DropReason
import com.innovatex.spendwell.transactionengine.model.NormalizedMessage
import com.innovatex.spendwell.transactionengine.model.RawSignal
import java.util.Locale
import java.util.regex.Pattern

object Stage2SourceIdentification {

    private val BANK_SENDER_PATTERNS = mapOf(
        "HDFC" to Pattern.compile("(?i)[a-z]{2}-?HDFCBK|[a-z]{2}-?HDFC"),
        "ICICI" to Pattern.compile("(?i)[a-z]{2}-?ICICIB|[a-z]{2}-?ICICI"),
        "SBI" to Pattern.compile("(?i)[a-z]{2}-?SBIINB|[a-z]{2}-?SBIPSG|[a-z]{2}-?SBIN"),
        "AXIS" to Pattern.compile("(?i)[a-z]{2}-?AXISBK|[a-z]{2}-?AXIS"),
        "KOTAK" to Pattern.compile("(?i)[a-z]{2}-?KOTAKB|[a-z]{2}-?KOTAK"),
        "IDFC" to Pattern.compile("(?i)[a-z]{2}-?IDFCFB|[a-z]{2}-?IDFC"),
        "PNB" to Pattern.compile("(?i)[a-z]{2}-?PUNJNB|[a-z]{2}-?PNBSMS"),
        "CANARA" to Pattern.compile("(?i)[a-z]{2}-?CANBNK"),
        "BOB" to Pattern.compile("(?i)[a-z]{2}-?BARBOD|[a-z]{2}-?BOBSMS"),
        "YES" to Pattern.compile("(?i)[a-z]{2}-?YESBNK"),
        "INDUSIND" to Pattern.compile("(?i)[a-z]{2}-?INDUSB"),
        "CRED" to Pattern.compile("(?i)[a-z]{2}-?CREDCL|[a-z]{2}-?CREDTX"),
        "GPAY" to Pattern.compile("(?i)com\\.google\\.android\\.apps\\.nbu\\.paisa\\.user|gpay"),
        "PHONEPE" to Pattern.compile("(?i)com\\.phonepe\\.app|phonepe"),
        "PAYTM" to Pattern.compile("(?i)net\\.one97\\.paytm|paytm")
    )

    fun identifyBank(sender: String): String? {
        val cleanSender = sender.trim()
        for ((bank, pattern) in BANK_SENDER_PATTERNS) {
            if (pattern.matcher(cleanSender).find()) {
                return bank
            }
        }
        return null
    }

    fun isKnownFinancialSender(sender: String): Boolean {
        return identifyBank(sender) != null
    }
}

object Stage3MessageNormalization {

    fun normalize(signal: RawSignal): NormalizedMessage {
        val detectedBank = Stage2SourceIdentification.identifyBank(signal.senderAddress)
        val isKnownSender = detectedBank != null

        var text = signal.rawText
            .replace('\u00A0', ' ') // Replace non-breaking spaces
            .replace(Regex("\\s+"), " ")
            .trim()

        // Normalize currency symbols
        text = text.replace(Regex("(?i)\\b(?:inr|rs\\.?|rup(?:ees?)?)\\s*"), "₹")

        return NormalizedMessage(
            signal = signal,
            normalizedText = text,
            detectedBank = detectedBank,
            isKnownFinancialSender = isKnownSender
        )
    }
}

object Stage4SpamAndReminderFilter {

    private val REMINDER_PATTERNS = listOf(
        Pattern.compile("(?i)\\b(?:due date|is due on|bill generated|minimum amount due|total amount due|pay before|pay by)\\b"),
        Pattern.compile("(?i)\\b(?:reminder to pay|upcoming bill|please pay by|avoid late fee)\\b")
    )

    private val PROMOTIONAL_PATTERNS = listOf(
        Pattern.compile("(?i)\\b(?:pre-approved|apply for loan|loan approved up to|congratulations! you are eligible|get cash in 5 minutes)\\b"),
        Pattern.compile("(?i)\\b(?:exclusive offer|instant cash discount|click to avail|flat [0-9]+% off)\\b"),
        Pattern.compile("(?i)\\b(?:upgrade your card|increase your limit|free credit card)\\b")
    )

    private val BALANCE_ONLY_PATTERNS = listOf(
        Pattern.compile("(?i)\\b(?:available balance is|clear balance is|bal is ₹|avl bal:?\\s*₹)\\s*[0-9,.]+\\s*(?:as of|on)?\\s*.*(?<!debited)(?<!credited)$")
    )

    fun filter(normalized: NormalizedMessage): DropReason? {
        val text = normalized.normalizedText
        val lower = text.lowercase(Locale.ROOT)

        // 1. Check if it is an OTP or authentication message
        if (SensitiveDataSanitizer.isAuthenticationOrOtpMessage(text)) {
            return DropReason.AUTHENTICATION_OTP
        }

        // 2. Check if it is promotional spam
        for (p in PROMOTIONAL_PATTERNS) {
            if (p.matcher(text).find()) {
                // If it explicitly has a debit confirmation, do not drop
                if (!hasActualDebitConfirmation(lower)) {
                    return DropReason.SPAM_OR_PROMOTIONAL
                }
            }
        }

        // 3. Check if it is a payment reminder
        for (p in REMINDER_PATTERNS) {
            if (p.matcher(text).find()) {
                if (!hasActualDebitConfirmation(lower)) {
                    return DropReason.PAYMENT_REMINDER_NOT_TRANSACTION
                }
            }
        }

        // 4. Check if it's pure balance enquiry without transaction
        if ((lower.contains("available bal") || lower.contains("bal in your a/c")) &&
            !lower.contains("debited") && !lower.contains("credited") && !lower.contains("paid") && !lower.contains("sent")
        ) {
            return DropReason.BALANCE_ENQUIRY_ONLY
        }

        return null
    }

    private fun hasActualDebitConfirmation(lower: String): Boolean {
        return (lower.contains("debited for") || lower.contains("debited with") || lower.contains("debited by") ||
                lower.contains("paid ₹") || lower.contains("sent ₹") || lower.contains("spent on") || lower.contains("credited with")) &&
                !lower.contains("will be debited")
    }
}
