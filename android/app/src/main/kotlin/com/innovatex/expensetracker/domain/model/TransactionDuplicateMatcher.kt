package com.innovatex.expensetracker.domain.model

/** 
 * Local multi-day matching for financial alerts describing the same underlying transaction.
 * Supports UPI delays (today vs tomorrow), cross-channel alerts (UPI app + Bank SMS),
 * carrier retries, and multi-day credit card payment settlements (up to 5 days).
 */
object TransactionDuplicateMatcher {

    fun isLikelyDuplicate(
        first: Transaction,
        second: Transaction,
        maxTimeDifferenceMs: Long = 72 * 60 * 60 * 1000L // 72 hours / 3 days default
    ): Boolean {
        if (first.currency != second.currency) return false
        if (kotlin.math.abs(first.amount - second.amount) >= 0.01) return false

        val timeDifference = kotlin.math.abs(first.timestamp - second.timestamp)

        // 1. Credit Card Bill Settlement Matcher (up to 5 days, even across DEBIT vs CREDIT confirmations)
        if (isCreditCardSettlementPair(first, second)) {
            if (timeDifference <= 5 * 24 * 60 * 60 * 1000L) {
                return true
            }
        }

        // For all other transactions, payment direction must match (DEBIT vs DEBIT or CREDIT vs CREDIT)
        if (first.type != second.type) return false

        // 2. Exact Reference / UTR ID Match (up to 7 days)
        val firstReference = first.referenceId.normalizedIdentifier()
        val secondReference = second.referenceId.normalizedIdentifier()
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

        // Independent corroboration:
        // - Cross-source alerts (e.g. UPI app push notification today + Bank SMS tomorrow): up to 72 hours
        // - Identical raw message content (e.g. carrier delayed retry): up to 72 hours
        // - Same account + compatible merchant (e.g. delayed SMS): up to 48 hours
        // - Embedded merchant within snippet: up to 48 hours
        return crossSource ||
            sameMessage ||
            (sameAccount && timeDifference <= 48 * 60 * 60 * 1000L) ||
            (merchantIsExplicitlyEmbedded(first, second) && timeDifference <= 48 * 60 * 60 * 1000L)
    }

    fun isCreditCardSettlementPair(first: Transaction, second: Transaction): Boolean {
        val text1 = "${first.merchantClean} ${first.merchantRaw} ${first.rawSnippet.orEmpty()}".lowercase()
        val text2 = "${second.merchantClean} ${second.merchantRaw} ${second.rawSnippet.orEmpty()}".lowercase()
        val cardKeywords = arrayOf(
            "credit card", "card payment", "card ending", "bill payment", "payment towards",
            "sbi card", "hdfc card", "icici card", "axis card", "onecard", "cred", "kotak card"
        )
        val hasCardKeyword = text1.containsAny(*cardKeywords) || text2.containsAny(*cardKeywords)
        if (!hasCardKeyword) return false

        val settlementKeywords = arrayOf(
            "payment received", "received towards", "thank you for payment", "bill paid",
            "payment success", "payment of", "card credited", "inward payment"
        )
        val hasSettlementVerb = text1.containsAny(*settlementKeywords) || text2.containsAny(*settlementKeywords)
        return hasSettlementVerb || merchantsCompatible(first, second)
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

    private fun String.containsAny(vararg keywords: String): Boolean =
        keywords.any { this.contains(it, ignoreCase = true) }
}
