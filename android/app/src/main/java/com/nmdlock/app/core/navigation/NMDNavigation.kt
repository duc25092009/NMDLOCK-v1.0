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

data class BottomNavItem(val route: String, val label: String, val icon: ImageVector)

val bottomNavItems = listOf(
    BottomNavItem(NavRoutes.Dashboard.route, "Tổng quan", Icons.Default.Dashboard),
    BottomNavItem(NavRoutes.Device.route, "Thiết bị", Icons.Default.PhoneAndroid),
    BottomNavItem(NavRoutes.Key.route, "Key", Icons.Default.VpnKey),
    BottomNavItem(NavRoutes.Optimization.route, "Tối ưu", Icons.Default.Speed),
    BottomNavItem(NavRoutes.Network.route, "Mạng", Icons.Default.Wifi),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NMDNavigation(
    navController: NavHostController = rememberNavController(),
    onLicenseExpired: () -> Unit = {}
) {
    Log.d("NMD_NAV", "NMDNavigation: Compose started")
    
    Scaffold(
        bottomBar = { NMDBottomBar(navController) },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { innerPadding ->
        Log.d("NMD_NAV", "NMDNavigation: Rendering NavHost with padding=$innerPadding")
        
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding).background(MaterialTheme.colorScheme.background)) {
            NavHost(
                navController = navController,
                startDestination = NavRoutes.Dashboard.route,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
            ) {
                Log.d("NMD_NAV", "NMDNavigation: Registering routes")
                
                composable(NavRoutes.Dashboard.route) {
                    Log.d("NMD_NAV", "NMDNavigation: Rendering DashboardScreen")
                    try {
                        DashboardScreen(
                            onNavigateToGame = { navController.navigate(NavRoutes.GameProfile.route) },
                            onNavigateToSettings = { navController.navigate(NavRoutes.Settings.route) },
                            onNavigateToSupport = { navController.navigate(NavRoutes.Support.route) },
                        )
                    } catch (e: Exception) {
                        Log.e("NMD_NAV", "DashboardScreen crashed", e)
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Lỗi tải Dashboard: ${e.message}", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                composable(NavRoutes.Device.route) {
                    try {
                        DeviceScreen()
                    } catch (e: Exception) {
                        Log.e("NMD_NAV", "DeviceScreen crashed", e)
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Lỗi tải Device: ${e.message}", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                composable(NavRoutes.Key.route) {
                    try {
                        KeyScreen()
                    } catch (e: Exception) {
                        Log.e("NMD_NAV", "KeyScreen crashed", e)
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Lỗi tải Key: ${e.message}", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                composable(NavRoutes.Optimization.route) {
                    try {
                        OptimizationScreen()
                    } catch (e: Exception) {
                        Log.e("NMD_NAV", "OptimizationScreen crashed", e)
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Lỗi tải Optimization: ${e.message}", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                composable(NavRoutes.Network.route) {
                    try {
                        NetworkScreen()
                    } catch (e: Exception) {
                        Log.e("NMD_NAV", "NetworkScreen crashed", e)
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Lỗi tải Network: ${e.message}", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                composable(NavRoutes.GameProfile.route) {
                    try {
                        GameProfileScreen(onBack = { navController.popBackStack() })
                    } catch (e: Exception) {
                        Log.e("NMD_NAV", "GameProfileScreen crashed", e)
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Lỗi tải GameProfile: ${e.message}", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                composable(NavRoutes.Settings.route) {
                    try {
                        SettingsScreen(onBack = { navController.popBackStack() })
                    } catch (e: Exception) {
                        Log.e("NMD_NAV", "SettingsScreen crashed", e)
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Lỗi tải Settings: ${e.message}", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                composable(NavRoutes.Support.route) {
                    try {
                        SupportScreen(onBack = { navController.popBackStack() })
                    } catch (e: Exception) {
                        Log.e("NMD_NAV", "SupportScreen crashed", e)
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Lỗi tải Support: ${e.message}", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NMDBottomBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = currentDestination?.route in NavRoutes.MAIN_ROUTES
    
    Log.d("NMD_NAV", "NMDBottomBar: currentRoute=${currentDestination?.route}, showBottomBar=$showBottomBar")
    
    if (showBottomBar) {
        Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f), tonalElevation = 0.dp, shadowElevation = 8.dp) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                bottomNavItems.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            Log.d("NMD_NAV", "BottomBar: Clicked ${item.route}")
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(imageVector = item.icon, contentDescription = item.label, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
                        label = { Text(text = item.label, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    )
                }
            }
        }
    }
}
