package com.innovatex.expensetracker.presentation.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovatex.expensetracker.domain.model.SalaryCycleHelper
import com.innovatex.expensetracker.domain.model.Transaction
import com.innovatex.expensetracker.domain.model.TransactionType
import com.innovatex.expensetracker.presentation.components.getCategoryIconVector
import com.innovatex.expensetracker.presentation.theme.PastelKineticTokens
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class CalendarSelectionMode {
    SINGLE_DAY,
    DATE_RANGE,
    WHOLE_MONTH
}

data class CalendarGridCell(
    val dayNumber: Int,
    val monthOffset: Int, // -1 = prev month, 0 = current month, 1 = next month
    val timestampMillis: Long,
    val isCurrentMonth: Boolean
)

@Composable
fun PastelCalendarMatrixView(
    currentMonthMillis: Long,
    allTransactions: List<Transaction>,
    currencySymbol: String = "₹",
    monthlyBudget: Double = 25000.0,
    salaryCreditDay: Int = 1,
    selectedDateRange: Pair<Long, Long>? = null,
    onDateRangeSelected: ((Pair<Long, Long>) -> Unit)? = null,
    onClearDateSelection: (() -> Unit)? = null,
    onTransactionClick: (Transaction) -> Unit = {},
    onLogForDayClick: () -> Unit = {},
    onExportClick: () -> Unit = {},
    onPreviousMonth: () -> Unit = {},
    onNextMonth: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val numberFormat = remember {
        NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }

    // Determine initial selection mode from incoming range
    var selectionMode by remember(selectedDateRange) {
        mutableStateOf(
            when {
                selectedDateRange == null -> CalendarSelectionMode.WHOLE_MONTH
                isSameDay(selectedDateRange.first, selectedDateRange.second) -> CalendarSelectionMode.SINGLE_DAY
                else -> CalendarSelectionMode.DATE_RANGE
            }
        )
    }

    // Internal date state
    var selectedSingleDayMillis by remember(selectedDateRange) {
        mutableStateOf(selectedDateRange?.first ?: System.currentTimeMillis())
    }
    var rangeStartMillis by remember(selectedDateRange) {
        mutableStateOf(selectedDateRange?.first)
    }
    var rangeEndMillis by remember(selectedDateRange) {
        mutableStateOf(selectedDateRange?.second)
    }

    val monthHeader = remember(currentMonthMillis, salaryCreditDay) {
        if (salaryCreditDay == 1) {
            SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(currentMonthMillis))
        } else {
            val key = SalaryCycleHelper.getCycleMonthKey(currentMonthMillis, salaryCreditDay)
            SalaryCycleHelper.formatCycleLabel(key, salaryCreditDay)
        }
    }

    // Generate complete 35 or 42 grid cells for the current calendar month
    val calendarCells = remember(currentMonthMillis) {
        getCalendarCellsForMonth(currentMonthMillis)
    }

    // Map each day start timestamp to (hasCredit, hasDebit)
    val dayActivityMap = remember(allTransactions, currentMonthMillis) {
        val map = mutableMapOf<Long, Pair<Boolean, Boolean>>()
        val cal = Calendar.getInstance()
        for (txn in allTransactions) {
            if (!txn.isCountableTransaction) continue
            cal.timeInMillis = txn.timestamp
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val dayStart = cal.timeInMillis
            val existing = map.getOrDefault(dayStart, Pair(false, false))
            val isCredit = txn.type == TransactionType.CREDIT
            map[dayStart] = Pair(existing.first || isCredit, existing.second || !isCredit)
        }
        map
    }

    val todayStartMillis = remember { getStartOfDay(System.currentTimeMillis()) }

    // Compute effective timestamp bounds based on active selection mode
    val (filterStartMillis, filterEndMillis) = remember(
        selectionMode,
        selectedSingleDayMillis,
        rangeStartMillis,
        rangeEndMillis,
        currentMonthMillis,
        salaryCreditDay
    ) {
        when (selectionMode) {
            CalendarSelectionMode.SINGLE_DAY -> {
                Pair(getStartOfDay(selectedSingleDayMillis), getEndOfDay(selectedSingleDayMillis))
            }
            CalendarSelectionMode.DATE_RANGE -> {
                val start = rangeStartMillis ?: currentMonthMillis
                val end = rangeEndMillis ?: rangeStartMillis ?: currentMonthMillis
                val minT = minOf(start, end)
                val maxT = maxOf(start, end)
                Pair(getStartOfDay(minT), getEndOfDay(maxT))
            }
            CalendarSelectionMode.WHOLE_MONTH -> {
                SalaryCycleHelper.getCycleRangeForTimestamp(currentMonthMillis, salaryCreditDay)
            }
        }
    }

    // Title label for the chosen selection
    val selectedPeriodTitle = remember(selectionMode, filterStartMillis, filterEndMillis, salaryCreditDay, currentMonthMillis) {
        when (selectionMode) {
            CalendarSelectionMode.SINGLE_DAY -> {
                SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault()).format(Date(filterStartMillis))
            }
            CalendarSelectionMode.DATE_RANGE -> {
                val df = SimpleDateFormat("dd MMM", Locale.getDefault())
                val daysCount = ((filterEndMillis - filterStartMillis) / 86400000L) + 1
                "${df.format(Date(filterStartMillis))} - ${df.format(Date(filterEndMillis))} · $daysCount Days"
            }
            CalendarSelectionMode.WHOLE_MONTH -> {
                if (salaryCreditDay == 1) {
                    "Whole Month · " + SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(filterStartMillis))
                } else {
                    "Cycle · " + SalaryCycleHelper.formatCycleLabel(
                        SalaryCycleHelper.getCycleMonthKey(currentMonthMillis, salaryCreditDay),
                        salaryCreditDay
                    )
                }
            }
        }
    }

    // Filter transactions to active date bounds
    val periodTransactions = remember(allTransactions, filterStartMillis, filterEndMillis) {
        allTransactions.filter { it.isCountableTransaction && it.timestamp in filterStartMillis..filterEndMillis }
    }
    val periodDebits = periodTransactions.filter { it.type == TransactionType.DEBIT }
    val periodCredits = periodTransactions.filter { it.type == TransactionType.CREDIT }
    val periodDebitTotal = periodDebits.sumOf { it.amount }
    val periodCreditTotal = periodCredits.sumOf { it.amount }

    // Dynamic Daily Cap & Pacing
    val daysInActivePeriod = remember(selectionMode, filterStartMillis, filterEndMillis, currentMonthMillis, salaryCreditDay) {
        when (selectionMode) {
            CalendarSelectionMode.SINGLE_DAY -> 1
            CalendarSelectionMode.DATE_RANGE -> (((filterEndMillis - filterStartMillis) / 86400000L) + 1).toInt().coerceAtLeast(1)
            CalendarSelectionMode.WHOLE_MONTH -> {
                SalaryCycleHelper.getDaysInCycle(
                    SalaryCycleHelper.getCycleMonthKey(currentMonthMillis, salaryCreditDay),
                    salaryCreditDay
                )
            }
        }
    }

    val daysInMonthTotal = remember(currentMonthMillis, salaryCreditDay) {
        SalaryCycleHelper.getDaysInCycle(
            SalaryCycleHelper.getCycleMonthKey(currentMonthMillis, salaryCreditDay),
            salaryCreditDay
        )
    }

    val effectiveBudgetLimit = remember(selectionMode, daysInActivePeriod, daysInMonthTotal, monthlyBudget) {
        if (selectionMode == CalendarSelectionMode.WHOLE_MONTH) {
            monthlyBudget
        } else {
            val dailyRate = if (monthlyBudget > 0 && daysInMonthTotal > 0) (monthlyBudget / daysInMonthTotal.toDouble()) else 800.0
            dailyRate * daysInActivePeriod
        }
    }

    val dailyCap = effectiveBudgetLimit
    val consumedRatio = if (effectiveBudgetLimit > 0) (periodDebitTotal / effectiveBudgetLimit).coerceIn(0.0, 1.0) else 0.0
    val isUnderCap = periodDebitTotal <= effectiveBudgetLimit
    val capDifference = kotlin.math.abs(effectiveBudgetLimit - periodDebitTotal)

    // Dynamic Category Ratio for the active period
    val periodCategoryDistribution = remember(periodDebits) {
        periodDebits.groupBy { it.categoryId }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    // Cell click handler
    val handleCellClick = { cell: CalendarGridCell ->
        if (cell.monthOffset == -1) onPreviousMonth()
        if (cell.monthOffset == 1) onNextMonth()

        when (selectionMode) {
            CalendarSelectionMode.SINGLE_DAY -> {
                selectedSingleDayMillis = cell.timestampMillis
                onDateRangeSelected?.invoke(Pair(getStartOfDay(cell.timestampMillis), getEndOfDay(cell.timestampMillis)))
            }
            CalendarSelectionMode.DATE_RANGE -> {
                if (rangeStartMillis == null || rangeEndMillis != null) {
                    rangeStartMillis = cell.timestampMillis
                    rangeEndMillis = null
                } else {
                    rangeEndMillis = cell.timestampMillis
                    val s = minOf(rangeStartMillis!!, cell.timestampMillis)
                    val e = maxOf(rangeStartMillis!!, cell.timestampMillis)
                    onDateRangeSelected?.invoke(Pair(getStartOfDay(s), getEndOfDay(e)))
                }
            }
            CalendarSelectionMode.WHOLE_MONTH -> {
                // Tapping any day cell switches directly to Day mode for that specific day
                selectionMode = CalendarSelectionMode.SINGLE_DAY
                selectedSingleDayMillis = cell.timestampMillis
                onDateRangeSelected?.invoke(Pair(getStartOfDay(cell.timestampMillis), getEndOfDay(cell.timestampMillis)))
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PastelKineticTokens.CanvasBg),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. SpendWell Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(PastelKineticTokens.AnchorPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "SpendWell Calendar",
                            tint = PastelKineticTokens.AmberAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "SPENDWELL",
                            color = PastelKineticTokens.TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = "Financial Calendar",
                            color = PastelKineticTokens.TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(PastelKineticTokens.SurfaceContainer)
                            .clickable { onExportClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.IosShare,
                            contentDescription = "Export CSV",
                            tint = PastelKineticTokens.TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(PastelKineticTokens.AmberFixed),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = PastelKineticTokens.AmberText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 2. Mode Selector: Day vs Range vs Whole Month
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(PastelKineticTokens.RadiusFull)
                    .background(PastelKineticTokens.SurfaceContainer)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Day Mode Tab
                val isDayActive = selectionMode == CalendarSelectionMode.SINGLE_DAY
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(PastelKineticTokens.RadiusFull)
                        .background(if (isDayActive) PastelKineticTokens.SurfaceCard else Color.Transparent)
                        .clickable {
                            selectionMode = CalendarSelectionMode.SINGLE_DAY
                            if (selectedSingleDayMillis == 0L) {
                                selectedSingleDayMillis = System.currentTimeMillis()
                            }
                            onDateRangeSelected?.invoke(Pair(getStartOfDay(selectedSingleDayMillis), getEndOfDay(selectedSingleDayMillis)))
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Single Day",
                            tint = if (isDayActive) PastelKineticTokens.AnchorPrimary else PastelKineticTokens.TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Day",
                            color = if (isDayActive) PastelKineticTokens.TextPrimary else PastelKineticTokens.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Range Mode Tab
                val isRangeActive = selectionMode == CalendarSelectionMode.DATE_RANGE
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(PastelKineticTokens.RadiusFull)
                        .background(if (isRangeActive) PastelKineticTokens.SurfaceCard else Color.Transparent)
                        .clickable {
                            selectionMode = CalendarSelectionMode.DATE_RANGE
                            if (rangeStartMillis == null) {
                                // Default to last 7 days up to today
                                val now = System.currentTimeMillis()
                                val cal = Calendar.getInstance().apply {
                                    timeInMillis = now
                                    add(Calendar.DAY_OF_YEAR, -6)
                                }
                                rangeStartMillis = cal.timeInMillis
                                rangeEndMillis = now
                                onDateRangeSelected?.invoke(Pair(getStartOfDay(rangeStartMillis!!), getEndOfDay(rangeEndMillis!!)))
                            }
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Date Range",
                            tint = if (isRangeActive) PastelKineticTokens.AnchorPrimary else PastelKineticTokens.TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Range",
                            color = if (isRangeActive) PastelKineticTokens.TextPrimary else PastelKineticTokens.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Whole Month Tab
                val isMonthActive = selectionMode == CalendarSelectionMode.WHOLE_MONTH
                Box(
                    modifier = Modifier
                        .weight(1.3f)
                        .clip(PastelKineticTokens.RadiusFull)
                        .background(if (isMonthActive) PastelKineticTokens.SurfaceCard else Color.Transparent)
                        .clickable {
                            selectionMode = CalendarSelectionMode.WHOLE_MONTH
                            rangeStartMillis = null
                            rangeEndMillis = null
                            onClearDateSelection?.invoke()
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Whole Month",
                            tint = if (isMonthActive) PastelKineticTokens.AnchorPrimary else PastelKineticTokens.TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Whole Month",
                            color = if (isMonthActive) PastelKineticTokens.TextPrimary else PastelKineticTokens.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. Quick Range Presets (when in Range mode)
        if (selectionMode == CalendarSelectionMode.DATE_RANGE) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val applyPreset = { startT: Long, endT: Long ->
                        rangeStartMillis = startT
                        rangeEndMillis = endT
                        onDateRangeSelected?.invoke(Pair(getStartOfDay(startT), getEndOfDay(endT)))
                    }

                    // 1. Today
                    QuickPresetPill(
                        label = "Today",
                        onClick = {
                            val now = System.currentTimeMillis()
                            applyPreset(now, now)
                        }
                    )

                    // 2. Yesterday
                    QuickPresetPill(
                        label = "Yesterday",
                        onClick = {
                            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                            applyPreset(cal.timeInMillis, cal.timeInMillis)
                        }
                    )

                    // 3. Last 7 Days
                    QuickPresetPill(
                        label = "Last 7 Days",
                        onClick = {
                            val now = System.currentTimeMillis()
                            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }
                            applyPreset(cal.timeInMillis, now)
                        }
                    )

                    // 4. This Week
                    QuickPresetPill(
                        label = "This Week",
                        onClick = {
                            val now = System.currentTimeMillis()
                            val cal = Calendar.getInstance().apply {
                                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                            }
                            applyPreset(cal.timeInMillis, now)
                        }
                    )

                    // 5. Last 30 Days
                    QuickPresetPill(
                        label = "Last 30 Days",
                        onClick = {
                            val now = System.currentTimeMillis()
                            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -29) }
                            applyPreset(cal.timeInMillis, now)
                        }
                    )

                    // 6. Reset to Month
                    QuickPresetPill(
                        label = "Reset",
                        icon = Icons.Default.RestartAlt,
                        onClick = {
                            selectionMode = CalendarSelectionMode.WHOLE_MONTH
                            rangeStartMillis = null
                            rangeEndMillis = null
                            onClearDateSelection?.invoke()
                        }
                    )
                }
            }
        }

        // 4. Month Navigation Header with Dynamic Info
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (selectionMode) {
                            CalendarSelectionMode.SINGLE_DAY -> "SELECTED DATE"
                            CalendarSelectionMode.DATE_RANGE -> "SELECTED RANGE"
                            CalendarSelectionMode.WHOLE_MONTH -> "MONTHLY OVERVIEW"
                        },
                        color = PastelKineticTokens.TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = monthHeader,
                            color = PastelKineticTokens.TextPrimary,
                            fontSize = if (salaryCreditDay > 1) 16.sp else 19.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Box(
                            modifier = Modifier
                                .clip(PastelKineticTokens.RadiusFull)
                                .background(
                                    when (selectionMode) {
                                        CalendarSelectionMode.SINGLE_DAY -> PastelKineticTokens.AmberFixed
                                        CalendarSelectionMode.DATE_RANGE -> PastelKineticTokens.SkyFixed
                                        CalendarSelectionMode.WHOLE_MONTH -> PastelKineticTokens.SurfaceHigh
                                    }
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "$daysInActivePeriod ${if (daysInActivePeriod == 1) "Day" else "Days"}",
                                color = when (selectionMode) {
                                    CalendarSelectionMode.SINGLE_DAY -> PastelKineticTokens.AmberText
                                    CalendarSelectionMode.DATE_RANGE -> PastelKineticTokens.SkyText
                                    CalendarSelectionMode.WHOLE_MONTH -> PastelKineticTokens.TextSecondary
                                },
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PastelKineticTokens.SurfaceContainer)
                            .clickable { onPreviousMonth() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous Month",
                            tint = PastelKineticTokens.TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PastelKineticTokens.SurfaceContainer)
                            .clickable { onNextMonth() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next Month",
                            tint = PastelKineticTokens.TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 5. Full Interactive Monthly Calendar Grid Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, PastelKineticTokens.RadiusCard, spotColor = Color(0x10121316))
                    .clip(PastelKineticTokens.RadiusCard)
                    .background(PastelKineticTokens.SurfaceCard)
                    .padding(horizontal = 10.dp, vertical = 12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Weekday Column Headers (Mon to Sun)
                    val weekDayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        weekDayLabels.forEach { label ->
                            Text(
                                text = label,
                                color = PastelKineticTokens.TextSecondary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Calendar Grid Rows (35 or 42 cells)
                    val totalWeeks = calendarCells.size / 7
                    val rangeMin = if (selectionMode == CalendarSelectionMode.DATE_RANGE && rangeStartMillis != null) {
                        val start = rangeStartMillis!!
                        val end = rangeEndMillis ?: start
                        getStartOfDay(minOf(start, end))
                    } else null

                    val rangeMax = if (selectionMode == CalendarSelectionMode.DATE_RANGE && rangeStartMillis != null) {
                        val start = rangeStartMillis!!
                        val end = rangeEndMillis ?: start
                        getStartOfDay(maxOf(start, end))
                    } else null

                    for (weekIndex in 0 until totalWeeks) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (dayOfWeek in 0..6) {
                                val cell = calendarCells[weekIndex * 7 + dayOfWeek]
                                val cellStart = remember(cell.timestampMillis) { getStartOfDay(cell.timestampMillis) }
                                val isToday = cellStart == todayStartMillis

                                val activity = dayActivityMap[cellStart]
                                val hasCredit = activity?.first == true
                                val hasDebit = activity?.second == true

                                val isSelectedInSingle = selectionMode == CalendarSelectionMode.SINGLE_DAY &&
                                    isSameDay(cell.timestampMillis, selectedSingleDayMillis)

                                val isStart = rangeMin != null && cellStart == rangeMin
                                val isEnd = rangeMax != null && cellStart == rangeMax
                                val isBetween = rangeMin != null && rangeMax != null &&
                                    cellStart > rangeMin && cellStart < rangeMax
                                val isRangeSelected = isStart || isEnd || isBetween
                                val isSingleInSelection = isStart && isEnd

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable { handleCellClick(cell) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    // 1. Continuous Ribbon Connector Band for Date Range
                                    if (selectionMode == CalendarSelectionMode.DATE_RANGE && isRangeSelected && !isSingleInSelection) {
                                        when {
                                            isStart -> {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(28.dp)
                                                        .padding(start = 14.dp)
                                                        .background(
                                                            PastelKineticTokens.SkyFixed,
                                                            shape = if (dayOfWeek == 6) RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp) else RoundedCornerShape(0.dp)
                                                        )
                                                )
                                            }
                                            isEnd -> {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(28.dp)
                                                        .padding(end = 14.dp)
                                                        .background(
                                                            PastelKineticTokens.SkyFixed,
                                                            shape = if (dayOfWeek == 0) RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp) else RoundedCornerShape(0.dp)
                                                        )
                                                )
                                            }
                                            isBetween -> {
                                                val shape = when (dayOfWeek) {
                                                    0 -> RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                                                    6 -> RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
                                                    else -> RoundedCornerShape(0.dp)
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(28.dp)
                                                        .background(PastelKineticTokens.SkyFixed, shape = shape)
                                                )
                                            }
                                        }
                                    }

                                    // 2. Day Cell Focus Container
                                    val isFocusedPill = isSelectedInSingle || (selectionMode == CalendarSelectionMode.DATE_RANGE && (isStart || isEnd))
                                    val pillModifier = Modifier.size(32.dp)

                                    when {
                                        isFocusedPill -> {
                                            Box(
                                                modifier = pillModifier
                                                    .clip(PastelKineticTokens.RadiusSm)
                                                    .background(PastelKineticTokens.AnchorPrimary),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                DayCellContent(
                                                    dayNumber = cell.dayNumber,
                                                    textColor = PastelKineticTokens.OnAnchor,
                                                    isBold = true,
                                                    hasCredit = hasCredit,
                                                    hasDebit = hasDebit,
                                                    isDarkBackground = true
                                                )
                                            }
                                        }
                                        isToday && !isRangeSelected -> {
                                            Box(
                                                modifier = pillModifier
                                                    .clip(PastelKineticTokens.RadiusSm)
                                                    .background(PastelKineticTokens.AmberFixed.copy(alpha = 0.4f))
                                                    .border(1.dp, PastelKineticTokens.AmberAccent, PastelKineticTokens.RadiusSm),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                DayCellContent(
                                                    dayNumber = cell.dayNumber,
                                                    textColor = PastelKineticTokens.AmberText,
                                                    isBold = true,
                                                    hasCredit = hasCredit,
                                                    hasDebit = hasDebit,
                                                    isDarkBackground = false
                                                )
                                            }
                                        }
                                        else -> {
                                            val textColor = when {
                                                isBetween -> PastelKineticTokens.SkyDeep
                                                !cell.isCurrentMonth -> PastelKineticTokens.TextOutline.copy(alpha = 0.4f)
                                                else -> PastelKineticTokens.TextPrimary
                                            }
                                            DayCellContent(
                                                dayNumber = cell.dayNumber,
                                                textColor = textColor,
                                                isBold = isBetween,
                                                hasCredit = hasCredit,
                                                hasDebit = hasDebit,
                                                isDarkBackground = false
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Legend Strip
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(PastelKineticTokens.RadiusFull)
                    .background(PastelKineticTokens.SurfaceLow)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(PastelKineticTokens.SkyAccent))
                    Text("Inflow", color = PastelKineticTokens.TextSecondary, fontSize = 11.sp)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(PastelKineticTokens.AmberAccent))
                    Text("Outflow", color = PastelKineticTokens.TextSecondary, fontSize = 11.sp)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(PastelKineticTokens.RadiusXs)
                            .border(1.dp, PastelKineticTokens.AmberAccent, PastelKineticTokens.RadiusXs)
                    )
                    Text("Today", color = PastelKineticTokens.TextSecondary, fontSize = 11.sp)
                }
            }
        }

        // 7. Dynamic Summary Warm Amber Hero Pod
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, PastelKineticTokens.RadiusCard, spotColor = Color(0x18825500))
                    .clip(PastelKineticTokens.RadiusCard)
                    .background(PastelKineticTokens.AmberAccent)
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(PastelKineticTokens.RadiusFull)
                                .background(PastelKineticTokens.SurfaceCard.copy(alpha = 0.75f))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = selectedPeriodTitle,
                                color = PastelKineticTokens.AmberText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(PastelKineticTokens.RadiusFull)
                                .background(PastelKineticTokens.AnchorPrimary)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Safe",
                                    tint = if (isUnderCap) PastelKineticTokens.AmberFixed else PastelKineticTokens.Error,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = if (isUnderCap) "Safe Buffer +$currencySymbol${numberFormat.format(capDifference)}"
                                    else "Over Limit -$currencySymbol${numberFormat.format(capDifference)}",
                                    color = PastelKineticTokens.OnAnchor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Column {
                        Text(
                            text = "TOTAL NET OUTFLOW",
                            color = PastelKineticTokens.AmberTextVariant,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "-$currencySymbol${numberFormat.format(periodDebitTotal)}",
                                color = PastelKineticTokens.AmberText,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.6).sp
                            )
                            Text(
                                text = "across ${periodDebits.size} entries",
                                color = PastelKineticTokens.AmberTextVariant,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                        if (periodCreditTotal > 0.0) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "+$currencySymbol${numberFormat.format(periodCreditTotal)} inflow recorded",
                                color = PastelKineticTokens.AmberTextVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Dynamic Category Ratio Bar
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (periodCategoryDistribution.isEmpty() || periodDebitTotal <= 0.0) {
                            Text(
                                text = "No category spend recorded for this selection",
                                color = PastelKineticTokens.AmberTextVariant,
                                fontSize = 11.sp
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(PastelKineticTokens.RadiusFull)
                                    .background(PastelKineticTokens.SurfaceCard.copy(alpha = 0.5f))
                            )
                        } else {
                            val top3 = periodCategoryDistribution.take(3)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                top3.forEach { item ->
                                    val pct = if (periodDebitTotal > 0) ((item.second / periodDebitTotal) * 100).toInt() else 0
                                    Text(
                                        text = "${item.first.replaceFirstChar { it.titlecase() }} ($pct%)",
                                        color = PastelKineticTokens.AmberText,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(PastelKineticTokens.RadiusFull)
                                    .background(PastelKineticTokens.SurfaceCard.copy(alpha = 0.5f))
                                    .padding(1.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                val colors = listOf(PastelKineticTokens.AnchorPrimary, PastelKineticTokens.SkyDeep, PastelKineticTokens.SurfaceCard)
                                top3.forEachIndexed { i, item ->
                                    val weight = if (periodDebitTotal > 0.0) (item.second / periodDebitTotal).toFloat().coerceIn(0.05f, 1f) else 1f
                                    Box(
                                        modifier = Modifier
                                            .weight(weight)
                                            .height(6.dp)
                                            .clip(PastelKineticTokens.RadiusFull)
                                            .background(colors.getOrElse(i) { PastelKineticTokens.SurfaceCard })
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 8. Bento Quick Glance Pods (Daily Cap & Pacing)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sky Glaze Pod: Period Budget / Daily Cap
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(PastelKineticTokens.RadiusCard)
                        .background(PastelKineticTokens.SkyFixed)
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(PastelKineticTokens.RadiusFull)
                                    .background(PastelKineticTokens.SurfaceCard.copy(alpha = 0.7f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = when (selectionMode) {
                                        CalendarSelectionMode.SINGLE_DAY -> "Daily Cap"
                                        CalendarSelectionMode.DATE_RANGE -> "Range Cap"
                                        CalendarSelectionMode.WHOLE_MONTH -> "Monthly Budget"
                                    },
                                    color = PastelKineticTokens.SkyText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Icon(imageVector = Icons.Default.Speed, contentDescription = "Cap", tint = PastelKineticTokens.SkyText, modifier = Modifier.size(16.dp))
                        }
                        Column {
                            Text("$currencySymbol${numberFormat.format(dailyCap)}", color = PastelKineticTokens.SkyText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("${(consumedRatio * 100).toInt()}% consumed", color = PastelKineticTokens.SkyTextVariant, fontSize = 11.sp)
                        }
                        Box(
                            modifier = Modifier.fillMaxWidth().height(5.dp).clip(PastelKineticTokens.RadiusFull).background(PastelKineticTokens.SurfaceCard.copy(alpha = 0.5f))
                        ) {
                            Box(modifier = Modifier.fillMaxWidth(consumedRatio.toFloat().coerceIn(0.04f, 1f)).height(5.dp).clip(PastelKineticTokens.RadiusFull).background(PastelKineticTokens.SkyText))
                        }
                    }
                }

                // Lilac Glaze Pod: Pacing
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(PastelKineticTokens.RadiusCard)
                        .background(PastelKineticTokens.SurfaceHigh)
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(PastelKineticTokens.RadiusFull)
                                    .background(PastelKineticTokens.SurfaceCard)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Pacing", color = PastelKineticTokens.TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Icon(
                                imageVector = if (isUnderCap) Icons.Default.SentimentSatisfied else Icons.Default.SentimentDissatisfied,
                                contentDescription = "Pacing",
                                tint = if (isUnderCap) PastelKineticTokens.AmberDeep else PastelKineticTokens.Error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column {
                            Text(if (isUnderCap) "Under Cap" else "Over Cap", color = PastelKineticTokens.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(
                                if (isUnderCap) "+$currencySymbol${numberFormat.format(capDifference)} buffer"
                                else "-$currencySymbol${numberFormat.format(capDifference)} over limit",
                                color = PastelKineticTokens.TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (isUnderCap) PastelKineticTokens.AmberDeep else PastelKineticTokens.Error))
                            Text(
                                text = if (isUnderCap) "Target met" else "Cap exceeded",
                                color = PastelKineticTokens.TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // 9. Time Logs Section Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Time Logs", color = PastelKineticTokens.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("${periodTransactions.size} events", color = PastelKineticTokens.TextSecondary, fontSize = 11.sp)
            }
        }

        // 10. Time Log Transaction Items
        if (periodTransactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(PastelKineticTokens.RadiusMd)
                        .background(PastelKineticTokens.SurfaceCard)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No transactions logged for this selection.", color = PastelKineticTokens.TextOutline, fontSize = 13.sp)
                }
            }
        } else {
            items(periodTransactions, key = { it.id }) { tx ->
                val isCredit = tx.type == TransactionType.CREDIT
                val formattedAmt = "${if (isCredit) "+" else "-"}$currencySymbol${numberFormat.format(tx.amount)}"
                val timeString = remember(tx.timestamp, selectionMode) {
                    if (selectionMode == CalendarSelectionMode.SINGLE_DAY) {
                        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(tx.timestamp))
                    } else {
                        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(tx.timestamp))
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, PastelKineticTokens.RadiusMd, spotColor = Color(0x0C121316))
                        .clip(PastelKineticTokens.RadiusMd)
                        .background(PastelKineticTokens.SurfaceCard)
                        .clickable { onTransactionClick(tx) }
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isCredit) PastelKineticTokens.AmberFixed.copy(alpha = 0.6f) else PastelKineticTokens.SkyFixed.copy(alpha = 0.5f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isCredit) Icons.Default.ArrowDownward else getCategoryIconVector(tx.categoryId),
                                    contentDescription = tx.categoryId,
                                    tint = if (isCredit) PastelKineticTokens.AmberDeep else PastelKineticTokens.SkyText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tx.merchantClean.ifBlank { tx.merchantRaw },
                                    color = PastelKineticTokens.TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(PastelKineticTokens.RadiusFull)
                                            .background(PastelKineticTokens.SurfaceLow)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = tx.sources.firstOrNull()?.name ?: "MANUAL",
                                            color = PastelKineticTokens.TextSecondary,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = timeString,
                                        color = PastelKineticTokens.TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = formattedAmt,
                                color = if (isCredit) PastelKineticTokens.AmberDeep else PastelKineticTokens.TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tx.categoryId.replaceFirstChar { it.titlecase() },
                                color = PastelKineticTokens.TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickPresetPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(PastelKineticTokens.RadiusFull)
            .background(PastelKineticTokens.SurfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = PastelKineticTokens.TextSecondary,
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = label,
                color = PastelKineticTokens.TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun DayCellContent(
    dayNumber: Int,
    textColor: Color,
    isBold: Boolean,
    hasCredit: Boolean,
    hasDebit: Boolean,
    isDarkBackground: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = dayNumber.toString(),
            color = textColor,
            fontSize = 11.5.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium
        )

        // Activity indicator dots (Sky for credit, Amber for debit)
        if (hasCredit || hasDebit) {
            Spacer(modifier = Modifier.height(1.5.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasCredit) {
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(
                                if (isDarkBackground) PastelKineticTokens.SkyFixed else PastelKineticTokens.SkyAccent
                            )
                    )
                }
                if (hasDebit) {
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(
                                if (isDarkBackground) PastelKineticTokens.AmberAccent else PastelKineticTokens.AmberDeep
                            )
                    )
                }
            }
        }
    }
}

private fun getCalendarCellsForMonth(monthMillis: Long): List<CalendarGridCell> {
    val cal = Calendar.getInstance().apply {
        timeInMillis = monthMillis
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val daysInCurrentMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Monday = 0 .. Sunday = 6

    val prevCal = (cal.clone() as Calendar).apply {
        add(Calendar.MONTH, -1)
    }
    val daysInPrevMonth = prevCal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val cells = mutableListOf<CalendarGridCell>()

    // 1. Trailing days of previous month
    for (i in firstDayOfWeek - 1 downTo 0) {
        val dayNum = daysInPrevMonth - i
        val dayCal = (prevCal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, dayNum)
        }
        cells.add(CalendarGridCell(dayNum, -1, dayCal.timeInMillis, false))
    }

    // 2. Days of current month
    for (dayNum in 1..daysInCurrentMonth) {
        val dayCal = (cal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, dayNum)
        }
        cells.add(CalendarGridCell(dayNum, 0, dayCal.timeInMillis, true))
    }

    // 3. Leading days of next month to complete the grid (35 or 42 cells)
    val nextCal = (cal.clone() as Calendar).apply {
        add(Calendar.MONTH, 1)
    }
    var nextMonthDay = 1
    while (cells.size % 7 != 0 || cells.size < 35) {
        val dayCal = (nextCal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, nextMonthDay)
        }
        cells.add(CalendarGridCell(nextMonthDay, 1, dayCal.timeInMillis, false))
        nextMonthDay++
    }

    return cells
}

private fun getStartOfDay(timestampMillis: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = timestampMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

private fun getEndOfDay(timestampMillis: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = timestampMillis
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }
    return cal.timeInMillis
}

private fun isSameDay(timeA: Long, timeB: Long): Boolean {
    val calA = Calendar.getInstance().apply { timeInMillis = timeA }
    val calB = Calendar.getInstance().apply { timeInMillis = timeB }
    return calA.get(Calendar.YEAR) == calB.get(Calendar.YEAR) &&
        calA.get(Calendar.DAY_OF_YEAR) == calB.get(Calendar.DAY_OF_YEAR)
}
