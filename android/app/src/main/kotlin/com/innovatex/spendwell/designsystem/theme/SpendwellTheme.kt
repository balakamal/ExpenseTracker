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

// Canvas & Surfaces (Light)
val CanvasBackground = Color(0xFFF8F9FF)
val CardSurfaceWhite = Color(0xFFFFFFFF)
val SurfaceContainerLowest = Color(0xFFFFFFFF)
val SurfaceContainerLow = Color(0xFFEFF4FF)
val SurfaceContainer = Color(0xFFE9EEFA)
val SurfaceContainerHigh = Color(0xFFE3E8F4)
val SurfaceContainerHighest = Color(0xFFDDE3EF)

val TextOnSurface = Color(0xFF161C24)
val TextOnSurfaceVariant = Color(0xFF434653)
val BorderOutline = Color(0xFF737685)
val BorderOutlineVariant = Color(0xFFC3C6D6)

val ErrorRed = Color(0xFFBA1A1A)
val OnError = Color(0xFFFFFFFF)
val ErrorContainer = Color(0xFFFFDAD6)
val OnErrorContainer = Color(0xFF93000A)

// Backward compatibility aliases
val ForestEmerald = EmeraldSecondary
val ForestEmeraldDark = OnSecondaryContainer
val VividMint = SecondaryFixedDim
val MintContainer = EmeraldSecondaryContainer
val DeepCharcoal = TextOnSurface
val OutflowRed = CrimsonTertiary
val InflowGreen = EmeraldSecondary
val WarningAmber = Color(0xFFF59E0B)

private val LightSpendwellColorScheme = lightColorScheme(
    primary = CobaltPrimary,
    onPrimary = OnPrimary,
    primaryContainer = CobaltPrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = EmeraldSecondary,
    onSecondary = OnSecondary,
    secondaryContainer = EmeraldSecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = CrimsonTertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = CrimsonTertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    background = CanvasBackground,
    onBackground = TextOnSurface,
    surface = CanvasBackground,
    onSurface = TextOnSurface,
    surfaceVariant = SurfaceContainerHigh,
    onSurfaceVariant = TextOnSurfaceVariant,
    surfaceContainerLowest = SurfaceContainerLowest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
    error = ErrorRed,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    outline = BorderOutline,
    outlineVariant = BorderOutlineVariant
)

private val DarkSpendwellColorScheme = darkColorScheme(
    primary = PrimaryFixedDim,
    onPrimary = Color(0xFF002B74),
    primaryContainer = Color(0xFF0040A2),
    onPrimaryContainer = PrimaryFixed,
    secondary = SecondaryFixedDim,
    onSecondary = Color(0xFF003825),
    secondaryContainer = Color(0xFF005138),
    onSecondaryContainer = SecondaryFixed,
    tertiary = TertiaryFixedDim,
    onTertiary = Color(0xFF561D25),
    tertiaryContainer = Color(0xFF72333B),
    onTertiaryContainer = TertiaryFixed,
    background = Color(0xFF10141A),
    onBackground = Color(0xFFE0E2EC),
    surface = Color(0xFF10141A),
    onSurface = Color(0xFFE0E2EC),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC4C6D0),
    surfaceContainerLowest = Color(0xFF0C0F14),
    surfaceContainerLow = Color(0xFF191C22),
    surfaceContainer = Color(0xFF1D2026),
    surfaceContainerHigh = Color(0xFF272A31),
    surfaceContainerHighest = Color(0xFF32353C),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF8E909A),
    outlineVariant = Color(0xFF43474E)
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
