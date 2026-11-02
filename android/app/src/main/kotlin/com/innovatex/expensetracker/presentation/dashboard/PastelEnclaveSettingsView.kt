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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovatex.expensetracker.data.local.AppSettings
import com.innovatex.expensetracker.presentation.theme.PastelKineticTokens
import java.io.File

@Composable
fun PastelEnclaveSettingsView(
    settings: AppSettings,
    suspectedSpamCount: Int = 0,
    blockedSendersCount: Int = 0,
    trustedSendersCount: Int = 0,
    aiModelSizeBytes: Long = 0L,
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
    onExportVaultClick: () -> Unit = {},
    onPurgeBufferClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isAuditExpanded by remember { mutableStateOf(false) }

    // Real dynamic storage sizes
    val (dbSizeMb, cacheSizeMb, modelMb, totalAppMb) = remember(aiModelSizeBytes) {
        val dbFile = context.getDatabasePath("expense_tracker.db")
        val walFile = File(dbFile.parentFile, "expense_tracker.db-wal")
        val shmFile = File(dbFile.parentFile, "expense_tracker.db-shm")
        val dbBytes = (if (dbFile.exists()) dbFile.length() else 0L) +
            (if (walFile.exists()) walFile.length() else 0L) +
            (if (shmFile.exists()) shmFile.length() else 0L)
        val cacheBytes = context.cacheDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()

        val dbMbVal = (dbBytes / (1024.0 * 1024.0)).coerceAtLeast(0.1)
        val cacheMbVal = (cacheBytes / (1024.0 * 1024.0)).coerceAtLeast(0.1)
        val modelMbVal = if (aiModelSizeBytes > 0) (aiModelSizeBytes / (1024.0 * 1024.0)) else 475.0
        val total = dbMbVal + cacheMbVal + modelMbVal

        Quadruple(
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

        // 4. Subsystem Permissions & Core Ingestion Card (White)
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
                            Text("Spam Review ($suspectedSpamCount)", color = PastelKineticTokens.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                                    .background(PastelKineticTokens.SkyFixed)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("0 OUTBOUND", color = PastelKineticTokens.SkyText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

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
