package com.nmdlock.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nmdlock.app.feature.dashboard.DashboardScreen
import com.nmdlock.app.feature.key.KeyScreen
import com.nmdlock.app.feature.license.LicenseGateScreen

@Composable
fun NMDNavigation() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "dashboard"
    ) {

        composable("dashboard") {
            DashboardScreen()
        }

        composable("key") {
            KeyScreen()
        }

        composable("license") {
            LicenseGateScreen()
        }
    }
}
