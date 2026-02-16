package com.openlifting.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val OpenLiftingColorScheme = darkColorScheme(
    primary = Blue400,
    onPrimary = TextPrimary,
    primaryContainer = Navy500,
    onPrimaryContainer = Blue300,

    secondary = Terracotta400,
    onSecondary = Navy950,
    secondaryContainer = Terracotta600,
    onSecondaryContainer = Terracotta400,

    tertiary = Terracotta500,
    onTertiary = TextPrimary,

    background = Navy900,
    onBackground = TextPrimary,

    surface = Navy700,
    onSurface = TextPrimary,
    surfaceVariant = Navy600,
    onSurfaceVariant = TextSecondary,

    error = Red400,
    onError = Navy950,
    errorContainer = Red500,
    onErrorContainer = TextPrimary,

    outline = Navy500,
    outlineVariant = Navy600,

    inverseSurface = TextPrimary,
    inverseOnSurface = Navy900,
    inversePrimary = Navy500,

    scrim = Color.Black,
    surfaceTint = Color.Transparent
)

@Composable
fun OpenLiftingMobileTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = Navy950.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = ForestGreenDark.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = OpenLiftingColorScheme,
        typography = Typography,
        content = content
    )
}
