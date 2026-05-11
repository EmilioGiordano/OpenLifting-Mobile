package com.openlifting.presentation.athlete.session

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openlifting.domain.model.Muscle
import com.openlifting.domain.model.MusclePair
import com.openlifting.ui.theme.MonoText
import com.openlifting.ui.theme.olExtras
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.cartesianLayerPadding
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

// Colores V-D: sin rojo en las líneas de músculo
private val muscleColors = mapOf(
    Muscle.VASTUS_LATERALIS to Color(0xFF0D9373),
    Muscle.VASTUS_MEDIALIS  to Color(0xFF4A90C4),
    Muscle.GLUTEUS_MAXIMUS  to Color(0xFFC8956C),
    Muscle.ERECTOR_SPINAE   to Color(0xFF8B5CF6),
    Muscle.BICEPS_FEMORIS   to Color(0xFF7B7585),
)

private val colorIzq = Color(0xFF0D9373)
private val colorDer = Color(0xFF4A90C4)

private val muscleOrder = Muscle.entries

private val repFormatter = CartesianValueFormatter { _, value, _ -> "R${value.toInt()}" }

@Composable
fun ActivationChartCard(
    repActivations: List<Map<Muscle, MusclePair>>,
    modifier: Modifier = Modifier
) {
    if (repActivations.isEmpty()) return

    var mode by remember { mutableStateOf("general") }
    var muscleIdx by remember { mutableIntStateOf(0) }

    val modelProducer = remember { CartesianChartModelProducer() }

    // Compute Y ceiling: max value across all reps and muscles, +20% headroom, rounded up to 10
    val allValues = repActivations.flatMap { map ->
        map.values.flatMap { listOf(it.left, it.right) }
    }
    val dataMax = allValues.maxOrNull() ?: 80f
    val yMax = (ceil(dataMax * 1.25f / 10f) * 10f).toDouble()

    LaunchedEffect(mode, muscleIdx, repActivations) {
        modelProducer.runTransaction {
            if (mode == "general") {
                lineSeries {
                    muscleOrder.forEach { muscle ->
                        series(
                            x = repActivations.indices.map { it + 1 },
                            y = repActivations.map { rep ->
                                val pair = rep[muscle] ?: MusclePair(0f, 0f)
                                ((pair.left + pair.right) / 2.0).toFloat()
                            }
                        )
                    }
                }
            } else {
                val muscle = muscleOrder[muscleIdx]
                lineSeries {
                    series(
                        x = repActivations.indices.map { it + 1 },
                        y = repActivations.map { (it[muscle] ?: MusclePair(0f, 0f)).left }
                    )
                    series(
                        x = repActivations.indices.map { it + 1 },
                        y = repActivations.map { (it[muscle] ?: MusclePair(0f, 0f)).right }
                    )
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
    ) {
        // ── Header ──────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text  = "ACTIVACIÓN MUSCULAR",
                    style = MonoText.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.08.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text  = "${repActivations.size} reps · %MVC",
                    style = MonoText.labelSmall,
                    color = MaterialTheme.olExtras.ink3
                )
            }
            ModeToggle(mode = mode, onModeChange = { mode = it })
        }

        // ── Chart ────────────────────────────────────────────────────
        val lineColors = if (mode == "general") muscleOrder.map { muscleColors[it]!! }
                         else listOf(colorIzq, colorDer)

        val labelColor = MaterialTheme.colorScheme.onSurface

        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(
                    lineProvider = LineCartesianLayer.LineProvider.series(
                        lineColors.map { color ->
                            val pointComponent = rememberShapeComponent(
                                fill  = fill(color),
                                shape = CorneredShape.Pill
                            )
                            LineCartesianLayer.rememberLine(
                                fill          = LineCartesianLayer.LineFill.single(fill(color)),
                                stroke        = LineCartesianLayer.LineStroke.Continuous(thicknessDp = 2f),
                                pointProvider = LineCartesianLayer.PointProvider.single(
                                    LineCartesianLayer.Point(component = pointComponent, sizeDp = 6f)
                                ),
                            )
                        }
                    ),
                    rangeProvider = CartesianLayerRangeProvider.fixed(minY = 0.0, maxY = yMax)
                ),
                startAxis = VerticalAxis.rememberStart(
                    label = rememberAxisLabelComponent(color = labelColor),
                    itemPlacer = remember { VerticalAxis.ItemPlacer.step({ 20.0 }) }
                ),
                bottomAxis = HorizontalAxis.rememberBottom(
                    label = rememberAxisLabelComponent(color = labelColor),
                    valueFormatter = repFormatter,
                ),
                layerPadding = { cartesianLayerPadding(scalableStart = 16.dp, scalableEnd = 16.dp) }
            ),
            modelProducer = modelProducer,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(horizontal = 4.dp),
        )

        Spacer(Modifier.height(4.dp))

        // ── Asymmetry bars — always visible ─────────────────────────
        AsymmetrySection(repActivations = repActivations)

        // ── General legend / Bilateral nav ───────────────────────────
        if (mode == "general") {
            GeneralLegend()
        } else {
            BilateralNav(
                muscleIdx      = muscleIdx,
                repActivations = repActivations,
                onPrev         = { if (muscleIdx > 0) muscleIdx-- },
                onNext         = { if (muscleIdx < muscleOrder.lastIndex) muscleIdx++ }
            )
        }
    }
}

