package com.openlifting.ui.screens.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.openlifting.data.model.Repetition
import com.openlifting.data.model.Series
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
                }
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
                Text("Sesion no encontrada")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Selector de serie
            SeriesSelector(
                series = session.series,
                selectedIndex = uiState.selectedSeriesIndex,
                onSelect = viewModel::selectSeries
            )

            val selectedSeries = session.series.getOrNull(uiState.selectedSeriesIndex)
            if (selectedSeries != null) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Peso: ${selectedSeries.weightKg.toInt()} kg | ${selectedSeries.repetitions.size} repeticiones",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Repeticiones con graficos
                selectedSeries.repetitions.forEach { rep ->
                    RepetitionCard(rep)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Recomendaciones
            if (uiState.recommendations.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "Recomendaciones",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                uiState.recommendations.forEach { rec ->
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(text = rec, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun SeriesSelector(
    series: List<Series>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        modifier = Modifier.fillMaxWidth()
    ) {
        series.forEachIndexed { index, s ->
            Tab(
                selected = selectedIndex == index,
                onClick = { onSelect(index) },
                text = { Text("Serie ${s.number}") }
            )
        }
    }
}

@Composable
fun RepetitionCard(repetition: Repetition) {
    val activations = SessionDetailViewModel.getActivationsForRep(repetition)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Repeticion ${repetition.number}",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(12.dp))

            activations.forEach { activation ->
                MuscleActivationRow(activation)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

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
                modifier = Modifier.width(120.dp)
            )
            if (activation.hasImbalance) {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = "Desbalance",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Barra izquierda
        ActivationBar(
            label = "Izq",
            percent = activation.leftPercent,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Barra derecha
        ActivationBar(
            label = "Der",
            percent = activation.rightPercent,
            color = MaterialTheme.colorScheme.secondary
        )

        if (activation.hasImbalance) {
            Text(
                text = "Diferencia: ${"%.1f".format(activation.difference)}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun ActivationBar(label: String, percent: Float, color: androidx.compose.ui.graphics.Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(30.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(16.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = (percent / 100f).coerceIn(0f, 1f))
                    .background(color, shape = MaterialTheme.shapes.small)
            )
        }
        Text(
            text = "${"%.0f".format(percent)}%",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(40.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}
