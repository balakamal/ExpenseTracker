package com.innovatex.expensetracker.domain.usecase

import com.innovatex.expensetracker.domain.model.Transaction
import com.innovatex.expensetracker.domain.model.TransactionDuplicateMatcher
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
        toleranceWindowMs: Long = 15 * 60 * 1000L
    ): DeduplicationResult {
        val searchWindow = maxOf(toleranceWindowMs, 30 * 60 * 1000L)
        val potentialDuplicates = transactionRepository.findPotentialDuplicates(
            startTime = candidate.timestamp - searchWindow,
            endTime = candidate.timestamp + searchWindow,
            amount = candidate.amount,
            currency = candidate.currency
        )
        val matchedExisting = potentialDuplicates.firstOrNull { existing ->
            if (!existing.isValidTransaction || existing.userOverrideValid == true) return@firstOrNull false
            existing.id != candidate.id && TransactionDuplicateMatcher.isLikelyDuplicate(
                existing,
                candidate,
                maxTimeDifferenceMs = searchWindow
            )
        }

        if (matchedExisting == null) {
            transactionRepository.saveTransaction(candidate)
            return DeduplicationResult.NewTransaction(candidate)
        }

        val merged = matchedExisting.copy(
            currency = candidate.currency,
            merchantClean = pickBestMerchant(matchedExisting.merchantClean, candidate.merchantClean),
            merchantRaw = pickBestMerchant(matchedExisting.merchantRaw, candidate.merchantRaw),
            categoryId = if (matchedExisting.categoryId != "general") matchedExisting.categoryId else candidate.categoryId,
            sources = (matchedExisting.sources + candidate.sources).distinct(),
            isDeduplicated = true,
            referenceId = matchedExisting.referenceId ?: candidate.referenceId,
            accountLast4 = matchedExisting.accountLast4 ?: candidate.accountLast4,
            rawSnippet = matchedExisting.rawSnippet ?: candidate.rawSnippet,
            notes = matchedExisting.notes ?: candidate.notes,
            aiClassification = matchedExisting.aiClassification ?: candidate.aiClassification
        )
        transactionRepository.updateTransaction(merged)
        return DeduplicationResult.Merged(merged)
    }

    private fun pickBestMerchant(first: String, second: String): String {
        val generic = setOf("general purchase", "unknown merchant", "pos purchase", "pos txn", "bank alert")
        val cleanFirst = first.trim()
        val cleanSecond = second.trim()
        if (cleanFirst.lowercase() in generic && cleanSecond.lowercase() !in generic) return cleanSecond
        if (cleanSecond.lowercase() in generic && cleanFirst.lowercase() !in generic) return cleanFirst
        return if (cleanFirst.length >= cleanSecond.length) cleanFirst else cleanSecond
    }
}
