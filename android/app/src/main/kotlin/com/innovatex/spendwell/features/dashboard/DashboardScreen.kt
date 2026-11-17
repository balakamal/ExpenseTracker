package com.innovatex.spendwell.features.dashboard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.core.content.ContextCompat
import com.innovatex.spendwell.common.Money
import com.innovatex.spendwell.designsystem.components.SpendwellCard
import com.innovatex.spendwell.designsystem.components.SpendwellCardContainer
import com.innovatex.spendwell.designsystem.components.SpendwellEmptyState
import com.innovatex.spendwell.designsystem.components.SpendwellLoadingState
import com.innovatex.spendwell.designsystem.components.SpendwellPrimaryButton
import com.innovatex.spendwell.designsystem.components.SpendwellTopAppBar
import com.innovatex.spendwell.designsystem.theme.CardSurfaceWhite
import com.innovatex.spendwell.designsystem.theme.CrimsonTertiary
import com.innovatex.spendwell.designsystem.theme.EmeraldSecondary
import com.innovatex.spendwell.designsystem.theme.PillShape
import com.innovatex.spendwell.designsystem.theme.SquircleCardShape
import com.innovatex.spendwell.designsystem.theme.SurfaceContainer
import com.innovatex.spendwell.designsystem.theme.SurfaceContainerHigh
import com.innovatex.spendwell.designsystem.theme.SurfaceContainerLow
import com.innovatex.spendwell.domain.model.Account
import com.innovatex.spendwell.domain.model.Transaction
import com.innovatex.spendwell.domain.model.TransactionType
import com.innovatex.spendwell.features.transactions.LogExpenseSheet
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToTransactions: () -> Unit,
    onNavigateToTransactionDetail: (String) -> Unit,
    onNavigateToReviewCenter: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToReconciliation: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPermissions: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var showLogExpenseSheet by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val requiredPermissions = remember {
        buildList {
            add(Manifest.permission.READ_SMS)
            add(Manifest.permission.RECEIVE_SMS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = (results[Manifest.permission.READ_SMS] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) &&
            (results[Manifest.permission.RECEIVE_SMS] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED)
        hasSmsPermission = granted
        if (granted) {
            Toast.makeText(context, "Permissions granted! Reading SMS history...", Toast.LENGTH_SHORT).show()
            viewModel.importSmsHistory(
                onComplete = { result ->
                    Toast.makeText(context, "Ledger synchronized! ${result.imported} transactions imported.", Toast.LENGTH_LONG).show()
                },
                onError = { err ->
                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        if (!hasSmsPermission) {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    fun triggerSmsSync() {
        if (!hasSmsPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_SMS,
                    Manifest.permission.RECEIVE_SMS
                )
            )
        } else {
            viewModel.importSmsHistory(
                onComplete = { result ->
                    Toast.makeText(context, "Sync complete! ${result.imported} new transactions added.", Toast.LENGTH_LONG).show()
                },
                onError = { err ->
                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    if (state.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            SpendwellLoadingState()
        }
        return
    }

    if (state.error != null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            SpendwellCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.errorContainer
            ) {
                Column {
                    Text(
                        text = "Dashboard Notice",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.error ?: "Unable to load dashboard data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            SpendwellTopAppBar(
                title = "Spendwell",
                subtitle = "Overview",
                isPrivate = !state.isBalanceObfuscated,
                userName = state.userName,
                onTogglePrivacy = { viewModel.toggleBalanceObfuscation() },
                onSearchClick = onNavigateToTransactions,
                onProfileClick = onNavigateToSettings
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // 1. Personalized Greeting Section (Reference Design Header)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hello,",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Normal
                        )
                        val displayName = if (state.userName.isNotBlank()) state.userName else "There"
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = (-0.5).sp
                        )
                    }

                    // Notification bell with pending review badge
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(SurfaceContainerLow)
                            .clickable { onNavigateToReviewCenter() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsNone,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                        if (state.pendingReviewCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 8.dp, end = 8.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(CrimsonTertiary)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // 2. Permission Setup Banner (if SMS permissions missing)
            if (!hasSmsPermission) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sms,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Enable Offline SMS Tracking",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Detect bank debits and credits automatically.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            SpendwellPrimaryButton(
                                text = "Grant",
                                onClick = { permissionLauncher.launch(requiredPermissions) },
                                modifier = Modifier.width(76.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // 3. "My Cards" Horizontal Carousel (Reference Design: Screen 1)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Cards",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "See all",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onNavigateToAccounts() }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                val accountsList = state.accounts
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (accountsList.isNotEmpty()) {
                        itemsIndexed(accountsList) { index, account ->
                            PaymentCardItem(
                                cardIndex = index,
                                bankName = account.bankName,
                                last4 = account.accountNumberLast4,
                                balance = account.currentBalance,
                                isObfuscated = state.isBalanceObfuscated,
                                onTogglePrivacy = { viewModel.toggleBalanceObfuscation() },
                                onClick = onNavigateToAccounts
                            )
                        }
                    } else {
                        // Default primary card with net balance
                        item {
                            PaymentCardItem(
                                cardIndex = 0,
                                bankName = "Spendwell Reserve",
                                last4 = "0001",
                                balance = state.netBalance,
                                isObfuscated = state.isBalanceObfuscated,
                                onTogglePrivacy = { viewModel.toggleBalanceObfuscation() },
                                onClick = onNavigateToAccounts
                            )
                        }
                    }

                    // Add Card / Account Button Item
                    item {
                        AddCardItem(onClick = onNavigateToAccounts)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }

            // 4. Quick Action Grid (Reference Design: 4 Squircle Pills)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Action 1: Transfer / Log Expense
                    QuickActionButton(
                        icon = Icons.Default.ArrowOutward,
                        label = "Transfer",
                        onClick = { showLogExpenseSheet = true },
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        iconTint = MaterialTheme.colorScheme.primary
                    )

                    // Action 2: Top Up / Add Income
                    QuickActionButton(
                        icon = Icons.Default.AddCircle,
                        label = "Top Up",
                        onClick = { showLogExpenseSheet = true },
                        modifier = Modifier.weight(1f)
                    )

                    // Action 3: Pay Bills / Reconcile
                    QuickActionButton(
                        icon = Icons.Default.Receipt,
                        label = "Pay Bills",
                        onClick = onNavigateToReconciliation,
                        modifier = Modifier.weight(1f)
                    )

                    // Action 4: More / Sync SMS
                    QuickActionButton(
                        icon = if (state.isImporting) Icons.Default.Sync else Icons.Default.MoreHoriz,
                        label = if (state.isImporting) "Syncing" else "More",
                        onClick = { triggerSmsSync() },
                        modifier = Modifier.weight(1f),
                        isLoading = state.isImporting
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))
            }

            // 5. Daily Safe-to-Spend & Cycle Pacing Card
            item {
                val cal = Calendar.getInstance()
                val totalDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val currentDay = cal.get(Calendar.DAY_OF_MONTH)
                val daysRemaining = (totalDaysInMonth - currentDay).coerceAtLeast(1)

                val totalInflow = state.monthlySummary?.totalIncome ?: Money.ZERO
                val totalOutflow = state.monthlySummary?.totalExpenses ?: Money.ZERO
                val spendablePool = if (state.netBalance.isPositive) state.netBalance else totalInflow
                val dailyAllowance = if (daysRemaining > 0 && spendablePool.isPositive) {
                    spendablePool.toPaisa() / daysRemaining
                } else 0L
                val dailyAllowanceMoney = Money.fromPaisa(dailyAllowance)
                val todaySpent = state.dailySummary?.totalExpenses ?: Money.ZERO

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = SquircleCardShape,
                    colors = CardDefaults.cardColors(containerColor = CardSurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                SquircleCardShape
                            )
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "SAFE-TO-SPEND",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            val displayDaily = if (state.isBalanceObfuscated) "₹ •••" else dailyAllowanceMoney.formatInr(false)
                            Text(
                                text = "$displayDaily / day",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$daysRemaining days remaining in cycle",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Today's Spent",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val displayToday = if (state.isBalanceObfuscated) "₹ •••" else todaySpent.formatInr(true)
                            Text(
                                text = displayToday,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (todaySpent.isZero) EmeraldSecondary else CrimsonTertiary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (todaySpent.isZero) "On track" else "Recorded",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = if (todaySpent.isZero) EmeraldSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }

            // 6. "Recent Transactions" Section (Reference Design: Clean Items with Glyphs & Time)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "See all",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onNavigateToTransactions() }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            if (state.recentTransactions.isEmpty()) {
                item {
                    SpendwellCard(modifier = Modifier.fillMaxWidth()) {
                        SpendwellEmptyState(
                            title = "No Transactions Found",
                            description = "Transactions logged manually or imported from SMS will appear here.",
                            actionText = "Sync Bank SMS",
                            onActionClick = { triggerSmsSync() },
                            icon = Icons.Default.ReceiptLong
                        )
                    }
                }
            } else {
                item {
                    SpendwellCardContainer(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            state.recentTransactions.take(5).forEachIndexed { index, tx ->
                                CleanTransactionItem(
                                    transaction = tx,
                                    onClick = { onNavigateToTransactionDetail(tx.id) }
                                )
                                if (index < 4 && index < state.recentTransactions.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 14.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                                        thickness = 0.8.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showLogExpenseSheet) {
        LogExpenseSheet(
            onDismiss = { showLogExpenseSheet = false },
            onSaveTransaction = { newTx ->
                viewModel.saveTransaction(newTx)
                showLogExpenseSheet = false
            }
        )
    }
}

// -------------------------------------------------------------
// COMPONENT 1: PAYMENT CARD ITEM (Horizontal Carousel)
// -------------------------------------------------------------
@Composable
private fun PaymentCardItem(
    cardIndex: Int,
    bankName: String,
    last4: String,
    balance: Money,
    isObfuscated: Boolean,
    onTogglePrivacy: () -> Unit,
    onClick: () -> Unit
) {
    // Alternating card colors matching the reference design: Card 0 = sleek dark, Card 1 = lime accent / emerald
    val isPrimary = cardIndex == 0
    val cardBg = if (isPrimary) Color(0xFF131722) else Color(0xFFE2F87A)
    val textColor = if (isPrimary) Color.White else Color(0xFF131722)
    val subtitleColor = if (isPrimary) Color(0xFF94A3B8) else Color(0xFF475569)

    Card(
        modifier = Modifier
            .width(260.dp)
            .height(155.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Brand & Chip Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = bankName.ifBlank { "Spendwell" }.take(14),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )

                // Contactless / Card Glyph
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isPrimary) Color(0xFF1E293B) else Color(0xFF131722).copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Middle: Masked Number
            Text(
                text = "•••• $last4",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = subtitleColor,
                letterSpacing = 2.sp
            )

            // Bottom: Balance Label & Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Balance",
                        style = MaterialTheme.typography.labelSmall,
                        color = subtitleColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isObfuscated) "₹ ••••••" else balance.formatInr(true),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        letterSpacing = (-0.5).sp
                    )
                }

                IconButton(
                    onClick = onTogglePrivacy,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isObfuscated) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle Balance",
                        tint = subtitleColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// COMPONENT 2: ADD CARD ITEM (At Carousel End)
// -------------------------------------------------------------
@Composable
private fun AddCardItem(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .width(130.dp)
            .height(155.dp)
            .clip(RoundedCornerShape(22.dp))
            .border(
                width = 1.2.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(22.dp)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        color = CardSurfaceWhite
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SurfaceContainerLow),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Account",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Add Card",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// -------------------------------------------------------------
// COMPONENT 3: QUICK ACTION BUTTON (Squircle Pill)
// -------------------------------------------------------------
@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = SurfaceContainerLow,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
    isLoading: Boolean = false
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    RoundedCornerShape(18.dp)
                )
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(containerColor),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

// -------------------------------------------------------------
// COMPONENT 4: CLEAN TRANSACTION ITEM (Reference Design: Screen 1)
// -------------------------------------------------------------
@Composable
private fun CleanTransactionItem(
    transaction: Transaction,
    onClick: () -> Unit
) {
    val isCredit = transaction.type == TransactionType.CREDIT

    val (glyphIcon, glyphBg, glyphTint) = when (transaction.categoryId) {
        "cat_food" -> Triple(Icons.Default.Restaurant, Color(0xFFFEE2E2), Color(0xFFDC2626))
        "cat_groceries" -> Triple(Icons.Default.ShoppingCart, Color(0xFFE0E7FF), Color(0xFF4338CA))
        "cat_salary" -> Triple(Icons.Default.Payments, Color(0xFFD1FAE5), Color(0xFF059669))
        "cat_shopping" -> Triple(Icons.Default.ShoppingBag, Color(0xFFF3E8FF), Color(0xFF7E22CE))
        "cat_transport" -> Triple(Icons.Default.DirectionsCar, Color(0xFFFEF3C7), Color(0xFFD97706))
        "cat_entertainment" -> Triple(Icons.Default.SmartDisplay, Color(0xFFFFEDD5), Color(0xFFC2410C))
        "cat_health" -> Triple(Icons.Default.FitnessCenter, Color(0xFFCCFBF1), Color(0xFF0F766E))
        else -> Triple(Icons.Default.ReceiptLong, SurfaceContainerLow, MaterialTheme.colorScheme.primary)
    }

    val isToday = android.text.format.DateUtils.isToday(transaction.timestamp)
    val isYesterday = android.text.format.DateUtils.isToday(transaction.timestamp + 86400_000L)
    val timeFormat = SimpleDateFormat("h:mm a", Locale.ENGLISH)
    val dateFormat = SimpleDateFormat("dd MMM, yyyy", Locale.ENGLISH)
    val dateLabel = when {
        isToday -> "Today"
        isYesterday -> "Yesterday"
        else -> dateFormat.format(Date(transaction.timestamp))
    }
    val timeLabel = timeFormat.format(Date(transaction.timestamp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Leading Rounded Glyph Container (42x42)
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(glyphBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = glyphIcon,
                contentDescription = null,
                tint = glyphTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Center Title & Subtitle
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.merchantName.ifBlank { transaction.description },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$dateLabel • ${transaction.categoryName}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Right-aligned Amount & Time
        Column(horizontalAlignment = Alignment.End) {
            val sign = if (isCredit) "+" else "−"
            val amountColor = if (isCredit) Color(0xFF059669) else MaterialTheme.colorScheme.onSurface
            Text(
                text = "$sign${transaction.amount.formatInr(true)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = timeLabel,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
