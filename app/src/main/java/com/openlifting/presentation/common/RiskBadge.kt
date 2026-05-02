package com.openlifting.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.openlifting.domain.model.RiskLevel
import com.openlifting.ui.theme.MonoText
import com.openlifting.ui.theme.olExtras

/**
 * Maps a [RiskLevel] to its semantic foreground color from the active theme.
 * Use this when you need the risk color outside of a [RiskBadge] (e.g. a chart line,
 * a bar fill, a left-edge rule on a recommendation card).
 */
@Composable
fun RiskLevel.toColor(): Color = when (this) {
    RiskLevel.NORMAL  -> MaterialTheme.olExtras.emerald
    RiskLevel.MONITOR -> MaterialTheme.olExtras.warn
    RiskLevel.RISK    -> MaterialTheme.olExtras.risk
}

/**
 * Tinted background variant of the risk color — for soft surfaces like badge fills,
 * banner backgrounds, recommendation cards.
 */
@Composable
fun RiskLevel.toSoftColor(): Color = when (this) {
    RiskLevel.NORMAL  -> MaterialTheme.olExtras.emeraldSoft
    RiskLevel.MONITOR -> MaterialTheme.olExtras.amberSoft
    RiskLevel.RISK    -> MaterialTheme.olExtras.riskSoft
}

fun RiskLevel.toLabel(): String = when (this) {
    RiskLevel.NORMAL  -> "NORMAL"
    RiskLevel.MONITOR -> "MONITOR"
    RiskLevel.RISK    -> "RIESGO"
}

@Composable
fun RiskBadge(level: RiskLevel, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(level.toSoftColor(), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text  = level.toLabel(),
            style = MonoText.labelSmall,
            color = level.toColor()
        )
    }
}
