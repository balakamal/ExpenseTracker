package com.innovatex.expensetracker.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovatex.expensetracker.domain.model.PaymentMedium
import com.innovatex.expensetracker.presentation.theme.ApexThemeTokens

@Composable
fun IngestionChannelToggleCard(
    title: String,
    subtitle: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(ApexThemeTokens.RadiusMd)
            .background(ApexThemeTokens.BgSurface)
            .border(1.dp, ApexThemeTokens.StrokeSubtle, ApexThemeTokens.RadiusMd)
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = ApexThemeTokens.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = ApexThemeTokens.TextSecondary,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ApexThemeTokens.BgCanvas,
                    checkedTrackColor = ApexThemeTokens.AccentPrimary,
                    uncheckedThumbColor = ApexThemeTokens.TextSecondary,
                    uncheckedTrackColor = ApexThemeTokens.BgGlassPill
                )
            )
        }
    }
}

@Composable
fun PaymentMediumBadge(
    medium: PaymentMedium,
    modifier: Modifier = Modifier
) {
    val badgeColor = remember(medium) {
        runCatching { Color(android.graphics.Color.parseColor(medium.colorHex)) }
            .getOrDefault(ApexThemeTokens.AccentCobalt)
    }

    Box(
        modifier = modifier
            .clip(ApexThemeTokens.RadiusFull)
            .background(badgeColor.copy(alpha = 0.14f))
            .border(1.dp, badgeColor.copy(alpha = 0.35f), ApexThemeTokens.RadiusFull)
            .padding(horizontal = 6.5.dp, vertical = 2.dp)
    ) {
        Text(
            text = medium.displayName.uppercase(),
            color = badgeColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}
