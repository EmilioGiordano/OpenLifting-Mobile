package com.openlifting.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Tokens that don't fit cleanly into Material 3's [androidx.compose.material3.ColorScheme] slots
 * but are part of the OpenLifting design system. Most palette decisions go through standard M3
 * slots (primary = ink/cream CTA, secondary = emerald, tertiary = amber, error = risk, etc).
 *
 * This extra palette covers the bits that need explicit semantic naming or live alongside the
 * standard slots:
 *   - [ink3]: third-level subdued text (M3 onSurface = ink, onSurfaceVariant = ink2 — we need a 3rd)
 *   - [bgTint]: warmer background tint used in the light theme for secondary surfaces
 *   - [warn]: amber used specifically for warn states (banners, monitor-level chips)
 *   - [riskBright]: brighter risk red used in dark mode for emphasis (e.g., bilateral bars in risk)
 *   - [emeraldSoft]/[amberSoft]/[riskSoft]: tinted-bg variants — convenience aliases for
 *     M3's secondaryContainer/tertiaryContainer/errorContainer slots so callers can read
 *     the intent at the call site instead of the M3 slot name.
 */
data class OpenLiftingExtras(
    val ink3: Color,
    val bgTint: Color,
    val rule: Color,
    val emerald: Color,
    val emeraldSoft: Color,
    val amber: Color,
    val amberSoft: Color,
    val warn: Color,
    val risk: Color,
    val riskSoft: Color,
    val riskBright: Color
)

internal val LightExtras = OpenLiftingExtras(
    ink3        = LightInk3,
    bgTint      = LightBgTint,
    rule        = LightRule,
    emerald     = LightEmerald,
    emeraldSoft = LightEmeraldSoft,
    amber       = LightAmber,
    amberSoft   = LightAmberSoft,
    warn        = LightWarn,
    risk        = LightRisk,
    riskSoft    = LightRiskSoft,
    riskBright  = LightRisk
)

internal val DarkExtras = OpenLiftingExtras(
    ink3        = DarkInk3,
    bgTint      = DarkSurface1,
    rule        = DarkRule,
    emerald     = DarkEmerald,
    emeraldSoft = DarkEmeraldSoft,
    amber       = DarkAmber,
    amberSoft   = DarkAmberSoft,
    warn        = DarkWarn,
    risk        = DarkRisk,
    riskSoft    = DarkRiskSoft,
    riskBright  = DarkRiskBright
)

internal val LocalOpenLiftingExtras = staticCompositionLocalOf { LightExtras }

/**
 * Access via `MaterialTheme.olExtras.amber`, `MaterialTheme.olExtras.ink3`, etc.
 */
val MaterialTheme.olExtras: OpenLiftingExtras
    @Composable
    @ReadOnlyComposable
    get() = LocalOpenLiftingExtras.current
