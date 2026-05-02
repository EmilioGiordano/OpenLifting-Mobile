package com.openlifting.presentation.instructor.profile

import androidx.compose.runtime.Composable
import com.openlifting.presentation.common.PlaceholderScreen

@Composable
fun InstructorProfileScreen(
    onLogout: () -> Unit,
    onSwitchToAthlete: () -> Unit
) {
    PlaceholderScreen(
        title       = "Perfil — Entrenador",
        description = "Datos del entrenador, contadores de atletas, ajustes de tema y cierre de sesión.",
        primaryAction   = "Modo demo: cambiar a Atleta" to onSwitchToAthlete,
        secondaryAction = "Cerrar sesión" to onLogout
    )
}
