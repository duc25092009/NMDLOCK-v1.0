package com.nmdlock.app.core.navigation

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 * Navigation routes - Sealed class cho type-safe navigation
 */
sealed class NavRoutes(val route: String) {
    object Dashboard : NavRoutes("dashboard")
    object Device : NavRoutes("device")
    object Key : NavRoutes("key")
    object Optimization : NavRoutes("optimization")
    object Network : NavRoutes("network")
    object GameProfile : NavRoutes("game_profile")
    object Settings : NavRoutes("settings")
    object Support : NavRoutes("support")
    
    companion object {
        val MAIN_ROUTES = listOf(
            Dashboard.route,
            Device.route,
            Key.route,
            Optimization.route,
            Network.route
        )
    }
}

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
 * 
 * FIX: 
 * - Thêm logging để debug navigation
 * - Đảm bảo NavHost luôn render với startDestination cố định
 * - Handle back navigation đúng cách
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NMDNavigation(
    navController: NavHostController = rememberNavController(),
    onLicenseExpired: () -> Unit = {}
) {
    Log.d("NMD_NAV", "NMDNavigation: Compose started")
    
    Scaffold(
        bottomBar = { NMDBottomBar(navController) },
        containerColor = DarkBackground,
        contentColor = DarkTextPrimary
    ) { innerPadding ->
        Log.d("NMD_NAV", "NMDNavigation: Rendering NavHost with padding=$innerPadding")
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBackground)
        ) {
            NavHost(
                navController = navController,
                startDestination = NavRoutes.Dashboard.route, // ← FIX: Route cố định, không dynamic
                modifier = Modifier.fillMaxSize(),
                enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
            ) {
                Log.d("NMD_NAV", "NMDNavigation: Registering routes")
                
                composable(NavRoutes.Dashboard.route) {
                    Log.d("NMD_NAV", "NMDNavigation: Rendering DashboardScreen")
                    DashboardScreen(
                        onNavigateToGame = { 
                            Log.d("NMD_NAV", "Navigate to GameProfile")
                            navController.navigate(NavRoutes.GameProfile.route) 
                        },
                        onNavigateToSettings = { 
                            Log.d("NMD_NAV", "Navigate to Settings")
                            navController.navigate(NavRoutes.Settings.route) 
                        },
                        onNavigateToSupport = { 
                            Log.d("NMD_NAV", "Navigate to Support")
                            navController.navigate(NavRoutes.Support.route) 
                        },
                    )
                }
                
                composable(NavRoutes.Device.route) {
                    Log.d("NMD_NAV", "NMDNavigation: Rendering DeviceScreen")
                    DeviceScreen()
                }
                
                composable(NavRoutes.Key.route) {
                    Log.d("NMD_NAV", "NMDNavigation: Rendering KeyScreen")
                    KeyScreen()
                }
                
                composable(NavRoutes.Optimization.route) {
                    Log.d("NMD_NAV", "NMDNavigation: Rendering OptimizationScreen")
                    OptimizationScreen()
                }
                
                composable(NavRoutes.Network.route) {
                    Log.d("NMD_NAV", "NMDNavigation: Rendering NetworkScreen")
                    NetworkScreen()
                }
                
                composable(NavRoutes.GameProfile.route) {
                    Log.d("NMD_NAV", "NMDNavigation: Rendering GameProfileScreen")
                    GameProfileScreen(
                        onBack = { 
                            Log.d("NMD_NAV", "GameProfile: Back pressed")
                            navController.popBackStack() 
                        }
                    )
                }
                
                composable(NavRoutes.Settings.route) {
                    Log.d("NMD_NAV", "NMDNavigation: Rendering SettingsScreen")
                    SettingsScreen(
                        onBack = { 
                            Log.d("NMD_NAV", "Settings: Back pressed")
                            navController.popBackStack() 
                        }
                    )
                }
                
                composable(NavRoutes.Support.route) {
                    Log.d("NMD_NAV", "NMDNavigation: Rendering SupportScreen")
                    SupportScreen(
                        onBack = { 
                            Log.d("NMD_NAV", "Support: Back pressed")
                            navController.popBackStack() 
                        }
                    )
                }
            }
        }
    }
}

/**
 * Bottom navigation bar with NMDLock styling.
 * 
 * FIX:
 * - Chỉ show bottom bar cho main routes
 * - Handle navigation với proper back stack management
 */
@Composable
fun NMDBottomBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    // Only show bottom bar for main screens (not for detail screens like GameProfile, Settings, Support)
    val showBottomBar = currentDestination?.route in NavRoutes.MAIN_ROUTES
    
    Log.d("NMD_NAV", "NMDBottomBar: currentRoute=${currentDestination?.route}, showBottomBar=$showBottomBar")
    
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
                            Log.d("NMD_NAV", "BottomBar: Clicked ${item.route}")
                            navController.navigate(item.route) {
                                // Pop up to the start destination of the graph to
                                // avoid building up a large stack of destinations
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination when
                                // reselecting the same item
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
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
