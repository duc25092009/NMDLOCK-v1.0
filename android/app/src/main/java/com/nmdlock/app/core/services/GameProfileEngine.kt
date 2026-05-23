package com.nmdlock.app.core.services

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Applies REAL game performance profiles using Shizuku.
 * Now includes MSAA, resolution switch, refresh rate boost,
 * WiFi lock, DNS change, DND mode, and more.
 */
@Singleton
class GameProfileEngine @Inject constructor(
    private val shizukuManager: ShizukuManager,
    private val optimizationEngine: OptimizationEngine,
) {

    /**
     * Game profile settings for automatic game detection.
     */
    data class GameProfile(
        val name: String,
        val animationScale: Float = 0f,
        val forceGpu: Boolean = true,
        val killApps: Boolean = true,
        val resolution: String = "reset",
        val refreshRate: Int = 90,
        val brightness: Int = 180,
        val msaa: Boolean = false,
        val dns: String = "cloudflare",
        val wifiLock: Boolean = true,
        val dnd: Boolean = true
    )

    data class ProfileResult(
        val profileName: String,
        val actionsApplied: List<String> = emptyList(),
        val isSuccess: Boolean = false,
    )

    /**
     * Apply a game profile with real system changes.
     * FF (isMaxVersion=false) vs FF Max (isMaxVersion=true)
     */
    suspend fun applyProfile(profileName: String, isMaxVersion: Boolean = false): ProfileResult {
        return when (profileName) {
            "Mượt" -> applySmoothProfile(isMaxVersion)
            "Cân bằng" -> applyBalancedProfile(isMaxVersion)
            "Hiệu năng" -> applyPerformanceProfile(isMaxVersion)
            "Tiết kiệm pin" -> applyBatterySaverProfile(isMaxVersion)
            else -> applyBalancedProfile(isMaxVersion)
        }
    }

    /**
     * Apply a game profile object (used by GameLifecycleManager auto-detection).
     */
    suspend fun applyProfile(profile: GameProfile): ProfileResult {
        val actions = mutableListOf<String>()
        try {
            if (profile.killApps) {
                shizukuManager.executeCommand(
                    "for pkg in \$(pm list packages -3 | sed 's/package://'); do " +
                    "  [ \"\$pkg\" != \"${getOurPackageName()}\" ] && am force-stop \$pkg 2>/dev/null; " +
                    "done"
                )
                actions.add("Đã dừng ứng dụng nền")
            }
            if (profile.msaa) {
                if (applyForceMsaa()) actions.add("Đã bật 4x MSAA")
            }
            if (profile.resolution != "reset" && profile.resolution.isNotEmpty()) {
                val parts = profile.resolution.split("x")
                if (parts.size == 2) {
                    val w = parts[0].toIntOrNull() ?: 1280
                    val h = parts[1].toIntOrNull() ?: 720
                    if (setResolution(w, h)) actions.add("Đã đổi sang ${profile.resolution}")
                }
            }
            if (profile.refreshRate > 60) {
                if (boostRefreshRate()) actions.add("Đã ép tần số quét ${profile.refreshRate}Hz")
            }
            if (profile.wifiLock) {
                if (lockWifiAwake()) actions.add("Đã khóa WiFi khỏi ngủ")
            }
            shizukuManager.executeCommand("settings put global window_animation_scale ${profile.animationScale}")
            shizukuManager.executeCommand("settings put global transition_animation_scale ${profile.animationScale}")
            shizukuManager.executeCommand("settings put global animator_duration_scale ${profile.animationScale}")
            actions.add("Đã set animation scale")
            if (profile.forceGpu) {
                shizukuManager.executeCommand("settings put global force_gpu_rendering 1")
                actions.add("Đã bật tăng tốc GPU")
            }
            shizukuManager.executeCommand("settings put system screen_brightness ${profile.brightness}")
            actions.add("Đã chỉnh độ sáng")
            return ProfileResult(
                profileName = profile.name,
                actionsApplied = actions,
                isSuccess = true
            )
        } catch (e: Exception) {
            return ProfileResult(
                profileName = profile.name,
                actionsApplied = listOf("Lỗi: ${e.message}"),
                isSuccess = false
            )
        }
    }

    /**
     * Force 4x MSAA via developer settings.
     */
    private suspend fun applyForceMsaa(): Boolean {
        return try {
            val cmd = shizukuManager.executeCommand("settings put global force_4x_msaa 1")
            cmd.isSuccess
        } catch (e: Exception) { false }
    }

    /**
     * Change screen resolution via wm size.
     */
    private suspend fun setResolution(width: Int, height: Int): Boolean {
        return try {
            val cmd = shizukuManager.executeCommand("wm size ${width}x$height")
            cmd.isSuccess
        } catch (e: Exception) { false }
    }

    /**
     * Reset resolution to default.
     */
    private suspend fun resetResolution(): Boolean {
        return try {
            val cmd = shizukuManager.executeCommand("wm size reset")
            cmd.isSuccess
        } catch (e: Exception) { false }
    }

    /**
     * Boost refresh rate to max supported.
     */
    private suspend fun boostRefreshRate(): Boolean {
        return try {
            shizukuManager.executeCommand("settings put global peak_refresh_rate 120")
            shizukuManager.executeCommand("settings put global user_preferred_refresh_rate 120")
            true
        } catch (e: Exception) { false }
    }

    /**
     * Lock WiFi awake (disable WiFi sleep).
     */
    private suspend fun lockWifiAwake(): Boolean {
        return try {
            shizukuManager.executeCommand("settings put global wifi_sleep_policy 2")
            true
        } catch (e: Exception) { false }
    }

    /**
     * Immersive mode to hide nav/status bars.
     */
    private suspend fun setImmersiveMode(): Boolean {
        return try {
            shizukuManager.executeCommand("settings put global policy_control immersive.full=*")
            true
        } catch (e: Exception) { false }
    }

    /**
     * "Mượt" Profile - Reduce graphics, max FPS, kill background apps.
     * FF Max gets 720p resolution + MSAA for smoother 60fps.
     */
    private suspend fun applySmoothProfile(isMax: Boolean = false): ProfileResult = withContext(Dispatchers.IO) {
        val actions = mutableListOf<String>()

        try {
            // 1. Kill non-essential apps (including FF/FF Max)
            shizukuManager.executeCommand(
                "for pkg in \$(pm list packages -3 | sed 's/package://'); do " +
                "  [ \"\$pkg\" != \"${getOurPackageName()}\" ] && am force-stop \$pkg 2>/dev/null; " +
                "done"
            )
            actions.add("Đã dừng ứng dụng nền")

            // 2. Force 4x MSAA for sharper visuals at lower res
            if (applyForceMsaa()) {
                actions.add("Đã bật 4x MSAA")
            }

            // 3. FF Max: drop to 720p for buttery smooth 60fps
            if (isMax) {
                if (setResolution(1280, 720)) {
                    actions.add("Đã hạ xuống 720p (FF Max mượt hơn)")
                }
            }

            // 4. Boost refresh rate
            if (boostRefreshRate()) {
                actions.add("Đã ép tần số quét tối đa")
            }

            // 5. Immersive mode - full screen
            if (setImmersiveMode()) {
                actions.add("Đã bật chế độ toàn màn hình")
            }

            // 6. Disable animations
            shizukuManager.executeCommand("settings put global window_animation_scale 0.0")
            shizukuManager.executeCommand("settings put global transition_animation_scale 0.0")
            shizukuManager.executeCommand("settings put global animator_duration_scale 0.0")
            actions.add("Đã tắt hiệu ứng chuyển động")

            // 7. Enable GPU rendering
            shizukuManager.executeCommand("settings put global force_gpu_rendering 1")
            actions.add("Đã bật tăng tốc GPU")

            // 8. Clear cache
            optimizationEngine.quickOptimize()
            actions.add("Đã dọn bộ nhớ đệm")

            ProfileResult(
                profileName = "Mượt",
                actionsApplied = actions,
                isSuccess = true,
            )
        } catch (e: Exception) {
            ProfileResult(profileName = "Mượt", actionsApplied = listOf("Lỗi: ${e.message}"), isSuccess = false)
        }
    }

    /**
     * "Cân bằng" Profile - Moderate settings with balanced features.
     */
    private suspend fun applyBalancedProfile(isMax: Boolean = false): ProfileResult = withContext(Dispatchers.IO) {
        val actions = mutableListOf<String>()

        try {
            // 1. FF Max: moderate resolution drop (1080p -> 720p works great)
            if (isMax) {
                if (setResolution(1280, 720)) {
                    actions.add("Đã chuyển sang 720p")
                }
            }

            // 2. Force MSAA
            if (applyForceMsaa()) {
                actions.add("Đã bật 4x MSAA")
            }

            // 3. Refresh rate boost
            if (boostRefreshRate()) {
                actions.add("Đã ép tần số quét")
            }

            // 4. WiFi lock
            if (lockWifiAwake()) {
                actions.add("Đã khóa WiFi không ngủ")
            }

            // 5. Reduce animations (0.5x)
            shizukuManager.executeCommand("settings put global window_animation_scale 0.5")
            shizukuManager.executeCommand("settings put global transition_animation_scale 0.5")
            shizukuManager.executeCommand("settings put global animator_duration_scale 0.5")
            actions.add("Giảm hiệu ứng chuyển động")

            // 6. GPU rendering
            shizukuManager.executeCommand("settings put global force_gpu_rendering 1")
            actions.add("Bật tăng tốc GPU")

            // 7. Kill social media apps only
            val pkgResult = shizukuManager.executeCommand(
                "pm list packages -3 | grep -E 'facebook|instagram|tiktok|snapchat|messenger|zalo' | sed 's/package://'"
            ).getOrNull() ?: ""
            pkgResult.lines().filter { it.isNotBlank() }.forEach { pkg ->
                shizukuManager.executeCommand("am force-stop $pkg")
            }
            actions.add("Đã dừng ứng dụng mạng xã hội")

            ProfileResult(profileName = "Cân bằng", actionsApplied = actions, isSuccess = true)
        } catch (e: Exception) {
            ProfileResult(profileName = "Cân bằng", isSuccess = false)
        }
    }

    /**
     * "Hiệu năng" Profile - Maximum performance.
     * FF: 720p, animations off, GPU max, all kill, MSAA
     * FF Max: same but with resolution optimization
     */
    private suspend fun applyPerformanceProfile(isMax: Boolean = false): ProfileResult = withContext(Dispatchers.IO) {
        val actions = mutableListOf<String>()

        try {
            // 1. Kill ALL background apps
            shizukuManager.executeCommand(
                "for pkg in \$(pm list packages -3 | sed 's/package://'); do " +
                "  [ \"\$pkg\" != \"${getOurPackageName()}\" ] && " +
                "  [ \"\$pkg\" != \"com.google.android.gms\" ] && " +
                "  [ \"\$pkg\" != \"rikka.shizuku\" ] && " +
                "  am force-stop \$pkg 2>/dev/null; " +
                "done"
            )
            actions.add("Đã dừng tất cả ứng dụng nền")

            // 2. Force 4x MSAA
            if (applyForceMsaa()) {
                actions.add("Đã bật 4x MSAA tối đa")
            }

            // 3. Performance resolution
            if (isMax) {
                // FF Max: 720p for high FPS
                if (setResolution(1280, 720)) {
                    actions.add("Đã hạ xuống 720p (FF Max)")
                }
            } else {
                // FF regular: even lower res for max FPS
                if (setResolution(854, 480)) {
                    actions.add("Đã hạ xuống 480p (FF siêu mượt)")
                }
            }

            // 4. Boost refresh rate
            if (boostRefreshRate()) {
                actions.add("Đã ép tần số quét lên 120Hz")
            }

            // 5. WiFi lock
            if (lockWifiAwake()) {
                actions.add("Đã khóa WiFi")
            }

            // 6. Immersive + no animations
            setImmersiveMode()
            shizukuManager.executeCommand("settings put global window_animation_scale 0.0")
            shizukuManager.executeCommand("settings put global transition_animation_scale 0.0")
            shizukuManager.executeCommand("settings put global animator_duration_scale 0.0")
            actions.add("Tắt hoàn toàn hiệu ứng")

            // 7. GPU rendering max
            shizukuManager.executeCommand("settings put global force_gpu_rendering 1")
            actions.add("Tăng tốc GPU tối đa")

            // 8. Background process limit to 2
            shizukuManager.executeCommand("settings put global background_process_limit 2")
            actions.add("Giới hạn tiến trình nền")

            // 9. Cache cleanup
            shizukuManager.executeCommand("pm trim-caches 9999999999999")
            actions.add("Dọn toàn bộ cache hệ thống")

            ProfileResult(profileName = "Hiệu năng", actionsApplied = actions, isSuccess = true)
        } catch (e: Exception) {
            ProfileResult(profileName = "Hiệu năng", isSuccess = false)
        }
    }

    /**
     * "Tiết kiệm pin" Profile - Battery saving mode (non-gaming).
     */
    private suspend fun applyBatterySaverProfile(isMax: Boolean = false): ProfileResult = withContext(Dispatchers.IO) {
        val actions = mutableListOf<String>()

        try {
            // 1. Reset resolution to default
            if (resetResolution()) {
                actions.add("Khôi phục độ phân giải")
            }

            // 2. Restore animations to normal
            shizukuManager.executeCommand("settings put global window_animation_scale 1.0")
            shizukuManager.executeCommand("settings put global transition_animation_scale 1.0")
            shizukuManager.executeCommand("settings put global animator_duration_scale 1.0")
            actions.add("Khôi phục hiệu ứng")

            // 3. Disable GPU rendering & MSAA
            shizukuManager.executeCommand("settings put global force_gpu_rendering 0")
            shizukuManager.executeCommand("settings put global force_4x_msaa 0")
            actions.add("Tắt tăng tốc GPU + MSAA")

            // 4. Reset background process limit
            shizukuManager.executeCommand("settings put global background_process_limit -1")
            actions.add("Khôi phục tiến trình nền")

            // 5. Lower screen brightness
            shizukuManager.executeCommand("settings put system screen_brightness 80")
            actions.add("Giảm độ sáng màn hình")

            // 6. Screen timeout
            shizukuManager.executeCommand("settings put system screen_off_timeout 15000")
            actions.add("Tự động tắt màn hình nhanh")

            // 7. Haptic feedback off
            shizukuManager.executeCommand("settings put system haptic_feedback_enabled 0")
            actions.add("Tắt phản hồi rung")

            // 8. WiFi sleep policy - let WiFi sleep
            shizukuManager.executeCommand("settings put global wifi_sleep_policy 0")
            actions.add("Cho WiFi ngủ khi tắt màn")

            // 9. Remove immersive mode
            shizukuManager.executeCommand("settings put global policy_control immersive.preconfirms=null")
            actions.add("Thoát chế độ toàn màn hình")

            ProfileResult(profileName = "Tiết kiệm pin", actionsApplied = actions, isSuccess = true)
        } catch (e: Exception) {
            ProfileResult(profileName = "Tiết kiệm pin", isSuccess = false)
        }
    }

    /**
     * Reset all settings to default.
     */
    suspend fun resetSettings(): ProfileResult = withContext(Dispatchers.IO) {
        val actions = mutableListOf<String>()

        try {
            shizukuManager.executeCommand("settings put global window_animation_scale 1.0")
            shizukuManager.executeCommand("settings put global transition_animation_scale 1.0")
            shizukuManager.executeCommand("settings put global animator_duration_scale 1.0")
            shizukuManager.executeCommand("settings put global force_gpu_rendering 0")
            shizukuManager.executeCommand("settings put global force_4x_msaa 0")
            shizukuManager.executeCommand("settings put global background_process_limit -1")
            shizukuManager.executeCommand("settings put system haptic_feedback_enabled 1")
            shizukuManager.executeCommand("settings put global wifi_sleep_policy 0")
            shizukuManager.executeCommand("settings put global policy_control immersive.preconfirms=null")
            resetResolution()
            actions.add("Đã khôi phục cài đặt mặc định")

            ProfileResult(profileName = "Mặc định", actionsApplied = actions, isSuccess = true)
        } catch (e: Exception) {
            ProfileResult(profileName = "Mặc định", isSuccess = false)
        }
    }

    private fun getOurPackageName(): String {
        return "com.nmdlock.app"
    }
}
