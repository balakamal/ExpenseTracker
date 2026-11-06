package com.innovatex.spendwell.transactionengine.stages

import com.innovatex.spendwell.database.SpendwellDatabase
import com.innovatex.spendwell.database.entity.AccountEntity
import com.innovatex.spendwell.database.entity.DuplicateCandidateEntity
import com.innovatex.spendwell.database.entity.MerchantEntity
import com.innovatex.spendwell.database.entity.TransactionClassificationEntity
import com.innovatex.spendwell.database.entity.TransactionEntity
import com.innovatex.spendwell.database.entity.TransactionSourceEventEntity
import com.innovatex.spendwell.domain.model.FinancialNature
import com.innovatex.spendwell.domain.model.Transaction
import com.innovatex.spendwell.domain.model.TransactionStatus
import com.innovatex.spendwell.domain.model.TransactionType
import com.innovatex.spendwell.transactionengine.model.EngineResult
import com.innovatex.spendwell.transactionengine.model.ExtractedFinancialInfo
import com.innovatex.spendwell.transactionengine.model.NormalizedMessage
import org.json.JSONArray
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID

object Stage9DuplicateCandidateDetection {

    suspend fun evaluate(
        db: SpendwellDatabase,
        info: ExtractedFinancialInfo
    ): DuplicateResolution {
        val txDao = db.transactionDao()

        // 1. Check by Reference ID or UTR (Deterministic match)
        if (!info.referenceId.isNullOrBlank() || !info.utrNumber.isNullOrBlank()) {
            val matches = txDao.findCandidatesByUtrOrRef(info.referenceId, info.utrNumber)
            val exactMatch = matches.firstOrNull()
            if (exactMatch != null) {
                // Exact reference match: same financial event received via multiple channels
                return DuplicateResolution.ExactMatch(exactMatch)
            }
        }

        // 2. Fuzzy window check: amount matches within 10-minute window
        val windowMs = 10 * 60 * 1000L
        val startTime = info.timestamp - windowMs
        val endTime = info.timestamp + windowMs
        val candidates = txDao.findCandidatesByAmountAndWindow(info.amount.toPaisa(), startTime, endTime)

        val potentialDuplicate = candidates.firstOrNull { existing ->
            existing.accountLast4Matches(info.accountLast4) ||
                    existing.merchantMatches(info.normalizedMerchant)
        }

        if (potentialDuplicate != null) {
            // Found a candidate that needs user review rather than automatic destructive merge
            return DuplicateResolution.CandidateForReview(potentialDuplicate)
        }

        return DuplicateResolution.Unique
    }

    private fun TransactionEntity.accountLast4Matches(last4: String?): Boolean {
        if (last4 == null || this.accountId == null) return false
        return this.accountId.endsWith(last4)
    }

    private fun TransactionEntity.merchantMatches(normalizedMerchant: String): Boolean {
        return this.description.contains(normalizedMerchant, ignoreCase = true)
    }
}

sealed class DuplicateResolution {
    data class ExactMatch(val existingTransaction: TransactionEntity) : DuplicateResolution()
    data class CandidateForReview(val existingTransaction: TransactionEntity) : DuplicateResolution()
    object Unique : DuplicateResolution()
}

object Stage10TransactionLifecycleResolution {

    suspend fun resolveLifecycle(
        db: SpendwellDatabase,
        info: ExtractedFinancialInfo
    ): LifecycleResolution {
        if (!info.isRefund && !info.isReversal) {
            return LifecycleResolution.Standard
        }

        val txDao = db.transactionDao()
        // Find recent debit for the same merchant or matching reference within 30 days
        val windowMs = 30L * 24 * 60 * 60 * 1000L
        val startTime = info.timestamp - windowMs
        val recentDebits = txDao.getTransactionsByRange(startTime, info.timestamp)
            .filter { it.type == TransactionType.DEBIT.name }

        val originalTx = recentDebits.firstOrNull {
            it.description.contains(info.normalizedMerchant, ignoreCase = true) ||
                    (info.referenceId != null && it.referenceId == info.referenceId)
        }

        return if (originalTx != null) {
            LifecycleResolution.LinkedRefund(originalTx.id)
        } else {
            LifecycleResolution.Standard
        }
    }
}

sealed class LifecycleResolution {
    data class LinkedRefund(val originalTransactionId: String) : LifecycleResolution()
    object Standard : LifecycleResolution()
}

object Stage11CategoryClassification {

