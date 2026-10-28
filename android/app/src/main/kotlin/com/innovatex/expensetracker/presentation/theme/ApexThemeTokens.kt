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
    // Corbel FinTech Surfaces & Canvas
    val BgCanvas = Color(0xFF0C1216)            // Deep slate-charcoal canvas (--bg: #0c1216)
    val BgSurface = Color(0xFF172129)           // Elevated dark card panel (--surface: #172129)
    val BgSurfaceElevated = Color(0xFF1A242B)   // Floating toolbars & sheets (--float: #1a242b)
    val BgSurfaceCard = Color(0xCC16222A)       // Frosted glass card revealing ambient mesh glow
    val BgGlassPill = Color(0x14FFFFFF)         // Frosted glass pill (--glass-2: #ffffff16)
    val BgGlassPillHover = Color(0x24FFFFFF)    // Interactive hover pill
    val BgHighContrast = Color(0xFFE8EEF1)      // Corbel signature active pill (--pill: #e8eef1)
    val TextHighContrast = Color(0xFF0F171C)    // High contrast ink text on pill (--on-pill: #0f171c)

    // Corbel Live Status Badge
    val BgLive = Color(0xFF6FD3B0)              // Corbel solid mint live badge (--mint / --up: #6fd3b0)
    val TextLive = Color(0xFF0F171C)            // Corbel deep ink text on live badge

    // Corbel Ambient Mesh Glows (from official CSS)
    val MeshA = Color(0x52468CC8) // Ocean Sky ambient glow (--mesh-a: #468cc852)
    val MeshB = Color(0x3D2EA080) // Mint Emerald ambient glow (--mesh-b: #2ea0803d)
    val MeshC = Color(0x30E6785F) // Coral Peach ambient glow (--mesh-c: #e6785f26)

    // Hairline Borders & Dividers
    val StrokeSubtle = Color(0x1AFFFFFF)   // Subtle hairline frosted glass rim (--rim: #ffffff13)
    val StrokeHighlight = Color(0x2EFFFFFF) // Active border (--line-2: #ffffff29)
    val StrokeActive = Color(0x40FFFFFF)

    // Calendar Range Highlight Connectors
    val RangeConnectorBg = Color(0x2B79B6E3)    // Soft sky connecting range band
    val RangeConnectorMint = Color(0x2B6FD3B0)  // Soft mint connecting range band

    // Corbel Mint (Credit, Inflow, Positive Performance)
    val AccentPrimary = Color(0xFF6FD3B0)
    val AccentPrimaryHover = Color(0xFF5FCAA8)
    val AccentPrimaryTint = Color(0x216FD3B0)   // 13% soft badge bg
    val AccentPrimaryBorder = Color(0x4D6FD3B0) // 30% badge border

    // Corbel Lemon / Gold (Highlights, Targets)
    val AccentSecondary = Color(0xFFE7B552)
    val AccentSecondaryTint = Color(0x21E7B552)
    val AccentSecondaryBorder = Color(0x4DE7B552)

    // Corbel Ocean Sky (Branding, Navigation, Information)
    val AccentCobalt = Color(0xFF79B6E3)
    val AccentCobaltHover = Color(0xFF8EC3EA)
    val AccentCobaltTint = Color(0x2179B6E3)
    val AccentCobaltBorder = Color(0x4D79B6E3)

    // Corbel Coral (Debits, Outflows, Expenses)
    val AccentDanger = Color(0xFFF2937C)
    val AccentDangerTint = Color(0x21F2937C)
    val AccentDangerBorder = Color(0x4DF2937C)
    val AccentExpense = AccentDanger

    // Corbel Neutral Editorial Typography
    val TextPrimary = Color(0xFFE8EEF1)
    val TextSecondary = Color(0xFFA7B3BB)
    val TextTertiary = Color(0xFF8D9AA3)
    val TextMuted = Color(0xFF5F6D78)

    // Corbel Squircles & Radii
    val RadiusFull = RoundedCornerShape(9999.dp)
    val RadiusXs = RoundedCornerShape(8.dp)
    val RadiusSm = RoundedCornerShape(14.dp)
    val RadiusMd = RoundedCornerShape(18.dp)
    val RadiusCard = RoundedCornerShape(22.dp)
    val RadiusLg = RoundedCornerShape(24.dp)
    val RadiusSheet = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)

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
