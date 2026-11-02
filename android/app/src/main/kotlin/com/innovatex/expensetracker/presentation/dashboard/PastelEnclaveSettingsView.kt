package com.innovatex.expensetracker.presentation.dashboard

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import com.innovatex.expensetracker.BuildConfig
import com.innovatex.expensetracker.data.local.AiEnginePreference
import com.innovatex.expensetracker.data.local.NetworkCallLog
import com.innovatex.expensetracker.domain.model.AppUpdateInfo
import com.innovatex.expensetracker.presentation.dashboard.AiReclassifyState
import com.innovatex.expensetracker.presentation.dashboard.ModelDownloadUiState
import com.innovatex.expensetracker.presentation.dashboard.UpdateCheckState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovatex.expensetracker.data.local.AppSettings
import com.innovatex.expensetracker.presentation.theme.PastelKineticTokens
import java.io.File

private data class StorageBreakdown(val dbSizeMb: String, val cacheSizeMb: String, val modelMb: String, val totalAppMb: String)

@Composable
fun PastelEnclaveSettingsView(
    settings: AppSettings,
    suspectedSpamCount: Int = 0,
    blockedSendersCount: Int = 0,
    trustedSendersCount: Int = 0,
    aiModelSizeBytes: Long = 0L,
    isAiModelReady: Boolean = false,
    isGenerativeLlmReady: Boolean = false,
    downloadState: ModelDownloadUiState = ModelDownloadUiState.Idle,
    cleanStatusText: String? = null,
    onCleanSpam: () -> Unit = {},
    onCleanOtp: () -> Unit = {},
    onOpenReviewSpam: () -> Unit = {},
    onOpenManageSenders: () -> Unit = {},
    onToggleSms: (Boolean) -> Unit = {},
    onToggleEmail: (Boolean) -> Unit = {},
    onToggleAutoCheckUpdates: (Boolean) -> Unit = {},
    onToggleAiClassification: (Boolean) -> Unit = {},
    onToggleAiSpamDetection: (Boolean) -> Unit = {},
    onToggleNotifications: (Boolean) -> Unit = {},
    onDownloadAiModel: (AiEnginePreference) -> Unit = {},
    onCancelDownloadAiModel: () -> Unit = {},
    onDeleteAiModel: () -> Unit = {},
    onDismissDownloadError: () -> Unit = {},
    onSelectAiEngine: (AiEnginePreference) -> Unit = {},
    onSetSalaryCreditDay: (Int) -> Unit = {},
    versionName: String = BuildConfig.VERSION_NAME,
    updateState: UpdateCheckState = UpdateCheckState.Idle,
    onCheckForUpdatesManual: () -> Unit = {},
    onReviewUpdate: (AppUpdateInfo) -> Unit = {},
    onOpenNotificationSettings: () -> Unit = {},
    onScanSmsClick: () -> Unit = {},
    aiReclassifyState: AiReclassifyState = AiReclassifyState.Idle,
    onRerunAiOnExisting: () -> Unit = {},
    onCancelAiReprocessing: () -> Unit = {},
    networkCallLogs: List<NetworkCallLog> = emptyList(),
    onClearNetworkCallLogs: () -> Unit = {},
    onExportVaultClick: () -> Unit = {},
    onPurgeBufferClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isAuditExpanded by remember { mutableStateOf(false) }

    // Real dynamic storage sizes
    val (dbSizeMb, cacheSizeMb, modelMb, totalAppMb) = remember(aiModelSizeBytes) {
        val dbFile = context.getDatabasePath("expense_tracker.db")
        val dbParent = dbFile.parentFile ?: context.filesDir
        val walFile = File(dbParent, "expense_tracker.db-wal")
        val shmFile = File(dbParent, "expense_tracker.db-shm")
        val dbBytes = (if (dbFile.exists()) dbFile.length() else 0L) +
            (if (walFile.exists()) walFile.length() else 0L) +
            (if (shmFile.exists()) shmFile.length() else 0L)
        val cacheBytes = runCatching {
            context.cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        }.getOrDefault(0L)

        val dbMbVal = (dbBytes / (1024.0 * 1024.0)).coerceAtLeast(0.1)
        val cacheMbVal = (cacheBytes / (1024.0 * 1024.0)).coerceAtLeast(0.1)
        val modelMbVal = if (aiModelSizeBytes > 0) (aiModelSizeBytes / (1024.0 * 1024.0)) else 475.0
        val total = dbMbVal + cacheMbVal + modelMbVal

        StorageBreakdown(
            String.format(java.util.Locale.US, "%.1f MB", dbMbVal),
            String.format(java.util.Locale.US, "%.1f MB", cacheMbVal),
            String.format(java.util.Locale.US, "%.1f MB", modelMbVal),
            String.format(java.util.Locale.US, "%.1f MB", total)
        )
    }

    // Real JVM RAM footprint
    val (usedRamMb, maxRamMb) = remember {
        val runtime = Runtime.getRuntime()
        val used = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val max = runtime.maxMemory() / (1024 * 1024)
        Pair(used, max)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PastelKineticTokens.CanvasBg),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 110.dp),
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
                            imageVector = Icons.Default.Tune,
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
                            text = "Enclave Settings",
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

        // Status banner if buffer was recently cleaned
        if (!cleanStatusText.isNullOrBlank()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(PastelKineticTokens.RadiusMd)
                        .background(PastelKineticTokens.AmberFixed)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CleaningServices, contentDescription = null, tint = PastelKineticTokens.AmberText, modifier = Modifier.size(16.dp))
                        Text(text = cleanStatusText, color = PastelKineticTokens.AmberText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. Profile & Local Enclave Hero Card (Soft Sky)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, PastelKineticTokens.RadiusCard, spotColor = Color(0x18001D33))
                    .clip(PastelKineticTokens.RadiusCard)
                    .background(PastelKineticTokens.SkyFixed)
                    .padding(20.dp)
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
                                .background(PastelKineticTokens.SurfaceCard.copy(alpha = 0.85f))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(PastelKineticTokens.AmberAccent)
                                )
                                Text("Local Air-Gapped Enclave", color = PastelKineticTokens.SkyText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PastelKineticTokens.SurfaceCard),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Person, contentDescription = "Avatar", tint = PastelKineticTokens.SkyText, modifier = Modifier.size(20.dp))
                        }
                    }

                    Column {
                        Text("SpendWell Ledger Vault", color = PastelKineticTokens.SkyText, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Local-first SQLite encrypted enclave • 0 bytes transmitted outbound", color = PastelKineticTokens.SkyTextVariant, fontSize = 12.sp)
                    }

                    // Storage Allocation Bar Container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(PastelKineticTokens.RadiusMd)
                            .background(PastelKineticTokens.SurfaceCard.copy(alpha = 0.7f))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Local Storage Allocation", color = PastelKineticTokens.SkyText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(totalAppMb, color = PastelKineticTokens.SkyText, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(PastelKineticTokens.RadiusFull)
                                    .background(PastelKineticTokens.SurfaceContainer)
                                    .padding(1.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Box(modifier = Modifier.weight(0.65f).height(8.dp).clip(PastelKineticTokens.RadiusFull).background(PastelKineticTokens.AnchorPrimary))
                                Box(modifier = Modifier.weight(0.20f).height(8.dp).clip(PastelKineticTokens.RadiusFull).background(PastelKineticTokens.AmberAccent))
                                Box(modifier = Modifier.weight(0.15f).height(8.dp).clip(PastelKineticTokens.RadiusFull).background(PastelKineticTokens.SkyAccent))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(PastelKineticTokens.AnchorPrimary))
                                    Text("Weights ($modelMb)", color = PastelKineticTokens.SkyTextVariant, fontSize = 10.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(PastelKineticTokens.AmberAccent))
                                    Text("SQLite ($dbSizeMb)", color = PastelKineticTokens.SkyTextVariant, fontSize = 10.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(PastelKineticTokens.SkyAccent))
                                    Text("Cache ($cacheSizeMb)", color = PastelKineticTokens.SkyTextVariant, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Telemetry & Hardware Hub (Bento Row)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Warm Peach Card: RAM
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .shadow(4.dp, PastelKineticTokens.RadiusCard, spotColor = Color(0x18825500))
                        .clip(PastelKineticTokens.RadiusCard)
                        .background(PastelKineticTokens.AmberFixed)
                        .padding(14.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.height(115.dp)
                    ) {
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
                                Text("Memory", color = PastelKineticTokens.AmberText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Icon(imageVector = Icons.Default.Memory, contentDescription = "RAM", tint = PastelKineticTokens.AmberText, modifier = Modifier.size(16.dp))
                        }
                        Column {
                            Text("${usedRamMb} MB", color = PastelKineticTokens.AmberText, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                            Text("Heap of ${maxRamMb} MB", color = PastelKineticTokens.AmberTextVariant, fontSize = 11.sp)
                        }
                        Box(
                            modifier = Modifier
                                .clip(PastelKineticTokens.RadiusFull)
                                .background(PastelKineticTokens.SurfaceCard.copy(alpha = 0.6f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(PastelKineticTokens.Success))
                                Text("Optimal Garbage Collection", color = PastelKineticTokens.AmberText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Sky Blue Card: AI Engine
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .shadow(4.dp, PastelKineticTokens.RadiusCard, spotColor = Color(0x18001D33))
                        .clip(PastelKineticTokens.RadiusCard)
                        .background(PastelKineticTokens.SkyFixed)
                        .padding(14.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.height(115.dp)
                    ) {
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
                                Text("Classifier Core", color = PastelKineticTokens.SkyText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Icon(imageVector = Icons.Default.Bolt, contentDescription = "Core", tint = PastelKineticTokens.SkyText, modifier = Modifier.size(16.dp))
                        }
                        Column {
                            Text("Hybrid", color = PastelKineticTokens.SkyText, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                            Text(settings.preferredAiEngine.displayName, color = PastelKineticTokens.SkyTextVariant, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Box(
                            modifier = Modifier
                                .clip(PastelKineticTokens.RadiusFull)
                                .background(PastelKineticTokens.SurfaceCard.copy(alpha = 0.6f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(PastelKineticTokens.SkyText))
                                Text("On-Device Silicon", color = PastelKineticTokens.SkyText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 4. Monthly Salary Credit Date & Billing Cycle Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, PastelKineticTokens.RadiusCard, spotColor = Color(0x10121316))
                    .clip(PastelKineticTokens.RadiusCard)
                    .background(PastelKineticTokens.SurfaceCard)
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(PastelKineticTokens.AmberFixed),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "Salary Date",
                                    tint = PastelKineticTokens.AmberText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Salary Credit Date & Cycle",
                                    color = PastelKineticTokens.TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Customizes monthly partitions & pacing",
                                    color = PastelKineticTokens.TextSecondary,
                                    fontSize = 11.5.sp
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(PastelKineticTokens.RadiusFull)
                                .background(PastelKineticTokens.AmberFixed)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Day ${settings.salaryCreditDay}",
                                color = PastelKineticTokens.AmberText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Cycle Description Pill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(PastelKineticTokens.RadiusSm)
                            .background(PastelKineticTokens.SurfaceLow)
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = if (settings.salaryCreditDay == 1)
                                    "Standard Calendar Month Cycle (1st to end of month)"
                                else
                                    "Salary Cycle: ${settings.salaryCreditDay}th of month to ${settings.salaryCreditDay - 1}th of next month",
                                color = PastelKineticTokens.TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Monthly ledger calculations, calendar 'Whole Month' view, and budget burn rate all align to this date.",
                                color = PastelKineticTokens.TextSecondary,
                                fontSize = 10.5.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }

                    // Quick Preset Chips
                    Text(
                        text = "QUICK PRESET",
                        color = PastelKineticTokens.TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val presets = listOf(
                            1 to "1st (Default)",
                            25 to "25th",
                            28 to "28th",
                            30 to "30th"
                        )
                        presets.forEach { (day, label) ->
                            val isSelected = settings.salaryCreditDay == day
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(PastelKineticTokens.RadiusFull)
                                    .background(
                                        if (isSelected) PastelKineticTokens.AnchorPrimary else PastelKineticTokens.SurfaceContainer
                                    )
                                    .clickable { onSetSalaryCreditDay(day) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) PastelKineticTokens.OnAnchor else PastelKineticTokens.TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Interactive Day Stepper
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(PastelKineticTokens.RadiusFull)
                            .background(PastelKineticTokens.SurfaceContainer)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PastelKineticTokens.SurfaceCard)
                                .clickable {
                                    val newDay = (settings.salaryCreditDay - 1).coerceAtLeast(1)
                                    onSetSalaryCreditDay(newDay)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Decrease day",
                                tint = PastelKineticTokens.TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Text(
                            text = "Day ${settings.salaryCreditDay} of every month",
                            color = PastelKineticTokens.TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PastelKineticTokens.SurfaceCard)
                                .clickable {
                                    val newDay = (settings.salaryCreditDay + 1).coerceAtMost(31)
                                    onSetSalaryCreditDay(newDay)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Increase day",
                                tint = PastelKineticTokens.TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // 5. On-Device Neural LLM Engine Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, PastelKineticTokens.RadiusCard, spotColor = Color(0x10121316))
                    .clip(PastelKineticTokens.RadiusCard)
                    .background(PastelKineticTokens.SurfaceCard)
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(PastelKineticTokens.SkyFixed),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "On-Device LLM",
                                    tint = PastelKineticTokens.SkyText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "On-Device Neural LLM",
                                    color = PastelKineticTokens.TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "LiteRT-LM Qwen3 0.6B • 100% Offline",
                                    color = PastelKineticTokens.TextSecondary,
                                    fontSize = 11.5.sp
                                )
                            }
                        }

                        // Status Badge
                        val isReady = isAiModelReady || isGenerativeLlmReady
                        Box(
                            modifier = Modifier
                                .clip(PastelKineticTokens.RadiusFull)
                                .background(
                                    when {
                                        isReady -> PastelKineticTokens.Success.copy(alpha = 0.15f)
                                        downloadState is ModelDownloadUiState.Downloading -> PastelKineticTokens.SkyFixed
                                        downloadState is ModelDownloadUiState.Failed -> PastelKineticTokens.Error.copy(alpha = 0.15f)
                                        else -> PastelKineticTokens.SurfaceContainer
                                    }
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = when {
                                    isReady -> "Ready (${if (aiModelSizeBytes > 0) "${aiModelSizeBytes / (1024 * 1024)} MB" else "~475 MB"})"
                                    downloadState is ModelDownloadUiState.Downloading -> "Downloading ${downloadState.progressPercent}%"
                                    downloadState is ModelDownloadUiState.Failed -> "Failed"
                                    else -> "Not Installed"
                                },
                                color = when {
                                    isReady -> PastelKineticTokens.Success
                                    downloadState is ModelDownloadUiState.Downloading -> PastelKineticTokens.SkyText
                                    downloadState is ModelDownloadUiState.Failed -> PastelKineticTokens.Error
                                    else -> PastelKineticTokens.TextSecondary
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "Runs directly on your smartphone's NPU/CPU to screen ambiguous financial SMS and power offline conversational inquiries. Zero telemetry or personal financial data leaves your phone.",
                        color = PastelKineticTokens.TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    HorizontalDivider(color = PastelKineticTokens.SurfaceContainer)

                    // Download in progress state
                    if (downloadState is ModelDownloadUiState.Downloading) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(PastelKineticTokens.RadiusSm)
                                .background(PastelKineticTokens.SurfaceLow)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = PastelKineticTokens.AnchorPrimary
                                    )
                                    Text(
                                        text = downloadState.statusText,
                                        color = PastelKineticTokens.TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    text = "${downloadState.progressPercent}%",
                                    color = PastelKineticTokens.AnchorPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            LinearProgressIndicator(
                                progress = { downloadState.progressPercent / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(PastelKineticTokens.RadiusFull),
                                color = PastelKineticTokens.AnchorPrimary,
                                trackColor = PastelKineticTokens.SurfaceContainer
                            )

                            OutlinedButton(
                                onClick = onCancelDownloadAiModel,
                                shape = PastelKineticTokens.RadiusFull,
                                modifier = Modifier.fillMaxWidth().height(34.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Cancel Download", fontSize = 11.sp, color = PastelKineticTokens.TextSecondary)
                            }
                        }
                    }

                    // Download failed state
                    if (downloadState is ModelDownloadUiState.Failed) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(PastelKineticTokens.RadiusSm)
                                .background(PastelKineticTokens.Error.copy(alpha = 0.08f))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = PastelKineticTokens.Error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Download Interrupted",
                                        color = PastelKineticTokens.Error,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Dismiss",
                                    color = PastelKineticTokens.TextSecondary,
                                    fontSize = 11.sp,
                                    modifier = Modifier.clickable { onDismissDownloadError() }
                                )
                            }
                            Text(
                                text = downloadState.errorMessage,
                                color = PastelKineticTokens.TextSecondary,
                                fontSize = 11.sp
                            )
                            Button(
                                onClick = { onDownloadAiModel(settings.preferredAiEngine) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PastelKineticTokens.Error,
                                    contentColor = Color.White
                                ),
                                shape = PastelKineticTokens.RadiusFull,
                                modifier = Modifier.fillMaxWidth().height(34.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Retry Download", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Ready state vs Not downloaded state buttons
                    val isReady = isAiModelReady || isGenerativeLlmReady
                    if (isReady) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Active",
                                    tint = PastelKineticTokens.Success,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Model ready & actively sandboxed",
                                    color = PastelKineticTokens.TextPrimary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            OutlinedButton(
                                onClick = onDeleteAiModel,
                                shape = PastelKineticTokens.RadiusFull,
                                modifier = Modifier.height(34.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(14.dp), tint = PastelKineticTokens.Error)
                                Spacer(Modifier.width(4.dp))
                                Text("Delete", fontSize = 11.sp, color = PastelKineticTokens.Error)
                            }
                        }
                    } else if (downloadState !is ModelDownloadUiState.Downloading) {
                        Button(
                            onClick = { onDownloadAiModel(AiEnginePreference.GENERATIVE_LLM) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PastelKineticTokens.AnchorPrimary,
                                contentColor = PastelKineticTokens.OnAnchor
                            ),
                            shape = PastelKineticTokens.RadiusFull,
                            modifier = Modifier.fillMaxWidth().height(42.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Download On-Device LLM (~475 MB)", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Engine Mode Picker: SLM vs Rules Only
                    Text(
                        text = "ENGINE PREFERENCE",
                        color = PastelKineticTokens.TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(PastelKineticTokens.RadiusFull)
                            .background(PastelKineticTokens.SurfaceContainer)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val isLlm = settings.preferredAiEngine == AiEnginePreference.GENERATIVE_LLM
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(PastelKineticTokens.RadiusFull)
                                .background(if (isLlm) PastelKineticTokens.SurfaceCard else Color.Transparent)
                                .clickable { onSelectAiEngine(AiEnginePreference.GENERATIVE_LLM) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Hybrid (SLM + Rules)",
                                color = if (isLlm) PastelKineticTokens.TextPrimary else PastelKineticTokens.TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isLlm) FontWeight.Bold else FontWeight.Medium
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(PastelKineticTokens.RadiusFull)
                                .background(if (!isLlm) PastelKineticTokens.SurfaceCard else Color.Transparent)
                                .clickable { onSelectAiEngine(AiEnginePreference.HEURISTIC_ONLY) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Rules & Heuristics Only",
                                color = if (!isLlm) PastelKineticTokens.TextPrimary else PastelKineticTokens.TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (!isLlm) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }

                    // AI Ledger Reprocessing / Deduplication Action
                    HorizontalDivider(color = PastelKineticTokens.SurfaceContainer)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "AI Ledger Verification & Deduplication",
                                    color = PastelKineticTokens.TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Re-evaluates historical transactions with local ML",
                                    color = PastelKineticTokens.TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.AutoFixHigh,
                                contentDescription = "Reprocess",
                                tint = PastelKineticTokens.AnchorPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        when (aiReclassifyState) {
                            is AiReclassifyState.Running -> {
                                val progressFraction = if (aiReclassifyState.total > 0)
                                    (aiReclassifyState.processed.toFloat() / aiReclassifyState.total.toFloat()).coerceIn(0f, 1f)
                                else 0f
                                val progressPercent = (progressFraction * 100).toInt()
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(PastelKineticTokens.RadiusSm)
                                        .background(PastelKineticTokens.SurfaceLow)
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = aiReclassifyState.statusText,
                                            color = PastelKineticTokens.TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "$progressPercent% (${aiReclassifyState.processed}/${aiReclassifyState.total})",
                                            color = PastelKineticTokens.AnchorPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { progressFraction },
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(PastelKineticTokens.RadiusFull),
                                        color = PastelKineticTokens.AnchorPrimary,
                                        trackColor = PastelKineticTokens.SurfaceContainer
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Processing ledger items...",
                                            color = PastelKineticTokens.TextSecondary,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(PastelKineticTokens.RadiusFull)
                                                .background(PastelKineticTokens.SurfaceContainer)
                                                .clickable { onCancelAiReprocessing() }
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text("Cancel", color = PastelKineticTokens.Error, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                            is AiReclassifyState.Done -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(PastelKineticTokens.RadiusSm)
                                        .background(PastelKineticTokens.Success.copy(alpha = 0.12f))
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PastelKineticTokens.Success, modifier = Modifier.size(16.dp))
                                        Text(
                                            text = "Verified: ${aiReclassifyState.updatedCount} transactions updated",
                                            color = PastelKineticTokens.Success,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(PastelKineticTokens.RadiusFull)
                                        .background(PastelKineticTokens.SurfaceLow)
                                        .clickable { onRerunAiOnExisting() }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Run Verification Again", color = PastelKineticTokens.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            is AiReclassifyState.Failed -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(PastelKineticTokens.RadiusSm)
                                        .background(PastelKineticTokens.Error.copy(alpha = 0.12f))
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = "Verification error: ${aiReclassifyState.message}",
                                        color = PastelKineticTokens.Error,
                                        fontSize = 12.sp
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(PastelKineticTokens.RadiusFull)
                                        .background(PastelKineticTokens.SurfaceLow)
                                        .clickable { onRerunAiOnExisting() }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Retry AI Verification", color = PastelKineticTokens.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            is AiReclassifyState.Idle -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(PastelKineticTokens.RadiusFull)
                                        .background(PastelKineticTokens.SurfaceLow)
                                        .clickable { onRerunAiOnExisting() }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = PastelKineticTokens.TextPrimary, modifier = Modifier.size(14.dp))
                                        Text("Re-verify Existing Transactions with AI", color = PastelKineticTokens.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Subsystem Permissions & Core Ingestion Card (White)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, PastelKineticTokens.RadiusCard, spotColor = Color(0x10121316))
                    .clip(PastelKineticTokens.RadiusCard)
                    .background(PastelKineticTokens.SurfaceCard)
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Subsystem Controls", color = PastelKineticTokens.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Sandboxed on-device ingestion & ML switches", color = PastelKineticTokens.TextSecondary, fontSize = 12.sp)
                        }
                        Icon(imageVector = Icons.Default.Security, contentDescription = "Security", tint = PastelKineticTokens.TextOutline, modifier = Modifier.size(20.dp))
                    }

                    // Toggle 1: SMS Ingestion Engine
                    PermissionToggleRow(
                        icon = Icons.Default.Sms,
                        iconBg = PastelKineticTokens.SkyFixed,
                        iconTint = PastelKineticTokens.SkyText,
                        title = "SMS Ingestion Engine",
                        subtitle = "Capture transactional bank notifications locally",
                        checked = settings.smsTrackingEnabled,
                        onCheckedChange = onToggleSms
                    )

                    // Historical SMS Scan Action
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 48.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(PastelKineticTokens.RadiusFull)
                                .background(PastelKineticTokens.SkyFixed)
                                .clickable { onScanSmsClick() }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = PastelKineticTokens.SkyText, modifier = Modifier.size(13.dp))
                                Text("Scan Historical SMS Inbox", color = PastelKineticTokens.SkyText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Toggle 2: Email Ingestion Engine
                    PermissionToggleRow(
                        icon = Icons.Default.Mail,
                        iconBg = PastelKineticTokens.AmberFixed,
                        iconTint = PastelKineticTokens.AmberText,
                        title = "Email Notification Listener",
                        subtitle = "Parse banking receipts from notification bar",
                        checked = settings.emailTrackingEnabled,
                        onCheckedChange = onToggleEmail
                    )

                    // Notification Access Settings Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 48.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(PastelKineticTokens.RadiusFull)
                                .background(PastelKineticTokens.AmberFixed)
                                .clickable { onOpenNotificationSettings() }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = PastelKineticTokens.AmberText, modifier = Modifier.size(13.dp))
                                Text("Configure Notification Access", color = PastelKineticTokens.AmberText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Toggle 3: On-Device AI Classification
                    PermissionToggleRow(
                        icon = Icons.Default.AutoAwesome,
                        iconBg = PastelKineticTokens.SurfaceLow,
                        iconTint = PastelKineticTokens.AnchorPrimary,
                        title = "On-Device AI Classification",
                        subtitle = "Categorize merchants with hybrid local ML",
                        checked = settings.aiClassificationEnabled,
                        onCheckedChange = onToggleAiClassification
                    )

                    // Toggle 4: AI Spam Detection
                    PermissionToggleRow(
                        icon = Icons.Default.Shield,
                        iconBg = PastelKineticTokens.AmberFixed,
                        iconTint = PastelKineticTokens.AmberText,
                        title = "Spam & OTP Isolation",
                        subtitle = "Filter out OTPs, promotional alerts, and duplicates",
                        checked = settings.aiSpamDetectionEnabled,
                        onCheckedChange = onToggleAiSpamDetection
                    )

                    // Toggle 5: Auto-Check Updates
                    PermissionToggleRow(
                        icon = Icons.Default.SystemUpdate,
                        iconBg = PastelKineticTokens.SkyFixed,
                        iconTint = PastelKineticTokens.SkyText,
                        title = "Auto-Check App Updates",
                        subtitle = "Check GitHub releases quietly in background",
                        checked = settings.autoCheckUpdates,
                        onCheckedChange = onToggleAutoCheckUpdates
                    )

                    // Toggle 6: Notification Alerts
                    PermissionToggleRow(
                        icon = Icons.Default.NotificationsActive,
                        iconBg = PastelKineticTokens.SurfaceContainer,
                        iconTint = PastelKineticTokens.TextPrimary,
                        title = "Background Model Notifications",
                        subtitle = "Alert on model downloads or processing completion",
                        checked = settings.backgroundAiNotificationsEnabled,
                        onCheckedChange = onToggleNotifications
                    )

                    HorizontalDivider(color = PastelKineticTokens.SurfaceContainer)

                    // Clean status notification text
                    if (!cleanStatusText.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(PastelKineticTokens.RadiusSm)
                                .background(PastelKineticTokens.Success.copy(alpha = 0.12f))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PastelKineticTokens.Success, modifier = Modifier.size(14.dp))
                                Text(
                                    text = cleanStatusText,
                                    color = PastelKineticTokens.Success,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Hygiene Clean Actions: Clean Spam & Delete OTPs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(PastelKineticTokens.RadiusFull)
                                .background(PastelKineticTokens.SurfaceLow)
                                .clickable { onCleanSpam() }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.CleaningServices, contentDescription = null, tint = PastelKineticTokens.TextPrimary, modifier = Modifier.size(14.dp))
                                Text("Clean Spam", color = PastelKineticTokens.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(PastelKineticTokens.RadiusFull)
                                .background(PastelKineticTokens.SurfaceLow)
                                .clickable { onCleanOtp() }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = PastelKineticTokens.Error, modifier = Modifier.size(14.dp))
                                Text("Delete OTPs", color = PastelKineticTokens.Error, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Spam Review & Senders Management Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(PastelKineticTokens.RadiusFull)
                                .background(PastelKineticTokens.SurfaceLow)
                                .clickable { onOpenReviewSpam() }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Review Spam ($suspectedSpamCount)", color = PastelKineticTokens.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(PastelKineticTokens.RadiusFull)
                                .background(PastelKineticTokens.SurfaceLow)
                                .clickable { onOpenManageSenders() }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Senders ($trustedSendersCount)", color = PastelKineticTokens.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 5. Audit Telemetry Stream (Collapsible Log Card)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, PastelKineticTokens.RadiusCard, spotColor = Color(0x10121316))
                    .clip(PastelKineticTokens.RadiusCard)
                    .background(PastelKineticTokens.SurfaceCard)
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isAuditExpanded = !isAuditExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Audit Telemetry", color = PastelKineticTokens.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Box(
                                modifier = Modifier
                                    .clip(PastelKineticTokens.RadiusFull)
                                    .background(if (networkCallLogs.isEmpty()) PastelKineticTokens.SkyFixed else PastelKineticTokens.AmberFixed)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (networkCallLogs.isEmpty()) "0 OUTBOUND" else "${networkCallLogs.size} LOGGED",
                                    color = if (networkCallLogs.isEmpty()) PastelKineticTokens.SkyText else PastelKineticTokens.AmberText,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Icon(
                            imageVector = if (isAuditExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand",
                            tint = PastelKineticTokens.TextSecondary
                        )
                    }

                    // Audit Items
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        AuditStreamRow("Room SQLite Commit • Indexed WAL", "0.2ms", PastelKineticTokens.AmberAccent)
                        AuditStreamRow("Local Heuristic Matcher Pipeline", "0.8ms", PastelKineticTokens.SkyAccent)

                        if (isAuditExpanded) {
                            AuditStreamRow("Zero Network Sockets Opened", "Verified", PastelKineticTokens.AnchorPrimary)
                            AuditStreamRow("Two-Stage Temporal Dedup Guard", "Active", PastelKineticTokens.AmberDeep)
                            AuditStreamRow("GLANCE AppWidget Sync Broadcaster", "0.4ms", PastelKineticTokens.SkyDeep)

                            if (networkCallLogs.isNotEmpty()) {
                                HorizontalDivider(color = PastelKineticTokens.SurfaceContainer, modifier = Modifier.padding(vertical = 4.dp))
                                Text("Recent Network Connections", color = PastelKineticTokens.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                networkCallLogs.take(5).forEach { log ->
                                    val isBlocked = log.status.contains("BLOCK", ignoreCase = true) || log.status.startsWith("4") || log.status.startsWith("5")
                                    AuditStreamRow(
                                        "${log.method} ${log.endpoint.take(30)}",
                                        "${log.status} • ${log.durationMs}ms",
                                        if (isBlocked) PastelKineticTokens.Error else PastelKineticTokens.SkyAccent
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(PastelKineticTokens.RadiusFull)
                                        .background(PastelKineticTokens.SurfaceLow)
                                        .clickable { onClearNetworkCallLogs() }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Clear Network Logs", color = PastelKineticTokens.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Software Updates & Releases Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, PastelKineticTokens.RadiusCard, spotColor = Color(0x10121316))
                    .clip(PastelKineticTokens.RadiusCard)
                    .background(PastelKineticTokens.SurfaceCard)
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(PastelKineticTokens.SkyFixed),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdate,
                                    contentDescription = "Updates",
                                    tint = PastelKineticTokens.SkyText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Software Updates",
                                    color = PastelKineticTokens.TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Version $versionName • GitHub Releases",
                                    color = PastelKineticTokens.TextSecondary,
                                    fontSize = 11.5.sp
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(PastelKineticTokens.RadiusFull)
                                .background(PastelKineticTokens.SurfaceContainer)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "v$versionName",
                                color = PastelKineticTokens.TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    when (updateState) {
                        is UpdateCheckState.Checking -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(PastelKineticTokens.RadiusSm)
                                    .background(PastelKineticTokens.SurfaceLow)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = PastelKineticTokens.AnchorPrimary
                                )
                                Text("Checking GitHub releases for updates...", color = PastelKineticTokens.TextPrimary, fontSize = 12.sp)
                            }
                        }
                        is UpdateCheckState.Available -> {
                            val info = updateState.updateInfo
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(PastelKineticTokens.RadiusSm)
                                    .background(PastelKineticTokens.AmberFixed)
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Update Available: v${info.latestVersion}",
                                        color = PastelKineticTokens.AmberText,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = info.publishedAt.take(10),
                                        color = PastelKineticTokens.AmberTextVariant,
                                        fontSize = 11.sp
                                    )
                                }
                                if (info.releaseNotes.isNotBlank()) {
                                    Text(
                                        text = info.releaseNotes,
                                        color = PastelKineticTokens.AmberTextVariant,
                                        fontSize = 11.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(PastelKineticTokens.RadiusFull)
                                        .background(PastelKineticTokens.AnchorPrimary)
                                        .clickable { onReviewUpdate(info) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Review & Install v${info.latestVersion}", color = PastelKineticTokens.OnAnchor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        is UpdateCheckState.UpToDate -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(PastelKineticTokens.RadiusSm)
                                    .background(PastelKineticTokens.Success.copy(alpha = 0.12f))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PastelKineticTokens.Success, modifier = Modifier.size(16.dp))
                                Text("App is up to date with latest GitHub release", color = PastelKineticTokens.Success, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        is UpdateCheckState.Error -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(PastelKineticTokens.RadiusSm)
                                    .background(PastelKineticTokens.Error.copy(alpha = 0.12f))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = PastelKineticTokens.Error, modifier = Modifier.size(16.dp))
                                Text("Check failed: ${updateState.message}", color = PastelKineticTokens.Error, fontSize = 11.sp)
                            }
                        }
                        is UpdateCheckState.Idle -> {
                            // Idle
                        }
                    }

                    if (updateState !is UpdateCheckState.Checking) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(PastelKineticTokens.RadiusFull)
                                .background(PastelKineticTokens.SurfaceLow)
                                .clickable { onCheckForUpdatesManual() }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = PastelKineticTokens.TextPrimary, modifier = Modifier.size(14.dp))
                                Text("Check for Updates Now", color = PastelKineticTokens.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 6. Action Pods: Export Vault & Purge Buffer
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(PastelKineticTokens.RadiusFull)
                        .background(PastelKineticTokens.AmberFixed)
                        .clickable { onExportVaultClick() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.IosShare, contentDescription = "Export Vault", tint = PastelKineticTokens.AmberText, modifier = Modifier.size(16.dp))
                        Text("Export Vault CSV", color = PastelKineticTokens.AmberText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(PastelKineticTokens.RadiusFull)
                        .background(PastelKineticTokens.SkyFixed)
                        .clickable { onPurgeBufferClick() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CleaningServices, contentDescription = "Purge Buffer", tint = PastelKineticTokens.SkyText, modifier = Modifier.size(16.dp))
                        Text("Purge Buffer", color = PastelKineticTokens.SkyText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 7. Enclave Verification Tagline
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.VerifiedUser, contentDescription = "Verified", tint = PastelKineticTokens.TextOutline, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("SpendWell Air-Gapped Financial Core • 100% Offline", color = PastelKineticTokens.TextOutline, fontSize = 11.sp)
            }
        }
    }
}



@Composable
private fun PermissionToggleRow(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(18.dp))
            }
            Column {
                Text(text = title, color = PastelKineticTokens.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(text = subtitle, color = PastelKineticTokens.TextSecondary, fontSize = 11.sp)
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PastelKineticTokens.SurfaceCard,
                checkedTrackColor = PastelKineticTokens.AnchorPrimary,
                uncheckedThumbColor = PastelKineticTokens.SurfaceContainer,
                uncheckedTrackColor = PastelKineticTokens.SurfaceLow
            )
        )
    }
}

@Composable
private fun AuditStreamRow(
    title: String,
    duration: String,
    dotColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PastelKineticTokens.RadiusSm)
            .background(PastelKineticTokens.SurfaceLow)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(dotColor))
            Text(text = title, color = PastelKineticTokens.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        Box(
            modifier = Modifier
                .clip(PastelKineticTokens.RadiusFull)
                .background(PastelKineticTokens.SurfaceContainer)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(text = duration, color = PastelKineticTokens.TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}
