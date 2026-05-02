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

    // Secondary = emerald (data / status-ok)
    secondary             = LightEmerald,
    onSecondary           = Color.White,
    secondaryContainer    = LightEmeraldSoft,
    onSecondaryContainer  = LightEmeraldInk,

    // Tertiary = amber (warmth + warn accent — never CTA)
    tertiary             = LightAmber,
    onTertiary           = LightAmberInk,
    tertiaryContainer    = LightAmberSoft,
    onTertiaryContainer  = LightAmberInk,

    background        = LightBg,
    onBackground      = LightInk,
    surface           = LightSurface,
    onSurface         = LightInk,
    surfaceVariant    = LightSurface2,
    onSurfaceVariant  = LightInk2,
    surfaceTint       = Color.Transparent,
    inverseSurface    = LightInk,
    inverseOnSurface  = LightBg,
    inversePrimary    = LightBg,

    // Error = risk
    error             = LightRisk,
    onError           = Color.White,
    errorContainer    = LightRiskSoft,
    onErrorContainer  = LightRiskInk,

    outline           = LightInk3,
    outlineVariant    = LightRule,

    scrim = Color(0x80000000)
)

private val DarkColors = darkColorScheme(
    // Primary = cream (CTA on warm dark — mirrors ink-on-light)
    primary             = DarkInk,
    onPrimary           = DarkBg,
    primaryContainer    = DarkInk,
    onPrimaryContainer  = DarkBg,

    // Secondary = emerald (brighter for dark contrast)
    secondary             = DarkEmerald,
    onSecondary           = DarkBg,
    secondaryContainer    = DarkEmeraldSoft,
    onSecondaryContainer  = DarkEmeraldInk,

    // Tertiary = amber accent (NOT warn — amber here is the softer #D9A878)
    tertiary             = DarkAmber,
    onTertiary           = DarkBg,
    tertiaryContainer    = DarkAmberSoft,
    onTertiaryContainer  = DarkAmberInk,

    background        = DarkBg,
    onBackground      = DarkInk,
    surface           = DarkSurface,
    onSurface         = DarkInk,
    surfaceVariant    = DarkSurface2,
    onSurfaceVariant  = DarkInk2,
    surfaceTint       = Color.Transparent,
    inverseSurface    = DarkInk,
    inverseOnSurface  = DarkBg,
    inversePrimary    = DarkBg,

    // Error = risk
    error             = DarkRisk,
    onError           = DarkBg,
    errorContainer    = DarkRiskSoft,
    onErrorContainer  = DarkRiskInk,

    // Outlines: outline = high-emphasis (rule-strong), outlineVariant = subtle (rule)
    outline           = DarkRuleStrong,
    outlineVariant    = DarkRule,

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