    private val MERCHANT_CATEGORY_MAP = mapOf(
        "Swiggy" to "cat_food",
        "Zomato" to "cat_food",
        "Starbucks" to "cat_food",
        "McDonald's" to "cat_food",
        "Domino's Pizza" to "cat_food",
        "Blinkit" to "cat_groceries",
        "Zepto" to "cat_groceries",
        "Amazon" to "cat_shopping",
        "Flipkart" to "cat_shopping",
        "Myntra" to "cat_shopping",
        "Uber" to "cat_transport",
        "Ola" to "cat_transport",
        "Rapido" to "cat_transport",
        "IRCTC" to "cat_transport",
        "MakeMyTrip" to "cat_transport",
        "Airtel" to "cat_utilities",
        "Jio" to "cat_utilities",
        "Tata Power" to "cat_utilities",
        "BESCOM" to "cat_utilities",
        "Netflix" to "cat_entertainment",
        "Spotify" to "cat_entertainment",
        "Disney+ Hotstar" to "cat_entertainment",
        "YouTube Premium" to "cat_entertainment",
        "Cult.fit" to "cat_health",
        "Zerodha" to "cat_investment",
        "Groww" to "cat_investment"
    )

    fun classify(info: ExtractedFinancialInfo): Pair<String, Float> {
        if (info.financialNature == FinancialNature.INCOME) {
            return Pair("cat_salary", 0.95f)
        }
        if (info.financialNature == FinancialNature.INVESTMENT_TRANSFER) {
            return Pair("cat_investment", 0.95f)
        }
        if (info.financialNature == FinancialNature.INTERNAL_TRANSFER ||
            info.financialNature == FinancialNature.CREDIT_CARD_BILL_PAYMENT
        ) {
            return Pair("cat_transfer", 0.90f)
        }

        val directCategory = MERCHANT_CATEGORY_MAP[info.normalizedMerchant]
        if (directCategory != null) {
            return Pair(directCategory, 0.98f)
        }

        return classify(info.normalizedMerchant)
    }

    suspend fun classify(
        db: SpendwellDatabase,
        merchantName: String
    ): Pair<String, Float> {
        val clean = merchantName.lowercase(Locale.ROOT)
        val merchant = db.merchantDao().findByCleanName(clean)
        if (merchant?.defaultCategoryId != null) {
            return Pair(merchant.defaultCategoryId, merchant.confidenceScore)
        }
        return classify(merchantName)
    }

    fun classify(merchantName: String): Pair<String, Float> {
        val clean = merchantName.lowercase(Locale.ROOT)

        // 2. High-precision deterministic merchant pattern matching
        return when {
            // Food & Dining
            clean.contains("swiggy") || clean.contains("zomato") || clean.contains("mcdonald") ||
                    clean.contains("domino") || clean.contains("starbucks") || clean.contains("kfc") ||
                    clean.contains("burger king") || clean.contains("subway") || clean.contains("restaurant") ||
                    clean.contains("cafe") || clean.contains("bakery") || clean.contains("pizza") -> Pair("cat_food", 0.95f)

            // Groceries & Daily Needs
            clean.contains("blinkit") || clean.contains("zepto") || clean.contains("instamart") ||
                    clean.contains("bigbasket") || clean.contains("dmart") || clean.contains("nature basket") ||
                    clean.contains("reliance fresh") || clean.contains("supermarket") || clean.contains("grocery") -> Pair("cat_groceries", 0.95f)

            // Shopping & E-Commerce
            clean.contains("amazon") || clean.contains("flipkart") || clean.contains("myntra") ||
                    clean.contains("ajio") || clean.contains("tata cliq") || clean.contains("nykaa") ||
                    clean.contains("zara") || clean.contains("h&m") || clean.contains("ikea") -> Pair("cat_shopping", 0.90f)

            // Travel & Mobility
            clean.contains("uber") || clean.contains("ola") || clean.contains("rapido") ||
                    clean.contains("irctc") || clean.contains("makemytrip") || clean.contains("goibibo") ||
                    clean.contains("indigo") || clean.contains("air india") || clean.contains("metro") ||
                    clean.contains("fuel") || clean.contains("petrol") || clean.contains("hpcl") || clean.contains("bpcl") || clean.contains("ioc") -> Pair("cat_transport", 0.95f)

            // Utilities & Bills
            clean.contains("electricity") || clean.contains("bescom") || clean.contains("airtel") ||
                    clean.contains("jio") || clean.contains("vodafone") || clean.contains("vi bill") ||
                    clean.contains("broadband") || clean.contains("act fiber") || clean.contains("piped gas") ||
                    clean.contains("water board") || clean.contains("recharge") -> Pair("cat_utilities", 0.95f)

            // Housing & Rent
            clean.contains("nobroker") || clean.contains("rent") || clean.contains("maintenance") ||
                    clean.contains("housing") || clean.contains("society") -> Pair("cat_housing", 0.90f)

            // Entertainment & OTT
            clean.contains("netflix") || clean.contains("spotify") || clean.contains("prime video") ||
                    clean.contains("hotstar") || clean.contains("youtube") || clean.contains("bookmyshow") ||
                    clean.contains("pvr") || clean.contains("inox") || clean.contains("cinema") -> Pair("cat_entertainment", 0.95f)

            // Healthcare & Pharmacy
            clean.contains("apollo") || clean.contains("pharmeasy") || clean.contains("1mg") ||
                    clean.contains("medplus") || clean.contains("hospital") || clean.contains("clinic") ||
                    clean.contains("diagnostic") || clean.contains("practo") -> Pair("cat_health", 0.95f)

            // Investments & Wealth
            clean.contains("zerodha") || clean.contains("groww") || clean.contains("upstox") ||
                    clean.contains("mutual fund") || clean.contains("kuvera") || clean.contains("et money") ||
                    clean.contains("indmoney") || clean.contains("smallcase") || clean.contains("camsonline") -> Pair("cat_investment", 0.98f)

            // Salary & Income
            clean.contains("salary") || clean.contains("payroll") || clean.contains("stipend") ||
                    clean.contains("dividend") || clean.contains("interest credited") -> Pair("cat_salary", 0.98f)

            // Default fallback category
            else -> Pair("cat_misc", 0.40f)
        }
    }
}

