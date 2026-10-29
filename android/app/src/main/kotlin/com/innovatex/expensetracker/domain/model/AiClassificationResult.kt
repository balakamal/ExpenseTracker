package com.innovatex.expensetracker.domain.model

enum class AuthenticityLabel {
    REAL_TRANSACTION,
    SPAM_PROMO,
    PAYMENT_REMINDER,
    UNCLASSIFIED
}

data class AiClassificationResult(
    val authenticityLabel: AuthenticityLabel,
    val authenticityConfidence: Float,
    val categoryId: String?,
    val categoryConfidence: Float?,
    val impliedTransactionType: TransactionType? = null,
    val ranByAi: Boolean = true
)

