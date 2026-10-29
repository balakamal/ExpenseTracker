package com.innovatex.expensetracker.ingestion.parser

import com.innovatex.expensetracker.domain.model.SourceChannel
import com.innovatex.expensetracker.domain.model.TransactionType
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

data class ParsedTransactionCandidate(
    val amount: Double,
    val currency: String,
    val type: TransactionType,
    val transactionTimestamp: Long,
    val rawMerchant: String,
    val cleanMerchant: String,
    val referenceId: String?,
    val accountLast4: String?,
    val sourceChannel: SourceChannel,
    val rawSnippet: String
)

@Singleton
class SmsParserEngine @Inject constructor() {

    // 1. Authentication / OTP patterns
    private val otpPattern = Pattern.compile(
        "(?i)(?:\\b(?:otp|one[-\\s]?time[-\\s]?password|verification[-\\s]?code|security[-\\s]?code)\\b|(?:is|use)\\s+code\\s+\\d{4,8}|\\bcode\\s+is\\s+\\d{4,8})"
    )

    // 2. Pre-approved loans, credit limit increase, CIBIL score marketing (excluding executed EMI debits)
    private val loanMarketingPattern = Pattern.compile(
        "(?i)\\b(?:pre-approved|pre\\s+approved|pre-qualified|pre\\s+qualified|loan\\s+offer|apply\\s+now|apply\\s+for\\s+(?:a\\s+)?loan|check\\s+eligibility|eligible\\s+for\\s+(?:a\\s+)?(?:loan|credit\\s+card)|credit\\s+limit\\s+(?:is|increased|enhanced)|limit\\s+increased\\s+to|enhance\\s+your\\s+limit|credit\\s+score|cibil\\s+score|instant\\s+cash\\s+loan|disbursed\\s+to\\s+(?:your\\s+)?(?:offer\\s+)?wallet|avail\\s+loan\\s+up\\s+to)\\b"
    )

    // 3. Promotional offers, discounts, coupons, cashback schemes, gaming/lottery
    private val promoPattern = Pattern.compile(
        "(?i)\\b(?:exclusive\\s+offer|special\\s+offer|claim\\s+now|claim\\s+your|claim\\s+reward|discount|coupon|voucher|promo\\s+code|use\\s+code|flat\\s+\\d+%|up\\s+to\\s+\\d+%|win\\s+cash|win\\s+up\\s+to|lucky\\s+winner|play\\s+(?:and|&)\\s+win|rummy|dream11|jackpot|hurry\\b|offer\\s+valid|sale\\s+is\\s+live|mega\\s+sale|on\\s+your\\s+next\\s+purchase|minimum\\s+purchase|bonus\\s+points|redeem\\s+now)\\b"
    )

    // 4. Bill due / Upcoming payment demands (Prospective/unpaid notices)
    val reminderAndProspectivePattern = Pattern.compile(
        "(?i)\\b(?:reminder|kindly\\s+pay|please\\s+pay|pay\\s+now|pay\\s+before|pay\\s+by|due\\s+date|due\\s+on|is\\s+due|payment\\s+due|amount\\s+due|minimum\\s+due|min\\s+due|total\\s+due|overdue|bill\\s+generated|bill\\s+is\\s+due|upcoming\\s+payment|scheduled\\s+for|will\\s+be\\s+(?:auto[- ]?)?debited|will\\s+be\\s+deducted|to\\s+be\\s+debited|to\\s+be\\s+paid|shall\\s+be\\s+debited|is\\s+scheduled\\s+to|maintain\\s+sufficient\\s+balance|keep\\s+sufficient\\s+balance|keep\\s+balance|recharge\\s+now|recharge\\s+with|pack\\s+expiring|validity\\s+expiring|plan\\s+expires|collect\\s+request|requested\\s+money|approve\\s+request|mandate\\s+created|mandate\\s+scheduled|autopay\\s+scheduled|standing\\s+instruction|statement\\s+generated|statement\\s+is\\s+ready)\\b"
    )

    private val billDuePattern = Pattern.compile(
        "(?i)\\b(?:bill\\s+is\\s+due|payment\\s+due|due\\s+date|due\\s+on\\s+\\d|pay\\s+before|pay\\s+by\\s+\\d|to\\s+avoid\\s+(?:late\\s+fee|disconnection|interruption)|minimum\\s+amount\\s+due|min\\s+due|total\\s+amount\\s+due|unpaid\\s+bill)\\b"
    )

    // 5. Payment requests / Mandate scheduling (Not completed executions)
    private val paymentRequestPattern = Pattern.compile(
        "(?i)\\b(?:requested\\s+money|request\\s+to\\s+pay|has\\s+requested|collect\\s+request|approve\\s+(?:payment\\s+)?request|mandate\\s+created|mandate\\s+request|autopay\\s+scheduled|standing\\s+instruction\\s+set)\\b"
    )

