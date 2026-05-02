package com.openlifting.presentation.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.openlifting.domain.model.UserRole
import com.openlifting.presentation.common.PlaceholderScreen

@Composable
fun RegisterScreen(
    onRegisterSuccess: (UserRole) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) {
            onRegisterSuccess((uiState as LoginUiState.Success).role)
        }
    }

    PlaceholderScreen(
        title       = "Registro",
        description = "Crear cuenta nueva. El stub registra un atleta mock (Test User / test@test.com).",
        stateLabel  = uiState::class.simpleName?.let { "STATE · ${it.uppercase()}" },
        primaryAction   = "Registrarse como atleta (mock)" to {
            viewModel.register("Test User", "test@test.com", "1234", UserRole.ATHLETE)
        },
        secondaryAction = "Ya tengo cuenta" to onNavigateBack
    )
}
