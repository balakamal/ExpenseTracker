package com.innovatex.expensetracker.presentation.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovatex.expensetracker.presentation.components.FormattedAiMarkdownText
import com.innovatex.expensetracker.presentation.theme.PastelKineticTokens
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PastelAiAssistantView(
    aiChatEntries: List<AiChatEntry> = emptyList(),
    currentMode: AiChatMode = AiChatMode.FINANCIAL_LEDGER,
    onModeChanged: (AiChatMode) -> Unit = {},
    currencySymbol: String = "₹",
    isAiModelReady: Boolean = true,
    aiModelSizeBytes: Long = 1400000000L,
    topCategoryName: String? = null,
    topCategorySpent: Double = 0.0,
    monthlyDebit: Double = 0.0,
    sessionCount: Int = 1,
    onOpenSessionsClick: () -> Unit = {},
    onNewChatClick: () -> Unit = {},
    onSendMessage: (String) -> Unit = {},
    onApplyLedgerAction: (jobId: String, expectedCount: Int) -> Unit = { _, _ -> },
    onScanReceiptClick: () -> Unit = {},
    onVoiceInputClick: () -> Unit = {},
    onExportLedgerClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val numberFormat = remember {
        NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }

    var chatInputText by remember { mutableStateOf("") }
    var suggestionShuffleOffset by remember { mutableIntStateOf(0) }

    // Mode-specific query pools
    val queryPool = remember(currentMode, topCategoryName) {
        if (currentMode == AiChatMode.FINANCIAL_LEDGER) {
            listOf(
                "🍔 ${topCategoryName ?: "Dining"} spend vs last week?",
                "📊 Summarize monthly category breakdown",
                "⚡ Check for duplicate subscription charges",
                "✏️ Recategorize Uber to Transport",
                "➕ Log expense: $currencySymbol 250 Coffee",
                "🔍 Identify my largest single expense this month",
                "💡 How to preserve $currencySymbol 500 this weekend?",
                "🛡️ Spot recurring subscriptions"
            )
        } else {
            listOf(
                "💡 Explain the 50-30-20 budget rule",
                "📈 How to build a 6-month emergency fund?",
                "💰 Best strategy to pay off credit card debt",
                "📝 Help me create a weekly savings challenge",
                "🧮 How does compounding interest work?",
                "☕ Practical tips to curb impulse spending",
                "🏖️ How to plan a realistic vacation budget"
            )
        }
    }

    val displayedQueries = remember(suggestionShuffleOffset, queryPool) {
        val count = queryPool.size
        listOf(
            queryPool[(suggestionShuffleOffset) % count],
            queryPool[(suggestionShuffleOffset + 1) % count],
            queryPool[(suggestionShuffleOffset + 2) % count]
        )
    }

    val modelSizeMb = remember(aiModelSizeBytes) {
        if (aiModelSizeBytes > 0) "${(aiModelSizeBytes / (1024 * 1024))} MB" else "~475 MB"
    }

    val bottomClearance = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + PastelKineticTokens.FloatingDockClearance

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PastelKineticTokens.CanvasBg),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = bottomClearance),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. SpendWell AI Header with New Chat & Sessions Actions
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
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "SpendWell AI",
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
                            text = "AI Assistant",
                            color = PastelKineticTokens.TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // New Chat Pill
                    Box(
                        modifier = Modifier
                            .clip(PastelKineticTokens.RadiusFull)
                            .background(PastelKineticTokens.AnchorPrimary)
                            .clickable { onNewChatClick() }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = PastelKineticTokens.OnAnchor, modifier = Modifier.size(15.dp))
                            Text("New Chat", color = PastelKineticTokens.OnAnchor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Sessions Drawer Button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(PastelKineticTokens.SurfaceContainer)
                            .clickable { onOpenSessionsClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forum,
                            contentDescription = "Sessions",
                            tint = PastelKineticTokens.TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // 2. Dual-Mode Segmented Control (App Ledger vs General AI)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(PastelKineticTokens.RadiusFull)
                    .background(PastelKineticTokens.SurfaceContainer)
                    .padding(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isAppLedger = currentMode == AiChatMode.FINANCIAL_LEDGER

                    // Option 1: App Ledger
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(PastelKineticTokens.RadiusFull)
                            .background(if (isAppLedger) PastelKineticTokens.SurfaceCard else Color.Transparent)
                            .clickable { onModeChanged(AiChatMode.FINANCIAL_LEDGER) }
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = if (isAppLedger) PastelKineticTokens.AmberDeep else PastelKineticTokens.TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "App Ledger",
                                color = if (isAppLedger) PastelKineticTokens.TextPrimary else PastelKineticTokens.TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isAppLedger) FontWeight.Bold else FontWeight.Medium
                            )
                            if (isAppLedger) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(PastelKineticTokens.AmberAccent)
                                )
                            }
                        }
                    }

                    // Option 2: General AI
                    val isGeneral = currentMode == AiChatMode.GENERAL
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(PastelKineticTokens.RadiusFull)
                            .background(if (isGeneral) PastelKineticTokens.SurfaceCard else Color.Transparent)
                            .clickable { onModeChanged(AiChatMode.GENERAL) }
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = if (isGeneral) PastelKineticTokens.SkyText else PastelKineticTokens.TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "General AI",
                                color = if (isGeneral) PastelKineticTokens.TextPrimary else PastelKineticTokens.TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isGeneral) FontWeight.Bold else FontWeight.Medium
                            )
                            if (isGeneral) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(PastelKineticTokens.SkyText)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Edge Intelligence Hero Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, PastelKineticTokens.RadiusCard, spotColor = Color(0x18001D33))
                    .clip(PastelKineticTokens.RadiusCard)
                    .background(
                        if (currentMode == AiChatMode.FINANCIAL_LEDGER) PastelKineticTokens.SkyFixed
                        else PastelKineticTokens.SurfaceCard
                    )
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(PastelKineticTokens.AmberAccent)
                            )
                            Text(
                                text = if (currentMode == AiChatMode.FINANCIAL_LEDGER) "LEDGER CO-PILOT" else "GENERAL FINANCIAL MENTOR",
                                color = if (currentMode == AiChatMode.FINANCIAL_LEDGER) PastelKineticTokens.SkyTextVariant else PastelKineticTokens.TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(PastelKineticTokens.SurfaceCard.copy(alpha = 0.8f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (currentMode == AiChatMode.FINANCIAL_LEDGER) Icons.Default.ReceiptLong else Icons.Default.Psychology,
                                contentDescription = "Mode",
                                tint = PastelKineticTokens.AnchorPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Text(
                        text = if (currentMode == AiChatMode.FINANCIAL_LEDGER) "On-Device Ledger AI Active" else "General AI Conversation Active",
                        color = PastelKineticTokens.TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.3).sp
                    )

                    Text(
                        text = if (currentMode == AiChatMode.FINANCIAL_LEDGER) {
                            "Directly connected to your SQLite ledger. Ask spending questions, analyze trends, or request ledger edits with 1 tap."
                        } else {
                            "100% private, zero-telemetry conversational AI. Explore personal finance strategies, budgeting advice, or general topics."
                        },
                        color = PastelKineticTokens.TextSecondary,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp
                    )

                    // Real Metrics Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(PastelKineticTokens.RadiusFull)
                                .background(PastelKineticTokens.SurfaceContainer)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Bolt, contentDescription = "Speed", tint = PastelKineticTokens.AmberDeep, modifier = Modifier.size(13.dp))
                                Text("On-Device", color = PastelKineticTokens.TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(PastelKineticTokens.RadiusFull)
                                .background(PastelKineticTokens.SurfaceContainer)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Memory, contentDescription = "Memory", tint = PastelKineticTokens.SkyText, modifier = Modifier.size(13.dp))
                                Text(modelSizeMb, color = PastelKineticTokens.TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(PastelKineticTokens.RadiusFull)
                                .background(PastelKineticTokens.AmberFixed)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(imageVector = Icons.Default.VerifiedUser, contentDescription = "Air-Gapped", tint = PastelKineticTokens.AmberText, modifier = Modifier.size(13.dp))
                                Text("Air-Gapped", color = PastelKineticTokens.AmberText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 4. Suggested Queries Strip
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "SUGGESTED FOR YOU",
                        color = PastelKineticTokens.TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier
                            .clip(PastelKineticTokens.RadiusFull)
                            .clickable { suggestionShuffleOffset++ }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Shuffle, contentDescription = "Shuffle", tint = PastelKineticTokens.AmberDeep, modifier = Modifier.size(13.dp))
                        Text("Shuffle", color = PastelKineticTokens.AmberDeep, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    displayedQueries.forEach { query ->
                        Box(
                            modifier = Modifier
                                .shadow(2.dp, PastelKineticTokens.RadiusFull, spotColor = Color(0x0C121316))
                                .clip(PastelKineticTokens.RadiusFull)
                                .background(PastelKineticTokens.SurfaceCard)
                                .clickable {
                                    chatInputText = query
                                    onSendMessage(query)
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(text = query, color = PastelKineticTokens.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 5. Starter Overview (When current session is empty)
        if (aiChatEntries.isEmpty()) {
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
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(PastelKineticTokens.AmberFixed),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (currentMode == AiChatMode.FINANCIAL_LEDGER) Icons.Default.TrendingUp else Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = PastelKineticTokens.AmberText,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = if (currentMode == AiChatMode.FINANCIAL_LEDGER) "LEDGER INTELLIGENCE SNAPSHOT" else "CONVERSATIONAL HARNESS",
                                    color = PastelKineticTokens.TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = if (currentMode == AiChatMode.FINANCIAL_LEDGER) "Ready to audit or edit your ledger" else "Ready to assist with your financial questions",
                                    color = PastelKineticTokens.TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = if (currentMode == AiChatMode.FINANCIAL_LEDGER) {
                                if (monthlyDebit > 0) {
                                    "You have logged $currencySymbol${numberFormat.format(monthlyDebit)} in total expenses this cycle. Top category: ${topCategoryName ?: "General"} ($currencySymbol${numberFormat.format(topCategorySpent)}). Ask the AI to reassign categories, spot duplicates, or log entries!"
                                } else {
                                    "Your local financial ledger is initialized. Ask the AI to inspect transactions, summarize spending, or edit categories."
                                }
                            } else {
                                "Ask general questions about the 50/30/20 budgeting rule, emergency funds, debt reduction, or everyday financial planning without sharing private bank data."
                            },
                            color = PastelKineticTokens.TextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // 6. Dynamic Conversation Thread
        items(aiChatEntries, key = { it.id }) { entry ->
            val timeString = remember(entry.submittedAt) {
                SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(entry.submittedAt))
            }
            var isThinkingExpanded by remember(entry.id) { mutableStateOf(false) }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // User Prompt Bubble
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 36.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Box(
                        modifier = Modifier
                            .clip(PastelKineticTokens.RadiusCard)
                            .background(PastelKineticTokens.AnchorPrimary)
                            .padding(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = entry.prompt,
                                color = PastelKineticTokens.OnAnchor,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                            Text(
                                text = "$timeString · Verified",
                                color = PastelKineticTokens.OnAnchorMuted,
                                fontSize = 10.sp,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }

                // AI Response & Reasoning Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 24.dp)
                        .shadow(4.dp, PastelKineticTokens.RadiusCard, spotColor = Color(0x10121316))
                        .clip(PastelKineticTokens.RadiusCard)
                        .background(PastelKineticTokens.SurfaceCard)
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Provenance Tag & Copy Action
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(PastelKineticTokens.RadiusFull)
                                    .background(PastelKineticTokens.SurfaceLow)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.FlashOn, contentDescription = "Speed", tint = PastelKineticTokens.AmberDeep, modifier = Modifier.size(13.dp))
                                    Text(
                                        text = if (entry.mode == AiChatMode.FINANCIAL_LEDGER) "On-Device AI · Ledger Grounded" else "On-Device AI · General Assistant",
                                        color = PastelKineticTokens.TextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            if (!entry.answer.isNullOrBlank()) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(PastelKineticTokens.SurfaceContainer)
                                        .clickable {
                                            clipboardManager.setText(AnnotatedString(entry.answer))
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = PastelKineticTokens.TextSecondary, modifier = Modifier.size(14.dp))
                                }
                            }
                        }

                        // Thinking Process Accordion (When thinking tokens exist)
                        if (!entry.thinking.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(PastelKineticTokens.RadiusMd)
                                    .background(PastelKineticTokens.SurfaceContainer.copy(alpha = 0.6f))
                                    .border(1.dp, PastelKineticTokens.OutlineSubtle, PastelKineticTokens.RadiusMd)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { isThinkingExpanded = !isThinkingExpanded },
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Lightbulb,
                                                contentDescription = "Thinking",
                                                tint = PastelKineticTokens.AmberDeep,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Text(
                                                text = "Thinking Process",
                                                color = PastelKineticTokens.TextPrimary,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = if (isThinkingExpanded) "(Tap to hide)" else "(Tap to inspect reasoning)",
                                                color = PastelKineticTokens.TextSecondary,
                                                fontSize = 10.5.sp
                                            )
                                        }

                                        Icon(
                                            imageVector = if (isThinkingExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = PastelKineticTokens.TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    AnimatedVisibility(
                                        visible = isThinkingExpanded,
                                        enter = fadeIn() + expandVertically(),
                                        exit = fadeOut() + shrinkVertically()
                                    ) {
                                        Column(modifier = Modifier.padding(top = 8.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(PastelKineticTokens.SurfaceCard)
                                                    .padding(10.dp)
                                            ) {
                                                Text(
                                                    text = entry.thinking,
                                                    color = PastelKineticTokens.TextSecondary,
                                                    fontSize = 11.5.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    lineHeight = 16.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Working indicator
                        if (entry.isWorking) {
                            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                            val alpha by infiniteTransition.animateFloat(
                                initialValue = 0.3f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(600),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "alpha"
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(PastelKineticTokens.AmberAccent.copy(alpha = alpha))
                                )
                                Text(
                                    text = "AI is thinking and reasoning locally on-device...",
                                    color = PastelKineticTokens.TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Error message
                        if (entry.error != null) {
                            Text(
                                text = "Notice: ${entry.error}",
                                color = PastelKineticTokens.Error,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }

                        // Formatted Answer narrative
                        if (!entry.answer.isNullOrBlank()) {
                            FormattedAiMarkdownText(
                                text = entry.answer,
                                textColor = PastelKineticTokens.TextPrimary
                            )
                        }

                        // Actionable Proposal Pod (For multiple ledger edit action types)
                        if (entry.ledgerAction != null) {
                            val action = entry.ledgerAction
                            val isRecategorize = action.type.startsWith("recategorize")
                            val isAdd = action.type == "add_expense"
                            val isSelfTransfer = action.type == "mark_self_transfer"
                            val isSpam = action.type == "mark_spam"
                            val isDelete = action.type == "delete_transaction"

                            val cardBg = when {
                                isAdd -> PastelKineticTokens.MintFixed
                                isSelfTransfer -> PastelKineticTokens.SkyFixed
                                isSpam || isDelete -> PastelKineticTokens.RoseFixed
                                else -> PastelKineticTokens.AmberFixed
                            }

                            val cardText = when {
                                isAdd -> PastelKineticTokens.MintText
                                isSelfTransfer -> PastelKineticTokens.SkyText
                                isSpam || isDelete -> PastelKineticTokens.RoseText
                                else -> PastelKineticTokens.AmberText
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(PastelKineticTokens.RadiusMd)
                                    .background(cardBg)
                                    .padding(14.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = when {
                                                isAdd -> Icons.Default.Add
                                                isSelfTransfer -> Icons.Default.Refresh
                                                isDelete -> Icons.Default.Delete
                                                isSpam -> Icons.Default.Security
                                                else -> Icons.Default.ReceiptLong
                                            },
                                            contentDescription = null,
                                            tint = cardText,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = when {
                                                isAdd -> "NEW LEDGER EXPENSE PROPOSAL"
                                                isSelfTransfer -> "SELF TRANSFER PROPOSAL"
                                                isDelete -> "DELETE TRANSACTION PROPOSAL"
                                                isSpam -> "SPAM CLASSIFICATION PROPOSAL"
                                                else -> "LEDGER RECATEGORIZATION PROPOSAL"
                                            },
                                            color = cardText,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.8.sp
                                        )
                                    }

                                    Text(
                                        text = when {
                                            isAdd -> "Log expense of $currencySymbol${action.amount ?: "0"} at \"${action.merchant ?: "Expense"}\" under \"${action.categoryName ?: action.categoryId}\". Reason: ${action.reason}"
                                            isSelfTransfer -> "Mark transaction(s) for \"${action.merchant}\" as Self-Transfer (excluded from spending totals). Reason: ${action.reason}"
                                            isDelete -> "Delete transaction for \"${action.merchant}\". Reason: ${action.reason}"
                                            isSpam -> "Flag \"${action.merchant}\" as promotional spam. Reason: ${action.reason}"
                                            else -> "Assign merchant \"${action.merchant}\" to category \"${action.categoryName ?: action.categoryId}\". Reason: ${action.reason}"
                                        },
                                        color = cardText,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )

                                    if (entry.actionStatus != null) {
                                        Text(
                                            text = entry.actionStatus,
                                            color = cardText,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Button(
                                            onClick = { onApplyLedgerAction(entry.id, 0) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = PastelKineticTokens.AnchorPrimary,
                                                contentColor = PastelKineticTokens.OnAnchor
                                            ),
                                            shape = PastelKineticTokens.RadiusFull,
                                            modifier = Modifier.fillMaxWidth().height(38.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = when {
                                                    isAdd -> "Apply: Record Expense"
                                                    isDelete -> "Apply: Delete Transaction"
                                                    isSelfTransfer -> "Apply: Mark Self Transfer"
                                                    isSpam -> "Apply: Mark as Spam"
                                                    else -> "Apply Recategorization"
                                                },
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7. Sticky Modern Chat Input Bar
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .shadow(12.dp, PastelKineticTokens.RadiusFull, spotColor = Color(0x18121316))
                    .clip(PastelKineticTokens.RadiusFull)
                    .background(PastelKineticTokens.SurfaceCard)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(PastelKineticTokens.SurfaceLow)
                            .clickable { onScanReceiptClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.DocumentScanner, contentDescription = "Receipt OCR", tint = PastelKineticTokens.TextSecondary, modifier = Modifier.size(18.dp))
                    }

                    BasicTextField(
                        value = chatInputText,
                        onValueChange = { chatInputText = it },
                        modifier = Modifier.weight(1f),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = PastelKineticTokens.TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(PastelKineticTokens.AnchorPrimary),
                        decorationBox = { innerTextField ->
                            if (chatInputText.isEmpty()) {
                                Text(
                                    text = if (currentMode == AiChatMode.FINANCIAL_LEDGER) "Ask AI to analyze or edit your ledger..." else "Ask AI anything...",
                                    color = PastelKineticTokens.TextOutline,
                                    fontSize = 13.sp
                                )
                            }
                            innerTextField()
                        }
                    )

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(PastelKineticTokens.SurfaceLow)
                            .clickable { onVoiceInputClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Mic, contentDescription = "Voice Input", tint = PastelKineticTokens.TextSecondary, modifier = Modifier.size(18.dp))
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(PastelKineticTokens.AnchorPrimary)
                            .clickable {
                                if (chatInputText.isNotBlank()) {
                                    onSendMessage(chatInputText)
                                    chatInputText = ""
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = "Send", tint = PastelKineticTokens.OnAnchor, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