// ── Mode toggle ──────────────────────────────────────────────────────────────

@Composable
private fun ModeToggle(mode: String, onModeChange: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text     = "General",
            style    = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color    = if (mode == "general") MaterialTheme.colorScheme.onSurface
                       else MaterialTheme.olExtras.ink3,
            modifier = Modifier.clickable { onModeChange("general") }.padding(horizontal = 4.dp, vertical = 2.dp)
        )
        Text(
            text  = "|",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        Text(
            text     = "Bilateral",
            style    = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color    = if (mode == "bilateral") MaterialTheme.colorScheme.onSurface
                       else MaterialTheme.olExtras.ink3,
            modifier = Modifier.clickable { onModeChange("bilateral") }.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

// ── General legend ───────────────────────────────────────────────────────────

@Composable
private fun GeneralLegend() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        muscleOrder.forEach { muscle ->
            val color = muscleColors[muscle] ?: return@forEach
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(color))
                Text(
                    text  = muscle.shortName,
                    style = MonoText.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Bilateral nav + IZQ/DER values ──────────────────────────────────────────

@Composable
private fun BilateralNav(
    muscleIdx: Int,
    repActivations: List<Map<Muscle, MusclePair>>,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    val muscle = muscleOrder[muscleIdx]
    val avgLeft  = repActivations.map { (it[muscle] ?: MusclePair(0f, 0f)).left  }.average().toFloat()
    val avgRight = repActivations.map { (it[muscle] ?: MusclePair(0f, 0f)).right }.average().toFloat()
    val delta    = if (max(avgLeft, avgRight) > 0f)
        (abs(avgLeft - avgRight) / max(avgLeft, avgRight) * 100).toInt() else 0

    val weaker = if (avgLeft < avgRight) "izq" else "der"
    val riskColor = when {
        delta >= 15 -> Color(0xFFB42318)
        delta >= 10 -> Color(0xFFB26A0E)
        else        -> null
    }
    val izqColor   = if (riskColor != null && weaker == "izq") riskColor else colorIzq
    val derColor   = if (riskColor != null && weaker == "der") riskColor else colorDer
    val deltaColor = riskColor ?: MaterialTheme.olExtras.ink3

    // Δ sits in the top-right corner so it doesn't fight the cluster horizontally.
    // The cluster (IZQ | nav | DER) has intrinsic width and is centered: the muscle
    // column is fixed-width so the cluster doesn't shift when navigating between muscles,
    // and centering (not weighting) keeps IZQ/DER grouped on wide screens.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            SideValue(label = "IZQ", value = avgLeft, color = izqColor)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onPrev, enabled = muscleIdx > 0, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Anterior",
                        modifier = Modifier.size(14.dp), tint = MaterialTheme.olExtras.ink3)
                }
                Column(
                    modifier = Modifier.width(110.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text  = muscle.displayName,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text  = "${muscleIdx + 1} / ${muscleOrder.size}",
                        style = MonoText.labelSmall,
                        color = MaterialTheme.olExtras.ink3
                    )
                }
                IconButton(onClick = onNext, enabled = muscleIdx < muscleOrder.lastIndex, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Siguiente",
                        modifier = Modifier.size(14.dp), tint = MaterialTheme.olExtras.ink3)
                }
            }

            SideValue(label = "DER", value = avgRight, color = derColor)
        }

        Text(
            text  = "Δ $delta%",
            style = MonoText.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = deltaColor,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

@Composable
private fun SideValue(label: String, value: Float, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MonoText.labelSmall, color = MaterialTheme.olExtras.ink3)
        Text(
            text  = "${"%.0f".format(value)}%",
            style = MonoText.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = color
        )
    }
}

