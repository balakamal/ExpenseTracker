package com.innovatex.expensetracker.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovatex.expensetracker.presentation.theme.ApexThemeTokens
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ApexFintechSummaryCard(
    displayMonth: String,
    totalDebit: Double,
    totalCredit: Double,
    budgetLimit: Double,
    progressPercent: Int,
    currencySymbol: String = "$",
    modifier: Modifier = Modifier,
    onBudgetClick: () -> Unit = {}
) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US).apply { minimumFractionDigits = 2; maximumFractionDigits = 2 } }
    val formattedDebit = "$currencySymbol${numberFormat.format(totalDebit)}"
    val formattedCredit = "$currencySymbol${numberFormat.format(totalCredit)}"
    val formattedBudget = "$currencySymbol${numberFormat.format(budgetLimit)}"

    val isOverBudget = progressPercent > 100
    val progressColor = if (isOverBudget) ApexThemeTokens.AccentDanger else ApexThemeTokens.AccentPrimary
    val progressTrack = if (isOverBudget) ApexThemeTokens.AccentDangerTint else ApexThemeTokens.AccentPrimaryTint

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(ApexThemeTokens.RadiusLg)
            .background(ApexThemeTokens.BgSurfaceCard)
            .border(1.dp, ApexThemeTokens.StrokeHighlight, ApexThemeTokens.RadiusLg)
            .padding(22.dp)
    ) {
        Column {
            // Month pill + Live indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .clip(ApexThemeTokens.RadiusFull)
                        .background(ApexThemeTokens.BgGlassPill)
                        .border(1.dp, ApexThemeTokens.StrokeSubtle, ApexThemeTokens.RadiusFull)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = displayMonth,
                        color = ApexThemeTokens.TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Live Sync Dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(ApexThemeTokens.AccentPrimary)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "LOCAL LIVE",
                    color = ApexThemeTokens.AccentPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Large Monthly Spend Headline
            Text(
                text = "Total Spent",
                color = ApexThemeTokens.TextTertiary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formattedDebit,
                color = ApexThemeTokens.TextPrimary,
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Linear Progress Bar towards Budget
            LinearProgressIndicator(
                progress = { (progressPercent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(ApexThemeTokens.RadiusFull),
                color = progressColor,
                trackColor = progressTrack,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Sub-metrics: Budget Target & Total Credits (Income)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${progressPercent}% of $formattedBudget budget",
                    color = if (isOverBudget) ApexThemeTokens.AccentDanger else ApexThemeTokens.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBudgetClick
                    )
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "+$formattedCredit received",
                    color = ApexThemeTokens.AccentPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Shimmering skeleton loader for frictionless data fetching without spinning wheels.
 */
@Composable
fun ShimmerCardLoader(
    modifier: Modifier = Modifier,
    height: Int = 80
) {
    val shimmerColors = listOf(
        ApexThemeTokens.BgSurface,
        ApexThemeTokens.BgSurfaceElevated,
        ApexThemeTokens.BgSurface
    )

    val transition = rememberInfiniteTransition(label = "Shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShimmerTranslate"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(ApexThemeTokens.RadiusMd)
            .background(brush)
            .border(1.dp, ApexThemeTokens.StrokeSubtle, ApexThemeTokens.RadiusMd)
    )
}
