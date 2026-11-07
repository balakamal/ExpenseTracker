package com.innovatex.spendwell.features.analytics

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innovatex.spendwell.analytics.CategorySpending
import com.innovatex.spendwell.analytics.FinancialAnalyticsEngine
import com.innovatex.spendwell.analytics.MonthlyFinancialSummary
import com.innovatex.spendwell.data.preferences.UserPreferencesRepository
import com.innovatex.spendwell.designsystem.components.SpendwellCard
import com.innovatex.spendwell.designsystem.components.SpendwellEmptyState
import com.innovatex.spendwell.designsystem.components.SpendwellLoadingState
import com.innovatex.spendwell.designsystem.components.SpendwellTopAppBar
import com.innovatex.spendwell.designsystem.theme.CardSurfaceWhite
import com.innovatex.spendwell.designsystem.theme.CobaltPrimary
import com.innovatex.spendwell.designsystem.theme.CrimsonTertiary
import com.innovatex.spendwell.designsystem.theme.EmeraldSecondary
import com.innovatex.spendwell.designsystem.theme.OnSecondaryContainer
import com.innovatex.spendwell.designsystem.theme.PillShape
import com.innovatex.spendwell.designsystem.theme.PrimaryFixed
import com.innovatex.spendwell.designsystem.theme.SecondaryFixed
import com.innovatex.spendwell.designsystem.theme.SquircleCardShape
import com.innovatex.spendwell.designsystem.theme.SurfaceContainer
import com.innovatex.spendwell.designsystem.theme.SurfaceContainerLow
import com.innovatex.spendwell.domain.repository.TransactionRepository
import com.innovatex.spendwell.integrations.sms.SmsHistoryImporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