// ── Asymmetry section ────────────────────────────────────────────────────────

@Composable
private fun AsymmetrySection(repActivations: List<Map<Muscle, MusclePair>>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text  = "ASIMETRÍA BILATERAL",
            style = MonoText.labelSmall.copy(letterSpacing = 0.06.sp),
            color = MaterialTheme.olExtras.ink3
        )
        Spacer(Modifier.height(2.dp))
        muscleOrder.forEach { muscle ->
            val avgLeft  = repActivations.map { (it[muscle] ?: MusclePair(0f, 0f)).left  }.average().toFloat()
            val avgRight = repActivations.map { (it[muscle] ?: MusclePair(0f, 0f)).right }.average().toFloat()
            val delta = if (max(avgLeft, avgRight) > 0f)
                (abs(avgLeft - avgRight) / max(avgLeft, avgRight) * 100).toInt() else 0
            val barColor = when {
                delta >= 15 -> Color(0xFFB42318)
                delta >= 10 -> Color(0xFFB26A0E)
                else        -> Color(0xFF0D9373)
            }
            AsymmetryRow(label = muscle.shortName, deltaPct = delta, barColor = barColor)
        }
    }
}

@Composable
private fun AsymmetryRow(label: String, deltaPct: Int, barColor: Color) {
    val trackColor = MaterialTheme.colorScheme.outlineVariant

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text     = label,
            style    = MonoText.labelSmall.copy(fontWeight = FontWeight.Medium),
            color    = MaterialTheme.olExtras.ink3,
            modifier = Modifier.width(28.dp)
        )

        // Canvas bar: track is neutral, fill grows from center rightward
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
        ) {
            val w = size.width
            val h = size.height
            val r = h / 2f
            val centerX = w / 2f

            // Background track
            drawRoundRect(
                color        = trackColor,
                cornerRadius = CornerRadius(r),
                size         = Size(w, h)
            )

            // Fill: grows from center to the right, represents deviation
            val fillFraction = (deltaPct * 3).coerceAtMost(50) / 100f
            val fillWidth = fillFraction * w
            if (fillWidth > 0f) {
                drawRect(
                    color    = barColor.copy(alpha = 0.4f),
                    topLeft  = Offset(centerX, 0f),
                    size     = Size(fillWidth, h)
                )
            }

            // Center divider line
            drawRect(
                color    = trackColor,
                topLeft  = Offset(centerX - 0.5.dp.toPx(), 0f),
                size     = Size(1.dp.toPx(), h)
            )
        }

        Text(
            text  = "$deltaPct%",
            style = MonoText.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = barColor,
            modifier = Modifier.width(30.dp)
        )
    }
}
