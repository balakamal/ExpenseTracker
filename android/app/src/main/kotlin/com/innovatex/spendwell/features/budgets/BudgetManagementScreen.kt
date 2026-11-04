package com.innovatex.spendwell.features.budgets

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innovatex.spendwell.common.Money
import com.innovatex.spendwell.designsystem.components.SpendwellCard
import com.innovatex.spendwell.designsystem.components.SpendwellEmptyState
import com.innovatex.spendwell.designsystem.components.SpendwellPillBadge
import com.innovatex.spendwell.designsystem.components.SpendwellPrimaryButton
import com.innovatex.spendwell.designsystem.theme.InflowGreen
import com.innovatex.spendwell.designsystem.theme.OutflowRed
import com.innovatex.spendwell.designsystem.theme.PillShape
import com.innovatex.spendwell.designsystem.theme.WarningAmber
import com.innovatex.spendwell.domain.model.Budget
import com.innovatex.spendwell.domain.repository.BudgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    private val currentMonthKey = SimpleDateFormat("yyyy-MM", Locale.ENGLISH).format(Date())

    val budgets: StateFlow<List<Budget>> = budgetRepository.observeBudgetsForMonth(currentMonthKey)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addBudget(categoryName: String, limitDouble: Double) {
        viewModelScope.launch {
            val budget = Budget(
                id = UUID.randomUUID().toString(),
                categoryId = "cat_${categoryName.lowercase().replace(" ", "_")}",
                categoryName = categoryName,
                limitAmount = Money.fromDouble(limitDouble),
                spentAmount = Money.ZERO,
                monthPeriod = currentMonthKey,
                alertThresholdPercentage = 85
            )
            budgetRepository.saveBudget(budget)
        }
    }
}

@Composable
fun BudgetManagementScreen(
    viewModel: BudgetViewModel,
    modifier: Modifier = Modifier
) {
    val budgets by viewModel.budgets.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertizontally
        ) {
            Column {
                Text(
                    text = "Budget Management",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Track limits & prevent overspending",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SpendwellPrimaryButton(
                text = "Set Budget",
                leadingIcon = Icons.Default.Add,
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth(0.45f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (budgets.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                SpendwellEmptyState(
                    title = "No Budgets Configured",
                    description = "Set category spending limits to stay on top of your financial goals and receive alerts before exceeding thresholds.",
                    actionText = "Create First Budget",
                    onActionClick = { showAddDialog = true }
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(budgets, key = { it.id }) { budget ->
                    BudgetItemCard(budget)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                item {
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }

    if (showAddDialog) {
        AddBudgetDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { cat, limit ->
                viewModel.addBudget(cat, limit)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun BudgetItemCard(budget: Budget) {
    val pct = budget.percentageUsed
    val progressColor = when {
        budget.isExceeded -> OutflowRed
        pct >= budget.alertThresholdPercentage -> WarningAmber
        else -> InflowGreen
    }

    SpendwellCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertizontally
            ) {
                Text(
                    text = budget.categoryName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                val badgeText = if (budget.isExceeded) "OVER BUDGET" else "${String.format(Locale.ENGLISH, "%.0f", pct)}% USED"
                SpendwellPillBadge(
                    text = badgeText,
                    backgroundColor = if (budget.isExceeded) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                    textColor = if (budget.isExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Spent: ${budget.spentAmount.formatInr(true)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Limit: ${budget.limitAmount.formatInr(true)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { (pct / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(PillShape),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.secondaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Remaining: ${budget.remainingAmount.formatInr(true)}",
                style = MaterialTheme.typography.bodySmall,
                color = if (budget.isExceeded) OutflowRed else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AddBudgetDialog(
    onDismiss: () -> Unit,
    onConfirm: (category: String, limit: Double) -> Unit
) {
    var categoryText by remember { mutableStateOf("Food & Dining") }
    var limitText by remember { mutableStateOf("5000") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Category Budget", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = categoryText,
                    onValueChange = { categoryText = it },
                    label = { Text("Category Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = PillShape
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it },
                    label = { Text("Monthly Limit (₹)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = PillShape
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val limit = limitText.toDoubleOrNull() ?: 5000.0
                    onConfirm(categoryText, limit)
                }
            ) {
                Text("Save", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
