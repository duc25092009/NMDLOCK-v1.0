package com.nmdlock.app.core.navigation

/**
 * Navigation route definitions for the app.
 */
sealed class NavRoutes(val route: String) {
    object Splash : NavRoutes("splash")
    object Onboarding : NavRoutes("onboarding")
    object Main : NavRoutes("main")
    object Dashboard : NavRoutes("dashboard")
    object Device : NavRoutes("device")
    object Key : NavRoutes("key")
    object Optimization : NavRoutes("optimization")
    object Network : NavRoutes("network")
    object GameProfile : NavRoutes("game_profile")
    object Settings : NavRoutes("settings")
    object Support : NavRoutes("support")
    object Logs : NavRoutes("logs")
    object LicenseGate : NavRoutes("license_gate")
}
