package com.openlifting.presentation.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.openlifting.ui.theme.olExtras

/**
 * Brand mark: zigzag EMG logo + "OpenLifting" wordmark.
 * Source: template/logo.svg (path scaled from a 28x28 viewBox).
 */
@Composable
fun BrandMark(modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        LogoIcon(modifier = Modifier.size(28.dp))
        Spacer(Modifier.size(8.dp))
        Text(
            text  = "OpenLifting",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun LogoIcon(modifier: Modifier = Modifier) {
    val emerald = MaterialTheme.olExtras.emerald
    val points = listOf(
        Offset(2f, 14f),
        Offset(6f, 14f),
        Offset(8f, 6f),
        Offset(11f, 22f),
        Offset(14f, 4f),
        Offset(17f, 20f),
        Offset(20f, 8f),
        Offset(22f, 14f),
        Offset(26f, 14f)
    )
    Canvas(modifier = modifier) {
        val scale = size.minDimension / 28f
        val path = Path().apply {
            val first = points.first()
            moveTo(first.x * scale, first.y * scale)
            points.drop(1).forEach { lineTo(it.x * scale, it.y * scale) }
        }
        drawPath(
            path  = path,
            color = emerald,
            style = Stroke(
                width = 2.5f * scale,
                cap   = StrokeCap.Round,
                join  = StrokeJoin.Round
            )
        )
    }
}
