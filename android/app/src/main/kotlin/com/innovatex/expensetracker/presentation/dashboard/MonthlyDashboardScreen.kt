package com.innovatex.expensetracker.presentation.dashboard

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import com.innovatex.expensetracker.presentation.components.ManageSendersBottomSheet
import com.innovatex.expensetracker.presentation.components.SpamReviewBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
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
    val cleanSpamStatus by viewModel.cleanSpamStatus.collectAsState()
    val suspectedSpamItems by viewModel.suspectedSpamItems.collectAsState()
    val spamSenders by viewModel.spamSenders.collectAsState()
    val trustedSenders by viewModel.trustedSenders.collectAsState()
    val isAiModelReady by viewModel.isAiModelReady.collectAsState()
    val aiModelSizeBytes by viewModel.aiModelSizeBytes.collectAsState()
    val modelDownloadState by viewModel.modelDownloadState.collectAsState()
    val aiReclassifyState by viewModel.aiReclassifyState.collectAsState()
    val isGenerativeLlmReady by viewModel.isGenerativeLlmReady.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.observeModelDownloadWork(context)
    }

    var showSettingsSheet by remember { mutableStateOf(false) }
    var showSpamReviewSheet by remember { mutableStateOf(false) }
    var showManageSendersSheet by remember { mutableStateOf(false) }
    var senderToBlockConfirm by remember { mutableStateOf<String?>(null) }
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

    // Ping animation for Corbel's "live now" status indicator (2.2s cubic curve matching Corbel web)
    val infiniteTransition = rememberInfiniteTransition(label = "livePing")
    val pingScale by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 2.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = CubicBezierEasing(0.2f, 0.6f, 0.3f, 1.0f)),
            repeatMode = RepeatMode.Restart
        ),
        label = "pingScale"
    )
    val pingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = CubicBezierEasing(0.2f, 0.6f, 0.3f, 1.0f)),
            repeatMode = RepeatMode.Restart
        ),
        label = "pingAlpha"
    )

    // Ambient floating mesh animation (Smooth drift matching Corbel .mesh blobs)
    val meshTransition = rememberInfiniteTransition(label = "meshMotion")
    val meshDriftA by meshTransition.animateFloat(
        initialValue = -0.05f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "meshDriftA"
    )
    val meshDriftB by meshTransition.animateFloat(
        initialValue = 0.04f,
        targetValue = -0.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "meshDriftB"
    )
    val meshPulse by meshTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "meshPulse"
    )

    Box(modifier = Modifier.fillMaxSize().background(ApexThemeTokens.BgCanvas)) {
        // Corbel Ambient Background Mesh (Drifting, pulsing radial glow spots)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Top-left Ocean Glow (Floating Blob A)
            val centerA = Offset(width * (0.15f + meshDriftA), height * (0.08f + meshDriftB * 0.5f))
            val radiusA = width * 0.8f * meshPulse
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ApexThemeTokens.MeshA, Color.Transparent),
                    center = centerA,
                    radius = radiusA
                ),
                radius = radiusA,
                center = centerA
            )

            // Top-right Coral Glow (Floating Blob C)
            val centerC = Offset(width * (0.9f - meshDriftA * 0.7f), height * (0.25f + meshDriftB))
            val radiusC = width * 0.65f * meshPulse
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ApexThemeTokens.MeshC, Color.Transparent),
                    center = centerC,
                    radius = radiusC
                ),
                radius = radiusC,
                center = centerC
            )

            // Bottom-right Mint Glow (Floating Blob B)
            val centerB = Offset(width * (0.8f + meshDriftB), height * (0.85f - meshDriftA * 0.5f))
            val radiusB = width * 0.75f * meshPulse
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ApexThemeTokens.MeshB, Color.Transparent),
                    center = centerB,
                    radius = radiusB
                ),
                radius = radiusB,
                center = centerB
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
                        // Brand Icon Mark (Dual Cash-Flow Arcs with + and -)
                        ExpenseTrackerLogoMark(modifier = Modifier.size(38.dp))

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "Expense Tracker",
                                color = ApexThemeTokens.TextPrimary,
                                fontSize = 16.sp,
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

                        // Corbel "live now" indicator pill (Signature Corbel solid mint badge with ink text)
                        Box(
                            modifier = Modifier
                                .clip(ApexThemeTokens.RadiusFull)
                                .background(ApexThemeTokens.BgLive)
                                .padding(horizontal = 9.dp, vertical = 4.5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Animated ping ring
                                    Canvas(modifier = Modifier.size(14.dp)) {
                                        drawCircle(
                                            color = ApexThemeTokens.TextLive.copy(alpha = pingAlpha * 0.4f),
                                            radius = (size.minDimension / 2) * pingScale
                                        )
                                        drawCircle(
                                            color = ApexThemeTokens.TextLive,
                                            radius = 3.dp.toPx()
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "live",
                                    color = ApexThemeTokens.TextLive,
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

                // Suspected Spam Suggestion Banner (Dismissible, User-Confirmed)
                if (suspectedSpamItems.isNotEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(ApexThemeTokens.RadiusMd)
                                .background(ApexThemeTokens.AccentDangerTint)
                                .border(1.dp, ApexThemeTokens.AccentDangerBorder, ApexThemeTokens.RadiusMd)
                                .clickable { showSpamReviewSheet = true }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = ApexThemeTokens.AccentDanger,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${suspectedSpamItems.size} Suspected Spam Sender(s)",
                                        color = ApexThemeTokens.AccentDanger,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Tap to review and confirm blocking or keeping them",
                                        color = ApexThemeTokens.TextSecondary,
                                        fontSize = 10.5.sp
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(ApexThemeTokens.RadiusFull)
                                        .background(ApexThemeTokens.AccentDanger)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Review",
                                        color = Color.Black,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // 1. Corbel Interactive Financial Calendar (Range-aware, Overlap-free)
                item {
                    val dateRange = remember(uiState.startDateMillis, uiState.endDateMillis) {
                        if (uiState.startDateMillis != null) {
                            Pair(uiState.startDateMillis!!, uiState.endDateMillis ?: uiState.startDateMillis!!)
                        } else null
                    }
                    CorbelCalendar(
                        currentMonthMillis = calendarMonthMillis,
                        selectedDateRange = dateRange,
                        allTransactions = uiState.allTransactions,
                        onDateTapped = { timestamp -> viewModel.onDayTapped(timestamp) },
                        onPresetSelected = { preset -> viewModel.selectPreset(preset) },
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
                DetailRow(label = "Payment Medium", value = txn.paymentMedium.displayName)
                DetailRow(label = "Sender / Source", value = txn.sender ?: "Unknown")
                DetailRow(label = "Category", value = category?.name ?: txn.categoryId.replaceFirstChar { it.titlecase() })
                DetailRow(label = "Channel", value = if (txn.sources.contains(SourceChannel.EMAIL)) "Email Notification" else "SMS Ingestion")
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

                // Authenticity & Validity Status Banner
                val isValid = txn.isValidTransaction
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ApexThemeTokens.RadiusSm)
                        .background(if (isValid) ApexThemeTokens.AccentPrimaryTint else ApexThemeTokens.AccentSecondaryTint)
                        .border(1.dp, if (isValid) ApexThemeTokens.AccentPrimaryBorder else ApexThemeTokens.AccentSecondaryBorder, ApexThemeTokens.RadiusSm)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isValid) Icons.Default.CheckCircle else Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (isValid) ApexThemeTokens.AccentPrimary else ApexThemeTokens.AccentSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isValid) "AUTHENTIC TRANSACTION" else "PROMOTIONAL / MARKETING ALERT",
                                color = if (isValid) ApexThemeTokens.AccentPrimary else ApexThemeTokens.AccentSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isValid) "Included in dashboard balance & summary cards" else "Excluded from dashboard calculation cards",
                                color = ApexThemeTokens.TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Toggle Authenticity Button (Valid vs Promo/Spam)
                if (!isValid) {
                    Button(
                        onClick = {
                            viewModel.markTransactionAsValid(txn.id)
                            selectedTransactionForDetail = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ApexThemeTokens.AccentPrimary,
                            contentColor = ApexThemeTokens.TextHighContrast
                        ),
                        shape = ApexThemeTokens.RadiusFull,
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Mark as Valid Transaction (Include in Totals)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            viewModel.markTransactionAsSpam(txn.id)
                            selectedTransactionForDetail = null
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ApexThemeTokens.AccentSecondary
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ApexThemeTokens.AccentSecondaryBorder),
                        shape = ApexThemeTokens.RadiusFull,
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Mark as Promotion / Spam (Exclude from Totals)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

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

                // Block / Unblock Sender Action Row
                if (!txn.sender.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val isSenderBlocked = spamSenders.contains(txn.sender!!.uppercase())
                    if (isSenderBlocked) {
                        OutlinedButton(
                            onClick = {
                                viewModel.unblockSender(txn.sender!!)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = ApexThemeTokens.RadiusFull,
                            border = androidx.compose.foundation.BorderStroke(1.dp, ApexThemeTokens.AccentCobaltBorder)
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = ApexThemeTokens.AccentCobalt, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Sender is Blocked (Tap to Unblock)", color = ApexThemeTokens.AccentCobalt, fontSize = 12.sp)
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                senderToBlockConfirm = txn.sender
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = ApexThemeTokens.RadiusFull,
                            border = androidx.compose.foundation.BorderStroke(1.dp, ApexThemeTokens.AccentDangerBorder)
                        ) {
                            Icon(Icons.Default.Block, contentDescription = null, tint = ApexThemeTokens.AccentDanger, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Mark Sender as Spam & Block", color = ApexThemeTokens.AccentDanger, fontSize = 12.sp)
                        }
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
            cleanSpamStatus = cleanSpamStatus,
            suspectedSpamCount = suspectedSpamItems.size,
            blockedSendersCount = spamSenders.size,
            trustedSendersCount = trustedSenders.size,
            isAiModelReady = isAiModelReady,
            aiModelSizeBytes = aiModelSizeBytes,
            isGenerativeLlmReady = isGenerativeLlmReady,
            onSelectAiEngine = { viewModel.setPreferredAiEngine(it) },
            onDownloadAiModelWithEngine = { engine -> viewModel.downloadAiModel(context, engine) },
            downloadState = modelDownloadState,
            onDownloadAiModel = { viewModel.downloadAiModel(context) },
            onCancelDownloadAiModel = { viewModel.cancelDownloadAiModel(context) },
            onDismissDownloadError = { viewModel.dismissModelDownloadError() },
            onDeleteAiModel = { viewModel.deleteAiModel() },
            aiReclassifyState = aiReclassifyState,
            onRerunAiOnExisting = { viewModel.rerunAiOnExistingTransactions() },
            onToggleAiClassification = { viewModel.toggleAiClassification(it) },
            onToggleAiSpamDetection = { viewModel.toggleAiSpamDetection(it) },
            onCleanSpam = { viewModel.cleanSpamAndRecalculate() },
            onOpenReviewSpam = {
                showSettingsSheet = false
                showSpamReviewSheet = true
            },
            onOpenManageSenders = {
                showSettingsSheet = false
                showManageSendersSheet = true
            },
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

    // Review Suspected Spam Bottom Sheet
    if (showSpamReviewSheet) {
        SpamReviewBottomSheet(
            suspectedItems = suspectedSpamItems,
            onConfirmSpam = { item -> viewModel.confirmSpamSender(item) },
            onTrustSender = { item -> viewModel.trustSender(item) },
            onConfirmAll = { viewModel.confirmAllSpam() },
            onDismissRequest = { showSpamReviewSheet = false }
        )
    }

    // Manage Senders Bottom Sheet
    if (showManageSendersSheet) {
        ManageSendersBottomSheet(
            spamSenders = spamSenders,
            trustedSenders = trustedSenders,
            onUnblockSender = { sender -> viewModel.unblockSender(sender) },
            onAddSpamSender = { sender -> viewModel.markSenderAsSpam(sender, deleteExisting = true) },
            onUntrustSender = { sender -> viewModel.untrustSender(sender) },
            onAddTrustedSender = { sender -> viewModel.markSenderAsTrusted(sender) },
            onDismissRequest = { showManageSendersSheet = false }
        )
    }

    // Block Sender Confirmation Dialog
    senderToBlockConfirm?.let { sender ->
        AlertDialog(
            onDismissRequest = { senderToBlockConfirm = null },
            containerColor = ApexThemeTokens.BgSurface,
            title = {
                Text(
                    text = "Block Sender as Spam?",
                    color = ApexThemeTokens.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Sender '$sender' will be added to your blocked list. All existing transactions from this sender will be removed, and future incoming messages will be ignored.",
                    color = ApexThemeTokens.TextSecondary,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.markSenderAsSpam(sender, deleteExisting = true)
                        selectedTransactionForDetail = null
                        senderToBlockConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ApexThemeTokens.AccentDanger,
                        contentColor = Color.Black
                    ),
                    shape = ApexThemeTokens.RadiusSm
                ) {
                    Text("Block & Delete", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { senderToBlockConfirm = null },
                    shape = ApexThemeTokens.RadiusSm
                ) {
                    Text("Cancel", fontSize = 12.sp)
                }
            }
        )
    }

    // Update Details Modal Bottom Sheet
    selectedUpdateInfoForReview?.let { updateInfo ->
        UpdateDetailsBottomSheet(
            updateInfo = updateInfo,
            onDismiss = { selectedUpdateInfoForReview = null },
            onDownloadApk = { apkUrl ->
                runCatching {
                    val dm = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                    val request = android.app.DownloadManager.Request(Uri.parse(apkUrl)).apply {
                        setTitle("Expense Tracker Update v${updateInfo.latestVersion}")
                        setDescription("Downloading latest release APK...")
                        setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        setDestinationInExternalPublicDir(
                            android.os.Environment.DIRECTORY_DOWNLOADS,
                            "ExpenseTracker-${updateInfo.latestVersion}.apk"
                        )
                        setMimeType("application/vnd.android.package-archive")
                    }
                    dm.enqueue(request)
                    android.widget.Toast.makeText(
                        context,
                        "Downloading v${updateInfo.latestVersion} in background. Tap notification when done to install.",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                    selectedUpdateInfoForReview = null
                }.onFailure {
                    openUrl(apkUrl)
                }
            },
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

@Composable
fun ExpenseTrackerLogoMark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(ApexThemeTokens.RadiusSm)
            .background(ApexThemeTokens.BgSurfaceCard)
            .border(1.dp, ApexThemeTokens.StrokeSubtle, ApexThemeTokens.RadiusSm),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(24.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.width * 0.42f
            val strokeW = size.width * 0.16f
            val gap = size.height * 0.08f

            // Upper Cash-Flow Arc (Corbel Mint Inflow)
            drawArc(
                color = ApexThemeTokens.AccentPrimary,
                startAngle = 186f,
                sweepAngle = 168f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius - gap / 2f),
                size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = strokeW,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )

            // Lower Cash-Flow Arc (Corbel Coral Outflow)
            drawArc(
                color = ApexThemeTokens.AccentDanger,
                startAngle = 6f,
                sweepAngle = 168f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius + gap / 2f),
                size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = strokeW,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )

            // Credit Plus Sign (+) in Crisp White
            val plusY = center.y - size.height * 0.17f
            val plusSize = size.width * 0.11f
            drawLine(
                color = Color.White,
                start = Offset(center.x - plusSize, plusY),
                end = Offset(center.x + plusSize, plusY),
                strokeWidth = 2.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            drawLine(
                color = Color.White,
                start = Offset(center.x, plusY - plusSize),
                end = Offset(center.x, plusY + plusSize),
                strokeWidth = 2.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )

            // Debit Minus Sign (-) in Crisp White
            val minusY = center.y + size.height * 0.17f
            drawLine(
                color = Color.White,
                start = Offset(center.x - plusSize, minusY),
                end = Offset(center.x + plusSize, minusY),
                strokeWidth = 2.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}
