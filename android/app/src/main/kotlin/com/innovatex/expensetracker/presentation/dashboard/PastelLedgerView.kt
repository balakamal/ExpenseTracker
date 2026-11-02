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
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
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
import com.innovatex.expensetracker.domain.usecase.CategorySpendItem
import com.innovatex.expensetracker.presentation.theme.PastelKineticTokens
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun PastelLedgerView(
    totalCredit: Double,
    totalDebit: Double,
    creditCount: Int,
    debitCount: Int,
    transactions: List<Transaction>,
    categoryTotals: List<CategorySpendItem>,
    currencySymbol: String = "₹",
    onTransactionClick: (Transaction) -> Unit = {},
    onLogExpenseClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val numberFormat = remember {
        NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }

    val netCashflow = totalCredit - totalDebit
    val formattedNet = "$currencySymbol${numberFormat.format(netCashflow)}"
    val formattedInflow = "+$currencySymbol${numberFormat.format(totalCredit)}"
    val formattedOutflow = "-$currencySymbol${numberFormat.format(totalDebit)}"

    var selectedDayIndex by remember { mutableIntStateOf(3) } // Mid-week active
    var selectedCategoryFilter by remember { mutableStateOf("All Items") }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    val daysOfWeek = remember {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        val list = mutableListOf<Pair<String, Int>>()
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        for (i in 0 until 7) {
            list.add(Pair(dayFormat.format(cal.time), cal.get(Calendar.DAY_OF_MONTH)))
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        list
    }

    val filterCategories = remember(categoryTotals) {
        val list = mutableListOf("All Items")
        categoryTotals.forEach { list.add(it.category.name) }
        list
    }

    val filteredTransactions = remember(transactions, selectedCategoryFilter, searchQuery) {
        transactions.filter { tx ->
            val matchesCategory = selectedCategoryFilter == "All Items" || tx.category.equals(selectedCategoryFilter, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() || tx.merchant.contains(searchQuery, ignoreCase = true) || tx.description.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PastelKineticTokens.CanvasBg),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. SpendWell Top App Bar
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
                            imageVector = Icons.Default.ReceiptLong,
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
                            text = "Transactions & Summary",
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
                            .clickable { isSearchActive = !isSearchActive },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
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

        // Search Bar (Expandable)
        item {
            AnimatedVisibility(visible = isSearchActive) {
                androidx.compose.material3.OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search transactions, vendors...", fontSize = 14.sp) },
                    singleLine = true,
                    shape = PastelKineticTokens.RadiusFull,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    colors = androidx.compose.material3.TextFieldDefaults.colors(
                        focusedContainerColor = PastelKineticTokens.SurfaceCard,
                        unfocusedContainerColor = PastelKineticTokens.SurfaceCard,
                        focusedIndicatorColor = PastelKineticTokens.AnchorPrimary,
                        unfocusedIndicatorColor = PastelKineticTokens.OutlineVariant
                    )
                )
            }
        }

        // 2. Weekly Horizontal Day Strip
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(PastelKineticTokens.RadiusLg)
                    .background(PastelKineticTokens.SurfaceCard)
                    .padding(6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                daysOfWeek.forEachIndexed { index, pair ->
                    val isActive = selectedDayIndex == index
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .clip(PastelKineticTokens.RadiusMd)
                            .background(
                                if (isActive) PastelKineticTokens.AnchorPrimary else PastelKineticTokens.SurfaceLow
                            )
                            .clickable { selectedDayIndex = index }
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (isActive) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(PastelKineticTokens.AmberAccent)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                        Text(
                            text = pair.first,
                            color = if (isActive) PastelKineticTokens.AmberFixed else PastelKineticTokens.TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${pair.second}",
                            color = if (isActive) PastelKineticTokens.OnAnchor else PastelKineticTokens.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // 3. Hero Banner: Net Available Cashflow Card (Lilac)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 8.dp,
                        shape = PastelKineticTokens.RadiusCard,
                        spotColor = Color(0x18121316)
                    )
                    .clip(PastelKineticTokens.RadiusCard)
                    .background(PastelKineticTokens.LilacHero)
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(PastelKineticTokens.RadiusFull)
                                .background(PastelKineticTokens.SurfaceCard.copy(alpha = 0.85f))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Monthly Balance",
                                color = PastelKineticTokens.TextPrimary,
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
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = "Trending Up",
                                    tint = PastelKineticTokens.AmberAccent,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "+14.2%",
                                    color = PastelKineticTokens.OnAnchor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Column {
                        Text(
                            text = "Net Available Cashflow",
                            color = PastelKineticTokens.TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = formattedNet,
                            color = PastelKineticTokens.TextPrimary,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.8).sp
                        )
                    }

                    // Safe Pacing Progress Bar
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Safe pacing",
                                color = PastelKineticTokens.TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            val ratio = if (totalCredit > 0) ((totalDebit / totalCredit) * 100).toInt().coerceIn(0, 100) else 0
                            Text(
                                text = "$ratio% allocated",
                                color = PastelKineticTokens.TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        val progressFraction = if (totalCredit > 0) (totalDebit / totalCredit).toFloat().coerceIn(0f, 1f) else 0f
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(PastelKineticTokens.RadiusFull)
                                .background(PastelKineticTokens.SurfaceCard.copy(alpha = 0.7f))
                                .padding(1.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = progressFraction.coerceAtLeast(0.04f))
                                    .height(6.dp)
                                    .clip(PastelKineticTokens.RadiusFull)
                                    .background(PastelKineticTokens.AnchorPrimary)
                            )
                        }
                    }
                }
            }
        }

        // 4. Bento Section: "Your Balance Pods" (Inflow & Outflow side-by-side)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your balance pods",
                        color = PastelKineticTokens.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.clickable { /* Filter option */ }
                    ) {
                        Text(
                            text = "Filter",
                            color = PastelKineticTokens.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Filter",
                            tint = PastelKineticTokens.TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Left Pod: Honey Peach (Income Inflow)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .shadow(4.dp, PastelKineticTokens.RadiusCard, spotColor = Color(0x18825500))
                            .clip(PastelKineticTokens.RadiusCard)
                            .background(PastelKineticTokens.AmberFixed)
                            .padding(16.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.height(145.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(PastelKineticTokens.RadiusFull)
                                        .background(PastelKineticTokens.SurfaceCard.copy(alpha = 0.85f))
                                        .padding(horizontal = 10.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "Inflow",
                                        color = PastelKineticTokens.AmberText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(PastelKineticTokens.SurfaceCard),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = "Inflow",
                                        tint = PastelKineticTokens.AmberDeep,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = "Direct & Gigs",
                                    color = PastelKineticTokens.AmberTextVariant,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = formattedInflow,
                                    color = PastelKineticTokens.AmberText,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(PastelKineticTokens.AmberDeep)
                                    )
                                    Text(
                                        text = "$creditCount deposits",
                                        color = PastelKineticTokens.AmberTextVariant,
                                        fontSize = 11.sp
                                    )
                                }
                                Text(
                                    text = "Active",
                                    color = PastelKineticTokens.AmberText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Right Pod: Soft Sky Glaze (Spent Outflow)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .shadow(4.dp, PastelKineticTokens.RadiusCard, spotColor = Color(0x18001D33))
                            .clip(PastelKineticTokens.RadiusCard)
                            .background(PastelKineticTokens.SkyFixed)
                            .padding(16.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.height(145.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(PastelKineticTokens.RadiusFull)
                                        .background(PastelKineticTokens.SurfaceCard.copy(alpha = 0.85f))
                                        .padding(horizontal = 10.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "Outflow",
                                        color = PastelKineticTokens.SkyText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(PastelKineticTokens.SurfaceCard),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = "Outflow",
                                        tint = PastelKineticTokens.SkyText,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = "Fixed & Flex",
                                    color = PastelKineticTokens.SkyTextVariant,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = formattedOutflow,
                                    color = PastelKineticTokens.SkyText,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(PastelKineticTokens.SkyText)
                                    )
                                    Text(
                                        text = "$debitCount cleared",
                                        color = PastelKineticTokens.SkyTextVariant,
                                        fontSize = 11.sp
                                    )
                                }
                                Text(
                                    text = "73% limit",
                                    color = PastelKineticTokens.SkyText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Interactive Budget Goal Tracker Card
        item {
            val topCategory = categoryTotals.firstOrNull()?.category?.name ?: "Dining & Treats"
            val topCategorySpent = categoryTotals.firstOrNull()?.totalAmount ?: (totalDebit * 0.45).coerceAtLeast(0.0)
            val budgetCap = (topCategorySpent * 1.3).coerceAtLeast(100.0)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, PastelKineticTokens.RadiusCard, spotColor = Color(0x10121316))
                    .clip(PastelKineticTokens.RadiusCard)
                    .background(PastelKineticTokens.SurfaceCard)
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                                    .background(PastelKineticTokens.AmberFixed),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalCafe,
                                    contentDescription = topCategory,
                                    tint = PastelKineticTokens.AmberText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = topCategory,
                                    color = PastelKineticTokens.TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Monthly budget quota",
                                    color = PastelKineticTokens.TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(PastelKineticTokens.RadiusFull)
                                .background(PastelKineticTokens.SurfaceLow)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "On Track",
                                color = PastelKineticTokens.TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Limit Bar & Readout
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "$currencySymbol${numberFormat.format(topCategorySpent)}",
                                color = PastelKineticTokens.TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Cap: $currencySymbol${numberFormat.format(budgetCap)}",
                                color = PastelKineticTokens.TextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        val capRatio = if (budgetCap > 0) (topCategorySpent / budgetCap).toFloat().coerceIn(0f, 1f) else 0f
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(PastelKineticTokens.RadiusFull)
                                .background(PastelKineticTokens.SurfaceContainer)
                                .padding(1.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = capRatio)
                                    .height(8.dp)
                                    .clip(PastelKineticTokens.RadiusFull)
                                    .background(PastelKineticTokens.AmberAccent)
                            )
                        }
                    }
                }
            }
        }

        // 6. Category Filter Chips (Horizontal Scroll)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filterCategories.take(6).forEach { category ->
                    val isSelected = selectedCategoryFilter == category
                    Box(
                        modifier = Modifier
                            .clip(PastelKineticTokens.RadiusFull)
                            .background(
                                if (isSelected) PastelKineticTokens.AnchorPrimary else PastelKineticTokens.SurfaceLow
                            )
                            .clickable { selectedCategoryFilter = category }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = category,
                            color = if (isSelected) PastelKineticTokens.OnAnchor else PastelKineticTokens.TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 7. Recent Activity Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Activity",
                    color = PastelKineticTokens.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "View all",
                    color = PastelKineticTokens.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { selectedCategoryFilter = "All Items" }
                )
            }
        }

        // 8. Transaction Cards
        if (filteredTransactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No transactions found",
                        color = PastelKineticTokens.TextOutline,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            items(filteredTransactions.take(20), key = { it.id }) { tx ->
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
                                    imageVector = if (isCredit) Icons.Default.ArrowDownward else Icons.Default.ReceiptLong,
                                    contentDescription = tx.category,
                                    tint = if (isCredit) PastelKineticTokens.AmberDeep else PastelKineticTokens.SkyText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tx.merchant.ifBlank { tx.description },
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
                                            text = tx.channel.name,
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
                                text = tx.category,
                                color = PastelKineticTokens.TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // 9. Log New Expense Capsule FAB Trigger
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .clip(PastelKineticTokens.RadiusFull)
                        .background(PastelKineticTokens.AnchorPrimary)
                        .clickable { onLogExpenseClick() }
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Log Expense",
                        tint = PastelKineticTokens.OnAnchor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Log New Expense",
                        color = PastelKineticTokens.OnAnchor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

