package com.innovatex.expensetracker.presentation.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovatex.expensetracker.domain.model.Transaction
import com.innovatex.expensetracker.presentation.components.ApexFintechSummaryCard
import com.innovatex.expensetracker.presentation.components.ShimmerCardLoader
import com.innovatex.expensetracker.presentation.components.TransactionRow
import com.innovatex.expensetracker.presentation.theme.ApexThemeTokens

@Composable
fun MonthlyDashboardScreen(
    viewModel: DashboardViewModel,
    onSettingsClick: () -> Unit = {},
    onTransactionClick: (Transaction) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current

    Scaffold(
        containerColor = ApexThemeTokens.BgCanvas,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.triggerHistoricalScan(context) },
                containerColor = ApexThemeTokens.AccentPrimary,
                contentColor = ApexThemeTokens.BgCanvas,
                shape = ApexThemeTokens.RadiusMd
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Scan SMS")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // App Bar / Top Navigation Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "EXPENSE TRACKER",
                            color = ApexThemeTokens.AccentPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "Monthly Overview",
                            color = ApexThemeTokens.TextPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier
                            .clip(ApexThemeTokens.RadiusFull)
                            .background(ApexThemeTokens.BgGlassPill)
                            .border(1.dp, ApexThemeTokens.StrokeSubtle, ApexThemeTokens.RadiusFull)
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = ApexThemeTokens.TextSecondary
                        )
                    }
                }
            }

            // Month Carousel Selector Pills
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(uiState.availableMonths) { monthKey ->
                        val isSelected = monthKey == uiState.monthKey
                        val pillBg = if (isSelected) ApexThemeTokens.AccentCobalt else ApexThemeTokens.BgGlassPill
                        val pillTextColor = if (isSelected) ApexThemeTokens.TextPrimary else ApexThemeTokens.TextSecondary
                        val pillBorder = if (isSelected) ApexThemeTokens.AccentCobaltBorder else ApexThemeTokens.StrokeSubtle

                        Box(
                            modifier = Modifier
                                .clip(ApexThemeTokens.RadiusFull)
                                .background(pillBg)
                                .border(1.dp, pillBorder, ApexThemeTokens.RadiusFull)
                                .clickable { viewModel.selectMonth(monthKey) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = monthKey,
                                color = pillTextColor,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Hero FinTech Summary Card
            item {
                AnimatedVisibility(
                    visible = !uiState.isLoading,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    ApexFintechSummaryCard(
                        displayMonth = uiState.displayMonth,
                        totalDebit = uiState.totalDebit,
                        totalCredit = uiState.totalCredit,
                        budgetLimit = uiState.budgetLimit,
                        progressPercent = uiState.budgetProgressPercent
                    )
                }

                if (uiState.isLoading) {
                    ShimmerCardLoader(height = 190)
                }
            }

            // Category Spend Carousel
            if (uiState.categoryTotals.isNotEmpty()) {
                item {
                    Column {
                        Text(
                            text = "CATEGORY VELOCITY",
                            color = ApexThemeTokens.TextTertiary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(uiState.categoryTotals) { spendItem ->
                                Box(
                                    modifier = Modifier
                                        .clip(ApexThemeTokens.RadiusMd)
                                        .background(ApexThemeTokens.BgSurface)
                                        .border(1.dp, ApexThemeTokens.StrokeSubtle, ApexThemeTokens.RadiusMd)
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = spendItem.category.name,
                                            color = ApexThemeTokens.TextSecondary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "₹${String.format(java.util.Locale("en", "IN"), "%,.2f", spendItem.totalSpent)}",
                                            color = ApexThemeTokens.TextPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Recent Transactions Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RECENT TRANSACTIONS",
                        color = ApexThemeTokens.TextTertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${uiState.transactions.size} records",
                        color = ApexThemeTokens.TextTertiary,
                        fontSize = 11.sp
                    )
                }
            }

            // Transaction List Items or Empty State
            if (uiState.transactions.isEmpty() && !uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ApexThemeTokens.RadiusLg)
                            .background(ApexThemeTokens.BgSurface)
                            .border(1.dp, ApexThemeTokens.StrokeSubtle, ApexThemeTokens.RadiusLg)
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Inbox,
                                contentDescription = "No transactions",
                                tint = ApexThemeTokens.TextTertiary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Transactions Recorded",
                                color = ApexThemeTokens.TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Incoming financial SMS alerts will automatically be parsed and categorized here.",
                                color = ApexThemeTokens.TextSecondary,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(uiState.transactions, key = { it.id }) { txn ->
                    val categoryName = uiState.categoryTotals.find { it.category.id == txn.categoryId }?.category?.name ?: "General"
                    val categoryColor = uiState.categoryTotals.find { it.category.id == txn.categoryId }?.category?.colorHex ?: "#10B981"

                    TransactionRow(
                        transaction = txn,
                        categoryName = categoryName,
                        categoryColorHex = categoryColor,
                        onClick = { onTransactionClick(txn) }
                    )
                }
            }
        }
    }
}
