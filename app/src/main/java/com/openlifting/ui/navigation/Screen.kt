package com.openlifting.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    data object Dashboard : Screen("dashboard", "Dashboard", Icons.Filled.Home)
    data object History : Screen("history", "Historial", Icons.Filled.FitnessCenter)
    data object Profile : Screen("profile", "Perfil", Icons.Filled.Person)
    data object SessionDetail : Screen("session/{sessionId}", "Detalle de Sesion") {
        fun createRoute(sessionId: String) = "session/$sessionId"
    }
}

val bottomNavItems = listOf(Screen.Dashboard, Screen.History, Screen.Profile)
