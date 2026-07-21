package com.example.smarthome.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import com.example.smarthome.ui.theme.*

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

    // Nav items definition
    data class NavItem(
        val route: String,
        val label: String,
        val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
        val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
    )

    val navItems = listOf(
        NavItem(Routes.FLOOR_PLAN_LIST, "Floors", Icons.Filled.Layers, Icons.Outlined.Layers),
        NavItem(Routes.REPORTING, "Reports", Icons.Filled.BarChart, Icons.Outlined.BarChart)
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = SurfaceDark,
                    tonalElevation = 0.dp,
                ) {
                    navItems.forEach { item ->
                        val isSelected = currentDestination?.hierarchy?.any {
                            it.route == item.route
                        } == true

                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) },
                            selected = isSelected,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = TealPrimary,
                                selectedTextColor = TealPrimary,
                                indicatorColor = TealContainer,
                                unselectedIconColor = OnSurfaceVariant,
                                unselectedTextColor = OnSurfaceVariant
                            ),
                            onClick = {
                                navController.navigate(item.route) {
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
        },
        containerColor = BackgroundDark
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
            ) {
                FloorPlanGridScreen(
                    floorPlanId = it.arguments?.getString("floorPlanId") ?: "",
                    onBack = { navController.navigateUp() }
                )
            }
            composable(Routes.REPORTING) {
                ReportingScreen()
            }
        }
    }
}
