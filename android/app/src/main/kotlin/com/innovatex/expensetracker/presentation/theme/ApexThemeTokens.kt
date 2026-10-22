package com.innovatex.expensetracker.presentation.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Bespoke Dribbble-crafted FinTech tokens based on Apex Finance.
 * Deep Obsidian canvas with rich organic accents (Forest Emerald, Champagne Gold, Royal Cobalt).
 */
@Immutable
object ApexThemeTokens {
    // Canvas & Elevation Surfaces
    val BgCanvas = Color(0xFF090B10)
    val BgSurface = Color(0xFF12151F)
    val BgSurfaceElevated = Color(0xFF181C28)
    val BgSurfaceCard = Color(0xE0121621) // 88% alpha
    val BgGlassPill = Color(0x0DFFFFFF)   // 5% alpha
    val BgGlassPillHover = Color(0x17FFFFFF) // 9% alpha

    // Strokes & Borders (Micro-Contrast)
    val StrokeSubtle = Color(0x12FFFFFF)   // 7% alpha
    val StrokeHighlight = Color(0x24FFFFFF) // 14% alpha
    val StrokeActive = Color(0x3DFFFFFF)   // 24% alpha

    // Curated Primary Palette: Forest Emerald (Credit, Positive Balance, Wealth)
    val AccentPrimary = Color(0xFF10B981)
    val AccentPrimaryHover = Color(0xFF059669)
    val AccentPrimaryTint = Color(0x1F10B981)   // 12% alpha
    val AccentPrimaryBorder = Color(0x4710B981) // 28% alpha

    // Curated Secondary Palette: Champagne Gold / Amber (Budget Warnings, Targets)
    val AccentSecondary = Color(0xFFF59E0B)
    val AccentSecondaryTint = Color(0x1FF59E0B)   // 12% alpha
    val AccentSecondaryBorder = Color(0x47F59E0B) // 28% alpha

    // Royal Cobalt (Actions, Multi-Channel Badges, Analytics)
    val AccentCobalt = Color(0xFF3B82F6)
    val AccentCobaltHover = Color(0xFF2563EB)
    val AccentCobaltTint = Color(0x1F3B82F6)   // 12% alpha
    val AccentCobaltBorder = Color(0x473B82F6) // 28% alpha

    // Muted Rosewood (Debits, Expenses, Outflows)
    val AccentDanger = Color(0xFFF43F5E)
    val AccentDangerTint = Color(0x1FF43F5E)   // 12% alpha
    val AccentDangerBorder = Color(0x42F43F5E) // 26% alpha

    // Neutral Editorial Typography
    val TextPrimary = Color(0xFFF8FAFC)
    val TextSecondary = Color(0xFF94A3B8)
    val TextTertiary = Color(0xFF64748B)
    val TextMuted = Color(0xFF475569)

    // Material 3 Expressive Squircles & Radii
    val RadiusFull = RoundedCornerShape(9999.dp)
    val RadiusSm = RoundedCornerShape(14.dp)
    val RadiusMd = RoundedCornerShape(20.dp)
    val RadiusLg = RoundedCornerShape(28.dp)

    // Physics-Based Spring Animations
    fun <T> springPhysics() = spring<T>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    fun <T> springSnappy() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
}
