package com.innovatex.expensetracker.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovatex.expensetracker.BuildConfig
import com.innovatex.expensetracker.data.local.AppSettings
import com.innovatex.expensetracker.domain.model.AppUpdateInfo
import com.innovatex.expensetracker.presentation.dashboard.ModelDownloadUiState
import com.innovatex.expensetracker.presentation.dashboard.UpdateCheckState
import com.innovatex.expensetracker.presentation.theme.ApexThemeTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    settings: AppSettings,
    updateState: UpdateCheckState,
    cleanSpamStatus: String? = null,
    suspectedSpamCount: Int = 0,
    blockedSendersCount: Int = 0,
    trustedSendersCount: Int = 0,
    onCleanSpam: () -> Unit = {},
    onOpenReviewSpam: () -> Unit = {},
    onOpenManageSenders: () -> Unit = {},
    isAiModelReady: Boolean = false,
    aiModelSizeBytes: Long = 0L,
    downloadState: ModelDownloadUiState = ModelDownloadUiState.Idle,
    onDownloadAiModel: () -> Unit = {},
    onCancelDownloadAiModel: () -> Unit = {},
    onDeleteAiModel: () -> Unit = {},
    onToggleAiClassification: (Boolean) -> Unit = {},
    onDismiss: () -> Unit,
    onToggleSms: (Boolean) -> Unit,
    onToggleEmail: (Boolean) -> Unit,
    onToggleAutoCheckUpdates: (Boolean) -> Unit,
    onCheckForUpdatesManual: () -> Unit,
    onReviewUpdate: (AppUpdateInfo) -> Unit,
    onOpenNotificationSettings: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val formattedModelSize = remember(aiModelSizeBytes) {
        if (aiModelSizeBytes > 0) {
            "%.1f MB".format(aiModelSizeBytes / (1024.0 * 1024.0))
        } else {
            "4.8 MB"
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ApexThemeTokens.BgSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(ApexThemeTokens.RadiusFull)
                    .background(ApexThemeTokens.StrokeHighlight)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(ApexThemeTokens.RadiusSm)
                        .background(ApexThemeTokens.BgGlassPill),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = ApexThemeTokens.AccentPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "CONFIGURATION",
                        color = ApexThemeTokens.TextTertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Settings & Updates",
                        color = ApexThemeTokens.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

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

            Spacer(modifier = Modifier.height(24.dp))

            // On-Device AI Engine Section
            Text(
                text = "ON-DEVICE AI ENGINE (OFFLINE & SANDBOXED)",
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "BERT-Tiny Engine",
                                    color = ApexThemeTokens.TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(ApexThemeTokens.RadiusFull)
                                        .background(
                                            if (isAiModelReady) ApexThemeTokens.AccentPrimaryTint
                                            else ApexThemeTokens.AccentSecondaryTint
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isAiModelReady) "Ready ($formattedModelSize)"
                                            else if (downloadState is ModelDownloadUiState.Downloading) "Downloading (${downloadState.progressPercent}%)"
                                            else "Not Downloaded",
                                        color = if (isAiModelReady) ApexThemeTokens.AccentPrimary
                                            else if (downloadState is ModelDownloadUiState.Downloading) ApexThemeTokens.AccentCobalt
                                            else ApexThemeTokens.AccentSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "100% offline inference in app sandbox. Classifies real vs. spam/reminder and expense categories.",
                                color = ApexThemeTokens.TextTertiary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (!isAiModelReady) {
                        when (downloadState) {
                            is ModelDownloadUiState.Downloading -> {
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
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = ApexThemeTokens.AccentCobalt
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = downloadState.statusText,
                                                color = ApexThemeTokens.TextPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        Text(
                                            text = "${downloadState.progressPercent}%",
                                            color = ApexThemeTokens.AccentCobalt,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    androidx.compose.material3.LinearProgressIndicator(
                                        progress = { downloadState.progressPercent / 100f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(ApexThemeTokens.RadiusFull),
                                        color = ApexThemeTokens.AccentCobalt,
                                        trackColor = ApexThemeTokens.StrokeHighlight
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedButton(
                                        onClick = onCancelDownloadAiModel,
                                        shape = ApexThemeTokens.RadiusSm,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(32.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                                    ) {
                                        Text(
                                            text = "Cancel Download",
                                            fontSize = 11.sp,
                                            color = ApexThemeTokens.TextSecondary
                                        )
                                    }
                                }
                            }
                            is ModelDownloadUiState.Failed -> {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(ApexThemeTokens.AccentExpense.copy(alpha = 0.12f), ApexThemeTokens.RadiusSm)
                                            .border(1.dp, ApexThemeTokens.AccentExpense.copy(alpha = 0.3f), ApexThemeTokens.RadiusSm)
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            text = "Download failed: ${downloadState.errorMessage}",
                                            color = ApexThemeTokens.AccentExpense,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Button(
                                        onClick = onDownloadAiModel,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ApexThemeTokens.AccentCobalt,
                                            contentColor = Color.White
                                        ),
                                        shape = ApexThemeTokens.RadiusSm,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Retry Download (4.8 MB)",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            else -> {
                                Button(
                                    onClick = onDownloadAiModel,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ApexThemeTokens.AccentCobalt,
                                        contentColor = Color.White
                                    ),
                                    shape = ApexThemeTokens.RadiusSm,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Download AI Model (4.8 MB)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Enable AI Classification",
                                    color = ApexThemeTokens.TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Augments regex parser with contextual machine learning",
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

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = onDeleteAiModel,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = ApexThemeTokens.AccentDanger
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(ApexThemeTokens.AccentDangerBorder)
                            ),
                            shape = ApexThemeTokens.RadiusSm,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Delete Downloaded Model",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
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
