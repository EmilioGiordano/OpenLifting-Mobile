package com.openlifting.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.openlifting.domain.model.UserRole
import com.openlifting.presentation.auth.LoginScreen
import com.openlifting.presentation.auth.RegisterScreen
import com.openlifting.presentation.auth.SplashScreen

object Route {
    const val SPLASH          = "splash"
    const val LOGIN           = "login"
    const val REGISTER        = "register"
    const val ATHLETE_ROOT    = "athlete"
    const val INSTRUCTOR_ROOT = "instructor"
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    fun goToHome(role: UserRole, popUpFrom: String) {
        val dest = if (role == UserRole.ATHLETE) Route.ATHLETE_ROOT else Route.INSTRUCTOR_ROOT
        navController.navigate(dest) {
            popUpTo(popUpFrom) { inclusive = true }
        }
    }

    NavHost(navController = navController, startDestination = Route.SPLASH) {

        composable(Route.SPLASH) {
            SplashScreen(
                onSessionFound = { role -> goToHome(role, popUpFrom = Route.SPLASH) },
                onNoSession    = {
                    navController.navigate(Route.LOGIN) {
                        popUpTo(Route.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Route.LOGIN) {
            LoginScreen(
                onLoginSuccess       = { role -> goToHome(role, popUpFrom = Route.LOGIN) },
                onNavigateToRegister = { navController.navigate(Route.REGISTER) }
            )
        }

        composable(Route.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = { role -> goToHome(role, popUpFrom = Route.LOGIN) },
                onNavigateBack    = { navController.popBackStack() }
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
