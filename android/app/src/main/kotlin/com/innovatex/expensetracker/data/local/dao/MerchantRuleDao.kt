package com.innovatex.expensetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.innovatex.expensetracker.data.local.entity.MerchantRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantRuleDao {
    @Query("SELECT * FROM merchant_rules ORDER BY priority DESC, created_at DESC")
    fun getAllRules(): Flow<List<MerchantRuleEntity>>

    @Query("SELECT * FROM merchant_rules WHERE pattern = :pattern LIMIT 1")
    suspend fun getRuleByPattern(pattern: String): MerchantRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: MerchantRuleEntity)

    @Update
    suspend fun updateRule(rule: MerchantRuleEntity)

    @Delete
    suspend fun deleteRule(rule: MerchantRuleEntity)
}