    // 6. Telecom recharge and data marketing
    private val telecomPattern = Pattern.compile(
        "(?i)\\b(?:recharge\\s+with|recharge\\s+now|pack\\s+expiring|validity\\s+expiring|plan\\s+expires|\\d+\\s*gb/day|daily\\s+data|unlimited\\s+calls|talktime\\s+balance)\\b"
    )

    // 7. Balance enquiry / Security checks
    private val balanceCheckPattern = Pattern.compile(
        "(?i)(balance as of|statement available|e-statement|login detected|password changed|account blocked|card blocked|kyc update|update your kyc)"
    )

    // Debit patterns: past-tense or completed execution verbs across UPI, Bank, Cards, ATM
    private val debitKeywords = Pattern.compile(
        "(?i)\\b(debited|debit|spent|paid|withdrawn|charged|sent|deducted|transfer|transferred|purchase|purchased|txn|payment|auto-debited|auto\\s+debit|autopay|card\\s+used|used\\s+for|used\\s+at|dr\\.?\\s+with|dr\\.?\\s+of|dr\\b)\\b"
    )

    // Credit patterns: past-tense or completed inward transfer verbs
    private val creditKeywords = Pattern.compile(
        "(?i)\\b(credited|credit|deposited|refunded|received|added|salary|cashback|inward|cr\\.?\\s+with|cr\\.?\\s+of|cr\\b)\\b"
    )

    // Amount patterns ($12.50, USD 42.00, Rs. 500, INR 1,200.50, ₹500, 500/-, etc.)
    private val amountPattern = Pattern.compile(
        "(?i)(?:([$€£₹]|USD|EUR|GBP|INR|CAD|AUD|Rs\\.?|INR\\.?)\\s*([0-9,]+(?:\\.[0-9]{1,2})?))|([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(USD|EUR|GBP|INR|CAD|AUD|Rs\\.?|INR\\.?|/-)|(?:(?:debited|credited|paid|sent|spent|transfer|txn)\\s+(?:by|for|with|of)?\\s*(?:([$€£₹]|Rs\\.?|INR\\.?)\\s*)?([0-9,]+(?:\\.[0-9]{1,2})?))"
    )

    // Merchant extraction following prepositions (at Starbucks, to Whole Foods, for Uber, from Amazon, towards Swiggy, to VPA)
    private val merchantPrepositionPattern = Pattern.compile(
        "(?i)\\b(?:at|in|vpa|from|towards|(?:for(?!\\s*(?:[$€£₹]|USD|EUR|GBP|INR|CAD|AUD|Rs\\.?|INR\\.?|\\d)))|(?:to(?!\\s*(?:acct|a/c|account|card|\\*\\*|xx|\\d))))\\s+([A-Za-z0-9\\s\\.\\&\\*\\-@]{2,35}?)(?:\\s+(?:on|using|via|ref|rrn|utr|bal|avbl|acct|ending|card|upi)\\b|[.,;]|$)"
    )

    // Account last 4 digits (ending 1234, acct XX1234, card **1234, A/c ...4321, etc.)
    private val accountLast4Pattern = Pattern.compile(
        "(?i)(?:acct|account|a/c|card|ending|xx|\\*\\*|\\.\\.\\.)[\\s:]*(?:(?:ending|no\\.?|#)[\\s:]*)?(?:(?:in|with)[\\s:]*)?[xX\\*\\.]*(\\d{4})\\b"
    )

    // Transaction reference ID / UTR / UPI Ref / RRN
    private val referencePattern = Pattern.compile(
        "(?i)(?:ref|rrn|utr|txn|upi\\s*ref|invoice|order\\s*id)[\\s:#]*([A-Za-z0-9]{6,25})"
    )

    // Comprehensive Banking & Payment Gateways (India + Global + UPI + Wallets + Cards)
    private val bankingEntityPattern = Pattern.compile(
        "(?i)\\b(bank|hdfc|sbi|icici|axis|kotak|idfc|pnb|boi|bob|canara|union\\s*bank|yes\\s*bank|indusind|federal\\s*bank|rbl|central\\s*bank|indian\\s*bank|idbi|uco\\s*bank|south\\s*indian\\s*bank|karur\\s*vysya|bandhan|au\\s*small\\s*finance|dbs|standard\\s*chartered|hsbc|citi|amex|upi|vpa|phonepe|gpay|google\\s*pay|paytm|cred|bhim|amazon\\s*pay|slice|uni|onecard|jupiter|fi\\s*money|navi|supermoney|mobikwik|freecharge|chase|wellsfargo|bofa|barclays|monzo|revolut|paypal|venmo|cashapp|apple\\s*pay|zelle|capital\\s*one|discover|stripe|wallet|card|atm)\\b"
    )

