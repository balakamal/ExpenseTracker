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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innovatex.spendwell.ai.AiRefinementResult
import com.innovatex.spendwell.ai.SpendwellAiAdvisorEngine
import com.innovatex.spendwell.analytics.FinancialAnalyticsEngine
import com.innovatex.spendwell.analytics.FinancialCycleWindow
import com.innovatex.spendwell.common.Money
import com.innovatex.spendwell.data.preferences.UserPreferencesRepository
import com.innovatex.spendwell.designsystem.components.SpendwellCard
import com.innovatex.spendwell.designsystem.components.SpendwellCardContainer
import com.innovatex.spendwell.designsystem.components.SpendwellEmptyState
import com.innovatex.spendwell.designsystem.components.SpendwellFilterPill
import com.innovatex.spendwell.designsystem.components.SpendwellTopAppBar
import com.innovatex.spendwell.designsystem.components.SpendwellTransactionRow
import com.innovatex.spendwell.designsystem.theme.CardSurfaceWhite
import com.innovatex.spendwell.designsystem.theme.CobaltPrimary
import com.innovatex.spendwell.designsystem.theme.CrimsonTertiary
import com.innovatex.spendwell.designsystem.theme.EmeraldSecondary
import com.innovatex.spendwell.designsystem.theme.InterFontFamily
import com.innovatex.spendwell.designsystem.theme.PillShape
import com.innovatex.spendwell.designsystem.theme.PrimaryFixed
import com.innovatex.spendwell.designsystem.theme.SquircleCardShape
import com.innovatex.spendwell.designsystem.theme.SurfaceContainer
import com.innovatex.spendwell.designsystem.theme.SurfaceContainerHigh
import com.innovatex.spendwell.designsystem.theme.SurfaceContainerLow
import com.innovatex.spendwell.domain.model.FinancialNature
import com.innovatex.spendwell.domain.model.Transaction
import com.innovatex.spendwell.domain.model.TransactionStatus
import com.innovatex.spendwell.domain.model.TransactionType
import com.innovatex.spendwell.domain.repository.TransactionRepository
import com.innovatex.spendwell.integrations.sms.SmsHistoryImporter
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

data class CycleFilterOption(
    val label: String,
    val startTime: Long,
    val endTime: Long,
    val isAllTime: Boolean = false
)

