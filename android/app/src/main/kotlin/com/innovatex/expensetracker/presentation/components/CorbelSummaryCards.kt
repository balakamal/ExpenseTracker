package com.innovatex.expensetracker.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
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
import com.innovatex.expensetracker.presentation.theme.ApexThemeTokens
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CorbelTwoSummaryCards(
    totalCredit: Double,
    totalDebit: Double,
    creditCount: Int,
    debitCount: Int,
    selectedDateDisplay: String?,
    isOverallView: Boolean,
    currencySymbol: String = "₹",
    modifier: Modifier = Modifier
) {
    val numberFormat = remember {
        NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }

    val formattedCredit = "$currencySymbol${numberFormat.format(totalCredit)}"
    val formattedDebit = "$currencySymbol${numberFormat.format(totalDebit)}"

    val isRange = selectedDateDisplay?.contains("–") == true

    val creditSubtitle = when {
        isOverallView -> "$creditCount deposits recorded"
        isRange -> "$creditCount deposits in range"
        selectedDateDisplay != null -> "$creditCount deposits on this date"
        else -> "$creditCount deposits"
    }

    val debitSubtitle = when {
        isOverallView -> "$debitCount expenses recorded"
        isRange -> "$debitCount expenses in range"
        selectedDateDisplay != null -> "$debitCount expenses on this date"
        else -> "$debitCount expenses"
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Card 1: Credited Amount (Corbel Mint Theme)
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(ApexThemeTokens.RadiusCard)
                .background(ApexThemeTokens.BgSurfaceCard)
                .border(1.dp, ApexThemeTokens.StrokeSubtle, ApexThemeTokens.RadiusCard)
                .padding(18.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(ApexThemeTokens.AccentPrimaryTint)
                            .border(1.dp, ApexThemeTokens.AccentPrimaryBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ArrowDownward,
                            contentDescription = "Credited Inflow",
                            tint = ApexThemeTokens.AccentPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Mint Delta Tag
                    Box(
                        modifier = Modifier
                            .clip(ApexThemeTokens.RadiusFull)
                            .background(ApexThemeTokens.AccentPrimaryTint)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "CREDITED",
                            color = ApexThemeTokens.AccentPrimary,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (isOverallView) "Total Inflow" else if (isRange) "Range Inflow" else "Credited Amount",
                    color = ApexThemeTokens.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(4.dp))

                AnimatedContent(
                    targetState = formattedCredit,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "creditAmount"
                ) { target ->
                    Text(
                        text = "+$target",
                        color = ApexThemeTokens.AccentPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = creditSubtitle,
                    color = ApexThemeTokens.TextMuted,
                    fontSize = 11.sp
                )
            }
        }

        // Card 2: Debited Amount (Corbel Coral Theme)
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(ApexThemeTokens.RadiusCard)
                .background(ApexThemeTokens.BgSurfaceCard)
                .border(1.dp, ApexThemeTokens.StrokeSubtle, ApexThemeTokens.RadiusCard)
                .padding(18.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(ApexThemeTokens.AccentDangerTint)
                            .border(1.dp, ApexThemeTokens.AccentDangerBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ArrowUpward,
                            contentDescription = "Debited Outflow",
                            tint = ApexThemeTokens.AccentDanger,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Coral Delta Tag
                    Box(
                        modifier = Modifier
                            .clip(ApexThemeTokens.RadiusFull)
                            .background(ApexThemeTokens.AccentDangerTint)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "DEBITED",
                            color = ApexThemeTokens.AccentDanger,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (isOverallView) "Total Outflow" else "Debited Amount",
                    color = ApexThemeTokens.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(4.dp))

                AnimatedContent(
                    targetState = formattedDebit,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "debitAmount"
                ) { target ->
                    Text(
                        text = "-$target",
                        color = ApexThemeTokens.AccentDanger,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = debitSubtitle,
                    color = ApexThemeTokens.TextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}
