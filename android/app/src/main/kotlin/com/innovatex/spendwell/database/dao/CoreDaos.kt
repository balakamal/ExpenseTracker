package com.innovatex.spendwell.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.innovatex.spendwell.database.entity.AccountEntity
import com.innovatex.spendwell.database.entity.AiInsightEntity
import com.innovatex.spendwell.database.entity.BudgetEntity
import com.innovatex.spendwell.database.entity.CategoryEntity
import com.innovatex.spendwell.database.entity.CreditCardEntity
import com.innovatex.spendwell.database.entity.DuplicateCandidateEntity
import com.innovatex.spendwell.database.entity.MerchantEntity
import com.innovatex.spendwell.database.entity.ReconciliationSessionEntity
import com.innovatex.spendwell.database.entity.RecurringPaymentEntity
import com.innovatex.spendwell.database.entity.SavingsGoalEntity
import com.innovatex.spendwell.database.entity.TransactionSourceEventEntity
import com.innovatex.spendwell.database.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM financial_accounts WHERE isActive = 1 ORDER BY isPrimary DESC, accountName ASC")
    fun observeActiveAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM financial_accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: String): AccountEntity?

    @Query("SELECT * FROM financial_accounts WHERE accountNumberLast4 = :last4 AND bankName = :bankName LIMIT 1")
    suspend fun findAccountByLast4AndBank(last4: String, bankName: String): AccountEntity?

    @Query("SELECT * FROM financial_accounts WHERE accountNumberLast4 = :last4 LIMIT 1")
    suspend fun findAccountByLast4(last4: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: AccountEntity)

    @Update
    suspend fun update(account: AccountEntity)

    @Query("UPDATE financial_accounts SET currentBalancePaisa = currentBalancePaisa + :deltaPaisa WHERE id = :accountId")
    suspend fun adjustBalance(accountId: String, deltaPaisa: Long)

    @Query("SELECT * FROM credit_cards ORDER BY cardName ASC")
    fun observeCreditCards(): Flow<List<CreditCardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreditCard(card: CreditCardEntity)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun findByName(name: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaultCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE monthPeriod = :monthPeriod")
    fun observeBudgetsForMonth(monthPeriod: String): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND monthPeriod = :monthPeriod LIMIT 1")
    suspend fun getBudgetForCategoryAndMonth(categoryId: String, monthPeriod: String): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(budget: BudgetEntity)

    @Delete
    suspend fun delete(budget: BudgetEntity)
}

@Dao
interface SavingsGoalDao {
    @Query("SELECT * FROM savings_goals ORDER BY isCompleted ASC, targetDate ASC")
    fun observeGoals(): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals WHERE id = :id LIMIT 1")
    suspend fun getGoalById(id: String): SavingsGoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: SavingsGoalEntity)

    @Update
    suspend fun update(goal: SavingsGoalEntity)

    @Delete
    suspend fun delete(goal: SavingsGoalEntity)
}

@Dao
interface MerchantDao {
    @Query("SELECT * FROM merchants ORDER BY cleanName ASC")
    fun observeMerchants(): Flow<List<MerchantEntity>>

    @Query("SELECT * FROM merchants WHERE LOWER(cleanName) = LOWER(:cleanName) LIMIT 1")
    suspend fun findByCleanName(cleanName: String): MerchantEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(merchant: MerchantEntity)
}

@Dao
interface RecurringPaymentDao {
    @Query("SELECT * FROM recurring_payments WHERE isActive = 1 ORDER BY nextDueDate ASC")
    fun observeActiveRecurring(): Flow<List<RecurringPaymentEntity>>

    @Query("SELECT * FROM recurring_payments WHERE merchantId = :merchantId LIMIT 1")
    suspend fun findByMerchantId(merchantId: String): RecurringPaymentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recurring: RecurringPaymentEntity)

    @Update
    suspend fun update(recurring: RecurringPaymentEntity)
}

@Dao
interface TransactionSourceEventDao {
    @Query("SELECT * FROM transaction_source_events WHERE hashSignature = :hash LIMIT 1")
    suspend fun findByHash(hash: String): TransactionSourceEventEntity?

    @Query("SELECT * FROM transaction_source_events WHERE canonicalTransactionId = :canonicalId ORDER BY receivedTimestamp ASC")
    suspend fun getEventsForCanonical(canonicalId: String): List<TransactionSourceEventEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(event: TransactionSourceEventEntity): Long
}

@Dao
interface DuplicateCandidateDao {
    @Query("SELECT * FROM duplicate_candidates WHERE resolved = 0 ORDER BY detectedAt DESC")
    fun observeUnresolvedDuplicates(): Flow<List<DuplicateCandidateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(candidate: DuplicateCandidateEntity)

    @Query("UPDATE duplicate_candidates SET resolved = 1, resolutionAction = :action WHERE id = :candidateId")
    suspend fun resolve(candidateId: String, action: String)
}

@Dao
interface AiInsightDao {
    @Query("SELECT * FROM ai_insights WHERE isRead = 0 ORDER BY createdAt DESC")
    fun observeUnreadInsights(): Flow<List<AiInsightEntity>>

    @Query("SELECT * FROM ai_insights ORDER BY createdAt DESC LIMIT 20")
    fun observeRecentInsights(): Flow<List<AiInsightEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(insight: AiInsightEntity)

    @Query("UPDATE ai_insights SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)
}

@Dao
interface ReconciliationDao {
    @Query("SELECT * FROM reconciliation_sessions ORDER BY createdAt DESC")
    fun observeSessions(): Flow<List<ReconciliationSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: ReconciliationSessionEntity)

    @Update
    suspend fun update(session: ReconciliationSessionEntity)
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles WHERE id = 'primary_user' LIMIT 1")
    fun observeProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profiles WHERE id = 'primary_user' LIMIT 1")
    suspend fun getProfile(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: UserProfileEntity)
}
