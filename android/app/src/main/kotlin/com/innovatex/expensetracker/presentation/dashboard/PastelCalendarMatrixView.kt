package com.innovatex.expensetracker.presentation.dashboard

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovatex.expensetracker.domain.model.Transaction
import com.innovatex.expensetracker.domain.model.TransactionType
import com.innovatex.expensetracker.presentation.components.getCategoryIconVector
import com.innovatex.expensetracker.presentation.theme.PastelKineticTokens
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun PastelCalendarMatrixView(
    currentMonthMillis: Long,
    allTransactions: List<Transaction>,
    currencySymbol: String = "₹",
    monthlyBudget: Double = 25000.0,
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

    var isRangeMode by remember { mutableStateOf(false) }
    var selectedDayOffset by remember { mutableIntStateOf(3) }
    var rangeStartOffset by remember { mutableIntStateOf(1) }
    var rangeEndOffset by remember { mutableIntStateOf(5) }

    val monthHeader = remember(currentMonthMillis) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(currentMonthMillis))
    }

    val weekNumber = remember(currentMonthMillis) {
        val cal = Calendar.getInstance().apply { timeInMillis = currentMonthMillis }
        cal.get(Calendar.WEEK_OF_YEAR)
    }

    val weekDays = remember(currentMonthMillis) {
        val list = mutableListOf<Triple<String, Int, Long>>()
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val tempCal = Calendar.getInstance().apply {
            timeInMillis = currentMonthMillis
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        }
        for (i in 0 until 7) {
            list.add(Triple(dayFormat.format(tempCal.time), tempCal.get(Calendar.DAY_OF_MONTH), tempCal.timeInMillis))
            tempCal.add(Calendar.DAY_OF_MONTH, 1)
        }
        list
    }

    // Determine target timestamp bounds
    val selectedDayMillis = weekDays.getOrNull(selectedDayOffset)?.third ?: currentMonthMillis
    val (filterStartMillis, filterEndMillis) = remember(isRangeMode, selectedDayOffset, rangeStartOffset, rangeEndOffset, weekDays) {
        if (!isRangeMode) {
            val start = Calendar.getInstance().apply {
                timeInMillis = selectedDayMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            Pair(start, start + 86400000L)
        } else {
            val minOffset = minOf(rangeStartOffset, rangeEndOffset)
            val maxOffset = maxOf(rangeStartOffset, rangeEndOffset)
            val startDay = weekDays.getOrNull(minOffset)?.third ?: currentMonthMillis
            val endDay = weekDays.getOrNull(maxOffset)?.third ?: currentMonthMillis
            val start = Calendar.getInstance().apply {
                timeInMillis = startDay
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val end = Calendar.getInstance().apply {
                timeInMillis = endDay
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis
            Pair(start, end)
        }
    }

    val selectedDayTitle = remember(isRangeMode, filterStartMillis, filterEndMillis) {
        if (!isRangeMode) {
            SimpleDateFormat("EEEE, dd MMM", Locale.getDefault()).format(Date(filterStartMillis))
        } else {
            val df = SimpleDateFormat("dd MMM", Locale.getDefault())
            "${df.format(Date(filterStartMillis))} - ${df.format(Date(filterEndMillis))}"
        }
    }

    // Filter day's transactions
    val dayTransactions = remember(allTransactions, filterStartMillis, filterEndMillis) {
        allTransactions.filter { it.timestamp in filterStartMillis..filterEndMillis }
    }

    val dayDebits = dayTransactions.filter { it.type == TransactionType.DEBIT }
    val dayDebitTotal = dayDebits.sumOf { it.amount }

    // Dynamic Daily Cap & Pacing
    val dailyCap = if (monthlyBudget > 0) (monthlyBudget / 30.0) else 800.0
    val consumedRatio = (dayDebitTotal / dailyCap).coerceIn(0.0, 1.0)
    val isUnderCap = dayDebitTotal <= dailyCap
    val capDifference = kotlin.math.abs(dailyCap - dayDebitTotal)

    // Dynamic Category Ratio for the day
    val dayCategoryDistribution = remember(dayDebits) {
        dayDebits.groupBy { it.categoryId }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
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
                            contentDescription = "SpendWell",
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
                            text = "Calendar Insights",
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

        // 2. Mode Selector: Single Day vs Date Range
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(PastelKineticTokens.RadiusFull)
                    .background(PastelKineticTokens.SurfaceContainer)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(PastelKineticTokens.RadiusFull)
                        .background(if (!isRangeMode) PastelKineticTokens.SurfaceCard else Color.Transparent)
                        .clickable { isRangeMode = false }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Single Day",
                            tint = if (!isRangeMode) PastelKineticTokens.AnchorPrimary else PastelKineticTokens.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Single Day",
                            color = if (!isRangeMode) PastelKineticTokens.TextPrimary else PastelKineticTokens.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(PastelKineticTokens.RadiusFull)
                        .background(if (isRangeMode) PastelKineticTokens.SurfaceCard else Color.Transparent)
                        .clickable { isRangeMode = true }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Date Range",
                            tint = if (isRangeMode) PastelKineticTokens.AnchorPrimary else PastelKineticTokens.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Date Range",
                            color = if (isRangeMode) PastelKineticTokens.TextPrimary else PastelKineticTokens.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. Month Navigation Header with Dynamic Week Number
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "EXPENSE MATRIX",
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
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .clip(PastelKineticTokens.RadiusFull)
                                .background(PastelKineticTokens.AmberFixed)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Week $weekNumber",
                                color = PastelKineticTokens.AmberText,
                                fontSize = 11.sp,
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

        // 4. Weekly Horizontal Date Strip with Real Status Dots
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekDays.forEachIndexed { index, triple ->
                    val isSelectedInSingle = !isRangeMode && selectedDayOffset == index
                    val isSelectedInRange = isRangeMode && (
                        (index >= minOf(rangeStartOffset, rangeEndOffset)) &&
                        (index <= maxOf(rangeStartOffset, rangeEndOffset))
                    )
                    val isActive = isSelectedInSingle || isSelectedInRange

                    // Check transactions for this specific day
                    val dayStart = Calendar.getInstance().apply {
                        timeInMillis = triple.third
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    val dayEnd = dayStart + 86400000L
                    val dayHasCredit = allTransactions.any { it.timestamp in dayStart until dayEnd && it.type == TransactionType.CREDIT }
                    val dayHasDebit = allTransactions.any { it.timestamp in dayStart until dayEnd && it.type == TransactionType.DEBIT }

                    val dotColor = when {
                        dayHasDebit -> PastelKineticTokens.AmberAccent
                        dayHasCredit -> PastelKineticTokens.SkyAccent
                        else -> PastelKineticTokens.SurfaceDim
                    }

                    Column(
                        modifier = Modifier
                            .width(46.dp)
                            .height(76.dp)
                            .shadow(
                                elevation = if (isActive) 6.dp else 0.dp,
                                shape = PastelKineticTokens.RadiusLg,
                                spotColor = Color(0x20121316)
                            )
                            .clip(PastelKineticTokens.RadiusLg)
                            .background(
                                if (isActive) PastelKineticTokens.AnchorPrimary else PastelKineticTokens.SurfaceLow
                            )
                            .clickable {
                                if (!isRangeMode) {
                                    selectedDayOffset = index
                                } else {
                                    if (rangeStartOffset == rangeEndOffset) {
                                        rangeEndOffset = index
                                    } else {
                                        rangeStartOffset = index
                                        rangeEndOffset = index
                                    }
                                }
                            }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                        Text(
                            text = triple.first,
                            color = if (isActive) PastelKineticTokens.SurfaceHighest else PastelKineticTokens.TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${triple.second}",
                            color = if (isActive) PastelKineticTokens.OnAnchor else PastelKineticTokens.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 5. Legend Strip
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(PastelKineticTokens.RadiusFull)
                    .background(PastelKineticTokens.SurfaceLow)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
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
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(PastelKineticTokens.SurfaceDim))
                    Text("Zero Spend", color = PastelKineticTokens.TextSecondary, fontSize = 11.sp)
                }
            }
        }

        // 6. Daily Summary Warm Amber Hero Pod
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
                                .background(PastelKineticTokens.SurfaceCard.copy(alpha = 0.65f))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = selectedDayTitle,
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
                                text = "-$currencySymbol${numberFormat.format(dayDebitTotal)}",
                                color = PastelKineticTokens.AmberText,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.6).sp
                            )
                            Text(
                                text = "across ${dayDebits.size} entries",
                                color = PastelKineticTokens.AmberTextVariant,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                    }

                    // Dynamic Category Ratio Bar
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (dayCategoryDistribution.isEmpty()) {
                            Text(
                                text = "No category spend recorded for this period",
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
                            val top3 = dayCategoryDistribution.take(3)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                top3.forEach { item ->
                                    val pct = if (dayDebitTotal > 0) ((item.second / dayDebitTotal) * 100).toInt() else 0
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
                                    val weight = (item.second / dayDebitTotal).toFloat().coerceIn(0.05f, 1f)
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

        // 7. Bento Quick Glance Pods (Daily Cap & Pacing)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sky Glaze Pod: Daily Cap
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
                                Text("Daily Cap", color = PastelKineticTokens.SkyText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                            Box(modifier = Modifier.fillMaxWidth(consumedRatio.toFloat().coerceAtLeast(0.04f)).height(5.dp).clip(PastelKineticTokens.RadiusFull).background(PastelKineticTokens.SkyText))
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
                                if (isUnderCap) "+$currencySymbol${numberFormat.format(capDifference)} under curve"
                                else "-$currencySymbol${numberFormat.format(capDifference)} over curve",
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

        // 8. Time Logs Section Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Time Logs", color = PastelKineticTokens.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("${dayTransactions.size} events", color = PastelKineticTokens.TextSecondary, fontSize = 11.sp)
            }
        }

        // 9. Time Log Transaction Items
        if (dayTransactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(PastelKineticTokens.RadiusMd)
                        .background(PastelKineticTokens.SurfaceCard)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No transactions logged for this date.", color = PastelKineticTokens.TextOutline, fontSize = 13.sp)
                }
            }
        } else {
            items(dayTransactions, key = { it.id }) { tx ->
                val isCredit = tx.type == TransactionType.CREDIT
                val formattedAmt = "${if (isCredit) "+" else "-"}$currencySymbol${numberFormat.format(tx.amount)}"
                val timeString = remember(tx.timestamp) {
                    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(tx.timestamp))
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
