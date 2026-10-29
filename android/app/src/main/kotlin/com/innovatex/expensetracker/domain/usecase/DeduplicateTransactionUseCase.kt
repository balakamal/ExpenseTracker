package com.innovatex.expensetracker.domain.usecase

import com.innovatex.expensetracker.domain.model.Transaction
import com.innovatex.expensetracker.domain.repository.TransactionRepository
import javax.inject.Inject

sealed interface DeduplicationResult {
    data class Merged(val updatedTransaction: Transaction) : DeduplicationResult
    data class NewTransaction(val transaction: Transaction) : DeduplicationResult
}

class DeduplicateTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(
        candidate: Transaction,
        toleranceWindowMs: Long = 15 * 60 * 1000L // 15 minutes
    ): DeduplicationResult {
        // Expand search window to 30 minutes to account for slight notification delays
        val searchWindow = maxOf(toleranceWindowMs, 30 * 60 * 1000L)
        val startTime = candidate.timestamp - searchWindow
        val endTime = candidate.timestamp + searchWindow

        val potentialDuplicates = transactionRepository.findPotentialDuplicates(
            startTime = startTime,
            endTime = endTime,
            amount = candidate.amount,
            currency = candidate.currency
        )

        val matchedExisting = potentialDuplicates.firstOrNull { existing ->
            if (existing.id == candidate.id) return@firstOrNull false
            if (existing.type != candidate.type) return@firstOrNull false

            // Rule 1: Exact Reference ID (UTR / RRN / UPI Ref) match
            val refMatch = !existing.referenceId.isNullOrBlank() &&
                !candidate.referenceId.isNullOrBlank() &&
                existing.referenceId.equals(candidate.referenceId, ignoreCase = true)
            if (refMatch) return@firstOrNull true

            // Rule 2: Same Account + Same Amount within ±30 min
            val acctMatch = !existing.accountLast4.isNullOrBlank() &&
                !candidate.accountLast4.isNullOrBlank() &&
                existing.accountLast4 == candidate.accountLast4 &&
                kotlin.math.abs(existing.timestamp - candidate.timestamp) <= 30 * 60 * 1000L
            if (acctMatch) return@firstOrNull true

            // Rule 3: Cross-Channel Corroboration (Bank Alert + Merchant Order Alert)
            val isCrossChannel = (existing.sources + candidate.sources).distinct().size > 1 ||
                (existing.sender != null && candidate.sender != null && !existing.sender.equals(candidate.sender, ignoreCase = true))
            val timeClose = kotlin.math.abs(existing.timestamp - candidate.timestamp) <= 15 * 60 * 1000L
            if (timeClose && (isMerchantCrossCompatible(existing, candidate) ||
                    (isCrossChannel && (existing.accountLast4 != null || candidate.accountLast4 != null)))
            ) {
                return@firstOrNull true
            }

            // Rule 4: Similar merchant clean name within ±15 min
            val merchantMatch = isMerchantSimilar(existing.merchantClean, candidate.merchantClean) &&
                kotlin.math.abs(existing.timestamp - candidate.timestamp) <= 15 * 60 * 1000L
            if (merchantMatch) return@firstOrNull true

            // Rule 5: Time very close (<= 90 seconds) for identical amounts
            val timeVeryClose = kotlin.math.abs(existing.timestamp - candidate.timestamp) <= 90_000L
            timeVeryClose
        }

        return if (matchedExisting != null) {
            val combinedSources = (matchedExisting.sources + candidate.sources).distinct()
            val bestMerchantClean = pickBestMerchant(matchedExisting.merchantClean, candidate.merchantClean)
            val bestMerchantRaw = pickBestMerchant(matchedExisting.merchantRaw, candidate.merchantRaw)
            val bestCategoryId = if (matchedExisting.categoryId != "general") matchedExisting.categoryId else candidate.categoryId

            val merged = matchedExisting.copy(
                currency = candidate.currency,
                merchantClean = bestMerchantClean,
                merchantRaw = bestMerchantRaw,
                categoryId = bestCategoryId,
                sources = combinedSources,
                isDeduplicated = true,
                referenceId = matchedExisting.referenceId ?: candidate.referenceId,
                accountLast4 = matchedExisting.accountLast4 ?: candidate.accountLast4,
                rawSnippet = matchedExisting.rawSnippet ?: candidate.rawSnippet,
                notes = matchedExisting.notes ?: candidate.notes,
                aiClassification = matchedExisting.aiClassification ?: candidate.aiClassification
            )
            transactionRepository.updateTransaction(merged)
            DeduplicationResult.Merged(merged)
        } else {
            transactionRepository.saveTransaction(candidate)
            DeduplicationResult.NewTransaction(candidate)
        }
    }

    private fun isMerchantSimilar(m1: String, m2: String): Boolean {
        val s1 = m1.trim().lowercase()
        val s2 = m2.trim().lowercase()
        if (s1 == s2) return true
        if (s1.contains(s2) || s2.contains(s1)) return true
        return false
    }

    private fun isMerchantCrossCompatible(e1: Transaction, e2: Transaction): Boolean {
        val s1 = e1.merchantClean.trim().lowercase()
        val s2 = e2.merchantClean.trim().lowercase()
        if (isMerchantSimilar(s1, s2)) return true

        val raw1 = e1.rawSnippet?.lowercase() ?: ""
        val raw2 = e2.rawSnippet?.lowercase() ?: ""
        if (s1.length >= 3 && raw2.contains(s1)) return true
        if (s2.length >= 3 && raw1.contains(s2)) return true

        val sender1 = e1.sender?.lowercase() ?: ""
        val sender2 = e2.sender?.lowercase() ?: ""
        if (s1.length >= 3 && sender2.contains(s1)) return true
        if (s2.length >= 3 && sender1.contains(s2)) return true

        return false
    }

    private fun pickBestMerchant(m1: String, m2: String): String {
        val generic = setOf("general purchase", "unknown merchant", "pos purchase", "pos txn", "bank alert")
        val clean1 = m1.trim()
        val clean2 = m2.trim()
        if (clean1.lowercase() in generic && clean2.lowercase() !in generic) return clean2
        if (clean2.lowercase() in generic && clean1.lowercase() !in generic) return clean1
        return if (clean1.length >= clean2.length) clean1 else clean2
    }
}
