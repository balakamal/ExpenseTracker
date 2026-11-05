package com.innovatex.spendwell.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.innovatex.spendwell.database.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun observeAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun observeTransactionsPaged(): PagingSource<Int, TransactionEntity>

    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun observeTransactionsByRange(startTime: Long, endTime: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getTransactionsByRange(startTime: Long, endTime: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE isReviewed = 0 OR status = 'NEEDS_REVIEW' ORDER BY timestamp DESC")
    fun observePendingReviewTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT COUNT(*) FROM transactions WHERE isReviewed = 0 OR status = 'NEEDS_REVIEW'")
    fun observePendingReviewCount(): Flow<Int>

    @Query("SELECT * FROM transactions WHERE (referenceId IS NOT NULL AND referenceId = :refId) OR (utrNumber IS NOT NULL AND utrNumber = :utr) LIMIT 5")
    suspend fun findCandidatesByUtrOrRef(refId: String?, utr: String?): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE amountPaisa = :amountPaisa AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun findCandidatesByAmountAndWindow(amountPaisa: Long, startTime: Long, endTime: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE originalTransactionId = :originalId")
    suspend fun getLinkedTransactions(originalId: String): List<TransactionEntity>

    @Query("DELETE FROM transactions")
    suspend fun clearAll()
}
