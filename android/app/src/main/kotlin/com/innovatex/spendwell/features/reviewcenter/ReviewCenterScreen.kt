package com.innovatex.spendwell.features.reviewcenter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innovatex.spendwell.designsystem.components.SpendwellCard
import com.innovatex.spendwell.designsystem.components.SpendwellEmptyState
import com.innovatex.spendwell.designsystem.components.SpendwellPillBadge
import com.innovatex.spendwell.designsystem.components.SpendwellPrimaryButton
import com.innovatex.spendwell.designsystem.components.SpendwellTonalButton
import com.innovatex.spendwell.domain.model.DuplicateCandidate
import com.innovatex.spendwell.domain.model.Transaction
import com.innovatex.spendwell.domain.repository.DuplicateRepository
import com.innovatex.spendwell.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReviewItem(
    val candidate: DuplicateCandidate,
    val originalTransaction: Transaction?,
    val duplicateTransaction: Transaction?
)

@HiltViewModel
class ReviewCenterViewModel @Inject constructor(
    private val duplicateRepository: DuplicateRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    val reviewItems: StateFlow<List<ReviewItem>> = combine(
        duplicateRepository.observeUnresolvedDuplicates(),
        transactionRepository.observeAllTransactions()
    ) { candidates, txList ->
        val txMap = txList.associateBy { it.id }
        candidates.map { c ->
            ReviewItem(
                candidate = c,
                originalTransaction = txMap[c.originalTransactionId],
                duplicateTransaction = txMap[c.duplicateTransactionId]
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun mergeCandidate(candidateId: String, duplicateTxId: String) {
        viewModelScope.launch {
            duplicateRepository.resolveDuplicate(candidateId, "MERGED")
            transactionRepository.deleteTransaction(duplicateTxId)
        }
    }

    fun keepBoth(candidateId: String, duplicateTxId: String) {
        viewModelScope.launch {
            duplicateRepository.resolveDuplicate(candidateId, "KEPT_BOTH")
            transactionRepository.markAsReviewed(duplicateTxId)
        }
    }
}

@Composable
fun ReviewCenterScreen(
    viewModel: ReviewCenterViewModel,
    modifier: Modifier = Modifier
) {
    val items by viewModel.reviewItems.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Transaction Review Center",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Review potential duplicates and unverified alerts",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                SpendwellEmptyState(
                    title = "All Caught Up!",
                    description = "Zero flagged duplicate candidates or pending reviews. Your ledger is fully consistent and synchronized."
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(items, key = { it.candidate.id }) { item ->
                    DuplicateReviewCard(
                        item = item,
                        onMerge = { viewModel.mergeCandidate(item.candidate.id, item.candidate.duplicateTransactionId) },
                        onKeepBoth = { viewModel.keepBoth(item.candidate.id, item.candidate.duplicateTransactionId) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                item {
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
private fun DuplicateReviewCard(
    item: ReviewItem,
    onMerge: () -> Unit,
    onKeepBoth: () -> Unit
) {
    SpendwellCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Duplicate Candidate",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                SpendwellPillBadge(
                    text = "${(item.candidate.similarityScore * 100).toInt()}% MATCH",
                    backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                    textColor = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Reason tags
            item.candidate.reasons.forEach { reason ->
                Text(
                    text = "• $reason",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Candidate comparison
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Existing Entry", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text(text = item.originalTransaction?.merchantName ?: "Transaction 1", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(text = item.originalTransaction?.amount?.formatInr(true) ?: "", style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "New Candidate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text(text = item.duplicateTransaction?.merchantName ?: "Transaction 2", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(text = item.duplicateTransaction?.amount?.formatInr(true) ?: "", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SpendwellTonalButton(
                    text = "Keep Both",
                    onClick = onKeepBoth,
                    modifier = Modifier.weight(1f)
                )
                SpendwellPrimaryButton(
                    text = "Merge as One",
                    onClick = onMerge,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
