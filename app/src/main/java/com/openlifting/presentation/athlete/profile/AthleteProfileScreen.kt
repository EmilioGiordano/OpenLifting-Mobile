package com.openlifting.presentation.athlete.profile

import androidx.compose.runtime.Composable
import com.openlifting.presentation.common.PlaceholderScreen

@Composable
fun AthleteProfileScreen(
    onLogout: () -> Unit,
    onSwitchToInstructor: () -> Unit
) {
    PlaceholderScreen(
        title       = "Perfil",
        description = "Datos personales del atleta, calibración MVC, vinculación con entrenador, ajustes de tema (light/dark warm) y cierre de sesión.",
        primaryAction   = "Modo demo: cambiar a Entrenador" to onSwitchToInstructor,
        secondaryAction = "Cerrar sesión" to onLogout
    )
}
