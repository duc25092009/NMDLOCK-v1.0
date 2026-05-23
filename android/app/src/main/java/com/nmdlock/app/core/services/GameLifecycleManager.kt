package com.nmdlock.app.core.services

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * ═══════════════════════════════════════════════════════════════════════
 * PILLAR 5: ZERO-LATENCY GAME LIFECYCLE MANAGER
 * ═══════════════════════════════════════════════════════════════════════
 *
 * Phát hiện game start/stop tự động
 * Snapshot settings trước khi apply game profile
 * Tự động restore khi thoát game
 */
@Singleton
class GameLifecycleManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shizukuManager: ShizukuManager,
    private val commandQueue: ShizukuCommandQueue,
    private val profileEngine: GameProfileEngine,
    private val thermalEngine: PredictiveThermalEngine
) {
    private val _currentGame = MutableStateFlow<String?>(null)
    val currentGame: StateFlow<String?> = _currentGame.asStateFlow()

    val targetGames = setOf(
        "com.dts.freefireth",
        "com.dts.freefiremax",
        "com.tencent.ig",       // PUBG Mobile
        "com.pubg.krmobile",     // PUBG Korean
        "com.tencent.tmgp.sgame", // Arena of Valor
        "com.garena.game.codm",  // Call of Duty Mobile
        "com.mobile.legends"     // Mobile Legends
    )

    private var lifecycleScope: CoroutineScope? = null

    // Snapshot settings trước khi apply
    private var settingsSnapshot: SettingsSnapshot? = null
    private val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    data class SettingsSnapshot(
        val windowAnimationScale: Float = 1f,
        val transitionAnimationScale: Float = 1f,
        val animatorDurationScale: Float = 1f,
        val screenWidth: Int = 1080,
        val screenHeight: Int = 2400,
        val screenDensity: Int = 480,
        val refreshRate: Int = 60,
        val brightness: Int = 128,
        val privateDnsMode: String = "",
        val headsUpEnabled: Boolean = true
    )

    /**
     * Khởi động zero-latency detection loop
     * Poll mỗi 500ms để phát hiện game switch
     */
    fun start(scope: CoroutineScope) {
        lifecycleScope = scope
        scope.launch(Dispatchers.IO + SupervisorJob()) {
            var lastForeground = ""

            while (isActive) {
                val foreground = getForegroundPackage()

                if (foreground != lastForeground) {
                    lastForeground = foreground
                    handleTransition(foreground)
                }

                delay(500)
            }
        }
    }

    private suspend fun handleTransition(pkg: String) {
        val wasInGame = _currentGame.value != null
        val isNowInGame = pkg in targetGames

        when {
            // GAME LAUNCHED
            isNowInGame && !wasInGame -> {
                _currentGame.value = pkg
                settingsSnapshot = takeSettingsSnapshot()
                applyProGamingProfile(pkg)

                // Bật thermal monitoring + HUD
                thermalEngine.startMonitoring(lifecycleScope ?: return@launch)
                startHudOverlays(pkg)
            }

            // GAME EXITED
            !isNowInGame && wasInGame -> {
                val previousGame = _currentGame.value
                _currentGame.value = null

                thermalEngine.stopMonitoring()
                stopHudOverlays()

                // Delay restore để không conflict với game closing
                delay(2000)

                // Restore ONLY khi ở home screen
                if (isLauncherForeground()) {
                    restoreOriginalSettings()
                } else {
                    // Wait until launcher is foreground
                    waitForLauncherAndRestore()
                }
            }
        }
    }

    private suspend fun takeSettingsSnapshot(): SettingsSnapshot {
        return try {
            val cmds = listOf(
                "settings get global window_animation_scale",
                "settings get global transition_animation_scale",
                "settings get global animator_duration_scale",
                "settings get global peak_refresh_rate",
                "settings get system screen_brightness",
                "settings get global private_dns_mode",
                "settings get global heads_up_notifications_enabled"
            )

            val results = cmds.map { cmd ->
                shizukuManager.executeShellCommand(cmd).getOrElse { "" }
            }

            SettingsSnapshot(
                windowAnimationScale = results.getOrNull(0)?.trim()?.toFloatOrNull() ?: 1f,
                transitionAnimationScale = results.getOrNull(1)?.trim()?.toFloatOrNull() ?: 1f,
                animatorDurationScale = results.getOrNull(2)?.trim()?.toFloatOrNull() ?: 1f,
                refreshRate = results.getOrNull(3)?.trim()?.toFloatOrNull()?.toInt() ?: 60,
                brightness = results.getOrNull(4)?.trim()?.toIntOrNull() ?: 128,
                privateDnsMode = results.getOrNull(5)?.trim() ?: "",
                headsUpEnabled = results.getOrNull(6)?.trim() != "0"
            )
        } catch (e: Exception) {
            SettingsSnapshot()
        }
    }

    private suspend fun applyProGamingProfile(pkg: String) {
        val profile = when {
            pkg == "com.dts.freefireth" -> GameProfileEngine.GameProfile(
                name = "FreeFire Pro",
                animationScale = 0f,
                forceGpu = true,
                killApps = true,
                resolution = "1280x720",
                refreshRate = 90,
                brightness = 200,
                msaa = false,
                dns = "cloudflare",
                wifiLock = true,
                dnd = true
            )
            pkg == "com.dts.freefiremax" -> GameProfileEngine.GameProfile(
                name = "FF Max Pro",
                animationScale = 0f,
                forceGpu = true,
                killApps = true,
                resolution = "1600x900",
                refreshRate = 120,
                brightness = 200,
                msaa = true,
                dns = "cloudflare",
                wifiLock = true,
                dnd = true
            )
            else -> GameProfileEngine.GameProfile(
                name = "Auto Profile",
                animationScale = 0.5f,
                forceGpu = true,
                killApps = true,
                resolution = "reset",
                refreshRate = 90,
                brightness = 180,
                msaa = false,
                wifiLock = true,
                dnd = true
            )
        }

        profileEngine.applyProfile(profile)
    }

    private suspend fun restoreOriginalSettings() {
        val snapshot = settingsSnapshot ?: return

        val restoreCmds = listOf(
            "wm size reset",
            "wm density reset",
            "settings put global window_animation_scale ${snapshot.windowAnimationScale}",
            "settings put global transition_animation_scale ${snapshot.transitionAnimationScale}",
            "settings put global animator_duration_scale ${snapshot.animatorDurationScale}",
            "settings put global peak_refresh_rate ${snapshot.refreshRate}",
            "settings put system screen_brightness ${snapshot.brightness}",
            "settings put global heads_up_notifications_enabled ${if (snapshot.headsUpEnabled) 1 else 0}",
            "settings put global private_dns_mode ${snapshot.privateDnsMode}",
            "settings put global wifi_sleep_policy 0"
        )

        restoreCmds.forEach { cmd ->
            commandQueue.submit(cmd, ShizukuCommandQueue.Priority.HIGH)
        }
    }

    private fun getForegroundPackage(): String {
        return try {
            val now = System.currentTimeMillis()
            val stats = usm.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                now - 10000,
                now
            )
            stats?.maxByOrNull { it.lastTimeUsed }?.packageName ?: ""
        } catch (e: Exception) { "" }
    }

    private fun isLauncherForeground(): Boolean {
        return try {
            val pkg = getForegroundPackage()
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val launchers = context.packageManager.queryIntentActivities(intent, 0)
            launchers.any { it.activityInfo.packageName == pkg }
        } catch (e: Exception) { false }
    }

    private suspend fun waitForLauncherAndRestore() {
        var attempts = 0
        while (attempts < 60 && !isLauncherForeground()) {
            delay(1000)
            attempts++
        }
        if (isLauncherForeground()) restoreOriginalSettings()
    }

    private fun startHudOverlays(pkg: String) {
        // Intent để start FPS overlay service
        try {
            val intent = Intent(context, FpsOverlayService::class.java)
            context.startForegroundService(intent)
        } catch (e: Exception) { /* Fallback */ }
    }

    private fun stopHudOverlays() {
        try {
            val intent = Intent(context, FpsOverlayService::class.java)
            context.stopService(intent)
        } catch (e: Exception) { /* Fallback */ }
    }
}