object Stage11DeterministicMerchantRules {

    suspend fun classify(
        db: SpendwellDatabase,
        merchantName: String
    ): Pair<String, Float> {
        return Stage11CategoryClassification.classify(db, merchantName)
    }
}

object Stage12LedgerPersistence {

    suspend fun persist(
        db: SpendwellDatabase,
        info: ExtractedFinancialInfo,
        normalized: NormalizedMessage,
        duplicateResolution: DuplicateResolution,
        lifecycleResolution: LifecycleResolution,
        categoryId: String,
        confidence: Float
    ): EngineResult {
        // Multi-channel idempotency hash
        val hash = sha256("${info.amount.toPaisa()}:${info.normalizedMerchant}:${info.timestamp}")

        // Check if exact source signal was already ingested
        val existingSourceEvent = db.transactionSourceEventDao().findByHash(hash)
        if (existingSourceEvent != null) {
            return EngineResult.Dropped(
                dropReason = com.innovatex.spendwell.transactionengine.model.DropReason.SPAM_OR_PROMOTIONAL,
                message = "Exact signal payload was previously ingested and verified"
            )
        }

        // If duplicate is an exact match (e.g. SMS + Push Notification for same transaction)
        if (duplicateResolution is DuplicateResolution.ExactMatch) {
            val existingTx = duplicateResolution.existingTransaction
            val sourceEvent = TransactionSourceEventEntity(
                id = UUID.randomUUID().toString(),
                canonicalTransactionId = existingTx.id,
                sourceType = normalized.signal.sourceType.name,
                senderAddress = normalized.signal.senderAddress,
                rawMessage = normalized.signal.rawText,
                receivedTimestamp = normalized.signal.timestamp,
                hashSignature = hash
            )
            db.transactionSourceEventDao().insert(sourceEvent)
            return EngineResult.Success(
                transaction = existingTx.toDomainMinimal(),
                isNewTransaction = false,
                isDuplicateMerged = true
            )
        }

        // Account association (find or auto-provision from detected signal)
        var account = info.accountLast4?.let { last4 ->
            if (info.bankName != null) {
                db.accountDao().findAccountByLast4AndBank(last4, info.bankName)
            } else {
                db.accountDao().findAccountByLast4(last4)
            }
        }
        if (account == null && info.accountLast4 != null) {
            val bank = info.bankName ?: "Bank"
            val isCard = normalized.normalizedText.contains("card", ignoreCase = true) ||
                    normalized.normalizedText.contains("credit", ignoreCase = true)
            val acctType = if (isCard) "CREDIT_CARD" else "SAVINGS"
            val acctName = "$bank ••${info.accountLast4}"
            val newAccount = AccountEntity(
                id = UUID.randomUUID().toString(),
                accountName = acctName,
                accountType = acctType,
                accountNumberLast4 = info.accountLast4,
                bankName = bank,
                currentBalancePaisa = 0L,
                currency = "INR",
                isPrimary = false,
                isActive = true
            )
            db.accountDao().insert(newAccount)
            account = newAccount
        }
        val accountId = account?.id

        // Merchant persistence
        val merchantId = UUID.nameUUIDFromBytes(info.normalizedMerchant.lowercase(Locale.ROOT).toByteArray()).toString()
        val existingMerchant = db.merchantDao().findByCleanName(info.normalizedMerchant)
        if (existingMerchant == null) {
            db.merchantDao().insert(
                MerchantEntity(
                    id = merchantId,
                    cleanName = info.normalizedMerchant,
                    defaultCategoryId = categoryId,
                    confidenceScore = confidence
                )
            )
        }

        val originalTxId = if (lifecycleResolution is LifecycleResolution.LinkedRefund) {
            lifecycleResolution.originalTransactionId
        } else null

        val isCandidateReview = duplicateResolution is DuplicateResolution.CandidateForReview
        val txStatus = if (isCandidateReview) {
            TransactionStatus.NEEDS_REVIEW
        } else {
            info.status
        }

        val newTransactionId = UUID.randomUUID().toString()
        val txEntity = TransactionEntity(
            id = newTransactionId,
            amountPaisa = info.amount.toPaisa(),
            currency = info.amount.currencyCode,
            type = info.transactionType.name,
            status = txStatus.name,
            financialNature = info.financialNature.name,
            timestamp = info.timestamp,
            accountId = accountId,
            toAccountId = null,
            merchantId = merchantId,
            categoryId = categoryId,
            description = info.normalizedMerchant,
            referenceId = info.referenceId,
            utrNumber = info.utrNumber,
            rawMessageSnippet = normalized.signal.rawText.take(120),
            isReviewed = !isCandidateReview,
            originalTransactionId = originalTxId,
            recurringPaymentId = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        db.transactionDao().insert(txEntity)

        // Record multi-signal source event
        val sourceEvent = TransactionSourceEventEntity(
            id = UUID.randomUUID().toString(),
            canonicalTransactionId = newTransactionId,
            sourceType = normalized.signal.sourceType.name,
            senderAddress = normalized.signal.senderAddress,
            rawMessage = normalized.signal.rawText,
            receivedTimestamp = normalized.signal.timestamp,
            hashSignature = hash
        )
        db.transactionSourceEventDao().insert(sourceEvent)

        // Update account balance if known
        if (accountId != null && info.status == TransactionStatus.SUCCESSFUL) {
            val delta = if (info.transactionType == TransactionType.CREDIT) {
                info.amount.toPaisa()
            } else {
                -info.amount.toPaisa()
            }
            db.accountDao().adjustBalance(accountId, delta)
        }

        // If duplicate candidate for review, store candidate record
        var candidateId: String? = null
        if (duplicateResolution is DuplicateResolution.CandidateForReview) {
            val dupEntity = duplicateResolution.existingTransaction
            candidateId = UUID.randomUUID().toString()
            val reasons = JSONArray().apply {
                put("Same amount ₹${info.amount.formatInr(false)}")
                put("Occurred within 10-minute window")
            }.toString()

            db.duplicateCandidateDao().insert(
                DuplicateCandidateEntity(
                    id = candidateId,
                    originalTransactionId = dupEntity.id,
                    duplicateTransactionId = newTransactionId,
                    similarityScore = 0.85f,
                    reasonsJson = reasons,
                    resolved = false
                )
            )

            return EngineResult.FlaggedForReview(
                transaction = txEntity.toDomainMinimal(),
                duplicateCandidateId = candidateId,
                reason = "Similar amount and timestamp to recent transaction"
            )
        }

        return EngineResult.Success(
            transaction = txEntity.toDomainMinimal(),
            isNewTransaction = true,
            linkedRefundToOriginalId = originalTxId
        )
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun TransactionEntity.toDomainMinimal(): Transaction {
        return Transaction(
            id = id,
            amount = com.innovatex.spendwell.common.Money.fromPaisa(amountPaisa, currency),
            type = TransactionType.valueOf(type),
            status = TransactionStatus.valueOf(status),
            financialNature = FinancialNature.valueOf(financialNature),
            timestamp = timestamp,
            accountId = accountId,
            merchantId = merchantId,
            categoryId = categoryId,
            description = description,
            referenceId = referenceId,
            utrNumber = utrNumber,
            rawMessageSnippet = rawMessageSnippet,
            isReviewed = isReviewed,
            originalTransactionId = originalTransactionId,
            createdAt = createdAt
        )
    }
}

typealias Stage12StatePersistence = Stage12LedgerPersistence
