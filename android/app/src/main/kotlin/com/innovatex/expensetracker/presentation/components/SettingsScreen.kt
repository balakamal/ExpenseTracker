package com.innovatex.expensetracker.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovatex.expensetracker.BuildConfig
import com.innovatex.expensetracker.data.local.AppSettings
import com.innovatex.expensetracker.data.local.AiEnginePreference
import com.innovatex.expensetracker.data.local.NetworkCallLog
import com.innovatex.expensetracker.domain.model.AppUpdateInfo
import com.innovatex.expensetracker.presentation.dashboard.AiReclassifyState
import com.innovatex.expensetracker.presentation.dashboard.ModelDownloadUiState
import com.innovatex.expensetracker.presentation.dashboard.UpdateCheckState
import com.innovatex.expensetracker.presentation.theme.ApexThemeTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    settings: AppSettings,
    updateState: UpdateCheckState,
    cleanSpamStatus: String? = null,
    suspectedSpamCount: Int = 0,
    blockedSendersCount: Int = 0,
    trustedSendersCount: Int = 0,
    onCleanSpam: () -> Unit = {},
    onCleanOtp: () -> Unit = {},
    onOpenReviewSpam: () -> Unit = {},
    onOpenManageSenders: () -> Unit = {},
    isAiModelReady: Boolean = false,
    aiModelSizeBytes: Long = 0L,
    isGenerativeLlmReady: Boolean = false,
    onSelectAiEngine: (AiEnginePreference) -> Unit = {},
    onDownloadAiModelWithEngine: (AiEnginePreference) -> Unit = {},
    downloadState: ModelDownloadUiState = ModelDownloadUiState.Idle,
    onDownloadAiModel: () -> Unit = {},
    onCancelDownloadAiModel: () -> Unit = {},
    onDismissDownloadError: () -> Unit = {},
    onDeleteAiModel: () -> Unit = {},
    onToggleAiClassification: (Boolean) -> Unit = {},
    onToggleAiSpamDetection: (Boolean) -> Unit = {},
    onToggleBackgroundAiNotifications: (Boolean) -> Unit = {},
    aiReclassifyState: AiReclassifyState = AiReclassifyState.Idle,
    onRerunAiOnExisting: () -> Unit = {},
    onCancelAiReprocessing: () -> Unit = {},
    onToggleSms: (Boolean) -> Unit,
    onToggleEmail: (Boolean) -> Unit,
    onToggleAutoCheckUpdates: (Boolean) -> Unit,
    onCheckForUpdatesManual: () -> Unit,
    onReviewUpdate: (AppUpdateInfo) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    networkCallLogs: List<NetworkCallLog> = emptyList(),
    onClearNetworkCallLogs: () -> Unit = {},
    onBack: () -> Unit
) {
    val formattedModelSize = remember(aiModelSizeBytes) {
        if (aiModelSizeBytes > 0) {
            "%.1f MB".format(aiModelSizeBytes / (1024.0 * 1024.0))
        } else {
            "4.8 MB"
        }
    }

    Surface(modifier = modifier.fillMaxSize(), color = ApexThemeTokens.BgCanvas) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ApexThemeTokens.TextPrimary)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text("Settings", color = ApexThemeTokens.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Ingestion Channels Section
            Text(
                text = "FINANCIAL INGESTION CHANNELS",
                color = ApexThemeTokens.TextTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            IngestionChannelToggleCard(
                title = "SMS Alert Capture",
                subtitle = "Parses bank SMS messages locally on device",
                enabled = settings.smsTrackingEnabled,
                onToggle = onToggleSms
            )

            Spacer(modifier = Modifier.height(10.dp))

            IngestionChannelToggleCard(
                title = "Email Notification Listener",
                subtitle = "Captures financial alerts from mail applications",
                enabled = settings.emailTrackingEnabled,
                onToggle = onToggleEmail
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onOpenNotificationSettings,
                shape = ApexThemeTokens.RadiusSm,
                border = androidx.compose.foundation.BorderStroke(1.dp, ApexThemeTokens.StrokeSubtle),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ApexThemeTokens.TextSecondary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Android Notification Listener Access",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Anti-Spam & Ledger Cleanup Section
            Text(
                text = "DATA HYGIENE & ANTI-SPAM",
                color = ApexThemeTokens.TextTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Spam Detection Mode: Local vs AI
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ApexThemeTokens.RadiusMd)
                        .background(ApexThemeTokens.BgSurfaceElevated)
                        .border(1.dp, ApexThemeTokens.StrokeSubtle, ApexThemeTokens.RadiusMd)
                        .padding(14.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(ApexThemeTokens.RadiusFull)
                                        .background(if (settings.aiSpamDetectionEnabled && isAiModelReady) ApexThemeTokens.AccentCobaltTint else ApexThemeTokens.AccentSecondaryTint),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = if (settings.aiSpamDetectionEnabled && isAiModelReady) ApexThemeTokens.AccentCobalt else ApexThemeTokens.TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Spam Detection Mode",
                                        color = ApexThemeTokens.TextPrimary,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (settings.aiSpamDetectionEnabled && isAiModelReady) "AI Neural Model" else "Local Rules (Regex)",
                                        color = if (settings.aiSpamDetectionEnabled && isAiModelReady) ApexThemeTokens.AccentCobalt else ApexThemeTokens.TextTertiary,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Toggle Pills: [ Local ] [ AI ]
                            Row(
                                modifier = Modifier
                                    .clip(ApexThemeTokens.RadiusFull)
                                    .background(ApexThemeTokens.BgGlassPill)
                                    .border(1.dp, ApexThemeTokens.StrokeHighlight, ApexThemeTokens.RadiusFull)
                                    .padding(2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(ApexThemeTokens.RadiusFull)
                                        .background(if (!settings.aiSpamDetectionEnabled || !isAiModelReady) ApexThemeTokens.AccentSecondary else Color.Transparent)
                                        .clickable { onToggleAiSpamDetection(false) }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "Local",
                                        color = if (!settings.aiSpamDetectionEnabled || !isAiModelReady) Color.Black else ApexThemeTokens.TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(ApexThemeTokens.RadiusFull)
                                        .background(if (settings.aiSpamDetectionEnabled && isAiModelReady) ApexThemeTokens.AccentCobalt else Color.Transparent)
                                        .clickable {
                                            if (isAiModelReady) {
                                                onToggleAiSpamDetection(true)
                                            } else {
                                                onDownloadAiModel()
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "AI Model",
                                        color = if (settings.aiSpamDetectionEnabled && isAiModelReady) Color.White else ApexThemeTokens.TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (settings.aiSpamDetectionEnabled && isAiModelReady)
                                "On-device LiteRT-LM helps classify ambiguous messages; verified parsing controls every ledger field."
                            else if (!isAiModelReady)
                                "Local English and Hinglish rules stay active offline. Download the optional model for ambiguous message screening."
                            else
                                "Deterministic local keyword heuristics & regex filters active. Zero machine learning used for spam decisions.",
                            color = ApexThemeTokens.TextTertiary,
                            fontSize = 10.5.sp,
                            lineHeight = 14.5.sp
                        )
                    }
                }

                // 1. Review Suspected Spam
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ApexThemeTokens.RadiusMd)
                        .background(ApexThemeTokens.BgSurfaceElevated)
                        .border(1.dp, ApexThemeTokens.StrokeSubtle, ApexThemeTokens.RadiusMd)
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(ApexThemeTokens.RadiusFull)
                                .background(if (suspectedSpamCount > 0) ApexThemeTokens.AccentDangerTint else ApexThemeTokens.AccentPrimaryTint),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (suspectedSpamCount > 0) ApexThemeTokens.AccentDanger else ApexThemeTokens.AccentPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Suspected Spam",
                                    color = ApexThemeTokens.TextPrimary,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(ApexThemeTokens.RadiusFull)
                                        .background(if (suspectedSpamCount > 0) ApexThemeTokens.AccentDangerTint else ApexThemeTokens.AccentPrimaryTint)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (suspectedSpamCount > 0) "$suspectedSpamCount to review" else "Ledger Clean",
                                        color = if (suspectedSpamCount > 0) ApexThemeTokens.AccentDanger else ApexThemeTokens.AccentPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Inspect promotional or unverified messages. Final call is yours.",
                                color = ApexThemeTokens.TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Button(
                            onClick = onOpenReviewSpam,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (suspectedSpamCount > 0) ApexThemeTokens.AccentDanger else ApexThemeTokens.BgSurface,
                                contentColor = if (suspectedSpamCount > 0) Color.Black else ApexThemeTokens.TextPrimary
                            ),
                            shape = ApexThemeTokens.RadiusSm
                        ) {
                            Text(text = "Review", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 2. Manage Senders
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ApexThemeTokens.RadiusMd)
                        .background(ApexThemeTokens.BgSurfaceElevated)
                        .border(1.dp, ApexThemeTokens.StrokeSubtle, ApexThemeTokens.RadiusMd)
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(ApexThemeTokens.RadiusFull)
                                .background(ApexThemeTokens.AccentCobaltTint),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = ApexThemeTokens.AccentCobalt,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Manage Senders",
                                color = ApexThemeTokens.TextPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$blockedSendersCount blocked spam • $trustedSendersCount trusted",
                                color = ApexThemeTokens.TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        OutlinedButton(
                            onClick = onOpenManageSenders,
                            shape = ApexThemeTokens.RadiusSm,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = ApexThemeTokens.AccentCobalt
                            )
                        ) {
                            Text(text = "Rules", fontSize = 12.sp)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onCleanOtp, modifier = Modifier.weight(1f), shape = ApexThemeTokens.RadiusSm) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Delete OTPs", fontSize = 11.sp)
                    }
                    Button(
                        onClick = onCleanSpam,
                        modifier = Modifier.weight(1f),
                        shape = ApexThemeTokens.RadiusSm,
                        colors = ButtonDefaults.buttonColors(containerColor = ApexThemeTokens.AccentDanger)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Delete spam & promos", fontSize = 11.sp)
                    }
                }

                if (!cleanSpamStatus.isNullOrBlank()) {
                    Text(
                        text = cleanSpamStatus,
                        color = ApexThemeTokens.AccentPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // On-Device AI Engine Section
            Text(
                text = "ON-DEVICE AI ENGINES (OFFLINE & SANDBOXED)",
                color = ApexThemeTokens.TextTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Unified AI Management Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ApexThemeTokens.RadiusMd)
                    .background(ApexThemeTokens.BgSurfaceElevated)
                    .border(1.dp, ApexThemeTokens.StrokeSubtle, ApexThemeTokens.RadiusMd)
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(ApexThemeTokens.RadiusSm)
                                .background(ApexThemeTokens.AccentCobaltTint),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoFixHigh,
                                contentDescription = null,
                                tint = ApexThemeTokens.AccentCobalt,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "On-Device Neural Engine",
                                color = ApexThemeTokens.TextPrimary,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "100% on-device • Zero data leaves phone • Private",
                                color = ApexThemeTokens.TextTertiary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = ApexThemeTokens.StrokeSubtle, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "CHOOSE ACTIVE ENGINE",
                        color = ApexThemeTokens.TextSecondary,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Option 1: Generative SLM (Snapdragon / Tensor)
                    val isLlmSelected = settings.preferredAiEngine == AiEnginePreference.GENERATIVE_LLM
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ApexThemeTokens.RadiusSm)
                            .background(if (isLlmSelected) ApexThemeTokens.AccentCobaltTint.copy(alpha = 0.25f) else ApexThemeTokens.BgGlassPill)
                            .border(
                                width = if (isLlmSelected) 1.5.dp else 1.dp,
                                color = if (isLlmSelected) ApexThemeTokens.AccentCobalt else ApexThemeTokens.StrokeSubtle,
                                shape = ApexThemeTokens.RadiusSm
                            )
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Generative SLM",
                                        color = ApexThemeTokens.TextPrimary,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(ApexThemeTokens.RadiusFull)
                                            .background(ApexThemeTokens.AccentPrimaryTint)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Snapdragon / Tensor",
                                            color = ApexThemeTokens.AccentPrimary,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "LiteRT-LM Qwen3 0.6B runs on-device as a fallback for ambiguous English and Hinglish notifications. Financial details still come from the verified parser.",
                                    color = ApexThemeTokens.TextTertiary,
                                    fontSize = 10.5.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isGenerativeLlmReady) "Ready (about 475 MiB)" else "Download: about 475 MiB mixed INT4",
                                color = if (isGenerativeLlmReady) ApexThemeTokens.AccentPrimary else ApexThemeTokens.TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )

                            if (isGenerativeLlmReady) {
                                if (isLlmSelected) {
                                    Box(
                                        modifier = Modifier
                                            .clip(ApexThemeTokens.RadiusFull)
                                            .background(ApexThemeTokens.AccentPrimary)
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(text = "Active", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { onSelectAiEngine(AiEnginePreference.GENERATIVE_LLM) },
                                        modifier = Modifier.height(32.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
                                    ) {
                                        Text(text = "Use Engine", fontSize = 11.sp, color = ApexThemeTokens.AccentCobalt)
                                    }
                                }
                            } else {
                                Button(
                                    onClick = { onDownloadAiModelWithEngine(AiEnginePreference.GENERATIVE_LLM) },
                                    colors = ButtonDefaults.buttonColors(containerColor = ApexThemeTokens.AccentCobalt, contentColor = Color.White),
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Download", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Download Progress (when active)
                    if (downloadState is ModelDownloadUiState.Downloading) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ApexThemeTokens.BgGlassPill, ApexThemeTokens.RadiusSm)
                                .border(1.dp, ApexThemeTokens.StrokeHighlight, ApexThemeTokens.RadiusSm)
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = ApexThemeTokens.AccentCobalt)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = downloadState.statusText, color = ApexThemeTokens.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                                Text(text = "${downloadState.progressPercent}%", color = ApexThemeTokens.AccentCobalt, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { downloadState.progressPercent / 100f },
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(ApexThemeTokens.RadiusFull),
                                color = ApexThemeTokens.AccentCobalt,
                                trackColor = ApexThemeTokens.StrokeHighlight
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = onCancelDownloadAiModel,
                                shape = ApexThemeTokens.RadiusSm,
                                modifier = Modifier.fillMaxWidth().height(32.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) {
                                Text(text = "Cancel Download", fontSize = 11.sp, color = ApexThemeTokens.TextSecondary)
                            }
                        }
                    }
                    // Download Failure Alert
                    if (downloadState is ModelDownloadUiState.Failed) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ApexThemeTokens.AccentDangerTint, ApexThemeTokens.RadiusSm)
                                .border(1.dp, ApexThemeTokens.AccentDangerBorder, ApexThemeTokens.RadiusSm)
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = ApexThemeTokens.AccentDanger,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Download Failed",
                                        color = ApexThemeTokens.AccentDanger,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                TextButton(
                                    onClick = onDismissDownloadError,
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text(text = "Dismiss", color = ApexThemeTokens.TextTertiary, fontSize = 11.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = downloadState.errorMessage,
                                color = ApexThemeTokens.TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { onDownloadAiModelWithEngine(settings.preferredAiEngine) },
                                colors = ButtonDefaults.buttonColors(containerColor = ApexThemeTokens.AccentDanger, contentColor = Color.White),
                                shape = ApexThemeTokens.RadiusSm,
                                modifier = Modifier.fillMaxWidth().height(32.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Retry Download", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Download Success Banner
                    if (downloadState is ModelDownloadUiState.Success) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ApexThemeTokens.AccentPrimaryTint, ApexThemeTokens.RadiusSm)
                                .border(1.dp, ApexThemeTokens.AccentPrimaryBorder, ApexThemeTokens.RadiusSm)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = ApexThemeTokens.AccentPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Model downloaded and verified successfully!",
                                color = ApexThemeTokens.AccentPrimary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Background AI notifications",
                                color = ApexThemeTokens.TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "When allowed, show background AI reply, model setup, and ledger reprocessing updates.",
                                color = ApexThemeTokens.TextTertiary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                        Switch(
                            checked = settings.backgroundAiNotificationsEnabled,
                            onCheckedChange = onToggleBackgroundAiNotifications,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ApexThemeTokens.BgCanvas,
                                checkedTrackColor = ApexThemeTokens.AccentCobalt,
                                uncheckedThumbColor = ApexThemeTokens.TextSecondary,
                                uncheckedTrackColor = ApexThemeTokens.BgGlassPill
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = ApexThemeTokens.StrokeSubtle, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Toggle 1: Enable AI Classification
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AI Category Classification",
                                color = ApexThemeTokens.TextPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Categorizes expenses using the selected on-device AI model",
                                color = ApexThemeTokens.TextTertiary,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = settings.aiClassificationEnabled,
                            onCheckedChange = onToggleAiClassification,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ApexThemeTokens.BgCanvas,
                                checkedTrackColor = ApexThemeTokens.AccentCobalt,
                                uncheckedThumbColor = ApexThemeTokens.TextSecondary,
                                uncheckedTrackColor = ApexThemeTokens.BgGlassPill
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Toggle 2: AI Spam & Reminder Filter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AI Spam & Reminder Filter",
                                color = ApexThemeTokens.TextPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Excludes marketing promos & bill reminders from totals",
                                color = ApexThemeTokens.TextTertiary,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = settings.aiSpamDetectionEnabled,
                            onCheckedChange = onToggleAiSpamDetection,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ApexThemeTokens.BgCanvas,
                                checkedTrackColor = ApexThemeTokens.AccentCobalt,
                                uncheckedThumbColor = ApexThemeTokens.TextSecondary,
                                uncheckedTrackColor = ApexThemeTokens.BgGlassPill
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = ApexThemeTokens.StrokeSubtle, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Re-verify & Deduplicate Ledger Action
                    when (aiReclassifyState) {
                        is AiReclassifyState.Running -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(ApexThemeTokens.RadiusSm)
                                    .background(ApexThemeTokens.BgGlassPill)
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            color = ApexThemeTokens.AccentCobalt
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = aiReclassifyState.statusText,
                                            color = ApexThemeTokens.TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Text(
                                        text = "${aiReclassifyState.processed} / ${aiReclassifyState.total}",
                                        color = ApexThemeTokens.AccentCobalt,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = {
                                        if (aiReclassifyState.total > 0)
                                            aiReclassifyState.processed.toFloat() / aiReclassifyState.total.toFloat()
                                        else 0f
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(ApexThemeTokens.RadiusFull),
                                    color = ApexThemeTokens.AccentCobalt,
                                    trackColor = ApexThemeTokens.StrokeHighlight
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(
                                    onClick = onCancelAiReprocessing,
                                    modifier = Modifier.align(Alignment.End),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text(text = "Cancel", color = ApexThemeTokens.TextTertiary, fontSize = 11.sp)
                                }
                            }
                        }
                        else -> {
                            if (aiReclassifyState is AiReclassifyState.Failed) {
                                Text(
                                    text = aiReclassifyState.message,
                                    color = ApexThemeTokens.AccentDanger,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            if (aiReclassifyState is AiReclassifyState.Done) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(ApexThemeTokens.RadiusSm)
                                        .background(ApexThemeTokens.AccentPrimaryTint)
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = ApexThemeTokens.AccentPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Verified ${aiReclassifyState.updatedCount} txns • Suppressed ${aiReclassifyState.deduplicatedCount} duplicate records",
                                            color = ApexThemeTokens.AccentPrimary,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            Button(
                                onClick = onRerunAiOnExisting,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ApexThemeTokens.BgGlassPill,
                                    contentColor = ApexThemeTokens.AccentCobalt
                                ),
                                shape = ApexThemeTokens.RadiusSm,
                                border = BorderStroke(1.dp, ApexThemeTokens.AccentCobaltBorder),
                                modifier = Modifier.fillMaxWidth().height(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoFixHigh,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = ApexThemeTokens.AccentCobalt
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (aiReclassifyState is AiReclassifyState.Done) "Re-run Verification & Deduplication" else "Re-verify with AI & Deduplicate Ledger",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ApexThemeTokens.AccentCobalt
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("NETWORK ACTIVITY LOG", color = ApexThemeTokens.TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier.fillMaxWidth().clip(ApexThemeTokens.RadiusMd)
                    .background(ApexThemeTokens.BgSurfaceElevated)
                    .border(1.dp, ApexThemeTokens.StrokeSubtle, ApexThemeTokens.RadiusMd).padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${networkCallLogs.size} recent app requests", color = ApexThemeTokens.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("Only endpoint, result, and duration are stored. No message contents or account data.", color = ApexThemeTokens.TextTertiary, fontSize = 10.5.sp)
                    networkCallLogs.take(5).forEach { log ->
                        Text("${log.method} ${log.endpoint} · ${log.status} · ${log.durationMs} ms", color = ApexThemeTokens.TextSecondary, fontSize = 10.sp, maxLines = 2)
                    }
                    TextButton(onClick = onClearNetworkCallLogs, enabled = networkCallLogs.isNotEmpty()) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Clear logs")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App Updates & GitHub Releases Section
            Text(
                text = "APPLICATION UPDATES & RELEASES",
                color = ApexThemeTokens.TextTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ApexThemeTokens.RadiusMd)
                    .background(ApexThemeTokens.BgSurfaceElevated)
                    .border(1.dp, ApexThemeTokens.StrokeSubtle, ApexThemeTokens.RadiusMd)
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Installed Version",
                                color = ApexThemeTokens.TextSecondary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                                color = ApexThemeTokens.TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = onCheckForUpdatesManual,
                            enabled = updateState !is UpdateCheckState.Checking,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ApexThemeTokens.AccentCobalt,
                                contentColor = Color.White
                            ),
                            shape = ApexThemeTokens.RadiusSm
                        ) {
                            if (updateState is UpdateCheckState.Checking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Check for updates",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Check", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Auto-check switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Check on App Launch",
                                color = ApexThemeTokens.TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Pings GitHub Releases API (zero personal data sent)",
                                color = ApexThemeTokens.TextTertiary,
                                fontSize = 11.sp
                            )
                        }

                        Switch(
                            checked = settings.autoCheckUpdates,
                            onCheckedChange = onToggleAutoCheckUpdates,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ApexThemeTokens.BgCanvas,
                                checkedTrackColor = ApexThemeTokens.AccentCobalt,
                                uncheckedThumbColor = ApexThemeTokens.TextSecondary,
                                uncheckedTrackColor = ApexThemeTokens.BgGlassPill
                            )
                        )
                    }

                    // Update Status Feedback
                    AnimatedVisibility(visible = updateState !is UpdateCheckState.Idle) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))
                            when (updateState) {
                                is UpdateCheckState.Checking -> {
                                    Text(
                                        text = "Querying GitHub repository...",
                                        color = ApexThemeTokens.TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                                is UpdateCheckState.UpToDate -> {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = ApexThemeTokens.AccentPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "You are on the latest version",
                                            color = ApexThemeTokens.AccentPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                is UpdateCheckState.Available -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.SystemUpdate,
                                                contentDescription = null,
                                                tint = ApexThemeTokens.AccentCobalt,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "v${updateState.updateInfo.latestVersion} Ready",
                                                color = ApexThemeTokens.AccentCobalt,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Text(
                                            text = "Review Update ➔",
                                            color = ApexThemeTokens.AccentPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.clip(ApexThemeTokens.RadiusSm)
                                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                is UpdateCheckState.Error -> {
                                    Text(
                                        text = updateState.message,
                                        color = ApexThemeTokens.AccentDanger,
                                        fontSize = 12.sp
                                    )
                                }
                                else -> Unit
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun AiFeatureBullet(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(ApexThemeTokens.AccentCobalt)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = ApexThemeTokens.TextSecondary,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )
    }
}

