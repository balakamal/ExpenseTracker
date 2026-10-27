package com.innovatex.expensetracker.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovatex.expensetracker.domain.model.SuspectedSpamItem
import com.innovatex.expensetracker.presentation.theme.ApexThemeTokens
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpamReviewBottomSheet(
    suspectedItems: List<SuspectedSpamItem>,
    onConfirmSpam: (SuspectedSpamItem) -> Unit,
    onTrustSender: (SuspectedSpamItem) -> Unit,
    onConfirmAll: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }

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
                        .background(ApexThemeTokens.AccentSecondaryTint)
                        .border(1.dp, ApexThemeTokens.AccentSecondaryBorder, ApexThemeTokens.RadiusFull),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = ApexThemeTokens.AccentSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "REVIEW SUSPECTED SPAM",
                            color = ApexThemeTokens.TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        if (suspectedItems.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(ApexThemeTokens.RadiusFull)
                                    .background(ApexThemeTokens.AccentDangerTint)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${suspectedItems.size} Senders",
                                    color = ApexThemeTokens.AccentDanger,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "User Confirmation Required",
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

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "The system identified messages with promotional or unverified traits. No transactions are deleted without your confirmation.",
                color = ApexThemeTokens.TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (suspectedItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ApexThemeTokens.RadiusMd)
                        .background(ApexThemeTokens.BgSurfaceElevated)
                        .border(1.dp, ApexThemeTokens.StrokeSubtle, ApexThemeTokens.RadiusMd)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = ApexThemeTokens.AccentPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No Suspected Spam",
                            color = ApexThemeTokens.TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "All senders and transactions appear legitimate.",
                            color = ApexThemeTokens.TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(suspectedItems, key = { it.sender }) { item ->
                        SuspectedSpamCard(
                            item = item,
                            numberFormat = numberFormat,
                            onConfirmSpam = { onConfirmSpam(item) },
                            onTrustSender = { onTrustSender(item) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onConfirmAll,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ApexThemeTokens.AccentDanger,
                            contentColor = Color.Black
                        ),
                        shape = ApexThemeTokens.RadiusSm,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Block All (${suspectedItems.size})",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    OutlinedButton(
                        onClick = onDismissRequest,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ApexThemeTokens.TextSecondary
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(ApexThemeTokens.StrokeSubtle)
                        ),
                        shape = ApexThemeTokens.RadiusSm
                    ) {
                        Text(text = "Dismiss", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SuspectedSpamCard(
    item: SuspectedSpamItem,
    numberFormat: NumberFormat,
    onConfirmSpam: () -> Unit,
    onTrustSender: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ApexThemeTokens.RadiusMd)
            .background(ApexThemeTokens.BgSurfaceElevated)
            .border(1.dp, ApexThemeTokens.StrokeSubtle, ApexThemeTokens.RadiusMd)
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row: Sender and Reason
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(ApexThemeTokens.RadiusFull)
                        .background(ApexThemeTokens.AccentCobaltTint)
                        .border(1.dp, ApexThemeTokens.AccentCobaltBorder, ApexThemeTokens.RadiusFull)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = item.sender,
                        color = ApexThemeTokens.AccentCobalt,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(ApexThemeTokens.RadiusFull)
                        .background(ApexThemeTokens.AccentSecondaryTint)
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = item.reason,
                        color = ApexThemeTokens.AccentSecondary,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "₹${numberFormat.format(item.totalAmount)}",
                    color = ApexThemeTokens.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${item.transactionCount} transaction(s) recorded from this sender",
                color = ApexThemeTokens.TextMuted,
                fontSize = 11.sp
            )

            if (!item.sampleSnippet.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ApexThemeTokens.RadiusSm)
                        .background(ApexThemeTokens.BgCanvas)
                        .padding(9.dp)
                ) {
                    Text(
                        text = "\"${item.sampleSnippet}\"",
                        color = ApexThemeTokens.TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onTrustSender,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ApexThemeTokens.AccentPrimary
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(ApexThemeTokens.AccentPrimaryBorder)
                    ),
                    shape = ApexThemeTokens.RadiusSm,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Trust / Keep", fontSize = 11.5.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onConfirmSpam,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ApexThemeTokens.AccentDanger,
                        contentColor = Color.Black
                    ),
                    shape = ApexThemeTokens.RadiusSm,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Block & Delete", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
