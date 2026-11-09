package com.innovatex.spendwell.features.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
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
import com.innovatex.spendwell.analytics.CustomRangeFinancialSummary
import com.innovatex.spendwell.analytics.DailyFinancialSummary
import com.innovatex.spendwell.analytics.DaySpendBar
import com.innovatex.spendwell.analytics.FinancialAnalyticsEngine
import com.innovatex.spendwell.analytics.MerchantSpending
import com.innovatex.spendwell.analytics.MonthlyFinancialSummary
import com.innovatex.spendwell.analytics.TimeBlockSpending
import com.innovatex.spendwell.analytics.TimeGranularity
import com.innovatex.spendwell.analytics.WeeklyFinancialSummary
import com.innovatex.spendwell.data.preferences.UserPreferencesRepository
import com.innovatex.spendwell.designsystem.components.SpendwellBezierCumulativeBurnChart
import com.innovatex.spendwell.designsystem.components.SpendwellCard
import com.innovatex.spendwell.designsystem.components.SpendwellDonutChart
import com.innovatex.spendwell.designsystem.components.SpendwellEmptyState
import com.innovatex.spendwell.designsystem.components.SpendwellFilterPill
import com.innovatex.spendwell.designsystem.components.SpendwellLoadingState
import com.innovatex.spendwell.designsystem.components.SpendwellTopAppBar
import com.innovatex.spendwell.designsystem.components.SpendwellWeeklyInteractiveBarChart
import com.innovatex.spendwell.designsystem.theme.CardSurfaceWhite
import com.innovatex.spendwell.designsystem.theme.CobaltPrimary
import com.innovatex.spendwell.designsystem.theme.CrimsonTertiary
import com.innovatex.spendwell.designsystem.theme.EmeraldSecondary
import com.innovatex.spendwell.designsystem.theme.OnSecondaryContainer
import com.innovatex.spendwell.designsystem.theme.PillShape
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
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

    val selectedGranularity = MutableStateFlow(TimeGranularity.MONTHLY)

    val cycleStartDay: StateFlow<Int> = userPreferencesRepository.cycleStartDay
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 1)

    // Navigation states
    val selectedDailyTimestamp = MutableStateFlow(System.currentTimeMillis())
    val selectedWeekOffset = MutableStateFlow(0)
    val selectedCycleOffset = MutableStateFlow(0)
    val selectedCustomRange = MutableStateFlow<Pair<Long, Long>>(
        Pair(
            System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000L),
            System.currentTimeMillis()
        )
    )

    val syncedMonths: StateFlow<Set<String>> = userPreferencesRepository.syncedMonths
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val allTransactions = transactionRepository.observeAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // 1. Daily Summary
    val dailySummary: StateFlow<DailyFinancialSummary?> = combine(
        allTransactions,
        selectedDailyTimestamp
    ) { txList, timestamp ->
        FinancialAnalyticsEngine.analyzeDaily(txList, timestamp)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // 2. Weekly Summary
    val weeklySummary: StateFlow<WeeklyFinancialSummary?> = combine(
        allTransactions,
        selectedWeekOffset
    ) { txList, offset ->
        FinancialAnalyticsEngine.analyzeWeekly(txList, offset)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // 3. Monthly Summary
    val monthlySummary: StateFlow<MonthlyFinancialSummary?> = combine(
        allTransactions,
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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // 4. Custom Range Summary
    val customSummary: StateFlow<CustomRangeFinancialSummary?> = combine(
        allTransactions,
        selectedCustomRange
    ) { txList, range ->
        FinancialAnalyticsEngine.analyzeCustomRange(txList, range.first, range.second)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setGranularity(granularity: TimeGranularity) {
        selectedGranularity.value = granularity
    }

    fun previousDay() {
        selectedDailyTimestamp.value -= 86400_000L
    }

    fun nextDay() {
        val next = selectedDailyTimestamp.value + 86400_000L
        if (next <= System.currentTimeMillis() + 86400_000L) {
            selectedDailyTimestamp.value = next
        }
    }

    fun previousWeek() {
        selectedWeekOffset.value += 1
    }

    fun nextWeek() {
        if (selectedWeekOffset.value > 0) {
            selectedWeekOffset.value -= 1
        }
    }

    fun selectCycleOffset(offset: Int, startTime: Long, endTime: Long) {
        selectedCycleOffset.value = offset
        viewModelScope.launch {
            val key = "${startTime}_${endTime}"
            if (syncedMonths.value.contains(key)) return@launch
            try {
                smsHistoryImporter.importHistoryForRange(startTime, endTime)
                userPreferencesRepository.addSyncedMonth(key)
            } catch (ignored: Exception) {}
        }
    }

    fun setCustomRange(start: Long, end: Long) {
        selectedCustomRange.value = Pair(start, end)
    }

    fun setCustomPreset(days: Int) {
        val end = System.currentTimeMillis()
        val start = end - (days.toLong() * 24 * 60 * 60 * 1000L)
        selectedCustomRange.value = Pair(start, end)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val granularity by viewModel.selectedGranularity.collectAsState()
    val cycleStartDay by viewModel.cycleStartDay.collectAsState()
    val selectedOffset by viewModel.selectedCycleOffset.collectAsState()
    val selectedWeekOffset by viewModel.selectedWeekOffset.collectAsState()
    val selectedDailyTimestamp by viewModel.selectedDailyTimestamp.collectAsState()
    val selectedCustomRange by viewModel.selectedCustomRange.collectAsState()

    val dailySummary by viewModel.dailySummary.collectAsState()
    val weeklySummary by viewModel.weeklySummary.collectAsState()
    val monthlySummary by viewModel.monthlySummary.collectAsState()
    val customSummary by viewModel.customSummary.collectAsState()

    var cycleMenuExpanded by remember { mutableStateOf(false) }
    var showCustomRangeDialog by remember { mutableStateOf(false) }

    // Pre-calculate 6 month options
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

    Scaffold(
        topBar = {
            SpendwellTopAppBar(
                title = "Spendwell",
                subtitle = "Financial Intelligence",
                isPrivate = true,
                onSearchClick = {},
                onProfileClick = onNavigateToSettings
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
            // 1. Time Granularity Selector Pills
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TimeGranularity.entries.forEach { g ->
                        SpendwellFilterPill(
                            label = g.title,
                            isSelected = granularity == g,
                            onClick = { viewModel.setGranularity(g) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 2. Period Navigation Header
            item {
                when (granularity) {
                    TimeGranularity.DAILY -> {
                        DailyNavigator(
                            label = dailySummary?.dateLabel ?: "Select Date",
                            isToday = android.text.format.DateUtils.isToday(selectedDailyTimestamp),
                            onPrevious = { viewModel.previousDay() },
                            onNext = { viewModel.nextDay() }
                        )
                    }

                    TimeGranularity.WEEKLY -> {
                        WeeklyNavigator(
                            label = weeklySummary?.weekLabel ?: "Select Week",
                            isCurrentWeek = selectedWeekOffset == 0,
                            onPrevious = { viewModel.previousWeek() },
                            onNext = { viewModel.nextWeek() }
                        )
                    }

                    TimeGranularity.MONTHLY -> {
                        MonthlyNavigator(
                            currentLabel = currentCycleItem.label,
                            cycles = cycles,
                            selectedOffset = selectedOffset,
                            menuExpanded = cycleMenuExpanded,
                            onToggleMenu = { cycleMenuExpanded = !cycleMenuExpanded },
                            onSelectCycle = { c ->
                                viewModel.selectCycleOffset(c.offset, c.startTime, c.endTime)
                                cycleMenuExpanded = false
                            }
                        )
                    }

                    TimeGranularity.CUSTOM -> {
                        CustomRangeNavigator(
                            label = customSummary?.rangeLabel ?: "Custom Duration",
                            onOpenPicker = { showCustomRangeDialog = true },
                            onPresetSelect = { days -> viewModel.setCustomPreset(days) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 3. Stats Content based on selected Granularity
            when (granularity) {
                TimeGranularity.DAILY -> {
                    if (dailySummary == null) {
                        item { SpendwellLoadingState(message = "Analyzing day transactions...") }
                    } else {
                        val d = dailySummary!!
                        item {
                            DailyStatsCard(d)
                            Spacer(modifier = Modifier.height(16.dp))
                            DailyTimeBlocksCard(d.timeBlocks, d.microTxnCount, d.microTxnTotal)
                            if (d.categoryBreakdown.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                SpendwellDonutChart(categories = d.categoryBreakdown, totalExpenses = d.totalExpenses)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            SectionHeader(title = "Expenses by Category", subtitle = d.dateLabel)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        if (d.categoryBreakdown.isEmpty()) {
                            item { EmptyTransactionsCard(title = "No Expenses Today", description = "No consumption debits recorded for this day.") }
                        } else {
                            items(d.categoryBreakdown, key = { it.categoryId }) { cat ->
                                CategoryBreakdownRow(cat)
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                        if (d.topMerchants.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                TopMerchantsCard(d.topMerchants)
                            }
                        }
                    }
                }

                TimeGranularity.WEEKLY -> {
                    if (weeklySummary == null) {
                        item { SpendwellLoadingState(message = "Analyzing weekly spend...") }
                    } else {
                        val w = weeklySummary!!
                        item {
                            WeeklyStatsCard(w)
                            Spacer(modifier = Modifier.height(16.dp))
                            SpendwellWeeklyInteractiveBarChart(
                                bars = w.dailyBars,
                                peakDayLabel = w.peakDayLabel,
                                weekdayTotal = w.weekdayTotal,
                                weekendTotal = w.weekendTotal,
                                weekendVariance = w.weekendVariancePercentage
                            )
                            if (w.categoryBreakdown.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                SpendwellDonutChart(categories = w.categoryBreakdown, totalExpenses = w.totalExpenses)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            SectionHeader(title = "Expenses by Category", subtitle = w.weekLabel)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        if (w.categoryBreakdown.isEmpty()) {
                            item { EmptyTransactionsCard(title = "No Expenses This Week", description = "No debits recorded in this weekly window.") }
                        } else {
                            items(w.categoryBreakdown, key = { it.categoryId }) { cat ->
                                CategoryBreakdownRow(cat)
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                        if (w.topMerchants.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                TopMerchantsCard(w.topMerchants)
                            }
                        }
                    }
                }

                TimeGranularity.MONTHLY -> {
                    if (monthlySummary == null) {
                        item { SpendwellLoadingState(message = "Calculating financial metrics...") }
                    } else {
                        val m = monthlySummary!!
                        item {
                            MonthlyStatsCard(m)
                            Spacer(modifier = Modifier.height(16.dp))
                            if (m.cumulativeSpendPoints.isNotEmpty()) {
                                SpendwellBezierCumulativeBurnChart(
                                    points = m.cumulativeSpendPoints,
                                    strokeColor = CobaltPrimary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            if (m.categoryBreakdown.isNotEmpty()) {
                                SpendwellDonutChart(categories = m.categoryBreakdown, totalExpenses = m.totalExpenses)
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            if (m.anomalies.isNotEmpty()) {
                                AnomaliesAlertCard(m.anomalies)
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            SectionHeader(title = "Expenses by Category", subtitle = currentCycleItem.label)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        if (m.categoryBreakdown.isEmpty()) {
                            item { EmptyTransactionsCard(title = "No Categorized Expenses", description = "When you record transactions or sync SMS, spending distribution appears here.") }
                        } else {
                            items(m.categoryBreakdown, key = { it.categoryId }) { cat ->
                                CategoryBreakdownRow(cat)
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                        if (m.topMerchants.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                TopMerchantsCard(m.topMerchants)
                            }
                        }
                    }
                }

                TimeGranularity.CUSTOM -> {
                    if (customSummary == null) {
                        item { SpendwellLoadingState(message = "Aggregating custom period...") }
                    } else {
                        val c = customSummary!!
                        item {
                            CustomRangeStatsCard(c)
                            Spacer(modifier = Modifier.height(16.dp))
                            if (c.cumulativeSpendPoints.isNotEmpty()) {
                                SpendwellBezierCumulativeBurnChart(
                                    points = c.cumulativeSpendPoints,
                                    strokeColor = CobaltPrimary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            if (c.categoryBreakdown.isNotEmpty()) {
                                SpendwellDonutChart(categories = c.categoryBreakdown, totalExpenses = c.totalExpenses)
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            if (c.anomalies.isNotEmpty()) {
                                AnomaliesAlertCard(c.anomalies)
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            SectionHeader(title = "Expenses by Category", subtitle = c.rangeLabel)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        if (c.categoryBreakdown.isEmpty()) {
                            item { EmptyTransactionsCard(title = "No Expenses in Range", description = "No debits found between the chosen dates.") }
                        } else {
                            items(c.categoryBreakdown, key = { it.categoryId }) { cat ->
                                CategoryBreakdownRow(cat)
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                        if (c.topMerchants.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                TopMerchantsCard(c.topMerchants)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }

    // Material 3 Date Range Picker Dialog
    if (showCustomRangeDialog) {
        val dateRangePickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = selectedCustomRange.first,
            initialSelectedEndDateMillis = selectedCustomRange.second
        )

        DatePickerDialog(
            onDismissRequest = { showCustomRangeDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val start = dateRangePickerState.selectedStartDateMillis
                        val end = dateRangePickerState.selectedEndDateMillis
                        if (start != null && end != null) {
                            viewModel.setCustomRange(start, end)
                        }
                        showCustomRangeDialog = false
                    }
                ) {
                    Text("Apply Range", fontWeight = FontWeight.Bold, color = CobaltPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomRangeDialog = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                title = { Text("Select Duration", modifier = Modifier.padding(16.dp)) },
                modifier = Modifier.height(420.dp)
            )
        }
    }
}

// -------------------------------------------------------------
// NAVIGATOR COMPONENTS
// -------------------------------------------------------------

@Composable
private fun DailyNavigator(
    label: String,
    isToday: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardSurfaceWhite)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day", tint = CobaltPrimary)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = CobaltPrimary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        IconButton(onClick = onNext, enabled = !isToday) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Next Day",
                tint = if (!isToday) CobaltPrimary else MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

@Composable
private fun WeeklyNavigator(
    label: String,
    isCurrentWeek: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardSurfaceWhite)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Week", tint = CobaltPrimary)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.DateRange, contentDescription = null, tint = CobaltPrimary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        IconButton(onClick = onNext, enabled = !isCurrentWeek) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Next Week",
                tint = if (!isCurrentWeek) CobaltPrimary else MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

@Composable
private fun MonthlyNavigator(
    currentLabel: String,
    cycles: List<AnalyticsCycleItem>,
    selectedOffset: Int,
    menuExpanded: Boolean,
    onToggleMenu: () -> Unit,
    onSelectCycle: (AnalyticsCycleItem) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CardSurfaceWhite)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .clickable { onToggleMenu() }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = CobaltPrimary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "FINANCIAL CYCLE",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = currentLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Cycle", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        DropdownMenu(expanded = menuExpanded, onDismissRequest = onToggleMenu) {
            cycles.forEach { cycle ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = cycle.label,
                            fontWeight = if (cycle.offset == selectedOffset) FontWeight.Bold else FontWeight.Normal,
                            color = if (cycle.offset == selectedOffset) CobaltPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = { onSelectCycle(cycle) }
                )
            }
        }
    }
}

@Composable
private fun CustomRangeNavigator(
    label: String,
    onOpenPicker: () -> Unit,
    onPresetSelect: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CardSurfaceWhite)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .clickable { onOpenPicker() }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DateRange, contentDescription = null, tint = CobaltPrimary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "CUSTOM PERIOD (TAP TO CHANGE)",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Pick Date Range", tint = CobaltPrimary)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Preset Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(7 to "Last 7d", 14 to "Last 14d", 30 to "Last 30d", 90 to "Last 90d").forEach { (days, title) ->
                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(SurfaceContainerLow)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), PillShape)
                        .clickable { onPresetSelect(days) }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(text = title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = CobaltPrimary)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// STATS HERO CARDS
// -------------------------------------------------------------

@Composable
private fun DailyStatsCard(d: DailyFinancialSummary) {
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
                    Icon(Icons.Default.FlashOn, contentDescription = null, tint = CobaltPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "DAILY RUN-RATE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }

                // Comparison vs 30-day baseline
                val isMore = d.comparisonTo30DayAvgPct > 0
                val badgeBg = if (isMore) MaterialTheme.colorScheme.errorContainer else SecondaryFixed
                val badgeTint = if (isMore) CrimsonTertiary else OnSecondaryContainer
                Row(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(badgeBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(if (isMore) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward, contentDescription = null, tint = badgeTint, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${String.format(Locale.ENGLISH, "%.1f", Math.abs(d.comparisonTo30DayAvgPct))}% vs 30d avg",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeTint
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = "Inflow Credits", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = d.totalIncome.formatInr(true), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = EmeraldSecondary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Today's Outflow", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = d.totalExpenses.formatInr(true), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = CrimsonTertiary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceContainerLow)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "${d.transactionCount} Transactions", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = "Avg Ticket: ${d.averageTicketSize.formatInr(true)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = CobaltPrimary)
            }
        }
    }
}

@Composable
private fun DailyTimeBlocksCard(timeBlocks: List<TimeBlockSpending>, microCount: Int, microTotal: com.innovatex.spendwell.common.Money) {
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccessTime, contentDescription = null, tint = CobaltPrimary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "SPENDING BY TIME OF DAY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }

            Spacer(modifier = Modifier.height(14.dp))

            timeBlocks.forEach { block ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = block.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text(text = "${block.amount.formatInr(true)} (${block.transactionCount})", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                val pct = (block.percentageOfTotal / 100f).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(SurfaceContainer)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (pct > 0) pct else 0.01f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(if (pct > 0) CobaltPrimary else Color.Transparent)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (microCount > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SecondaryFixed.copy(alpha = 0.5f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Quick UPI spends (≤ ₹100)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = OnSecondaryContainer)
                    Text(text = "$microCount txns (${microTotal.formatInr(true)})", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = OnSecondaryContainer)
                }
            }
        }
    }
}

@Composable
private fun WeeklyStatsCard(w: WeeklyFinancialSummary) {
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
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = CobaltPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "WEEKLY CASH FLOW", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }

                // WoW Badge
                val isDown = w.weekOverWeekPercentage <= 0
                val badgeBg = if (isDown) SecondaryFixed else MaterialTheme.colorScheme.errorContainer
                val badgeTint = if (isDown) OnSecondaryContainer else CrimsonTertiary
                Row(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(badgeBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(if (isDown) Icons.Default.TrendingDown else Icons.Default.TrendingUp, contentDescription = null, tint = badgeTint, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "WoW ${String.format(Locale.ENGLISH, "%+.1f", w.weekOverWeekPercentage)}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeTint
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = "Total Income", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = w.totalIncome.formatInr(true), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = EmeraldSecondary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Weekly Outflow", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = w.totalExpenses.formatInr(true), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = CrimsonTertiary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceContainerLow)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Daily Pace: ${w.averageDailySpend.formatInr(true)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = "Peak: ${w.peakDayLabel}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = CobaltPrimary)
            }
        }
    }
}


@Composable
private fun MonthlyStatsCard(m: MonthlyFinancialSummary) {
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
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CobaltPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "MONTHLY CASH FLOW", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }

                Row(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(SecondaryFixed)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = OnSecondaryContainer, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "SAVINGS ${String.format(Locale.ENGLISH, "%.1f", m.savingsRatePercentage)}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = OnSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = "Total Inflow (Income)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = m.totalIncome.formatInr(true), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = EmeraldSecondary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Net Expenses", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = m.totalExpenses.formatInr(true), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = CrimsonTertiary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceContainerLow)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Daily Avg: ${m.averageDailySpend.formatInr(true)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = "Projected End: ${m.projectedMonthEndSpend.formatInr(true)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = CobaltPrimary)
            }
        }
    }
}

@Composable
private fun CustomRangeStatsCard(c: CustomRangeFinancialSummary) {
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
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = CobaltPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "CUSTOM PERIOD TOTALS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }

                // Comparison chip
                val isDown = c.periodOverPeriodPercentage <= 0
                val badgeBg = if (isDown) SecondaryFixed else MaterialTheme.colorScheme.errorContainer
                val badgeTint = if (isDown) OnSecondaryContainer else CrimsonTertiary
                Row(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(badgeBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(if (isDown) Icons.Default.TrendingDown else Icons.Default.TrendingUp, contentDescription = null, tint = badgeTint, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${String.format(Locale.ENGLISH, "%+.1f", c.periodOverPeriodPercentage)}% vs prior period",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeTint
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = "Period Inflow", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = c.totalIncome.formatInr(true), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = EmeraldSecondary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Period Outflow", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = c.totalExpenses.formatInr(true), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = CrimsonTertiary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceContainerLow)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Duration: ${c.totalDays} Days", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = "Daily Burn: ${c.averageDailySpend.formatInr(true)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = CobaltPrimary)
            }
        }
    }
}

// -------------------------------------------------------------
// ANOMALIES & SHARED COMPONENTS
// -------------------------------------------------------------

@Composable
private fun AnomaliesAlertCard(anomalies: List<com.innovatex.spendwell.analytics.SpendingAnomaly>) {
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
            anomalies.take(2).forEach { anomaly ->
                Text(
                    text = "• ${anomaly.transaction.merchantName}: ${anomaly.transaction.amount.formatInr(true)} (${anomaly.reason})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun TopMerchantsCard(merchants: List<MerchantSpending>) {
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Storefront, contentDescription = null, tint = CobaltPrimary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "TOP RECIPIENTS & MERCHANTS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            merchants.forEach { m ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = m.merchantName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(text = "${m.amount.formatInr(true)} (${m.transactionCount})", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = CobaltPrimary)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyTransactionsCard(title: String, description: String) {
    SpendwellCard(modifier = Modifier.fillMaxWidth()) {
        SpendwellEmptyState(title = title, description = description)
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