    fun isPersonalPhoneNumber(sender: String?): Boolean {
        if (sender.isNullOrBlank()) return false
        val clean = sender.trim().removePrefix("+")
        // Discard 10 to 12 digit personal mobile phone numbers (e.g. +919876543210, 9876543210)
        return clean.matches(Regex("^(?:91)?[6-9]\\d{9}$")) || clean.matches(Regex("^\\d{10,12}$"))
    }

    fun isProspectiveOrReminder(body: String): Boolean {
        if (body.isBlank()) return false
        val lower = body.lowercase()
        return reminderAndProspectivePattern.matcher(body).find() ||
            lower.containsAny(
                "will be debited", "will be auto-debited", "will be deducted", "to be debited",
                "to be paid", "due date", "due on", "payment due", "amount due", "minimum due",
                "min due", "total due", "overdue", "bill generated", "bill is due", "upcoming payment",
                "scheduled for", "maintain sufficient balance", "keep balance", "keep sufficient balance",
                "recharge now", "recharge with", "pack expiring", "validity expiring", "plan expires",
                "collect request", "requested money", "approve request", "mandate created",
                "mandate scheduled", "autopay scheduled", "standing instruction", "pay before", "pay by"
            )
    }

    fun isFinancialAlert(body: String, sender: String? = null): Boolean {
        if (body.isBlank()) return false
        if (otpPattern.matcher(body).find()) return false

        // 1. Definite Marketing Filters (excluding executed payments/EMIs)
        // Check for prospective notices, payment reminders, and upcoming demands
        val isReminder = isProspectiveOrReminder(body)
        val hasCompletedPaymentVerb = !isReminder && body.containsAny("debited", "spent", "paid", "withdrawn", "credited", "deposited", "transferred", "auto-debited")

        // If it is a reminder or prospective demand without actual execution, drop it
        if (isReminder && !hasCompletedPaymentVerb) return false
        
        // Loan marketing filter: only drop if it's NOT an executed EMI or loan debit
        if (loanMarketingPattern.matcher(body).find() && !hasCompletedPaymentVerb) return false

        // Bill due filter: only drop if it is NOT an executed payment
        if (billDuePattern.matcher(body).find() && !hasCompletedPaymentVerb) return false

        // Unexecuted collect requests / mandate registrations
        if (paymentRequestPattern.matcher(body).find() && !hasCompletedPaymentVerb) return false

        // Telecom recharge promos
        if (telecomPattern.matcher(body).find() && !hasCompletedPaymentVerb) return false

        val hasAmount = amountPattern.matcher(body).find()
        val hasDebit = debitKeywords.matcher(body).find()
        val hasCredit = creditKeywords.matcher(body).find()
        val hasType = hasDebit || hasCredit

        if (!hasAmount || !hasType) return false

        // Balance checks without genuine debit/credit verbs are dropped
        if (balanceCheckPattern.matcher(body).find() && !hasDebit && !hasCredit) return false

        // 2. Financial Anchor Verification:
        val hasAccount = accountLast4Pattern.matcher(body).find()
        val hasRef = referencePattern.matcher(body).find()
        val hasBankEntity = bankingEntityPattern.matcher(body).find() ||
            (sender != null && bankingEntityPattern.matcher(sender).find())
        val hasUpiHandle = body.contains(Regex("(?i)[a-zA-Z0-9\\.\\-_]+@(okhdfcbank|okaxis|okicici|oksbi|paytm|ybl|ibl|axl|apl|upi)"))

        val hasAnchor = hasAccount || hasRef || hasBankEntity || hasUpiHandle

        // If sender is a personal mobile number, require at least one strong anchor (A/C, Ref, or Bank/UPI)
        // to avoid conversational messages like "I sent you 500 yesterday"
        if (isPersonalPhoneNumber(sender) && !hasAccount && !hasRef && !hasUpiHandle) {
            return false
        }

        // If message has promotional keywords, it MUST have strong anchors (account or ref or bank entity)
        // to be recognized as a genuine bank transaction with a promo footer.
        if (promoPattern.matcher(body).find()) {
            if (!hasAccount && !hasRef && !hasUpiHandle) return false
        }

        return hasAnchor
    }

