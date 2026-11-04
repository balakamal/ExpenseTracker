package com.innovatex.spendwell.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 1. User Profile Entity
 */
@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val id: String = "primary_user",
    val displayName: String,
    val preferredCurrency: String = "INR",
    val isPrivateModeOnly: Boolean = true,
    val isBiometricEnabled: Boolean = false,
    val isNotificationListenerEnabled: Boolean = false,
    val isSmsIngestionEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 2. Financial Account Entity
 */
@Entity(
    tableName = "financial_accounts",
    indices = [
        Index(value = ["accountNumberLast4", "bankName"], unique = false),
        Index(value = ["accountType"])
    ]
)
data class AccountEntity(
    @PrimaryKey val id: String,
    val accountName: String,
    val accountType: String, // SAVINGS, CURRENT, CREDIT_CARD, WALLET, CASH, INVESTMENT, LOAN
    val accountNumberLast4: String,
    val bankName: String,
    val currentBalancePaisa: Long = 0L,
    val currency: String = "INR",
    val isPrimary: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 3. Credit Card Entity
 */
@Entity(
    tableName = "credit_cards",
    indices = [Index(value = ["last4", "bankName"]), Index(value = ["linkedAccountId"])]
)
data class CreditCardEntity(
    @PrimaryKey val id: String,
    val cardName: String,
    val bankName: String,
    val last4: String,
    val creditLimitPaisa: Long,
    val availableCreditPaisa: Long,
    val billingCycleDay: Int,
    val paymentDueDateDay: Int,
    val linkedAccountId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 4. Master Transaction Entity
 */
@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["accountId"]),
        Index(value = ["merchantId"]),
        Index(value = ["categoryId"]),
        Index(value = ["referenceId"]),
        Index(value = ["utrNumber"]),
        Index(value = ["status"]),
        Index(value = ["financialNature"])
    ]
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val amountPaisa: Long,
    val currency: String = "INR",
    val type: String, // DEBIT, CREDIT, TRANSFER
    val status: String, // DETECTED, PENDING, SUCCESSFUL, FAILED, REVERSED, REFUNDED, NEEDS_REVIEW
    val financialNature: String, // CONSUMPTION_EXPENSE, INCOME, INTERNAL_TRANSFER, CREDIT_CARD_BILL_PAYMENT, LOAN_DISBURSEMENT, INVESTMENT_TRANSFER, CASH_WITHDRAWAL, REFUND
    val timestamp: Long,
    val accountId: String?,
    val toAccountId: String? = null,
    val merchantId: String? = null,
    val categoryId: String? = null,
    val description: String,
    val referenceId: String? = null,
    val utrNumber: String? = null,
    val rawMessageSnippet: String? = null,
    val isReviewed: Boolean = false,
    val originalTransactionId: String? = null, // Links refunds or reversals to the original txn
    val recurringPaymentId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 5. Multi-Signal Source Event Entity (Preserves raw SMS, notifications, statements)
 */
@Entity(
    tableName = "transaction_source_events",
    indices = [
        Index(value = ["canonicalTransactionId"]),
        Index(value = ["hashSignature"], unique = true),
        Index(value = ["receivedTimestamp"])
    ]
)
data class TransactionSourceEventEntity(
    @PrimaryKey val id: String,
    val canonicalTransactionId: String?,
    val sourceType: String, // SMS, NOTIFICATION, STATEMENT_PDF, STATEMENT_CSV, MANUAL
    val senderAddress: String,
    val rawMessage: String,
    val receivedTimestamp: Long,
    val hashSignature: String
)

/**
 * 6. Merchant Entity
 */
@Entity(
    tableName = "merchants",
    indices = [Index(value = ["cleanName"], unique = true)]
)
data class MerchantEntity(
    @PrimaryKey val id: String,
    val cleanName: String,
    val rawPattern: String? = null,
    val defaultCategoryId: String? = null,
    val isSubscription: Boolean = false,
    val confidenceScore: Float = 1.0f
)

/**
 * 7. Category Entity
 */
@Entity(
    tableName = "categories",
    indices = [Index(value = ["name"], unique = true)]
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val iconName: String,
    val colorHex: String,
    val isIncome: Boolean = false,
    val isSystemDefault: Boolean = false,
    val parentCategoryId: String? = null
)

/**
 * 8. Transaction Classification Entity
 */
