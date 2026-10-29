package com.innovatex.expensetracker.data.repository

import com.innovatex.expensetracker.data.local.PreferencesDataStore
import com.innovatex.expensetracker.data.local.dao.TransactionDao
import com.innovatex.expensetracker.data.local.dao.UserFeedbackDao
import com.innovatex.expensetracker.data.local.entity.TransactionEntity
import com.innovatex.expensetracker.data.local.entity.UserFeedbackExemplarEntity
import com.innovatex.expensetracker.di.IoDispatcher
import com.innovatex.expensetracker.domain.model.AuthenticityLabel
import com.innovatex.expensetracker.domain.model.MonthlySummary
import com.innovatex.expensetracker.domain.model.SuspectedSpamItem
import com.innovatex.expensetracker.domain.model.Transaction
import com.innovatex.expensetracker.domain.model.TransactionType
import com.innovatex.expensetracker.domain.repository.TransactionRepository
import com.innovatex.expensetracker.ingestion.parser.SmsParserEngine
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val userFeedbackDao: UserFeedbackDao,
    private val smsParserEngine: SmsParserEngine,
    private val preferencesDataStore: PreferencesDataStore,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : TransactionRepository {

    override fun getTransactionsForMonth(monthKey: String): Flow<List<Transaction>> =
        transactionDao.getTransactionsForMonth(monthKey)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override fun getAllTransactions(): Flow<List<Transaction>> =
        transactionDao.getAllTransactionsFlow()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override fun getTransactionsForDate(startTime: Long, endTime: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsForDateRange(startTime, endTime)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> =
        transactionDao.getRecentTransactions(limit)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override suspend fun getTransactionById(id: String): Transaction? = withContext(ioDispatcher) {
        transactionDao.getTransactionById(id)?.toDomain()
    }

    override suspend fun saveTransaction(transaction: Transaction): String = withContext(ioDispatcher) {
        val entity = TransactionEntity.fromDomain(transaction)
        transactionDao.insertTransaction(entity)
        transaction.id
    }

    override suspend fun saveTransactions(transactions: List<Transaction>): Unit = withContext(ioDispatcher) {
        val entities = transactions.map { TransactionEntity.fromDomain(it) }
        transactionDao.insertTransactions(entities)
    }

    override suspend fun updateTransaction(transaction: Transaction): Unit = withContext(ioDispatcher) {
        val entity = TransactionEntity.fromDomain(transaction)
        transactionDao.updateTransaction(entity)
    }

    override suspend fun deleteTransaction(id: String): Unit = withContext(ioDispatcher) {
        transactionDao.deleteTransactionById(id)
    }

    override suspend fun findPotentialDuplicates(
        startTime: Long,
        endTime: Long,
        amount: Double,
        currency: String
    ): List<Transaction> = withContext(ioDispatcher) {
        transactionDao.findPotentialDuplicates(startTime, endTime, amount, currency)
            .map { it.toDomain() }
    }

    override fun getAvailableMonths(): Flow<List<String>> =
        transactionDao.getAvailableMonths().flowOn(ioDispatcher)

    override fun getMonthlySummary(monthKey: String): Flow<MonthlySummary> =
        getTransactionsForMonth(monthKey).map { transactions ->
            var totalDebit = 0.0
            var totalCredit = 0.0
            val categoryTotals = mutableMapOf<String, Double>()

            // Strictly compute dashboard totals from genuine/valid transactions only
            val validTransactions = transactions.filter { it.isValidTransaction }

            for (txn in validTransactions) {
                when (txn.type) {
                    TransactionType.DEBIT -> {
                        totalDebit += txn.amount
                        val currentCategorySum = categoryTotals.getOrDefault(txn.categoryId, 0.0)
                        categoryTotals[txn.categoryId] = currentCategorySum + txn.amount
                    }
                    TransactionType.CREDIT -> {
                        totalCredit += txn.amount
                    }
                }
            }

            MonthlySummary(
                monthKey = monthKey,
                totalDebit = totalDebit,
                totalCredit = totalCredit,
                netBalance = totalCredit - totalDebit,
                budgetLimit = 25000.0, // Default INR monthly budget limit
                categoryTotals = categoryTotals,
                transactionCount = validTransactions.size
            )
        }.flowOn(ioDispatcher)

    override fun getOverallSummary(): Flow<MonthlySummary> =
        getAllTransactions().map { transactions ->
            var totalDebit = 0.0
            var totalCredit = 0.0
            val categoryTotals = mutableMapOf<String, Double>()

            val validTransactions = transactions.filter { it.isValidTransaction }

            for (txn in validTransactions) {
                when (txn.type) {
                    TransactionType.DEBIT -> {
                        totalDebit += txn.amount
                        val currentCategorySum = categoryTotals.getOrDefault(txn.categoryId, 0.0)
                        categoryTotals[txn.categoryId] = currentCategorySum + txn.amount
                    }
                    TransactionType.CREDIT -> {
                        totalCredit += txn.amount
                    }
                }
            }

            MonthlySummary(
                monthKey = "ALL",
                totalDebit = totalDebit,
                totalCredit = totalCredit,
                netBalance = totalCredit - totalDebit,
                budgetLimit = 50000.0,
                categoryTotals = categoryTotals,
                transactionCount = validTransactions.size
            )
        }.flowOn(ioDispatcher)

    override suspend fun purgeDuplicates(): Int = withContext(ioDispatcher) {
        val allEntities = transactionDao.getAllTransactions()
        val seenTransactions = mutableMapOf<String, Transaction>()
        val duplicateIds = mutableListOf<String>()

        for (entity in allEntities) {
            val domain = entity.toDomain()
            val timeBucket30m = domain.timestamp / (30 * 60 * 1000L)
            val timeBucket15m = domain.timestamp / (15 * 60 * 1000L)
            val refKey = domain.referenceId?.trim()?.lowercase() ?: ""
            val acctKey = domain.accountLast4?.trim() ?: ""

            val key = when {
                refKey.isNotBlank() -> "REF_${domain.amount}_${domain.currency}_$refKey"
                acctKey.isNotBlank() -> "ACCT_${domain.amount}_${domain.currency}_${acctKey}_$timeBucket30m"
                else -> {
                    val normMerchant = domain.merchantClean.lowercase().take(15)
                    "MERCHANT_${domain.amount}_${domain.currency}_${normMerchant}_$timeBucket15m"
                }
            }

            val existing = seenTransactions[key]
            if (existing != null) {
                val combinedSources = (existing.sources + domain.sources).distinct()
                val bestMerchant = if (existing.merchantClean.lowercase() in setOf("general purchase", "unknown merchant")) {
                    domain.merchantClean
                } else {
                    existing.merchantClean
                }
                val updated = existing.copy(
                    merchantClean = bestMerchant,
                    sources = combinedSources,
                    isDeduplicated = true,
                    referenceId = existing.referenceId ?: domain.referenceId,
                    accountLast4 = existing.accountLast4 ?: domain.accountLast4
                )
                seenTransactions[key] = updated
                transactionDao.updateTransaction(TransactionEntity.fromDomain(updated))
                duplicateIds.add(entity.id)
            } else {
                seenTransactions[key] = domain
            }
        }

        if (duplicateIds.isNotEmpty()) {
            transactionDao.deleteTransactionsByIds(duplicateIds)
        }
        duplicateIds.size
    }

    override suspend fun purgeSpamTransactions(): Int = withContext(ioDispatcher) {
        val allEntities = transactionDao.getAllTransactions()
        val spamIds = mutableListOf<String>()

        for (entity in allEntities) {
            val domain = entity.toDomain()
            val sender = domain.sender
            val rawSnippet = domain.rawSnippet ?: ""
            val merchantRaw = domain.merchantRaw

            val isSpam = preferencesDataStore.isSpamSender(sender) ||
                rawSnippet.containsAny("loan", "pre-approved", "apply now", "credit limit", "insurance", "casino", "lottery", "win") ||
                merchantRaw.containsAny("loan", "pre-approved", "apply now")

            if (isSpam) {
                spamIds.add(entity.id)
            }
        }

        if (spamIds.isNotEmpty()) {
            transactionDao.deleteTransactionsByIds(spamIds)
        }
        spamIds.size
    }

    override suspend fun getSuspectedSpamItems(): List<SuspectedSpamItem> = withContext(ioDispatcher) {
        val allEntities = transactionDao.getAllTransactions()
        val settings = preferencesDataStore.appSettingsFlow.first()
        val suspectedBySender = mutableMapOf<String, MutableList<TransactionEntity>>()
        val senderReasons = mutableMapOf<String, String>()

        for (entity in allEntities) {
            val domain = entity.toDomain()
            val sender = domain.sender ?: "Unknown"

            if (preferencesDataStore.isTrustedSender(sender)) {
                continue
            }

            val rawSnippet = domain.rawSnippet ?: ""
            val merchantRaw = domain.merchantRaw
            var reason: String? = null

            if (preferencesDataStore.isSpamSender(sender)) {
                reason = "Marked as spam sender"
            } else if (rawSnippet.containsAny("loan", "pre-approved", "apply now", "credit limit", "congratulations", "win cash", "voucher")) {
                reason = "Contains promotional/marketing keywords"
            } else if (merchantRaw.containsAny("loan", "pre-approved", "apply now", "promotion", "cashback")) {
                reason = "Merchant name indicates promotional alert"
            } else if (settings.aiSpamDetectionEnabled && domain.aiClassification != null) {
                if (domain.aiClassification.authenticityLabel == AuthenticityLabel.SPAM_PROMO) {
                    val conf = (domain.aiClassification.authenticityConfidence * 100).toInt()
                    reason = "AI Flagged: Promotional alert ($conf% confidence)"
                } else if (domain.aiClassification.authenticityLabel == AuthenticityLabel.PAYMENT_REMINDER) {
                    val conf = (domain.aiClassification.authenticityConfidence * 100).toInt()
                    reason = "AI Flagged: Payment reminder / bill notice ($conf% confidence)"
                }
            }

            if (reason != null) {
                suspectedBySender.getOrPut(sender) { mutableListOf() }.add(entity)
                if (!senderReasons.containsKey(sender)) {
                    senderReasons[sender] = reason
                }
            }
        }

        suspectedBySender.map { (sender, entities) ->
            val allMessages = entities.mapNotNull { it.rawSnippet?.ifBlank { null } ?: it.merchantRaw.ifBlank { null } }.distinct()
            SuspectedSpamItem(
                sender = sender,
                transactionCount = entities.size,
                totalAmount = entities.sumOf { it.amount },
                reason = senderReasons[sender] ?: "Suspected marketing message",
                sampleSnippet = allMessages.firstOrNull(),
                sampleMessages = allMessages,
                transactionIds = entities.map { it.id }
            )
        }
    }

    override suspend fun confirmSpam(sender: String, transactionIds: List<String>): Unit = withContext(ioDispatcher) {
        preferencesDataStore.markSenderAsSpam(sender)
        if (transactionIds.isNotEmpty()) {
            transactionDao.deleteTransactionsByIds(transactionIds)
        }
    }

    override suspend fun trustSender(sender: String): Unit = withContext(ioDispatcher) {
        preferencesDataStore.markSenderAsTrusted(sender)
    }

    override suspend fun deleteTransactionsBySender(sender: String): Int = withContext(ioDispatcher) {
        val allEntities = transactionDao.getAllTransactions()
        val targetSender = sender.trim().uppercase()
        val matchingIds = allEntities.filter { entity ->
            val domain = entity.toDomain()
            domain.sender?.trim()?.uppercase() == targetSender
        }.map { it.id }

        if (matchingIds.isNotEmpty()) {
            transactionDao.deleteTransactionsByIds(matchingIds)
        }
        matchingIds.size
    }

    override suspend fun setTransactionValidity(id: String, isValid: Boolean): Unit = withContext(ioDispatcher) {
        transactionDao.updateTransactionValidity(id, isValid)
    }

    override suspend fun recordUserFeedback(
        sender: String?,
        merchantClean: String,
        rawSnippet: String?,
        label: AuthenticityLabel
    ): Unit = withContext(ioDispatcher) {
        userFeedbackDao.insertExemplar(
            UserFeedbackExemplarEntity(
                sender = sender?.trim()?.uppercase()?.ifBlank { null },
                merchantClean = merchantClean.trim(),
                rawSnippet = rawSnippet?.take(300),
                assignedLabel = label
            )
        )
    }

    private fun String.containsAny(vararg keywords: String): Boolean =
        keywords.any { this.contains(it, ignoreCase = true) }
}
