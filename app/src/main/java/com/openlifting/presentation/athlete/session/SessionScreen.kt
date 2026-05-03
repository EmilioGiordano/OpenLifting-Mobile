package com.openlifting.presentation.athlete.session

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.openlifting.domain.model.Muscle
import com.openlifting.domain.model.MusclePair
import com.openlifting.domain.model.RepPhase
import com.openlifting.domain.model.RiskLevel
import com.openlifting.domain.model.SetMetrics
import com.openlifting.domain.model.SquatDepth
import com.openlifting.domain.model.SquatVariant
import com.openlifting.presentation.common.RiskBadge
import com.openlifting.ui.theme.MonoText
import com.openlifting.ui.theme.olExtras

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(
    onFinish: () -> Unit,
    viewModel: SessionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            SessionTopBar(
                title = when (uiState) {
                    is SessionUiState.SessionSummary -> "Sesión completada"
                    else -> "Serie ${viewModel.currentSetNumber()}"
                },
                showFinishAction = uiState is SessionUiState.MetadataEntry,
                onBack   = { viewModel.finalizeSession(onFinish) },
                onFinish = { viewModel.finalizeSession(onFinish) }
            )

            when (val state = uiState) {
                is SessionUiState.MetadataEntry -> MetadataContent(
                    setNumber = viewModel.currentSetNumber(),
                    onMeasure = viewModel::measureSet
                )
                is SessionUiState.MeasuringInProgress -> MeasuringInProgressContent(state)
                is SessionUiState.AnalysisReady -> AnalysisContent(
                    state = state,
                    onNextSet = viewModel::nextSet,
                    onFinalize = { viewModel.finalizeSession(onFinish) }
                )
                is SessionUiState.SessionSummary -> SummaryContent(
                    state = state,
                    onExit = { viewModel.exitSummary(onFinish) }
                )
                is SessionUiState.Error -> ErrorContent(state.message)
            }
        }
    }
}

