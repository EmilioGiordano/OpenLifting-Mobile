package com.openlifting.presentation.athlete.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openlifting.domain.model.RiskLevel
import com.openlifting.presentation.common.RiskBadge
import com.openlifting.presentation.common.toColor
import com.openlifting.ui.theme.MonoText
import com.openlifting.ui.theme.olExtras
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private val DATE_FMT = SimpleDateFormat("dd MMM, HH:mm", Locale("es"))

// ── Pending banner (calibration) ────────────────────────────────────────────

@Composable
fun PendingCalibrationBanner(onCalibrate: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.olExtras.amberSoft)
            .border(1.dp, MaterialTheme.olExtras.warn.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.WarningAmber,
            contentDescription = null,
            tint = MaterialTheme.olExtras.warn,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text     = "Calibración MVC pendiente — los resultados pueden ser imprecisos",
            style    = MaterialTheme.typography.bodySmall,
            color    = MaterialTheme.olExtras.warn,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onCalibrate, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
            Text(
                text  = "Calibrar",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.olExtras.warn
            )
        }
    }
}

// ── Last session card ───────────────────────────────────────────────────────

@Composable
fun LastSessionCard(summary: LastSessionSummary, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text  = "ÚLTIMA SESIÓN",
                    style = MonoText.labelSmall,
                    color = MaterialTheme.olExtras.ink3
                )
                Text(
                    text  = DATE_FMT.format(Date(summary.startedAt)),
                    style = MonoText.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RiskBadge(summary.overallRisk)
        }

        // 3-metric strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MetricCell(label = "BSA peor", value = "%.1f%%".format(summary.bsaWorstPct))
            MetricCell(label = "ES:GMax",  value = "%.2f".format(summary.esGmaxRatio))
            MetricCell(label = "H:Q",      value = "%.2f".format(summary.hqRatio))
        }

        // Footer line
        Text(
            text  = buildFooterLine(summary),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.olExtras.ink3
        )
    }
}

@Composable
private fun MetricCell(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text  = label.uppercase(),
            style = MonoText.labelSmall,
            color = MaterialTheme.olExtras.ink3
        )
        Text(
            text  = value,
            style = MonoText.displaySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun buildFooterLine(s: LastSessionSummary): String = buildString {
    append("${s.setCount} serie${if (s.setCount == 1) "" else "s"}")
    if (s.maxLoadKg > 0f) append(" · ${s.maxLoadKg.toInt()} kg máx")
    s.durationMinutes?.let { append(" · $it min") }
}

// ── BSA trend card with custom-canvas sparkline ─────────────────────────────

@Composable
fun BsaTrendCard(
    points: List<TrendPoint>,
    currentValue: Float,
    deltaVsPrevious: Float?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text  = "Tendencia BSA",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text  = "Últimas ${points.size} sesiones",
                style = MonoText.labelSmall,
                color = MaterialTheme.olExtras.ink3
            )
        }

        BsaSparkline(values = points.map { it.value })

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text  = "ACTUAL",
                    style = MonoText.labelSmall,
                    color = MaterialTheme.olExtras.ink3
                )
                Text(
                    text  = "%.1f%%".format(currentValue),
                    style = MonoText.displayMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            DeltaPill(value = deltaVsPrevious, isHigherWorse = true)
        }
    }
}

