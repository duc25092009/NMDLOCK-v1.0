package com.nmdlock.app.core.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nmdlock.app.core.ui.theme.*
import com.nmdlock.app.feature.dashboard.DashboardScreen
import com.nmdlock.app.feature.device.DeviceScreen
import com.nmdlock.app.feature.key.KeyScreen
import com.nmdlock.app.feature.optimization.OptimizationScreen
import com.nmdlock.app.feature.network.NetworkScreen
import com.nmdlock.app.feature.game.GameProfileScreen
import com.nmdlock.app.feature.settings.SettingsScreen
import com.nmdlock.app.feature.support.SupportScreen

/**
 * Bottom navigation items.
 */
data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

val bottomNavItems = listOf(
    BottomNavItem(NavRoutes.Dashboard.route, "Tổng quan", Icons.Default.Dashboard),
    BottomNavItem(NavRoutes.Device.route, "Thiết bị", Icons.Default.PhoneAndroid),
    BottomNavItem(NavRoutes.Key.route, "Key", Icons.Default.VpnKey),
    BottomNavItem(NavRoutes.Optimization.route, "Tối ưu", Icons.Default.Speed),
    BottomNavItem(NavRoutes.Network.route, "Mạng", Icons.Default.Wifi),
)

/**
 * Main navigation host with bottom navigation bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NMDNavigation() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { NMDBottomBar(navController) }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = NavRoutes.Dashboard.route,
            ) {
                composable(NavRoutes.Dashboard.route) {
                    DashboardScreen(
                        onNavigateToGame = { navController.navigate(NavRoutes.GameProfile.route) },
                        onNavigateToSettings = { navController.navigate(NavRoutes.Settings.route) },
                        onNavigateToSupport = { navController.navigate(NavRoutes.Support.route) },
                    )
                }
                composable(NavRoutes.Device.route) {
                    DeviceScreen()
                }
                composable(NavRoutes.Key.route) {
                    KeyScreen()
                }
                composable(NavRoutes.Optimization.route) {
                    OptimizationScreen()
                }
                composable(NavRoutes.Network.route) {
                    NetworkScreen()
                }
                composable(NavRoutes.GameProfile.route) {
                    GameProfileScreen(onBack = { navController.popBackStack() })
                }
                composable(NavRoutes.Settings.route) {
                    SettingsScreen(onBack = { navController.popBackStack() })
                }
                composable(NavRoutes.Support.route) {
                    SupportScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}

/**
 * Bottom navigation bar with NMDLock styling.
 */
@Composable
fun NMDBottomBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Only show bottom bar for main screens
    val showBottomBar = bottomNavItems.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.route } == true
    }

    if (showBottomBar) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = DarkSurface,
            tonalElevation = 0.dp,
            shadowElevation = 8.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                bottomNavItems.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = if (selected) Purple400 else DarkTextSecondary,
                            )
                        },
                        label = {
                            Text(
                                text = item.label,
                                color = if (selected) Purple400 else DarkTextSecondary,
                                fontSize = 10.sp,
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Purple600.copy(alpha = 0.15f),
                        ),
                    )
                }
            }
        }
    }
}