// ── Top bar ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionTopBar(
    title: String,
    showFinishAction: Boolean,
    onBack: () -> Unit,
    onFinish: () -> Unit
) {
    TopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
        },
        actions = {
            if (showFinishAction) {
                TextButton(onClick = onFinish) {
                    Text("Finalizar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

// ── Metadata (form before measuring) ────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MetadataContent(
    setNumber: Int,
    onMeasure: (Float, Int, SquatVariant, SquatDepth, Float) -> Unit
) {
    var loadKg     by remember { mutableStateOf("100") }
    var reps       by remember { mutableIntStateOf(5) }
    var variant    by remember { mutableStateOf(SquatVariant.LOW_BAR) }
    var depth      by remember { mutableStateOf(SquatDepth.PARALLEL) }
    var rpe        by remember { mutableFloatStateOf(7f) }
    var variantOpen by remember { mutableStateOf(false) }
    var depthOpen   by remember { mutableStateOf(false) }

    val canSubmit = (loadKg.toFloatOrNull() ?: 0f) > 0f && reps > 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text  = "DATOS DE LA SERIE",
            style = MonoText.labelSmall,
            color = MaterialTheme.olExtras.ink3,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Peso
        OutlinedTextField(
            value         = loadKg,
            onValueChange = { loadKg = it.filter { c -> c.isDigit() || c == '.' } },
            label         = { Text("Peso") },
            suffix        = { Text("kg", style = MonoText.labelMedium) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine    = true,
            modifier      = Modifier.fillMaxWidth()
        )

        // Reps stepper
        StepperField(
            label      = "Reps objetivo",
            value      = reps,
            onChange   = { reps = it.coerceIn(1, 20) },
            range      = 1..20
        )

        // Variant dropdown
        ExposedDropdownMenuBox(expanded = variantOpen, onExpandedChange = { variantOpen = it }) {
            OutlinedTextField(
                value         = variant.displayName,
                onValueChange = {},
                readOnly      = true,
                label         = { Text("Variante") },
                trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(variantOpen) },
                modifier      = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = variantOpen, onDismissRequest = { variantOpen = false }
            ) {
                SquatVariant.entries.forEach { v ->
                    DropdownMenuItem(
                        text    = { Text(v.displayName) },
                        onClick = { variant = v; variantOpen = false }
                    )
                }
            }
        }

        // Depth dropdown
        ExposedDropdownMenuBox(expanded = depthOpen, onExpandedChange = { depthOpen = it }) {
            OutlinedTextField(
                value         = depth.displayName,
                onValueChange = {},
                readOnly      = true,
                label         = { Text("Profundidad") },
                trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(depthOpen) },
                modifier      = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = depthOpen, onDismissRequest = { depthOpen = false }
            ) {
                SquatDepth.entries.forEach { d ->
                    DropdownMenuItem(
                        text    = { Text(d.displayName) },
                        onClick = { depth = d; depthOpen = false }
                    )
                }
            }
        }

        // RPE slider
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("RPE", style = MaterialTheme.typography.labelLarge)
                Text(
                    text  = formatRpeValue(rpe),
                    style = MonoText.titleLarge,
                    color = MaterialTheme.olExtras.emerald
                )
            }
            Slider(
                value         = rpe,
                onValueChange = { rpe = (it * 2f).toInt() / 2f },  // step 0.5
                valueRange    = 1f..10f,
                steps         = 17  // (10-1)/0.5 - 1 = 17 inner steps
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("1", style = MonoText.labelSmall, color = MaterialTheme.olExtras.ink3)
                Text("10", style = MonoText.labelSmall, color = MaterialTheme.olExtras.ink3)
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick  = {
                onMeasure(loadKg.toFloatOrNull() ?: 0f, reps, variant, depth, rpe)
            },
            enabled  = canSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(bottom = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Bolt,
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .padding(end = 6.dp)
            )
            Text("Simular medición", style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun StepperField(label: String, value: Int, onChange: (Int) -> Unit, range: IntRange) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        StepperButton("−", enabled = value > range.first) { onChange(value - 1) }
        Box(modifier = Modifier.size(width = 48.dp, height = 32.dp), contentAlignment = Alignment.Center) {
            Text(
                text  = value.toString(),
                style = MonoText.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        StepperButton("+", enabled = value < range.last) { onChange(value + 1) }
    }
}

@Composable
private fun StepperButton(symbol: String, enabled: Boolean, onClick: () -> Unit) {
    val color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.olExtras.ink3
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(32.dp)) {
        Text(symbol, style = MonoText.titleLarge, color = color)
    }
}

private fun formatRpeValue(rpe: Float): String =
    if (rpe == rpe.toInt().toFloat()) rpe.toInt().toString()
    else "%.1f".format(rpe)

// ── Measuring in progress (live realtime stream) ───────────────────────────

@Composable
private fun MeasuringInProgressContent(state: SessionUiState.MeasuringInProgress) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header: rep counter, timer, phase chip
        LiveHeaderCard(state)

        // Set context (load × reps + chips) — reused from analysis
        SetHeader(
            loadKg     = state.loadKg,
            targetReps = state.targetReps,
            rpe        = state.rpe,
            variant    = state.variant,
            depth      = state.depth
        )

        // Live bilateral block
        Text(
            text     = "Activación muscular en vivo",
            style    = MaterialTheme.typography.labelLarge,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Muscle.entries.forEach { muscle ->
                val live = state.liveActivations[muscle] ?: MusclePair(0f, 0f)
                val peak = state.peaksThisRep[muscle] ?: MusclePair(0f, 0f)
                LiveBilateralRow(muscle = muscle, live = live, peak = peak)
            }
        }

        // Footer: captured reps strip
        if (state.capturedReps.isNotEmpty()) {
            CapturedRepsStrip(reps = state.capturedReps, totalReps = state.targetReps)
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun LiveHeaderCard(state: SessionUiState.MeasuringInProgress) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Rep counter
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text  = "REP",
                style = MonoText.labelSmall,
                color = MaterialTheme.olExtras.ink3
            )
            Text(
                text  = "${state.currentRep.coerceAtLeast(0)} / ${state.targetReps}",
                style = MonoText.displaySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Phase chip
        PhaseChip(phase = state.phase)

        // Timer
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text  = "TIEMPO",
                style = MonoText.labelSmall,
                color = MaterialTheme.olExtras.ink3
            )
            Text(
                text  = formatTimer(state.totalElapsedMs),
                style = MonoText.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun PhaseChip(phase: RepPhase?) {
    val (label, fg, bg) = when (phase) {
        RepPhase.ECCENTRIC  -> Triple("EXCÉNTRICA",  MaterialTheme.olExtras.amberInk, MaterialTheme.olExtras.amberSoft)
        RepPhase.ISOMETRIC  -> Triple("PARADA",      MaterialTheme.olExtras.ink3,     MaterialTheme.colorScheme.surfaceVariant)
        RepPhase.CONCENTRIC -> Triple("CONCÉNTRICA", MaterialTheme.olExtras.emerald,  MaterialTheme.olExtras.emeraldSoft)
        null                 -> Triple("ESPERANDO",   MaterialTheme.olExtras.ink3,     MaterialTheme.colorScheme.surfaceVariant)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text = label, style = MonoText.labelMedium, color = fg)
    }
}

@Composable
private fun LiveBilateralRow(muscle: Muscle, live: MusclePair, peak: MusclePair) {
    val animatedL by animateFloatAsState(targetValue = live.left,  label = "${muscle.shortName}-L")
    val animatedR by animateFloatAsState(targetValue = live.right, label = "${muscle.shortName}-R")
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text  = muscle.shortName,
            style = MonoText.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(40.dp)
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            LiveSideBar(label = "IZQ", pct = animatedL, peakPct = peak.left)
            LiveSideBar(label = "DER", pct = animatedR, peakPct = peak.right)
        }
    }
}

@Composable
private fun LiveSideBar(label: String, pct: Float, peakPct: Float) {
    val barColor = liveBarColor(pct)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text  = label,
            style = MonoText.labelSmall,
            color = MaterialTheme.olExtras.ink3,
            modifier = Modifier.width(24.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            // Bar fill
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = (pct / 100f).coerceIn(0f, 1f))
                    .background(barColor, RoundedCornerShape(4.dp))
            )
            // Peak marker (subtle vertical line at peak position)
            if (peakPct > 1f && peakPct >= pct) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = (peakPct / 100f).coerceIn(0f, 1f))
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.onSurface)
                    )
                }
            }
        }
        Text(
            text  = "${pct.toInt()}%",
            style = MonoText.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(36.dp)
        )
    }
}

