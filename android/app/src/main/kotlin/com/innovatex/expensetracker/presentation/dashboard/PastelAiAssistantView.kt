package com.innovatex.expensetracker.presentation.dashboard

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RamenDining
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovatex.expensetracker.presentation.theme.PastelKineticTokens
import java.text.NumberFormat
import java.util.Locale

@Composable
fun PastelAiAssistantView(
    currencySymbol: String = "₹",
    isAiModelReady: Boolean = true,
    aiModelSizeBytes: Long = 1400000000L,
    onSendMessage: (String) -> Unit = {},
    onScanReceiptClick: () -> Unit = {},
    onExportLedgerClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var chatInputText by remember { mutableStateOf("") }
    val suggestedQueries = remember {
        listOf(
            "🍔 Dining spend vs last week?",
            "💡 Save $currencySymbol 100 this weekend",
            "📊 Summarize grocery trends",
            "⚡ Detect duplicate subscriptions"
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PastelKineticTokens.CanvasBg),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 100.dp),
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
                        text = "Gemma 2B Local Core",
                        color = PastelKineticTokens.SkyText,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.4).sp
                    )
                    Text(
                        text = "100% Private, zero telemetry. High-speed financial deduction directly on your silicon.",
                        color = PastelKineticTokens.SkyTextVariant,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    // Metrics Badges
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
                                Text("320ms avg", color = PastelKineticTokens.SkyText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                                Text("1.4 GB / 32 GB", color = PastelKineticTokens.SkyText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
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
                    suggestedQueries.forEach { query ->
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

        // 4. User Query Message Bubble
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 40.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .clip(PastelKineticTokens.RadiusCard)
                        .background(PastelKineticTokens.AnchorContainer)
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "How much did I spend on dining this week compared to last week?",
                            color = PastelKineticTokens.OnAnchor,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                        Text(
                            text = "9:41 AM · Verified",
                            color = PastelKineticTokens.OnAnchorMuted,
                            fontSize = 10.sp,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }
        }

        // 5. AI Multi-Modal Structured Response Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, PastelKineticTokens.RadiusCard, spotColor = Color(0x10121316))
                    .clip(PastelKineticTokens.RadiusCard)
                    .background(PastelKineticTokens.SurfaceCard)
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Neural Provenance Tag
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
                                Text("Processed locally in 280ms · 0 bytes outbound", color = PastelKineticTokens.TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(PastelKineticTokens.SurfaceContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = PastelKineticTokens.TextSecondary, modifier = Modifier.size(14.dp))
                        }
                    }

                    // Synthesis Narrative
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "You spent $currencySymbol 142.50 on dining this week.",
                            color = PastelKineticTokens.TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "That is 18% lower than last week ($currencySymbol 174.00). You're on pace to preserve your weekend dining budget!",
                            color = PastelKineticTokens.TextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }

                    // Interactive Dual Bar Velocity Chart
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(PastelKineticTokens.RadiusMd)
                            .background(PastelKineticTokens.SurfaceLow)
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Daily Food Velocity", color = PastelKineticTokens.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(PastelKineticTokens.AmberAccent))
                                        Text("This Week", color = PastelKineticTokens.TextSecondary, fontSize = 10.sp)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(PastelKineticTokens.SurfaceHighest))
                                        Text("Prev", color = PastelKineticTokens.TextSecondary, fontSize = 10.sp)
                                    }
                                }
                            }

                            // 7 Day Dual Bars
                            val days = listOf("M", "T", "W", "T", "F", "S", "S")
                            val currentRatios = listOf(0.25f, 0.40f, 0.30f, 0.50f, 0.95f, 0.45f, 0.20f)
                            val prevRatios = listOf(0.35f, 0.55f, 0.45f, 0.60f, 0.85f, 0.70f, 0.40f)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                days.forEachIndexed { i, d ->
                                    val isPeak = i == 4 // Friday
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Bottom,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                                            verticalAlignment = Alignment.Bottom,
                                            modifier = Modifier.height(80.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .width(7.dp)
                                                    .height((prevRatios[i] * 76).dp)
                                                    .clip(PastelKineticTokens.RadiusFull)
                                                    .background(PastelKineticTokens.SurfaceHighest)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .width(7.dp)
                                                    .height((currentRatios[i] * 76).dp)
                                                    .clip(PastelKineticTokens.RadiusFull)
                                                    .background(if (isPeak) PastelKineticTokens.AmberDeep else PastelKineticTokens.AmberAccent)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = d,
                                            color = if (isPeak) PastelKineticTokens.TextPrimary else PastelKineticTokens.TextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = if (isPeak) FontWeight.ExtraBold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Peach SpendWell Insight Pod
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(PastelKineticTokens.RadiusMd)
                            .background(PastelKineticTokens.AmberFixed)
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(PastelKineticTokens.AmberAccent.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.RamenDining, contentDescription = "Ramen", tint = PastelKineticTokens.AmberText, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text("PEAK OUTLIER", color = PastelKineticTokens.AmberTextVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Text(
                                    text = "$currencySymbol 48.20 at Ramen Bar on Friday accounted for 34% of this week's food ledger.",
                                    color = PastelKineticTokens.AmberText,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    // Quick Action Dock
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(PastelKineticTokens.RadiusFull)
                                .background(PastelKineticTokens.SurfaceLow)
                                .clickable { onExportLedgerClick() }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = "View Ledger", tint = PastelKineticTokens.TextPrimary, modifier = Modifier.size(16.dp))
                                Text("View Food Ledger", color = PastelKineticTokens.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                            .background(PastelKineticTokens.SurfaceLow),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Mic, contentDescription = "Voice", tint = PastelKineticTokens.TextSecondary, modifier = Modifier.size(18.dp))
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

