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

    private val otpPattern = Pattern.compile(
        "(?i)(otp|one[-\\s]?time[-\\s]?password|verification[-\\s]?code|\\bcode\\b[:\\s]+\\d{4,8})"
    )

    private val promoPattern = Pattern.compile(
        "(?i)(congratulations|exclusive offer|claim now|discount|coupon|pre-approved|apply now|win cash)"
    )

    private val balanceCheckPattern = Pattern.compile(
        "(?i)(balance as of|statement available|e-statement|login detected|password changed)"
    )

    // Debit patterns
    private val debitKeywords = Pattern.compile(
        "(?i)\\b(debited|spent|paid|withdrawn|charged|purchase|sent|deducted|transfer to)\\b"
    )

    // Credit patterns
    private val creditKeywords = Pattern.compile(
        "(?i)\\b(credited|deposited|refunded|cashback|received|added to account)\\b"
    )

    // Amount patterns ($12.50, USD 42.00, Rs. 500, INR 1,200.50, €30.00, £15.20)
    private val amountPattern = Pattern.compile(
        "(?i)(?:([$€£₹]|USD|EUR|GBP|INR|CAD|AUD|Rs\\.?)\\s*([0-9,]+(?:\\.[0-9]{1,2})?))|([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(USD|EUR|GBP|INR|CAD|AUD)"
    )

    // Merchant extraction following prepositions (at Starbucks, to Whole Foods, for Uber, from Amazon)
    private val merchantPrepositionPattern = Pattern.compile(
        "(?i)\\b(?:at|in|vpa|from|(?:for(?!\\s*(?:[$€£₹]|USD|EUR|GBP|INR|CAD|AUD|Rs\\.?|\\d)))|(?:to(?!\\s*(?:acct|a/c|account|card|\\*\\*|xx|\\d))))\\s+([A-Za-z0-9\\s\\.\\&\\*\\-]{2,30}?)(?:\\s+(?:on|using|via|ref|rrn|utr|bal|avbl|acct|ending|card)\\b|[.,;]|$)"
    )

    // Account last 4 digits (ending 1234, acct XX1234, card **1234, card ending in 1234)
    private val accountLast4Pattern = Pattern.compile(
        "(?i)(?:acct|account|a/c|card|ending|xx|\\*\\*)[\\s:]*(?:(?:ending|no\\.?|#)[\\s:]*)?(?:(?:in|with)[\\s:]*)?[xX\\*]*(\\d{4})\\b"
    )

    // Transaction reference ID / UTR
    private val referencePattern = Pattern.compile(
        "(?i)(?:ref|rrn|utr|txn)[\\s:#]*([A-Za-z0-9]{6,20})"
    )

    fun isFinancialAlert(body: String): Boolean {
        if (body.isBlank()) return false
        if (otpPattern.matcher(body).find()) return false
        if (promoPattern.matcher(body).find()) return false
        if (balanceCheckPattern.matcher(body).find() && !debitKeywords.matcher(body).find() && !creditKeywords.matcher(body).find()) return false

        val hasAmount = amountPattern.matcher(body).find()
        val hasType = debitKeywords.matcher(body).find() || creditKeywords.matcher(body).find()
        return hasAmount && hasType
    }

    fun parse(
        body: String,
        receivedTimestamp: Long = System.currentTimeMillis(),
        sourceChannel: SourceChannel = SourceChannel.SMS
    ): ParsedTransactionCandidate? {
        if (!isFinancialAlert(body)) return null

        // 1. Extract Amount & Currency
        val amountMatcher = amountPattern.matcher(body)
        var parsedAmount = 0.0
        var parsedCurrency = "USD"

        if (amountMatcher.find()) {
            val symbolOrCode1 = amountMatcher.group(1)
            val amountStr1 = amountMatcher.group(2)
            val amountStr2 = amountMatcher.group(3)
            val symbolOrCode2 = amountMatcher.group(4)

            val rawAmount = (amountStr1 ?: amountStr2)?.replace(",", "")
            parsedAmount = rawAmount?.toDoubleOrNull() ?: return null

            val rawCurrency = symbolOrCode1 ?: symbolOrCode2 ?: "USD"
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
        return when (raw.trim().uppercase()) {
            "$", "USD" -> "USD"
            "€", "EUR" -> "EUR"
            "£", "GBP" -> "GBP"
            "₹", "RS", "RS.", "INR" -> "INR"
            "CAD" -> "CAD"
            "AUD" -> "AUD"
            else -> "USD"
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
