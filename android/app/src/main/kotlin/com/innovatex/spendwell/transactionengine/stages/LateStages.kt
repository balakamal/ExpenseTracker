package com.innovatex.spendwell.transactionengine.stages

import com.innovatex.spendwell.database.SpendwellDatabase
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
    object Standard : LifecycleResolution()
    data class LinkedRefund(val originalTransactionId: String) : LifecycleResolution()
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

        return Pair("cat_misc", 0.50f)
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
        val hash = sha256(normalized.signal.rawText + normalized.signal.senderAddress + normalized.signal.timestamp)

        // Check if exact source event hash already exists
        val existingEvent = db.transactionSourceEventDao().findByHash(hash)
        if (existingEvent != null) {
            // Already processed this exact signal
            val canonicalTx = existingEvent.canonicalTransactionId?.let { db.transactionDao().getTransactionById(it) }
            if (canonicalTx != null) {
                return EngineResult.Success(
                    transaction = canonicalTx.toDomainMinimal(),
                    isNewTransaction = false,
                    isDuplicateMerged = true
                )
            }
        }

        // Handle Exact Duplicate match: link source event to existing canonical transaction
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

        // Account association
        val account = info.accountLast4?.let { last4 ->
            if (info.bankName != null) {
                db.accountDao().findAccountByLast4AndBank(last4, info.bankName)
            } else {
                db.accountDao().findAccountByLast4(last4)
            }
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
