package com.openlifting.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    // Primary = highest-contrast neutral (CTA fill)
    primary             = LightInk,
    onPrimary           = LightBg,
    primaryContainer    = LightInk,
    onPrimaryContainer  = LightBg,

    // Secondary = emerald (data / status-ok accent)
    secondary             = LightEmerald,
    onSecondary           = Color.White,
    secondaryContainer    = LightEmeraldSoft,
    onSecondaryContainer  = LightEmerald,

    // Tertiary = amber (warmth + warn accent — never CTA)
    tertiary             = LightAmber,
    onTertiary           = LightInk,
    tertiaryContainer    = LightAmberSoft,
    onTertiaryContainer  = LightWarn,

    // Surfaces
    background        = LightBg,
    onBackground      = LightInk,
    surface           = LightSurface,
    onSurface         = LightInk,
    surfaceVariant    = LightBgTint,
    onSurfaceVariant  = LightInk2,
    surfaceTint       = Color.Transparent,
    inverseSurface    = LightInk,
    inverseOnSurface  = LightBg,
    inversePrimary    = LightBg,

    // Error = risk
    error               = LightRisk,
    onError             = Color.White,
    errorContainer      = LightRiskSoft,
    onErrorContainer    = LightRisk,

    // Borders / dividers
    outline             = LightInk3,
    outlineVariant      = LightRule,

    scrim = Color(0x80000000)
)

private val DarkColors = darkColorScheme(
    // Primary = cream (CTA fill on warm dark — mirrors ink-on-light)
    primary             = DarkInk,
    onPrimary           = DarkBg,
    primaryContainer    = DarkInk,
    onPrimaryContainer  = DarkBg,

    // Secondary = emerald (brighter for dark contrast)
    secondary             = DarkEmerald,
    onSecondary           = DarkBg,
    secondaryContainer    = DarkEmeraldSoft,
    onSecondaryContainer  = DarkEmerald,

    // Tertiary = amber (warn states + accents — never CTA)
    tertiary             = DarkAmber,
    onTertiary           = DarkBg,
    tertiaryContainer    = DarkAmberSoft,
    onTertiaryContainer  = DarkAmberBright,

    // Surfaces — leverage M3 surfaceContainer* slots for our 3 dark surface levels
    background        = DarkBg,
    onBackground      = DarkInk,
    surface           = DarkSurface1,
    onSurface         = DarkInk,
    surfaceVariant    = DarkSurface2,
    onSurfaceVariant  = DarkInk2,
    surfaceTint       = Color.Transparent,
    inverseSurface    = DarkInk,
    inverseOnSurface  = DarkBg,
    inversePrimary    = DarkBg,

    // Error = risk
    error               = DarkRisk,
    onError             = DarkBg,
    errorContainer      = DarkRiskSoft,
    onErrorContainer    = DarkRiskBright,

    // Borders / dividers
    outline             = DarkInk3,
    outlineVariant      = DarkRule,

    scrim = Color(0xCC000000)
)

@Composable
fun OpenLiftingMobileTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val extras      = if (darkTheme) DarkExtras else LightExtras

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalOpenLiftingExtras provides extras) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = Typography,
            shapes      = OpenLiftingShapes,
            content     = content
        )
    }
}
