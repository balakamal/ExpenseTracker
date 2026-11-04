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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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
import com.innovatex.spendwell.designsystem.components.SpendwellEmptyState
import com.innovatex.spendwell.designsystem.components.SpendwellLoadingState
import com.innovatex.spendwell.designsystem.components.SpendwellTransactionRow
import com.innovatex.spendwell.designsystem.theme.PillShape
import com.innovatex.spendwell.domain.model.Transaction
import com.innovatex.spendwell.domain.model.TransactionStatus
import com.innovatex.spendwell.domain.model.TransactionType
import com.innovatex.spendwell.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class TransactionFeedViewModel @Inject constructor(
    transactionRepository: TransactionRepository
) : ViewModel() {

    val transactions: StateFlow<List<Transaction>> = transactionRepository.observeAllTransactions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
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
                (tx.categoryName.contains(searchQuery, ignoreCase = true))

        val matchesFilter = when (selectedFilter) {
            "DEBITS" -> tx.type == TransactionType.DEBIT
            "CREDITS" -> tx.type == TransactionType.CREDIT
            "REVIEW" -> !tx.isReviewed || tx.status == TransactionStatus.NEEDS_REVIEW
            else -> true
        }

        matchesQuery && matchesFilter
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Transaction Feed",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search merchants, categories, or reference #") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = PillShape,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Filter Chips
        val filters = listOf(
            "ALL" to "All",
            "DEBITS" to "Expenses",
            "CREDITS" to "Income",
            "REVIEW" to "Needs Review"
        )

        LazyRow(modifier = Modifier.fillMaxWidth()) {
            items(filters) { (key, label) ->
                val isSelected = selectedFilter == key
                val bg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                val textColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface

                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(bg)
                        .clickable { selectedFilter = key }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredTransactions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                SpendwellEmptyState(
                    title = "No Transactions Found",
                    description = if (searchQuery.isNotBlank()) "No items match your search '$searchQuery'." else "Your transaction list is currently empty."
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredTransactions, key = { it.id }) { tx ->
                    SpendwellTransactionRow(
                        transaction = tx,
                        onClick = { onNavigateToDetail(tx.id) }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
                item {
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}
