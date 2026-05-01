package com.openlifting.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.openlifting.domain.model.UserRole
import com.openlifting.presentation.auth.LoginScreen
import com.openlifting.presentation.auth.LoginViewModel
import com.openlifting.presentation.auth.LoginUiState
import com.openlifting.presentation.auth.RegisterScreen

object Route {
    const val LOGIN         = "login"
    const val REGISTER      = "register"
    const val ATHLETE_ROOT  = "athlete"
    const val INSTRUCTOR_ROOT = "instructor"
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val loginViewModel: LoginViewModel = hiltViewModel()
    val uiState by loginViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { loginViewModel.checkSession() }

    LaunchedEffect(uiState) {
        when (uiState) {
            is LoginUiState.Success -> {
                val role = (uiState as LoginUiState.Success).role
                val dest = if (role == UserRole.ATHLETE) Route.ATHLETE_ROOT else Route.INSTRUCTOR_ROOT
                navController.navigate(dest) {
                    popUpTo(Route.LOGIN) { inclusive = true }
                }
            }
            else -> Unit
        }
    }

    NavHost(navController = navController, startDestination = Route.LOGIN) {

        composable(Route.LOGIN) {
            LoginScreen(
                onLoginSuccess = { role ->
                    val dest = if (role == UserRole.ATHLETE) Route.ATHLETE_ROOT else Route.INSTRUCTOR_ROOT
                    navController.navigate(dest) {
                        popUpTo(Route.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Route.REGISTER) }
            )
        }

        composable(Route.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = { role ->
                    val dest = if (role == UserRole.ATHLETE) Route.ATHLETE_ROOT else Route.INSTRUCTOR_ROOT
                    navController.navigate(dest) {
                        popUpTo(Route.LOGIN) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Route.ATHLETE_ROOT) {
            AthleteScaffold(
                onLogout = {
                    navController.navigate(Route.LOGIN) {
                        popUpTo(Route.ATHLETE_ROOT) { inclusive = true }
                    }
                },
                onSwitchToInstructor = {
                    navController.navigate(Route.INSTRUCTOR_ROOT) {
                        popUpTo(Route.ATHLETE_ROOT) { inclusive = true }
                    }
                }
            )
        }

        composable(Route.INSTRUCTOR_ROOT) {
            InstructorScaffold(
                onLogout = {
                    navController.navigate(Route.LOGIN) {
                        popUpTo(Route.INSTRUCTOR_ROOT) { inclusive = true }
                    }
                },
                onSwitchToAthlete = {
                    navController.navigate(Route.ATHLETE_ROOT) {
                        popUpTo(Route.INSTRUCTOR_ROOT) { inclusive = true }
                    }
                }
            )
        }
    }
}
