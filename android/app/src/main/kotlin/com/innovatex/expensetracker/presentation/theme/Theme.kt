package com.innovatex.expensetracker.presentation.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val PastelKineticLightColorScheme = lightColorScheme(
    primary = PastelKineticTokens.AnchorPrimary,
    onPrimary = PastelKineticTokens.OnAnchor,
    primaryContainer = PastelKineticTokens.AnchorContainer,
    onPrimaryContainer = PastelKineticTokens.OnAnchorMuted,
    secondary = PastelKineticTokens.AmberDeep,
    onSecondary = PastelKineticTokens.OnAnchor,
    secondaryContainer = PastelKineticTokens.AmberFixed,
    onSecondaryContainer = PastelKineticTokens.AmberText,
    tertiary = PastelKineticTokens.SkyDeep,
    onTertiary = PastelKineticTokens.OnAnchor,
    tertiaryContainer = PastelKineticTokens.SkyFixed,
    onTertiaryContainer = PastelKineticTokens.SkyText,
    background = PastelKineticTokens.CanvasBg,
    onBackground = PastelKineticTokens.TextPrimary,
    surface = PastelKineticTokens.SurfaceCard,
    onSurface = PastelKineticTokens.TextPrimary,
    surfaceVariant = PastelKineticTokens.SurfaceLow,
    onSurfaceVariant = PastelKineticTokens.TextSecondary,
    error = PastelKineticTokens.Error,
    onError = PastelKineticTokens.OnAnchor,
    errorContainer = PastelKineticTokens.ErrorContainer,
    onErrorContainer = PastelKineticTokens.OnErrorContainer,
    outline = PastelKineticTokens.OutlineVariant,
    outlineVariant = PastelKineticTokens.SurfaceContainer
)

@Composable
fun ApexExpenseTrackerTheme(
    darkTheme: Boolean = false, // Default to Stitch Pastel Kinetic Neo-Modern light theme
    content: @Composable () -> Unit
) {
    val colorScheme = PastelKineticLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = PastelKineticTokens.CanvasBg.toArgb()
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = true
                controller.isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PastelKineticTypography,
        content = content
    )
}
