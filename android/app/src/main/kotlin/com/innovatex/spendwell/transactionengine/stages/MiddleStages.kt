package com.innovatex.spendwell.transactionengine.stages

import com.innovatex.spendwell.common.Money
import com.innovatex.spendwell.domain.model.FinancialNature
import com.innovatex.spendwell.domain.model.TransactionStatus
import com.innovatex.spendwell.domain.model.TransactionType
import com.innovatex.spendwell.transactionengine.model.ExtractedFinancialInfo
import com.innovatex.spendwell.transactionengine.model.NormalizedMessage
import java.util.Locale
import java.util.regex.Pattern

object Stage5TransactionDetection {

    private val TRANSACTION_INDICATORS = listOf(
        Pattern.compile("(?i)\\b(?:debited|withdrawn|spent|paid|sent|deducted|charged|purchase)\\b"),
        Pattern.compile("(?i)\\b(?:credited|deposited|salary credited|refunded|reversed|cashback received)\\b")
    )

    fun isTransaction(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)

        // Negative indicators
        if (lower.contains("points received") || lower.contains("reward points") ||
            lower.contains("message received") || lower.contains("request received") ||
            lower.contains("pre-approved") || lower.contains("loan sanctioned")
        ) {
            // Check if there's an actual debit/credit verb
            val hasExplicitTxn = lower.contains("debited for") || lower.contains("debited with") ||
                    lower.contains("debited by") || lower.contains("credited with") ||
                    lower.contains("paid ₹") || lower.contains("sent ₹")
            if (!hasExplicitTxn) return false
        }

        for (pattern in TRANSACTION_INDICATORS) {
            if (pattern.matcher(text).find()) {
                return true
            }
        }
        return false
    }
}

object Stage6StructuredExtraction {

    // Matches formatted numbers with commas (e.g. 1,500.00, 1,50,000.50) OR plain unformatted numbers (e.g. 1500, 1500.00, 150)
    // with negative lookahead (?![0-9]) to prevent greedy partial-digit matching (e.g. 1500 -> 150).
    private val AMOUNT_PATTERN = Pattern.compile("₹\\s*([0-9]{1,3}(?:,[0-9]{2,3})+(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)(?![0-9])")
    // Fallback amount: e.g. "for 1,250.00" or "amount: 1500"
    private val FALLBACK_AMOUNT_PATTERN = Pattern.compile("(?i)(?:for|of|amount|amt)(?:\\s*[:#])?\\s*([0-9]+(?:,[0-9]+)*(?:\\.[0-9]{1,2})?)(?![0-9])")

    // Account Last 4
    private val ACCOUNT_PATTERNS = listOf(
        Pattern.compile("(?i)(?:a/c|acct|account|card)(?:\\s*(?:no\\.?|ending|ending with|ending in))?\\s*(?:[x*]{2,}|xx)?([0-9]{4})\\b"),
        Pattern.compile("(?i)\\b(?:x{2,}|[*]{2,})([0-9]{4})\\b")
    )

    // Reference ID / UTR / RRN
    private val REF_PATTERNS = listOf(
        Pattern.compile("(?i)\\b(?:utr|upi\\s*ref(?:\\s*no)?|rrn|ref(?:\\s*no)?|txn(?:\\s*id)?|reference)\\s*[:#]?\\s*([A-Za-z0-9]{6,22})\\b"),
        Pattern.compile("(?i)\\b(?:ref|utr)[:\\s]+([0-9]{10,16})\\b")
    )

    // Merchant / Payee extraction
    private val MERCHANT_PATTERNS = listOf(
        Pattern.compile("(?i)(?:paid\\s+to|sent\\s+to|transfer(?:red)?\\s+to|credited\\s+from|from)\\s+([A-Za-z0-9 &._@\\-]+?)(?:\\s+(?:on|via|ref|using|avl|bal|a/c|utr|through|by|dated|\\.|\\,)|$)"),
        Pattern.compile("(?i)(?:at|for|vpa)\\s+([A-Za-z0-9 &._@\\-]+?)(?:\\s+(?:on|via|ref|using|avl|bal|a/c|utr|through|by|dated|\\.|\\,)|$)")
    )

