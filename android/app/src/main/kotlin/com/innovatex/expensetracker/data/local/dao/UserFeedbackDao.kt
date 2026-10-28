package com.innovatex.expensetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.innovatex.expensetracker.data.local.entity.UserFeedbackExemplarEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserFeedbackDao {

    @Query("SELECT * FROM user_feedback_exemplars ORDER BY created_at DESC")
    fun getAllExemplarsFlow(): Flow<List<UserFeedbackExemplarEntity>>

    @Query("SELECT * FROM user_feedback_exemplars ORDER BY created_at DESC")
    suspend fun getAllExemplars(): List<UserFeedbackExemplarEntity>

    @Query("SELECT * FROM user_feedback_exemplars WHERE sender = :sender ORDER BY created_at DESC LIMIT 1")
    suspend fun findLatestForSender(sender: String): UserFeedbackExemplarEntity?

    @Query("SELECT * FROM user_feedback_exemplars WHERE merchant_clean = :merchantClean ORDER BY created_at DESC LIMIT 1")
    suspend fun findLatestForMerchant(merchantClean: String): UserFeedbackExemplarEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExemplar(exemplar: UserFeedbackExemplarEntity)

    @Query("DELETE FROM user_feedback_exemplars WHERE id = :id")
    suspend fun deleteExemplar(id: String)

    @Query("DELETE FROM user_feedback_exemplars WHERE sender = :sender")
    suspend fun deleteExemplarsForSender(sender: String)
}

