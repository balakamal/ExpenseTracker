package com.innovatex.expensetracker.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovatex.expensetracker.presentation.theme.PastelKineticTokens

data class PastelNavItemData(
    val label: String,
    val icon: ImageVector,
    val index: Int
)

val SpendWellNavItems = listOf(
    PastelNavItemData("Ledger", Icons.Default.AccountBalanceWallet, 0),
    PastelNavItemData("Calendar", Icons.Default.CalendarMonth, 1),
    PastelNavItemData("Gemma", Icons.Default.AutoAwesome, 2),
    PastelNavItemData("Enclave", Icons.Default.Tune, 3)
)

/**
 * Floating island bottom navigation dock inspired by Stitch Pastel Kinetic Neo-Modern.
 * Suspended obsidian capsule with active pure white stadium pill transition.
 */
@Composable
fun PastelFloatingNavDock(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 380.dp)
                .fillMaxWidth()
                .height(64.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = PastelKineticTokens.RadiusFull,
                    spotColor = Color(0x40121316),
                    ambientColor = Color(0x20121316)
                )
                .clip(PastelKineticTokens.RadiusFull)
                .background(PastelKineticTokens.AnchorContainer)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SpendWellNavItems.forEach { item ->
                val isSelected = selectedTab == item.index

                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.0f else 0.96f,
                    animationSpec = PastelKineticTokens.springSnappy(),
                    label = "dockItemScale"
                )

                val pillBgColor by animateColorAsState(
                    targetValue = if (isSelected) PastelKineticTokens.SurfaceCard else Color.Transparent,
                    animationSpec = PastelKineticTokens.springPhysics(),
                    label = "dockPillBg"
                )

                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) PastelKineticTokens.AnchorPrimary else Color(0xFFC7C6CA),
                    animationSpec = PastelKineticTokens.springSnappy(),
                    label = "dockPillContent"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .scale(scale)
                        .clip(PastelKineticTokens.RadiusFull)
                        .background(pillBgColor)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onTabSelected(item.index)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = contentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = item.label,
                            color = contentColor,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}