@Composable
private fun BsaSparkline(values: List<Float>) {
    val emerald = MaterialTheme.olExtras.emerald
    val warn    = MaterialTheme.olExtras.warn
    val risk    = MaterialTheme.olExtras.risk
    val rule    = MaterialTheme.colorScheme.outlineVariant
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MonoText.labelSmall.copy(fontSize = 10.sp)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        if (values.isEmpty()) return@Canvas
        val w = size.width
        val h = size.height
        // Domain: 0% to max(20, maxValue + 2). Reference lines at 10 and 15.
        val maxY = (values.max().coerceAtLeast(15f) + 2f).coerceAtLeast(20f)

        fun yFor(v: Float) = h - (v / maxY) * h

        // Reference lines (dotted) — drawn before labels so the text sits on top.
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
        drawLine(
            color       = warn,
            start       = Offset(0f, yFor(10f)),
            end         = Offset(w, yFor(10f)),
            strokeWidth = 1.5f,
            pathEffect  = dashEffect
        )
        drawLine(
            color       = risk,
            start       = Offset(0f, yFor(15f)),
            end         = Offset(w, yFor(15f)),
            strokeWidth = 1.5f,
            pathEffect  = dashEffect
        )

        // Threshold labels: pinned to the left edge, sitting just above each line so they
        // don't visually overlap the dashed line itself.
        val warnLabel = textMeasurer.measure("Monitoreo 10%", labelStyle.copy(color = warn))
        val riskLabel = textMeasurer.measure("Riesgo 15%",    labelStyle.copy(color = risk))
        val labelGap = 3f
        drawText(
            textLayoutResult = warnLabel,
            topLeft = Offset(2f, yFor(10f) - warnLabel.size.height - labelGap)
        )
        drawText(
            textLayoutResult = riskLabel,
            topLeft = Offset(2f, yFor(15f) - riskLabel.size.height - labelGap)
        )

        if (values.size < 2) {
            // Single point: draw a small dot
            val x = w / 2f
            val y = yFor(values.first())
            drawCircle(emerald, radius = 4f, center = androidx.compose.ui.geometry.Offset(x, y))
            return@Canvas
        }

        // Trend line
        val stepX = w / (values.size - 1f)
        for (i in 0 until values.size - 1) {
            drawLine(
                color       = emerald,
                start       = androidx.compose.ui.geometry.Offset(i * stepX, yFor(values[i])),
                end         = androidx.compose.ui.geometry.Offset((i + 1) * stepX, yFor(values[i + 1])),
                strokeWidth = 3f,
                cap         = StrokeCap.Round
            )
        }
        // Endpoint dot
        drawCircle(
            color  = emerald,
            radius = 5f,
            center = androidx.compose.ui.geometry.Offset(w, yFor(values.last())),
            style  = Stroke(width = 2f)
        )
        drawCircle(
            color  = emerald,
            radius = 3f,
            center = androidx.compose.ui.geometry.Offset(w, yFor(values.last()))
        )

        // Suppress unused-var warning for outline rule
        @Suppress("UNUSED_EXPRESSION") rule
    }
}

// ── Metric delta chip (ES:GMax + H:Q on Home) ───────────────────────────────

@Composable
fun MetricDeltaChip(
    label: String,
    value: String,
    delta: Float?,
    risk: RiskLevel,
    isHigherWorse: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text  = label.uppercase(),
                style = MonoText.labelSmall,
                color = MaterialTheme.olExtras.ink3
            )
            RiskDot(risk)
        }
        Text(
            text  = value,
            style = MonoText.displaySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        DeltaPill(value = delta, isHigherWorse = isHigherWorse)
    }
}

@Composable
private fun DeltaPill(value: Float?, isHigherWorse: Boolean) {
    val tint: Color = when {
        value == null            -> MaterialTheme.olExtras.ink3
        abs(value) < 0.005f      -> MaterialTheme.olExtras.ink3
        (value > 0f) == isHigherWorse -> MaterialTheme.olExtras.risk
        else                     -> MaterialTheme.olExtras.emerald
    }
    val text = when {
        value == null       -> "—"
        abs(value) < 0.005f -> "0.00"
        else                -> (if (value > 0f) "+" else "") + "%.2f".format(value)
    }
    val suffix = if (value != null) " vs anterior" else ""
    Text(
        text  = "$text$suffix",
        style = MonoText.labelSmall,
        color = tint
    )
}

@Composable
private fun RiskDot(level: RiskLevel) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(level.toColor())
    )
}

// ── Empty state ─────────────────────────────────────────────────────────────

@Composable
fun HomeEmptyState(onStartFirstSession: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.olExtras.emeraldSoft),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text  = "—",
                style = MonoText.displaySmall,
                color = MaterialTheme.olExtras.emerald
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text  = "Aún no tenés sesiones",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text  = "Empezá tu primera sesión para ver tu balance muscular y progreso.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
