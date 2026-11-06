package com.innovatex.spendwell.domain.repository

import androidx.paging.PagingSource
import com.innovatex.spendwell.database.entity.TransactionEntity
import com.innovatex.spendwell.domain.model.Account
import com.innovatex.spendwell.domain.model.AiInsight
import com.innovatex.spendwell.domain.model.Budget
import com.innovatex.spendwell.domain.model.Category
import com.innovatex.spendwell.domain.model.DuplicateCandidate
import com.innovatex.spendwell.domain.model.RecurringPayment
import com.innovatex.spendwell.domain.model.SavingsGoal
import com.innovatex.spendwell.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun observeAllTransactions(): Flow<List<Transaction>>
    fun observeTransactionsByRange(startTime: Long, endTime: Long): Flow<List<Transaction>>
    fun observeTransactionsPaged(): PagingSource<Int, TransactionEntity>
    fun observePendingReviewTransactions(): Flow<List<Transaction>>
    fun observePendingReviewCount(): Flow<Int>
    suspend fun getTransactionById(id: String): Transaction?
    suspend fun saveTransaction(transaction: Transaction): String
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(id: String)
    suspend fun markAsReviewed(id: String)
    suspend fun updateCategory(id: String, categoryId: String)
}

interface AccountRepository {
    fun observeAccounts(): Flow<List<Account>>
    fun observeAllAccounts(): Flow<List<Account>> = observeAccounts()
    suspend fun getAccountById(id: String): Account?
    suspend fun saveAccount(account: Account)
    suspend fun insertAccount(account: Account) = saveAccount(account)
    suspend fun adjustBalance(accountId: String, deltaPaisa: Long)
}

interface CategoryRepository {
    fun observeCategories(): Flow<List<Category>>
    suspend fun getCategoryById(id: String): Category?
    suspend fun initializeDefaultCategories()
    suspend fun saveCategory(category: Category)
}

interface BudgetRepository {
    fun observeBudgetsForMonth(monthPeriod: String): Flow<List<Budget>>
    suspend fun saveBudget(budget: Budget)
    suspend fun deleteBudget(budget: Budget)
}

interface SavingsGoalRepository {
    fun observeGoals(): Flow<List<SavingsGoal>>
    fun observeAllGoals(): Flow<List<SavingsGoal>> = observeGoals()
    suspend fun getGoalById(id: String): SavingsGoal?
    suspend fun saveGoal(goal: SavingsGoal)
    suspend fun insertGoal(goal: SavingsGoal) = saveGoal(goal)
    suspend fun deleteGoal(goal: SavingsGoal)
}

interface RecurringPaymentRepository {
    fun observeActiveRecurring(): Flow<List<RecurringPayment>>
    suspend fun saveRecurring(recurring: RecurringPayment)
}

interface InsightRepository {
    fun observeUnreadInsights(): Flow<List<AiInsight>>
    fun observeRecentInsights(): Flow<List<AiInsight>>
    suspend fun saveInsight(insight: AiInsight)
    suspend fun markAsRead(id: String)
}

interface DuplicateRepository {
    fun observeUnresolvedDuplicates(): Flow<List<DuplicateCandidate>>
    suspend fun resolveDuplicate(candidateId: String, action: String)
}
