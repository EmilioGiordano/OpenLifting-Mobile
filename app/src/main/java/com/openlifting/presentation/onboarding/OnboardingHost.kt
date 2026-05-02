package com.openlifting.presentation.onboarding

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Self-contained host for the onboarding flow with its own NavController so the same
 * OnboardingViewModel scope is shared across all sub-screens (Welcome -> Profile -> MVC
 * Explain -> MVC Capture -> Done).
 *
 * Two entry modes:
 *  - FullOnboarding: the post-register flow that walks the user through everything.
 *  - Recalibration: skips Welcome + Profile, jumps straight to MVC Explain. Used when an
 *    existing athlete pulls the trigger from the home banner or profile screen.
 */

private object OnbRoute {
    const val WELCOME      = "onb/welcome"
    const val PROFILE      = "onb/profile"
    const val MVC_EXPLAIN  = "onb/mvc-explain"
    const val MVC_CAPTURE  = "onb/mvc-capture"
    const val DONE         = "onb/done"
}

enum class OnboardingEntry(val start: String) {
    FullOnboarding(OnbRoute.WELCOME),
    Recalibration(OnbRoute.MVC_EXPLAIN)
}

@Composable
fun OnboardingHost(
    entry: OnboardingEntry,
    onCompleted: () -> Unit,
    onAbort: () -> Unit
) {
    val nav = rememberNavController()
    val sharedVm: OnboardingViewModel = hiltViewModel()

    NavHost(navController = nav, startDestination = entry.start) {

        composable(OnbRoute.WELCOME) {
            OnboardingWelcomeScreen(
                onContinue = { nav.navigate(OnbRoute.PROFILE) },
                onSkip     = onAbort
            )
        }

        composable(OnbRoute.PROFILE) {
            OnboardingProfileScreen(
                onContinue = {
                    nav.navigate(OnbRoute.MVC_EXPLAIN) {
                        popUpTo(OnbRoute.PROFILE) { inclusive = true }
                    }
                },
                onBack    = { nav.popBackStack() },
                viewModel = sharedVm
            )
        }

        composable(OnbRoute.MVC_EXPLAIN) {
            OnboardingMvcExplainScreen(
                onStart   = { nav.navigate(OnbRoute.MVC_CAPTURE) },
                onSkip    = onAbort,
                viewModel = sharedVm
            )
        }

        composable(OnbRoute.MVC_CAPTURE) {
            OnboardingMvcCaptureScreen(
                onComplete = {
                    nav.navigate(OnbRoute.DONE) {
                        popUpTo(OnbRoute.MVC_CAPTURE) { inclusive = true }
                    }
                },
                onCancel  = onAbort,
                viewModel = sharedVm
            )
        }

        composable(OnbRoute.DONE) {
            OnboardingDoneScreen(
                onContinue = onCompleted,
                viewModel  = sharedVm
            )
        }
    }
}
