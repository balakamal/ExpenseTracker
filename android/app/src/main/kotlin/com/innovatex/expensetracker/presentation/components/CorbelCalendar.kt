package com.innovatex.expensetracker.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovatex.expensetracker.domain.model.Transaction
import com.innovatex.expensetracker.domain.model.TransactionType
import com.innovatex.expensetracker.presentation.theme.ApexThemeTokens
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CorbelCalendar(
    currentMonthMillis: Long,
    selectedDateMillis: Long?,
    allTransactions: List<Transaction>,
    onDateSelected: (Long) -> Unit,
    onClearDateSelection: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val monthTitle = remember(currentMonthMillis) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(currentMonthMillis))
    }

    // Days grid preparation
    val calendarDays = remember(currentMonthMillis) {
        getCalendarDaysForMonth(currentMonthMillis)
    }

    // Map days with transaction activity (Credits and Debits)
    val dayActivityMap = remember(allTransactions, currentMonthMillis) {
        val map = mutableMapOf<Int, Pair<Boolean, Boolean>>() // Day -> (hasCredit, hasDebit)
        val cal = Calendar.getInstance()
        val curCal = Calendar.getInstance().apply { timeInMillis = currentMonthMillis }
        val curYear = curCal.get(Calendar.YEAR)
        val curMonth = curCal.get(Calendar.MONTH)

        for (txn in allTransactions) {
            cal.timeInMillis = txn.timestamp
            if (cal.get(Calendar.YEAR) == curYear && cal.get(Calendar.MONTH) == curMonth) {
                val day = cal.get(Calendar.DAY_OF_MONTH)
                val existing = map.getOrDefault(day, Pair(false, false))
                val isCredit = txn.type == TransactionType.CREDIT
                map[day] = Pair(existing.first || isCredit, existing.second || !isCredit)
            }
        }
        map
    }

    // Check currently selected day in this month
    val selectedDayInMonth = remember(selectedDateMillis, currentMonthMillis) {
        if (selectedDateMillis == null) null
        else {
            val selCal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
            val curCal = Calendar.getInstance().apply { timeInMillis = currentMonthMillis }
            if (selCal.get(Calendar.YEAR) == curCal.get(Calendar.YEAR) &&
                selCal.get(Calendar.MONTH) == curCal.get(Calendar.MONTH)
            ) {
                selCal.get(Calendar.DAY_OF_MONTH)
            } else null
        }
    }

    val todayDayInMonth = remember(currentMonthMillis) {
        val today = Calendar.getInstance()
        val curCal = Calendar.getInstance().apply { timeInMillis = currentMonthMillis }
        if (today.get(Calendar.YEAR) == curCal.get(Calendar.YEAR) &&
            today.get(Calendar.MONTH) == curCal.get(Calendar.MONTH)
        ) {
            today.get(Calendar.DAY_OF_MONTH)
        } else null
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(ApexThemeTokens.RadiusCard)
            .background(ApexThemeTokens.BgSurface)
            .border(1.dp, ApexThemeTokens.StrokeSubtle, ApexThemeTokens.RadiusCard)
            .padding(16.dp)
    ) {
        Column {
            // Header Row: Month title, Controls, and Overall toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "FINANCIAL CALENDAR",
                        color = ApexThemeTokens.TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = monthTitle,
                        color = ApexThemeTokens.TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Overall Months vs Date Selected Status Pill
                val isOverall = selectedDateMillis == null
                Box(
                    modifier = Modifier
                        .clip(ApexThemeTokens.RadiusFull)
                        .background(if (isOverall) ApexThemeTokens.AccentCobaltTint else ApexThemeTokens.BgGlassPill)
                        .border(
                            1.dp,
                            if (isOverall) ApexThemeTokens.AccentCobaltBorder else ApexThemeTokens.StrokeSubtle,
                            ApexThemeTokens.RadiusFull
                        )
                        .clickable(onClick = onClearDateSelection)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isOverall) ApexThemeTokens.AccentCobalt else ApexThemeTokens.TextSecondary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isOverall) "All Months" else "Clear Filter",
                            color = if (isOverall) ApexThemeTokens.AccentCobalt else ApexThemeTokens.TextSecondary,
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

                Spacer(modifier = Modifier.width(6.dp))

                // Month Switchers
                IconButton(
                    onClick = onPreviousMonth,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(ApexThemeTokens.BgGlassPill)
                ) {
                    Icon(
                        Icons.Default.ChevronLeft,
                        contentDescription = "Previous Month",
                        tint = ApexThemeTokens.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onNextMonth,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(ApexThemeTokens.BgGlassPill)
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Next Month",
                        tint = ApexThemeTokens.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
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

            Spacer(modifier = Modifier.height(8.dp))

            // Day Grid Rows
            val totalWeeks = (calendarDays.size + 6) / 7
            for (weekIndex in 0 until totalWeeks) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (dayOfWeek in 0..6) {
                        val dayIndex = weekIndex * 7 + dayOfWeek
                        val dayNumber = calendarDays.getOrNull(dayIndex)

                        if (dayNumber != null) {
                            val isSelected = selectedDayInMonth == dayNumber
                            val isToday = todayDayInMonth == dayNumber
                            val activity = dayActivityMap[dayNumber]
                            val hasCredit = activity?.first == true
                            val hasDebit = activity?.second == true

                            val cellBg by animateColorAsState(
                                targetValue = when {
                                    isSelected -> ApexThemeTokens.BgHighContrast
                                    isToday -> ApexThemeTokens.BgGlassPillHover
                                    else -> Color.Transparent
                                },
                                label = "cellBg"
                            )

                            val cellText = when {
                                isSelected -> ApexThemeTokens.TextHighContrast
                                isToday -> ApexThemeTokens.AccentCobalt
                                else -> ApexThemeTokens.TextPrimary
                            }

                            val cellScale by animateFloatAsState(
                                targetValue = if (isSelected) 1.05f else 1.0f,
                                label = "cellScale"
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .scale(cellScale)
                                    .clip(ApexThemeTokens.RadiusSm)
                                    .background(cellBg)
                                    .border(
                                        width = if (isSelected) 0.dp else if (isToday) 1.dp else 0.dp,
                                        color = if (isToday) ApexThemeTokens.AccentCobaltBorder else Color.Transparent,
                                        shape = ApexThemeTokens.RadiusSm
                                    )
                                    .clickable {
                                        val cal = Calendar.getInstance().apply {
                                            timeInMillis = currentMonthMillis
                                            set(Calendar.DAY_OF_MONTH, dayNumber)
                                            set(Calendar.HOUR_OF_DAY, 12)
                                            set(Calendar.MINUTE, 0)
                                            set(Calendar.SECOND, 0)
                                            set(Calendar.MILLISECOND, 0)
                                        }
                                        onDateSelected(cal.timeInMillis)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = dayNumber.toString(),
                                        color = cellText,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium
                                    )

                                    // Tiny transaction activity indicator dots (Mint for credit, Coral for debit)
                                    if (hasCredit || hasDebit) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (hasCredit) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(3.5.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isSelected) ApexThemeTokens.TextHighContrast else ApexThemeTokens.AccentPrimary)
                                                )
                                            }
                                            if (hasDebit) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(3.5.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isSelected) ApexThemeTokens.AccentDanger else ApexThemeTokens.AccentDanger)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Blank cell outside month bounds
                            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
            }
        }
    }
}

private fun getCalendarDaysForMonth(monthMillis: Long): List<Int?> {
    val cal = Calendar.getInstance().apply {
        timeInMillis = monthMillis
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    // Sunday is 1, Monday is 2 in Calendar; convert so Monday = 0, Sunday = 6
    val firstDayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7

    val list = mutableListOf<Int?>()
    for (i in 0 until firstDayOfWeek) {
        list.add(null)
    }
    for (day in 1..maxDays) {
        list.add(day)
    }
    return list
}
