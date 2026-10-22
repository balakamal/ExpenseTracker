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
    val createdAt: Long = System.currentTimeMillis()
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
