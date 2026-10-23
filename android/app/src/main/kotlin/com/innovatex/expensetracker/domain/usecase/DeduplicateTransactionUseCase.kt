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
        toleranceWindowMs: Long = 10 * 60 * 1000L // 10 minutes
    ): DeduplicationResult {
        val startTime = candidate.timestamp - toleranceWindowMs
        val endTime = candidate.timestamp + toleranceWindowMs

        val potentialDuplicates = transactionRepository.findPotentialDuplicates(
            startTime = startTime,
            endTime = endTime,
            amount = candidate.amount,
            currency = candidate.currency
        )

        // Find corroborated match
        val matchedExisting = potentialDuplicates.firstOrNull { existing ->
            // Don't match against itself if already in DB
            if (existing.id == candidate.id) return@firstOrNull false

            val sameType = existing.type == candidate.type
            val refMatch = !existing.referenceId.isNullOrBlank() &&
                existing.referenceId.equals(candidate.referenceId, ignoreCase = true)
            val acctMatch = !existing.accountLast4.isNullOrBlank() &&
                existing.accountLast4 == candidate.accountLast4
            val merchantMatch = isMerchantSimilar(existing.merchantClean, candidate.merchantClean)
            val timeVeryClose = kotlin.math.abs(existing.timestamp - candidate.timestamp) <= 60_000L

            sameType && (refMatch || acctMatch || merchantMatch || timeVeryClose)
        }

        return if (matchedExisting != null) {
            val combinedSources = (matchedExisting.sources + candidate.sources).distinct()
            val merged = matchedExisting.copy(
                currency = candidate.currency,
                sources = combinedSources,
                isDeduplicated = true,
                referenceId = matchedExisting.referenceId ?: candidate.referenceId,
                accountLast4 = matchedExisting.accountLast4 ?: candidate.accountLast4,
                rawSnippet = matchedExisting.rawSnippet ?: candidate.rawSnippet
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
}
