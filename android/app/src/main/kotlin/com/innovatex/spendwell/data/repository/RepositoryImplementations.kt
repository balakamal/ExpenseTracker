package com.innovatex.spendwell.data.repository

import androidx.paging.PagingSource
import com.innovatex.spendwell.common.Money
import com.innovatex.spendwell.core.DispatcherProvider
import com.innovatex.spendwell.database.dao.AccountDao
import com.innovatex.spendwell.database.dao.AiInsightDao
import com.innovatex.spendwell.database.dao.BudgetDao
import com.innovatex.spendwell.database.dao.CategoryDao
import com.innovatex.spendwell.database.dao.DuplicateCandidateDao
import com.innovatex.spendwell.database.dao.MerchantDao
import com.innovatex.spendwell.database.dao.RecurringPaymentDao
import com.innovatex.spendwell.database.dao.SavingsGoalDao
import com.innovatex.spendwell.database.dao.TransactionDao
import com.innovatex.spendwell.database.entity.AccountEntity
import com.innovatex.spendwell.database.entity.AiInsightEntity
import com.innovatex.spendwell.database.entity.BudgetEntity
import com.innovatex.spendwell.database.entity.CategoryEntity
import com.innovatex.spendwell.database.entity.DuplicateCandidateEntity
import com.innovatex.spendwell.database.entity.RecurringPaymentEntity
import com.innovatex.spendwell.database.entity.SavingsGoalEntity
import com.innovatex.spendwell.database.entity.TransactionEntity
import com.innovatex.spendwell.domain.model.Account
import com.innovatex.spendwell.domain.model.AccountType
import com.innovatex.spendwell.domain.model.AiInsight
import com.innovatex.spendwell.domain.model.Budget
import com.innovatex.spendwell.domain.model.Category
import com.innovatex.spendwell.domain.model.DuplicateCandidate
import com.innovatex.spendwell.domain.model.FinancialNature
import com.innovatex.spendwell.domain.model.RecurringPayment
import com.innovatex.spendwell.domain.model.SavingsGoal
import com.innovatex.spendwell.domain.model.Transaction
import com.innovatex.spendwell.domain.model.TransactionStatus
import com.innovatex.spendwell.domain.model.TransactionType
import com.innovatex.spendwell.domain.repository.AccountRepository
import com.innovatex.spendwell.domain.repository.BudgetRepository
import com.innovatex.spendwell.domain.repository.CategoryRepository
import com.innovatex.spendwell.domain.repository.DuplicateRepository
import com.innovatex.spendwell.domain.repository.InsightRepository
import com.innovatex.spendwell.domain.repository.RecurringPaymentRepository
import com.innovatex.spendwell.domain.repository.SavingsGoalRepository
import com.innovatex.spendwell.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val merchantDao: MerchantDao,
    private val dispatchers: DispatcherProvider
) : TransactionRepository {

    override fun observeAllTransactions(): Flow<List<Transaction>> {
        return combine(
            transactionDao.observeAllTransactions(),
            categoryDao.observeCategories(),
            merchantDao.observeMerchants()
        ) { txList, catList, merchList ->
            val catMap = catList.associateBy { it.id }
            val merchMap = merchList.associateBy { it.id }
            txList.map { it.toDomain(catMap, merchMap) }
        }.flowOn(dispatchers.io)
    }

    override fun observeTransactionsByRange(startTime: Long, endTime: Long): Flow<List<Transaction>> {
        return combine(
            transactionDao.observeTransactionsByRange(startTime, endTime),
            categoryDao.observeCategories(),
            merchantDao.observeMerchants()
        ) { txList, catList, merchList ->
            val catMap = catList.associateBy { it.id }
            val merchMap = merchList.associateBy { it.id }
            txList.map { it.toDomain(catMap, merchMap) }
        }.flowOn(dispatchers.io)
    }

    override fun observeTransactionsPaged(): PagingSource<Int, TransactionEntity> {
        return transactionDao.observeTransactionsPaged()
    }

    override fun observePendingReviewTransactions(): Flow<List<Transaction>> {
        return combine(
            transactionDao.observePendingReviewTransactions(),
            categoryDao.observeCategories(),
            merchantDao.observeMerchants()
        ) { txList, catList, merchList ->
            val catMap = catList.associateBy { it.id }
            val merchMap = merchList.associateBy { it.id }
            txList.map { it.toDomain(catMap, merchMap) }
        }.flowOn(dispatchers.io)
    }

    override fun observePendingReviewCount(): Flow<Int> {
        return transactionDao.observePendingReviewCount().flowOn(dispatchers.io)
    }

    override suspend fun getTransactionById(id: String): Transaction? = withContext(dispatchers.io) {
        val entity = transactionDao.getTransactionById(id) ?: return@withContext null
        val cat = entity.categoryId?.let { categoryDao.getCategoryById(it) }
        val catMap = cat?.let { mapOf(it.id to it) } ?: emptyMap()
        entity.toDomain(catMap, emptyMap())
    }

    override suspend fun saveTransaction(transaction: Transaction): String = withContext(dispatchers.io) {
        val id = if (transaction.id.isNotBlank()) transaction.id else UUID.randomUUID().toString()
        val entity = transaction.copy(id = id).toEntity()
        transactionDao.insert(entity)
        id
    }

    override suspend fun updateTransaction(transaction: Transaction) = withContext(dispatchers.io) {
        transactionDao.update(transaction.toEntity())
    }

    override suspend fun deleteTransaction(id: String) = withContext(dispatchers.io) {
        val existing = transactionDao.getTransactionById(id)
        if (existing != null) {
            transactionDao.delete(existing)
        }
    }

    override suspend fun markAsReviewed(id: String) = withContext(dispatchers.io) {
        val existing = transactionDao.getTransactionById(id) ?: return@withContext
        val updated = existing.copy(
            isReviewed = true,
            status = if (existing.status == TransactionStatus.NEEDS_REVIEW.name) TransactionStatus.SUCCESSFUL.name else existing.status,
            updatedAt = System.currentTimeMillis()
        )
        transactionDao.update(updated)
    }

    override suspend fun updateCategory(id: String, categoryId: String) = withContext(dispatchers.io) {
        val existing = transactionDao.getTransactionById(id) ?: return@withContext
        transactionDao.update(existing.copy(categoryId = categoryId, isReviewed = true, updatedAt = System.currentTimeMillis()))
    }
}

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao,
    private val dispatchers: DispatcherProvider
) : AccountRepository {

    override fun observeAccounts(): Flow<List<Account>> {
        return accountDao.observeActiveAccounts().map { list ->
            list.map { it.toDomain() }
        }.flowOn(dispatchers.io)
    }

    override suspend fun getAccountById(id: String): Account? = withContext(dispatchers.io) {
        accountDao.getAccountById(id)?.toDomain()
    }

    override suspend fun saveAccount(account: Account) = withContext(dispatchers.io) {
        accountDao.insert(account.toEntity())
    }

    override suspend fun adjustBalance(accountId: String, deltaPaisa: Long) = withContext(dispatchers.io) {
        accountDao.adjustBalance(accountId, deltaPaisa)
    }
}

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao,
    private val dispatchers: DispatcherProvider
) : CategoryRepository {

    override fun observeCategories(): Flow<List<Category>> {
        return categoryDao.observeCategories().map { list ->
            list.map { it.toDomain() }
        }.flowOn(dispatchers.io)
    }

    override suspend fun getCategoryById(id: String): Category? = withContext(dispatchers.io) {
        categoryDao.getCategoryById(id)?.toDomain()
    }

    override suspend fun initializeDefaultCategories() = withContext(dispatchers.io) {
        val defaults = listOf(
            CategoryEntity("cat_food", "Food & Dining", "restaurant", "#FF7A30", isIncome = false, isSystemDefault = true),
            CategoryEntity("cat_groceries", "Groceries", "shopping_cart", "#159B70", isIncome = false, isSystemDefault = true),
            CategoryEntity("cat_shopping", "Shopping", "shopping_bag", "#8B5CF6", isIncome = false, isSystemDefault = true),
            CategoryEntity("cat_transport", "Transportation", "directions_car", "#3B82F6", isIncome = false, isSystemDefault = true),
            CategoryEntity("cat_utilities", "Utilities & Bills", "receipt", "#EAB308", isIncome = false, isSystemDefault = true),
            CategoryEntity("cat_housing", "Housing & Rent", "home", "#6366F1", isIncome = false, isSystemDefault = true),
            CategoryEntity("cat_entertainment", "Entertainment", "movie", "#EC4899", isIncome = false, isSystemDefault = true),
            CategoryEntity("cat_health", "Health & Medical", "local_hospital", "#EF4444", isIncome = false, isSystemDefault = true),
            CategoryEntity("cat_salary", "Salary & Income", "payments", "#10B981", isIncome = true, isSystemDefault = true),
            CategoryEntity("cat_investment", "Investments", "trending_up", "#06B6D4", isIncome = false, isSystemDefault = true),
            CategoryEntity("cat_transfer", "Transfer & Payments", "swap_horiz", "#64748B", isIncome = false, isSystemDefault = true),
            CategoryEntity("cat_misc", "Miscellaneous", "category", "#94A3B8", isIncome = false, isSystemDefault = true)
        )
        categoryDao.insertDefaultCategories(defaults)
    }

    override suspend fun saveCategory(category: Category) = withContext(dispatchers.io) {
        categoryDao.insert(category.toEntity())
    }
}

