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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
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
import com.innovatex.expensetracker.presentation.components.getCategoryIconVector
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
    monthlyBudget: Double = 25000.0,
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
    val formattedNet = "${if (netCashflow >= 0) "" else "-"}$currencySymbol${numberFormat.format(kotlin.math.abs(netCashflow))}"
    val formattedInflow = "+$currencySymbol${numberFormat.format(totalCredit)}"
    val formattedOutflow = "-$currencySymbol${numberFormat.format(totalDebit)}"

    // Dynamic savings rate or deficit velocity
    val savingsRate = if (totalCredit > 0) (((totalCredit - totalDebit) / totalCredit) * 100).toInt() else 0
    val velocityPositive = netCashflow >= 0
    val velocityLabel = if (totalCredit > 0) {
        if (savingsRate >= 0) "+$savingsRate% saved" else "$savingsRate% deficit"
    } else {
        if (totalDebit > 0) "100% outflow" else "0% activity"
    }

    var selectedDayIndex by remember { mutableStateOf<Int?>(null) } // null = all days
    var selectedCategoryFilter by remember { mutableStateOf("All Items") }
    var balancePodFilter by remember { mutableIntStateOf(0) } // 0 = All, 1 = Inflow Only, 2 = Outflow Only
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    val daysOfWeek = remember {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        val list = mutableListOf<Triple<String, Int, Int>>() // DayName, DayOfMonth, DayOfYear
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        for (i in 0 until 7) {
            list.add(Triple(dayFormat.format(cal.time), cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.DAY_OF_YEAR)))
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        list
    }

    val categoryMap = remember(categoryTotals) {
        categoryTotals.associate { it.category.id to it.category.name }
    }

    val filterCategories = remember(categoryTotals) {
        val list = mutableListOf("All Items")
        categoryTotals.forEach { list.add(it.category.name) }
        list
    }

    // Filter transactions dynamically
    val filteredTransactions = remember(
        transactions,
        selectedCategoryFilter,
        searchQuery,
        categoryMap,
        selectedDayIndex,
        balancePodFilter
    ) {
        transactions.filter { tx ->
            // Category filter
            val catName = categoryMap[tx.categoryId] ?: tx.categoryId
            val matchesCategory = selectedCategoryFilter == "All Items" ||
                catName.equals(selectedCategoryFilter, ignoreCase = true) ||
                tx.categoryId.equals(selectedCategoryFilter, ignoreCase = true)

            // Search filter
            val matchesSearch = searchQuery.isBlank() ||
                tx.merchantClean.contains(searchQuery, ignoreCase = true) ||
                tx.merchantRaw.contains(searchQuery, ignoreCase = true)

            // Balance pod filter (Inflow / Outflow)
            val matchesPod = when (balancePodFilter) {
                1 -> tx.type == TransactionType.CREDIT
                2 -> tx.type == TransactionType.DEBIT
                else -> true
            }

            // Day of week filter
            val matchesDay = if (selectedDayIndex != null) {
                val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                val targetDayOfYear = daysOfWeek.getOrNull(selectedDayIndex!!)?.third
                targetDayOfYear == null || txCal.get(Calendar.DAY_OF_YEAR) == targetDayOfYear
            } else {
                true
            }

            matchesCategory && matchesSearch && matchesPod && matchesDay
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
                            .background(if (isSearchActive) PastelKineticTokens.AmberFixed else PastelKineticTokens.SurfaceContainer)
                            .clickable { isSearchActive = !isSearchActive },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (isSearchActive) PastelKineticTokens.AmberText else PastelKineticTokens.TextPrimary,
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
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search transactions, vendors...", fontSize = 14.sp) },
                    singleLine = true,
                    shape = PastelKineticTokens.RadiusFull,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = PastelKineticTokens.SurfaceCard,
                        unfocusedContainerColor = PastelKineticTokens.SurfaceCard,
                        focusedIndicatorColor = PastelKineticTokens.AnchorPrimary,
                        unfocusedIndicatorColor = PastelKineticTokens.OutlineVariant
                    )
                )
            }
        }

        // 2. Weekly Horizontal Day Strip with Toggle
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(PastelKineticTokens.RadiusLg)
                        .background(PastelKineticTokens.SurfaceCard)
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    daysOfWeek.forEachIndexed { index, triple ->
                        val isActive = selectedDayIndex == index
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .clip(PastelKineticTokens.RadiusMd)
                                .background(
                                    if (isActive) PastelKineticTokens.AnchorPrimary else PastelKineticTokens.SurfaceLow
                                )
                                .clickable {
                                    selectedDayIndex = if (selectedDayIndex == index) null else index
                                }
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
                                text = triple.first,
                                color = if (isActive) PastelKineticTokens.AmberFixed else PastelKineticTokens.TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${triple.second}",
                                color = if (isActive) PastelKineticTokens.OnAnchor else PastelKineticTokens.TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold
                            )
                        }
                    }
                }
                if (selectedDayIndex != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Filtered by ${daysOfWeek[selectedDayIndex!!].first}, day ${daysOfWeek[selectedDayIndex!!].second}",
                            color = PastelKineticTokens.TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Reset day",
                            color = PastelKineticTokens.AmberDeep,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { selectedDayIndex = null }
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
                                    imageVector = if (velocityPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                    contentDescription = "Velocity",
                                    tint = if (velocityPositive) PastelKineticTokens.AmberAccent else PastelKineticTokens.Error,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = velocityLabel,
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
                    val budgetCap = if (monthlyBudget > 0) monthlyBudget else 25000.0
                    val allocatedRatio = ((totalDebit / budgetCap) * 100).toInt().coerceIn(0, 100)
                    val progressFraction = (totalDebit / budgetCap).toFloat().coerceIn(0f, 1f)

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Monthly pacing",
                                color = PastelKineticTokens.TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "$allocatedRatio% allocated ($currencySymbol${numberFormat.format(totalDebit)} / $currencySymbol${numberFormat.format(budgetCap)})",
                                color = PastelKineticTokens.TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

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
                        modifier = Modifier
                            .clip(PastelKineticTokens.RadiusFull)
                            .background(if (balancePodFilter != 0) PastelKineticTokens.AmberFixed else Color.Transparent)
                            .clickable {
                                balancePodFilter = (balancePodFilter + 1) % 3
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = when (balancePodFilter) {
                                1 -> "Inflow Only"
                                2 -> "Outflow Only"
                                else -> "Filter Pods"
                            },
                            color = if (balancePodFilter != 0) PastelKineticTokens.AmberText else PastelKineticTokens.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Filter",
                            tint = if (balancePodFilter != 0) PastelKineticTokens.AmberText else PastelKineticTokens.TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Left Pod: Honey Peach (Income Inflow)
                    val isPodActive = balancePodFilter == 0 || balancePodFilter == 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .shadow(4.dp, PastelKineticTokens.RadiusCard, spotColor = Color(0x18825500))
                            .clip(PastelKineticTokens.RadiusCard)
                            .background(if (isPodActive) PastelKineticTokens.AmberFixed else PastelKineticTokens.SurfaceLow)
                            .clickable { balancePodFilter = if (balancePodFilter == 1) 0 else 1 }
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
                                    text = if (creditCount > 0) "Income & Deposits" else "No deposits yet",
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
                                    text = if (totalCredit > 0) "Active" else "Empty",
                                    color = PastelKineticTokens.AmberText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Right Pod: Soft Sky Glaze (Spent Outflow)
                    val isOutflowPodActive = balancePodFilter == 0 || balancePodFilter == 2
                    val budgetCapVal = if (monthlyBudget > 0) monthlyBudget else 25000.0
                    val outflowLimitPct = ((totalDebit / budgetCapVal) * 100).toInt()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .shadow(4.dp, PastelKineticTokens.RadiusCard, spotColor = Color(0x18001D33))
                            .clip(PastelKineticTokens.RadiusCard)
                            .background(if (isOutflowPodActive) PastelKineticTokens.SkyFixed else PastelKineticTokens.SurfaceLow)
                            .clickable { balancePodFilter = if (balancePodFilter == 2) 0 else 2 }
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
                                    text = if (debitCount > 0) "Debits & Bills" else "No expenses yet",
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
                                    text = "$outflowLimitPct% limit",
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

        // 5. Interactive Budget Goal Tracker Card (Dynamic Top Category)
        item {
            val topCategoryItem = categoryTotals.firstOrNull()
            val topCategoryName = topCategoryItem?.category?.name ?: "General Budget"
            val topCategorySpent = topCategoryItem?.totalSpent ?: totalDebit
            val budgetCap = topCategoryItem?.category?.monthlyBudgetLimit
                ?: (if (topCategoryItem != null) (topCategoryItem.totalSpent * 1.3).coerceAtLeast(1000.0) else monthlyBudget)
            val capRatio = if (budgetCap > 0) (topCategorySpent / budgetCap).toFloat().coerceIn(0f, 1f) else 0f
            val isOverBudget = topCategorySpent > budgetCap

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
                                    imageVector = topCategoryItem?.let { getCategoryIconVector(it.category.id) } ?: Icons.Default.LocalCafe,
                                    contentDescription = topCategoryName,
                                    tint = PastelKineticTokens.AmberText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = topCategoryName,
                                    color = PastelKineticTokens.TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Monthly category quota",
                                    color = PastelKineticTokens.TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(PastelKineticTokens.RadiusFull)
                                .background(if (isOverBudget) PastelKineticTokens.ErrorContainer else PastelKineticTokens.SurfaceLow)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isOverBudget) "Over Quota" else "On Track",
                                color = if (isOverBudget) PastelKineticTokens.Error else PastelKineticTokens.TextPrimary,
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
                                    .fillMaxWidth(fraction = capRatio.coerceAtLeast(0.02f))
                                    .height(8.dp)
                                    .clip(PastelKineticTokens.RadiusFull)
                                    .background(if (isOverBudget) PastelKineticTokens.Error else PastelKineticTokens.AmberAccent)
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
                filterCategories.take(8).forEach { category ->
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
                    text = "Recent Activity (${filteredTransactions.size})",
                    color = PastelKineticTokens.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Reset filters",
                    color = PastelKineticTokens.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable {
                        selectedCategoryFilter = "All Items"
                        selectedDayIndex = null
                        balancePodFilter = 0
                        searchQuery = ""
                    }
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
                        text = "No transactions found matching active filters",
                        color = PastelKineticTokens.TextOutline,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            items(filteredTransactions.take(30), key = { it.id }) { tx ->
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
                                text = categoryMap[tx.categoryId] ?: tx.categoryId.replaceFirstChar { it.titlecase() },
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
