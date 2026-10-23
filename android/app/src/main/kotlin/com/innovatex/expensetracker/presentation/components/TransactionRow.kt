package com.innovatex.expensetracker.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CarRental
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovatex.expensetracker.domain.model.SourceChannel
import com.innovatex.expensetracker.domain.model.Transaction
import com.innovatex.expensetracker.domain.model.TransactionType
import com.innovatex.expensetracker.presentation.theme.ApexThemeTokens
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionRow(
    transaction: Transaction,
    categoryName: String,
    categoryColorHex: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale("en", "IN")).apply { minimumFractionDigits = 2; maximumFractionDigits = 2 } }
    val formattedAmount = numberFormat.format(transaction.amount)
    val isCredit = transaction.type == TransactionType.CREDIT

    val amountColor = if (isCredit) ApexThemeTokens.AccentPrimary else ApexThemeTokens.TextPrimary
    val amountPrefix = if (isCredit) "+" else "-"

    val categoryColor = remember(categoryColorHex) {
        runCatching { Color(android.graphics.Color.parseColor(categoryColorHex)) }.getOrDefault(ApexThemeTokens.AccentPrimary)
    }

    val iconVector = getCategoryIconVector(transaction.categoryId)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(ApexThemeTokens.RadiusMd)
            .background(ApexThemeTokens.BgSurface)
            .border(1.dp, ApexThemeTokens.StrokeSubtle, ApexThemeTokens.RadiusMd)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Category Icon with subtle background tint
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(categoryColor.copy(alpha = 0.12f))
                    .border(1.dp, categoryColor.copy(alpha = 0.28f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = categoryName,
                    tint = categoryColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Merchant & Source channel / Date
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = transaction.merchantClean,
                    color = ApexThemeTokens.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTimestamp(transaction.timestamp),
                        color = ApexThemeTokens.TextTertiary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Source Badge (SMS / Email / SMS+Email)
                    SourceBadge(sources = transaction.sources, isDeduplicated = transaction.isDeduplicated)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Amount
            Text(
                text = "$amountPrefix₹${formattedAmount}",
                color = amountColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SourceBadge(
    sources: List<SourceChannel>,
    isDeduplicated: Boolean,
    modifier: Modifier = Modifier
) {
    val text = when {
        isDeduplicated || (sources.contains(SourceChannel.SMS) && sources.contains(SourceChannel.EMAIL)) -> "SMS + EMAIL"
        sources.contains(SourceChannel.EMAIL) -> "EMAIL"
        sources.contains(SourceChannel.SMS) -> "SMS"
        else -> "MANUAL"
    }

    val badgeColor = when {
        isDeduplicated -> ApexThemeTokens.AccentCobalt
        sources.contains(SourceChannel.EMAIL) -> ApexThemeTokens.AccentSecondary
        else -> ApexThemeTokens.TextTertiary
    }

    Box(
        modifier = modifier
            .clip(ApexThemeTokens.RadiusFull)
            .background(badgeColor.copy(alpha = 0.12f))
            .border(1.dp, badgeColor.copy(alpha = 0.24f), ApexThemeTokens.RadiusFull)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = badgeColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

fun getCategoryIconVector(categoryId: String): ImageVector {
    return when (categoryId.lowercase()) {
        "food_dining" -> Icons.Default.Fastfood
        "groceries" -> Icons.Default.ShoppingCart
        "shopping" -> Icons.Default.ShoppingBag
        "transport" -> Icons.Default.DirectionsCar
        "utilities" -> Icons.Default.Receipt
        "entertainment" -> Icons.Default.Movie
        "health" -> Icons.Default.Favorite
        "income" -> Icons.Default.TrendingUp
        else -> Icons.Default.MoreHoriz
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("d MMM, h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
