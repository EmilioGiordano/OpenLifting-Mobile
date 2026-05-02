package com.openlifting.presentation.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.openlifting.domain.model.UserRole
import com.openlifting.presentation.common.PlaceholderScreen

@Composable
fun LoginScreen(
    onLoginSuccess: (UserRole) -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.checkSession() }

    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) {
            onLoginSuccess((uiState as LoginUiState.Success).role)
        }
    }

    PlaceholderScreen(
        title       = "Login",
        description = "Pantalla de inicio de sesión. Pulsá el botón para autenticarte con credenciales mock (test@test.com / 1234).",
        stateLabel  = uiState::class.simpleName?.let { "STATE · ${it.uppercase()}" },
        primaryAction   = "Ingresar (mock)" to { viewModel.login("test@test.com", "1234") },
        secondaryAction = "Crear cuenta"     to onNavigateToRegister
    )
}