@Entity(
    tableName = "transaction_classifications",
    indices = [Index(value = ["transactionId"], unique = true)]
)
data class TransactionClassificationEntity(
    @PrimaryKey val transactionId: String,
    val predictedCategoryId: String,
    val confidence: Float,
    val classificationMethod: String, // HEURISTIC_RULE, LOCAL_MODEL, USER_OVERRIDE
    val feedbackProvided: Boolean = false,
    val classifiedAt: Long = System.currentTimeMillis()
)

/**
 * 9. Duplicate Candidate Entity
 */
@Entity(
    tableName = "duplicate_candidates",
    indices = [
        Index(value = ["originalTransactionId"]),
        Index(value = ["duplicateTransactionId"]),
        Index(value = ["resolved"])
    ]
)
data class DuplicateCandidateEntity(
    @PrimaryKey val id: String,
    val originalTransactionId: String,
    val duplicateTransactionId: String,
    val similarityScore: Float,
    val reasonsJson: String,
    val resolved: Boolean = false,
    val resolutionAction: String? = null, // MERGED, KEPT_BOTH, DISMISSED
    val detectedAt: Long = System.currentTimeMillis()
)

/**
 * 10. Recurring Payment Entity
 */
@Entity(
    tableName = "recurring_payments",
    indices = [Index(value = ["merchantId"]), Index(value = ["nextDueDate"])]
)
data class RecurringPaymentEntity(
    @PrimaryKey val id: String,
    val merchantId: String,
    val title: String,
    val amountPaisa: Long,
    val cadence: String, // DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY
    val expectedDayOfMonth: Int,
    val nextDueDate: Long,
    val isActive: Boolean = true,
    val lastBilledDate: Long? = null,
    val categoryId: String? = null
)

/**
 * 11. Budget Entity
 */
@Entity(
    tableName = "budgets",
    indices = [Index(value = ["categoryId", "monthPeriod"], unique = true)]
)
data class BudgetEntity(
    @PrimaryKey val id: String,
    val categoryId: String,
    val limitAmountPaisa: Long,
    val monthPeriod: String, // YYYY-MM
    val alertThresholdPercentage: Int = 85,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 12. Savings Goal Entity
 */
@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey val id: String,
    val name: String,
    val targetAmountPaisa: Long,
    val currentSavedPaisa: Long = 0L,
    val targetDate: Long,
    val category: String = "GENERAL",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 13. Financial Rule Entity
 */
@Entity(tableName = "financial_rules")
data class FinancialRuleEntity(
    @PrimaryKey val id: String,
    val ruleType: String, // MERCHANT_MAPPING, KEYWORD_FILTER, SPAM_SENDER, TRANSFER_ACCOUNT
    val pattern: String,
    val targetCategoryId: String? = null,
    val targetAccountId: String? = null,
    val priority: Int = 0,
    val isActive: Boolean = true
)

/**
 * 14. AI Insight Entity
 */
@Entity(tableName = "ai_insights")
data class AiInsightEntity(
    @PrimaryKey val id: String,
    val title: String,
    val message: String,
    val type: String, // SPENDING_ANOMALY, BUDGET_WARNING, RECURRING_SPIKE, SAVINGS_OPPORTUNITY, GENERAL_ADVICE
    val actionPayloadJson: String? = null,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 15. Reconciliation Session Entity
 */
@Entity(
    tableName = "reconciliation_sessions",
    indices = [Index(value = ["accountId"])]
)
data class ReconciliationSessionEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val statementPeriodStart: Long,
    val statementPeriodEnd: Long,
    val openingBalancePaisa: Long,
    val closingBalancePaisa: Long,
    val matchedCount: Int = 0,
    val unmatchedCount: Int = 0,
    val discrepancyPaisa: Long = 0L,
    val status: String = "IN_PROGRESS", // IN_PROGRESS, RECONCILED, DISCREPANCY_FLAGGED
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 16. Imported Statement Entity
 */
@Entity(tableName = "imported_statements")
data class ImportedStatementEntity(
    @PrimaryKey val id: String,
    val fileName: String,
    val format: String, // PDF, CSV
    val fileHash: String,
    val accountId: String,
    val importTimestamp: Long = System.currentTimeMillis(),
    val transactionCount: Int = 0,
    val status: String = "PROCESSED"
)