@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val dispatchers: DispatcherProvider
) : BudgetRepository {

    override fun observeBudgetsForMonth(monthPeriod: String): Flow<List<Budget>> {
        return combine(
            budgetDao.observeBudgetsForMonth(monthPeriod),
            categoryDao.observeCategories()
        ) { budgets, categories ->
            val catMap = categories.associateBy { it.id }
            val (startTime, endTime) = parseMonthBounds(monthPeriod)
            val txList = transactionDao.getTransactionsByRange(startTime, endTime)

            budgets.map { b ->
                val spentPaisa = txList
                    .filter { it.categoryId == b.categoryId && it.type == TransactionType.DEBIT.name }
                    .sumOf { it.amountPaisa }

                Budget(
                    id = b.id,
                    categoryId = b.categoryId,
                    categoryName = catMap[b.categoryId]?.name ?: "Category",
                    limitAmount = Money.fromPaisa(b.limitAmountPaisa),
                    spentAmount = Money.fromPaisa(spentPaisa),
                    monthPeriod = b.monthPeriod,
                    alertThresholdPercentage = b.alertThresholdPercentage
                )
            }
        }.flowOn(dispatchers.io)
    }

    override suspend fun saveBudget(budget: Budget) = withContext(dispatchers.io) {
        val entity = BudgetEntity(
            id = budget.id.ifBlank { UUID.randomUUID().toString() },
            categoryId = budget.categoryId,
            limitAmountPaisa = budget.limitAmount.toPaisa(),
            monthPeriod = budget.monthPeriod,
            alertThresholdPercentage = budget.alertThresholdPercentage
        )
        budgetDao.insertOrUpdate(entity)
    }

    override suspend fun deleteBudget(budget: Budget) = withContext(dispatchers.io) {
        val entity = BudgetEntity(
            id = budget.id,
            categoryId = budget.categoryId,
            limitAmountPaisa = budget.limitAmount.toPaisa(),
            monthPeriod = budget.monthPeriod,
            alertThresholdPercentage = budget.alertThresholdPercentage
        )
        budgetDao.delete(entity)
    }

    private fun parseMonthBounds(monthPeriod: String): Pair<Long, Long> {
        val parts = monthPeriod.split("-")
        val year = parts.getOrNull(0)?.toIntOrNull() ?: 2026
        val month = (parts.getOrNull(1)?.toIntOrNull() ?: 1) - 1

        val cal = Calendar.getInstance()
        cal.set(year, month, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.set(year, month, cal.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis

        return Pair(start, end)
    }
}

@Singleton
class SavingsGoalRepositoryImpl @Inject constructor(
    private val savingsGoalDao: SavingsGoalDao,
    private val dispatchers: DispatcherProvider
) : SavingsGoalRepository {

    override fun observeGoals(): Flow<List<SavingsGoal>> {
        return savingsGoalDao.observeGoals().map { list ->
            list.map { it.toDomain() }
        }.flowOn(dispatchers.io)
    }

    override suspend fun getGoalById(id: String): SavingsGoal? = withContext(dispatchers.io) {
        savingsGoalDao.getGoalById(id)?.toDomain()
    }

    override suspend fun saveGoal(goal: SavingsGoal) = withContext(dispatchers.io) {
        savingsGoalDao.insert(goal.toEntity())
    }

    override suspend fun deleteGoal(goal: SavingsGoal) = withContext(dispatchers.io) {
        savingsGoalDao.delete(goal.toEntity())
    }
}

@Singleton
class RecurringPaymentRepositoryImpl @Inject constructor(
    private val recurringPaymentDao: RecurringPaymentDao,
    private val dispatchers: DispatcherProvider
) : RecurringPaymentRepository {

    override fun observeActiveRecurring(): Flow<List<RecurringPayment>> {
        return recurringPaymentDao.observeActiveRecurring().map { list ->
            list.map { it.toDomain() }
        }.flowOn(dispatchers.io)
    }

    override suspend fun saveRecurring(recurring: RecurringPayment) = withContext(dispatchers.io) {
        recurringPaymentDao.insert(recurring.toEntity())
    }
}

@Singleton
class InsightRepositoryImpl @Inject constructor(
    private val aiInsightDao: AiInsightDao,
    private val dispatchers: DispatcherProvider
) : InsightRepository {

    override fun observeUnreadInsights(): Flow<List<AiInsight>> {
        return aiInsightDao.observeUnreadInsights().map { list ->
            list.map { it.toDomain() }
        }.flowOn(dispatchers.io)
    }

    override fun observeRecentInsights(): Flow<List<AiInsight>> {
        return aiInsightDao.observeRecentInsights().map { list ->
            list.map { it.toDomain() }
        }.flowOn(dispatchers.io)
    }

    override suspend fun saveInsight(insight: AiInsight) = withContext(dispatchers.io) {
        aiInsightDao.insert(insight.toEntity())
    }

    override suspend fun markAsRead(id: String) = withContext(dispatchers.io) {
        aiInsightDao.markAsRead(id)
    }
}

@Singleton
class DuplicateRepositoryImpl @Inject constructor(
    private val duplicateCandidateDao: DuplicateCandidateDao,
    private val dispatchers: DispatcherProvider
) : DuplicateRepository {

    override fun observeUnresolvedDuplicates(): Flow<List<DuplicateCandidate>> {
        return duplicateCandidateDao.observeUnresolvedDuplicates().map { list ->
            list.map { it.toDomain() }
        }.flowOn(dispatchers.io)
    }

    override suspend fun resolveDuplicate(candidateId: String, action: String) = withContext(dispatchers.io) {
        duplicateCandidateDao.resolve(candidateId, action)
    }
}

// ----------------- MAPPING EXTENSIONS -----------------

private fun TransactionEntity.toDomain(
    catMap: Map<String, CategoryEntity>,
    merchMap: Map<String, com.innovatex.spendwell.database.entity.MerchantEntity>
): Transaction {
    val cat = categoryId?.let { catMap[it] }
    val merch = merchantId?.let { merchMap[it] }
    return Transaction(
        id = id,
        amount = Money.fromPaisa(amountPaisa, currency),
        type = try { TransactionType.valueOf(type) } catch (e: Exception) { TransactionType.DEBIT },
        status = try { TransactionStatus.valueOf(status) } catch (e: Exception) { TransactionStatus.SUCCESSFUL },
        financialNature = try { FinancialNature.valueOf(financialNature) } catch (e: Exception) { FinancialNature.CONSUMPTION_EXPENSE },
        timestamp = timestamp,
        accountId = accountId,
        toAccountId = toAccountId,
        merchantId = merchantId,
        merchantName = merch?.cleanName ?: description,
        categoryId = categoryId,
        categoryName = cat?.name ?: "Miscellaneous",
        description = description,
        referenceId = referenceId,
        utrNumber = utrNumber,
        rawMessageSnippet = rawMessageSnippet,
        isReviewed = isReviewed,
        originalTransactionId = originalTransactionId,
        recurringPaymentId = recurringPaymentId,
        createdAt = createdAt
    )
}

private fun Transaction.toEntity(): TransactionEntity {
    return TransactionEntity(
        id = id,
        amountPaisa = amount.toPaisa(),
        currency = amount.currencyCode,
        type = type.name,
        status = status.name,
        financialNature = financialNature.name,
        timestamp = timestamp,
        accountId = accountId,
        toAccountId = toAccountId,
        merchantId = merchantId,
        categoryId = categoryId,
        description = description,
        referenceId = referenceId,
        utrNumber = utrNumber,
        rawMessageSnippet = rawMessageSnippet,
        isReviewed = isReviewed,
        originalTransactionId = originalTransactionId,
        recurringPaymentId = recurringPaymentId,
        createdAt = createdAt,
        updatedAt = System.currentTimeMillis()
    )
}

private fun AccountEntity.toDomain(): Account {
    return Account(
        id = id,
        accountName = accountName,
        accountType = try { AccountType.valueOf(accountType) } catch (e: Exception) { AccountType.SAVINGS },
        accountNumberLast4 = accountNumberLast4,
        bankName = bankName,
        currentBalance = Money.fromPaisa(currentBalancePaisa, currency),
        isPrimary = isPrimary,
        isActive = isActive
    )
}

private fun Account.toEntity(): AccountEntity {
    return AccountEntity(
        id = id,
        accountName = accountName,
        accountType = accountType.name,
        accountNumberLast4 = accountNumberLast4,
        bankName = bankName,
        currentBalancePaisa = currentBalance.toPaisa(),
        currency = currentBalance.currencyCode,
        isPrimary = isPrimary,
        isActive = isActive
    )
}

private fun CategoryEntity.toDomain(): Category {
    return Category(
        id = id,
        name = name,
        iconName = iconName,
        colorHex = colorHex,
        isIncome = isIncome,
        isSystemDefault = isSystemDefault,
        parentCategoryId = parentCategoryId
    )
}

private fun Category.toEntity(): CategoryEntity {
    return CategoryEntity(
        id = id,
        name = name,
        iconName = iconName,
        colorHex = colorHex,
        isIncome = isIncome,
        isSystemDefault = isSystemDefault,
        parentCategoryId = parentCategoryId
    )
}

private fun SavingsGoalEntity.toDomain(): SavingsGoal {
    return SavingsGoal(
        id = id,
        name = name,
        targetAmount = Money.fromPaisa(targetAmountPaisa),
        currentSaved = Money.fromPaisa(currentSavedPaisa),
        targetDate = targetDate,
        category = category,
        isCompleted = isCompleted
    )
}

private fun SavingsGoal.toEntity(): SavingsGoalEntity {
    return SavingsGoalEntity(
        id = id,
        name = name,
        targetAmountPaisa = targetAmount.toPaisa(),
        currentSavedPaisa = currentSaved.toPaisa(),
        targetDate = targetDate,
        category = category,
        isCompleted = isCompleted
    )
}

private fun RecurringPaymentEntity.toDomain(): RecurringPayment {
    return RecurringPayment(
        id = id,
        merchantId = merchantId,
        title = title,
        amount = Money.fromPaisa(amountPaisa),
        cadence = cadence,
        expectedDayOfMonth = expectedDayOfMonth,
        nextDueDate = nextDueDate,
        isActive = isActive,
        lastBilledDate = lastBilledDate,
        categoryId = categoryId
    )
}

private fun RecurringPayment.toEntity(): RecurringPaymentEntity {
    return RecurringPaymentEntity(
        id = id,
        merchantId = merchantId,
        title = title,
        amountPaisa = amount.toPaisa(),
        cadence = cadence,
        expectedDayOfMonth = expectedDayOfMonth,
        nextDueDate = nextDueDate,
        isActive = isActive,
        lastBilledDate = lastBilledDate,
        categoryId = categoryId
    )
}

private fun AiInsightEntity.toDomain(): AiInsight {
    return AiInsight(
        id = id,
        title = title,
        message = message,
        type = type,
        actionPayloadJson = actionPayloadJson,
        isRead = isRead,
        createdAt = createdAt
    )
}

private fun AiInsight.toEntity(): AiInsightEntity {
    return AiInsightEntity(
        id = id,
        title = title,
        message = message,
        type = type,
        actionPayloadJson = actionPayloadJson,
        isRead = isRead,
        createdAt = createdAt
    )
}

private fun DuplicateCandidateEntity.toDomain(): DuplicateCandidate {
    val reasonsList = try {
        val arr = JSONArray(reasonsJson)
        (0 until arr.length()).map { arr.getString(it) }
    } catch (e: Exception) {
        listOf("Similarity threshold match")
    }
    return DuplicateCandidate(
        id = id,
        originalTransactionId = originalTransactionId,
        duplicateTransactionId = duplicateTransactionId,
        similarityScore = similarityScore,
        reasons = reasonsList,
        resolved = resolved,
        detectedAt = detectedAt
    )
}
