package com.innovatex.spendwell.transactionengine.model

import com.innovatex.spendwell.common.Money
import com.innovatex.spendwell.domain.model.FinancialNature
import com.innovatex.spendwell.domain.model.SourceType
import com.innovatex.spendwell.domain.model.Transaction
import com.innovatex.spendwell.domain.model.TransactionStatus
import com.innovatex.spendwell.domain.model.TransactionType

data class RawSignal(
    val sourceType: SourceType,
    val senderAddress: String,
    val rawText: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class NormalizedMessage(
    val signal: RawSignal,
    val normalizedText: String,
    val detectedBank: String?,
    val isKnownFinancialSender: Boolean
)

data class ExtractedFinancialInfo(
    val amount: Money,
    val transactionType: TransactionType,
    val status: TransactionStatus,
    val financialNature: FinancialNature,
    val accountLast4: String?,
    val bankName: String?,
    val rawMerchant: String?,
    val normalizedMerchant: String,
    val referenceId: String?,
    val utrNumber: String?,
    val isRefund: Boolean = false,
    val isReversal: Boolean = false,
    val timestamp: Long
)

sealed class EngineResult {
    data class Success(
        val transaction: Transaction,
        val isNewTransaction: Boolean,
        val isDuplicateMerged: Boolean = false,
        val linkedRefundToOriginalId: String? = null
    ) : EngineResult()

    data class FlaggedForReview(
        val transaction: Transaction,
        val duplicateCandidateId: String?,
        val reason: String
    ) : EngineResult()

    data class Dropped(
        val dropReason: DropReason,
        val message: String
    ) : EngineResult()
}

enum class DropReason {
    SPAM_OR_PROMOTIONAL,
    PAYMENT_REMINDER_NOT_TRANSACTION,
    AUTHENTICATION_OTP,
    BALANCE_ENQUIRY_ONLY,
    NON_FINANCIAL_MESSAGE,
    INVALID_AMOUNT_OR_FORMAT
}
