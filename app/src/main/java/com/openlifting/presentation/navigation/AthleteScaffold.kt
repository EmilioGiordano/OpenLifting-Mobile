package com.openlifting.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.openlifting.presentation.athlete.history.HistoryScreen
import com.openlifting.presentation.athlete.history.SessionDetailScreen
import com.openlifting.presentation.athlete.home.AthleteHomeScreen
import com.openlifting.presentation.athlete.profile.AthleteProfileScreen
import com.openlifting.presentation.athlete.profile.EditAthleteProfileScreen
import com.openlifting.presentation.athlete.session.SessionScreen

sealed class AthleteTab(val route: String, val label: String, val icon: ImageVector) {
    object Home       : AthleteTab("athlete/home",    "Inicio",        Icons.Filled.Home)
    object NewSession : AthleteTab("athlete/session",  "Nueva",         Icons.Filled.Add)
    object History    : AthleteTab("athlete/history",  "Historial",     Icons.Filled.List)
    object Profile    : AthleteTab("athlete/profile",  "Perfil",        Icons.Filled.Person)
}

private val athleteTabs = listOf(
    AthleteTab.Home,
    AthleteTab.NewSession,
    AthleteTab.History,
    AthleteTab.Profile
)

@Composable
fun AthleteScaffold(
    onLogout: () -> Unit,
    onSwitchToInstructor: () -> Unit,
    onStartRecalibration: () -> Unit = {},
    onStartProfileSetup: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route

    // Bottom nav hidden during session and detail screens
    val tabRoutes = athleteTabs.map { it.route }
    val showBottomBar = currentRoute in tabRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                AthleteBottomBar(
                    currentRoute = currentRoute,
                    onTabSelected = { tab ->
                        when (tab) {
                            is AthleteTab.NewSession -> navController.navigate(AthleteTab.NewSession.route)
                            is AthleteTab.Home -> navController.popBackStack(AthleteTab.Home.route, inclusive = false)
                            else -> navController.navigate(tab.route) {
                                popUpTo(AthleteTab.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        AthleteNavHost(
            navController = navController,
            innerPadding = innerPadding,
            onLogout = onLogout,
            onSwitchToInstructor = onSwitchToInstructor,
            onStartRecalibration = onStartRecalibration,
            onStartProfileSetup = onStartProfileSetup
        )
    }
}

@Composable
private fun AthleteBottomBar(
    currentRoute: String?,
    onTabSelected: (AthleteTab) -> Unit
) {
    NavigationBar {
        athleteTabs.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = { onTabSelected(tab) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) }
            )
        }
    }
}

@Composable
private fun AthleteNavHost(
    navController: NavHostController,
    innerPadding: androidx.compose.foundation.layout.PaddingValues,
    onLogout: () -> Unit,
    onSwitchToInstructor: () -> Unit,
    onStartRecalibration: () -> Unit,
    onStartProfileSetup: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = AthleteTab.Home.route,
        modifier = Modifier.padding(innerPadding)
    ) {
        composable(AthleteTab.Home.route) {
            AthleteHomeScreen(
                onNewSession  = { navController.navigate(AthleteTab.NewSession.route) },
                onCalibrate   = onStartRecalibration
            )
        }

        composable(AthleteTab.NewSession.route) {
            SessionScreen(
                onFinish = {
                    navController.navigate(AthleteTab.Home.route) {
                        popUpTo(AthleteTab.NewSession.route) { inclusive = true }
                    }
                }
            )
        }

        composable(AthleteTab.History.route) {
            HistoryScreen(
                onSessionClick = { sessionId ->
                    navController.navigate("athlete/history/$sessionId")
                }
            )
        }

        composable("athlete/history/{sessionId}") { backStack ->
            val sessionId = backStack.arguments?.getString("sessionId")?.toLongOrNull() ?: return@composable
            SessionDetailScreen(
                sessionId = sessionId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(AthleteTab.Profile.route) {
            AthleteProfileScreen(
                onLogout             = onLogout,
                onSwitchToInstructor = onSwitchToInstructor,
                onRecalibrate        = onStartRecalibration,
                onEditProfile        = { navController.navigate("athlete/profile/edit") },
                onSetupProfile       = onStartProfileSetup
            )
        }

        composable("athlete/profile/edit") {
            EditAthleteProfileScreen(
                onSaved = { navController.popBackStack() },
                onBack  = { navController.popBackStack() }
            )
        }
    }
}
