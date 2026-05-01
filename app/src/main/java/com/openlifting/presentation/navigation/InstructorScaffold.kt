package com.openlifting.presentation.navigation

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.openlifting.presentation.instructor.home.InstructorHomeScreen
import com.openlifting.presentation.instructor.profile.InstructorProfileScreen

sealed class InstructorTab(val route: String, val label: String, val icon: ImageVector) {
    object Athletes : InstructorTab("instructor/athletes", "Atletas", Icons.Filled.Group)
    object Profile  : InstructorTab("instructor/profile",  "Perfil",  Icons.Filled.Person)
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

    Scaffold(
        bottomBar = {
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
    ) { innerPadding ->
        InstructorNavHost(
            navController = navController,
            innerPadding = innerPadding,
            onLogout = onLogout,
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
            InstructorHomeScreen()
        }

        composable(InstructorTab.Profile.route) {
            InstructorProfileScreen(
                onLogout = onLogout,
                onSwitchToAthlete = onSwitchToAthlete
            )
        }
    }
}