@HiltViewModel
class TransactionFeedViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val smsHistoryImporter: SmsHistoryImporter,
    private val aiAdvisorEngine: SpendwellAiAdvisorEngine
) : ViewModel() {
    val transactions: StateFlow<List<Transaction>> = transactionRepository.observeAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val cycleStartDay: StateFlow<Int> = userPreferencesRepository.cycleStartDay
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 1)

    val syncedMonths: StateFlow<Set<String>> = userPreferencesRepository.syncedMonths
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val isRefiningAi = kotlinx.coroutines.flow.MutableStateFlow(false)
    val aiRefinementResults = kotlinx.coroutines.flow.MutableStateFlow<List<AiRefinementResult>>(emptyList())

    fun saveTransaction(tx: Transaction) {
        viewModelScope.launch {
            transactionRepository.saveTransaction(tx)
        }
    }

    fun runAiCategorization(candidates: List<Transaction>) {
        if (candidates.isEmpty() || isRefiningAi.value) return
        viewModelScope.launch {
            isRefiningAi.value = true
            try {
                val results = aiAdvisorEngine.refineTransactionsWithAi(candidates)
                aiRefinementResults.value = results
            } catch (ignored: Exception) {
            } finally {
                isRefiningAi.value = false
            }
        }
    }

    fun applyRefinements(refinements: List<AiRefinementResult>) {
        viewModelScope.launch {
            refinements.forEach { ref ->
                transactionRepository.updateCategory(ref.transactionId, ref.suggestedCategoryId)
                val tx = transactionRepository.getTransactionById(ref.transactionId)
                if (tx != null && ref.cleanedMerchant.isNotBlank() && ref.cleanedMerchant != tx.merchantName) {
                    transactionRepository.saveTransaction(
                        tx.copy(
                            merchantName = ref.cleanedMerchant,
                            categoryId = ref.suggestedCategoryId,
                            categoryName = ref.suggestedCategoryName
                        )
                    )
                }
            }
            aiRefinementResults.value = emptyList()
        }
    }

    fun dismissRefinements() {
        aiRefinementResults.value = emptyList()
    }

    fun lazyLoadCycleHistory(startTime: Long, endTime: Long) {
        viewModelScope.launch {
            val key = "${startTime}_${endTime}"
            if (syncedMonths.value.contains(key)) return@launch
            try {
                smsHistoryImporter.importHistoryForRange(startTime, endTime)
                userPreferencesRepository.addSyncedMonth(key)
            } catch (ignored: Exception) {}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFeedScreen(
    viewModel: TransactionFeedViewModel,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allTransactions by viewModel.transactions.collectAsState()
    val cycleStartDay by viewModel.cycleStartDay.collectAsState()
    val isRefiningAi by viewModel.isRefiningAi.collectAsState()
    val aiRefinements by viewModel.aiRefinementResults.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }

    val cycleOptions = remember(cycleStartDay) {
        val list = mutableListOf<CycleFilterOption>()
        val cal = Calendar.getInstance()
        for (i in 0..5) {
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH)
            val cycle = FinancialAnalyticsEngine.calculateCycle(y, m, cycleStartDay)
            list.add(CycleFilterOption(cycle.label, cycle.startTime, cycle.endTime, false))
            cal.add(Calendar.MONTH, -1)
        }
        list.add(CycleFilterOption("All Time", 0L, Long.MAX_VALUE, true))
        list
    }

    var selectedCycle by remember(cycleOptions) { mutableStateOf(cycleOptions.first()) }
    var monthMenuExpanded by remember { mutableStateOf(false) }
    var showLogExpenseSheet by remember { mutableStateOf(false) }

    // Pre-filter transactions for the selected month/cycle window
    val cycleFilteredTransactions = remember(allTransactions, selectedCycle) {
        if (selectedCycle.isAllTime) {
            allTransactions
        } else {
            allTransactions.filter { it.timestamp in selectedCycle.startTime..selectedCycle.endTime }
        }
    }

    // Counts for filter pills strictly for the selected cycle
    val debitsCount = cycleFilteredTransactions.count {
        it.type == TransactionType.DEBIT &&
                it.status != TransactionStatus.REFUNDED &&
                it.status != TransactionStatus.REVERSED &&
                it.status != TransactionStatus.FAILED &&
                it.status != TransactionStatus.NEEDS_REVIEW
    }
    val creditsCount = cycleFilteredTransactions.count {
        it.type == TransactionType.CREDIT &&
                it.status != TransactionStatus.REFUNDED &&
                it.status != TransactionStatus.REVERSED &&
                it.status != TransactionStatus.FAILED &&
                it.status != TransactionStatus.NEEDS_REVIEW
    }
    val pendingCount = cycleFilteredTransactions.count {
        it.status == TransactionStatus.NEEDS_REVIEW || it.status == TransactionStatus.PENDING
    }

    // Filtering logic with search query and selected filter tab
    val filteredTransactions = cycleFilteredTransactions.filter { tx ->
        val matchesQuery = searchQuery.isBlank() ||
            tx.merchantName.contains(searchQuery, ignoreCase = true) ||
            tx.description.contains(searchQuery, ignoreCase = true) ||
            (tx.referenceId?.contains(searchQuery, ignoreCase = true) == true) ||
            (tx.utrNumber?.contains(searchQuery, ignoreCase = true) == true) ||
            tx.categoryName.contains(searchQuery, ignoreCase = true) ||
            tx.amount.formatInr(false).contains(searchQuery)

        val matchesFilter = when (selectedFilter) {
            "DEBITS" -> tx.type == TransactionType.DEBIT &&
                    tx.status != TransactionStatus.REFUNDED &&
                    tx.status != TransactionStatus.REVERSED &&
                    tx.status != TransactionStatus.FAILED
            "CREDITS" -> tx.type == TransactionType.CREDIT &&
                    tx.status != TransactionStatus.FAILED &&
                    tx.status != TransactionStatus.REVERSED
            "PENDING" -> tx.status == TransactionStatus.NEEDS_REVIEW || tx.status == TransactionStatus.PENDING
            else -> true
        }
        matchesQuery && matchesFilter
    }

    // Monthly summary calculation strictly from cycleFilteredTransactions
    // Exclude CC Bill payments and internal transfers from consumption outflow to prevent double-counting
    val outflowTotal = cycleFilteredTransactions
        .filter {
            it.type == TransactionType.DEBIT &&
                    it.status != TransactionStatus.REFUNDED &&
                    it.status != TransactionStatus.REVERSED &&
                    it.status != TransactionStatus.FAILED &&
                    it.status != TransactionStatus.NEEDS_REVIEW &&
                    it.financialNature != FinancialNature.CREDIT_CARD_BILL_PAYMENT &&
                    it.financialNature != FinancialNature.INTERNAL_TRANSFER
        }
        .fold(Money.ZERO) { acc, tx -> acc + tx.amount }

    val inflowTotal = cycleFilteredTransactions
        .filter {
            it.type == TransactionType.CREDIT &&
                    it.status != TransactionStatus.REFUNDED &&
                    it.status != TransactionStatus.REVERSED &&
                    it.status != TransactionStatus.FAILED &&
                    it.status != TransactionStatus.NEEDS_REVIEW
        }
        .fold(Money.ZERO) { acc, tx -> acc + tx.amount }

    val netTotal = inflowTotal - outflowTotal

    // Grouping by date
    val groupedTransactions = remember(filteredTransactions) {
        groupTransactionsByDate(filteredTransactions)
    }

    Scaffold(
        topBar = {
            SpendwellTopAppBar(
                title = "Spendwell",
                subtitle = "Spendwell — Transactions Ledger (Light)",
                isPrivate = true,
                onSearchClick = {},
                onProfileClick = onNavigateToSettings
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showLogExpenseSheet = true },
                containerColor = CobaltPrimary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(54.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add transaction manually",
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // 1. Search Bar & Tune Filter Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Search Bar Pill
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(CircleShape)
                            .background(CardSurfaceWhite)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), CircleShape)
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = CobaltPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search merchants, notes, tags",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontFamily = InterFontFamily,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Round Filter Tune Button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(CardSurfaceWhite)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), CircleShape)
                            .clickable {
                                selectedFilter = if (selectedFilter == "ALL") "DEBITS" else "ALL"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Filter Options",
                            tint = CobaltPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // 2. Filter Chips Scrollable Carousel
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Month/Cycle Dropdown Pill
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(CardSurfaceWhite)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), CircleShape)
                                .clickable { monthMenuExpanded = true }
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedCycle.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = monthMenuExpanded,
                            onDismissRequest = { monthMenuExpanded = false }
                        ) {
                            cycleOptions.forEach { cycle ->
                                DropdownMenuItem(
                                    text = { Text(cycle.label) },
                                    onClick = {
                                        selectedCycle = cycle
                                        monthMenuExpanded = false
                                        if (!cycle.isAllTime) {
                                            viewModel.lazyLoadCycleHistory(cycle.startTime, cycle.endTime)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Filter Pills
                    SpendwellFilterPill(
                        label = "All (${cycleFilteredTransactions.size})",
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
                        label = if (isRefiningAi) "✨ Categorizing..." else "✨ AI Categorize",
                        isSelected = false,
                        onClick = {
                            val candidates = allTransactions.filter {
                                it.categoryName.equals("Miscellaneous", ignoreCase = true) ||
                                it.categoryId == "cat_other" ||
                                it.status == TransactionStatus.NEEDS_REVIEW
                            }
                            viewModel.runAiCategorization(if (candidates.isNotEmpty()) candidates else allTransactions.take(15))
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // 3. Month Overview Card (Stitch White Squircle Card)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = SquircleCardShape,
                    colors = CardDefaults.cardColors(containerColor = CardSurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), SquircleCardShape)
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Insights,
                                    contentDescription = null,
                                    tint = CobaltPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${selectedCycle.label.uppercase()} NET CASHFLOW",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                modifier = Modifier
                                .clip(PillShape)
                                .background(SurfaceContainerLow)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = EmeraldSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "${cycleFilteredTransactions.size} records • On-Device",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 3-Column Metrics Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Outflow
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Outflow",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = outflowTotal.formatInr(true),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = CrimsonTertiary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$debitsCount items",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Inflow
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Inflow",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "+${inflowTotal.formatInr(true)}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldSecondary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$creditsCount items",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Net Savings
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Net Savings",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                val netSign = if (!netTotal.isNegative) "+" else ""
                                Text(
                                    text = "$netSign${netTotal.formatInr(true)}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = CobaltPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (!netTotal.isNegative) "Surplus" else "Deficit",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = if (!netTotal.isNegative) EmeraldSecondary else CrimsonTertiary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Mini Progress Ratio Indicator
                        val totalFlow = (inflowTotal + outflowTotal).toPaisa().toDouble()
                        val inflowRatio = if (totalFlow > 0) (inflowTotal.toPaisa().toDouble() / totalFlow).coerceIn(0.1, 0.9) else 0.7
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(SurfaceContainer)
                        ) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(inflowRatio.toFloat())
                                        .fillMaxSize()
                                        .background(EmeraldSecondary)
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize()
                                        .background(CrimsonTertiary)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Filtered strictly for ${selectedCycle.label}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${filteredTransactions.size} shown",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = CobaltPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // 4. Grouped Transactions Section
            if (filteredTransactions.isEmpty()) {
                item {
                    SpendwellCard(modifier = Modifier.fillMaxWidth()) {
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
                }
            } else {
                groupedTransactions.forEach { (dateHeader, txsInGroup) ->
                    item(key = "header_$dateHeader") {
                        val groupOutflow = txsInGroup
                            .filter {
                                it.type == TransactionType.DEBIT &&
                                        it.status != TransactionStatus.REFUNDED &&
                                        it.status != TransactionStatus.REVERSED &&
                                        it.status != TransactionStatus.FAILED &&
                                        it.status != TransactionStatus.NEEDS_REVIEW &&
                                        it.financialNature != FinancialNature.CREDIT_CARD_BILL_PAYMENT &&
                                        it.financialNature != FinancialNature.INTERNAL_TRANSFER
                            }
                            .fold(Money.ZERO) { acc, tx -> acc + tx.amount }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = dateHeader,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(CobaltPrimary)
                                )
                            }
                            Text(
                                text = "${txsInGroup.size} Transactions • ${groupOutflow.formatInr(true)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    item(key = "card_$dateHeader") {
                        SpendwellCardContainer(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                txsInGroup.forEachIndexed { index, tx ->
                                    SpendwellTransactionRow(
                                        transaction = tx,
                                        onClick = { onNavigateToDetail(tx.id) }
                                    )
                                    if (index < txsInGroup.lastIndex) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 14.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                                            thickness = 0.8.dp
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // 5. 90-Day Local Archive Footer Card
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(SquircleCardShape)
                            .background(SurfaceContainerLow)
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceContainer)
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderZip,
                                    contentDescription = null,
                                    tint = CobaltPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "90-Day Local Archive",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Transactions older than 90 days are archived locally in encrypted storage to maintain peak performance and absolute confidentiality.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(CircleShape)
                                    .background(CardSurfaceWhite)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), CircleShape)
                                    .clickable {
                                        Toast.makeText(context, "Archive index ready: 0 offline records", Toast.LENGTH_SHORT).show()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = CobaltPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Search Archive",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(CircleShape)
                                    .background(CardSurfaceWhite)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), CircleShape)
                                    .clickable {
                                        Toast.makeText(context, "Exporting encrypted statement...", Toast.LENGTH_SHORT).show()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        tint = CobaltPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Get Statement",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(48.dp))
                }
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

    if (aiRefinements.isNotEmpty()) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissRefinements() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = CardSurfaceWhite,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = CobaltPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AI Transaction Accuracy",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${aiRefinements.size} candidate transactions refined on-device",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { viewModel.dismissRefinements() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(aiRefinements.size) { idx ->
                        val ref = aiRefinements[idx]
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = ref.cleanedMerchant,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (ref.originalMerchant.isNotBlank() && ref.originalMerchant != ref.cleanedMerchant) {
                                            Text(
                                                text = "Raw: ${ref.originalMerchant}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(PillShape)
                                            .background(EmeraldSecondary.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "${(ref.confidence * 100).toInt()}% Match",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldSecondary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Category: ",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = ref.suggestedCategoryName,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = CobaltPrimary
                                    )
                                }

                                if (ref.tags.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ref.tags.forEach { tag ->
                                            Text(
                                                text = tag,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier
                                                    .clip(PillShape)
                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.dismissRefinements() },
                        colors = ButtonDefaults.outlinedButtonColors(),
                        modifier = Modifier.weight(1f),
                        shape = PillShape
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            viewModel.applyRefinements(aiRefinements)
                            Toast.makeText(context, "Applied AI refinements to ${aiRefinements.size} transactions", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CobaltPrimary),
                        modifier = Modifier.weight(2f),
                        shape = PillShape
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Apply All (${aiRefinements.size})",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
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
