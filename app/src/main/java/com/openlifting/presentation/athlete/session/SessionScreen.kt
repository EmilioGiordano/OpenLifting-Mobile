package com.openlifting.presentation.athlete.session

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.openlifting.domain.model.SquatDepth
import com.openlifting.domain.model.SquatVariant
import com.openlifting.presentation.common.PlaceholderScreen

@Composable
fun SessionScreen(
    onFinish: () -> Unit,
    viewModel: SessionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val (description, primaryLabel, primaryAction) = when (val state = uiState) {
        is SessionUiState.MetadataEntry -> Triple(
            "Ingresá los datos de la serie ${viewModel.currentSetNumber()}. El stub usa: 100 kg × 5 reps, Low Bar, Paralela, RPE 7.",
            "Simular medición (mock)",
            { viewModel.measureSet(100f, 5, SquatVariant.LOW_BAR, SquatDepth.PARALLEL, 7f) }
        )
        is SessionUiState.Measuring -> Triple(
            "Simulando captura del ESP32… (~2s)",
            null,
            null
        )
        is SessionUiState.AnalysisReady -> Triple(
            "Análisis listo. Serie ${state.setNumber} — ${state.loadKg.toInt()} kg × ${state.targetReps} reps. " +
                "BSA peor: ${"%.1f".format(state.metrics.bsaWorstPct)}% · ES:GMax ${"%.2f".format(state.metrics.esGmaxRatio)}.",
            "Siguiente serie",
            viewModel::nextSet
        )
        is SessionUiState.Error -> Triple(state.message, null, null)
    }

    PlaceholderScreen(
        title       = "Sesión",
        description = description,
        stateLabel  = "STATE · ${uiState::class.simpleName?.uppercase()}",
        primaryAction   = if (primaryLabel != null && primaryAction != null) primaryLabel to primaryAction else null,
        secondaryAction = "Finalizar sesión" to { viewModel.endSession(onFinish) }
    )
}
