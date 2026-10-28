package com.innovatex.expensetracker.data.repository

import com.innovatex.expensetracker.data.local.PreferencesDataStore
import com.innovatex.expensetracker.data.local.dao.TransactionDao
import com.innovatex.expensetracker.data.local.entity.TransactionEntity
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

    override suspend fun saveTransactions(transactions: List<Transaction>) = withContext(ioDispatcher) {
        val entities = transactions.map { TransactionEntity.fromDomain(it) }
        transactionDao.insertTransactions(entities)
    }

    override suspend fun updateTransaction(transaction: Transaction) = withContext(ioDispatcher) {
        transactionDao.updateTransaction(TransactionEntity.fromDomain(transaction))
    }

    override suspend fun deleteTransaction(id: String) = withContext(ioDispatcher) {
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

            for (txn in transactions) {
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
                transactionCount = transactions.size
            )
        }.flowOn(ioDispatcher)

    override fun getOverallSummary(): Flow<MonthlySummary> =
        getAllTransactions().map { transactions ->
            var totalDebit = 0.0
            var totalCredit = 0.0
            val categoryTotals = mutableMapOf<String, Double>()

            for (txn in transactions) {
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
                transactionCount = transactions.size
            )
        }.flowOn(ioDispatcher)

    override suspend fun purgeDuplicates(): Int = withContext(ioDispatcher) {
        val allEntities = transactionDao.getAllTransactions()
        val seenKeys = mutableSetOf<String>()
        val duplicateIds = mutableListOf<String>()

        for (entity in allEntities) {
            val key = if (!entity.referenceId.isNullOrBlank()) {
                "ref_${entity.referenceId.trim().lowercase()}_${entity.amount}_${entity.type.name}"
            } else {
                val timeBucket = entity.timestamp / (2 * 60 * 1000L) // 2-minute bucket
                val amountInt = (entity.amount * 100).toLong()
                val cleanMerchant = entity.merchantClean.trim().lowercase()
                "bucket_${timeBucket}_${amountInt}_${entity.type.name}_$cleanMerchant"
            }

            if (seenKeys.contains(key)) {
                duplicateIds.add(entity.id)
            } else {
                seenKeys.add(key)
            }
        }

        if (duplicateIds.isNotEmpty()) {
            transactionDao.deleteTransactionsByIds(duplicateIds)
        }
        duplicateIds.size
    }

    override suspend fun purgeSpamTransactions(): Int = withContext(ioDispatcher) {
        val allEntities = transactionDao.getAllTransactions()
        val spamSenders = preferencesDataStore.spamSendersFlow.first()
        val spamIds = mutableListOf<String>()

        for (entity in allEntities) {
            val domain = entity.toDomain()
            val senderKey = domain.sender?.uppercase()
            // Only purge transactions whose sender has been confirmed as spam by user
            if (senderKey != null && spamSenders.contains(senderKey)) {
                spamIds.add(entity.id)
            }
        }

        if (spamIds.isNotEmpty()) {
            transactionDao.deleteTransactionsByIds(spamIds)
        }
        spamIds.size
    }

    override suspend fun getSuspectedSpamItems(): List<SuspectedSpamItem> = withContext(ioDispatcher) {
        val settings = preferencesDataStore.appSettingsFlow.first()
        val allEntities = transactionDao.getAllTransactions()
        val trustedSenders = preferencesDataStore.trustedSendersFlow.first()
        val spamSenders = preferencesDataStore.spamSendersFlow.first()

        val suspectedBySender = mutableMapOf<String, MutableList<TransactionEntity>>()
        val senderReasons = mutableMapOf<String, String>()

        for (entity in allEntities) {
            val domain = entity.toDomain()
            val sender = domain.sender ?: "Unknown Sender"
            val senderKey = sender.uppercase()

            // If user explicitly trusted this sender, skip
            if (trustedSenders.contains(senderKey)) continue
            // If already confirmed as spam, it is already accounted for
            if (spamSenders.contains(senderKey)) continue

            val snippet = entity.rawSnippet?.lowercase() ?: ""
            val merchant = entity.merchantClean.lowercase()

            val aiAuth = if (settings.aiSpamDetectionEnabled) domain.aiClassification?.authenticityLabel else null
            val aiConf = if (settings.aiSpamDetectionEnabled) domain.aiClassification?.authenticityConfidence ?: 0f else 0f

            val reason = when {
                aiAuth == AuthenticityLabel.SPAM_PROMO && aiConf >= 0.70f ->
                    "AI Detected Promotion (${(aiConf * 100).toInt()}% conf)"
                aiAuth == AuthenticityLabel.PAYMENT_REMINDER && aiConf >= 0.70f ->
                    "AI Detected Payment Reminder (${(aiConf * 100).toInt()}% conf)"
                snippet.containsAny("loan", "pre-approved", "pre approved", "eligible for a loan", "instant cash") ||
                    merchant.containsAny("loan", "pre-approved") -> "Pre-approved loan promotion"
                snippet.containsAny("rummy", "win cash", "jackpot", "lottery", "bonus points") ||
                    merchant.containsAny("rummy", "dream11") -> "Gaming or lottery advertisement"
                snippet.containsAny("coupon", "promo code", "flat 50%", "flat 20%", "claim reward", "use code") ||
                    merchant.containsAny("discount", "coupon") -> "Promotional discount or voucher"
                snippet.containsAny("recharge now", "recharge with", "daily data", "unlimited calls") ||
                    merchant.containsAny("recharge") -> "Telecom recharge promotion"
                snippet.containsAny("bill is due", "due on", "avoid late fee", "pay before") -> "Unpaid bill due reminder"
                !smsParserEngine.isFinancialAlert(entity.rawSnippet ?: "", sender) -> "Unverified financial notification"
                else -> null
            }

            if (reason != null) {
                suspectedBySender.getOrPut(sender) { mutableListOf() }.add(entity)
                if (!senderReasons.containsKey(sender)) {
                    senderReasons[sender] = reason
                }
            }
        }

        suspectedBySender.map { (sender, entities) ->
            val allMessages = entities.mapNotNull { it.rawSnippet?.ifBlank { null } ?: it.merchantRaw?.ifBlank { null } }.distinct()
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

    private fun String.containsAny(vararg keywords: String): Boolean =
        keywords.any { this.contains(it, ignoreCase = true) }
}

