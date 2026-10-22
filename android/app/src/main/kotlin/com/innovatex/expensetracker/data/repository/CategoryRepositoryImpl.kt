package com.innovatex.expensetracker.data.repository

import com.innovatex.expensetracker.data.local.dao.CategoryDao
import com.innovatex.expensetracker.data.local.dao.MerchantRuleDao
import com.innovatex.expensetracker.data.local.entity.CategoryEntity
import com.innovatex.expensetracker.data.local.entity.MerchantRuleEntity
import com.innovatex.expensetracker.di.IoDispatcher
import com.innovatex.expensetracker.domain.model.Category
import com.innovatex.expensetracker.domain.model.MerchantRule
import com.innovatex.expensetracker.domain.repository.CategoryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao,
    private val merchantRuleDao: MerchantRuleDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : CategoryRepository {

    override fun getAllCategories(): Flow<List<Category>> =
        categoryDao.getAllCategories()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override suspend fun getCategoryById(id: String): Category? = withContext(ioDispatcher) {
        categoryDao.getCategoryById(id)?.toDomain()
    }

    override suspend fun saveCategory(category: Category) = withContext(ioDispatcher) {
        categoryDao.insertCategory(CategoryEntity.fromDomain(category))
    }

    override suspend fun updateCategory(category: Category) = withContext(ioDispatcher) {
        categoryDao.updateCategory(CategoryEntity.fromDomain(category))
    }

    override suspend fun deleteCategory(category: Category) = withContext(ioDispatcher) {
        categoryDao.deleteCategory(CategoryEntity.fromDomain(category))
    }

    override fun getAllRules(): Flow<List<MerchantRule>> =
        merchantRuleDao.getAllRules()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override suspend fun getRuleByPattern(pattern: String): MerchantRule? = withContext(ioDispatcher) {
        merchantRuleDao.getRuleByPattern(pattern)?.toDomain()
    }

    override suspend fun saveRule(rule: MerchantRule) = withContext(ioDispatcher) {
        merchantRuleDao.insertRule(MerchantRuleEntity.fromDomain(rule))
    }

    override suspend fun deleteRule(rule: MerchantRule) = withContext(ioDispatcher) {
        merchantRuleDao.deleteRule(MerchantRuleEntity.fromDomain(rule))
    }
}
