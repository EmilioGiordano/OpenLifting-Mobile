package com.openlifting.presentation.athlete.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.openlifting.domain.model.Muscle
import com.openlifting.domain.model.SquatDepth
import com.openlifting.domain.model.SquatVariant
import com.openlifting.presentation.common.RiskBadge
import com.openlifting.presentation.common.toColor

@Composable
fun SessionScreen(
    onFinish: () -> Unit,
    viewModel: SessionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Surface(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is SessionUiState.MetadataEntry -> SetMetadataContent(
                setNumber = viewModel.currentSetNumber(),
                onMeasure = { kg, reps, variant, depth, rpe ->
                    viewModel.measureSet(kg, reps, variant, depth, rpe)
                },
                onFinish = { viewModel.endSession(onFinish) }
            )
            is SessionUiState.Measuring -> MeasuringContent()
            is SessionUiState.AnalysisReady -> SetAnalysisContent(
                state = state,
                onNextSet = { viewModel.nextSet() },
                onFinish = { viewModel.endSession(onFinish) }
            )
            is SessionUiState.Error -> Box(
                Modifier.fillMaxSize(), contentAlignment = Alignment.Center
            ) { Text(state.message, color = MaterialTheme.colorScheme.error) }
        }
    }
}

// ── Metadata (before measuring) ────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetMetadataContent(
    setNumber: Int,
    onMeasure: (Float, Int, SquatVariant, SquatDepth, Float) -> Unit,
    onFinish: () -> Unit
) {
    var loadKg by remember { mutableStateOf("") }
    var reps by remember { mutableStateOf("5") }
    var variant by remember { mutableStateOf(SquatVariant.HIGH_BAR) }
    var depth by remember { mutableStateOf(SquatDepth.PARALLEL) }
    var rpe by remember { mutableStateOf("7") }
    var variantExpanded by remember { mutableStateOf(false) }
    var depthExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Serie $setNumber", style = MaterialTheme.typography.headlineMedium)
        Text("Ingresá los datos antes de comenzar",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = loadKg, onValueChange = { loadKg = it },
            label = { Text("Peso (kg)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        OutlinedTextField(
            value = reps, onValueChange = { reps = it },
            label = { Text("Reps objetivo") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )

        // Variante
        ExposedDropdownMenuBox(expanded = variantExpanded, onExpandedChange = { variantExpanded = it }) {
            OutlinedTextField(
                value = variant.displayName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Variante") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(variantExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = variantExpanded, onDismissRequest = { variantExpanded = false }) {
                SquatVariant.entries.forEach {
                    DropdownMenuItem(text = { Text(it.displayName) }, onClick = { variant = it; variantExpanded = false })
                }
            }
        }

        // Profundidad
        ExposedDropdownMenuBox(expanded = depthExpanded, onExpandedChange = { depthExpanded = it }) {
            OutlinedTextField(
                value = depth.displayName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Profundidad") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(depthExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = depthExpanded, onDismissRequest = { depthExpanded = false }) {
                SquatDepth.entries.forEach {
                    DropdownMenuItem(text = { Text(it.displayName) }, onClick = { depth = it; depthExpanded = false })
                }
            }
        }

        OutlinedTextField(
            value = rpe, onValueChange = { rpe = it },
            label = { Text("RPE (1-10)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                val kg = loadKg.toFloatOrNull() ?: 80f
                val r = reps.toIntOrNull() ?: 5
                val e = rpe.toFloatOrNull() ?: 7f
                onMeasure(kg, r, variant, depth, e)
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Simular medición") }

        OutlinedButton(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
            Text("Finalizar sesión")
        }
    }
}

// ── Measuring spinner ────────────────────────────────────────────

@Composable
private fun MeasuringContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator()
            Text("Simulando medición del ESP32…",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── Analysis ─────────────────────────────────────────────────────

@Composable
private fun SetAnalysisContent(
    state: SessionUiState.AnalysisReady,
    onNextSet: () -> Unit,
    onFinish: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(16.dp))
            Text("Serie ${state.setNumber} — ${state.loadKg.toInt()} kg × ${state.targetReps} reps",
                style = MaterialTheme.typography.headlineSmall)
        }

        // Bilateral activation bars
        item {
            Text("Activación muscular (%MVC)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp))
        }
        items(Muscle.entries) { muscle ->
            val pair = state.activations[muscle] ?: MusclePair(0f, 0f)
            BilateralMuscleRow(muscle = muscle, pair = pair)
        }

        // Metric cards
        item {
            Text("Métricas", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp))
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("BSA cuád.", "%.1f%%".format(state.metrics.bsaVlPct),
                    state.metrics.vlRisk, Modifier.weight(1f))
                MetricCard("BSA glúteos", "%.1f%%".format(state.metrics.bsaGmaxPct),
                    state.metrics.gmaxRisk, Modifier.weight(1f))
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("ES:GMax", "%.2f".format(state.metrics.esGmaxRatio),
                    state.metrics.esGmaxRisk, Modifier.weight(1f))
                MetricCard("H:Q", "%.2f".format(state.metrics.hqRatio),
                    state.metrics.hqRisk, Modifier.weight(1f))
            }
        }

        // Recommendations
        if (state.recommendations.isNotEmpty()) {
            item {
                Text("Recomendaciones", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp))
            }
            items(state.recommendations) { rec ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = rec.severity.toColor().copy(alpha = 0.08f)
                    )
                ) {
                    Text(rec.text, modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // Actions
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)) {
                Button(onClick = onNextSet, modifier = Modifier.fillMaxWidth()) {
                    Text("Siguiente serie")
                }
                OutlinedButton(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
                    Text("Finalizar sesión")
                }
            }
        }
    }
}

@Composable
private fun BilateralMuscleRow(muscle: Muscle, pair: MusclePair) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(muscle.shortName, style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.width(36.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Izq", style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LinearProgressIndicator(
                        progress = { (pair.left / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.weight(1f).height(6.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("${pair.left.toInt()}%", style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(32.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Der", style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LinearProgressIndicator(
                        progress = { (pair.right / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.weight(1f).height(6.dp),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text("${pair.right.toInt()}%", style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(32.dp))
                }
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, risk: com.openlifting.domain.model.RiskLevel, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall)
            RiskBadge(risk)
        }
    }
}
