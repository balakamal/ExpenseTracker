package com.innovatex.spendwell.features.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innovatex.spendwell.common.Money
import com.innovatex.spendwell.designsystem.components.SpendwellEmptyState
import com.innovatex.spendwell.designsystem.components.SpendwellTransactionRow
import com.innovatex.spendwell.designsystem.theme.PillShape
import com.innovatex.spendwell.domain.model.FinancialNature
import com.innovatex.spendwell.domain.model.Transaction
import com.innovatex.spendwell.domain.model.TransactionStatus
import com.innovatex.spendwell.domain.model.TransactionType
import com.innovatex.spendwell.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TransactionFeedViewModel @Inject constructor(
    transactionRepository: TransactionRepository
) : ViewModel() {
    val transactions: StateFlow<List<Transaction>> = transactionRepository.observeAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@Composable
fun TransactionFeedScreen(
    viewModel: TransactionFeedViewModel,
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val allTransactions by viewModel.transactions.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredTransactions = allTransactions.filter { tx ->
        val matchesQuery = searchQuery.isBlank() ||
            tx.merchantName.contains(searchQuery, ignoreCase = true) ||
            tx.description.contains(searchQuery, ignoreCase = true) ||
            (tx.referenceId?.contains(searchQuery, ignoreCase = true) == true) ||
            tx.categoryName.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (selectedFilter) {
            "DEBITS" -> tx.type == TransactionType.DEBIT
            "CREDITS" -> tx.type == TransactionType.CREDIT
            "REVIEW" -> !tx.isReviewed || tx.status == TransactionStatus.NEEDS_REVIEW
            else -> true
        }
        matchesQuery && matchesFilter
    }

    val expenses = allTransactions
        .filter { it.financialNature == FinancialNature.CONSUMPTION_EXPENSE }
        .fold(Money.ZERO) { total, tx -> total + tx.amount }
    val income = allTransactions
        .filter { it.financialNature == FinancialNature.INCOME }
        .fold(Money.ZERO) { total, tx -> total + tx.amount }
    val pendingReview = allTransactions.count {
        !it.isReviewed || it.status == TransactionStatus.NEEDS_REVIEW
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Your ledger",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "${allTransactions.size} recorded transactions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (pendingReview > 0) {
                Text(
                    "$pendingReview to review",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            LedgerSummaryCard(
                title = "Money in",
                amount = income.formatInr(true),
                amountColor = Color(0xFF159B70),
                modifier = Modifier.weight(1f)
            )
            LedgerSummaryCard(
                title = "Spent",
                amount = expenses.formatInr(true),
                amountColor = Color(0xFFE03153),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search merchant, category or reference") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search ledger") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))
        val filters = listOf(
            "ALL" to "All",
            "DEBITS" to "Expenses",
            "CREDITS" to "Income",
            "REVIEW" to "Needs review"
        )
        LazyRow(modifier = Modifier.fillMaxWidth()) {
            items(filters) { (key, label) ->
                val selected = selectedFilter == key
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clip(PillShape)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { selectedFilter = key }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        label,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        if (filteredTransactions.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                SpendwellEmptyState(
                    title = if (allTransactions.isEmpty()) "Your ledger is ready" else "No matching transactions",
                    description = when {
                        allTransactions.isEmpty() -> "Transactions imported from supported sources or added by you will appear here."
                        searchQuery.isNotBlank() -> "No recorded transactions match “$searchQuery”. Try another search."
                        selectedFilter == "REVIEW" -> "There are no transactions currently marked for review."
                        else -> "There are no transactions in this filter."
                    }
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredTransactions, key = { it.id }) { tx ->
                    SpendwellTransactionRow(
                        transaction = tx,
                        onClick = { onNavigateToDetail(tx.id) }
                    )
                    Spacer(Modifier.height(8.dp))
                }
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun LedgerSummaryCard(
    title: String,
    amount: String,
    amountColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text(
                amount,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = amountColor,
                maxLines = 1
            )
        }
    }
}
