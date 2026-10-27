package com.innovatex.expensetracker.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovatex.expensetracker.presentation.theme.ApexThemeTokens

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ManageSendersBottomSheet(
    spamSenders: Set<String>,
    trustedSenders: Set<String>,
    onUnblockSender: (String) -> Unit,
    onAddSpamSender: (String) -> Unit,
    onUntrustSender: (String) -> Unit,
    onAddTrustedSender: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Blocked, 1: Trusted
    var newSenderInput by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = ApexThemeTokens.BgCanvas,
        shape = ApexThemeTokens.RadiusSheet,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(ApexThemeTokens.RadiusFull)
                        .background(ApexThemeTokens.AccentCobaltTint)
                        .border(1.dp, ApexThemeTokens.AccentCobaltBorder, ApexThemeTokens.RadiusFull),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = ApexThemeTokens.AccentCobalt,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SENDER MANAGEMENT",
                        color = ApexThemeTokens.TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Spam & Trusted Senders",
                        color = ApexThemeTokens.TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = ApexThemeTokens.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = ApexThemeTokens.BgSurfaceElevated,
                contentColor = ApexThemeTokens.TextPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = if (selectedTab == 0) ApexThemeTokens.AccentDanger else ApexThemeTokens.AccentPrimary,
                        height = 3.dp
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ApexThemeTokens.RadiusSm)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0; newSenderInput = "" },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (selectedTab == 0) ApexThemeTokens.AccentDanger else ApexThemeTokens.TextMuted
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Blocked Spam (${spamSenders.size})",
                                fontSize = 12.5.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 0) ApexThemeTokens.AccentDanger else ApexThemeTokens.TextSecondary
                            )
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1; newSenderInput = "" },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (selectedTab == 1) ApexThemeTokens.AccentPrimary else ApexThemeTokens.TextMuted
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Trusted (${trustedSenders.size})",
                                fontSize = 12.5.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 1) ApexThemeTokens.AccentPrimary else ApexThemeTokens.TextSecondary
                            )
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Add Sender Input Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ApexThemeTokens.RadiusSm)
                    .background(ApexThemeTokens.BgSurfaceElevated)
                    .border(1.dp, ApexThemeTokens.StrokeSubtle, ApexThemeTokens.RadiusSm)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = newSenderInput,
                    onValueChange = { newSenderInput = it },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = ApexThemeTokens.TextPrimary,
                        fontSize = 13.sp
                    ),
                    cursorBrush = SolidColor(ApexThemeTokens.AccentPrimary),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (newSenderInput.isEmpty()) {
                            Text(
                                text = if (selectedTab == 0) "e.g. AD-RUMMY, +919876543210..." else "e.g. AD-HDFCBK, AX-ICICIB...",
                                color = ApexThemeTokens.TextMuted,
                                fontSize = 12.sp
                            )
                        }
                        innerTextField()
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        val clean = newSenderInput.trim()
                        if (clean.isNotBlank()) {
                            if (selectedTab == 0) onAddSpamSender(clean) else onAddTrustedSender(clean)
                            newSenderInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTab == 0) ApexThemeTokens.AccentDanger else ApexThemeTokens.AccentPrimary,
                        contentColor = Color.Black
                    ),
                    shape = ApexThemeTokens.RadiusSm,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (selectedTab == 0) "Block" else "Trust",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Senders Chip List
            val currentList = if (selectedTab == 0) spamSenders.toList() else trustedSenders.toList()

            if (currentList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ApexThemeTokens.RadiusMd)
                        .background(ApexThemeTokens.BgSurfaceElevated)
                        .border(1.dp, ApexThemeTokens.StrokeSubtle, ApexThemeTokens.RadiusMd)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (selectedTab == 0) "No senders currently blocked." else "No senders explicitly trusted yet.",
                        color = ApexThemeTokens.TextMuted,
                        fontSize = 13.sp
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (sender in currentList) {
                            SenderChip(
                                sender = sender,
                                isSpam = selectedTab == 0,
                                onRemove = {
                                    if (selectedTab == 0) onUnblockSender(sender) else onUntrustSender(sender)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SenderChip(
    sender: String,
    isSpam: Boolean,
    onRemove: () -> Unit
) {
    val bgColor = if (isSpam) ApexThemeTokens.AccentDangerTint else ApexThemeTokens.AccentPrimaryTint
    val borderColor = if (isSpam) ApexThemeTokens.AccentDangerBorder else ApexThemeTokens.AccentPrimaryBorder
    val textColor = if (isSpam) ApexThemeTokens.AccentDanger else ApexThemeTokens.AccentPrimary

    Row(
        modifier = Modifier
            .clip(ApexThemeTokens.RadiusFull)
            .background(bgColor)
            .border(1.dp, borderColor, ApexThemeTokens.RadiusFull)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = sender,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Remove",
            tint = textColor,
            modifier = Modifier
                .size(14.dp)
                .clickable { onRemove() }
        )
    }
}
