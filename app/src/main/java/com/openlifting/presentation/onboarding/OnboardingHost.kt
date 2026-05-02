package com.openlifting.presentation.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/**
 * Self-contained host for the onboarding / calibration flow with its own NavController so the
 * same [OnboardingViewModel] scope is shared across all sub-screens (Welcome -> Profile ->
 * MVC Explain -> MVC Capture -> Done).
 *
 * Three entry modes:
 *  - [OnboardingEntry.FullOnboarding]: post-register flow that walks the user through
 *    everything (athlete onboarding).
 *  - [OnboardingEntry.Recalibration]: existing athlete updates their own MVCs. Skips Welcome
 *    and Profile, jumps straight to MVC Explain.
 *  - [OnboardingEntry.GuestCalibration]: instructor calibrates a guest profile they just
 *    created. Same path as Recalibration but writes to a specified [profileId] instead of
 *    the logged-in user's profile.
 */

private object OnbRoute {
    const val WELCOME      = "onb/welcome"
    const val PROFILE      = "onb/profile"
    const val MVC_EXPLAIN  = "onb/mvc-explain"
    const val MVC_CAPTURE  = "onb/mvc-capture"
    const val DONE         = "onb/done"
}

sealed interface OnboardingEntry {
    val start: String

    data object FullOnboarding : OnboardingEntry {
        override val start = OnbRoute.WELCOME
    }
    data object Recalibration : OnboardingEntry {
        override val start = OnbRoute.MVC_EXPLAIN
    }
    /** Instructor-driven calibration of a guest profile. */
    data class GuestCalibration(val profileId: Long) : OnboardingEntry {
        override val start = OnbRoute.MVC_EXPLAIN
    }
}

@Composable
fun OnboardingHost(
    entry: OnboardingEntry,
    onCompleted: () -> Unit,
    onAbort: () -> Unit
) {
    val nav = rememberNavController()
    val sharedVm: OnboardingViewModel = hiltViewModel()

    // Wire the target profile for guest calibration mode before the screens read state.
    LaunchedEffect(entry) {
        if (entry is OnboardingEntry.GuestCalibration) {
            sharedVm.setTargetProfile(entry.profileId)
        }
    }

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
