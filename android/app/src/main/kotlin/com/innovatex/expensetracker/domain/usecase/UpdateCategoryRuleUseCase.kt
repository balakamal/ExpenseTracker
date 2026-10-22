package com.innovatex.expensetracker.domain.usecase

import com.innovatex.expensetracker.domain.model.MerchantRule
import com.innovatex.expensetracker.domain.repository.CategoryRepository
import com.innovatex.expensetracker.domain.repository.TransactionRepository
import javax.inject.Inject

class UpdateCategoryRuleUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository
) {
    suspend fun reassignCategoryAndLearnRule(
        transactionId: String,
        newCategoryId: String,
        createRule: Boolean
    ) {
        val transaction = transactionRepository.getTransactionById(transactionId) ?: return

        // 1. Update current transaction
        val updated = transaction.copy(categoryId = newCategoryId)
        transactionRepository.updateTransaction(updated)

        // 2. Persist rule if requested
        if (createRule && transaction.merchantClean.isNotBlank()) {
            categoryRepository.saveRule(
                MerchantRule(
                    pattern = transaction.merchantClean,
                    categoryId = newCategoryId,
                    isRegex = false,
                    priority = 100
                )
            )
        }
    }
}
