package com.nmdlock.app.core.navigation

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
            Dashboard.route, Device.route, Key.route, Optimization.route, Network.route
        )
    }
}
