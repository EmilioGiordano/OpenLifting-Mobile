package com.openlifting.ui.screens.session

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.openlifting.data.model.MuscleGroup
import com.openlifting.data.model.Repetition
import com.openlifting.data.model.Series
import com.openlifting.ui.theme.Blue300
import com.openlifting.ui.theme.Blue400
import com.openlifting.ui.theme.Red400
import com.openlifting.ui.theme.Terracotta400
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    sessionId: String,
    onBack: () -> Unit,
    viewModel: SessionDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(sessionId) {
        viewModel.loadSession(sessionId)
    }

    val session = uiState.session

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        session?.let {
                            "Sesion ${it.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}"
                        } ?: "Detalle de sesion"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        if (session == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Sesion no encontrada", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Session score chart (all series)
            SessionScoreChart(uiState.seriesScores)

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Series tabs
            SeriesSelector(
                series = session.series,
                selectedIndex = uiState.selectedSeriesIndex,
                onSelect = viewModel::selectSeries
            )

            val selectedSeries = session.series.getOrNull(uiState.selectedSeriesIndex)
            if (selectedSeries != null) {
                Spacer(modifier = Modifier.height(12.dp))

                // 3. Series info + score
                SeriesInfoStrip(
                    series = selectedSeries,
                    totalSeries = session.series.size,
                    qualityScore = uiState.currentSeriesMetrics?.qualityScore
                )

                // 4. Fatigue curve
                if (uiState.fatigueData.size >= 2) {
                    Spacer(modifier = Modifier.height(12.dp))
                    FatigueCurveCard(uiState.fatigueData)
                }

                // 5. Series metrics
                uiState.currentSeriesMetrics?.let { metrics ->
                    Spacer(modifier = Modifier.height(12.dp))
                    SeriesMetricsRow(metrics)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 6. Repetitions with quality badges
                selectedSeries.repetitions.forEachIndexed { index, rep ->
                    val quality = uiState.repQualities.getOrNull(index)
                    RepetitionCard(rep, quality)
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // 7. Recommendations
            if (uiState.recommendations.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                RecommendationsCard(uiState.recommendations)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// ─── Session Score Chart ─────────────────────────────────────────────

@Composable
fun SessionScoreChart(seriesScores: List<Int>) {
    if (seriesScores.isEmpty()) return

    val maxBarHeight = 120.dp

    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Puntaje por Serie",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(maxBarHeight + 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                seriesScores.forEachIndexed { _, score ->
                    val color = scoreColor(score)

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            "$score",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height(maxBarHeight * (score / 100f))
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(color)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // X-axis labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                seriesScores.indices.forEach { i ->
                    Text(
                        "S${i + 1}",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ─── Fatigue Curve ───────────────────────────────────────────────────

private val muscleChartColors = mapOf(
    MuscleGroup.QUADRICEPS to Blue400,
    MuscleGroup.GLUTES to Terracotta400,
    MuscleGroup.HAMSTRINGS to Blue300,
    MuscleGroup.LOWER_BACK to Red400
)

@Composable
fun FatigueCurveCard(fatigueData: List<Map<MuscleGroup, Float>>) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Curva de Fatiga",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Legend (2x2 grid)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ChartLegendDot("Cuadriceps", Blue400)
                ChartLegendDot("Gluteos", Terracotta400)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ChartLegendDot("B. Femoral", Blue300)
                ChartLegendDot("Lumbar", Red400)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Chart
            Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                // Y-axis labels
                Column(
                    modifier = Modifier
                        .width(32.dp)
                        .fillMaxHeight()
                        .padding(top = 4.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("100", "75", "50", "25", "0").forEach { label ->
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            textAlign = TextAlign.End,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Canvas
                val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 36.dp, end = 4.dp, top = 4.dp, bottom = 20.dp)
                ) {
                    val chartWidth = size.width
                    val chartHeight = size.height
                    val pointCount = fatigueData.size

                    // Grid lines
                    for (i in 0..4) {
                        val y = chartHeight * (1 - i / 4f)
                        drawLine(
                            gridColor,
                            Offset(0f, y),
                            Offset(chartWidth, y),
                            strokeWidth = 0.5.dp.toPx()
                        )
                    }

                    // Lines per muscle
                    muscleChartColors.forEach { (muscle, color) ->
                        val path = Path()
                        fatigueData.forEachIndexed { index, data ->
                            val x = if (pointCount > 1) {
                                chartWidth * index / (pointCount - 1)
                            } else chartWidth / 2
                            val y = chartHeight * (1 - (data[muscle] ?: 0f) / 100f)
                            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(
                            path, color,
                            style = Stroke(
                                width = 2.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                        // Data dots
                        fatigueData.forEachIndexed { index, data ->
                            val x = if (pointCount > 1) {
                                chartWidth * index / (pointCount - 1)
                            } else chartWidth / 2
                            val y = chartHeight * (1 - (data[muscle] ?: 0f) / 100f)
                            drawCircle(color, radius = 3.dp.toPx(), center = Offset(x, y))
                        }
                    }
                }

                // X-axis labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .padding(start = 36.dp, end = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    fatigueData.indices.forEach { i ->
                        Text(
                            "R${i + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChartLegendDot(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─── Series Metrics ──────────────────────────────────────────────────

@Composable
fun SeriesMetricsRow(metrics: SeriesMetrics) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MetricCard(
            modifier = Modifier.weight(0.6f),
            title = "Simetría",
            value = "${"%.0f".format(metrics.symmetryIndex)}%",
            valueColor = if (metrics.symmetryIndex >= 85f)
                MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.secondary
        )
        MetricCard(
            modifier = Modifier.weight(1.2f),
            title = "Pico: ${"%.0f".format(metrics.peakActivation)}%",
            value = "R${metrics.peakRep}: ${metrics.peakMuscle.displayName} ${metrics.peakSide}",
            valueColor = MaterialTheme.colorScheme.primary
        )
        MetricCard(
            modifier = Modifier.weight(1.2f),
            title = "Compensación lumbar",
            value = "${"%.2f".format(metrics.compensationIndex)}",
            valueColor = if (metrics.compensationIndex <= 0.7f)
                MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.error
        )
    }
}

@Composable
fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    valueColor: Color
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = valueColor
            )
        }
    }
}

// ─── Series Selector & Info ──────────────────────────────────────────

@Composable
fun SeriesSelector(
    series: List<Series>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary,
        edgePadding = 16.dp
    ) {
        series.forEachIndexed { index, s ->
            Tab(
                selected = selectedIndex == index,
                onClick = { onSelect(index) },
                text = {
                    Text(
                        "Serie ${s.number} \u00B7 ${s.weightKg.toInt()}kg",
                        fontWeight = if (selectedIndex == index) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SeriesInfoStrip(series: Series, totalSeries: Int, qualityScore: Int?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            InfoColumn("Peso", "${series.weightKg.toInt()} kg")
            InfoColumn("Reps", "${series.repetitions.size}")
            InfoColumn("Serie", "${series.number}/$totalSeries")
            qualityScore?.let {
                InfoColumn(
                    label = "Score",
                    value = "$it",
                    valueColor = scoreColor(it)
                )
            }
        }
    }
}

@Composable
fun InfoColumn(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

// ─── Repetition Cards ────────────────────────────────────────────────

@Composable
fun RepetitionCard(repetition: Repetition, quality: RepQuality? = null) {
    val activations = SessionDetailViewModel.getActivationsForRep(repetition)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Repeticion ${repetition.number}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    quality?.let { QualityBadge(it) }
                }
                // Legend
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LegendItem("Izq", MaterialTheme.colorScheme.primary)
                    LegendItem("Der", MaterialTheme.colorScheme.secondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            activations.forEach { activation ->
                MuscleActivationRow(activation)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun QualityBadge(quality: RepQuality) {
    val color = when (quality.level) {
        RepQualityLevel.GOOD -> MaterialTheme.colorScheme.primary
        RepQualityLevel.ACCEPTABLE -> MaterialTheme.colorScheme.secondary
        RepQualityLevel.POOR -> MaterialTheme.colorScheme.error
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            "${quality.score}",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─── Muscle Activation Bars ──────────────────────────────────────────

@Composable
fun MuscleActivationRow(activation: MuscleActivation) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = activation.muscle.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.width(120.dp)
            )
            if (activation.hasImbalance) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = "Desbalance",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${"%.1f".format(activation.difference)}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        ActivationBar(
            label = "Izq",
            percent = activation.leftPercent,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        ActivationBar(
            label = "Der",
            percent = activation.rightPercent,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
fun ActivationBar(label: String, percent: Float, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(30.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.12f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = (percent / 100f).coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(10.dp))
                    .background(color)
            )
        }
        Text(
            text = "${"%.0f".format(percent)}%",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.width(42.dp),
            textAlign = TextAlign.End
        )
    }
}

// ─── Recommendations ─────────────────────────────────────────────────

@Composable
fun RecommendationsCard(recommendations: List<String>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "Recomendaciones",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            recommendations.forEachIndexed { index, rec ->
                if (index > 0) Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "\u2022",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(rec, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────

@Composable
fun scoreColor(score: Int): Color = when {
    score >= 75 -> MaterialTheme.colorScheme.primary
    score >= 50 -> MaterialTheme.colorScheme.secondary
    else -> MaterialTheme.colorScheme.error
}