@Composable
private fun liveBarColor(pct: Float): androidx.compose.ui.graphics.Color = when {
    pct >= 90f -> MaterialTheme.olExtras.warn       // saturating
    pct >= 60f -> MaterialTheme.olExtras.emerald    // active
    else       -> MaterialTheme.olExtras.emerald.copy(alpha = 0.6f)  // low/resting
}

@Composable
private fun CapturedRepsStrip(reps: List<com.openlifting.presentation.athlete.session.RepCapture>, totalReps: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text  = "CAPTURADAS",
            style = MonoText.labelSmall,
            color = MaterialTheme.olExtras.ink3
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (1..totalReps).forEach { repNum ->
                val captured = reps.any { it.repNumber == repNum }
                RepDot(repNumber = repNum, captured = captured)
            }
        }
    }
}

@Composable
private fun RepDot(repNumber: Int, captured: Boolean) {
    val color = if (captured) MaterialTheme.olExtras.emerald else MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(if (captured) MaterialTheme.olExtras.emeraldSoft else androidx.compose.ui.graphics.Color.Transparent)
            .border(1.dp, color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text  = repNumber.toString(),
            style = MonoText.labelSmall,
            color = if (captured) MaterialTheme.olExtras.emerald else MaterialTheme.olExtras.ink3
        )
    }
}

private fun formatTimer(elapsedMs: Long): String {
    val totalSec = elapsedMs / 1000L
    val min = totalSec / 60L
    val sec = totalSec % 60L
    val tenths = (elapsedMs % 1000L) / 100L
    return "%02d:%02d.%01d".format(min, sec, tenths)
}

// ── Analysis (after measurement) ────────────────────────────────────────────

