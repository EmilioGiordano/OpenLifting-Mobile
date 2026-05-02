package com.openlifting.presentation.athlete.home

import androidx.compose.runtime.Composable
import com.openlifting.presentation.common.PlaceholderScreen

@Composable
fun AthleteHomeScreen(
    onNewSession: () -> Unit
) {
    PlaceholderScreen(
        title       = "Inicio",
        description = "Dashboard del atleta. Acá va a estar el resumen de la última sesión, la tendencia de BSA, y el CTA para comenzar una sesión nueva.",
        primaryAction = "Nueva sesión" to onNewSession
    )
}