    fun parse(
        body: String,
        receivedTimestamp: Long = System.currentTimeMillis(),
        sourceChannel: SourceChannel = SourceChannel.SMS,
        sender: String? = null
    ): ParsedTransactionCandidate? {
        if (!isFinancialAlert(body, sender)) return null

        // 1. Extract Amount & Currency
        val amountMatcher = amountPattern.matcher(body)
        var parsedAmount = 0.0
        var parsedCurrency = "INR"

        if (amountMatcher.find()) {
            val symbolOrCode1 = amountMatcher.group(1)
            val amountStr1 = amountMatcher.group(2)
            val amountStr2 = amountMatcher.group(3)
            val symbolOrCode2 = amountMatcher.group(4)
            val symbolOrCode3 = amountMatcher.group(5)
            val amountStr3 = amountMatcher.group(6)

            val rawAmount = (amountStr1 ?: amountStr2 ?: amountStr3)?.replace(",", "")
            parsedAmount = rawAmount?.toDoubleOrNull() ?: return null

            // Sanity check on amount to filter out extreme promotional numbers
            if (parsedAmount <= 0.0 || parsedAmount > 50_000_000.0) return null

            val rawCurrency = symbolOrCode1 ?: symbolOrCode2 ?: symbolOrCode3 ?: "INR"
            parsedCurrency = normalizeCurrency(rawCurrency)
        } else {
            return null
        }

        // 2. Classify Transaction Type (Credit vs Debit)
        val isDebit = debitKeywords.matcher(body).find()
        val isCredit = creditKeywords.matcher(body).find()
        val type = when {
            isDebit -> TransactionType.DEBIT
            isCredit -> TransactionType.CREDIT
            else -> TransactionType.DEBIT
        }

        // 3. Extract Merchant
        var rawMerchant = "Unknown Merchant"
        val merchantMatcher = merchantPrepositionPattern.matcher(body)
        if (merchantMatcher.find()) {
            rawMerchant = merchantMatcher.group(1)?.trim() ?: "Unknown Merchant"
        } else {
            val infoMatcher = Pattern.compile("(?i)\\b(?:info|desc|narr|details)[:\\s]+([A-Za-z0-9\\s\\.\\&\\*\\-@]{2,30})").matcher(body)
            if (infoMatcher.find()) {
                rawMerchant = infoMatcher.group(1)?.trim() ?: "Unknown Merchant"
            }
        }
        if (rawMerchant == "Unknown Merchant" && !sender.isNullOrBlank() && !isPersonalPhoneNumber(sender)) {
            val senderClean = sender.trim().substringAfter("-").take(15)
            if (senderClean.isNotBlank()) {
                rawMerchant = senderClean
            }
        }
        val cleanMerchant = cleanMerchantName(rawMerchant)

        // 4. Extract Account Last 4
        var accountLast4: String? = null
        val accountMatcher = accountLast4Pattern.matcher(body)
        if (accountMatcher.find()) {
            accountLast4 = accountMatcher.group(1)
        }

        // 5. Extract Reference / UTR ID
        var referenceId: String? = null
        val refMatcher = referencePattern.matcher(body)
        if (refMatcher.find()) {
            referenceId = refMatcher.group(1)
        }

        val snippet = if (body.length > 120) body.take(117) + "..." else body

        return ParsedTransactionCandidate(
            amount = parsedAmount,
            currency = parsedCurrency,
            type = type,
            transactionTimestamp = receivedTimestamp,
            rawMerchant = rawMerchant,
            cleanMerchant = cleanMerchant,
            referenceId = referenceId,
            accountLast4 = accountLast4,
            sourceChannel = sourceChannel,
            rawSnippet = snippet
        )
    }

    private fun normalizeCurrency(raw: String): String {
        return when (raw.trim().uppercase().replace(".", "")) {
            "$", "USD" -> "USD"
            "€", "EUR" -> "EUR"
            "£", "GBP" -> "GBP"
            "₹", "RS", "INR", "/-" -> "INR"
            "CAD" -> "CAD"
            "AUD" -> "AUD"
            else -> "INR"
        }
    }

    private fun cleanMerchantName(raw: String): String {
        // Strip trailing punctuation and common boilerplate
        var cleaned = raw.replace(Regex("[.,;:#\\*]+$"), "").trim()
        
        // Handle VPA handles cleanly: e.g. swiggy@icici -> Swiggy, zomato@hdfcbank -> Zomato
        if (cleaned.contains("@")) {
            val prefix = cleaned.substringBefore("@").trim()
            if (prefix.length in 2..20 && !prefix.matches(Regex("^\\d+$"))) {
                cleaned = prefix
            }
        }

        if (cleaned.isBlank() || cleaned.equals("Unknown Merchant", ignoreCase = true)) {
            return "General Purchase"
        }

        // If ALL-CAPS, convert to Title Case (e.g. STARBUCKS -> Starbucks)
        if (cleaned.all { it.isUpperCase() || !it.isLetter() }) {
            cleaned = cleaned.lowercase().split(" ").joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
        }
        return cleaned
    }

    private fun String.containsAny(vararg keywords: String): Boolean =
        keywords.any { this.contains(it, ignoreCase = true) }
}
