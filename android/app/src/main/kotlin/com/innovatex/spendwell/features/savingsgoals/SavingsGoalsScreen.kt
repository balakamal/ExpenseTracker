package com.innovatex.spendwell.features.savingsgoals

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.innovatex.spendwell.designsystem.theme.PillShape
import com.innovatex.spendwell.domain.model.SavingsGoal
import com.innovatex.spendwell.domain.repository.SavingsGoalRepository
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
class SavingsGoalsViewModel @Inject constructor(
    private val savingsGoalRepository: SavingsGoalRepository
) : ViewModel() {

    val goals: StateFlow<List<SavingsGoal>> = savingsGoalRepository.observeGoals()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createGoal(name: String, targetAmountDouble: Double) {
        viewModelScope.launch {
            val goal = SavingsGoal(
                id = UUID.randomUUID().toString(),
                name = name,
                targetAmount = Money.fromDouble(targetAmountDouble),
                currentSaved = Money.ZERO,
                targetDate = System.currentTimeMillis() + 90L * 24 * 60 * 60 * 1000L,
                category = "SAVINGS"
            )
            savingsGoalRepository.saveGoal(goal)
        }
    }
}

@Composable
fun SavingsGoalsScreen(
    viewModel: SavingsGoalsViewModel,
    modifier: Modifier = Modifier
) {
    val goals by viewModel.goals.collectAsState()
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Savings Goals",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Track progress toward target milestones",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SpendwellPrimaryButton(
                text = "New Goal",
                leadingIcon = Icons.Default.Add,
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth(0.42f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (goals.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                SpendwellEmptyState(
                    title = "No Goals Established",
                    description = "Define milestones for an emergency fund, travel, or vehicle purchase to monitor your progress automatically.",
                    actionText = "Create First Goal",
                    onActionClick = { showAddDialog = true }
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(goals, key = { it.id }) { goal ->
                    GoalCard(goal)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                item {
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }

    if (showAddDialog) {
        AddGoalDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, target ->
                viewModel.createGoal(name, target)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun GoalCard(goal: SavingsGoal) {
    val dateText = SimpleDateFormat("MMM yyyy", Locale.ENGLISH).format(Date(goal.targetDate))

    SpendwellCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = goal.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                SpendwellPillBadge(
                    text = "${String.format(Locale.ENGLISH, "%.0f", goal.progressPercentage)}% SAVED",
                    backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                    textColor = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Saved: ${goal.currentSaved.formatInr(true)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InflowGreen,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Target: ${goal.targetAmount.formatInr(true)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { (goal.progressPercentage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(PillShape),
                color = InflowGreen,
                trackColor = MaterialTheme.colorScheme.secondaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Target date: $dateText • ${goal.remainingAmount.formatInr(true)} remaining",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AddGoalDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, target: Double) -> Unit
) {
    var nameText by remember { mutableStateOf("Emergency Fund") }
    var targetText by remember { mutableStateOf("100000") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Savings Goal", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Goal Title") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = PillShape
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it },
                    label = { Text("Target Amount (₹)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = PillShape
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val target = targetText.toDoubleOrNull() ?: 100000.0
                    onConfirm(nameText, target)
                }
            ) {
                Text("Create Goal", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
