package com.innovatex.expensetracker.ai

import com.innovatex.expensetracker.di.IoDispatcher
import com.innovatex.expensetracker.domain.model.Transaction
import com.innovatex.expensetracker.domain.model.TransactionDuplicateMatcher
import com.innovatex.expensetracker.domain.repository.TransactionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** LLM review for a small, locally prefiltered set of same-amount candidates. */
class AiDuplicateReview @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val llmClassifierEngine: LlmClassifierEngine,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun reviewUncertainDuplicates(maxReviews: Int = MAX_REVIEWS): Int = withContext(ioDispatcher) {
        if (!llmClassifierEngine.isGenerativeLlmReady()) return@withContext 0
        val groups = transactionRepository.getAllTransactions().first()
            .filter { it.isCountableTransaction && it.userOverrideValid != true }
            .groupBy { DuplicateGroupKey(it.amount.toLongCents(), it.currency, it.type.name) }
            .values
        var reviews = 0
        var markedDuplicates = 0
        val alreadyMarked = mutableSetOf<String>()

        reviewLoop@ for (group in groups) {
            val ordered = group.sortedBy { it.timestamp }
            for (firstIndex in ordered.indices) {
                val first = ordered[firstIndex]
                if (reviews >= maxReviews) break@reviewLoop
                if (first.id in alreadyMarked) continue
                for (secondIndex in firstIndex + 1 until ordered.size) {
                    val second = ordered[secondIndex]
                    val timeDifference = kotlin.math.abs(second.timestamp - first.timestamp)
                    if (timeDifference > MAX_TIME_GAP_MS) break
                    if (second.id in alreadyMarked || !isPlausiblePair(first, second)) continue

                    reviews++
                    val decision = try {
                        llmClassifierEngine.comparePotentialDuplicates(first, second)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        null
                    }
                    if (decision?.isSameTransaction != true || decision.confidence < MIN_CONFIDENCE) continue

                    val duplicate = chooseDuplicate(first, second) ?: continue
                    if (transactionRepository.markTransactionAsDuplicate(duplicate.id)) {
                        alreadyMarked += duplicate.id
                        markedDuplicates++
                    }
                    if (reviews >= maxReviews) break@reviewLoop
                }
            }
        }
        markedDuplicates
    }

    private fun isPlausiblePair(first: Transaction, second: Transaction): Boolean {
        if (first.currency != second.currency || first.type != second.type) return false
        if (kotlin.math.abs(first.amount - second.amount) >= 0.01) return false
        if (TransactionDuplicateMatcher.isLikelyDuplicate(first, second, MAX_TIME_GAP_MS)) return false

        val sameAccount = !first.accountLast4.isNullOrBlank() && first.accountLast4 == second.accountLast4
        val sourcesDiffer = first.sources.none { it in second.sources }
        val firstTokens = evidenceTokens(first)
        val secondTokens = evidenceTokens(second)
        val sharedTokens = firstTokens.intersect(secondTokens).size
        return sameAccount || sharedTokens >= 2 || (sourcesDiffer && sharedTokens >= 1)
    }

    private fun evidenceTokens(transaction: Transaction): Set<String> =
        "${transaction.merchantClean} ${transaction.merchantRaw} ${transaction.rawSnippet.orEmpty()}"
            .lowercase()
            .split(Regex("[^\\p{L}\\p{N}]+"))
            .filter { it.length >= 3 }
            .toSet()

    private fun chooseDuplicate(first: Transaction, second: Transaction): Transaction? {
        if (first.userOverrideValid == true) return second.takeUnless { it.userOverrideValid == true }
        if (second.userOverrideValid == true) return first
        val firstScore = evidenceScore(first)
        val secondScore = evidenceScore(second)
        return when {
            firstScore < secondScore -> first
            secondScore < firstScore -> second
            first.timestamp > second.timestamp -> first
            else -> second
        }
    }

    private fun evidenceScore(transaction: Transaction): Int =
        (if (!transaction.referenceId.isNullOrBlank()) 4 else 0) +
            (if (!transaction.accountLast4.isNullOrBlank()) 2 else 0) +
            (if (!transaction.rawSnippet.isNullOrBlank()) 1 else 0) +
            transaction.sources.distinct().size

    private fun Double.toLongCents(): Long = (this * 100).toLong()

    private data class DuplicateGroupKey(val amountCents: Long, val currency: String, val type: String)

    private companion object {
        const val MAX_REVIEWS = 32
        const val MIN_CONFIDENCE = 0.95f
        const val MAX_TIME_GAP_MS = 6 * 60 * 60 * 1000L
    }
}
