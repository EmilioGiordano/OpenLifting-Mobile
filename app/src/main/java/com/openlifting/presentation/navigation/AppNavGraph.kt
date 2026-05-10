package com.openlifting.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.openlifting.domain.usecase.auth.StartRoute
import com.openlifting.presentation.auth.LoginScreen
import com.openlifting.presentation.auth.RegisterScreen
import com.openlifting.presentation.auth.SplashScreen
import com.openlifting.presentation.onboarding.OnboardingEntry
import com.openlifting.presentation.onboarding.OnboardingHost

object Route {
    const val SPLASH              = "splash"
    const val LOGIN               = "login"
    const val REGISTER            = "register"
    const val ONBOARDING          = "onboarding"          // post-register full flow (profile + MVC)
    const val CALIBRATION_INITIAL = "calibration_initial" // existing profile, never calibrated
    const val RECALIBRATE         = "recalibrate"         // explicit recalibration from profile menu
    const val ATHLETE_ROOT        = "athlete"
    const val INSTRUCTOR_ROOT     = "instructor"
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Route.SPLASH) {

        composable(Route.SPLASH) {
            SplashScreen(
                onSessionFound = { route -> navController.goToStartRoute(route, popUpFrom = Route.SPLASH) },
                onNoSession    = {
                    navController.navigate(Route.LOGIN) {
                        popUpTo(Route.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Route.LOGIN) {
            LoginScreen(
                onLoginSuccess       = { route -> navController.goToStartRoute(route, popUpFrom = Route.LOGIN) },
                onNavigateToRegister = { navController.navigate(Route.REGISTER) }
            )
        }

        composable(Route.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = { route -> navController.goToStartRoute(route, popUpFrom = Route.LOGIN) },
                onNavigateBack    = { navController.popBackStack() }
            )
        }

        composable(Route.ONBOARDING) {
            OnboardingHost(
                entry       = OnboardingEntry.FullOnboarding,
                onCompleted = {
                    navController.navigate(Route.ATHLETE_ROOT) {
                        popUpTo(Route.ONBOARDING) { inclusive = true }
                    }
                },
                onAbort = {
                    navController.navigate(Route.ATHLETE_ROOT) {
                        popUpTo(Route.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(Route.CALIBRATION_INITIAL) {
            OnboardingHost(
                entry       = OnboardingEntry.Recalibration,
                onCompleted = {
                    navController.navigate(Route.ATHLETE_ROOT) {
                        popUpTo(Route.CALIBRATION_INITIAL) { inclusive = true }
                    }
                },
                onAbort = {
                    navController.navigate(Route.ATHLETE_ROOT) {
                        popUpTo(Route.CALIBRATION_INITIAL) { inclusive = true }
                    }
                }
            )
        }

        composable(Route.RECALIBRATE) {
            OnboardingHost(
                entry       = OnboardingEntry.Recalibration,
                onCompleted = { navController.popBackStack() },
                onAbort     = { navController.popBackStack() }
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
                },
                onStartRecalibration = { navController.navigate(Route.RECALIBRATE) },
                onStartProfileSetup  = { navController.navigate(Route.ONBOARDING) }
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

private fun NavController.goToStartRoute(route: StartRoute, popUpFrom: String) {
    val dest = when (route) {
        StartRoute.AthleteHome              -> Route.ATHLETE_ROOT
        StartRoute.InstructorHome           -> Route.INSTRUCTOR_ROOT
        StartRoute.AthleteOnboardingProfile -> Route.ONBOARDING
        StartRoute.AthleteCalibration       -> Route.CALIBRATION_INITIAL
        StartRoute.Login                    -> Route.LOGIN
    }
    navigate(dest) { popUpTo(popUpFrom) { inclusive = true } }
}
