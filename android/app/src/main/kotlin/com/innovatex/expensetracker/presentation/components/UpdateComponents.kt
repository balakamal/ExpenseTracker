package com.innovatex.expensetracker.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.OpenInBrowser
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovatex.expensetracker.domain.model.AppUpdateInfo
import com.innovatex.expensetracker.presentation.theme.ApexThemeTokens

/**
 * Animated glowing badge pill in the Top Bar showing when an update is ready.
 */
@Composable
fun UpdateBadgePill(
    latestVersion: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "badgeGlow")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier
            .clip(ApexThemeTokens.RadiusFull)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        ApexThemeTokens.AccentCobalt.copy(alpha = 0.25f * alphaAnim),
                        ApexThemeTokens.AccentPrimary.copy(alpha = 0.25f * alphaAnim)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        ApexThemeTokens.AccentCobalt.copy(alpha = alphaAnim),
                        ApexThemeTokens.AccentPrimary.copy(alpha = alphaAnim)
                    )
                ),
                shape = ApexThemeTokens.RadiusFull
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(ApexThemeTokens.RadiusFull)
                    .background(ApexThemeTokens.AccentPrimary)
            )
            Text(
                text = "v$latestVersion Update",
                color = ApexThemeTokens.TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp
            )
        }
    }
}

/**
 * Hero alert banner displayed on the dashboard when a new GitHub release is detected.
 */
@Composable
fun UpdateAlertBanner(
    updateInfo: AppUpdateInfo,
    onUpdateClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(ApexThemeTokens.RadiusMd)
            .background(
                Brush.linearGradient(
                    listOf(
                        ApexThemeTokens.BgSurface,
                        Color(0xFF161F33) // Subtle dark cobalt tint
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        ApexThemeTokens.AccentCobalt.copy(alpha = 0.6f),
                        ApexThemeTokens.AccentPrimary.copy(alpha = 0.4f)
                    )
                ),
                shape = ApexThemeTokens.RadiusMd
            )
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(ApexThemeTokens.RadiusSm)
                        .background(ApexThemeTokens.AccentCobaltTint),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NewReleases,
                        contentDescription = "New update",
                        tint = ApexThemeTokens.AccentCobalt,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "NEW VERSION AVAILABLE",
                        color = ApexThemeTokens.AccentCobalt,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Release v${updateInfo.latestVersion}",
                        color = ApexThemeTokens.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onDismissClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = ApexThemeTokens.TextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = updateInfo.releaseTitle.ifBlank { "Latest features and optimizations published to GitHub." },
                color = ApexThemeTokens.TextSecondary,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onUpdateClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ApexThemeTokens.AccentCobalt,
                        contentColor = Color.White
                    ),
                    shape = ApexThemeTokens.RadiusSm,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Get Update",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Comprehensive modal bottom sheet detailing release changes and providing direct APK download link.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateDetailsBottomSheet(
    updateInfo: AppUpdateInfo,
    onDismiss: () -> Unit,
    onDownloadApk: (String) -> Unit,
    onOpenReleasePage: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                        .size(44.dp)
                        .clip(ApexThemeTokens.RadiusMd)
                        .background(ApexThemeTokens.AccentCobaltTint),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NewReleases,
                        contentDescription = null,
                        tint = ApexThemeTokens.AccentCobalt,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "GitHub Release",
                        color = ApexThemeTokens.TextTertiary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Version ${updateInfo.latestVersion}",
                        color = ApexThemeTokens.TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Version diff pill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ApexThemeTokens.RadiusSm)
                    .background(ApexThemeTokens.BgCanvas)
                    .border(1.dp, ApexThemeTokens.StrokeSubtle, ApexThemeTokens.RadiusSm)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "INSTALLED",
                            color = ApexThemeTokens.TextTertiary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "v${updateInfo.currentVersion}",
                            color = ApexThemeTokens.TextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = "➔",
                        color = ApexThemeTokens.AccentPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "LATEST (GITHUB)",
                            color = ApexThemeTokens.AccentCobalt,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "v${updateInfo.latestVersion}",
                            color = ApexThemeTokens.AccentPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Release Notes
            Text(
                text = "WHAT'S CHANGED",
                color = ApexThemeTokens.TextTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .clip(ApexThemeTokens.RadiusSm)
                    .background(ApexThemeTokens.BgCanvas)
                    .border(1.dp, ApexThemeTokens.StrokeSubtle, ApexThemeTokens.RadiusSm)
                    .padding(14.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = updateInfo.releaseNotes.ifBlank { "Performance improvements and bug fixes." },
                    color = ApexThemeTokens.TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Button(
                onClick = {
                    onDownloadApk(updateInfo.downloadApkUrl ?: updateInfo.releasePageUrl)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ApexThemeTokens.AccentPrimary,
                    contentColor = ApexThemeTokens.BgCanvas
                ),
                shape = ApexThemeTokens.RadiusMd,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Download APK Update",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = { onOpenReleasePage(updateInfo.releasePageUrl) },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = ApexThemeTokens.TextSecondary
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, ApexThemeTokens.StrokeHighlight),
                shape = ApexThemeTokens.RadiusMd,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInBrowser,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "View Release on GitHub",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