@Composable
private fun AnalysisContent(
    state: SessionUiState.AnalysisReady,
    onNextSet: () -> Unit,
    onFinalize: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(Modifier.height(4.dp)) }

        item {
            SetHeader(
                loadKg     = state.loadKg,
                targetReps = state.targetReps,
                rpe        = state.rpe,
                variant    = state.variant,
                depth      = state.depth
            )
        }

        // Bilateral block
        item {
            Text(
                text  = "Activación muscular (%MVC)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        items(Muscle.entries) { muscle ->
            val pair = state.activations[muscle] ?: MusclePair(0f, 0f)
            val (bsa, risk) = bsaForMuscle(state.metrics, muscle)
            BilateralRow(
                muscle    = muscle,
                leftPct   = pair.left,
                rightPct  = pair.right,
                bsaPct    = bsa,
                risk      = risk
            )
        }

        // Metric cards 2x2
        item {
            Text(
                text  = "Métricas",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    label = "BSA peor",
                    value = "%.1f%%".format(state.metrics.bsaWorstPct),
                    risk  = listOf(state.metrics.vlRisk, state.metrics.vmRisk, state.metrics.gmaxRisk, state.metrics.esRisk)
                                .maxByOrNull { it.ordinal } ?: RiskLevel.NORMAL,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = "ES:GMax",
                    value = "%.2f".format(state.metrics.esGmaxRatio),
                    risk  = state.metrics.esGmaxRisk,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    label = "H:Q",
                    value = "%.2f".format(state.metrics.hqRatio),
                    risk  = state.metrics.hqRisk,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = "Fatiga",
                    value = "%.2f".format(state.metrics.intraSetFatigueRatio),
                    risk  = state.metrics.fatigueRisk,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Recommendations
        if (state.recommendations.isNotEmpty()) {
            item {
                Text(
                    text  = "Recomendaciones",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(state.recommendations) { rec -> RecommendationCard(rec) }
        }

        // Actions
        item {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick  = onNextSet,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Siguiente serie", style = MaterialTheme.typography.labelLarge)
            }
        }
        item {
            OutlinedButton(
                onClick  = onFinalize,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Finalizar sesión")
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

private fun bsaForMuscle(m: SetMetrics, muscle: Muscle): Pair<Float, RiskLevel> = when (muscle) {
    Muscle.VASTUS_LATERALIS -> m.bsaVlPct to m.vlRisk
    Muscle.VASTUS_MEDIALIS  -> m.bsaVmPct to m.vmRisk
    Muscle.GLUTEUS_MAXIMUS  -> m.bsaGmaxPct to m.gmaxRisk
    Muscle.ERECTOR_SPINAE   -> m.bsaEsPct  to m.esRisk
    Muscle.BICEPS_FEMORIS   -> 0f to RiskLevel.NORMAL  // no BSA tracked for BF directly
}

// ── Session Summary ─────────────────────────────────────────────────────────

@Composable
private fun SummaryContent(
    state: SessionUiState.SessionSummary,
    onExit: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(Modifier.height(4.dp)) }

        // Hero summary card
        item {
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text  = "RESUMEN",
                            style = MonoText.labelSmall,
                            color = MaterialTheme.olExtras.ink3
                        )
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text  = state.totalSets.toString(),
                                style = MonoText.displayMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text  = "series completadas",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                    }
                    RiskBadge(state.overallRisk)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SummaryStat("VOLUMEN", "${state.totalVolumeKg.toInt()} kg")
                    SummaryStat("MÁX",     "${state.maxLoadKg.toInt()} kg")
                    SummaryStat("DURACIÓN", state.durationMinutes?.let { "$it min" } ?: "—")
                }
            }
        }

        // Sets recap
        item {
            Text(
                text  = "Series",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        item { SetsRecapTable(items = state.sets) }

        // Top recommendations
        if (state.topRecommendations.isNotEmpty()) {
            item {
                Text(
                    text  = "Recomendaciones",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(state.topRecommendations) { rec -> RecommendationCard(rec) }
        }

        // Exit
        item {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick  = onExit,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Volver al inicio", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: String) {
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

// ── Error ───────────────────────────────────────────────────────────────────

@Composable
private fun ErrorContent(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text  = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(24.dp)
        )
    }
}