    fun extract(normalized: NormalizedMessage): ExtractedFinancialInfo? {
        val text = normalized.normalizedText
        val lower = text.lowercase(Locale.ROOT)

        // 1. Extract Amount
        var amountStr: String? = null
        val amountMatcher = AMOUNT_PATTERN.matcher(text)
        if (amountMatcher.find()) {
            amountStr = amountMatcher.group(1)
        } else {
            val fallbackMatcher = FALLBACK_AMOUNT_PATTERN.matcher(text)
            if (fallbackMatcher.find()) {
                amountStr = fallbackMatcher.group(1)
            }
        }

        if (amountStr == null) return null
        val money = Money.fromString(amountStr)
        if (money.isZero) return null

        // 2. Transaction Type
        val isCredit = lower.contains("credited") || lower.contains("received") || lower.contains("deposited") || lower.contains("refunded")
        val type = if (isCredit) TransactionType.CREDIT else TransactionType.DEBIT

        // 3. Status and Lifecycle flags
        val isFailed = lower.contains("failed") || lower.contains("declined") || lower.contains("unsuccessful")
        val isReversal = lower.contains("reversed") || lower.contains("reversal")
        val isRefund = lower.contains("refund") || lower.contains("refunded")
        val isPending = lower.contains("pending") || lower.contains("in process")

        val status = when {
            isFailed -> TransactionStatus.FAILED
            isReversal -> TransactionStatus.REVERSED
            isRefund -> TransactionStatus.REFUNDED
            isPending -> TransactionStatus.PENDING
            else -> TransactionStatus.SUCCESSFUL
        }

        // 4. Account Last 4
        var acctLast4: String? = null
        for (pattern in ACCOUNT_PATTERNS) {
            val m = pattern.matcher(text)
            if (m.find()) {
                acctLast4 = m.group(1)
                break
            }
        }

        // 5. Reference / UTR
        var refId: String? = null
        var utr: String? = null
        for (pattern in REF_PATTERNS) {
            val m = pattern.matcher(text)
            if (m.find()) {
                val candidate = m.group(1)
                if (m.group(0)?.lowercase()?.contains("utr") == true) {
                    utr = candidate
                } else {
                    refId = candidate
                }
                break
            }
        }

        // 6. Raw Merchant extraction
        var rawMerchant: String? = null
        for (pattern in MERCHANT_PATTERNS) {
            val m = pattern.matcher(text)
            if (m.find()) {
                val match = m.group(1)?.trim()
                if (!match.isNullOrBlank() && match.length > 2 && !match.startsWith("₹") && !match.all { it.isDigit() }) {
                    rawMerchant = match
                    break
                }
            }
        }

        // 7. Merchant normalization
        val normalizedMerchant = Stage8MerchantNormalization.cleanMerchantName(rawMerchant ?: "Transfer")

        // 8. Determine Financial Nature
        val nature = Stage7AmountAndDirectionValidation.determineNature(
            type = type,
            merchantName = normalizedMerchant,
            rawText = lower,
            isRefundOrReversal = isRefund || isReversal
        )

        return ExtractedFinancialInfo(
            amount = money,
            transactionType = type,
            status = status,
            financialNature = nature,
            accountLast4 = acctLast4,
            bankName = normalized.detectedBank,
            rawMerchant = rawMerchant,
            normalizedMerchant = normalizedMerchant,
            referenceId = refId,
            utrNumber = utr,
            isRefund = isRefund,
            isReversal = isReversal,
            timestamp = normalized.signal.timestamp
        )
    }
}

object Stage7AmountAndDirectionValidation {

