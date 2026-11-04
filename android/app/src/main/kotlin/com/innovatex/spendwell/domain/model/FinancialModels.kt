package com.innovatex.spendwell.domain.model

import com.innovatex.spendwell.common.Money

enum class TransactionType {
    DEBIT,
    CREDIT,
    TRANSFER
}

enum class TransactionStatus {
    DETECTED,
    PENDING,
    SUCCESSFUL,
    FAILED,
    REVERSED,
    REFUNDED,
    NEEDS_REVIEW
}

/**
 * Strict classification of economic activity to prevent double counting:
 * - Credit card bill payment is NOT an expense (purchases are already counted).
 * - Loan disbursement is NOT income (it's a liability).
 * - Investment transfers are NOT consumption expenses.
 * - Cash withdrawals are separate from actual cash spending.
 */
enum class FinancialNature {
    CONSUMPTION_EXPENSE,
    INCOME,
    INTERNAL_TRANSFER,
    CREDIT_CARD_BILL_PAYMENT,
    LOAN_DISBURSEMENT,
    INVESTMENT_TRANSFER,
    CASH_WITHDRAWAL,
    REFUND
}

enum class AccountType {
    SAVINGS,
    CURRENT,
    CREDIT_CARD,
    WALLET,
    CASH,
    INVESTMENT,
    LOAN
}

enum class SourceType {
    SMS,
    NOTIFICATION,
    STATEMENT_PDF,
    STATEMENT_CSV,
    MANUAL
}

data class Transaction(
    val id: String,
    val amount: Money,
    val type: TransactionType,
    val status: TransactionStatus,
    val financialNature: FinancialNature,
    val timestamp: Long,
    val accountId: String?,
    val toAccountId: String? = null,
    val merchantId: String? = null,
    val merchantName: String = "",
    val categoryId: String? = null,
    val categoryName: String = "Miscellaneous",
    val description: String,
    val referenceId: String? = null,
    val utrNumber: String? = null,
    val rawMessageSnippet: String? = null,
    val isReviewed: Boolean = false,
    val originalTransactionId: String? = null,
    val recurringPaymentId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class Account(
    val id: String,
    val accountName: String,
    val accountType: AccountType,
    val accountNumberLast4: String,
    val bankName: String,
    val currentBalance: Money,
    val isPrimary: Boolean = false,
    val isActive: Boolean = true
)

data class Category(
    val id: String,
    val name: String,
    val iconName: String,
    val colorHex: String,
    val isIncome: Boolean = false,
    val isSystemDefault: Boolean = false,
    val parentCategoryId: String? = null
)

data class Budget(
    val id: String,
    val categoryId: String,
    val categoryName: String,
    val limitAmount: Money,
    val spentAmount: Money,
    val monthPeriod: String, // YYYY-MM
    val alertThresholdPercentage: Int = 85
) {
    val remainingAmount: Money get() = (limitAmount - spentAmount).let { if (it.isNegative) Money.ZERO else it }
    val percentageUsed: Float
        get() {
            if (limitAmount.isZero) return 0f
            return (spentAmount.toDisplayDouble() / limitAmount.toDisplayDouble()).toFloat() * 100f
        }
    val isExceeded: Boolean get() = spentAmount > limitAmount
}

data class SavingsGoal(
    val id: String,
    val name: String,
    val targetAmount: Money,
    val currentSaved: Money,
    val targetDate: Long,
    val category: String,
    val isCompleted: Boolean = false
) {
    val progressPercentage: Float
        get() {
            if (targetAmount.isZero) return 0f
            return ((currentSaved.toDisplayDouble() / targetAmount.toDisplayDouble()).toFloat() * 100f).coerceIn(0f, 100f)
        }
    val remainingAmount: Money get() = (targetAmount - currentSaved).let { if (it.isNegative) Money.ZERO else it }
}

data class RecurringPayment(
    val id: String,
    val merchantId: String,
    val title: String,
    val amount: Money,
    val cadence: String,
    val expectedDayOfMonth: Int,
    val nextDueDate: Long,
    val isActive: Boolean = true,
    val lastBilledDate: Long? = null,
    val categoryId: String? = null
)

data class AiInsight(
    val id: String,
    val title: String,
    val message: String,
    val type: String,
    val actionPayloadJson: String? = null,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class DuplicateCandidate(
    val id: String,
    val originalTransactionId: String,
    val duplicateTransactionId: String,
    val similarityScore: Float,
    val reasons: List<String>,
    val resolved: Boolean = false,
    val detectedAt: Long = System.currentTimeMillis()
)
