package com.innovatex.expensetracker.domain.model

/** Conservative local matching for two alerts that may describe one ledger transaction. */
object TransactionDuplicateMatcher {
    fun isLikelyDuplicate(
        first: Transaction,
        second: Transaction,
        maxTimeDifferenceMs: Long = 30 * 60 * 1000L
    ): Boolean {
        if (first.currency != second.currency || first.type != second.type) return false
        if (kotlin.math.abs(first.amount - second.amount) >= 0.01) return false

        val firstReference = first.referenceId.normalizedIdentifier()
        val secondReference = second.referenceId.normalizedIdentifier()
        val timeDifference = kotlin.math.abs(first.timestamp - second.timestamp)
        if (firstReference != null && firstReference == secondReference) {
            return timeDifference <= 7 * 24 * 60 * 60 * 1000L
        }
        if (timeDifference > maxTimeDifferenceMs) return false

        val merchantCompatible = merchantsCompatible(first, second)
        if (!merchantCompatible) return false

        val crossSource = first.sources.none { it in second.sources }
        val sameAccount = !first.accountLast4.isNullOrBlank() &&
            first.accountLast4 == second.accountLast4
        val sameMessage = normalizedMessage(first.rawSnippet) == normalizedMessage(second.rawSnippet) &&
            !first.rawSnippet.isNullOrBlank()

        // Require independent corroboration; amount and timing alone are never sufficient.
        return crossSource ||
            sameMessage ||
            (sameAccount && timeDifference <= 90_000L) ||
            (merchantIsExplicitlyEmbedded(first, second) && timeDifference <= 15 * 60 * 1000L)
    }

    private fun merchantsCompatible(first: Transaction, second: Transaction): Boolean {
        val firstMerchant = first.merchantClean.normalizeMerchant()
        val secondMerchant = second.merchantClean.normalizeMerchant()
        val generic = setOf("generalpurchase", "unknownmerchant", "pospurchase", "postxn", "bankalert", "purchase")
        val firstSpecific = firstMerchant !in generic && firstMerchant.length >= 4
        val secondSpecific = secondMerchant !in generic && secondMerchant.length >= 4

        if (firstSpecific && secondSpecific &&
            (firstMerchant == secondMerchant ||
                (minOf(firstMerchant.length, secondMerchant.length) >= 4 &&
                    (firstMerchant.contains(secondMerchant) || secondMerchant.contains(firstMerchant))))
        ) return true

        val firstRaw = first.rawSnippet.orEmpty().normalizeMerchant()
        val secondRaw = second.rawSnippet.orEmpty().normalizeMerchant()
        return (firstSpecific && secondRaw.contains(firstMerchant)) ||
            (secondSpecific && firstRaw.contains(secondMerchant))
    }

    private fun merchantIsExplicitlyEmbedded(first: Transaction, second: Transaction): Boolean {
        val firstMerchant = first.merchantClean.normalizeMerchant()
        val secondMerchant = second.merchantClean.normalizeMerchant()
        val generic = setOf("generalpurchase", "unknownmerchant", "pospurchase", "postxn", "bankalert", "purchase")
        val firstRaw = first.rawSnippet.orEmpty().normalizeMerchant()
        val secondRaw = second.rawSnippet.orEmpty().normalizeMerchant()
        return (firstMerchant in generic && secondMerchant.length >= 4 && firstRaw.contains(secondMerchant)) ||
            (secondMerchant in generic && firstMerchant.length >= 4 && secondRaw.contains(firstMerchant))
    }

    private fun String.normalizeMerchant(): String =
        lowercase().filter { it.isLetterOrDigit() }

    private fun String?.normalizedIdentifier(): String? =
        this?.trim()?.takeIf { it.isNotEmpty() }?.lowercase()

    private fun normalizedMessage(value: String?): String =
        value.orEmpty().lowercase().filter { it.isLetterOrDigit() }
}
