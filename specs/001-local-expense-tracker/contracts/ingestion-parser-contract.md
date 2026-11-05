# Ingestion & Parser Contract

**Feature**: `001-local-expense-tracker`  
**Type**: Internal Domain / Data Pipeline Contract

---

## 1. Raw Event Ingestion

### 1.1 Ingestion Event Payload
```kotlin
data class RawIngestionEvent(
    val sourceChannel: SourceChannel, // SMS or EMAIL
    val senderIdentifier: String,     // e.g. "HDFCBK", "alerts@chase.com", "Google Pay"
    val fullMessageBody: String,      // Raw text content of SMS or notification subtext
    val receivedTimestamp: Long,      // Epoch millisecond when received by device
    val notificationTitle: String? = null // Notification title (for Email/Push)
)
```

---

## 2. Parser Engine Interface

```kotlin
interface TransactionParserEngine {
    /**
     * Determines if the message represents an actionable financial transaction.
     * Rejects OTPs, promotional campaigns, low balance warnings without debits.
     */
    fun isFinancialAlert(event: RawIngestionEvent): Boolean

    /**
     * Parses the raw message into a structured parsed transaction candidate.
     * Returns null if extraction of critical triad (amount, type, timestamp) fails.
     */
    fun parse(event: RawIngestionEvent): ParsedTransactionCandidate?
}

data class ParsedTransactionCandidate(
    val amount: Double,
    val currency: String,
    val type: TransactionType, // DEBIT or CREDIT
    val transactionTimestamp: Long,
    val rawMerchant: String,
    val cleanMerchant: String,
    val referenceId: String?,
    val accountLast4: String?,
    val sourceChannel: SourceChannel,
    val rawSnippet: String
)
```

---

## 3. Financial Extraction Heuristics & Regex Contracts

### 3.1 Non-Financial / Spam Blacklist Filter
Any message matching these regex patterns is immediately dropped:
- OTP patterns: `(?i)(otp|one[-\s]?time[-\s]?password|verification[-\s]?code|\bcode\b[:\s]+\d{4,8})`
- Promotional / Offer patterns: `(?i)(congratulations|exclusive offer|claim now|discount|coupon|pre-approved|apply now)`
- Informational non-debited: `(?i)(balance as of|statement available|e-statement|login detected|password changed)`

### 3.2 Amount & Currency Extraction
- Standard formats:
  - Currency symbols: `[$€£₹]\s*([0-9,]+(?:\.[0-9]{2})?)`
  - Currency codes: `\b(USD|EUR|GBP|INR|CAD|AUD)\s*([0-9,]+(?:\.[0-9]{2})?)`
  - Bank abbreviations: `\b(?:Rs\.?|INR)\s*([0-9,]+(?:\.[0-9]{2})?)`

### 3.3 Transaction Type Classification
- **Debit Keywords**: `(?i)\b(debited|spent|paid|withdrawn|charged|purchase|sent|deducted|transfer to)\b`
- **Credit Keywords**: `(?i)\b(credited|deposited|refunded|cashback|received|added to account)\b`

### 3.4 Merchant Extraction
- Extracts text following prepositions: `(?i)\b(?:at|in|vpa|from|(?:for(?!\s*(?:[$€£₹]|USD|EUR|GBP|INR|CAD|AUD|Rs\.?|\d)))|(?:to(?!\s*(?:acct|a/c|account|card|\*\*|xx|\d))))\s+([A-Za-z0-9\s\.\&\*\-]{2,30}?)(?:\s+(?:on|using|via|ref|rrn|utr|bal|avbl|acct|ending|card)\b|[.,;]|$)`
- Cleans and normalizes strings: strips trailing punctuation, converts ALL-CAPS to Title Case (e.g. `STARBUCKS #1204` -> `Starbucks`).

### 3.5 Account Last 4 Digits Extraction
- Matches account references: `(?i)(?:acct|account|a/c|card|ending|xx|\*\*)[\\s:]*(?:(?:ending|no\.?|#)[\s:]*)?(?:(?:in|with)[\s:]*)?[xX\*]*(\d{4})\b`

