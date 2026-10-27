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

    // 2. Pre-approved loans, credit limit increase, CIBIL score marketing
    private val loanMarketingPattern = Pattern.compile(
        "(?i)\\b(?:pre-approved|pre\\s+approved|pre-qualified|pre\\s+qualified|instant\\s+loan|personal\\s+loan|home\\s+loan|car\\s+loan|business\\s+loan|loan\\s+offer|loan\\s+of|apply\\s+now|apply\\s+for\\s+(?:a\\s+)?loan|check\\s+eligibility|eligible\\s+for\\s+(?:a\\s+)?(?:loan|credit\\s+card)|credit\\s+limit\\s+(?:is|increased|enhanced)|limit\\s+increased\\s+to|enhance\\s+your\\s+limit|credit\\s+score|cibil\\s+score|instant\\s+cash|disbursed\\s+to\\s+(?:your\\s+)?(?:offer\\s+)?wallet)\\b"
    )

    // 3. Promotional offers, discounts, coupons, cashback schemes, gaming/lottery
    private val promoPattern = Pattern.compile(
        "(?i)\\b(?:exclusive\\s+offer|special\\s+offer|claim\\s+now|claim\\s+your|claim\\s+reward|discount|coupon|voucher|promo\\s+code|use\\s+code|flat\\s+\\d+%|up\\s+to\\s+\\d+%|win\\s+cash|win\\s+up\\s+to|lucky\\s+winner|play\\s+(?:and|&)\\s+win|rummy|dream11|jackpot|hurry\\b|offer\\s+valid|sale\\s+is\\s+live|mega\\s+sale|on\\s+your\\s+next\\s+purchase|minimum\\s+purchase|bonus\\s+points|redeem\\s+now)\\b"
    )

    // 4. Bill due / Upcoming payment demands (Prospective/unpaid notices)
    private val billDuePattern = Pattern.compile(
        "(?i)\\b(?:bill\\s+is\\s+due|payment\\s+due|due\\s+date|due\\s+on\\s+\\d|pay\\s+before|pay\\s+by\\s+\\d|to\\s+avoid\\s+(?:late\\s+fee|disconnection|interruption)|minimum\\s+amount\\s+due|min\\s+due|total\\s+amount\\s+due|unpaid\\s+bill|bill\\s+generated|statement\\s+generated)\\b"
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

    // Specific Debit patterns: completed past-tense action
    private val debitKeywords = Pattern.compile(
        "(?i)\\b(debited|spent|paid|withdrawn|charged|sent\\s+to|deducted|transfer\\s+to|transferred\\s+to|auto-debited|auto\\s+debit|autopay|dr\\.?\\s+with|dr\\.?\\s+of|dr\\b)\\b"
    )

    // Specific Credit patterns: completed past-tense action
    private val creditKeywords = Pattern.compile(
        "(?i)\\b(credited|deposited|refunded|received\\s+from|added\\s+to\\s+(?:your\\s+)?(?:account|a/c|wallet)|salary\\s+credited|cr\\.?\\s+with|cr\\.?\\s+of|cr\\b)\\b"
    )

    // Amount patterns ($12.50, USD 42.00, Rs. 500, INR 1,200.50, ₹500, 500/-, etc.)
    private val amountPattern = Pattern.compile(
        "(?i)(?:([$€£₹]|USD|EUR|GBP|INR|CAD|AUD|Rs\\.?|INR\\.?)\\s*([0-9,]+(?:\\.[0-9]{1,2})?))|([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(USD|EUR|GBP|INR|CAD|AUD|Rs\\.?|INR\\.?|/-)|(?:(?:debited|credited|paid|sent|spent)\\s+(?:by|for|with|of)?\\s*(?:([$€£₹]|Rs\\.?|INR\\.?)\\s*)?([0-9,]+(?:\\.[0-9]{1,2})?))"
    )

    // Merchant extraction following prepositions
    private val merchantPrepositionPattern = Pattern.compile(
        "(?i)\\b(?:at|in|vpa|from|towards|(?:for(?!\\s*(?:[$€£₹]|USD|EUR|GBP|INR|CAD|AUD|Rs\\.?|INR\\.?|\\d)))|(?:to(?!\\s*(?:acct|a/c|account|card|\\*\\*|xx|\\d))))\\s+([A-Za-z0-9\\s\\.\\&\\*\\-]{2,30}?)(?:\\s+(?:on|using|via|ref|rrn|utr|bal|avbl|acct|ending|card)\\b|[.,;]|$)"
    )

    // Account last 4 digits (ending 1234, acct XX1234, card **1234, A/c ...4321, etc.)
    private val accountLast4Pattern = Pattern.compile(
        "(?i)(?:acct|account|a/c|card|ending|xx|\\*\\*|\\.\\.\\.)[\\s:]*(?:(?:ending|no\\.?|#)[\\s:]*)?(?:(?:in|with)[\\s:]*)?[xX\\*\\.]*(\\d{4})\\b"
    )

    // Transaction reference ID / UTR / UPI Ref / RRN
    private val referencePattern = Pattern.compile(
        "(?i)(?:ref|rrn|utr|txn|upi\\s*ref)[\\s:#]*([A-Za-z0-9]{6,25})"
    )

    // Banking entity identifier
    private val bankingEntityPattern = Pattern.compile(
        "(?i)\\b(bank|hdfc|sbi|icici|axis|kotak|idfc|paytm|gpay|phonepe|cred|amex|citi|pnb|boi|canara|yes\\s*bank|indusind|chase|wellsfargo|bofa|barclays|paypal|vpa|wallet|upi)\\b"
    )

    fun isPersonalPhoneNumber(sender: String?): Boolean {
        if (sender.isNullOrBlank()) return false
        val clean = sender.trim().removePrefix("+")
        // Discard 10 to 12 digit personal mobile phone numbers (e.g. +919876543210, 9876543210)
        return clean.matches(Regex("^(?:91)?[6-9]\\d{9}$")) || clean.matches(Regex("^\\d{10,12}$"))
    }

    fun isFinancialAlert(body: String, sender: String? = null): Boolean {
        if (body.isBlank()) return false
        if (otpPattern.matcher(body).find()) return false

        // 1. Reject if sender is a personal mobile number
        if (isPersonalPhoneNumber(sender)) return false

        // 2. Reject definitive non-transactional marketing / notices
        if (loanMarketingPattern.matcher(body).find()) return false
        if (billDuePattern.matcher(body).find()) return false
        if (paymentRequestPattern.matcher(body).find()) return false
        if (telecomPattern.matcher(body).find()) return false

        val hasAmount = amountPattern.matcher(body).find()
        val hasDebit = debitKeywords.matcher(body).find()
        val hasCredit = creditKeywords.matcher(body).find()
        val hasType = hasDebit || hasCredit

        if (!hasAmount || !hasType) return false

        // 3. Balance checks without genuine debit/credit verbs are dropped
        if (balanceCheckPattern.matcher(body).find() && !hasDebit && !hasCredit) return false

        // 4. Financial Anchor Verification:
        // A genuine transaction must possess an account mask, a transaction reference ID,
        // or a known banking sender entity.
        val hasAccount = accountLast4Pattern.matcher(body).find()
        val hasRef = referencePattern.matcher(body).find()
        val hasBankEntity = bankingEntityPattern.matcher(body).find() ||
            (sender != null && bankingEntityPattern.matcher(sender).find())

        val hasAnchor = hasAccount || hasRef || hasBankEntity

        // If message has promotional keywords, it MUST have strong anchors (account or ref)
        // to be recognized as a genuine bank transaction with a promo footer.
        if (promoPattern.matcher(body).find()) {
            if (!hasAccount && !hasRef) return false
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
}
