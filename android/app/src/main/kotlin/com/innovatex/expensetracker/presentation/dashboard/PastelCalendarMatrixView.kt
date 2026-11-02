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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Verified
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
import com.innovatex.expensetracker.presentation.theme.PastelKineticTokens
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun PastelCalendarMatrixView(
    currentMonthMillis: Long,
    allTransactions: List<Transaction>,
    currencySymbol: String = "₹",
    onTransactionClick: (Transaction) -> Unit = {},
    onLogForDayClick: () -> Unit = {},
    onExportClick: () -> Unit = {},
    onPreviousMonth: () -> Unit = {},
    onNextMonth: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val numberFormat = remember {
        NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }

    var isRangeMode by remember { mutableStateOf(false) }
    var selectedDayOffset by remember { mutableIntStateOf(3) } // Wednesday active

    val monthHeader = remember(currentMonthMillis) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(currentMonthMillis))
    }

    val cal = remember(currentMonthMillis) {
        Calendar.getInstance().apply {
            timeInMillis = currentMonthMillis
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        }
    }

    val weekDays = remember(currentMonthMillis) {
        val list = mutableListOf<Triple<String, Int, Long>>()
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val tempCal = Calendar.getInstance().apply {
            timeInMillis = currentMonthMillis
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        }
        for (i in 0 until 7) {
            list.add(Triple(dayFormat.format(tempCal.time), tempCal.get(Calendar.DAY_OF_MONTH), tempCal.timeInMillis))
            tempCal.add(Calendar.DAY_OF_MONTH, 1)
        }
        list
    }

    val selectedDayMillis = weekDays.getOrNull(selectedDayOffset)?.third ?: currentMonthMillis
    val selectedDayTitle = remember(selectedDayMillis) {
        SimpleDateFormat("EEEE, dd MMM", Locale.getDefault()).format(Date(selectedDayMillis))
    }

    // Filter day's transactions
    val dayTransactions = remember(allTransactions, selectedDayMillis) {
        val dayStart = Calendar.getInstance().apply {
            timeInMillis = selectedDayMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val dayEnd = dayStart + 86400000L

        allTransactions.filter { it.timestamp in dayStart until dayEnd }
    }

    val dayDebits = dayTransactions.filter { it.type == TransactionType.DEBIT }
    val dayDebitTotal = dayDebits.sumOf { it.amount }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PastelKineticTokens.CanvasBg),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. SpendWell Header
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
                            imageVector = Icons.Default.CalendarMonth,
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
                            text = "Calendar Insights",
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
                            .background(PastelKineticTokens.SurfaceContainer),
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

        // 2. Mode Selector: Single Day vs Date Range
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(PastelKineticTokens.RadiusFull)
                    .background(PastelKineticTokens.SurfaceContainer)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(PastelKineticTokens.RadiusFull)
                        .background(if (!isRangeMode) PastelKineticTokens.SurfaceCard else Color.Transparent)
                        .clickable { isRangeMode = false }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Single Day",
                            tint = if (!isRangeMode) PastelKineticTokens.AnchorPrimary else PastelKineticTokens.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Single Day",
                            color = if (!isRangeMode) PastelKineticTokens.TextPrimary else PastelKineticTokens.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(PastelKineticTokens.RadiusFull)
                        .background(if (isRangeMode) PastelKineticTokens.SurfaceCard else Color.Transparent)
                        .clickable { isRangeMode = true }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Date Range",
                            tint = if (isRangeMode) PastelKineticTokens.AnchorPrimary else PastelKineticTokens.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Date Range",
                            color = if (isRangeMode) PastelKineticTokens.TextPrimary else PastelKineticTokens.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. Month Navigation Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "EXPENSE MATRIX",
                        color = PastelKineticTokens.TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = monthHeader,
                            color = PastelKineticTokens.TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .clip(PastelKineticTokens.RadiusFull)
                                .background(PastelKineticTokens.AmberFixed)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Week 43",
                                color = PastelKineticTokens.AmberText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PastelKineticTokens.SurfaceContainer)
                            .clickable { onPreviousMonth() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous Month",
                            tint = PastelKineticTokens.TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PastelKineticTokens.SurfaceContainer)
                            .clickable { onNextMonth() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next Month",
                            tint = PastelKineticTokens.TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 4. Weekly Horizontal Date Strip with Indicator Dots
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekDays.forEachIndexed { index, triple ->
                    val isActive = selectedDayOffset == index
                    Column(
                        modifier = Modifier
                            .width(46.dp)
                            .height(76.dp)
                            .shadow(
                                elevation = if (isActive) 6.dp else 0.dp,
                                shape = PastelKineticTokens.RadiusLg,
                                spotColor = Color(0x20121316)
                            )
                            .clip(PastelKineticTokens.RadiusLg)
                            .background(
                                if (isActive) PastelKineticTokens.AnchorPrimary else PastelKineticTokens.SurfaceLow
                            )
                            .clickable { selectedDayOffset = index }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    when (index % 3) {
                                        0 -> PastelKineticTokens.SkyAccent
                                        1 -> PastelKineticTokens.AmberAccent
                                        else -> PastelKineticTokens.SurfaceDim
                                    }
                                )
                        )
                        Text(
                            text = triple.first,
                            color = if (isActive) PastelKineticTokens.SurfaceHighest else PastelKineticTokens.TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${triple.second}",
                            color = if (isActive) PastelKineticTokens.OnAnchor else PastelKineticTokens.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 5. Legend Strip
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(PastelKineticTokens.RadiusFull)
                    .background(PastelKineticTokens.SurfaceLow)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(PastelKineticTokens.SkyAccent))
                    Text("Inflow", color = PastelKineticTokens.TextSecondary, fontSize = 11.sp)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(PastelKineticTokens.AmberAccent))
                    Text("Outflow", color = PastelKineticTokens.TextSecondary, fontSize = 11.sp)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(PastelKineticTokens.SurfaceDim))
                    Text("Zero Spend", color = PastelKineticTokens.TextSecondary, fontSize = 11.sp)
                }
            }
        }

        // 6. Daily Summary Warm Amber Hero Pod
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, PastelKineticTokens.RadiusCard, spotColor = Color(0x18825500))
                    .clip(PastelKineticTokens.RadiusCard)
                    .background(PastelKineticTokens.AmberAccent)
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(PastelKineticTokens.RadiusFull)
                                .background(PastelKineticTokens.SurfaceCard.copy(alpha = 0.65f))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = selectedDayTitle,
                                color = PastelKineticTokens.AmberText,
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
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Safe",
                                    tint = PastelKineticTokens.AmberFixed,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Safe Buffer +$currencySymbol 42",
                                    color = PastelKineticTokens.OnAnchor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Column {
                        Text(
                            text = "TOTAL NET OUTFLOW",
                            color = PastelKineticTokens.AmberTextVariant,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "-$currencySymbol${numberFormat.format(dayDebitTotal)}",
                                color = PastelKineticTokens.AmberText,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.6).sp
                            )
                            Text(
                                text = "across ${dayDebits.size} tabs",
                                color = PastelKineticTokens.AmberTextVariant,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                    }

                    // Category Ratio Bar
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Food (65%)", color = PastelKineticTokens.AmberText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("Treats (20%)", color = PastelKineticTokens.AmberText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("Groceries (15%)", color = PastelKineticTokens.AmberText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(PastelKineticTokens.RadiusFull)
                                .background(PastelKineticTokens.SurfaceCard.copy(alpha = 0.5f))
                                .padding(1.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Box(modifier = Modifier.weight(0.65f).height(6.dp).clip(PastelKineticTokens.RadiusFull).background(PastelKineticTokens.AnchorPrimary))
                            Box(modifier = Modifier.weight(0.20f).height(6.dp).clip(PastelKineticTokens.RadiusFull).background(PastelKineticTokens.SkyDeep))
                            Box(modifier = Modifier.weight(0.15f).height(6.dp).clip(PastelKineticTokens.RadiusFull).background(PastelKineticTokens.SurfaceCard))
                        }
                    }
                }
            }
        }

        // 7. Bento Quick Glance Pods (Daily Cap & Pacing)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sky Glaze Pod: Daily Cap
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(PastelKineticTokens.RadiusCard)
                        .background(PastelKineticTokens.SkyFixed)
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(PastelKineticTokens.RadiusFull)
                                    .background(PastelKineticTokens.SurfaceCard.copy(alpha = 0.7f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Daily Cap", color = PastelKineticTokens.SkyText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Icon(imageVector = Icons.Default.Speed, contentDescription = "Cap", tint = PastelKineticTokens.SkyText, modifier = Modifier.size(16.dp))
                        }
                        Column {
                            Text("$currencySymbol 120.00", color = PastelKineticTokens.SkyText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("65% consumed", color = PastelKineticTokens.SkyTextVariant, fontSize = 11.sp)
                        }
                        Box(
                            modifier = Modifier.fillMaxWidth().height(5.dp).clip(PastelKineticTokens.RadiusFull).background(PastelKineticTokens.SurfaceCard.copy(alpha = 0.5f))
                        ) {
                            Box(modifier = Modifier.fillMaxWidth(0.65f).height(5.dp).clip(PastelKineticTokens.RadiusFull).background(PastelKineticTokens.SkyText))
                        }
                    }
                }

                // Lilac Glaze Pod: Pacing
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(PastelKineticTokens.RadiusCard)
                        .background(PastelKineticTokens.SurfaceHigh)
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(PastelKineticTokens.RadiusFull)
                                    .background(PastelKineticTokens.SurfaceCard)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Pacing", color = PastelKineticTokens.TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Icon(imageVector = Icons.Default.SentimentSatisfied, contentDescription = "Gentle", tint = PastelKineticTokens.AmberDeep, modifier = Modifier.size(16.dp))
                        }
                        Column {
                            Text("Gentle", color = PastelKineticTokens.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("+$currencySymbol 14 under curve", color = PastelKineticTokens.TextSecondary, fontSize = 11.sp)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(PastelKineticTokens.AmberDeep))
                            Text("Target met", color = PastelKineticTokens.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // 8. Time Logs Section Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Time Logs", color = PastelKineticTokens.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("${dayTransactions.size} events", color = PastelKineticTokens.TextSecondary, fontSize = 11.sp)
            }
        }

        // 9. Time Log Transaction Items
        if (dayTransactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(PastelKineticTokens.RadiusMd)
                        .background(PastelKineticTokens.SurfaceCard)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No transactions logged for this date.", color = PastelKineticTokens.TextOutline, fontSize = 13.sp)
                }
            }
        } else {
            items(dayTransactions, key = { it.id }) { tx ->
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
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (isCredit) PastelKineticTokens.AmberFixed else PastelKineticTokens.SkyFixed),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isCredit) Icons.Default.ReceiptLong else Icons.Default.LunchDining,
                                    contentDescription = tx.category,
                                    tint = if (isCredit) PastelKineticTokens.AmberDeep else PastelKineticTokens.SkyText,
                                    modifier = Modifier.size(18.dp)
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
                                    Text(text = timeString, color = PastelKineticTokens.TextSecondary, fontSize = 11.sp)
                                    Box(
                                        modifier = Modifier
                                            .clip(PastelKineticTokens.RadiusFull)
                                            .background(PastelKineticTokens.SurfaceContainer)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(text = tx.category, color = PastelKineticTokens.TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
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
                                text = tx.channel.name,
                                color = PastelKineticTokens.TextOutline,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        // 10. Bottom Actions: Log for this Day + Export
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(PastelKineticTokens.RadiusFull)
                        .background(PastelKineticTokens.AnchorPrimary)
                        .clickable { onLogForDayClick() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Log", tint = PastelKineticTokens.OnAnchor, modifier = Modifier.size(16.dp))
                        Text("Log for this Day", color = PastelKineticTokens.OnAnchor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(PastelKineticTokens.RadiusFull)
                        .background(PastelKineticTokens.SurfaceContainer)
                        .clickable { onExportClick() }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = "Export", tint = PastelKineticTokens.TextPrimary, modifier = Modifier.size(16.dp))
                        Text("Export", color = PastelKineticTokens.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

