package com.openlifting.presentation.athlete.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.openlifting.presentation.common.PlaceholderScreen

@Composable
fun SessionDetailScreen(
    sessionId: Long,
    onBack: () -> Unit,
    viewModel: SessionDetailViewModel = hiltViewModel()
) {
    val sets by viewModel.sets.collectAsState()

    LaunchedEffect(sessionId) { viewModel.load(sessionId) }

    PlaceholderScreen(
        title       = "Detalle de sesión",
        description = "Sesión #$sessionId · ${sets.size} serie${if (sets.size == 1) "" else "s"}. Acá va a vivir el detalle plegable por serie con bilateral block + métricas + recomendaciones.",
        secondaryAction = "Volver" to onBack
    )
}
