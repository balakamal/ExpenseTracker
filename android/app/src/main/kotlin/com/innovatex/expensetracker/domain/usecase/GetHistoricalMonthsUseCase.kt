package com.innovatex.expensetracker.domain.usecase

import com.innovatex.expensetracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetHistoricalMonthsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(): Flow<List<String>> =
        transactionRepository.getAvailableMonths()
}
