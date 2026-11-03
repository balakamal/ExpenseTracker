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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import com.innovatex.expensetracker.presentation.components.ManageSendersBottomSheet
import com.innovatex.expensetracker.presentation.components.SpamReviewBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.innovatex.expensetracker.presentation.components.SettingsScreen
import com.innovatex.expensetracker.presentation.components.ShimmerCardLoader
import com.innovatex.expensetracker.presentation.components.UpdateAlertBanner
import com.innovatex.expensetracker.presentation.components.UpdateBadgePill
import com.innovatex.expensetracker.presentation.components.UpdateDetailsBottomSheet
import com.innovatex.expensetracker.presentation.theme.ApexThemeTokens
import com.innovatex.expensetracker.presentation.theme.PastelKineticTokens
import com.innovatex.expensetracker.presentation.components.PastelFloatingNavDock
import com.innovatex.expensetracker.presentation.dashboard.PastelLedgerView
import com.innovatex.expensetracker.presentation.dashboard.PastelCalendarMatrixView
import com.innovatex.expensetracker.presentation.dashboard.PastelAiAssistantView
import com.innovatex.expensetracker.presentation.dashboard.PastelEnclaveSettingsView
import com.innovatex.expensetracker.presentation.transaction.CategoryPickerBottomSheet
import com.innovatex.expensetracker.presentation.transaction.LogExpenseBottomSheet
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyDashboardScreen(
    viewModel: DashboardViewModel,
    onSettingsClick: () -> Unit = {},
    onTransactionClick: (Transaction) -> Unit = {},
    initialSelectedTab: Int = 0,
    openAiTabSignal: Int = 0
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    val showUpdateBanner by viewModel.showUpdateBanner.collectAsState()
    val calendarMonthMillis by viewModel.calendarMonthMillis.collectAsState()
    val selectedDateRange by viewModel.selectedDateRange.collectAsState()
    val cleanSpamStatus by viewModel.cleanSpamStatus.collectAsState()
    val suspectedSpamItems by viewModel.suspectedSpamItems.collectAsState()
    val spamSenders by viewModel.spamSenders.collectAsState()
    val trustedSenders by viewModel.trustedSenders.collectAsState()
    val isAiModelReady by viewModel.isAiModelReady.collectAsState()
    val aiModelSizeBytes by viewModel.aiModelSizeBytes.collectAsState()
    val modelDownloadState by viewModel.modelDownloadState.collectAsState()
    val aiReclassifyState by viewModel.aiReclassifyState.collectAsState()
    val networkCallLogs by viewModel.networkCallLogs.collectAsState()
    val processingMessage by viewModel.processingMessage.collectAsState()
    val aiChatEntries by viewModel.aiChatEntries.collectAsState()
    val availableCategories by viewModel.availableCategories.collectAsState()
    val isGenerativeLlmReady by viewModel.isGenerativeLlmReady.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.observeModelDownloadWork(context)
        viewModel.observeLedgerReprocessingWork(context)
    }
    var showSpamReviewSheet by remember { mutableStateOf(false) }
    var showLogExpenseSheet by remember { mutableStateOf(false) }
    var showManageSendersSheet by remember { mutableStateOf(false) }
    var senderToBlockConfirm by remember { mutableStateOf<String?>(null) }
    var selectedUpdateInfoForReview by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var selectedTransactionForDetail by remember { mutableStateOf<Transaction?>(null) }
    var transactionForCategoryChange by remember { mutableStateOf<Transaction?>(null) }
    var selectedMainTab by rememberSaveable { mutableIntStateOf(if (initialSelectedTab == 1) 3 else initialSelectedTab.coerceIn(0, 3)) }
    var aiChatMode by rememberSaveable { mutableStateOf(AiChatMode.FINANCIAL_LEDGER) }
    var aiChatDraft by remember { mutableStateOf("") }
    var pendingAiActionId by remember { mutableStateOf<String?>(null) }

    val receiptPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            showLogExpenseSheet = true
        }
    }

    val speechRecognizerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                viewModel.askAi(context, spoken, aiChatMode)
            }
        }
    }

    BackHandler(enabled = selectedMainTab != 0) { selectedMainTab = 0 }

    LaunchedEffect(openAiTabSignal) {
        if (openAiTabSignal > 0) {
            selectedMainTab = 2
        }
    }

    val openUrl = { url: String ->
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(PastelKineticTokens.CanvasBg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // GitHub Update Alert Banner (shown when update is available and banner not dismissed)
            if (updateState is UpdateCheckState.Available && showUpdateBanner) {
                UpdateAlertBanner(
                    updateInfo = (updateState as UpdateCheckState.Available).updateInfo,
                    onUpdateClick = {
                        selectedUpdateInfoForReview = (updateState as UpdateCheckState.Available).updateInfo
                    },
                    onDismissClick = {
                        viewModel.dismissUpdateBanner()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                    when (selectedMainTab) {
                        0 -> PastelLedgerView(
                            totalCredit = uiState.totalCredit,
                            totalDebit = uiState.totalDebit,
                            creditCount = uiState.creditTransactionCount,
                            debitCount = uiState.debitTransactionCount,
                            transactions = uiState.transactions,
                            categoryTotals = uiState.categoryTotals,
                            currencySymbol = "₹",
                            monthlyBudget = settings.defaultMonthlyBudget,
                            availableMonths = uiState.availableMonths,
                            selectedMonth = uiState.monthKey,
                            salaryCreditDay = settings.salaryCreditDay,
                            onMonthSelected = { viewModel.selectMonth(it) },
                            onTransactionClick = { txn -> selectedTransactionForDetail = txn },
                            onLogExpenseClick = { showLogExpenseSheet = true },
                            onScanSmsClick = { viewModel.triggerHistoricalScan(context) },
                            onMergeAsDuplicate = { primaryId, duplicateIds ->
                                viewModel.mergeTransactions(primaryId, duplicateIds)
                            },
                            onBatchMarkAsSpamOrReminder = { ids, isReminder ->
                                viewModel.batchMarkAsSpamOrReminder(ids, isReminder)
                            },
                            onBatchDeleteTransactions = { ids ->
                                viewModel.batchDeleteTransactions(ids)
                            }
                        )
                        1 -> PastelCalendarMatrixView(
                            currentMonthMillis = calendarMonthMillis,
                            allTransactions = uiState.allTransactions,
                            currencySymbol = "₹",
                            monthlyBudget = settings.defaultMonthlyBudget,
                            salaryCreditDay = settings.salaryCreditDay,
                            selectedDateRange = selectedDateRange,
                            onDateRangeSelected = { range ->
                                viewModel.selectDateRange(range.first, range.second)
                            },
                            onClearDateSelection = {
                                viewModel.clearDateFilter()
                            },
                            onTransactionClick = { txn -> selectedTransactionForDetail = txn },
                            onLogForDayClick = { showLogExpenseSheet = true },
                            onExportClick = { viewModel.exportLedgerToCsv(context, uiState.allTransactions, "SpendWell_Month_${uiState.monthKey}") },
                            onPreviousMonth = { viewModel.previousCalendarMonth() },
                            onNextMonth = { viewModel.nextCalendarMonth() }
                        )
                        2 -> PastelAiAssistantView(
                            aiChatEntries = aiChatEntries,
                            currencySymbol = "₹",
                            isAiModelReady = isAiModelReady,
                            aiModelSizeBytes = aiModelSizeBytes,
                            topCategoryName = uiState.categoryTotals.firstOrNull()?.category?.name,
                            topCategorySpent = uiState.categoryTotals.firstOrNull()?.totalSpent ?: 0.0,
                            monthlyDebit = uiState.totalDebit,
                            onSendMessage = { q -> viewModel.askAi(context, q, aiChatMode) },
                            onApplyLedgerAction = { jobId, count -> viewModel.applyAiLedgerAction(jobId, count) },
                            onScanReceiptClick = { receiptPickerLauncher.launch("image/*") },
                            onVoiceInputClick = {
                                val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Ask Gemma about your finances...")
                                }
                                runCatching { speechRecognizerLauncher.launch(intent) }
                            },
                            onExportLedgerClick = { viewModel.exportLedgerToCsv(context, uiState.allTransactions, "SpendWell_Ledger") }
                        )
                        3 -> PastelEnclaveSettingsView(
                            settings = settings,
                            suspectedSpamCount = suspectedSpamItems.size,
                            blockedSendersCount = spamSenders.size,
                            trustedSendersCount = trustedSenders.size,
                            aiModelSizeBytes = aiModelSizeBytes,
                            isAiModelReady = isAiModelReady,
                            isGenerativeLlmReady = isGenerativeLlmReady,
                            downloadState = modelDownloadState,
                            cleanStatusText = cleanSpamStatus,
                            onCleanSpam = { viewModel.cleanSpamAndRecalculate() },
                            onCleanOtp = { viewModel.cleanOtpAndRecalculate() },
                            onOpenReviewSpam = { showSpamReviewSheet = true },
                            onOpenManageSenders = { showManageSendersSheet = true },
                            onToggleSms = { viewModel.toggleSms(it) },
                            onToggleEmail = { viewModel.toggleEmail(it) },
                            onToggleAutoCheckUpdates = { viewModel.toggleAutoCheckUpdates(it) },
                            onToggleAiClassification = { viewModel.toggleAiClassification(it) },
                            onToggleAiSpamDetection = { viewModel.toggleAiSpamDetection(it) },
                            onToggleNotifications = { viewModel.setBackgroundAiNotificationsEnabled(it) },
                            onDownloadAiModel = { engine -> viewModel.downloadAiModel(context, engine) },
                            onCancelDownloadAiModel = { viewModel.cancelDownloadAiModel(context) },
                            onDeleteAiModel = { viewModel.deleteAiModel() },
                            onDismissDownloadError = { viewModel.dismissModelDownloadError() },
                            onSelectAiEngine = { viewModel.setPreferredAiEngine(it) },
                            onSetSalaryCreditDay = { viewModel.setSalaryCreditDay(it) },
                            versionName = com.innovatex.expensetracker.BuildConfig.VERSION_NAME,
                            updateState = updateState,
                            onCheckForUpdatesManual = { viewModel.checkForUpdates(manual = true) },
                            onReviewUpdate = { updateInfo -> selectedUpdateInfoForReview = updateInfo },
                            onOpenNotificationSettings = onSettingsClick,
                            onScanSmsClick = { viewModel.triggerHistoricalScan(context) },
                            aiReclassifyState = aiReclassifyState,
                            onRerunAiOnExisting = { viewModel.rerunAiOnExistingTransactions(context) },
                            onCancelAiReprocessing = { viewModel.cancelLedgerReprocessing(context) },
                            networkCallLogs = networkCallLogs,
                            onClearNetworkCallLogs = { viewModel.clearNetworkCallLogs() },
                            onExportVaultClick = { viewModel.exportLedgerToCsv(context, uiState.allTransactions, "SpendWell_Vault_Export") },
                            onPurgeBufferClick = { viewModel.cleanSpamAndRecalculate() }
                        )
                    }
                }
            }

        // Layer 2: Subtle diffusion scrim behind floating dock for seamless text readability
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            PastelKineticTokens.CanvasBg.copy(alpha = 0.5f),
                            PastelKineticTokens.CanvasBg.copy(alpha = 0.9f)
                        )
                    )
                )
        )

        // Layer 3: Floating frosted glass navigation dock
        PastelFloatingNavDock(
            selectedTab = selectedMainTab,
            onTabSelected = { selectedMainTab = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }

    
    // Log New Expense Bottom Sheet
    if (showLogExpenseSheet) {
        LogExpenseBottomSheet(
            availableCategories = availableCategories,
            currencySymbol = "₹",
            onDismissRequest = { showLogExpenseSheet = false },
            onSaveExpense = { amount, merchant, categoryId, type ->
                viewModel.addManualExpense(amount, merchant, categoryId, type)
            },
            onScanSmsClick = { viewModel.triggerHistoricalScan(context) }
        )
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
                val isSelfTransfer = txn.isSelfTransfer
                val isLikelyDuplicate = !isValid &&
                    txn.aiClassification?.authenticityLabel == com.innovatex.expensetracker.domain.model.AuthenticityLabel.DUPLICATE
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ApexThemeTokens.RadiusSm)
                        .background(when {
                            isSelfTransfer -> ApexThemeTokens.AccentCobaltTint
                            isValid -> ApexThemeTokens.AccentPrimaryTint
                            else -> ApexThemeTokens.AccentSecondaryTint
                        })
                        .border(1.dp, when {
                            isSelfTransfer -> ApexThemeTokens.AccentCobaltBorder
                            isValid -> ApexThemeTokens.AccentPrimaryBorder
                            else -> ApexThemeTokens.AccentSecondaryBorder
                        }, ApexThemeTokens.RadiusSm)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isSelfTransfer) Icons.Default.Refresh else if (isValid) Icons.Default.CheckCircle else Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (isSelfTransfer) ApexThemeTokens.AccentCobalt else if (isValid) ApexThemeTokens.AccentPrimary else ApexThemeTokens.AccentSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = when {
                                    isSelfTransfer -> "SELF TRANSFER"
                                    isValid -> "AUTHENTIC TRANSACTION"
                                    isLikelyDuplicate -> "LIKELY DUPLICATE"
                                    else -> "PROMOTIONAL / MARKETING ALERT"
                                },
                                color = when {
                                    isSelfTransfer -> ApexThemeTokens.AccentCobalt
                                    isValid -> ApexThemeTokens.AccentPrimary
                                    isLikelyDuplicate -> ApexThemeTokens.AccentDanger
                                    else -> ApexThemeTokens.AccentSecondary
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when {
                                    isSelfTransfer -> "Kept in the ledger, but excluded from income and spending totals. Delete it below if you don't want it listed."
                                    isValid -> "Included in dashboard balance & summary cards"
                                    isLikelyDuplicate -> "Another matching transaction is counted; restore this one if they are separate purchases."
                                    else -> "Excluded from dashboard calculation cards"
                                },
                                color = ApexThemeTokens.TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Toggle Authenticity Button (Valid vs Promo/Spam)
                if (isSelfTransfer) {
                    OutlinedButton(
                        onClick = {
                            viewModel.markTransactionAsSelfTransfer(txn.id, false)
                            selectedTransactionForDetail = null
                        },
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = ApexThemeTokens.RadiusFull,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ApexThemeTokens.StrokeHighlight)
                    ) {
                        Text("Treat as regular transaction", color = ApexThemeTokens.TextPrimary, fontSize = 12.sp)
                    }
                } else if (!isValid) {
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

                if (!isSelfTransfer && isValid) {
                    OutlinedButton(
                        onClick = {
                            viewModel.markTransactionAsSelfTransfer(txn.id, true)
                            selectedTransactionForDetail = null
                        },
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = ApexThemeTokens.RadiusFull,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ApexThemeTokens.AccentCobaltBorder)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = ApexThemeTokens.AccentCobalt, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mark as self transfer (exclude from totals)", color = ApexThemeTokens.AccentCobalt, fontSize = 12.sp)
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

    val pendingActionEntry = aiChatEntries.firstOrNull { it.id == pendingAiActionId }
    val pendingAction = pendingActionEntry?.ledgerAction
    val pendingCategory = pendingAction?.let { action -> availableCategories.firstOrNull { it.id == action.categoryId } }
    val pendingMatchCount = pendingAction?.let { action ->
        uiState.allTransactions.count { it.isCountableTransaction && it.merchantClean.trim().equals(action.merchant.trim(), ignoreCase = true) && it.categoryId != action.categoryId }
    } ?: 0
    if (pendingActionEntry != null && pendingAction != null) {
        AlertDialog(
            onDismissRequest = { pendingAiActionId = null },
            containerColor = ApexThemeTokens.BgSurface,
            title = { Text("Apply ledger change?", color = ApexThemeTokens.TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (pendingCategory != null) {
                        "Change the category for $pendingMatchCount transaction${if (pendingMatchCount == 1) "" else "s"} from ${pendingAction.merchant} to ${pendingCategory.name}?"
                    } else "The target category is unavailable. No transactions will be changed.",
                    color = ApexThemeTokens.TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.applyAiLedgerAction(pendingActionEntry.id, pendingMatchCount)
                        pendingAiActionId = null
                    },
                    enabled = pendingCategory != null && pendingMatchCount > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = ApexThemeTokens.AccentPrimary, contentColor = ApexThemeTokens.TextHighContrast)
                ) { Text("Apply") }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingAiActionId = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun CorbelNavItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val foreground = if (selected) ApexThemeTokens.TextHighContrast else ApexThemeTokens.TextSecondary
    Row(
        modifier = Modifier
            .clip(ApexThemeTokens.RadiusFull)
            .background(if (selected) ApexThemeTokens.BgHighContrast else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = foreground, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(7.dp))
        Text(label, color = foreground, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AiModeTab(title: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.clip(ApexThemeTokens.RadiusFull)
            .background(if (selected) ApexThemeTokens.AccentCobaltTint else ApexThemeTokens.BgGlassPill)
            .border(1.dp, if (selected) ApexThemeTokens.AccentCobaltBorder else ApexThemeTokens.StrokeSubtle, ApexThemeTokens.RadiusFull)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(title, color = if (selected) ApexThemeTokens.AccentCobalt else ApexThemeTokens.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
            .clip(PastelKineticTokens.RadiusSm)
            .background(PastelKineticTokens.SurfaceCard)
            .border(1.dp, PastelKineticTokens.OutlineVariant, PastelKineticTokens.RadiusSm),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(24.dp)) {
            val w = size.width
            val h = size.height

            // 1. Back Sky Document
            drawRoundRect(
                color = Color(0xFFA7CAEF),
                topLeft = Offset(w * 0.18f, h * 0.12f),
                size = androidx.compose.ui.geometry.Size(w * 0.52f, h * 0.68f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.14f, w * 0.14f)
            )

            // 2. Front Obsidian Document
            drawRoundRect(
                color = Color(0xFF121824),
                topLeft = Offset(w * 0.30f, h * 0.20f),
                size = androidx.compose.ui.geometry.Size(w * 0.54f, h * 0.70f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.14f, w * 0.14f)
            )

            // 3. Header lines
            drawLine(
                color = Color.White,
                start = Offset(w * 0.40f, h * 0.35f),
                end = Offset(w * 0.68f, h * 0.35f),
                strokeWidth = 2.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            drawLine(
                color = Color(0xFFD8D5F6),
                start = Offset(w * 0.40f, h * 0.46f),
                end = Offset(w * 0.58f, h * 0.46f),
                strokeWidth = 2.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )

            // 4. Bar chart columns
            drawLine(
                color = Color(0xFFCEE5FF),
                start = Offset(w * 0.44f, h * 0.76f),
                end = Offset(w * 0.44f, h * 0.64f),
                strokeWidth = 2.5.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            drawLine(
                color = Color(0xFFA7CAEF),
                start = Offset(w * 0.53f, h * 0.76f),
                end = Offset(w * 0.53f, h * 0.58f),
                strokeWidth = 2.5.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            drawLine(
                color = Color(0xFFD8D5F6),
                start = Offset(w * 0.62f, h * 0.76f),
                end = Offset(w * 0.62f, h * 0.52f),
                strokeWidth = 2.5.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )

            // 5. Golden Amber sparkle star
            drawCircle(
                color = Color(0xFFFFB84C),
                radius = w * 0.08f,
                center = Offset(w * 0.82f, h * 0.38f)
            )
        }
    }
}
