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
 *   - [ink3]: third-level subdued text (M3 onSurface = ink, onSurfaceVariant = ink-2)
 *   - [bgTint]: the warm bg-tint surface, between bg and surface (used for subtle nested fills)
 *   - [rule] / [ruleStrong]: design has two divider strengths in dark mode
 *   - [emerald] / [emeraldSoft] / [emeraldInk]: full triplet for status-ok states
 *   - [amber] / [amberSoft] / [amberInk]: warmth accent (NOT primary CTA, NOT warn)
 *   - [warn] / [warnSoft] / [warnInk]: explicit warn-state palette (calibration banner, monitor risk)
 *   - [risk] / [riskSoft] / [riskInk]: full risk palette (M3's errorContainer covers riskSoft via slot)
 *
 * Token values are sourced from the canonical design HTML in `UI-inspiration/Openlifting-design.html`.
 */
data class OpenLiftingExtras(
    val ink3: Color,
    val bgTint: Color,
    val rule: Color,
    val ruleStrong: Color,

    val emerald: Color,
    val emeraldSoft: Color,
    val emeraldInk: Color,

    val amber: Color,
    val amberSoft: Color,
    val amberInk: Color,

    val warn: Color,
    val warnSoft: Color,
    val warnInk: Color,

    val risk: Color,
    val riskSoft: Color,
    val riskInk: Color
)

internal val LightExtras = OpenLiftingExtras(
    ink3        = LightInk3,
    bgTint      = LightBgTint,
    rule        = LightRule,
    ruleStrong  = LightRule,         // light has only one rule level

    emerald     = LightEmerald,
    emeraldSoft = LightEmeraldSoft,
    emeraldInk  = LightEmeraldInk,

    amber       = LightAmber,
    amberSoft   = LightAmberSoft,
    amberInk    = LightAmberInk,

    warn        = LightWarn,
    warnSoft    = LightWarnSoft,
    warnInk     = LightWarnInk,

    risk        = LightRisk,
    riskSoft    = LightRiskSoft,
    riskInk     = LightRiskInk
)

internal val DarkExtras = OpenLiftingExtras(
    ink3        = DarkInk3,
    bgTint      = DarkBgTint,
    rule        = DarkRule,
    ruleStrong  = DarkRuleStrong,

    emerald     = DarkEmerald,
    emeraldSoft = DarkEmeraldSoft,
    emeraldInk  = DarkEmeraldInk,

    amber       = DarkAmber,
    amberSoft   = DarkAmberSoft,
    amberInk    = DarkAmberInk,

    warn        = DarkWarn,
    warnSoft    = DarkWarnSoft,
    warnInk     = DarkWarnInk,

    risk        = DarkRisk,
    riskSoft    = DarkRiskSoft,
    riskInk     = DarkRiskInk
)

internal val LocalOpenLiftingExtras = staticCompositionLocalOf { LightExtras }

/**
 * Access via `MaterialTheme.olExtras.amber`, `MaterialTheme.olExtras.ink3`, etc.
 */
val MaterialTheme.olExtras: OpenLiftingExtras
    @Composable
    @ReadOnlyComposable
    get() = LocalOpenLiftingExtras.current
