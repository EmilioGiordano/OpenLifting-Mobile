package com.openlifting.presentation.athlete.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.openlifting.presentation.common.PlaceholderScreen

@Composable
fun HistoryScreen(
    onSessionClick: (Long) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val sessions by viewModel.sessions.collectAsState()

    val description = if (sessions.isEmpty()) {
        "Sin sesiones registradas aún. Iniciá una sesión desde el tab 'Nueva' para popular el historial."
    } else {
        "${sessions.size} sesión${if (sessions.size == 1) "" else "es"} registrada${if (sessions.size == 1) "" else "s"}. Pulsá el botón para abrir el detalle de la más reciente."
    }

    val openLatest: (() -> Unit)? = sessions.firstOrNull()?.let { item ->
        { onSessionClick(item.sessionId) }
    }

    PlaceholderScreen(
        title         = "Historial",
        description   = description,
        primaryAction = openLatest?.let { "Abrir última sesión" to it }
    )
}
