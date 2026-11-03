package com.innovatex.expensetracker.presentation.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovatex.expensetracker.presentation.theme.PastelKineticTokens
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PastelAiAssistantView(
    aiChatEntries: List<AiChatEntry> = emptyList(),
    currencySymbol: String = "₹",
    isAiModelReady: Boolean = true,
    aiModelSizeBytes: Long = 1400000000L,
    topCategoryName: String? = null,
    topCategorySpent: Double = 0.0,
    monthlyDebit: Double = 0.0,
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

    val queryPool = remember(topCategoryName) {
        listOf(
            "🍔 ${topCategoryName ?: "Dining"} spend vs last week?",
            "💡 How to preserve $currencySymbol 500 this weekend?",
            "📊 Summarize monthly category breakdown",
            "⚡ Check for duplicate subscription charges",
            "📈 Forecast end-of-month balance",
            "🔍 Identify my largest single expense this month"
        )
    }

    val displayedQueries = remember(suggestionShuffleOffset, queryPool) {
        val count = queryPool.size
        listOf(
            queryPool[(suggestionShuffleOffset) % count],
            queryPool[(suggestionShuffleOffset + 1) % count],
            queryPool[(suggestionShuffleOffset + 2) % count]
        )
    }

    // Model size readout
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
                            imageVector = Icons.Default.AutoAwesome,
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
                            text = "Gemma Assistant",
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

        // 2. Edge Intelligence Hero Banner (Sky Glaze)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, PastelKineticTokens.RadiusCard, spotColor = Color(0x18001D33))
                    .clip(PastelKineticTokens.RadiusCard)
                    .background(PastelKineticTokens.SkyFixed)
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
                                text = "EDGE INTELLIGENCE",
                                color = PastelKineticTokens.SkyTextVariant,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PastelKineticTokens.SurfaceCard.copy(alpha = 0.8f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Intelligence",
                                tint = PastelKineticTokens.SkyText,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Text(
                        text = if (isAiModelReady) "Gemma 2B Local Core Active" else "Gemma Core Ready for Download",
                        color = PastelKineticTokens.SkyText,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.4).sp
                    )
                    Text(
                        text = "100% Private, zero telemetry. High-speed financial deduction directly on your device silicon.",
                        color = PastelKineticTokens.SkyTextVariant,
                        fontSize = 13.sp,
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
                                .background(PastelKineticTokens.SurfaceCard.copy(alpha = 0.8f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Bolt, contentDescription = "Speed", tint = PastelKineticTokens.SkyText, modifier = Modifier.size(13.dp))
                                Text("On-Device", color = PastelKineticTokens.SkyText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(PastelKineticTokens.RadiusFull)
                                .background(PastelKineticTokens.SurfaceCard.copy(alpha = 0.8f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Memory, contentDescription = "Memory", tint = PastelKineticTokens.SkyText, modifier = Modifier.size(13.dp))
                                Text(modelSizeMb, color = PastelKineticTokens.SkyText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

        // 3. Suggested Queries Strip
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("SUGGESTED QUERIES", color = PastelKineticTokens.TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
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

        // 4. Starter Overview (When no chat history exists)
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
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = PastelKineticTokens.AmberText,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text("LEDGER INTELLIGENCE SNAPSHOT", color = PastelKineticTokens.TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Text("Ready to assist with your ledger", color = PastelKineticTokens.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Text(
                            text = if (monthlyDebit > 0) {
                                "You have logged $currencySymbol${numberFormat.format(monthlyDebit)} in total expenses this cycle. Top category: ${topCategoryName ?: "General"} ($currencySymbol${numberFormat.format(topCategorySpent)})."
                            } else {
                                "Your local financial ledger is initialized. Ask Gemma to categorize items, summarize bills, or detect repeat charges."
                            },
                            color = PastelKineticTokens.TextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // 5. Dynamic Conversation Thread
        items(aiChatEntries, key = { it.id }) { entry ->
            val timeString = remember(entry.submittedAt) {
                SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(entry.submittedAt))
            }

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

                // AI Response or Reasoning State Card
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
                        // Provenance Tag
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
                                    Text("Gemma 2B · On-device execution", color = PastelKineticTokens.TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
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
                                    text = "Gemma is reasoning locally on-device...",
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

                        // Answer narrative
                        if (!entry.answer.isNullOrBlank()) {
                            Text(
                                text = entry.answer,
                                color = PastelKineticTokens.TextPrimary,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }

                        // Actionable Proposal Pod
                        if (entry.ledgerAction != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(PastelKineticTokens.RadiusMd)
                                    .background(PastelKineticTokens.AmberFixed)
                                    .padding(14.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null, tint = PastelKineticTokens.AmberText, modifier = Modifier.size(16.dp))
                                        Text(
                                            text = "LEDGER RECATEGORIZATION PROPOSAL",
                                            color = PastelKineticTokens.AmberTextVariant,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.8.sp
                                        )
                                    }

                                    Text(
                                        text = "Assign merchant \"${entry.ledgerAction.merchant}\" to category \"${entry.ledgerAction.categoryId}\". Reason: ${entry.ledgerAction.reason}",
                                        color = PastelKineticTokens.AmberText,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )

                                    if (entry.actionStatus != null) {
                                        Text(
                                            text = entry.actionStatus,
                                            color = PastelKineticTokens.AmberTextVariant,
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
                                            Text("Apply Recategorization", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Sticky Modern Chat Input Bar
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
                                Text("Ask Gemma about your finances...", color = PastelKineticTokens.TextOutline, fontSize = 13.sp)
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
