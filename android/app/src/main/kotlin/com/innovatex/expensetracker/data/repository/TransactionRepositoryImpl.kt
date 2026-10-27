package com.innovatex.expensetracker.data.repository

import com.innovatex.expensetracker.data.local.dao.TransactionDao
import com.innovatex.expensetracker.data.local.entity.TransactionEntity
import com.innovatex.expensetracker.di.IoDispatcher
import com.innovatex.expensetracker.domain.model.MonthlySummary
import com.innovatex.expensetracker.domain.model.Transaction
import com.innovatex.expensetracker.domain.model.TransactionType
import com.innovatex.expensetracker.domain.repository.TransactionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
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
}
