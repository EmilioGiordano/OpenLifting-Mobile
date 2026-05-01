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

val RiskNormal  = Color(0xFF2E7D32)
val RiskMonitor = Color(0xFFF57F17)
val RiskRed     = Color(0xFFC62828)

fun RiskLevel.toColor() = when (this) {
    RiskLevel.NORMAL  -> RiskNormal
    RiskLevel.MONITOR -> RiskMonitor
    RiskLevel.RISK    -> RiskRed
}

fun RiskLevel.toLabel() = when (this) {
    RiskLevel.NORMAL  -> "Normal"
    RiskLevel.MONITOR -> "Monitorear"
    RiskLevel.RISK    -> "Atención"
}

@Composable
fun RiskBadge(level: RiskLevel, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(level.toColor().copy(alpha = 0.15f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = level.toLabel(),
            style = MaterialTheme.typography.labelSmall,
            color = level.toColor()
        )
    }
}
