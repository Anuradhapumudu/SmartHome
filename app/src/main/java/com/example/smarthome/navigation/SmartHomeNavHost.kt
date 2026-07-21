package com.example.smarthome.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.smarthome.ui.floorplan.FloorPlanGridScreen
import com.example.smarthome.ui.floorplan.FloorPlanListScreen
import com.example.smarthome.ui.reporting.ReportingScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartHomeNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Screens that show the bottom nav bar
    val showBottomBar = currentDestination?.route in listOf(
        Routes.FLOOR_PLAN_LIST,
        Routes.REPORTING
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = androidx.compose.ui.unit.Dp(0f)
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Dashboard, contentDescription = "Floors") },
                        label = { Text("Floors") },
                        selected = currentDestination?.hierarchy?.any {
                            it.route == Routes.FLOOR_PLAN_LIST
                        } == true,
                        onClick = {
                            navController.navigate(Routes.FLOOR_PLAN_LIST) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Analytics, contentDescription = "Reports") },
                        label = { Text("Reports") },
                        selected = currentDestination?.hierarchy?.any {
                            it.route == Routes.REPORTING
                        } == true,
                        onClick = {
                            navController.navigate(Routes.REPORTING) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.FLOOR_PLAN_LIST,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.FLOOR_PLAN_LIST) {
                FloorPlanListScreen(
                    onFloorPlanClick = { floorPlanId ->
                        navController.navigate(Routes.floorPlanGrid(floorPlanId))
                    }
                )
            }
            composable(
                route = Routes.FLOOR_PLAN_GRID,
                arguments = listOf(navArgument("floorPlanId") { type = NavType.StringType })
            ) { backStackEntry ->
                val floorPlanId = backStackEntry.arguments?.getString("floorPlanId") ?: return@composable
                FloorPlanGridScreen(
                    floorPlanId = floorPlanId,
                    onBack = { navController.navigateUp() }
                )
            }
            composable(Routes.REPORTING) {
                ReportingScreen()
            }
        }
    }
}
