package com.innovatex.expensetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.innovatex.expensetracker.data.local.entity.TransactionEntity
import androidx.room.ColumnInfo
import kotlinx.coroutines.flow.Flow

data class TransactionCursorRow(
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "id") val id: String
)

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE month_key = :monthKey ORDER BY timestamp DESC")
    fun getTransactionsForMonth(monthKey: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getTransactionsForDateRange(startTime: Long, endTime: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int = 5): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Query(
        """
        SELECT * FROM transactions 
        WHERE timestamp BETWEEN :startTime AND :endTime 
          AND ABS(amount - :amount) < 0.01
          AND currency = :currency
        """
    )
    suspend fun findPotentialDuplicates(
        startTime: Long,
        endTime: Long,
        amount: Double,
        currency: String
    ): List<TransactionEntity>

    @Query("SELECT DISTINCT month_key FROM transactions ORDER BY month_key DESC")
    fun getAvailableMonths(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactions(): List<TransactionEntity>

    @Query("SELECT COUNT(*) FROM transactions WHERE user_override_valid IS NULL")
    suspend fun countTransactionsForReprocessing(): Int

    @Query("SELECT timestamp, id FROM transactions ORDER BY timestamp DESC, id DESC LIMIT 1")
    suspend fun getLatestTransactionCursor(): TransactionCursorRow?

    @Query(
        """
        SELECT * FROM transactions
        WHERE user_override_valid IS NULL
          AND (:afterTimestamp IS NULL OR timestamp > :afterTimestamp OR (timestamp = :afterTimestamp AND id > :afterId))
          AND (timestamp < :throughTimestamp OR (timestamp = :throughTimestamp AND id <= :throughId))
        ORDER BY timestamp ASC, id ASC
        LIMIT :limit
        """
    )
    suspend fun getTransactionsForReprocessing(
        afterTimestamp: Long?,
        afterId: String?,
        throughTimestamp: Long,
        throughId: String,
        limit: Int
    ): List<TransactionEntity>

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: String)

    @Query("DELETE FROM transactions WHERE id IN (:ids)")
    suspend fun deleteTransactionsByIds(ids: List<String>)

    @Query("UPDATE transactions SET user_override_valid = :isValid WHERE id = :id")
    suspend fun updateTransactionValidity(id: String, isValid: Boolean?)
}
