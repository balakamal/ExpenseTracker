package com.innovatex.expensetracker.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovatex.expensetracker.presentation.theme.ApexThemeTokens
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun MonthCarouselSelector(
    availableMonths: List<String>,
    selectedMonth: String,
    onMonthSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        items(availableMonths) { monthKey ->
            val isSelected = monthKey == selectedMonth
            val background = if (isSelected) ApexThemeTokens.AccentCobalt else ApexThemeTokens.BgGlassPill
            val textColor = if (isSelected) ApexThemeTokens.TextPrimary else ApexThemeTokens.TextSecondary
            val border = if (isSelected) ApexThemeTokens.AccentCobaltBorder else ApexThemeTokens.StrokeSubtle

            val displayLabel = formatMonthLabel(monthKey)

            Box(
                modifier = Modifier
                    .clip(ApexThemeTokens.RadiusFull)
                    .background(background)
                    .border(1.dp, border, ApexThemeTokens.RadiusFull)
                    .clickable { onMonthSelected(monthKey) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = displayLabel,
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

private fun formatMonthLabel(monthKey: String): String {
    return runCatching {
        val parser = SimpleDateFormat("yyyy-MM", Locale.US)
        val formatter = SimpleDateFormat("MMM yyyy", Locale.US)
        val date = parser.parse(monthKey)
        if (date != null) formatter.format(date) else monthKey
    }.getOrDefault(monthKey)
}
