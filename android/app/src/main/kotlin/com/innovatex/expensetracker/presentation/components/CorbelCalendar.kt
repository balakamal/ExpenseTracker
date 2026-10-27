package com.innovatex.expensetracker.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovatex.expensetracker.domain.model.Transaction
import com.innovatex.expensetracker.domain.model.TransactionType
import com.innovatex.expensetracker.presentation.dashboard.DateRangePreset
import com.innovatex.expensetracker.presentation.theme.ApexThemeTokens
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CalendarCell(
    val dayNumber: Int,
    val monthOffset: Int, // -1 = prev, 0 = current, 1 = next
    val timestampMillis: Long,
    val isCurrentMonth: Boolean
)

@Composable
fun CorbelCalendar(
    currentMonthMillis: Long,
    selectedDateRange: Pair<Long, Long>?,
    allTransactions: List<Transaction>,
    onDateTapped: (Long) -> Unit,
    onPresetSelected: (DateRangePreset) -> Unit,
    onClearDateSelection: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val monthTitle = remember(currentMonthMillis) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(currentMonthMillis))
    }

    // Complete 35 or 42 grid of calendar days (including trailing & leading days)
    val calendarCells = remember(currentMonthMillis) {
        getCalendarCellsForMonth(currentMonthMillis)
    }

    // Map each day with transaction activity (Credits and Debits)
    val dayActivityMap = remember(allTransactions, currentMonthMillis) {
        val map = mutableMapOf<Long, Pair<Boolean, Boolean>>() // DayStartMillis -> (hasCredit, hasDebit)
        val cal = Calendar.getInstance()

        for (txn in allTransactions) {
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

    // Range boundaries normalized to startOfDay and endOfDay
    val rangeNormalized = remember(selectedDateRange) {
        if (selectedDateRange == null) null
        else {
            val startDay = getStartOfDay(selectedDateRange.first)
            val endDay = getEndOfDay(selectedDateRange.second)
            Pair(startDay, endDay)
        }
    }

    val isSingleDaySelection = remember(selectedDateRange) {
        selectedDateRange != null && isSameDay(selectedDateRange.first, selectedDateRange.second)
    }

    val todayMillis = remember {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(ApexThemeTokens.RadiusCard)
            .background(ApexThemeTokens.BgSurfaceCard)
            .border(1.dp, ApexThemeTokens.StrokeSubtle, ApexThemeTokens.RadiusCard)
            .padding(16.dp)
    ) {
        Column {
            // 1. Top Metadata Row: Micro-label and Reset Filter Status Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "FINANCIAL CALENDAR",
                    color = ApexThemeTokens.TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )

                Spacer(modifier = Modifier.weight(1f))

                // Status Pill: Shows active range or "Overall"
                val isOverall = selectedDateRange == null
                Box(
                    modifier = Modifier
                        .clip(ApexThemeTokens.RadiusFull)
                        .background(if (isOverall) ApexThemeTokens.AccentCobaltTint else ApexThemeTokens.BgGlassPillHover)
                        .border(
                            1.dp,
                            if (isOverall) ApexThemeTokens.AccentCobaltBorder else ApexThemeTokens.StrokeHighlight,
                            ApexThemeTokens.RadiusFull
                        )
                        .clickable(onClick = onClearDateSelection)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isOverall) ApexThemeTokens.AccentCobalt else ApexThemeTokens.AccentPrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isOverall) "All Ledger" else "Reset Filter",
                            color = if (isOverall) ApexThemeTokens.AccentCobalt else ApexThemeTokens.TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (!isOverall) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = ApexThemeTokens.TextSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Month Navigation Row: Month Title and Connected Month Switcher Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = monthTitle,
                    color = ApexThemeTokens.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp
                )

                Spacer(modifier = Modifier.weight(1f))

                // Connected Corbel Glass Month Switcher Pill (Collision-Free)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(ApexThemeTokens.RadiusFull)
                        .background(ApexThemeTokens.BgGlassPill)
                        .border(1.dp, ApexThemeTokens.StrokeSubtle, ApexThemeTokens.RadiusFull)
                        .padding(horizontal = 4.dp, vertical = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onPreviousMonth),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ChevronLeft,
                            contentDescription = "Previous Month",
                            tint = ApexThemeTokens.TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(14.dp)
                            .background(ApexThemeTokens.StrokeSubtle)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onNextMonth),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Next Month",
                            tint = ApexThemeTokens.TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Range Preset Pills: All, Today, 7D, This Month, 30D
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DateRangePreset.values().forEach { preset ->
                    val isPresetActive = when (preset) {
                        DateRangePreset.ALL -> selectedDateRange == null
                        DateRangePreset.TODAY -> isSingleDaySelection && isSameDay(selectedDateRange!!.first, System.currentTimeMillis())
                        else -> false
                    }

                    Box(
                        modifier = Modifier
                            .clip(ApexThemeTokens.RadiusFull)
                            .background(if (isPresetActive) ApexThemeTokens.BgHighContrast else ApexThemeTokens.BgGlassPill)
                            .border(
                                1.dp,
                                if (isPresetActive) Color.Transparent else ApexThemeTokens.StrokeSubtle,
                                ApexThemeTokens.RadiusFull
                            )
                            .clickable { onPresetSelected(preset) }
                            .padding(horizontal = 11.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = preset.label,
                            color = if (isPresetActive) ApexThemeTokens.TextHighContrast else ApexThemeTokens.TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isPresetActive) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Weekday Headers (M, T, W, T, F, S, S)
            val weekDays = listOf("M", "T", "W", "T", "F", "S", "S")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekDays.forEach { dayName ->
                    Text(
                        text = dayName,
                        color = ApexThemeTokens.TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Day Grid Rows with Connected Range Highlights
            val totalWeeks = calendarCells.size / 7
            for (weekIndex in 0 until totalWeeks) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (dayOfWeek in 0..6) {
                        val cellIndex = weekIndex * 7 + dayOfWeek
                        val cell = calendarCells[cellIndex]

                        val cellStartMillis = remember(cell.timestampMillis) { getStartOfDay(cell.timestampMillis) }
                        val isToday = cellStartMillis == todayMillis

                        val isStart = selectedDateRange != null && isSameDay(cell.timestampMillis, selectedDateRange.first)
                        val isEnd = selectedDateRange != null && isSameDay(cell.timestampMillis, selectedDateRange.second)
                        val isBetween = rangeNormalized != null &&
                            cellStartMillis > rangeNormalized.first &&
                            cellStartMillis < rangeNormalized.second &&
                            !isStart && !isEnd

                        val isSelected = isStart || isEnd || isBetween
                        val isSingle = isStart && isEnd

                        val activity = dayActivityMap[cellStartMillis]
                        val hasCredit = activity?.first == true
                        val hasDebit = activity?.second == true

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable {
                                    if (cell.monthOffset == -1) onPreviousMonth()
                                    if (cell.monthOffset == 1) onNextMonth()
                                    onDateTapped(cell.timestampMillis)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            // 1. Background Connector Band for Multi-Day Ranges (No overlapping borders!)
                            if (!isSingle && isSelected) {
                                when {
                                    isStart -> {
                                        // Connector extending from center to right edge
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(30.dp)
                                                .padding(start = 15.dp)
                                                .background(
                                                    ApexThemeTokens.RangeConnectorBg,
                                                    shape = if (dayOfWeek == 6) RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp) else RoundedCornerShape(0.dp)
                                                )
                                        )
                                    }
                                    isEnd -> {
                                        // Connector extending from left edge to center
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(30.dp)
                                                .padding(end = 15.dp)
                                                .background(
                                                    ApexThemeTokens.RangeConnectorBg,
                                                    shape = if (dayOfWeek == 0) RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp) else RoundedCornerShape(0.dp)
                                                )
                                        )
                                    }
                                    isBetween -> {
                                        // Continuous full-width connector band
                                        val shape = when (dayOfWeek) {
                                            0 -> RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                                            6 -> RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
                                            else -> RoundedCornerShape(0.dp)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(30.dp)
                                                .background(ApexThemeTokens.RangeConnectorBg, shape = shape)
                                        )
                                    }
                                }
                            }

                            // 2. Focused Date Squircle Pill for Start, End, or Today
                            val pillModifier = Modifier.size(32.dp)
                            when {
                                isStart || isEnd -> {
                                    Box(
                                        modifier = pillModifier
                                            .clip(ApexThemeTokens.RadiusSm)
                                            .background(ApexThemeTokens.BgHighContrast),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        DayContent(
                                            dayNumber = cell.dayNumber,
                                            textColor = ApexThemeTokens.TextHighContrast,
                                            isBold = true,
                                            hasCredit = hasCredit,
                                            hasDebit = hasDebit,
                                            isPillSelected = true
                                        )
                                    }
                                }
                                isToday && !isSelected -> {
                                    Box(
                                        modifier = pillModifier
                                            .clip(ApexThemeTokens.RadiusSm)
                                            .background(ApexThemeTokens.BgGlassPillHover)
                                            .border(1.dp, ApexThemeTokens.AccentCobaltBorder, ApexThemeTokens.RadiusSm),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        DayContent(
                                            dayNumber = cell.dayNumber,
                                            textColor = ApexThemeTokens.AccentCobalt,
                                            isBold = true,
                                            hasCredit = hasCredit,
                                            hasDebit = hasDebit,
                                            isPillSelected = false
                                        )
                                    }
                                }
                                else -> {
                                    // Regular unselected day
                                    val textColor = when {
                                        isBetween -> ApexThemeTokens.AccentCobalt
                                        !cell.isCurrentMonth -> ApexThemeTokens.TextMuted.copy(alpha = 0.35f)
                                        else -> ApexThemeTokens.TextPrimary
                                    }
                                    DayContent(
                                        dayNumber = cell.dayNumber,
                                        textColor = textColor,
                                        isBold = isBetween,
                                        hasCredit = hasCredit,
                                        hasDebit = hasDebit,
                                        isPillSelected = false
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

@Composable
private fun DayContent(
    dayNumber: Int,
    textColor: Color,
    isBold: Boolean,
    hasCredit: Boolean,
    hasDebit: Boolean,
    isPillSelected: Boolean
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

        // Activity indicator dots (Mint for credit, Coral for debit)
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
                            .background(if (isPillSelected) ApexThemeTokens.TextHighContrast else ApexThemeTokens.AccentPrimary)
                    )
                }
                if (hasDebit) {
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(ApexThemeTokens.AccentDanger)
                    )
                }
            }
        }
    }
}

private fun getCalendarCellsForMonth(monthMillis: Long): List<CalendarCell> {
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

    val cells = mutableListOf<CalendarCell>()

    // 1. Trailing days of previous month
    for (i in firstDayOfWeek - 1 downTo 0) {
        val dayNum = daysInPrevMonth - i
        val dayCal = (prevCal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, dayNum)
        }
        cells.add(CalendarCell(dayNum, -1, dayCal.timeInMillis, false))
    }

    // 2. Days of current month
    for (dayNum in 1..daysInCurrentMonth) {
        val dayCal = (cal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, dayNum)
        }
        cells.add(CalendarCell(dayNum, 0, dayCal.timeInMillis, true))
    }

    // 3. Leading days of next month to complete the grid to multiple of 7
    val nextCal = (cal.clone() as Calendar).apply {
        add(Calendar.MONTH, 1)
    }
    var nextMonthDay = 1
    while (cells.size % 7 != 0 || cells.size < 35) {
        val dayCal = (nextCal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, nextMonthDay)
        }
        cells.add(CalendarCell(nextMonthDay, 1, dayCal.timeInMillis, false))
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
