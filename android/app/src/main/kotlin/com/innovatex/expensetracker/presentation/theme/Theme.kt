package com.innovatex.expensetracker.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = ApexThemeTokens.AccentPrimary,
    onPrimary = ApexThemeTokens.BgCanvas,
    primaryContainer = ApexThemeTokens.AccentPrimaryTint,
    onPrimaryContainer = ApexThemeTokens.AccentPrimary,
    secondary = ApexThemeTokens.AccentSecondary,
    onSecondary = ApexThemeTokens.BgCanvas,
    secondaryContainer = ApexThemeTokens.AccentSecondaryTint,
    onSecondaryContainer = ApexThemeTokens.AccentSecondary,
    tertiary = ApexThemeTokens.AccentCobalt,
    onTertiary = ApexThemeTokens.BgCanvas,
    tertiaryContainer = ApexThemeTokens.AccentCobaltTint,
    onTertiaryContainer = ApexThemeTokens.AccentCobalt,
    background = ApexThemeTokens.BgCanvas,
    onBackground = ApexThemeTokens.TextPrimary,
    surface = ApexThemeTokens.BgSurface,
    onSurface = ApexThemeTokens.TextPrimary,
    surfaceVariant = ApexThemeTokens.BgSurfaceElevated,
    onSurfaceVariant = ApexThemeTokens.TextSecondary,
    error = ApexThemeTokens.AccentDanger,
    onError = ApexThemeTokens.BgCanvas,
    errorContainer = ApexThemeTokens.AccentDangerTint,
    onErrorContainer = ApexThemeTokens.AccentDanger,
    outline = ApexThemeTokens.StrokeHighlight,
    outlineVariant = ApexThemeTokens.StrokeSubtle
)

@Composable
fun ApexExpenseTrackerTheme(
    darkTheme: Boolean = true, // Default to deep obsidian dark mode for elite FinTech feel
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = ApexThemeTokens.BgCanvas.toArgb()
                window.navigationBarColor = ApexThemeTokens.BgCanvas.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = false
                controller.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CorbelTypography,
        content = content
    )
}