data class AnalyticsCycleItem(
    val offset: Int,
    val label: String,
    val year: Int,
    val month: Int,
    val startTime: Long,
    val endTime: Long
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val smsHistoryImporter: SmsHistoryImporter
) : ViewModel() {

    val cycleStartDay: StateFlow<Int> = userPreferencesRepository.cycleStartDay
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 1)

    val selectedCycleOffset = MutableStateFlow(0)

    val syncedMonths: StateFlow<Set<String>> = userPreferencesRepository.syncedMonths
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val summary: StateFlow<MonthlyFinancialSummary?> = combine(
        transactionRepository.observeAllTransactions(),
        cycleStartDay,
        selectedCycleOffset
    ) { txList, startDay, offset ->
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -offset)
        val targetYear = cal.get(Calendar.YEAR)
        val targetMonth = cal.get(Calendar.MONTH)

        FinancialAnalyticsEngine.analyze(
            transactions = txList,
            year = targetYear,
            monthZeroIndexed = targetMonth,
            cycleStartDay = startDay
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun selectCycleOffset(offset: Int, startTime: Long, endTime: Long) {
        selectedCycleOffset.value = offset
        // Lazy load SMS for this cycle if not already cached
        viewModelScope.launch {
            val key = "${startTime}_${endTime}"
            if (syncedMonths.value.contains(key)) return@launch
            try {
                smsHistoryImporter.importHistoryForRange(startTime, endTime)
                userPreferencesRepository.addSyncedMonth(key)
            } catch (ignored: Exception) {}
        }
    }
}

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    modifier: Modifier = Modifier
) {
    val summary by viewModel.summary.collectAsState()
    val cycleStartDay by viewModel.cycleStartDay.collectAsState()
    val selectedOffset by viewModel.selectedCycleOffset.collectAsState()

    var cycleMenuExpanded by remember { mutableStateOf(false) }

    // Build the 6 cycle options based on current date and cycleStartDay
    val cycles = remember(cycleStartDay) {
        val list = mutableListOf<AnalyticsCycleItem>()
        val cal = Calendar.getInstance()
        for (i in 0..5) {
            val c = cal.clone() as Calendar
            c.add(Calendar.MONTH, -i)
            val y = c.get(Calendar.YEAR)
            val m = c.get(Calendar.MONTH)
            val window = FinancialAnalyticsEngine.calculateCycle(y, m, cycleStartDay)
            val label = if (i == 0) "${window.label} (Current)" else window.label
            list.add(AnalyticsCycleItem(i, label, y, m, window.startTime, window.endTime))
        }
        list
    }

    val currentCycleItem = cycles.find { it.offset == selectedOffset } ?: cycles.first()

    if (summary == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            SpendwellLoadingState(message = "Calculating financial metrics...")
        }
        return
    }

    val s = summary!!

    Scaffold(
        topBar = {
            SpendwellTopAppBar(
                title = "Spendwell",
                subtitle = "Spending Analytics",
                isPrivate = true,
                onSearchClick = {},
                onProfileClick = {}
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            item {
                // Cycle Selector Bar
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CardSurfaceWhite)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable { cycleMenuExpanded = true }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = CobaltPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "FINANCIAL CYCLE",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = currentCycleItem.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Cycle",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = cycleMenuExpanded,
                        onDismissRequest = { cycleMenuExpanded = false }
                    ) {
                        cycles.forEach { cycle ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = cycle.label,
                                        fontWeight = if (cycle.offset == selectedOffset) FontWeight.Bold else FontWeight.Normal,
                                        color = if (cycle.offset == selectedOffset) CobaltPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    viewModel.selectCycleOffset(cycle.offset, cycle.startTime, cycle.endTime)
                                    cycleMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Cash Flow Summary Card (Stitch White Squircle Card)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = SquircleCardShape,
                    colors = CardDefaults.cardColors(containerColor = CardSurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), SquircleCardShape)
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = CobaltPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "CYCLE CASH FLOW",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .clip(PillShape)
                                    .background(SecondaryFixed)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = OnSecondaryContainer,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "SAVINGS ${String.format(Locale.ENGLISH, "%.1f", s.savingsRatePercentage)}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSecondaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Total Inflow (Salary/Income)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = s.totalIncome.formatInr(true),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldSecondary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Net Expenses (Outflow)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = s.totalExpenses.formatInr(true),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = CrimsonTertiary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Daily Velocity & Projections in SurfaceContainerLow
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceContainerLow)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Daily Avg: ${s.averageDailySpend.formatInr(true)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Projected End: ${s.projectedMonthEndSpend.formatInr(true)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = CobaltPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Spending Anomaly Alert
                if (s.anomalies.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = SquircleCardShape,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = CrimsonTertiary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Spending Anomalies Detected",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = CrimsonTertiary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            s.anomalies.take(2).forEach { anomaly ->
                                Text(
                                    text = "• ${anomaly.transaction.merchantName}: ${anomaly.transaction.amount.formatInr(true)} (${anomaly.reason})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Expense by Category",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = currentCycleItem.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            if (s.categoryBreakdown.isEmpty()) {
                item {
                    SpendwellCard(modifier = Modifier.fillMaxWidth()) {
                        SpendwellEmptyState(
                            title = "No Categorized Expenses",
                            description = "When you record transactions or sync SMS for this cycle, your spending distribution across food, utilities, transport and shopping will appear here."
                        )
                    }
                }
            } else {
                items(s.categoryBreakdown, key = { it.categoryId }) { cat ->
                    CategoryBreakdownRow(cat)
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun CategoryBreakdownRow(item: CategorySpending) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = SquircleCardShape,
        colors = CardDefaults.cardColors(containerColor = CardSurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), SquircleCardShape)
                .padding(16.dp)
        ) {
            val catColor = try {
                Color(android.graphics.Color.parseColor(item.colorHex))
            } catch (_: Exception) {
                CobaltPrimary
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(catColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.categoryName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "${item.amount.formatInr(true)} (${String.format(Locale.ENGLISH, "%.1f", item.percentageOfTotal)}%)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            val pct = (item.percentageOfTotal / 100f).coerceIn(0.02f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(SurfaceContainer)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(pct)
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(catColor)
                )
            }
        }
    }
}
