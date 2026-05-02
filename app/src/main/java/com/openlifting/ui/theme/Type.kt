package com.openlifting.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// TODO: Replace with Google Fonts (Space Grotesk display, Inter body, IBM Plex Mono numeric)
//       once font_certs.xml is added to res/values. For now we use the system default
//       sans-serif and monospace families — sizing/weight hierarchy already matches the
//       design system, so the visual rhythm is preserved.

private val DisplayFamily = FontFamily.Default        // -> Space Grotesk
private val BodyFamily    = FontFamily.Default        // -> Inter
val MonoFamily            = FontFamily.Monospace      // -> IBM Plex Mono (used by composables for numeric values)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.25).sp
    ),
    displaySmall = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleLarge = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

/**
 * Mono variants for numeric values (BSA %, ratios, %MVC, kg, RPE, timer digits, etc).
 * The "lab tool" look depends on numbers being monospaced — apply these styles to any
 * Text composable rendering a number.
 */
object MonoText {
    val displayLarge  = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 40.sp)
    val displayMedium = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp)
    val displaySmall  = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp)
    val titleLarge    = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.Medium,   fontSize = 18.sp, lineHeight = 24.sp)
    val titleMedium   = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.Medium,   fontSize = 15.sp, lineHeight = 22.sp)
    val bodyMedium    = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.Normal,   fontSize = 14.sp, lineHeight = 20.sp)
    val bodySmall     = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.Normal,   fontSize = 12.sp, lineHeight = 16.sp)
    val labelMedium   = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.Medium,   fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp)
    val labelSmall    = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.Medium,   fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp)
}
