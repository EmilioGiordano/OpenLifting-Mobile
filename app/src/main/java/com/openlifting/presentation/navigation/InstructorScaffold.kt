package com.openlifting.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.openlifting.presentation.athlete.session.SessionRouteArgs
import com.openlifting.presentation.athlete.session.SessionScreen
import com.openlifting.presentation.instructor.athlete.InstructorAthleteDetailScreen
import com.openlifting.presentation.instructor.guest.CreateGuestScreen
import com.openlifting.presentation.instructor.home.InstructorHomeScreen
import com.openlifting.presentation.instructor.profile.InstructorProfileScreen
import com.openlifting.presentation.onboarding.OnboardingEntry
import com.openlifting.presentation.onboarding.OnboardingHost

sealed class InstructorTab(val route: String, val label: String, val icon: ImageVector) {
    object Athletes : InstructorTab("instructor/athletes", "Atletas", Icons.Filled.Group)
    object Profile  : InstructorTab("instructor/profile",  "Perfil",  Icons.Filled.Person)
}

private object InstructorRoute {
    const val GUEST_NEW = "instructor/guest/new"

    /** profileId param — calibrates the guest with that AthleteProfile id. */
    const val GUEST_CALIBRATE = "instructor/guest/{profileId}/calibrate"
    fun guestCalibrate(profileId: Long) = "instructor/guest/$profileId/calibrate"

    /** profileId param — read-only detail view of one of my athletes. */
    const val ATHLETE_DETAIL = "instructor/athlete/{profileId}"
    fun athleteDetail(profileId: Long) = "instructor/athlete/$profileId"

    /** profileId param — recalibrate this athlete's MVCs (instructor-driven). */
    const val ATHLETE_RECALIBRATE = "instructor/athlete/{profileId}/recalibrate"
    fun athleteRecalibrate(profileId: Long) = "instructor/athlete/$profileId/recalibrate"

    /** Two args — start a session for [athleteUserId] supervised by [instructorUserId]. */
    const val ATHLETE_SESSION = "instructor/session/{athleteUserId}/{instructorUserId}"
    fun athleteSession(athleteUserId: Long, instructorUserId: Long) =
        "instructor/session/$athleteUserId/$instructorUserId"
}

private val instructorTabs = listOf(InstructorTab.Athletes, InstructorTab.Profile)

@Composable
fun InstructorScaffold(
    onLogout: () -> Unit,
    onSwitchToAthlete: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route

    val tabRoutes = instructorTabs.map { it.route }
    val showBottomBar = currentRoute in tabRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    instructorTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(InstructorTab.Athletes.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        InstructorNavHost(
            navController = navController,
            innerPadding  = innerPadding,
            onLogout      = onLogout,
            onSwitchToAthlete = onSwitchToAthlete
        )
    }
}

@Composable
private fun InstructorNavHost(
    navController: NavHostController,
    innerPadding: androidx.compose.foundation.layout.PaddingValues,
    onLogout: () -> Unit,
    onSwitchToAthlete: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = InstructorTab.Athletes.route,
        modifier = Modifier.padding(innerPadding)
    ) {
        composable(InstructorTab.Athletes.route) {
            InstructorHomeScreen(
                onCreateGuest  = { navController.navigate(InstructorRoute.GUEST_NEW) },
                onAthleteClick = { profileId ->
                    navController.navigate(InstructorRoute.athleteDetail(profileId))
                }
            )
        }

        composable(InstructorRoute.GUEST_NEW) {
            CreateGuestScreen(
                onCreated = { profileId ->
                    // Created → continue straight into calibration of this guest
                    navController.navigate(InstructorRoute.guestCalibrate(profileId)) {
                        popUpTo(InstructorRoute.GUEST_NEW) { inclusive = true }
                    }
                },
                onCancel = { navController.popBackStack() }
            )
        }

        composable(
            route     = InstructorRoute.GUEST_CALIBRATE,
            arguments = listOf(navArgument("profileId") { type = NavType.LongType })
        ) { backStack ->
            val profileId = backStack.arguments?.getLong("profileId") ?: return@composable
            OnboardingHost(
                entry       = OnboardingEntry.GuestCalibration(profileId),
                onCompleted = {
                    navController.navigate(InstructorTab.Athletes.route) {
                        popUpTo(InstructorTab.Athletes.route) { inclusive = true }
                    }
                },
                onAbort = {
                    navController.navigate(InstructorTab.Athletes.route) {
                        popUpTo(InstructorTab.Athletes.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route     = InstructorRoute.ATHLETE_DETAIL,
            arguments = listOf(navArgument("profileId") { type = NavType.LongType })
        ) { backStack ->
            val profileId = backStack.arguments?.getLong("profileId") ?: return@composable
            InstructorAthleteDetailScreen(
                profileId      = profileId,
                onBack         = { navController.popBackStack() },
                onStartSession = { athleteUserId, instructorUserId ->
                    navController.navigate(
                        InstructorRoute.athleteSession(athleteUserId, instructorUserId)
                    )
                },
                onRecalibrate  = { pid ->
                    navController.navigate(InstructorRoute.athleteRecalibrate(pid))
                },
                onSessionClick = { _ ->
                    // TODO: navigate to a read-only Session Detail for instructors. For now
                    // the row tap is a no-op so it doesn't dead-end.
                }
            )
        }

        composable(
            route     = InstructorRoute.ATHLETE_RECALIBRATE,
            arguments = listOf(navArgument("profileId") { type = NavType.LongType })
        ) { backStack ->
            val profileId = backStack.arguments?.getLong("profileId") ?: return@composable
            OnboardingHost(
                entry       = OnboardingEntry.GuestCalibration(profileId),
                onCompleted = { navController.popBackStack() },
                onAbort     = { navController.popBackStack() }
            )
        }

        composable(
            route     = InstructorRoute.ATHLETE_SESSION,
            arguments = listOf(
                navArgument(SessionRouteArgs.ATHLETE_USER_ID)    { type = NavType.LongType },
                navArgument(SessionRouteArgs.INSTRUCTOR_USER_ID) { type = NavType.LongType }
            )
        ) {
            // SessionViewModel reads ATHLETE_USER_ID + INSTRUCTOR_USER_ID from
            // SavedStateHandle automatically — args populated by Compose Navigation.
            SessionScreen(
                onFinish = { navController.popBackStack() }
            )
        }

        composable(InstructorTab.Profile.route) {
            InstructorProfileScreen(
                onLogout          = onLogout,
                onSwitchToAthlete = onSwitchToAthlete
            )
        }
    }
}
