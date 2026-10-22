package com.innovatex.expensetracker.domain.usecase

import com.innovatex.expensetracker.domain.model.MerchantRule
import com.innovatex.expensetracker.domain.model.Transaction
import com.innovatex.expensetracker.domain.model.TransactionType
import com.innovatex.expensetracker.domain.repository.CategoryRepository
import com.innovatex.expensetracker.domain.repository.TransactionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CategorizationRuleTest {

    private val categoryRepository: CategoryRepository = mockk(relaxed = true)
    private val transactionRepository: TransactionRepository = mockk(relaxed = true)
    private lateinit var useCase: UpdateCategoryRuleUseCase

    @BeforeEach
    fun setup() {
        useCase = UpdateCategoryRuleUseCase(categoryRepository, transactionRepository)
    }

    @Test
    fun `test reassign category and save merchant rule`() = runTest {
        val txn = Transaction(
            id = "txn_99",
            amount = 12.0,
            type = TransactionType.DEBIT,
            timestamp = 1000L,
            monthKey = "2026-10",
            merchantRaw = "BLUE BOTTLE",
            merchantClean = "Blue Bottle",
            categoryId = "general"
        )

        coEvery { transactionRepository.getTransactionById("txn_99") } returns txn

        useCase.reassignCategoryAndLearnRule(
            transactionId = "txn_99",
            newCategoryId = "food_dining",
            createRule = true
        )

        coVerify(exactly = 1) {
            transactionRepository.updateTransaction(match { it.categoryId == "food_dining" })
        }

        coVerify(exactly = 1) {
            categoryRepository.saveRule(match { it.pattern == "Blue Bottle" && it.categoryId == "food_dining" })
        }
    }
}
