package com.innovatex.spendwell.features.transactions

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innovatex.spendwell.common.Money
import com.innovatex.spendwell.designsystem.components.SpendwellEmptyState
import com.innovatex.spendwell.designsystem.components.SpendwellFilterPill
import com.innovatex.spendwell.designsystem.components.SpendwellTransactionRow
import com.innovatex.spendwell.designsystem.theme.ForestEmerald
import com.innovatex.spendwell.designsystem.theme.InflowGreen
import com.innovatex.spendwell.designsystem.theme.OutflowRed
import com.innovatex.spendwell.designsystem.theme.PillShape
import com.innovatex.spendwell.designsystem.theme.VividMint
import com.innovatex.spendwell.domain.model.FinancialNature
import com.innovatex.spendwell.domain.model.Transaction
import com.innovatex.spendwell.domain.model.TransactionStatus
import com.innovatex.spendwell.domain.model.TransactionType
import com.innovatex.spendwell.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class TransactionFeedViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {
    val transactions: StateFlow<List<Transaction>> = transactionRepository.observeAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveTransaction(tx: Transaction) {
        viewModelScope.launch {
            transactionRepository.saveTransaction(tx)
        }
    }
}

@Composable
fun TransactionFeedScreen(
    viewModel: TransactionFeedViewModel,
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allTransactions by viewModel.transactions.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }
    val monthOptions = remember {
        val formatter = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH)
        val list = mutableListOf<String>()
        val c = Calendar.getInstance()
        for (i in 0..5) {
            list.add(formatter.format(c.time))
            c.add(Calendar.MONTH, -1)
        }
        list.add("All Time")
        list
    }
    var selectedMonth by remember { mutableStateOf(monthOptions.first()) }
    var monthMenuExpanded by remember { mutableStateOf(false) }
    var isVoiceListening by remember { mutableStateOf(false) }
    var showLogExpenseSheet by remember { mutableStateOf(false) }

    // Counts for filter pills
    val debitsCount = allTransactions.count { it.type == TransactionType.DEBIT && it.status != TransactionStatus.REFUNDED && it.status != TransactionStatus.REVERSED && it.status != TransactionStatus.FAILED }
    val creditsCount = allTransactions.count { it.type == TransactionType.CREDIT }
    val pendingCount = allTransactions.count { it.status == TransactionStatus.NEEDS_REVIEW || it.status == TransactionStatus.PENDING }
    val refundsCount = allTransactions.count { it.status == TransactionStatus.REFUNDED || it.status == TransactionStatus.REVERSED || it.categoryName.contains("Refund", ignoreCase = true) }
    val flaggedCount = allTransactions.count { !it.isReviewed || it.status == TransactionStatus.NEEDS_REVIEW }

    // Filtering logic
    val filteredTransactions = allTransactions.filter { tx ->
        val matchesQuery = searchQuery.isBlank() ||
            tx.merchantName.contains(searchQuery, ignoreCase = true) ||
            tx.description.contains(searchQuery, ignoreCase = true) ||
            (tx.referenceId?.contains(searchQuery, ignoreCase = true) == true) ||
            (tx.utrNumber?.contains(searchQuery, ignoreCase = true) == true) ||
            tx.categoryName.contains(searchQuery, ignoreCase = true) ||
            tx.amount.formatInr(false).contains(searchQuery)

        val matchesFilter = when (selectedFilter) {
            "DEBITS" -> tx.type == TransactionType.DEBIT && tx.status != TransactionStatus.REFUNDED && tx.status != TransactionStatus.REVERSED && tx.status != TransactionStatus.FAILED
            "CREDITS" -> tx.type == TransactionType.CREDIT
            "PENDING" -> tx.status == TransactionStatus.NEEDS_REVIEW || tx.status == TransactionStatus.PENDING
            "REFUNDS" -> tx.status == TransactionStatus.REFUNDED || tx.status == TransactionStatus.REVERSED || tx.categoryName.contains("Refund", ignoreCase = true)
            "FLAGGED" -> !tx.isReviewed || tx.status == TransactionStatus.NEEDS_REVIEW
            else -> true
        }
        val matchesMonth = if (selectedMonth == "All Time") true else {
            val txDate = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(Date(tx.timestamp))
            txDate == selectedMonth
        }
        matchesQuery && matchesFilter && matchesMonth
    }

    // Monthly summary calculation
    val outflowTotal = allTransactions
        .filter { it.type == TransactionType.DEBIT && it.status != TransactionStatus.REFUNDED && it.status != TransactionStatus.REVERSED && it.status != TransactionStatus.FAILED }
        .fold(Money.ZERO) { acc, tx -> acc + tx.amount }

    val inflowTotal = allTransactions
        .filter { it.type == TransactionType.CREDIT }
        .fold(Money.ZERO) { acc, tx -> acc + tx.amount }

    val netTotal = inflowTotal - outflowTotal

    // Grouping by date
    val groupedTransactions = remember(filteredTransactions) {
        groupTransactionsByDate(filteredTransactions)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            // 1. Top Header Area (Matching Stitch)
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Spendwell",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Spendwell — Transaction Feed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Private Mode Pill Badge
                    Row(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = ForestEmerald,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Private Mode",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 2. Search Bar with Mic & Filter Icons
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            if (isVoiceListening) "Listening for merchant or amount..." else "Search merchants, notes, UPI IDs...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    isVoiceListening = !isVoiceListening
                                    Toast.makeText(
                                        context,
                                        if (isVoiceListening) "Voice listening: Speak merchant or amount..." else "Voice search ended",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice Search",
                                    tint = if (isVoiceListening) ForestEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    selectedFilter = if (selectedFilter == "ALL") "DEBITS" else "ALL"
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Filter",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = PillShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = ForestEmerald
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 3. Date Selector & Horizontal Filter Pills
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Date Range Selector Pill
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(PillShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { monthMenuExpanded = true }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = ForestEmerald,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = selectedMonth,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = monthMenuExpanded,
                            onDismissRequest = { monthMenuExpanded = false }
                        ) {
                            monthOptions.forEach { month ->
                                DropdownMenuItem(
                                    text = { Text(month) },
                                    onClick = {
                                        selectedMonth = month
                                        monthMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .height(18.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    // Filter Pills
                    SpendwellFilterPill(
                        label = "All (${allTransactions.size})",
                        isSelected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" }
                    )
                    SpendwellFilterPill(
                        label = "Debits ($debitsCount)",
                        isSelected = selectedFilter == "DEBITS",
                        onClick = { selectedFilter = "DEBITS" }
                    )
                    SpendwellFilterPill(
                        label = "Credits ($creditsCount)",
                        isSelected = selectedFilter == "CREDITS",
                        onClick = { selectedFilter = "CREDITS" }
                    )
                    SpendwellFilterPill(
                        label = "Pending ($pendingCount)",
                        isSelected = selectedFilter == "PENDING",
                        onClick = { selectedFilter = "PENDING" }
                    )
                    SpendwellFilterPill(
                        label = "Refunds ($refundsCount)",
                        isSelected = selectedFilter == "REFUNDS",
                        onClick = { selectedFilter = "REFUNDS" }
                    )
                    SpendwellFilterPill(
                        label = "Flagged / AI ($flaggedCount)",
                        isSelected = selectedFilter == "FLAGGED",
                        onClick = { selectedFilter = "FLAGGED" }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 4. Spending Summary Mini-Banner (Month Overview)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "MONTH OVERVIEW",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(PillShape)
                                        .background(ForestEmerald.copy(alpha = 0.15f))
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Verified",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = ForestEmerald
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = ForestEmerald,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "${allTransactions.size} parsed via SMS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ForestEmerald
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Outflow
                            Column {
                                Text(
                                    text = "Outflow",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = outflowTotal.formatInr(true),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .height(24.dp)
                                    .width(1.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            )

                            // Inflow
                            Column {
                                Text(
                                    text = "Inflow",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "+ ${inflowTotal.formatInr(true)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = InflowGreen
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .height(24.dp)
                                    .width(1.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            )

                            // Net
                            Column {
                                Text(
                                    text = "Net",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                val netSign = if (!netTotal.isNegative) "+ " else ""
                                Text(
                                    text = "$netSign${netTotal.formatInr(true)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!netTotal.isNegative) InflowGreen else OutflowRed
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 5. Grouped Transactions Section
            if (filteredTransactions.isEmpty()) {
                item {
                    SpendwellEmptyState(
                        title = if (allTransactions.isEmpty()) "Ledger is Ready" else "No matching transactions",
                        description = when {
                            allTransactions.isEmpty() -> "Transactions parsed on-device from SMS and payment notifications will appear here automatically."
                            searchQuery.isNotBlank() -> "No transactions match \"$searchQuery\". Try clearing your search."
                            else -> "No transactions found under the \"$selectedFilter\" filter."
                        },
                        actionText = if (searchQuery.isNotBlank() || selectedFilter != "ALL") "Reset Filters" else null,
                        onActionClick = {
                            searchQuery = ""
                            selectedFilter = "ALL"
                        }
                    )
                }
            } else {
                groupedTransactions.forEach { (dateHeader, txsInGroup) ->
                    item(key = "header_$dateHeader") {
                        val groupOutflow = txsInGroup
                            .filter { it.type == TransactionType.DEBIT && it.status != TransactionStatus.REFUNDED && it.status != TransactionStatus.REVERSED && it.status != TransactionStatus.FAILED }
                            .fold(Money.ZERO) { acc, tx -> acc + tx.amount }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dateHeader,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${txsInGroup.size} Transactions • ${groupOutflow.formatInr(true)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    item(key = "card_$dateHeader") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                txsInGroup.forEachIndexed { index, tx ->
                                    SpendwellTransactionRow(
                                        transaction = tx,
                                        onClick = { onNavigateToDetail(tx.id) }
                                    )
                                    if (index < txsInGroup.lastIndex) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            thickness = 0.8.dp
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // 6. End of Stream Indicator
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockClock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Transactions older than 90 days are archived locally in encrypted storage.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Search Complete Archive",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = ForestEmerald,
                            modifier = Modifier
                                .clip(PillShape)
                                .clickable {
                                    Toast.makeText(context, "Archive index: All transactions already in local encrypted SQLite.", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(88.dp))
                }
            }
        }

        // Floating Fast Action Panel (Statement Download Pill + Add Cash FAB)
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Statement Export Pill
            Row(
                modifier = Modifier
                    .clip(PillShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, PillShape)
                    .clickable {
                        Toast.makeText(context, "Exporting ledger statement (CSV / PDF)...", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    tint = ForestEmerald,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Statement",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // + FAB for Manual Cash
            FloatingActionButton(
                onClick = { showLogExpenseSheet = true },
                containerColor = ForestEmerald,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(54.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add manual expense",
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }

    if (showLogExpenseSheet) {
        LogExpenseSheet(
            onDismiss = { showLogExpenseSheet = false },
            onSaveTransaction = { tx ->
                viewModel.saveTransaction(tx)
                showLogExpenseSheet = false
                Toast.makeText(context, "Cash expense logged successfully", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

private fun groupTransactionsByDate(transactions: List<Transaction>): Map<String, List<Transaction>> {
    val result = LinkedHashMap<String, MutableList<Transaction>>()
    val now = Calendar.getInstance()
    val txCal = Calendar.getInstance()

    val dayFormat = SimpleDateFormat("dd MMM", Locale.ENGLISH)
    val fullDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)

    transactions.forEach { tx ->
        txCal.timeInMillis = tx.timestamp
        val isSameYear = now.get(Calendar.YEAR) == txCal.get(Calendar.YEAR)
        val isToday = isSameYear && now.get(Calendar.DAY_OF_YEAR) == txCal.get(Calendar.DAY_OF_YEAR)
        val isYesterday = isSameYear && (now.get(Calendar.DAY_OF_YEAR) - txCal.get(Calendar.DAY_OF_YEAR) == 1)

        val header = when {
            isToday -> "Today, ${dayFormat.format(Date(tx.timestamp))}"
            isYesterday -> "Yesterday, ${dayFormat.format(Date(tx.timestamp))}"
            else -> fullDateFormat.format(Date(tx.timestamp))
        }

        result.getOrPut(header) { mutableListOf() }.add(tx)
    }
    return result
}
