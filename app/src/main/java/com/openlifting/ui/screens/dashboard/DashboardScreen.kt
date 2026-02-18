package com.openlifting.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.openlifting.data.model.BalanceStatus
import com.openlifting.data.model.MuscleGroup
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onSessionClick: (String) -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.FitnessCenter,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("OpenLifting", fontWeight = FontWeight.Bold)
                        }
                        if (uiState.userName.isNotEmpty()) {
                            Text(
                                text = "Hola, ${uiState.userName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(2.dp))

            // 1. Balance status hero card with score
            BalanceStatusCard(
                status = uiState.balanceStatus,
                qualityScore = uiState.qualityScore,
                alertSummary = uiState.alertSummary
            )

            // 2. Muscle activation card
            if (uiState.muscleAverages.isNotEmpty()) {
                MuscleActivationCard(muscleAverages = uiState.muscleAverages)
            }

            // 3. Compact stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CompactStatCard(
                    modifier = Modifier.weight(1f),
                    value = "${uiState.totalSessions}",
                    label = "Sesiones totales"
                )
                CompactStatCard(
                    modifier = Modifier.weight(1f),
                    value = "${uiState.maxWeightHistoric} kg",
                    label = "Mejor peso"
                )
            }

            // 4. Latest session card with score
            uiState.latestSession?.let { session ->
                Text(
                    text = "Ultima sesion",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSessionClick(session.id) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Leading icon
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.FitnessCenter,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))

                        // Date + stats
                        Column(modifier = Modifier.weight(1f)) {
                            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                            Text(
                                text = session.date.format(formatter),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            val maxW = session.series.maxOfOrNull { it.weightKg } ?: 0f
                            Text(
                                text = "${session.series.size} series  \u2022  ${maxW.toInt()} kg",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Quality score badge
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = "${uiState.qualityScore}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = scoreColor(uiState.qualityScore)
                            )
                            Text(
                                text = "pts",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Ver detalle",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 5. Recommendations with severity
            if (uiState.recommendations.isNotEmpty()) {
                Text(
                    text = "Recomendaciones",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                RecommendationsCard(recommendations = uiState.recommendations)
            }

            // 6. Progress trend sparkline
            if (uiState.sessionScoreTrend.size >= 2) {
                Text(
                    text = "Progreso reciente",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SessionTrendCard(scoreTrend = uiState.sessionScoreTrend)
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// --- Balance Status Hero Card ---

@Composable
fun BalanceStatusCard(
    status: BalanceStatus,
    qualityScore: Int,
    alertSummary: String?
) {
    val (icon, color) = when (status) {
        BalanceStatus.GOOD -> Icons.Filled.CheckCircle to MaterialTheme.colorScheme.primary
        BalanceStatus.WARNING -> Icons.Filled.Warning to MaterialTheme.colorScheme.secondary
        BalanceStatus.ALERT -> Icons.Filled.Warning to MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.08f)
        ),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Balance Muscular",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))

                // Score + status chip row
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "$qualityScore",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                    Text(
                        text = "/100",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    // Status chip
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = color.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = status.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = color,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // Alert summary
                if (alertSummary != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = alertSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// --- Muscle Activation Card ---

@Composable
fun MuscleActivationCard(
    muscleAverages: Map<MuscleGroup, Float>,
    modifier: Modifier = Modifier
) {
    val orderedMuscles = listOf(
        MuscleGroup.QUADRICEPS,
        MuscleGroup.GLUTES,
        MuscleGroup.HAMSTRINGS,
        MuscleGroup.LOWER_BACK
    )

    // Detect alert: glutes significantly lower than lower back
    val avgGlute = muscleAverages[MuscleGroup.GLUTES] ?: 0f
    val avgLumbar = muscleAverages[MuscleGroup.LOWER_BACK] ?: 0f
    val gluteAlert = avgLumbar > avgGlute * 0.7f

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Activacion Muscular",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Ultima serie",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(14.dp))

            orderedMuscles.forEach { muscle ->
                val pct = muscleAverages[muscle] ?: 0f
                val showAlert = when (muscle) {
                    MuscleGroup.GLUTES -> gluteAlert
                    MuscleGroup.LOWER_BACK -> gluteAlert
                    else -> false
                }
                MuscleBarRow(
                    muscleName = muscle.displayName,
                    percent = pct,
                    showAlert = showAlert,
                    alertColor = if (muscle == MuscleGroup.LOWER_BACK)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.secondary
                )
                if (muscle != orderedMuscles.last()) {
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun MuscleBarRow(
    muscleName: String,
    percent: Float,
    showAlert: Boolean,
    alertColor: Color = MaterialTheme.colorScheme.secondary
) {
    val barColor = when {
        showAlert -> alertColor
        percent > 50f -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = muscleName,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(100.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = (percent / 100f).coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${percent.toInt()}%",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(34.dp),
            textAlign = TextAlign.End,
            fontWeight = FontWeight.SemiBold,
            color = if (showAlert) alertColor else MaterialTheme.colorScheme.onSurface
        )
        if (showAlert) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = "Alerta en $muscleName",
                tint = alertColor,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(14.dp)
            )
        } else {
            Spacer(modifier = Modifier.width(18.dp))
        }
    }
}

// --- Compact Stat Card ---

@Composable
fun CompactStatCard(modifier: Modifier = Modifier, value: String, label: String) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// --- Recommendations Card with severity ---

@Composable
fun RecommendationsCard(recommendations: List<DashboardRecommendation>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            recommendations.forEachIndexed { index, rec ->
                if (index > 0) Spacer(modifier = Modifier.height(10.dp))

                val (icon, tint) = when (rec.severity) {
                    RecommendationSeverity.HIGH -> Icons.Filled.Warning to MaterialTheme.colorScheme.secondary
                    RecommendationSeverity.MEDIUM -> Icons.Filled.Info to MaterialTheme.colorScheme.primary
                    RecommendationSeverity.LOW -> Icons.Filled.CheckCircle to MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = rec.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (rec.severity == RecommendationSeverity.LOW)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// --- Session Trend Card ---

@Composable
fun SessionTrendCard(
    scoreTrend: List<Int>,
    modifier: Modifier = Modifier
) {
    if (scoreTrend.size < 2) return

    val maxScore = scoreTrend.maxOrNull()?.coerceAtLeast(1) ?: 1
    val delta = scoreTrend.last() - scoreTrend.dropLast(1).average().toInt()

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Puntaje por sesion",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                val trendColor = if (delta >= 0)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error
                val trendLabel = if (delta >= 0) "+$delta pts" else "$delta pts"
                Text(
                    text = trendLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = trendColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(14.dp))

            // Sparkline bar chart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                scoreTrend.forEachIndexed { index, score ->
                    val isLast = index == scoreTrend.lastIndex
                    val fraction = score.toFloat() / maxScore.toFloat()
                    val barColor = if (isLast)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(fraction.coerceIn(0.05f, 1f))
                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .background(barColor)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Sesion 1",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Hoy",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// --- Helpers ---

@Composable
private fun scoreColor(score: Int): Color = when {
    score >= 70 -> MaterialTheme.colorScheme.primary
    score >= 45 -> MaterialTheme.colorScheme.secondary
    else -> MaterialTheme.colorScheme.error
}
