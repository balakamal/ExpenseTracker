package com.innovatex.spendwell.features.dashboard

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.sp
import com.innovatex.spendwell.designsystem.components.SpendwellCard
import com.innovatex.spendwell.designsystem.components.SpendwellEmptyState
import com.innovatex.spendwell.designsystem.components.SpendwellLoadingState
import com.innovatex.spendwell.designsystem.components.SpendwellPillBadge
import com.innovatex.spendwell.designsystem.components.SpendwellTransactionRow
import com.innovatex.spendwell.designsystem.theme.DeepCharcoal
import com.innovatex.spendwell.designsystem.theme.InflowGreen
import com.innovatex.spendwell.designsystem.theme.OutflowRed
import com.innovatex.spendwell.designsystem.theme.VividMint
import com.innovatex.spendwell.features.transactions.LogExpenseSheet

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToTransactions: () -> Unit,
    onNavigateToTransactionDetail: (String) -> Unit,
    onNavigateToReviewCenter: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToReconciliation: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var showLogExpenseSheet by remember { mutableStateOf(false) }

    if (state.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            SpendwellLoadingState()
        }
        return
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showLogExpenseSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Log Expense"
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(20.dp))

                // Top Header: App Branding + Privacy Toggle + Settings Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertizontally
                ) {
                    Column {
                        Text(
                            text = "Spendwell",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Encrypted Local Ledger",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertizontally) {
                        IconButton(onClick = { viewModel.toggleBalanceObfuscation() }) {
                            Icon(
                                imageVector = if (state.isBalanceObfuscated) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle Balance Privacy",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Primary Wealth Card (Hero surface: Deep Charcoal #101820)
                SpendwellCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = DeepCharcoal
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertizontally
                        ) {
                            Text(
                                text = "TOTAL NET BALANCE",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFA0B5AF),
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp
                            )
                            SpendwellPillBadge(
                                text = "PRIVATE ENCLAVE",
                                backgroundColor = Color(0xFF1E2D27),
                                textColor = VividMint
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val balanceDisplay = if (state.isBalanceObfuscated) {
                            "₹ ••••••"
                        } else {
                            state.netBalance.formatInr(true)
                        }

                        Text(
                            text = balanceDisplay,
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Income vs Expense summary metrics
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Monthly Inflow",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFA0B5AF)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = state.monthlySummary?.totalIncome?.formatInr(true) ?: "₹ 0.00",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = InflowGreen
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Monthly Outflow",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFA0B5AF)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = state.monthlySummary?.totalExpenses?.formatInr(true) ?: "₹ 0.00",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = OutflowRed
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Review Alerts Banner
                if (state.pendingReviewCount > 0) {
                    SpendwellCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                        onClick = onNavigateToReviewCenter
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertizontally,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertizontally) {
                                Icon(
                                    imageVector = Icons.Default.RateReview,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "${state.pendingReviewCount} Transactions Need Review",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Duplicate candidates or unclassified alerts",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            SpendwellPillBadge(
                                text = "REVIEW",
                                backgroundColor = MaterialTheme.colorScheme.primary,
                                textColor = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Quick Actions Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    QuickActionItem(
                        title = "Accounts",
                        icon = Icons.Default.AccountBalance,
                        onClick = onNavigateToAccounts
                    )
                    QuickActionItem(
                        title = "Reconcile",
                        icon = Icons.Default.CompareArrows,
                        onClick = onNavigateToReconciliation
                    )
                    QuickActionItem(
                        title = "Review",
                        icon = Icons.Default.RateReview,
                        onClick = onNavigateToReviewCenter
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Recent Transactions Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertizontally
                ) {
                    Text(
                        text = "Recent Activity",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "See All",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onNavigateToTransactions() }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            if (state.recentTransactions.isEmpty()) {
                item {
                    SpendwellCard(modifier = Modifier.fillMaxWidth()) {
                        SpendwellEmptyState(
                            title = "No Transactions Recorded",
                            description = "Incoming bank SMS and payment notifications will automatically populate your ledger, or tap + to log one manually."
                        )
                    }
                }
            } else {
                items(state.recentTransactions, key = { it.id }) { tx ->
                    SpendwellTransactionRow(
                        transaction = tx,
                        onClick = { onNavigateToTransactionDetail(tx.id) }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (showLogExpenseSheet) {
        LogExpenseSheet(
            onDismiss = { showLogExpenseSheet = false },
            onSaveTransaction = { tx -> viewModel.saveTransaction(tx) }
        )
    }
}

@Composable
private fun QuickActionItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
