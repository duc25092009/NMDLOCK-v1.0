package com.nmdlock.app.core.services

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ═══════════════════════════════════════════════════════════════════════
 * PILLAR C: GAME-SPECIFIC TOUCH OPTIMIZATION
 * ═══════════════════════════════════════════════════════════════════════
 *
 * Kết hợp TouchSensitivityBooster + InputLatencyReducer
 * để tối ưu touch cho từng game cụ thể (Free Fire, FF Max...)
 *
 * Free Fire specific optimization:
 * - Cơ chế kéo tâm, bắn, nhảy
 * - Tăng touch precision cho joystick
 * - Giảm debounce, tăng multi-touch accuracy
 */
@Singleton
class GameTouchOptimizer @Inject constructor(
    private val sensitivityBooster: TouchSensitivityBooster,
    private val latencyReducer: InputLatencyReducer,
    private val shizukuManager: ShizukuManager,
) {
    companion object {
        private const val TAG = "GameTouchOptimizer"
    }

    /**
     * Free Fire specific optimization
     * Tối ưu cho cơ chế kéo tâm, bắn, nhảy
     */
    suspend fun applyFreeFireOptimization() {
        try {
            // Base sensitivity - Ultra cho Free Fire
            sensitivityBooster.enable(TouchSensitivityBooster.SensitivityLevel.ULTRA)

            // Zero latency mode
            latencyReducer.enableZeroLatencyMode()

            // Game-specific touch profiles
            val ffCommands = listOf(
                // Tăng touch precision cho vùng giữa màn hình (nơi có joystick)
                "settings put system touch_precision_x 1.0",
                "settings put system touch_precision_y 1.0",
                // Giảm debounce time (thời gian bỏ qua touch lặp)
                "settings put system touch_debounce_time 0",
                // Tăng multi-touch accuracy (cho nút bắn + di chuyển)
                "settings put system multitouch_max_pointers 10",
                // Disable palm rejection (đôi khi chặn touch legit)
                "settings put system palm_rejection_threshold 100"
            )

            ffCommands.forEach { cmd ->
                try {
                    shizukuManager.executeCommand(cmd)
                } catch (_: Exception) { }
            }

            Log.d(TAG, "Free Fire touch optimization applied successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply Free Fire optimization: ${e.message}")
        }
    }

    /**
     * Restore Free Fire optimization settings
     */
    suspend fun restoreFreeFireOptimization() {
        try {
            sensitivityBooster.disable()
            latencyReducer.disableZeroLatencyMode()

            val restoreCommands = listOf(
                "settings put system touch_debounce_time 2",
                "settings put system multitouch_max_pointers 3",
                "settings put system palm_rejection_threshold 50"
            )

            restoreCommands.forEach { cmd ->
                try {
                    shizukuManager.executeCommand(cmd)
                } catch (_: Exception) { }
            }

            Log.d(TAG, "Free Fire touch optimization restored")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore Free Fire optimization: ${e.message}")
        }
    }

    /**
     * Apply game-specific touch optimization dựa trên package name
     */
    suspend fun applyGameTouchProfile(gamePackage: String) {
        when (gamePackage) {
            "com.dts.freefireth", "com.dts.freefiremax" -> {
                applyFreeFireOptimization()
            }
            "com.tencent.ig" -> {
                // PUBG Mobile — cần precision cao cho scope
                sensitivityBooster.enable(TouchSensitivityBooster.SensitivityLevel.ULTRA)
            }
            "com.mobile.legends" -> {
                // Mobile Legends — cần response nhanh
                sensitivityBooster.enable(TouchSensitivityBooster.SensitivityLevel.HIGH)
                latencyReducer.enableZeroLatencyMode()
            }
            else -> {
                // Default gaming profile
                sensitivityBooster.enable(TouchSensitivityBooster.SensitivityLevel.HIGH)
                latencyReducer.enableZeroLatencyMode()
            }
        }
    }

    /**
     * Auto-tune sensitivity dựa trên device screen size
     */
    fun calculateOptimalSensitivity(screenWidth: Int, screenHeight: Int): TouchSensitivityBooster.SensitivityLevel {
        return sensitivityBooster.calculateOptimalLevel(screenWidth, screenHeight)
    }
}
