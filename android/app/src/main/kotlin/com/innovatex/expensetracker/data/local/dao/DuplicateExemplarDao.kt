package com.innovatex.expensetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.innovatex.expensetracker.data.local.entity.DuplicateExemplarEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DuplicateExemplarDao {

    @Query("SELECT * FROM duplicate_exemplars ORDER BY created_at DESC")
    fun getAllExemplarsFlow(): Flow<List<DuplicateExemplarEntity>>

    @Query("SELECT * FROM duplicate_exemplars ORDER BY created_at DESC LIMIT :limit")
    suspend fun getRecentExemplars(limit: Int = 10): List<DuplicateExemplarEntity>

    @Query(
        """
        SELECT * FROM duplicate_exemplars 
        WHERE ABS(amount - :amount) < 0.01 
           OR merchant_clean_a = :merchantClean 
           OR merchant_clean_b = :merchantClean
        ORDER BY created_at DESC 
        LIMIT :limit
        """
    )
    suspend fun findMatchingExemplars(
        amount: Double,
        merchantClean: String,
        limit: Int = 5
    ): List<DuplicateExemplarEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExemplar(exemplar: DuplicateExemplarEntity)

    @Query("DELETE FROM duplicate_exemplars WHERE id = :id")
    suspend fun deleteExemplar(id: String)
}
