package com.innovatex.expensetracker.presentation.dashboard

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovatex.expensetracker.domain.model.AppUpdateInfo
import com.innovatex.expensetracker.domain.model.Category
import com.innovatex.expensetracker.domain.model.SourceChannel
import com.innovatex.expensetracker.domain.model.Transaction
import com.innovatex.expensetracker.domain.model.TransactionType
import com.innovatex.expensetracker.presentation.components.CorbelCalendar
import com.innovatex.expensetracker.presentation.components.CorbelCategoryBreakdown
import com.innovatex.expensetracker.presentation.components.CorbelTransactionTable
import com.innovatex.expensetracker.presentation.components.CorbelTwoSummaryCards
import com.innovatex.expensetracker.presentation.components.SettingsBottomSheet
import com.innovatex.expensetracker.presentation.components.ShimmerCardLoader
import com.innovatex.expensetracker.presentation.components.UpdateAlertBanner
import com.innovatex.expensetracker.presentation.components.UpdateBadgePill
import com.innovatex.expensetracker.presentation.components.UpdateDetailsBottomSheet
import com.innovatex.expensetracker.presentation.theme.ApexThemeTokens
import com.innovatex.expensetracker.presentation.transaction.CategoryPickerBottomSheet
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyDashboardScreen(
    viewModel: DashboardViewModel,
    onSettingsClick: () -> Unit = {},
    onTransactionClick: (Transaction) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    val showUpdateBanner by viewModel.showUpdateBanner.collectAsState()
    val calendarMonthMillis by viewModel.calendarMonthMillis.collectAsState()
    val context = LocalContext.current

    var showSettingsSheet by remember { mutableStateOf(false) }
    var selectedUpdateInfoForReview by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var selectedTransactionForDetail by remember { mutableStateOf<Transaction?>(null) }
    var transactionForCategoryChange by remember { mutableStateOf<Transaction?>(null) }

    val openUrl = { url: String ->
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    // Ping animation for Corbel's "live now" status indicator
    val infiniteTransition = rememberInfiniteTransition(label = "livePing")
    val pingScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pingScale"
    )
    val pingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pingAlpha"
    )

    Box(modifier = Modifier.fillMaxSize().background(ApexThemeTokens.BgCanvas)) {
        // Corbel Ambient Background Mesh (Soft radial glow spots)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Top-left Ocean Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ApexThemeTokens.MeshA, Color.Transparent),
                    center = Offset(width * 0.15f, height * 0.08f),
                    radius = width * 0.75f
                ),
                radius = width * 0.75f,
                center = Offset(width * 0.15f, height * 0.08f)
            )

            // Top-right Coral Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ApexThemeTokens.MeshC, Color.Transparent),
                    center = Offset(width * 0.9f, height * 0.25f),
                    radius = width * 0.6f
                ),
                radius = width * 0.6f,
                center = Offset(width * 0.9f, height * 0.25f)
            )

            // Bottom-right Mint Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ApexThemeTokens.MeshB, Color.Transparent),
                    center = Offset(width * 0.8f, height * 0.85f),
                    radius = width * 0.7f
                ),
                radius = width * 0.7f,
                center = Offset(width * 0.8f, height * 0.85f)
            )
        }

        Scaffold(
            containerColor = Color.Transparent,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { viewModel.triggerHistoricalScan(context) },
                    containerColor = ApexThemeTokens.AccentPrimary,
                    contentColor = ApexThemeTokens.TextHighContrast,
                    shape = CircleShape,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Scan SMS",
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .statusBarsPadding(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Corbel App Bar: Brand mark, Title, Live Status Pill & Settings
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Brand Icon Mark
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(ApexThemeTokens.RadiusSm)
                                .background(ApexThemeTokens.BgHighContrast),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "C",
                                color = ApexThemeTokens.TextHighContrast,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "CORBEL EXPENSE",
                                color = ApexThemeTokens.TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.3).sp
                            )
                            Text(
                                text = "On-device ledger • Live sync",
                                color = ApexThemeTokens.TextMuted,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Corbel "live now" indicator pill
                        Box(
                            modifier = Modifier
                                .clip(ApexThemeTokens.RadiusFull)
                                .background(ApexThemeTokens.AccentPrimaryTint)
                                .border(1.dp, ApexThemeTokens.AccentPrimaryBorder, ApexThemeTokens.RadiusFull)
                                .padding(horizontal = 9.dp, vertical = 5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Animated ping ring
                                    Canvas(modifier = Modifier.size(14.dp)) {
                                        drawCircle(
                                            color = ApexThemeTokens.AccentPrimary.copy(alpha = pingAlpha),
                                            radius = (size.minDimension / 2) * pingScale
                                        )
                                        drawCircle(
                                            color = ApexThemeTokens.AccentPrimary,
                                            radius = 3.5.dp.toPx()
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "live",
                                    color = ApexThemeTokens.AccentPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // GitHub Update Badge Pill (shows when a new version is available)
                        if (updateState is UpdateCheckState.Available) {
                            UpdateBadgePill(
                                latestVersion = (updateState as UpdateCheckState.Available).updateInfo.latestVersion,
                                onClick = {
                                    selectedUpdateInfoForReview = (updateState as UpdateCheckState.Available).updateInfo
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        // Settings Button in glass circle
                        IconButton(
                            onClick = { showSettingsSheet = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ApexThemeTokens.BgGlassPill)
                                .border(1.dp, ApexThemeTokens.StrokeSubtle, CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = ApexThemeTokens.TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // GitHub Update Alert Banner
                if (updateState is UpdateCheckState.Available && showUpdateBanner) {
                    item {
                        UpdateAlertBanner(
                            updateInfo = (updateState as UpdateCheckState.Available).updateInfo,
                            onUpdateClick = {
                                selectedUpdateInfoForReview = (updateState as UpdateCheckState.Available).updateInfo
                            },
                            onDismissClick = {
                                viewModel.dismissUpdateBanner()
                            }
                        )
                    }
                }

                // 1. Corbel Interactive Financial Calendar
                item {
                    CorbelCalendar(
                        currentMonthMillis = calendarMonthMillis,
                        selectedDateMillis = uiState.selectedDateMillis,
                        allTransactions = uiState.allTransactions,
                        onDateSelected = { timestamp -> viewModel.selectDate(timestamp) },
                        onClearDateSelection = { viewModel.clearDateFilter() },
                        onPreviousMonth = { viewModel.previousCalendarMonth() },
                        onNextMonth = { viewModel.nextCalendarMonth() }
                    )
                }

                // 2. Two Cards: Credited Amount & Debited Amount
                item {
                    if (uiState.isLoading) {
                        ShimmerCardLoader(height = 130)
                    } else {
                        CorbelTwoSummaryCards(
                            totalCredit = uiState.totalCredit,
                            totalDebit = uiState.totalDebit,
                            creditCount = uiState.creditTransactionCount,
                            debitCount = uiState.debitTransactionCount,
                            selectedDateDisplay = uiState.selectedDateDisplay,
                            isOverallView = uiState.isOverallView
                        )
                    }
                }

                // 3. Category Breakdown (Horizontal progress cards)
                item {
                    CorbelCategoryBreakdown(
                        categoryTotals = uiState.categoryTotals,
                        isOverallView = uiState.isOverallView
                    )
                }

                // 4. Corbel Transaction Table (Detailed ledger)
                item {
                    CorbelTransactionTable(
                        transactions = uiState.transactions,
                        categoryTotals = uiState.categoryTotals,
                        onTransactionClick = { txn ->
                            selectedTransactionForDetail = txn
                        }
                    )
                }
            }
        }
    }

    // Transaction Details Bottom Sheet
    selectedTransactionForDetail?.let { txn ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val dateFormatter = remember { SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault()) }
        val numberFormat = remember { NumberFormat.getNumberInstance(Locale("en", "IN")).apply { minimumFractionDigits = 2; maximumFractionDigits = 2 } }
        val isCredit = txn.type == TransactionType.CREDIT
        val category = uiState.categoryTotals.find { it.category.id == txn.categoryId }?.category

        ModalBottomSheet(
            onDismissRequest = { selectedTransactionForDetail = null },
            sheetState = sheetState,
            containerColor = ApexThemeTokens.BgSurfaceElevated,
            shape = ApexThemeTokens.RadiusSheet
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 14.dp)
            ) {
                // Handle bar
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(ApexThemeTokens.StrokeHighlight)
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(
                            text = "TRANSACTION DETAILS",
                            color = ApexThemeTokens.TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = txn.merchantClean.ifBlank { "Merchant" },
                            color = ApexThemeTokens.TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = (if (isCredit) "+₹" else "-₹") + numberFormat.format(txn.amount),
                        color = if (isCredit) ApexThemeTokens.AccentPrimary else ApexThemeTokens.AccentDanger,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = ApexThemeTokens.StrokeSubtle)
                Spacer(modifier = Modifier.height(14.dp))

                // Detail Rows
                DetailRow(label = "Date & Time", value = dateFormatter.format(Date(txn.timestamp)))
                DetailRow(label = "Category", value = category?.name ?: txn.categoryId.replaceFirstChar { it.titlecase() })
                DetailRow(label = "Channel", value = if (txn.sources.contains(SourceChannel.NOTIFICATION)) "Email Notification" else "SMS Ingestion")
                if (!txn.accountLast4.isNullOrBlank()) {
                    DetailRow(label = "Account", value = "Ending in ••${txn.accountLast4}")
                }
                if (!txn.referenceId.isNullOrBlank()) {
                    DetailRow(label = "Reference ID", value = txn.referenceId)
                }

                if (!txn.rawSnippet.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "RAW ALERT SNIPPET",
                        color = ApexThemeTokens.TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ApexThemeTokens.RadiusSm)
                            .background(ApexThemeTokens.BgCanvas)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = txn.rawSnippet,
                            color = ApexThemeTokens.TextSecondary,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons: Edit Category, Delete
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val target = selectedTransactionForDetail
                            selectedTransactionForDetail = null
                            transactionForCategoryChange = target
                        },
                        modifier = Modifier.weight(1f),
                        shape = ApexThemeTokens.RadiusFull,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ApexThemeTokens.StrokeHighlight)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Category", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Category", color = ApexThemeTokens.TextPrimary, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.deleteTransaction(txn.id)
                            selectedTransactionForDetail = null
                        },
                        modifier = Modifier.weight(1f),
                        shape = ApexThemeTokens.RadiusFull,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ApexThemeTokens.AccentDangerBorder)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ApexThemeTokens.AccentDanger, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Delete", color = ApexThemeTokens.AccentDanger, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }

    // Category Reassignment Bottom Sheet
    transactionForCategoryChange?.let { txn ->
        CategoryPickerBottomSheet(
            merchantName = txn.merchantClean,
            currentCategoryId = txn.categoryId,
            categories = uiState.categoryTotals.map { it.category },
            onCategorySelected = { newCatId, createRule ->
                viewModel.reassignCategory(txn.id, newCatId, createRule)
                transactionForCategoryChange = null
            },
            onDismiss = { transactionForCategoryChange = null }
        )
    }

    // Settings Bottom Sheet
    if (showSettingsSheet) {
        SettingsBottomSheet(
            settings = settings,
            updateState = updateState,
            onDismiss = { showSettingsSheet = false },
            onToggleSms = { viewModel.toggleSms(it) },
            onToggleEmail = { viewModel.toggleEmail(it) },
            onToggleAutoCheckUpdates = { viewModel.toggleAutoCheckUpdates(it) },
            onCheckForUpdatesManual = { viewModel.checkForUpdates(manual = true) },
            onReviewUpdate = { updateInfo ->
                showSettingsSheet = false
                selectedUpdateInfoForReview = updateInfo
            },
            onOpenNotificationSettings = onSettingsClick
        )
    }

    // Update Details Modal Bottom Sheet
    selectedUpdateInfoForReview?.let { updateInfo ->
        UpdateDetailsBottomSheet(
            updateInfo = updateInfo,
            onDismiss = { selectedUpdateInfoForReview = null },
            onDownloadApk = { apkUrl -> openUrl(apkUrl) },
            onOpenReleasePage = { releaseUrl -> openUrl(releaseUrl) }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = ApexThemeTokens.TextSecondary,
            fontSize = 12.sp
        )
        Text(
            text = value,
            color = ApexThemeTokens.TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
