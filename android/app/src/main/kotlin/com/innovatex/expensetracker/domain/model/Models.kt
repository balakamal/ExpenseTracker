package com.innovatex.expensetracker.domain.model

import java.util.UUID

/**
 * Pure Kotlin Domain Models.
 * STRICT ENFORCEMENT: Zero Android framework imports (`android.*`) allowed in this file.
 */

enum class TransactionType {
    DEBIT,
    CREDIT
}

enum class SourceChannel {
    SMS,
    EMAIL,
    MANUAL
}

enum class PaymentMedium(
    val displayName: String,
    val colorHex: String,
    val iconName: String
) {
    UPI(
        displayName = "UPI",
        colorHex = "#8B5CF6", // Violet (UPI, PhonePe, GPay)
        iconName = "qr_code"
    ),
    CARD(
        displayName = "Card",
        colorHex = "#3B82F6", // Royal Cobalt (Credit/Debit Card)
        iconName = "credit_card"
    ),
    NETBANKING(
        displayName = "NetBanking",
        colorHex = "#06B6D4", // Cyan (NEFT, IMPS, RTGS)
        iconName = "account_balance"
    ),
    WALLET(
        displayName = "Wallet",
        colorHex = "#F59E0B", // Champagne Amber (Paytm, Amazon Pay Wallet)
        iconName = "account_balance_wallet"
    ),
    ATM(
        displayName = "ATM Cash",
        colorHex = "#EC4899", // Rose Pink (ATM Withdrawal)
        iconName = "local_atm"
    ),
    BANK_ACCOUNT(
        displayName = "Bank",
        colorHex = "#10B981", // Emerald (Direct Bank Transfer/Account)
        iconName = "account_balance"
    ),
    OTHER(
        displayName = "Other",
        colorHex = "#79B6E3", // Soft Ocean
        iconName = "payments"
    );

    companion object {
        fun detect(
            rawSnippet: String?,
            merchantRaw: String? = null,
            accountLast4: String? = null,
            referenceId: String? = null
        ): PaymentMedium {
            val text = buildString {
                if (!rawSnippet.isNullOrBlank()) append(rawSnippet).append(" ")
                if (!merchantRaw.isNullOrBlank()) append(merchantRaw).append(" ")
                if (!referenceId.isNullOrBlank()) append(referenceId)
            }.lowercase()

            if (text.isBlank()) return OTHER

            return when {
                // 1. UPI: Matches UPI, VPA, VPAs (@ok, @ybl, @paytm), and UPI apps
                text.containsAny(
                    "upi", "vpa", "@ok", "@paytm", "@ybl", "@ibl", "@axl", "@apl",
                    "@upi", "phonepe", "gpay", "google pay", "bhim", "cred upi",
                    "paytm upi", "upi ref", "upi/p2m", "upi/p2p", "upi/p2a", "qr code"
                ) -> UPI

                // 2. ATM: Cash withdrawals
                text.containsAny(
                    "atm", "cash withdrawal", "atm wdl", "withdrawn from atm", "atm cash", "cash wdl"
                ) -> ATM

                // 3. Card: Credit/Debit/Forex/Prepaid card transactions
                text.containsAny(
                    "card", "credit card", "debit card", "visa", "mastercard", "rupay",
                    "amex", "american express", "diners", "card ending", "using card",
                    "pos txn", "pos purchase", "card ending in", "card ending with"
                ) -> CARD

                // 4. Wallet: Prepaid wallets
                text.containsAny(
                    "wallet", "paytm wallet", "amazon pay balance", "phonepe wallet",
                    "mobikwik", "freecharge wallet"
                ) -> WALLET

                // 5. Netbanking / IMPS / NEFT / RTGS
                text.containsAny(
                    "imps", "neft", "rtgs", "netbanking", "internet banking", "fund transfer",
                    "inb txn", "direct transfer"
                ) -> NETBANKING

                // 6. Direct Bank Account (A/C, Acct, Account)
                text.containsAny(
                    "a/c", "acct", "account", "bank", "hdfc", "sbi", "icici", "axis", "kotak",
                    "pnb", "bob", "canara", "union bank", "idfc", "yes bank", "indusind"
                ) -> BANK_ACCOUNT

                else -> OTHER
            }
        }

        private fun String.containsAny(vararg keywords: String): Boolean =
            keywords.any { this.contains(it, ignoreCase = true) }
    }
}

data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val currency: String = "USD",
    val type: TransactionType,
    val timestamp: Long,
    val monthKey: String, // "YYYY-MM"
    val merchantRaw: String,
    val merchantClean: String,
    val categoryId: String = "general",
    val sources: List<SourceChannel> = listOf(SourceChannel.SMS),
    val isDeduplicated: Boolean = false,
    val referenceId: String? = null,
    val accountLast4: String? = null,
    val rawSnippet: String? = null,
    val notes: String? = null,
    val aiClassification: AiClassificationResult? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val paymentMedium: PaymentMedium
        get() = PaymentMedium.detect(rawSnippet, merchantRaw, accountLast4, referenceId)

    val sender: String?
        get() = notes?.ifBlank { null } ?: extractSenderFallback(rawSnippet, merchantRaw)

    companion object {
        private val senderPrefixRegex = Regex("(?i)(?:\\[(?:sender|from):\\s*([^\\]]+)\\]|^([A-Za-z]{2}-[A-Za-z0-9]{4,10})\\b)")

        fun extractSenderFallback(rawSnippet: String?, merchantRaw: String?): String? {
            if (!rawSnippet.isNullOrBlank()) {
                val match = senderPrefixRegex.find(rawSnippet)
                if (match != null) {
                    val candidate = (match.groupValues.getOrNull(1) ?: match.groupValues.getOrNull(2))?.trim()
                    if (!candidate.isNullOrBlank()) return candidate
                }
            }
            if (!merchantRaw.isNullOrBlank() && merchantRaw != "Unknown Merchant") {
                return merchantRaw.trim()
            }
            return null
        }
    }
}

data class SuspectedSpamItem(
    val sender: String,
    val transactionCount: Int,
    val totalAmount: Double,
    val reason: String,
    val sampleSnippet: String?,
    val sampleMessages: List<String> = emptyList(),
    val transactionIds: List<String>
)

data class Category(
    val id: String,
    val name: String,
    val iconName: String,
    val colorHex: String,
    val monthlyBudgetLimit: Double? = null,
    val isSystemDefault: Boolean = false,
    val sortOrder: Int = 0
)

data class MerchantRule(
    val id: String = UUID.randomUUID().toString(),
    val pattern: String,
    val categoryId: String,
    val isRegex: Boolean = false,
    val priority: Int = 100,
    val createdAt: Long = System.currentTimeMillis()
)

data class MonthlyBudget(
    val monthKey: String,
    val budgetLimit: Double,
    val alertThresholdPercent: Int = 85
)

data class MonthlySummary(
    val monthKey: String,
    val totalDebit: Double,
    val totalCredit: Double,
    val netBalance: Double = totalCredit - totalDebit,
    val budgetLimit: Double = 0.0,
    val budgetProgressPercent: Int = if (budgetLimit > 0) ((totalDebit / budgetLimit) * 100).toInt() else 0,
    val remainingBudget: Double = (budgetLimit - totalDebit).coerceAtLeast(0.0),
    val categoryTotals: Map<String, Double> = emptyMap(),
    val transactionCount: Int = 0
)
