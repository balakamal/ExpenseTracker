package com.innovatex.expensetracker.domain.repository

import com.innovatex.expensetracker.domain.model.MonthlySummary
import com.innovatex.expensetracker.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getTransactionsForMonth(monthKey: String): Flow<List<Transaction>>
    fun getAllTransactions(): Flow<List<Transaction>>
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
}
