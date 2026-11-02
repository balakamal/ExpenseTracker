package com.innovatex.expensetracker.presentation.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Backward-compatible tokens mapped directly to Stitch 'Pastel Kinetic Neo-Modern' for SpendWell.
 * Project: 17901262614306867771
 */
@Immutable
object ApexThemeTokens {
    // Canvas & Surface
    val BgCanvas = PastelKineticTokens.CanvasBg
    val BgSurface = PastelKineticTokens.SurfaceCard
    val BgSurfaceElevated = PastelKineticTokens.SurfaceLow
    val BgSurfaceCard = PastelKineticTokens.SurfaceCard
    val BgGlassPill = PastelKineticTokens.SurfaceContainer
    val BgGlassPillHover = PastelKineticTokens.SurfaceHigh
    val BgHighContrast = PastelKineticTokens.AnchorPrimary
    val TextHighContrast = PastelKineticTokens.OnAnchor

    // Live Status Badge
    val BgLive = PastelKineticTokens.AmberAccent
    val TextLive = PastelKineticTokens.AmberText

    // Ambient Tint
    val MeshA = Color(0x1ADBDEFF)
    val MeshB = Color(0x1AFDE0B0)
    val MeshC = Color(0x1AE9E5FA)

    // Hairline Borders & Dividers
    val StrokeSubtle = PastelKineticTokens.OutlineVariant
    val StrokeHighlight = PastelKineticTokens.SurfaceHighest
    val StrokeActive = PastelKineticTokens.AnchorPrimary

    // Range Highlights
    val RangeConnectorBg = PastelKineticTokens.SkyFixed
    val RangeConnectorMint = PastelKineticTokens.AmberFixed

    // Accents
    val AccentPrimary = PastelKineticTokens.AnchorPrimary
    val AccentPrimaryHover = Color(0xFF26272C)
    val AccentPrimaryTint = PastelKineticTokens.SurfaceLow
    val AccentPrimaryBorder = PastelKineticTokens.OutlineVariant

    val AccentSecondary = PastelKineticTokens.AmberAccent
    val AccentSecondaryTint = PastelKineticTokens.AmberFixed
    val AccentSecondaryBorder = PastelKineticTokens.AmberAccent

    val AccentCobalt = PastelKineticTokens.SkyAccent
    val AccentCobaltHover = PastelKineticTokens.SkyHighlight
    val AccentCobaltTint = PastelKineticTokens.SkyFixed
    val AccentCobaltBorder = PastelKineticTokens.SkyAccent

    val AccentDanger = PastelKineticTokens.Error
    val AccentDangerTint = PastelKineticTokens.ErrorContainer
    val AccentDangerBorder = PastelKineticTokens.Error
    val AccentExpense = PastelKineticTokens.AnchorPrimary

    // Editorial Typography
    val TextPrimary = PastelKineticTokens.TextPrimary
    val TextSecondary = PastelKineticTokens.TextSecondary
    val TextTertiary = PastelKineticTokens.TextOutline
    val TextMuted = PastelKineticTokens.OutlineVariant

    // Squircles & Radii
    val RadiusFull = PastelKineticTokens.RadiusFull
    val RadiusXs = PastelKineticTokens.RadiusXs
    val RadiusSm = PastelKineticTokens.RadiusSm
    val RadiusMd = PastelKineticTokens.RadiusMd
    val RadiusCard = PastelKineticTokens.RadiusCard
    val RadiusLg = PastelKineticTokens.RadiusLg
    val RadiusSheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

    // Physics
    fun <T> springPhysics() = PastelKineticTokens.springPhysics<T>()
    fun <T> springSnappy() = PastelKineticTokens.springSnappy<T>()
}
