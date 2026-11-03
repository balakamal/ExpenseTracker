package com.innovatex.expensetracker.presentation.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Stitch 'Pastel Kinetic Neo-Modern' design tokens for SpendWell.
 * Project: 17901262614306867771
 * Playful Neo-Modernism, tactile depth, candy-toned pastel geometry with deep obsidian contrast anchors.
 */
@Immutable
object PastelKineticTokens {
    // Canvas & Wallpaper
    val CanvasBg = Color(0xFFFAF8FF)          // Soft Iris tint canvas
    val CanvasWallpaper = Color(0xFFE8E5FA)   // Outer shell soft tint wallpaper

    // Surface Tonal Hierarchy
    val SurfaceCard = Color(0xFFFFFFFF)       // Pure snow white card base (surface-container-lowest)
    val SurfaceLow = Color(0xFFF4F3FA)        // Soft lavender-grey (surface-container-low)
    val SurfaceContainer = Color(0xFFEEEDF4)  // Sub-container surface
    val SurfaceHigh = Color(0xFFE8E7EE)       // Recessed container (surface-container-high)
    val SurfaceHighest = Color(0xFFE2E2E9)    // Iris lavender divider / recessed pod
    val SurfaceDim = Color(0xFFDAD9E0)        // Dim surface neutral

    // Deep Obsidian Anchors (High-contrast focus)
    val AnchorPrimary = Color(0xFF121316)     // Deep Carbon Black primary buttons & active tags
    val AnchorContainer = Color(0xFF1B1B1F)   // Floating bottom bar background
    val OnAnchor = Color(0xFFFFFFFF)          // High contrast white text on dark anchors
    val OnAnchorMuted = Color(0xFF848387)     // Muted text on dark container

    // Frosted Glassmorphism Theme Tokens (Modern Floating Island)
    val GlassDockBgTop = Color(0xE61E2028)       // 90% opacity deep obsidian glass top
    val GlassDockBgBottom = Color(0xD9131418)    // 85% opacity dark obsidian glass bottom
    val GlassDockBorderTop = Color(0x4DFFFFFF)   // 30% white specular top highlight border
    val GlassDockBorderBottom = Color(0x0DFFFFFF)// Subtle light refraction along bottom
    val FloatingDockClearance = 96.dp            // Dynamic scroll clearance above navigation bars

    // Typography Ink
    val TextPrimary = Color(0xFF1A1B20)       // Deep Obsidian primary ink (on-surface)
    val TextSecondary = Color(0xFF46464B)     // Sub-labels & secondary descriptions
    val TextOutline = Color(0xFF76777B)       // Muted metadata & placeholders
    val OutlineVariant = Color(0xFFC7C6CB)    // Subtle boundary strokes
    val OutlineSubtle = OutlineVariant        // Subtle outline alias

    // Warm Amber / Honey Peach Pod (Inflow, highlights, progress)
    val AmberFixed = Color(0xFFFFDDB4)        // Honey Peach container background (secondary-fixed)
    val AmberAccent = Color(0xFFFDBA59)       // Saturated Amber active accent (secondary-fixed-dim)
    val AmberContainer = Color(0xFFFDBA59)
    val AmberText = Color(0xFF291800)         // Dark tone text on amber (on-secondary-fixed)
    val AmberTextVariant = Color(0xFF633F00)  // Subtitle text on amber
    val AmberDeep = Color(0xFF825500)         // Dark amber accent (secondary)

    // Soft Sky Glaze Pod (Outflow, edge intelligence, hardware)
    val SkyFixed = Color(0xFFCEE5FF)          // Soft Sky Glaze container (tertiary-fixed)
    val SkyAccent = Color(0xFFA7CAEF)         // Sky accent (tertiary-fixed-dim)
    val SkyHighlight = Color(0xFFB5D8FE)      // Sky highlight
    val SkyText = Color(0xFF001D33)           // Dark blue text on sky (on-tertiary-fixed)
    val SkyTextVariant = Color(0xFF254969)    // Subtitle text on sky
    val SkyDeep = Color(0xFF001D33)           // Darkest sky tone

    // Soft Iris Lilac / Lavender (Net cashflow hero banner, pacing)
    val LilacHero = Color(0xFFD8D5F6)         // Hero card background
    val LilacSurface = Color(0xFFE8E5FA)      // Lilac container
    val BlushLilac = Color(0xFFF2A8E5)        // Milestone accent

    // Soft Mint / Sage Pod (Inflow / new additions)
    val MintFixed = Color(0xFFCEF5E4)
    val MintText = Color(0xFF003822)

    // Soft Rose Pod (Spam / deletion / warnings)
    val RoseFixed = Color(0xFFFFDAD6)
    val RoseText = Color(0xFF93000A)

    // System Semantic States
    val Error = Color(0xFFBA1A1A)             // Error text & alerts
    val ErrorContainer = Color(0xFFFFDAD6)    // Soft error container
    val OnErrorContainer = Color(0xFF93000A)
    val Success = Color(0xFF1E8E3E)           // Safe / verified indicator

    // Neo-Modern Shapes & Radii
    val RadiusFull = RoundedCornerShape(9999.dp) // Stadium pill
    val RadiusXl = RoundedCornerShape(32.dp)     // Viewport / Hero cards
    val RadiusCard = RoundedCornerShape(24.dp)   // Bento cards & sheets
    val RadiusLg = RoundedCornerShape(20.dp)     // Module containers
    val RadiusMd = RoundedCornerShape(16.dp)     // Standard inner cards
    val RadiusSm = RoundedCornerShape(12.dp)     // Micro tiles & badges
    val RadiusXs = RoundedCornerShape(8.dp)      // Micro tags
    val RadiusSheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp) // Bottom sheets

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

