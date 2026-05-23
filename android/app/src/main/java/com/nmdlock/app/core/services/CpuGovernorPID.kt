package com.nmdlock.app.core.services

import com.nmdlock.app.core.util.FloatCircularBuffer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ═══════════════════════════════════════════════════════════════════════
 * PILLAR 2: PID CONTROLLER — Auto-tune CPU Governor theo target FPS
 * ═══════════════════════════════════════════════════════════════════════
 *
 * PID (Proportional-Integral-Derivative) Controller
 * - Input: Current FPS (từ game)
 * - Target: 60 FPS (có thể config)
 * - Output: CPU governor config (min/max freq, governor)
 *
 * ⚠️ LƯU Ý: Việc ghi vào /sys/devices/system/cpu/ yêu cầu ROOT
 * Nếu không có root, controller sẽ dùng `settings put global` thay thế
 */
@Singleton
class CpuGovernorPID @Inject constructor(
    private val commandQueue: ShizukuCommandQueue
) {
    // PID parameters (tuned empirically cho gaming)
    private val kp = 2.0f   // Proportional gain
    private val ki = 0.5f   // Integral gain
    private val kd = 1.0f   // Derivative gain

    private var targetFps = 60f
    private var integral = 0f
    private var lastError = 0f
    private var lastTime = System.nanoTime()

    // Anti-windup: giới hạn integral term
    private val integralMin = -50f
    private val integralMax = 50f

    // Hysteresis: tránh flapping giữa các config
    private var currentConfigIndex = 2 // Bắt đầu từ "balanced"

    private val _pidOutput = MutableStateFlow(PidOutput())
    val pidOutput: StateFlow<PidOutput> = _pidOutput.asStateFlow()

    private var autoTuneJob: Job? = null

    data class GovernorConfig(
        val name: String,
        val governor: String,     // performance, schedutil, ondemand, powersave
        val minFreq: Int,         // MHz
        val maxFreq: Int,
        val boostPulse: Boolean,
        val description: String
    )

    data class PidOutput(
        val targetFps: Float = 60f,
        val currentFps: Float = 0f,
        val error: Float = 0f,
        val pTerm: Float = 0f,
        val iTerm: Float = 0f,
        val dTerm: Float = 0f,
        val output: Float = 0f,
        val config: GovernorConfig = GovernorConfig("default", "schedutil", 1000, 2000, false, "Default")
    )

    // Pre-defined governor configs (từ aggressive nhất đến tiết kiệm nhất)
    private val configs = listOf(
        GovernorConfig("extreme", "performance", 2000, 2800, true, "Hiệu năng tối đa (root)"),
        GovernorConfig("gaming", "performance", 1800, 2400, true, "Chơi game (root)"),
        GovernorConfig("balanced", "schedutil", 1400, 2200, true, "Cân bằng"),
        GovernorConfig("efficient", "schedutil", 1000, 2000, false, "Tiết kiệm pin"),
        GovernorConfig("powersave", "ondemand", 600, 1500, false, "Tiết kiệm tối đa")
    )

    fun setTargetFps(fps: Int) {
        targetFps = fps.toFloat()
    }

    /**
     * Bắt đầu auto-tuning loop
     * @param fpsProvider Lambda trả về FPS hiện tại
     */
    fun startAutoTuning(scope: CoroutineScope, fpsProvider: () -> Float) {
        if (autoTuneJob?.isActive == true) return
        autoTuneJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                val currentFps = fpsProvider()
                if (currentFps > 0f) {
                    computeAndApply(currentFps)
                }
                delay(1000) // Tune mỗi giây
            }
        }
    }

    fun stopAutoTuning() {
        autoTuneJob?.cancel()
        autoTuneJob = null
    }

    /**
     * PID Computation
     */
    private suspend fun computeAndApply(currentFps: Float) {
        val now = System.nanoTime()
        val dt = (now - lastTime) / 1_000_000_000.0
        lastTime = now

        // Error = Target - Current
        val error = targetFps - currentFps

        // Proportional term
        val pTerm = kp * error

        // Integral term with anti-windup
        integral += error * dt.toFloat()
        integral = integral.coerceIn(integralMin, integralMax)
        val iTerm = ki * integral

        // Derivative term
        val derivative = if (dt > 0) (error - lastError) / dt.toFloat() else 0f
        val dTerm = kd * derivative
        lastError = error

        // PID output
        val output = pTerm + iTerm + dTerm

        // Map output to governor config (với hysteresis)
        val newConfigIndex = when {
            output > 25 -> 0 // extreme
            output > 15 -> 1 // gaming
            output > 5  -> 2 // balanced
            output > -5 -> 3 // efficient
            else -> 4        // powersave
        }

        // Hysteresis: chỉ thay đổi nếu khác config hiện tại >= 2 bậc
        if (kotlin.math.abs(newConfigIndex - currentConfigIndex) >= 2 ||
            (newConfigIndex != currentConfigIndex && output > 10f)) {
            currentConfigIndex = newConfigIndex
            applyGovernorConfig(configs[newConfigIndex])
        }

        _pidOutput.value = PidOutput(
            targetFps = targetFps,
            currentFps = currentFps,
            error = error,
            pTerm = pTerm,
            iTerm = iTerm,
            dTerm = dTerm,
            output = output,
            config = configs[newConfigIndex]
        )
    }

    /**
     * Apply governor config
     * Thử ghi vào /sys/ trước (cần root), fallback sang settings (Shizuku)
     */
    private suspend fun applyGovernorConfig(config: GovernorConfig) {
        // Cách 1: Ghi trực tiếp vào /sys/ (cần ROOT)
        val rootCommands = (0..7).map { core ->
            "echo ${config.governor} > /sys/devices/system/cpu/cpu$core/cpufreq/scaling_governor 2>/dev/null"
        } + listOf(
            "echo ${config.minFreq * 1000} > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq 2>/dev/null",
            "echo ${config.maxFreq * 1000} > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq 2>/dev/null"
        )

        // Cách 2: Fallback settings global (Shizuku, ko cần root)
        val shizukuFallback = listOf(
            "settings put global window_animation_scale ${if (config.governor == "performance") 0 else 0.5}",
            "settings put global transition_animation_scale ${if (config.governor == "performance") 0 else 0.5}",
            "settings put global animator_duration_scale ${if (config.governor == "performance") 0 else 0.5}",
            "settings put global force_gpu_rendering ${if (config.governor != "powersave") 1 else 0}",
            "settings put global power_sound ${if (config.governor == "powersave") 0 else 1}"
        )

        // Submit cả 2 cách: nếu root không được, Shizuku fallback sẽ chạy
        rootCommands.forEach { cmd ->
            commandQueue.submit(cmd, ShizukuCommandQueue.Priority.LOW)
        }
        shizukuFallback.forEach { cmd ->
            commandQueue.submit(cmd, ShizukuCommandQueue.Priority.NORMAL)
        }
    }

    fun getAvailableConfigs(): List<GovernorConfig> = configs
}
