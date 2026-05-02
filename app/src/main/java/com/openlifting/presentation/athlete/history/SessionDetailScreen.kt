package com.openlifting.presentation.athlete.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.openlifting.domain.model.Muscle
import com.openlifting.domain.model.RiskLevel
import com.openlifting.domain.model.SetMetrics
import com.openlifting.presentation.athlete.session.BilateralRow
import com.openlifting.presentation.athlete.session.MetricCard
import com.openlifting.presentation.athlete.session.MusclePair
import com.openlifting.presentation.athlete.session.RecommendationCard
import com.openlifting.presentation.athlete.session.SetHeader
import com.openlifting.presentation.common.RiskBadge
import com.openlifting.presentation.common.toColor
import com.openlifting.presentation.common.toSoftColor
import com.openlifting.ui.theme.MonoText
import com.openlifting.ui.theme.olExtras
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DATE_FMT = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("es"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    sessionId: Long,
    onBack: () -> Unit,
    viewModel: SessionDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(sessionId) { viewModel.load(sessionId) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    val title = (uiState as? SessionDetailUiState.Loaded)
                        ?.let { "Sesión ${shortDate(it.data.startedAt)}" }
                        ?: "Detalle de sesión"
                    Text(title, style = MaterialTheme.typography.titleLarge)
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

            when (val state = uiState) {
                SessionDetailUiState.Loading -> LoadingBlock()
                SessionDetailUiState.NotFound -> NotFoundBlock()
                is SessionDetailUiState.Loaded -> LoadedContent(state.data)
            }
        }
    }
}

@Composable
private fun LoadingBlock() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            color       = MaterialTheme.olExtras.emerald,
            strokeWidth = 2.dp
        )
    }
}

@Composable
private fun NotFoundBlock() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text     = "Sesión no encontrada",
            style    = MaterialTheme.typography.bodyLarge,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(24.dp)
        )
    }
}

@Composable
private fun LoadedContent(data: SessionDetailUiData) {
    val expanded = remember { mutableStateOf(setOf<Int>()) }  // expanded set numbers

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(Modifier.height(4.dp)) }
        item { SessionHeroCard(data) }

        item {
            Text(
                text  = "Series",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        items(data.sets) { item ->
            CollapsibleSetCard(
                item       = item,
                isExpanded = item.setNumber in expanded.value,
                onToggle   = {
                    expanded.value = expanded.value.toMutableSet().apply {
                        if (contains(item.setNumber)) remove(item.setNumber)
                        else add(item.setNumber)
                    }
                }
            )
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

// ── Hero summary card ───────────────────────────────────────────────────────

@Composable
private fun SessionHeroCard(data: SessionDetailUiData) {
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
                    text  = "FECHA",
                    style = MonoText.labelSmall,
                    color = MaterialTheme.olExtras.ink3
                )
                Text(
                    text  = DATE_FMT.format(Date(data.startedAt)).replaceFirstChar { it.uppercase() },
                    style = MonoText.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text  = "Sentadilla trasera",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.olExtras.ink3
                )
            }
            RiskBadge(data.overallRisk)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Stat("SERIES",   data.totalSets.toString())
            Stat("VOLUMEN",  "${data.totalVolumeKg.toInt()} kg")
            Stat("MÁX",      "${data.maxLoadKg.toInt()} kg")
            Stat("DURACIÓN", data.durationMinutes?.let { "$it min" } ?: "—")
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text  = label,
            style = MonoText.labelSmall,
            color = MaterialTheme.olExtras.ink3
        )
        Text(
            text  = value,
            style = MonoText.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ── Collapsible per-set card ────────────────────────────────────────────────

@Composable
private fun CollapsibleSetCard(
    item: SetExpandedItem,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
    ) {
        // Header (always visible)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Set number circle
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = item.setNumber.toString(),
                    style = MonoText.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Title + subtitle
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text  = "${item.loadKg.toInt()} kg × ${item.targetReps}",
                    style = MonoText.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text  = "RPE ${formatRpe(item.rpe)} · BSA ${"%.1f".format(item.metrics.bsaWorstPct)}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.olExtras.ink3
                )
            }

            // BSA chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(item.overallRisk.toSoftColor())
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text  = "%.0f%%".format(item.metrics.bsaWorstPct),
                    style = MonoText.labelSmall,
                    color = item.overallRisk.toColor()
                )
            }

            Icon(
                imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Plegar" else "Desplegar",
                tint = MaterialTheme.olExtras.ink3,
                modifier = Modifier.size(20.dp)
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ExpandedContent(item = item)
            }
        }
    }
}

@Composable
private fun ExpandedContent(item: SetExpandedItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SetHeader(
            loadKg     = item.loadKg,
            targetReps = item.targetReps,
            rpe        = item.rpe,
            variant    = item.variant,
            depth      = item.depth
        )

        // Bilateral block
        Text(
            text  = "Activación muscular (%MVC)",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Muscle.entries.forEach { muscle ->
                val pair = item.activations[muscle] ?: MusclePair(0f, 0f)
                val (bsa, risk) = bsaForMuscle(item.metrics, muscle)
                BilateralRow(
                    muscle   = muscle,
                    leftPct  = pair.left,
                    rightPct = pair.right,
                    bsaPct   = bsa,
                    risk     = risk
                )
            }
        }

        // Metric cards 2x2
        Text(
            text  = "Métricas",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(
                label = "BSA peor",
                value = "%.1f%%".format(item.metrics.bsaWorstPct),
                risk  = listOf(item.metrics.vlRisk, item.metrics.vmRisk, item.metrics.gmaxRisk, item.metrics.esRisk)
                            .maxByOrNull { it.ordinal } ?: RiskLevel.NORMAL,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label = "ES:GMax",
                value = "%.2f".format(item.metrics.esGmaxRatio),
                risk  = item.metrics.esGmaxRisk,
                modifier = Modifier.weight(1f)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(
                label = "H:Q",
                value = "%.2f".format(item.metrics.hqRatio),
                risk  = item.metrics.hqRisk,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label = "Fatiga",
                value = "%.2f".format(item.metrics.intraSetFatigueRatio),
                risk  = item.metrics.fatigueRisk,
                modifier = Modifier.weight(1f)
            )
        }

        if (item.recommendations.isNotEmpty()) {
            Text(
                text  = "Recomendaciones",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            item.recommendations.forEach { rec -> RecommendationCard(rec) }
        }
    }
}

private fun bsaForMuscle(m: SetMetrics, muscle: Muscle): Pair<Float, RiskLevel> = when (muscle) {
    Muscle.VASTUS_LATERALIS -> m.bsaVlPct  to m.vlRisk
    Muscle.VASTUS_MEDIALIS  -> m.bsaVmPct  to m.vmRisk
    Muscle.GLUTEUS_MAXIMUS  -> m.bsaGmaxPct to m.gmaxRisk
    Muscle.ERECTOR_SPINAE   -> m.bsaEsPct  to m.esRisk
    Muscle.BICEPS_FEMORIS   -> 0f to RiskLevel.NORMAL  // BSA not tracked for BF
}

private fun formatRpe(rpe: Float): String =
    if (rpe == rpe.toInt().toFloat()) rpe.toInt().toString()
    else "%.1f".format(rpe)

private fun shortDate(timestamp: Long): String =
    SimpleDateFormat("dd/MM", Locale("es")).format(Date(timestamp))
