package com.nmdlock.app.core.services

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ═══════════════════════════════════════════════════════════════════════
 * PILLAR A: TOUCH SENSITIVITY BOOSTER (Siêu Nhạy)
 * ═══════════════════════════════════════════════════════════════════════
 *
 * Tăng độ nhạy màn hình cảm ứng qua Shizuku commands.
 * Hỗ trợ 4 levels: NORMAL → HIGH → ULTRA → EXTREME
 * Có kernel-level tuning cho device hỗ trợ (Samsung, Xiaomi, ROG...)
 */
@Singleton
class TouchSensitivityBooster @Inject constructor(
    private val shizukuManager: ShizukuManager,
    private val commandQueue: ShizukuCommandQueue,
) {
    companion object {
        private const val TAG = "TouchSensitivityBooster"
    }

    enum class SensitivityLevel(val value: Int, val displayName: String) {
        NORMAL(0, "Bình thường"),
        HIGH(1, "Nhạy"),
        ULTRA(2, "Siêu nhạy"),
        EXTREME(3, "Cực nhạy")  // Có thể gây false positive
    }

    private val _currentLevel = MutableStateFlow(SensitivityLevel.NORMAL)
    val currentLevel: StateFlow<SensitivityLevel> = _currentLevel.asStateFlow()

    // Snapshot để restore
    private var originalSettings: Map<String, String> = emptyMap()
    private var isEnabled = false

    /**
     * Apply siêu nhạy — Giảm touch slop (khoảng cách tối thiểu để nhận swipe)
     * và tăng sampling rate
     */
    suspend fun enable(level: SensitivityLevel) {
        try {
            if (originalSettings.isEmpty()) {
                backupSettings()
            }

            _currentLevel.value = level
            isEnabled = true

            val commands = buildSensitivityCommands(level)
            commands.forEach { cmd ->
                commandQueue.submit(cmd, ShizukuCommandQueue.Priority.HIGH)
            }

            // Kernel-level tuning (nếu device hỗ trợ)
            applyKernelTouchTuning(level)

            Log.d(TAG, "Touch sensitivity enabled: ${level.displayName}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable sensitivity: ${e.message}")
        }
    }

    suspend fun disable() {
        try {
            restoreSettings()
            _currentLevel.value = SensitivityLevel.NORMAL
            isEnabled = false
            Log.d(TAG, "Touch sensitivity disabled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disable sensitivity: ${e.message}")
        }
    }

    fun isEnabled(): Boolean = isEnabled

    private fun buildSensitivityCommands(level: SensitivityLevel): List<String> {
        return when (level) {
            SensitivityLevel.NORMAL -> listOf(
                "settings put system pointer_speed 0.0",
                "settings put system touch_slop 8",
                "settings put secure long_press_timeout 500",
                "settings put secure multi_press_timeout 300"
            )
            SensitivityLevel.HIGH -> listOf(
                "settings put system pointer_speed 0.5",
                "settings put system touch_slop 4",
                "settings put secure long_press_timeout 300",
                "settings put secure multi_press_timeout 200",
                "settings put system stylus_icon_enabled 0"
            )
            SensitivityLevel.ULTRA -> listOf(
                "settings put system pointer_speed 1.0",
                "settings put system touch_slop 2",
                "settings put secure long_press_timeout 200",
                "settings put secure multi_press_timeout 150",
                // Gaming mode touch sensitivity (Xiaomi/ROG)
                "settings put system gaming_touch_sensitivity 1",
                "setprop persist.sys.game_touch_sensitivity 1"
            )
            SensitivityLevel.EXTREME -> listOf(
                "settings put system pointer_speed 1.0",
                "settings put system touch_slop 1",        // Minimum slop
                "settings put secure long_press_timeout 100",
                "settings put secure multi_press_timeout 100",
                // Disable touch filtering
                "setprop debug.input.touch_filtering 0",
                "setprop debug.sf.disable_touch_resampling 1",
                // Samsung gaming touch boost
                "settings put system game_touch_boost 1",
                // Extra touch parameters
                "settings put system touch_debounce_time 0",
                "settings put system multitouch_max_pointers 10"
            )
        }
    }

    /**
     * KERNEL-LEVEL TUNING — sysfs touch sensitivity
     * Chỉ hoạt động trên một số kernel tùy chỉnh
     */
    private suspend fun applyKernelTouchTuning(level: SensitivityLevel) {
        val kernelCommands = mutableListOf<String>()

        // Base kernel commands
        kernelCommands.addAll(
            listOf(
                // Samsung touch sensitivity
                "echo 1 > /sys/class/sec/tsp/cmd 2>/dev/null",
                "echo 'glove_mode,1' > /sys/class/sec/tsp/cmd 2>/dev/null",
                // Touch boost (MSM chipsets)
                "echo 1 > /sys/module/msm_performance/parameters/touchboost 2>/dev/null",
                "echo 1 > /sys/power/pnpmgr/touch_boost 2>/dev/null",
                // Increase touch sampling rate
                "echo 240 > /sys/class/touch/touch_dev/glove_mode 2>/dev/null",
                // Disable touch filter
                "echo 0 > /sys/devices/virtual/touchscreen/filter_en 2>/dev/null"
            )
        )

        when (level) {
            SensitivityLevel.EXTREME -> kernelCommands.addAll(
                listOf(
                    // Force maximum sensitivity
                    "echo 1 > /sys/class/sec/tsp/force_sensitivity 2>/dev/null",
                    "echo 1 > /sys/devices/virtual/touchscreen/force_mode 2>/dev/null",
                    // CPU input boost
                    "echo 1 > /sys/kernel/hba/input_boost_enable 2>/dev/null",
                    "echo 1800000 > /sys/kernel/hba/input_boost_freq 2>/dev/null"
                )
            )
            SensitivityLevel.ULTRA -> kernelCommands.addAll(
                listOf(
                    // Moderate kernel tuning
                    "echo 1 > /sys/devices/virtual/touchscreen/force_mode 2>/dev/null"
                )
            )
            else -> { /* no-op */ }
        }

        kernelCommands.forEach { cmd ->
            try {
                commandQueue.submit(cmd, ShizukuCommandQueue.Priority.NORMAL)
            } catch (_: Exception) { }
        }
    }

    private suspend fun backupSettings() {
        val keys = listOf(
            "system pointer_speed",
            "system touch_slop",
            "secure long_press_timeout",
            "secure multi_press_timeout"
        )

        val map = mutableMapOf<String, String>()
        for (key in keys) {
            try {
                val result = shizukuManager.executeCommand("settings get $key")
                val value = result.getOrNull()?.trim()?.takeIf { it != "null" && it.isNotEmpty() }
                map[key] = value ?: "default"
            } catch (_: Exception) {
                map[key] = "default"
            }
        }
        originalSettings = map
    }

    private suspend fun restoreSettings() {
        if (originalSettings.isEmpty()) return

        originalSettings.forEach { (key, value) ->
            try {
                shizukuManager.executeCommand("settings put $key $value")
            } catch (_: Exception) { }
        }
        originalSettings = emptyMap()
    }

    /**
     * Calculate optimal sensitivity dựa trên screen size.
     */
    fun calculateOptimalLevel(screenWidth: Int, screenHeight: Int): SensitivityLevel {
        val screenSizeInches = kotlin.math.sqrt(
            (screenWidth * screenWidth + screenHeight * screenHeight).toDouble()
        ) / 320.0  // Assume 320 DPI

        return when {
            screenSizeInches < 5.5 -> SensitivityLevel.EXTREME  // Phone nhỏ
            screenSizeInches < 6.5 -> SensitivityLevel.ULTRA    // Phone vừa
            screenSizeInches < 7.5 -> SensitivityLevel.HIGH     // Phone to
            else -> SensitivityLevel.NORMAL                     // Tablet
        }
    }
}