    fun determineNature(
        type: TransactionType,
        merchantName: String,
        rawText: String,
        isRefundOrReversal: Boolean
    ): FinancialNature {
        val lowerMerch = merchantName.lowercase(Locale.ROOT)

        if (isRefundOrReversal) {
            return FinancialNature.REFUND
        }

        // Cash withdrawal
        if (rawText.contains("cash with") || rawText.contains("atm with") || rawText.contains("atm cash")) {
            return FinancialNature.CASH_WITHDRAWAL
        }

        // Internal transfer / Self-transfer (strictly excluded from consumption expenses)
        if (lowerMerch.contains("self") || lowerMerch.contains("own account") ||
            rawText.contains("transfer to self") || rawText.contains("self transfer") ||
            rawText.contains("transfer between own") || rawText.contains("to own a/c") ||
            rawText.contains("sent to self")
        ) {
            return FinancialNature.INTERNAL_TRANSFER
        }

        // Credit Card bill payment (strictly excluded from consumption expenses to prevent double counting)
        if (lowerMerch.contains("credit card") || lowerMerch.contains("cc payment") || lowerMerch.contains("cred") ||
            rawText.contains("credit card payment") || rawText.contains("card bill paid") ||
            rawText.contains("towards credit card") || rawText.contains("payment received for your credit card") ||
            rawText.contains("payment for card ending")
        ) {
            return FinancialNature.CREDIT_CARD_BILL_PAYMENT
        }

        // Investment transfer
        if (lowerMerch.contains("zerodha") || lowerMerch.contains("groww") || lowerMerch.contains("upstox") ||
            lowerMerch.contains("mutual fund") || lowerMerch.contains("kuvera") || lowerMerch.contains("et money")
        ) {
            return FinancialNature.INVESTMENT_TRANSFER
        }

        // Loan disbursement
        if (type == TransactionType.CREDIT && (rawText.contains("loan disburs") || rawText.contains("loan sanctioned"))) {
            return FinancialNature.LOAN_DISBURSEMENT
        }

        // Default direction
        return if (type == TransactionType.CREDIT) {
            FinancialNature.INCOME
        } else {
            FinancialNature.CONSUMPTION_EXPENSE
        }
    }
}

object Stage8MerchantNormalization {

    private val KNOWN_MERCHANTS = mapOf(
        "swiggy" to "Swiggy",
        "zomato" to "Zomato",
        "blinkit" to "Blinkit",
        "zepto" to "Zepto",
        "amazon" to "Amazon",
        "flipkart" to "Flipkart",
        "myntra" to "Myntra",
        "uber" to "Uber",
        "ola" to "Ola",
        "rapido" to "Rapido",
        "netflix" to "Netflix",
        "spotify" to "Spotify",
        "hotstar" to "Disney+ Hotstar",
        "youtube" to "YouTube Premium",
        "apple" to "Apple Services",
        "google" to "Google Services",
        "irctc" to "IRCTC",
        "makemytrip" to "MakeMyTrip",
        "airtel" to "Airtel",
        "jio" to "Jio",
        "vi" to "Vodafone Idea",
        "tata power" to "Tata Power",
        "bescom" to "BESCOM",
        "cult.fit" to "Cult.fit",
        "curefit" to "Cult.fit",
        "starbucks" to "Starbucks",
        "mcdonald" to "McDonald's",
        "domino" to "Domino's Pizza"
    )

    fun cleanMerchantName(raw: String): String {
        var clean = raw.trim()

        // Strip UPI VPA suffixes like @okhdfcbank, @icici, @paytm, @apl, @axl
        if (clean.contains("@")) {
            clean = clean.substringBefore("@")
        }

        // Strip prefixes like "VPA ", "UPI-", "POS-"
        clean = clean.replace(Regex("(?i)^(?:vpa|upi|pos|e-com|imps|neft|rtgs)[-_/ ]+"), "")

        // Normalize matching known brands
        val lower = clean.lowercase(Locale.ROOT)
        for ((key, canonical) in KNOWN_MERCHANTS) {
            if (lower.contains(key)) {
                return canonical
            }
        }

        // Clean up punctuation and title-case
        clean = clean.replace(Regex("[^a-zA-Z0-9 &.'-]"), " ").trim()
        clean = clean.split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { it.replaceFirstChar { ch -> ch.uppercaseChar() } }

        return if (clean.isBlank()) "Merchant" else clean
    }
}
