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
        "SBI" to Pattern.compile("(?i)[a-z]{2}-?SBIINB|[a-z]{2}-?SBIPSG|[a-z]{2}-?SBIN|[a-z]{2}-?SBICRD"),
        "AXIS" to Pattern.compile("(?i)[a-z]{2}-?AXISBK|[a-z]{2}-?AXIS"),
        "KOTAK" to Pattern.compile("(?i)[a-z]{2}-?KOTAKB|[a-z]{2}-?KOTAK"),
        "IDFC" to Pattern.compile("(?i)[a-z]{2}-?IDFCFB|[a-z]{2}-?IDFC"),
        "PNB" to Pattern.compile("(?i)[a-z]{2}-?PUNJNB|[a-z]{2}-?PNBSMS"),
        "CANARA" to Pattern.compile("(?i)[a-z]{2}-?CANBNK"),
        "BOB" to Pattern.compile("(?i)[a-z]{2}-?BARBOD|[a-z]{2}-?BOBSMS"),
        "YES" to Pattern.compile("(?i)[a-z]{2}-?YESBNK"),
        "INDUSIND" to Pattern.compile("(?i)[a-z]{2}-?INDUSB"),
        "CITI" to Pattern.compile("(?i)[a-z]{2}-?CITIBK"),
        "SCB" to Pattern.compile("(?i)[a-z]{2}-?SCBLTD"),
        "AMEX" to Pattern.compile("(?i)[a-z]{2}-?AMEXIN|[a-z]{2}-?AMEX"),
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

    /**
     * Checks if the sender is an ordinary personal 10-digit phone number.
     * Genuine bank alerts in India and internationally use alphanumeric sender IDs (e.g. AD-HDFCBK).
     */
    fun isPersonalPhoneNumber(sender: String): Boolean {
        val digitsOnly = sender.filter { it.isDigit() }
        // Standard personal mobile numbers are 10-12 digits starting with country code or digits
        return (digitsOnly.length in 10..13) && !sender.any { it.isLetter() }
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
        // Statements & Bill due notices
        Pattern.compile("(?i)\\b(?:due date|is due on|due on|payable by|pay before|pay by|bill generated|statement generated|statement for.*is generated|bill of.*is generated)\\b"),
        Pattern.compile("(?i)\\b(?:minimum amount due|total amount due|min due|total due|min amt due|total amt due|outstanding balance)\\b"),
        Pattern.compile("(?i)\\b(?:reminder to pay|upcoming bill|please pay by|avoid late fee|pay now to avoid|to avoid disconnection)\\b"),
        // Scheduled debits & Mandates (NOT executed transactions yet)
        Pattern.compile("(?i)\\b(?:will be debited|is scheduled for deduction|scheduled for deduction|mandate scheduled|si scheduled|autopay scheduled|reminder:.*emi|upcoming emi)\\b"),
        // Upcoming utility & subscription renewals
        Pattern.compile("(?i)\\b(?:recharge due|broadband bill.*due|electricity bill.*due|water bill.*due|gas bill.*due)\\b")
    )

    private val PROMOTIONAL_PATTERNS = listOf(
        // Loan & Credit Cards offers
        Pattern.compile("(?i)\\b(?:pre-approved|apply for loan|loan approved up to|pre-qualified|eligible for.*loan|get cash in [0-9]+ minutes|instant cash loan|collateral free)\\b"),
        Pattern.compile("(?i)\\b(?:upgrade your card|increase your limit|limit enhanced|free credit card|apply now for credit card|card limit increased)\\b"),
        // Marketing offers & Discounts
        Pattern.compile("(?i)\\b(?:exclusive offer|instant cash discount|click to avail|flat [0-9]+% off|discount coupon|voucher worth|special offer|limited period offer)\\b"),
        // Lottery, Gambling & Phishing Spam
        Pattern.compile("(?i)\\b(?:rummy|lottery|win up to ₹|stand a chance to win|congratulations.*winner|claimed successfully.*click|reward points worth|points expiring)\\b"),
        // Spend promos (e.g. "Spend Rs 500 to get Rs 50")
        Pattern.compile("(?i)\\b(?:spend ₹.*(?:to get|and get)|shop for ₹.*get ₹)\\b")
    )

    fun filter(normalized: NormalizedMessage): DropReason? {
        val text = normalized.normalizedText
        val lower = text.lowercase(Locale.ROOT)
        val sender = normalized.signal.senderAddress

        // 1. Reject personal 10-digit mobile numbers sending unverified text
        if (Stage2SourceIdentification.isPersonalPhoneNumber(sender)) {
            return DropReason.SPAM_OR_PROMOTIONAL
        }

        // 2. Check if it is an OTP or authentication message
        if (SensitiveDataSanitizer.isAuthenticationOrOtpMessage(text)) {
            return DropReason.AUTHENTICATION_OTP
        }

        // 3. Check if it is promotional spam
        for (p in PROMOTIONAL_PATTERNS) {
            if (p.matcher(text).find()) {
                // If it explicitly has an executed debit or credit confirmation without future condition, keep; else drop
                if (!hasActualExecutedConfirmation(lower)) {
                    return DropReason.SPAM_OR_PROMOTIONAL
                }
            }
        }

        // 4. Check if it is a payment reminder or scheduled mandate notice
        for (p in REMINDER_PATTERNS) {
            if (p.matcher(text).find()) {
                if (!hasActualExecutedConfirmation(lower)) {
                    return DropReason.PAYMENT_REMINDER_NOT_TRANSACTION
                }
            }
        }

        // 5. Check if it's pure balance enquiry without transaction
        if ((lower.contains("available bal") || lower.contains("bal in your a/c") || lower.contains("clear balance")) &&
            !lower.contains("debited") && !lower.contains("credited") && !lower.contains("paid") && !lower.contains("sent")
        ) {
            return DropReason.BALANCE_ENQUIRY_ONLY
        }

        return null
    }

    /**
     * Confirms that an actual past-tense monetary transaction has completed.
     * Prevents dropping genuine receipts that mention terms like "offer" or "statement".
     */
    private fun hasActualExecutedConfirmation(lower: String): Boolean {
        // Disqualifiers: future or scheduled actions
        if (lower.contains("will be debited") || lower.contains("scheduled for") ||
            lower.contains("is due on") || lower.contains("pay before") || lower.contains("payable by") ||
            lower.contains("to pay click") || lower.contains("avoid late fee")
        ) {
            return false
        }

        // Definite past-tense debit verbs
        val hasDebit = lower.contains("debited for") || lower.contains("debited with") || lower.contains("debited by") ||
                lower.contains("paid ₹") || lower.contains("sent ₹") || lower.contains("spent on") ||
                lower.contains("withdrawn from") || lower.contains("purchase of ₹")

        // Definite past-tense credit verbs
        val hasCredit = lower.contains("credited with") || lower.contains("credited to your a/c") ||
                lower.contains("credited by ₹") || lower.contains("salary credited") || lower.contains("refund of ₹")

        return hasDebit || hasCredit
    }
}
