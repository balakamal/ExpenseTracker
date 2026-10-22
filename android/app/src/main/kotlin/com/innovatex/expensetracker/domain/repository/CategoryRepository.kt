package com.innovatex.expensetracker.domain.repository

import com.innovatex.expensetracker.domain.model.Category
import com.innovatex.expensetracker.domain.model.MerchantRule
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getAllCategories(): Flow<List<Category>>
    suspend fun getCategoryById(id: String): Category?
    suspend fun saveCategory(category: Category)
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(category: Category)

    fun getAllRules(): Flow<List<MerchantRule>>
    suspend fun getRuleByPattern(pattern: String): MerchantRule?
    suspend fun saveRule(rule: MerchantRule)
    suspend fun deleteRule(rule: MerchantRule)
}
