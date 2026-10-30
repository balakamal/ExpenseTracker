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
    onTransactionClick: (Transaction) -> Unit = {},
    initialSelectedTab: Int = 0,
    openAiTabSignal: Int = 0
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
    var showSettingsScreen by rememberSaveable { mutableStateOf(initialSelectedTab == 1) }
    var showSpamReviewSheet by remember { mutableStateOf(false) }
    var showManageSendersSheet by remember { mutableStateOf(false) }
    var senderToBlockConfirm by remember { mutableStateOf<String?>(null) }
    var selectedUpdateInfoForReview by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var selectedTransactionForDetail by remember { mutableStateOf<Transaction?>(null) }
    var transactionForCategoryChange by remember { mutableStateOf<Transaction?>(null) }
    var selectedMainTab by rememberSaveable { mutableIntStateOf(initialSelectedTab.coerceIn(0, 2)) }
    var aiChatMode by rememberSaveable { mutableStateOf(AiChatMode.FINANCIAL_LEDGER) }
    var aiChatDraft by remember { mutableStateOf("") }
    var pendingAiActionId by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = showSettingsScreen) { showSettingsScreen = false }

    LaunchedEffect(openAiTabSignal) {
        if (openAiTabSignal > 0) {
            showSettingsScreen = false
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
            bottomBar = {
                if (!showSettingsScreen) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = ApexThemeTokens.RadiusFull,
                        color = ApexThemeTokens.BgSurface.copy(alpha = 0.94f),
                        tonalElevation = 0.dp,
                        shadowElevation = 12.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ApexThemeTokens.StrokeHighlight.copy(alpha = 0.55f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CorbelNavItem("Ledger", Icons.Default.Home, !showSettingsScreen && selectedMainTab == 0) {
                                showSettingsScreen = false
                                selectedMainTab = 0
                            }
                            CorbelNavItem("Settings", Icons.Default.Settings, showSettingsScreen) {
                                showSettingsScreen = true
                            }
                            CorbelNavItem("AI Ask", Icons.Default.AutoAwesome, !showSettingsScreen && selectedMainTab == 2) {
                                showSettingsScreen = false
                                selectedMainTab = 2
                            }
                        }
                    }
                }
                }
            },
            floatingActionButton = {
                if (!showSettingsScreen && selectedMainTab == 0) {
                    FloatingActionButton(
                        onClick = { viewModel.triggerHistoricalScan(context) },
                        containerColor = ApexThemeTokens.AccentPrimary,
                        contentColor = ApexThemeTokens.TextHighContrast,
                        shape = CircleShape,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Scan SMS", modifier = Modifier.size(22.dp))
                    }
                }
            }
        ) { innerPadding ->
            if (showSettingsScreen) {
                SettingsScreen(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
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
                    onRerunAiOnExisting = { viewModel.rerunAiOnExistingTransactions(context) },
                    onCancelAiReprocessing = { viewModel.cancelLedgerReprocessing(context) },
                    onToggleAiClassification = { viewModel.toggleAiClassification(it) },
                    onToggleAiSpamDetection = { viewModel.toggleAiSpamDetection(it) },
                    onToggleBackgroundAiNotifications = { viewModel.setBackgroundAiNotificationsEnabled(it) },
                    onCleanSpam = { viewModel.cleanSpamAndRecalculate() },
                    onCleanOtp = { viewModel.cleanOtpAndRecalculate() },
                    onOpenReviewSpam = { showSettingsScreen = false; selectedMainTab = 0; showSpamReviewSheet = true },
                    onOpenManageSenders = { showSettingsScreen = false; selectedMainTab = 0; showManageSendersSheet = true },
                    onToggleSms = { viewModel.toggleSms(it) },
                    onToggleEmail = { viewModel.toggleEmail(it) },
                    onToggleAutoCheckUpdates = { viewModel.toggleAutoCheckUpdates(it) },
                    onCheckForUpdatesManual = { viewModel.checkForUpdates(manual = true) },
                    onReviewUpdate = { updateInfo -> showSettingsScreen = false; selectedUpdateInfoForReview = updateInfo },
                    onOpenNotificationSettings = onSettingsClick,
                    networkCallLogs = networkCallLogs,
                    onClearNetworkCallLogs = { viewModel.clearNetworkCallLogs() },
                    onBack = { showSettingsScreen = false }
                )
            } else LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .statusBarsPadding(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (selectedMainTab == 2) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Ask AI", color = ApexThemeTokens.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text(
                                if (aiChatMode == AiChatMode.FINANCIAL_LEDGER) {
                                    "Ask about your ledger, or request a merchant recategorization. Review and confirm any suggested change before it is applied."
                                } else "General AI answers questions but cannot change your ledger.",
                                color = ApexThemeTokens.TextSecondary,
                                fontSize = 13.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AiModeTab(AiChatMode.FINANCIAL_LEDGER.title, aiChatMode == AiChatMode.FINANCIAL_LEDGER) { aiChatMode = AiChatMode.FINANCIAL_LEDGER }
                                AiModeTab(AiChatMode.GENERAL.title, aiChatMode == AiChatMode.GENERAL) { aiChatMode = AiChatMode.GENERAL }
                            }
                            Text(
                                if (aiChatMode == AiChatMode.FINANCIAL_LEDGER) "Ask about spending, income, categories, or your budget." else "Chat about everyday questions. This mode does not use your ledger.",
                                color = ApexThemeTokens.TextTertiary,
                                fontSize = 12.sp
                            )
                            if (!isGenerativeLlmReady) {
                                Text("Download the on-device language model in Settings to start either chat.", color = ApexThemeTokens.AccentSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                    items(aiChatEntries.filter { it.mode == aiChatMode }, key = { it.id }) { entry ->
                        Column(
                            modifier = Modifier.fillMaxWidth().clip(ApexThemeTokens.RadiusMd)
                                .background(ApexThemeTokens.BgSurfaceElevated)
                                .border(1.dp, ApexThemeTokens.StrokeSubtle, ApexThemeTokens.RadiusMd).padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("You", color = ApexThemeTokens.AccentCobalt, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(entry.prompt, color = ApexThemeTokens.TextPrimary, fontSize = 14.sp)
                            when {
                                entry.isWorking -> Text("Thinking on-device… You can switch tabs; the task will keep running.", color = ApexThemeTokens.TextTertiary, fontSize = 12.sp)
                                entry.answer != null -> {
                                    HorizontalDivider(color = ApexThemeTokens.StrokeSubtle)
                                    Text("Apex AI", color = ApexThemeTokens.AccentPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(entry.answer, color = ApexThemeTokens.TextPrimary, fontSize = 14.sp)
                                }
                                entry.error != null -> Text(entry.error, color = ApexThemeTokens.AccentDanger, fontSize = 12.sp)
                            }
                            entry.ledgerAction?.let { action ->
                                val targetCategory = availableCategories.firstOrNull { it.id == action.categoryId }
                                val matchingCount = uiState.allTransactions.count {
                                    it.isValidTransaction && it.merchantClean.trim().equals(action.merchant.trim(), ignoreCase = true) && it.categoryId != action.categoryId
                                }
                                Column(
                                    modifier = Modifier.fillMaxWidth().clip(ApexThemeTokens.RadiusSm)
                                        .background(ApexThemeTokens.AccentCobaltTint)
                                        .border(1.dp, ApexThemeTokens.AccentCobaltBorder, ApexThemeTokens.RadiusSm)
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("Suggested ledger change", color = ApexThemeTokens.AccentCobalt, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${action.merchant} → ${targetCategory?.name ?: "Unknown category"} ($matchingCount transaction${if (matchingCount == 1) "" else "s"})",
                                        color = ApexThemeTokens.TextPrimary,
                                        fontSize = 13.sp
                                    )
                                    if (action.reason.isNotBlank()) {
                                        Text(action.reason, color = ApexThemeTokens.TextSecondary, fontSize = 12.sp)
                                    }
                                    if (entry.actionStatus != null) {
                                        Text(entry.actionStatus, color = ApexThemeTokens.AccentPrimary, fontSize = 12.sp)
                                    } else if (targetCategory != null && matchingCount > 0) {
                                        OutlinedButton(
                                            onClick = { pendingAiActionId = entry.id },
                                            shape = ApexThemeTokens.RadiusSm
                                        ) { Text("Review change", fontSize = 12.sp) }
                                    } else {
                                        Text("This proposal no longer matches an available category or transaction.", color = ApexThemeTokens.AccentSecondary, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextField(
                                value = aiChatDraft,
                                onValueChange = { aiChatDraft = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text(if (aiChatMode == AiChatMode.GENERAL) "Ask anything…" else "Ask about your ledger…") },
                                maxLines = 4,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = ApexThemeTokens.BgSurfaceElevated,
                                    unfocusedContainerColor = ApexThemeTokens.BgSurfaceElevated
                                )
                            )
                            Button(
                                onClick = {
                                    val question = aiChatDraft.trim()
                                    if (question.isNotEmpty()) {
                                        viewModel.askAi(context, question, aiChatMode)
                                        aiChatDraft = ""
                                    }
                                },
                                enabled = aiChatDraft.isNotBlank() && isGenerativeLlmReady
                            ) { Text("Send") }
                        }
                    }
                } else {
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
                            processingMessage?.let { current ->
                                Text("Processing: $current", color = ApexThemeTokens.AccentCobalt, fontSize = 10.sp, maxLines = 1)
                            }
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
                            onClick = { showSettingsScreen = true },
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
        uiState.allTransactions.count { it.isValidTransaction && it.merchantClean.trim().equals(action.merchant.trim(), ignoreCase = true) && it.categoryId != action.categoryId }
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
