package com.innovatex.spendwell.features.transactiondetail

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovatex.spendwell.designsystem.components.SpendwellCard
import com.innovatex.spendwell.designsystem.components.SpendwellLoadingState
import com.innovatex.spendwell.designsystem.components.SpendwellPillBadge
import com.innovatex.spendwell.designsystem.components.SpendwellPrimaryButton
import com.innovatex.spendwell.designsystem.components.SpendwellTonalButton
import com.innovatex.spendwell.designsystem.theme.InflowGreen
import com.innovatex.spendwell.designsystem.theme.OutflowRed
import com.innovatex.spendwell.domain.model.Category
import com.innovatex.spendwell.domain.model.Transaction
import com.innovatex.spendwell.domain.model.TransactionType
import com.innovatex.spendwell.domain.repository.CategoryRepository
import com.innovatex.spendwell.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionDetailScreen(
    transactionId: String,
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var transaction by remember { mutableStateOf<Transaction?>(null) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(transactionId) {
        transaction = transactionRepository.getTransactionById(transactionId)
        categories = categoryRepository.observeCategories().first()
    }

    val currentTx = transaction
    if (currentTx == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            SpendwellLoadingState(message = "Loading transaction...")
        }
        return
    }

    val isCredit = currentTx.type == TransactionType.CREDIT
    val amountColor = if (isCredit) InflowGreen else OutflowRed
    val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy, hh:mm a", Locale.ENGLISH)
    val dateText = dateFormat.format(Date(currentTx.timestamp))

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Transaction Details",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Hero Card
        SpendwellCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = MaterialTheme.colorScheme.surface
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = currentTx.merchantName.ifBlank { currentTx.description },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = currentTx.amount.formatSigned(isCredit),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )

                Spacer(modifier = Modifier.height(12.dp))

                SpendwellPillBadge(
                    text = currentTx.financialNature.name.replace("_", " "),
                    backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                    textColor = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Metadata Card
        SpendwellCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                DetailRow(label = "Date & Time", value = dateText)
                DetailRow(label = "Status", value = currentTx.status.name)
                DetailRow(
                    label = "Category",
                    value = currentTx.categoryName,
                    actionText = "Change",
                    onAction = { categoryMenuExpanded = true }
                )

                DropdownMenu(
                    expanded = categoryMenuExpanded,
                    onDismissRequest = { categoryMenuExpanded = false }
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.name) },
                            onClick = {
                                categoryMenuExpanded = false
                                scope.launch {
                                    transactionRepository.updateCategory(currentTx.id, cat.id)
                                    transaction = currentTx.copy(categoryId = cat.id, categoryName = cat.name)
                                }
                            }
                        )
                    }
                }

                if (!currentTx.referenceId.isNullOrBlank()) {
                    DetailRow(label = "Reference / RRN", value = currentTx.referenceId)
                }

                if (!currentTx.utrNumber.isNullOrBlank()) {
                    DetailRow(label = "UTR Number", value = currentTx.utrNumber)
                }

                if (!currentTx.originalTransactionId.isNullOrBlank()) {
                    DetailRow(label = "Linked Original Txn", value = currentTx.originalTransactionId)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Raw Ingested Signal Snippet
        if (!currentTx.rawMessageSnippet.isNullOrBlank()) {
            SpendwellCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Auditable Signal Record",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = currentTx.rawMessageSnippet,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (!currentTx.isReviewed) {
            SpendwellPrimaryButton(
                text = "Mark as Reviewed",
                leadingIcon = Icons.Default.Check,
                onClick = {
                    scope.launch {
                        transactionRepository.markAsReviewed(currentTx.id)
                        onNavigateBack()
                    }
                }
            )
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (actionText != null && onAction != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onAction() }
                )
            }
        }
    }
}
