package com.innovatex.spendwell.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Stitch Official Tokens (Project 6535465419424087375)
// Primary (Google Cobalt Blue)
val CobaltPrimary = Color(0xFF1955C5)
val OnPrimary = Color(0xFFFFFFFF)
val CobaltPrimaryContainer = Color(0xFF3D6FDF)
val OnPrimaryContainer = Color(0xFFFEFCFF)
val PrimaryFixed = Color(0xFFDAE2FF)
val OnPrimaryFixed = Color(0xFF001848)
val PrimaryFixedDim = Color(0xFFB2C5FF)
val OnPrimaryFixedVariant = Color(0xFF0040A2)

// Secondary (Emerald Green for Inflow / Verified / Credited)
val EmeraldSecondary = Color(0xFF006C4C)
val OnSecondary = Color(0xFFFFFFFF)
val EmeraldSecondaryContainer = Color(0xFF85F8C6)
val OnSecondaryContainer = Color(0xFF007351)
val SecondaryFixed = Color(0xFF85F8C6)
val OnSecondaryFixed = Color(0xFF002115)
val SecondaryFixedDim = Color(0xFF68DBAB)
val OnSecondaryFixedVariant = Color(0xFF005138)

// Tertiary (Crimson for Outflow / Expenses / High alerts)
val CrimsonTertiary = Color(0xFFB8083A)
val OnTertiary = Color(0xFFFFFFFF)
val CrimsonTertiaryContainer = Color(0xFFDB2D50)
val OnTertiaryContainer = Color(0xFFFFF7F7)
val TertiaryFixed = Color(0xFFFFDADB)
val OnTertiaryFixed = Color(0xFF40000E)
val TertiaryFixedDim = Color(0xFFFFB2B7)
val OnTertiaryFixedVariant = Color(0xFF91002B)

val ErrorRed = Color(0xFFBA1A1A)
val OnError = Color(0xFFFFFFFF)
val ErrorContainer = Color(0xFFFFDAD6)
val OnErrorContainer = Color(0xFF93000A)

// Backward compatibility static aliases
val ForestEmerald = EmeraldSecondary
val ForestEmeraldDark = OnSecondaryContainer
val VividMint = SecondaryFixedDim
val MintContainer = EmeraldSecondaryContainer
val OutflowRed = CrimsonTertiary
val InflowGreen = EmeraldSecondary
val WarningAmber = Color(0xFFF59E0B)
val PrimaryContainer = CobaltPrimaryContainer
val SecondaryContainer = EmeraldSecondaryContainer
val TertiaryContainer = CrimsonTertiaryContainer

// Dynamic Theme Adaptive Accessors (100% Light & Dark Mode Parity)
val CardSurfaceWhite: Color
    @Composable
    get() = MaterialTheme.colorScheme.surface

val CanvasBackground: Color
    @Composable
    get() = MaterialTheme.colorScheme.background

val SurfaceContainerLow: Color
    @Composable
    get() = MaterialTheme.colorScheme.surfaceContainerLow

val SurfaceContainer: Color
    @Composable
    get() = MaterialTheme.colorScheme.surfaceContainer

val SurfaceContainerHigh: Color
    @Composable
    get() = MaterialTheme.colorScheme.surfaceContainerHigh

val SurfaceContainerHighest: Color
    @Composable
    get() = MaterialTheme.colorScheme.surfaceContainerHighest

val SurfaceContainerLowest: Color
    @Composable
    get() = MaterialTheme.colorScheme.surfaceContainerLowest

val TextOnSurface: Color
    @Composable
    get() = MaterialTheme.colorScheme.onSurface

val TextOnSurfaceVariant: Color
    @Composable
    get() = MaterialTheme.colorScheme.onSurfaceVariant

val DeepCharcoal: Color
    @Composable
    get() = MaterialTheme.colorScheme.onSurface

val BorderOutline: Color
    @Composable
    get() = MaterialTheme.colorScheme.outline

val BorderOutlineVariant: Color
    @Composable
    get() = MaterialTheme.colorScheme.outlineVariant

// Light Theme Palette
private val LightCanvas = Color(0xFFF6F8FC)
private val LightSurface = Color(0xFFFFFFFF)
private val LightOnSurface = Color(0xFF0F172A)
private val LightOnSurfaceVariant = Color(0xFF475569)
private val LightOutline = Color(0xFF94A3B8)
private val LightOutlineVariant = Color(0xFFE2E8F0)

private val LightSpendwellColorScheme = lightColorScheme(
    primary = CobaltPrimary,
    onPrimary = OnPrimary,
    primaryContainer = Color(0xFFDAE2FF),
    onPrimaryContainer = Color(0xFF001848),
    secondary = EmeraldSecondary,
    onSecondary = OnSecondary,
    secondaryContainer = Color(0xFF85F8C6),
    onSecondaryContainer = Color(0xFF002115),
    tertiary = CrimsonTertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = Color(0xFFFFDADB),
    onTertiaryContainer = Color(0xFF40000E),
    background = LightCanvas,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = Color(0xFFEFF3FA),
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F4FA),
    surfaceContainer = LightSurface,
    surfaceContainerHigh = Color(0xFFE2E8F0),
    surfaceContainerHighest = Color(0xFFCBD5E1),
    error = ErrorRed,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant
)

// Dark Theme Palette (Obsidian & Deep FinTech Canvas)
private val DarkCanvas = Color(0xFF0B0E14)
private val DarkSurface = Color(0xFF151922)
private val DarkOnSurface = Color(0xFFF1F5F9)
private val DarkOnSurfaceVariant = Color(0xFF94A3B8)
private val DarkOutline = Color(0xFF475569)
private val DarkOutlineVariant = Color(0xFF242A38)

private val DarkSpendwellColorScheme = darkColorScheme(
    primary = Color(0xFF85A8FF),
    onPrimary = Color(0xFF00296B),
    primaryContainer = Color(0xFF1E2B4D),
    onPrimaryContainer = Color(0xFFDAE2FF),
    secondary = Color(0xFF68DBAB),
    onSecondary = Color(0xFF003825),
    secondaryContainer = Color(0xFF005138),
    onSecondaryContainer = Color(0xFF85F8C6),
    tertiary = Color(0xFFFFB2B7),
    onTertiary = Color(0xFF561D25),
    tertiaryContainer = Color(0xFF4D1420),
    onTertiaryContainer = Color(0xFFFFDADB),
    background = DarkCanvas,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = Color(0xFF1E2432),
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerLowest = Color(0xFF0E1118),
    surfaceContainerLow = Color(0xFF11141C),
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = Color(0xFF1E2432),
    surfaceContainerHighest = Color(0xFF282F3E),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant
)

@Composable
fun SpendwellTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkSpendwellColorScheme else LightSpendwellColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SpendwellTypography,
        shapes = SpendwellShapes,
        content = content
    )
}
