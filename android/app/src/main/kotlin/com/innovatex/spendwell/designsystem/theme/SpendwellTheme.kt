package com.innovatex.spendwell.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Spendwell Primary & Brand Palette (from Stitch project 6535465419424087375)
val ForestEmerald = Color(0xFF159B70)
val ForestEmeraldDark = Color(0xFF00694A)
val VividMint = Color(0xFF36C99A)
val MintContainer = Color(0xFFEFF5F3)
val MintContainerDark = Color(0xFF1C2B26)

val DeepCharcoal = Color(0xFF101820)
val CharcoalSurface = Color(0xFF18222A)
val CharcoalCard = Color(0xFF1F2B35)

val CanvasLight = Color(0xFFF7FAF9)
val CanvasDark = Color(0xFF0E151A)

val SurfaceWhite = Color(0xFFFFFFFF)
val TextPrimaryLight = Color(0xFF172B27)
val TextSecondaryLight = Color(0xFF526A64)
val TextTertiaryLight = Color(0xFF8A9E99)

val TextPrimaryDark = Color(0xFFF5F8F7)
val TextSecondaryDark = Color(0xFFA0B5AF)
val TextTertiaryDark = Color(0xFF70857F)

val OutflowRed = Color(0xFFD9383A)
val InflowGreen = Color(0xFF159B70)
val WarningAmber = Color(0xFFF59E0B)

val HairlineBorderLight = Color(0x0F172B27)
val HairlineBorderDark = Color(0x1FFFFFFF)

private val LightSpendwellColorScheme = lightColorScheme(
    primary = ForestEmerald,
    onPrimary = Color.White,
    primaryContainer = MintContainer,
    onPrimaryContainer = ForestEmeraldDark,
    secondary = VividMint,
    onSecondary = DeepCharcoal,
    secondaryContainer = MintContainer,
    onSecondaryContainer = ForestEmeraldDark,
    tertiary = DeepCharcoal,
    onTertiary = Color.White,
    background = CanvasLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceWhite,
    onSurface = TextPrimaryLight,
    surfaceVariant = MintContainer,
    onSurfaceVariant = TextSecondaryLight,
    error = OutflowRed,
    onError = Color.White,
    outline = TextTertiaryLight,
    outlineVariant = HairlineBorderLight
)

private val DarkSpendwellColorScheme = darkColorScheme(
    primary = VividMint,
    onPrimary = DeepCharcoal,
    primaryContainer = MintContainerDark,
    onPrimaryContainer = VividMint,
    secondary = ForestEmerald,
    onSecondary = Color.White,
    secondaryContainer = MintContainerDark,
    onSecondaryContainer = VividMint,
    tertiary = DeepCharcoal,
    onTertiary = Color.White,
    background = CanvasDark,
    onBackground = TextPrimaryDark,
    surface = CharcoalSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = CharcoalCard,
    onSurfaceVariant = TextSecondaryDark,
    error = OutflowRed,
    onError = Color.White,
    outline = TextTertiaryDark,
    outlineVariant = HairlineBorderDark
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
