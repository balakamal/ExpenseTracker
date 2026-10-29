package com.innovatex.expensetracker.domain.repository

import com.innovatex.expensetracker.domain.model.AuthenticityLabel
import com.innovatex.expensetracker.domain.model.MonthlySummary
import com.innovatex.expensetracker.domain.model.SuspectedSpamItem
import com.innovatex.expensetracker.domain.model.Transaction
import com.innovatex.expensetracker.domain.model.TransactionCursor
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getTransactionsForMonth(monthKey: String): Flow<List<Transaction>>
    fun getAllTransactions(): Flow<List<Transaction>>
    suspend fun countTransactionsForReprocessing(): Int
    suspend fun latestTransactionCursor(): TransactionCursor?
    suspend fun getTransactionsForReprocessing(
        after: TransactionCursor?,
        through: TransactionCursor,
        limit: Int
    ): List<Transaction>
    fun getTransactionsForDate(startTime: Long, endTime: Long): Flow<List<Transaction>>
    fun getRecentTransactions(limit: Int = 5): Flow<List<Transaction>>
    suspend fun getTransactionById(id: String): Transaction?
    suspend fun saveTransaction(transaction: Transaction): String
    suspend fun saveTransactions(transactions: List<Transaction>)
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(id: String)
    suspend fun findPotentialDuplicates(
        startTime: Long,
        endTime: Long,
        amount: Double,
        currency: String
    ): List<Transaction>
    fun getAvailableMonths(): Flow<List<String>>
    fun getMonthlySummary(monthKey: String): Flow<MonthlySummary>
    fun getOverallSummary(): Flow<MonthlySummary>
    suspend fun purgeDuplicates(): Int
    suspend fun purgeSpamTransactions(): Int
    suspend fun getSuspectedSpamItems(): List<SuspectedSpamItem>
    suspend fun confirmSpam(sender: String, transactionIds: List<String>)
    suspend fun trustSender(sender: String)
    suspend fun deleteTransactionsBySender(sender: String): Int
    suspend fun setTransactionValidity(id: String, isValid: Boolean)
    suspend fun recordUserFeedback(sender: String?, merchantClean: String, rawSnippet: String?, label: AuthenticityLabel)
}
