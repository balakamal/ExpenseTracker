package com.innovatex.expensetracker.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovatex.expensetracker.domain.usecase.CategorySpendItem
import com.innovatex.expensetracker.presentation.theme.ApexThemeTokens
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CorbelCategoryBreakdown(
    categoryTotals: List<CategorySpendItem>,
    isOverallView: Boolean,
    modifier: Modifier = Modifier
) {
    val numberFormat = remember {
        NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
        ) {
            Text(
                text = "CATEGORY BREAKDOWN",
                color = ApexThemeTokens.TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${categoryTotals.size} categories",
                color = ApexThemeTokens.TextMuted,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (categoryTotals.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ApexThemeTokens.RadiusCard)
                    .background(ApexThemeTokens.BgSurfaceCard)
                    .border(1.dp, ApexThemeTokens.StrokeSubtle, ApexThemeTokens.RadiusCard)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No category activity for this selection",
                    color = ApexThemeTokens.TextSecondary,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categoryTotals, key = { it.category.id }) { item ->
                    val color = remember(item.category.colorHex) {
                        runCatching { Color(android.graphics.Color.parseColor(item.category.colorHex)) }
                            .getOrDefault(ApexThemeTokens.AccentCobalt)
                    }
                    val iconVector = getCategoryIconVector(item.category.id)
                    val formattedSpent = "₹${numberFormat.format(item.totalSpent)}"

                    Box(
                        modifier = Modifier
                            .width(170.dp)
                            .clip(ApexThemeTokens.RadiusCard)
                            .background(ApexThemeTokens.BgSurfaceCard)
                            .border(1.dp, ApexThemeTokens.StrokeSubtle, ApexThemeTokens.RadiusCard)
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(color.copy(alpha = 0.15f))
                                        .border(1.dp, color.copy(alpha = 0.35f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = iconVector,
                                        contentDescription = item.category.name,
                                        tint = color,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                // Percentage Pill
                                Box(
                                    modifier = Modifier
                                        .clip(ApexThemeTokens.RadiusFull)
                                        .background(ApexThemeTokens.BgGlassPill)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${item.percentageOfTotal.toInt()}%",
                                        color = ApexThemeTokens.TextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = item.category.name,
                                color = ApexThemeTokens.TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = formattedSpent,
                                color = ApexThemeTokens.TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.3).sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Corbel-styled horizontal meter bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(ApexThemeTokens.StrokeSubtle)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction = (item.percentageOfTotal / 100f).coerceIn(0.05f, 1f))
                                        .height(4.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
