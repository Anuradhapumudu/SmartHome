package com.example.smarthome.navigation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.smarthome.ui.auth.AuthViewModel
import com.example.smarthome.ui.auth.LoginScreen
import com.example.smarthome.ui.auth.SignupScreen
import com.example.smarthome.ui.floorplan.FloorPlanGridScreen
import com.example.smarthome.ui.floorplan.FloorPlanListScreen
import com.example.smarthome.ui.reporting.ReportingScreen
import com.example.smarthome.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartHomeNavHost(
    authViewModel: AuthViewModel = viewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val authUser by authViewModel.user.collectAsStateWithLifecycle()

    val systemDark = isSystemInDarkTheme()
    var isDarkMode by remember { mutableStateOf(systemDark) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val showMainNavigation = currentDestination?.route in listOf(
        Routes.FLOOR_PLAN_LIST,
        Routes.REPORTING
    )

    data class NavItem(
        val route: String,
        val label: String,
        val icon: androidx.compose.ui.graphics.vector.ImageVector
    )

    val navItems = listOf(
        NavItem(Routes.FLOOR_PLAN_LIST, "Floors", Icons.Outlined.Layers),
        NavItem(Routes.REPORTING, "Analytics", Icons.Outlined.AutoGraph)
    )

    SmartHomeTheme(darkTheme = isDarkMode) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = showMainNavigation,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = MaterialTheme.colorScheme.surface
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = authUser?.email ?: "Guest Identity",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Smart Home Operator",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Night Mode",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { isDarkMode = it }
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    NavigationDrawerItem(
                        label = { Text("Sign Out") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            authViewModel.logout()
                        },
                        icon = { Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedTextColor = MaterialTheme.colorScheme.error,
                            unselectedIconColor = MaterialTheme.colorScheme.error
                        )
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        ) {
            Scaffold(
                bottomBar = {
                    if (showMainNavigation) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 0.dp,
                        ) {
                            navItems.forEach { item ->
                                val isSelected = currentDestination?.hierarchy?.any {
                                    it.route == item.route
                                } == true

                                NavigationBarItem(
                                    icon = {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.label
                                        )
                                    },
                                    label = { Text(item.label) },
                                    selected = isSelected,
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
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
                containerColor = MaterialTheme.colorScheme.background
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = if (authUser == null) Routes.LOGIN else Routes.FLOOR_PLAN_LIST,
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable(Routes.LOGIN) {
                        LoginScreen(
                            onNavigateToSignup = { navController.navigate(Routes.SIGNUP) },
                            onLoginSuccess = {
                                navController.navigate(Routes.FLOOR_PLAN_LIST) {
                                    popUpTo(Routes.LOGIN) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(Routes.SIGNUP) {
                        SignupScreen(
                            onNavigateToLogin = { navController.navigate(Routes.LOGIN) },
                            onSignupSuccess = {
                                navController.navigate(Routes.FLOOR_PLAN_LIST) {
                                    popUpTo(Routes.LOGIN) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(Routes.FLOOR_PLAN_LIST) {
                        FloorPlanListScreen(
                            onFloorPlanClick = { floorPlanId ->
                                navController.navigate(Routes.floorPlanGrid(floorPlanId))
                            },
                            onOpenDrawer = { scope.launch { drawerState.open() } }
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
                        ReportingScreen(
                            onOpenDrawer = { scope.launch { drawerState.open() } }
                        )
                    }
                }
            }
        }
    }
}
