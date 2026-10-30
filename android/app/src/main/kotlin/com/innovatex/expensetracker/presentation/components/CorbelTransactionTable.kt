package com.innovatex.expensetracker.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovatex.expensetracker.domain.model.SourceChannel
import com.innovatex.expensetracker.domain.model.Transaction
import com.innovatex.expensetracker.domain.model.TransactionType
import com.innovatex.expensetracker.domain.model.PaymentMedium
import com.innovatex.expensetracker.domain.usecase.CategorySpendItem
import com.innovatex.expensetracker.presentation.theme.ApexThemeTokens
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CorbelTransactionTable(
    transactions: List<Transaction>,
    categoryTotals: List<CategorySpendItem>,
    onTransactionClick: (Transaction) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(TransactionFilter.ALL) }
    var selectedMedium by remember { mutableStateOf(PaymentMediumFilter.ALL) }
    var isTableView by remember { mutableStateOf(false) }

    val numberFormat = remember {
        NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }

    val dateFormatter = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    // Map Category IDs to Category names and colors
    val categoryMap = remember(categoryTotals) {
        categoryTotals.associate { it.category.id to it.category }
    }

    // Filter transactions based on search and type pill
    val filteredTransactions = remember(transactions, searchQuery, selectedFilter, selectedMedium) {
        transactions.filter { txn ->
            val matchesFilter = when (selectedFilter) {
                TransactionFilter.ALL -> txn.isValidTransaction
                TransactionFilter.DEBITS -> txn.type == TransactionType.DEBIT && txn.isValidTransaction
                TransactionFilter.CREDITS -> txn.type == TransactionType.CREDIT && txn.isValidTransaction
                TransactionFilter.PROMO_SPAM -> !txn.isValidTransaction
            }

            val matchesSearch = if (searchQuery.isBlank()) true else {
                txn.merchantClean.contains(searchQuery, ignoreCase = true) ||
                    txn.merchantRaw.contains(searchQuery, ignoreCase = true) ||
                    (txn.referenceId?.contains(searchQuery, ignoreCase = true) == true) ||
                    (txn.accountLast4?.contains(searchQuery) == true) ||
                    txn.paymentMedium.displayName.contains(searchQuery, ignoreCase = true) ||
                    (txn.sender?.contains(searchQuery, ignoreCase = true) == true)
            }

            val matchesMedium = when (selectedMedium) {
                PaymentMediumFilter.ALL -> true
                PaymentMediumFilter.UPI -> txn.paymentMedium == PaymentMedium.UPI
                PaymentMediumFilter.DEBIT_CARD -> txn.paymentMedium == PaymentMedium.CARD &&
                    txn.rawSnippet.orEmpty().contains("debit card", ignoreCase = true)
                PaymentMediumFilter.CREDIT_CARD -> txn.paymentMedium == PaymentMedium.CARD &&
                    txn.rawSnippet.orEmpty().contains("credit card", ignoreCase = true)
            }
            matchesFilter && matchesSearch && matchesMedium
        }
    }

    val debitCount = remember(transactions) { transactions.count { it.type == TransactionType.DEBIT && it.isValidTransaction } }
    val creditCount = remember(transactions) { transactions.count { it.type == TransactionType.CREDIT && it.isValidTransaction } }
    val promoSpamCount = remember(transactions) { transactions.count { !it.isValidTransaction } }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(ApexThemeTokens.RadiusCard)
            .background(ApexThemeTokens.BgSurfaceCard)
            .border(1.dp, ApexThemeTokens.StrokeSubtle, ApexThemeTokens.RadiusCard)
            .padding(16.dp)
    ) {
        Column {
            // Table Top Toolbar: Title & Search Field
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "TRANSACTION LEDGER",
                        color = ApexThemeTokens.TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${filteredTransactions.size} Records",
                        color = ApexThemeTokens.TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Search pill input
                Box(
                    modifier = Modifier
                        .width(160.dp)
                        .height(34.dp)
                        .clip(ApexThemeTokens.RadiusFull)
                        .background(ApexThemeTokens.BgGlassPill)
                        .border(1.dp, ApexThemeTokens.StrokeSubtle, ApexThemeTokens.RadiusFull)
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = ApexThemeTokens.TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search...",
                                    color = ApexThemeTokens.TextMuted,
                                    fontSize = 12.sp
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    color = ApexThemeTokens.TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                cursorBrush = SolidColor(ApexThemeTokens.AccentCobalt),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = ApexThemeTokens.TextMuted,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { searchQuery = "" }
                            )
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp)
            ) {
                PaymentMediumFilter.entries.forEach { filter ->
                    FilterPill(
                        label = filter.label,
                        isSelected = selectedMedium == filter,
                        onClick = { selectedMedium = filter }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Corbel Filter Pills (All, Debits, Credits) & View Mode Switcher (Cards / Table)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    FilterPill(
                        label = "All ${debitCount + creditCount}",
                        isSelected = selectedFilter == TransactionFilter.ALL,
                        onClick = { selectedFilter = TransactionFilter.ALL }
                    )
                    FilterPill(
                        label = "Debits $debitCount",
                        isSelected = selectedFilter == TransactionFilter.DEBITS,
                        activeColor = ApexThemeTokens.AccentDanger,
                        onClick = { selectedFilter = TransactionFilter.DEBITS }
                    )
                    FilterPill(
                        label = "Credits $creditCount",
                        isSelected = selectedFilter == TransactionFilter.CREDITS,
                        activeColor = ApexThemeTokens.AccentPrimary,
                        onClick = { selectedFilter = TransactionFilter.CREDITS }
                    )
                    if (promoSpamCount > 0) {
                        FilterPill(
                            label = "Reminders/Spam $promoSpamCount",
                            isSelected = selectedFilter == TransactionFilter.PROMO_SPAM,
                            activeColor = ApexThemeTokens.AccentSecondary,
                            onClick = { selectedFilter = TransactionFilter.PROMO_SPAM }
                        )
                    }
                }

                // Corbel Segmented View Switcher: Cards vs Table
                Row(
                    modifier = Modifier
                        .clip(ApexThemeTokens.RadiusFull)
                        .background(ApexThemeTokens.BgGlassPill)
                        .border(1.dp, ApexThemeTokens.StrokeSubtle, ApexThemeTokens.RadiusFull)
                        .padding(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(ApexThemeTokens.RadiusFull)
                            .background(if (!isTableView) ApexThemeTokens.BgHighContrast else Color.Transparent)
                            .clickable { isTableView = false }
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Cards",
                            color = if (!isTableView) ApexThemeTokens.TextHighContrast else ApexThemeTokens.TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (!isTableView) FontWeight.Bold else FontWeight.Medium
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(ApexThemeTokens.RadiusFull)
                            .background(if (isTableView) ApexThemeTokens.BgHighContrast else Color.Transparent)
                            .clickable { isTableView = true }
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Table",
                            color = if (isTableView) ApexThemeTokens.TextHighContrast else ApexThemeTokens.TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isTableView) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Table / Cards Body Rows or Empty State
            if (filteredTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ReceiptLong,
                            contentDescription = "No Records",
                            tint = ApexThemeTokens.TextMuted,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Transactions Found",
                            color = ApexThemeTokens.TextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Try adjusting your search terms" else "No transactions match this selection",
                            color = ApexThemeTokens.TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            } else if (!isTableView) {
                // 1. CARDS VIEW: Mobile-Optimized FinTech Ledger (Full Merchant Visibility, Squircle Category Icon)
                Column(modifier = Modifier.fillMaxWidth()) {
                    filteredTransactions.forEachIndexed { index, txn ->
                        val date = Date(txn.timestamp)
                        val dateStr = dateFormatter.format(date)
                        val timeStr = timeFormatter.format(date)
                        val isCredit = txn.type == TransactionType.CREDIT
                        val category = categoryMap[txn.categoryId]
                        val categoryColor = category?.colorHex?.let {
                            runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull()
                        } ?: if (isCredit) ApexThemeTokens.AccentPrimary else ApexThemeTokens.AccentCobalt

                        val isPromo = !txn.isValidTransaction
                        val amountStr = if (isPromo) "₹${numberFormat.format(txn.amount)}" else if (isCredit) "+₹${numberFormat.format(txn.amount)}" else "-₹${numberFormat.format(txn.amount)}"
                        val amountColor = if (isPromo) ApexThemeTokens.TextMuted else if (isCredit) ApexThemeTokens.AccentPrimary else ApexThemeTokens.TextPrimary

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTransactionClick(txn) }
                                .padding(horizontal = 4.dp, vertical = 10.dp)
                        ) {
                            // Category Squircle Avatar with initial
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(ApexThemeTokens.RadiusSm)
                                    .background(categoryColor.copy(alpha = 0.14f))
                                    .border(1.dp, categoryColor.copy(alpha = 0.35f), ApexThemeTokens.RadiusSm),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (category?.name ?: txn.categoryId).take(2).uppercase(),
                                    color = categoryColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(11.dp))

                            // Middle Column: Merchant Name & Subline Chips
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = txn.merchantClean.ifBlank { "Unknown Merchant" },
                                    color = ApexThemeTokens.TextPrimary,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Category Chip
                                    Box(
                                        modifier = Modifier
                                            .clip(ApexThemeTokens.RadiusFull)
                                            .background(categoryColor.copy(alpha = 0.12f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(categoryColor)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = category?.name ?: txn.categoryId.replaceFirstChar { it.titlecase() },
                                                color = categoryColor,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    if (isPromo) {
                                        Box(
                                            modifier = Modifier
                                                .clip(ApexThemeTokens.RadiusFull)
                                                .background(ApexThemeTokens.AccentSecondaryTint)
                                                .border(0.5.dp, ApexThemeTokens.AccentSecondaryBorder, ApexThemeTokens.RadiusFull)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "PROMO",
                                                color = ApexThemeTokens.AccentSecondary,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    PaymentMediumBadge(medium = txn.paymentMedium)

                                    if (!txn.accountLast4.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "••${txn.accountLast4}",
                                            color = ApexThemeTokens.TextMuted,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Right Column: Formatted Tabular Amount & Date/Time
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = amountStr,
                                    color = amountColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.End
                                )
                                if (isPromo) {
                                    Text(
                                        text = "Excluded from totals",
                                        color = ApexThemeTokens.AccentSecondary,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.End
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$dateStr, $timeStr",
                                    color = ApexThemeTokens.TextMuted,
                                    fontSize = 10.5.sp,
                                    textAlign = TextAlign.End
                                )
                            }
                        }

                        if (index < filteredTransactions.lastIndex) {
                            HorizontalDivider(
                                color = ApexThemeTokens.StrokeSubtle.copy(alpha = 0.5f),
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
            } else {
                // 2. TABLE VIEW: Horizontally Scrollable Multi-Column Grid (Zero Squishing)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    Column(modifier = Modifier.widthIn(min = 490.dp)) {
                        // Table Header Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ApexThemeTokens.BgGlassPill)
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                .clip(ApexThemeTokens.RadiusXs)
                        ) {
                            Text(
                                text = "DATE",
                                color = ApexThemeTokens.TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(72.dp)
                            )
                            Text(
                                text = "MERCHANT & CHANNEL",
                                color = ApexThemeTokens.TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(170.dp)
                            )
                            Text(
                                text = "CATEGORY",
                                color = ApexThemeTokens.TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(110.dp)
                            )
                            Text(
                                text = "AMOUNT",
                                color = ApexThemeTokens.TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(100.dp)
                            )
                        }

                        HorizontalDivider(
                            color = ApexThemeTokens.StrokeSubtle,
                            thickness = 1.dp
                        )

                        filteredTransactions.forEachIndexed { index, txn ->
                            val date = Date(txn.timestamp)
                            val dateStr = dateFormatter.format(date)
                            val timeStr = timeFormatter.format(date)
                            val isCredit = txn.type == TransactionType.CREDIT
                            val category = categoryMap[txn.categoryId]
                            val categoryColor = category?.colorHex?.let {
                                runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull()
                            } ?: if (isCredit) ApexThemeTokens.AccentPrimary else ApexThemeTokens.AccentCobalt

                            val isPromo = !txn.isValidTransaction
                            val amountStr = if (isPromo) "₹${numberFormat.format(txn.amount)}" else if (isCredit) "+₹${numberFormat.format(txn.amount)}" else "-₹${numberFormat.format(txn.amount)}"
                            val amountColor = if (isPromo) ApexThemeTokens.TextMuted else if (isCredit) ApexThemeTokens.AccentPrimary else ApexThemeTokens.TextPrimary

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onTransactionClick(txn) }
                                    .padding(horizontal = 10.dp, vertical = 11.dp)
                            ) {
                                // Column 1: Date & Time
                                Column(modifier = Modifier.width(72.dp)) {
                                    Text(
                                        text = dateStr,
                                        color = ApexThemeTokens.TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = timeStr,
                                        color = ApexThemeTokens.TextMuted,
                                        fontSize = 9.5.sp
                                    )
                                }

                                // Column 2: Merchant & Channel
                                Column(
                                    modifier = Modifier
                                        .width(170.dp)
                                        .padding(end = 8.dp)
                                ) {
                                    Text(
                                        text = txn.merchantClean.ifBlank { "Unknown Merchant" },
                                        color = ApexThemeTokens.TextPrimary,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isPromo) {
                                        Box(
                                            modifier = Modifier
                                                .clip(ApexThemeTokens.RadiusFull)
                                                .background(ApexThemeTokens.AccentSecondaryTint)
                                                .border(0.5.dp, ApexThemeTokens.AccentSecondaryBorder, ApexThemeTokens.RadiusFull)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "PROMO",
                                                color = ApexThemeTokens.AccentSecondary,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    PaymentMediumBadge(medium = txn.paymentMedium)
                                        if (!txn.accountLast4.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "••${txn.accountLast4}",
                                                color = ApexThemeTokens.TextMuted,
                                                fontSize = 9.5.sp
                                            )
                                        }
                                    }
                                }

                                // Column 3: Category Pill
                                Box(
                                    modifier = Modifier
                                        .width(110.dp)
                                        .clip(ApexThemeTokens.RadiusFull)
                                        .background(categoryColor.copy(alpha = 0.12f))
                                        .padding(horizontal = 7.dp, vertical = 3.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(categoryColor)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = category?.name ?: txn.categoryId.replaceFirstChar { it.titlecase() },
                                            color = categoryColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Column 4: Amount
                                Text(
                                    text = amountStr,
                                    color = amountColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.width(100.dp)
                                )
                            }

                            if (index < filteredTransactions.lastIndex) {
                                HorizontalDivider(
                                    color = ApexThemeTokens.StrokeSubtle.copy(alpha = 0.5f),
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(horizontal = 10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    isSelected: Boolean,
    activeColor: Color = ApexThemeTokens.TextHighContrast,
    onClick: () -> Unit
) {
    val bg = if (isSelected) ApexThemeTokens.BgHighContrast else ApexThemeTokens.BgGlassPill
    val textColor = if (isSelected) ApexThemeTokens.TextHighContrast else ApexThemeTokens.TextSecondary
    val border = if (isSelected) Color.Transparent else ApexThemeTokens.StrokeSubtle

    Box(
        modifier = Modifier
            .clip(ApexThemeTokens.RadiusFull)
            .background(bg)
            .border(1.dp, border, ApexThemeTokens.RadiusFull)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

enum class TransactionFilter {
    ALL,
    DEBITS,
    CREDITS,
    PROMO_SPAM
}

private enum class PaymentMediumFilter(val label: String) {
    ALL("All methods"), UPI("UPI"), DEBIT_CARD("Debit card"), CREDIT_CARD("Credit card")
}
