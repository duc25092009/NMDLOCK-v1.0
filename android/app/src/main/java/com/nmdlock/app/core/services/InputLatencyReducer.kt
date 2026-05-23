package com.nmdlock.app.core.services

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * ═══════════════════════════════════════════════════════════════════════
 * PILLAR B: INPUT LATENCY REDUCER (Siêu Phản Hồi)
 * ═══════════════════════════════════════════════════════════════════════
 *
 * Zero-Latency Touch Pipeline — Tối ưu toàn bộ pipeline:
 * Input → SurfaceFlinger → Render → Display
 *
 * Features:
 * - Input dispatcher optimization
 * - SurfaceFlinger latency reduction
 * - Render ahead prediction
 * - CPU touch boost
 * - GPU priority boost
 * - Predictive touch engine (Kalman Filter)
 */
@Singleton
class InputLatencyReducer @Inject constructor(
    private val shizukuManager: ShizukuManager,
    private val commandQueue: ShizukuCommandQueue,
) {
    companion object {
        private const val TAG = "InputLatencyReducer"
    }

    private val _isZeroLatencyEnabled = MutableStateFlow(false)
    val isZeroLatencyEnabled: StateFlow<Boolean> = _isZeroLatencyEnabled.asStateFlow()

    private val _predictedTouch = MutableStateFlow<PredictedTouch?>(null)
    val predictedTouch: StateFlow<PredictedTouch?> = _predictedTouch.asStateFlow()

    // Predictive touch engine
    private val predictiveEngine = PredictiveTouchEngine()

    /**
     * Enable Zero-Latency Mode — Kích hoạt tất cả tối ưu
     */
    suspend fun enableZeroLatencyMode() {
        try {
            val commands = mutableListOf<String>()

            // ============================================
            // 1. INPUT DISPATCHER OPTIMIZATION
            // ============================================
            commands.addAll(
                listOf(
                    // Disable touch event filtering/resampling
                    "setprop debug.input.dispatch_latency_warning_ms 1",
                    "setprop debug.input.enable_touch_resampling 0",
                    "setprop debug.input.max_touch_resample_time 0",
                    // Reduce input queue depth
                    "setprop debug.input.max_batch_size 1",
                    // Priority boost cho input dispatcher thread
                    "setprop debug.input.dispatcher_priority 1"
                )
            )

            // ============================================
            // 2. SURFACEFLINGER LATENCY REDUCTION
            // ============================================
            commands.addAll(
                listOf(
                    // Latch unsignaled buffers - giảm wait time
                    "setprop debug.sf.latch_unsignaled 1",
                    // Disable backpressure (có thể gây tearing nhưng giảm latency)
                    "setprop debug.sf.enable_gl_backpressure 0",
                    // Early phase offset - render sớm hơn
                    "setprop debug.sf.early_phase_offset_ns 1000000",
                    "setprop debug.sf.early_gl_phase_offset_ns 2000000",
                    // Triple buffering
                    "setprop debug.sf.max_acquire_duration 1",
                    "setprop ro.surface_flinger.max_frame_buffer_acquired_buffers 3",
                    // Disable VSYNC smoothing để response nhanh hơn
                    "setprop debug.choreographer.vsync 0",
                    "setprop debug.sf.disable_backpressure 1",
                    // Frame rate override (nếu màn hình hỗ trợ)
                    "settings put global peak_refresh_rate 120",
                    "settings put global min_refresh_rate 120"
                )
            )

            // ============================================
            // 3. RENDER AHEAD (Dự đoán frame tiếp theo)
            // ============================================
            commands.addAll(
                listOf(
                    // Render 2 frames ahead
                    "setprop debug.hwui.render_ahead 2",
                    // Increase HWUI thread priority
                    "setprop debug.hwui.render_dirty_regions 0",
                    "setprop debug.hwui.profile false",
                    // Disable HWUI features gây latency
                    "setprop debug.hwui.disable_vsync true"
                )
            )

            // ============================================
            // 4. CPU TOUCH BOOST (Tăng CPU freq khi chạm)
            // ============================================
            commands.addAll(
                listOf(
                    // MSM touch boost
                    "echo 1 > /sys/module/msm_performance/parameters/touchboost 2>/dev/null",
                    "echo '0:1800000 1:1800000 2:1800000 3:1800000' > /sys/module/msm_performance/parameters/cpu_max_freq 2>/dev/null",
                    // PNP manager touch boost
                    "echo 1 > /sys/power/pnpmgr/touch_boost 2>/dev/null",
                    "echo 2000000 > /sys/power/pnpmgr/touch_boost_freq 2>/dev/null",
                    "echo 100 > /sys/power/pnpmgr/touch_boost_duration 2>/dev/null",
                    // CPU input boost (Samsung)
                    "echo 1 > /sys/kernel/hba/input_boost_enable 2>/dev/null",
                    "echo 1800000 > /sys/kernel/hba/input_boost_freq 2>/dev/null",
                    // SchedTune boost cho top-app
                    "echo 30 > /dev/stune/top-app/schedtune.boost 2>/dev/null",
                    "echo 1 > /dev/stune/top-app/schedtune.prefer_idle 2>/dev/null",
                    // CPU freq lock (high performance khi đang touch)
                    "echo performance > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor 2>/dev/null",
                    "echo 1200000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq 2>/dev/null"
                )
            )

            // ============================================
            // 5. GPU PRIORITY BOOST
            // ============================================
            commands.addAll(
                listOf(
                    // GPU frequency lock
                    "echo performance > /sys/class/kgsl/kgsl-3d0/devfreq/governor 2>/dev/null",
                    // GPU boost on touch
                    "echo 1 > /sys/class/kgsl/kgsl-3d0/devfreq/adreno_boost 2>/dev/null"
                )
            )

            // ============================================
            // 6. HAPTIC FEEDBACK OPTIMIZATION
            // ============================================
            commands.addAll(
                listOf(
                    // Giảm thời gian rung phản hồi
                    "settings put system haptic_feedback_duration 10",
                    "settings put system haptic_feedback_intensity 50",
                    // Disable unnecessary haptics
                    "settings put system sound_effects_enabled 0"
                )
            )

            // Execute tất cả qua Shizuku
            commands.forEach { cmd ->
                try {
                    commandQueue.submit(cmd, ShizukuCommandQueue.Priority.HIGH)
                } catch (_: Exception) { }
            }

            _isZeroLatencyEnabled.value = true
            Log.d(TAG, "Zero-Latency Mode enabled: ${commands.size} commands applied")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable zero-latency mode: ${e.message}")
        }
    }

    /**
     * Disable Zero-Latency Mode — Khôi phục cài đặt
     */
    suspend fun disableZeroLatencyMode() {
        try {
            val restoreCommands = listOf(
                "setprop debug.input.enable_touch_resampling 1",
                "setprop debug.sf.latch_unsignaled 0",
                "setprop debug.sf.enable_gl_backpressure 1",
                "setprop debug.hwui.render_ahead 0",
                "setprop debug.hwui.disable_vsync false",
                "setprop debug.choreographer.vsync 1",
                "setprop debug.sf.disable_backpressure 0",
                "settings put global peak_refresh_rate 60",
                "settings put global min_refresh_rate 60",
                // Restore CPU governor
                "echo schedutil > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor 2>/dev/null",
                // Restore haptic
                "settings put system sound_effects_enabled 1"
            )

            restoreCommands.forEach { cmd ->
                try {
                    commandQueue.submit(cmd, ShizukuCommandQueue.Priority.NORMAL)
                } catch (_: Exception) { }
            }

            _isZeroLatencyEnabled.value = false
            Log.d(TAG, "Zero-Latency Mode disabled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disable zero-latency mode: ${e.message}")
        }
    }

    /**
     * Predictive Touch Engine — Dự đoán vị trí chạm tiếp theo dựa trên trajectory
     * Dùng Kalman Filter để smooth prediction
     */
    fun onTouch(x: Float, y: Float, timestamp: Long): PredictedTouch? {
        val prediction = predictiveEngine.onTouch(x, y, timestamp)
        _predictedTouch.value = prediction
        return prediction
    }

    fun resetPredictiveEngine() {
        predictiveEngine.reset()
        _predictedTouch.value = null
    }

    /**
     * Predictive Touch — Dự đoán vị trí chạm tiếp theo
     */
    class PredictiveTouchEngine {
        private var kalmanX = KalmanFilter1D(processNoise = 0.1, measurementNoise = 1.0)
        private var kalmanY = KalmanFilter1D(processNoise = 0.1, measurementNoise = 1.0)

        private var lastTouchTime = 0L
        private var velocityX = 0f
        private var velocityY = 0f
        private var lastX = 0f
        private var lastY = 0f

        fun onTouch(x: Float, y: Float, timestamp: Long): PredictedTouch? {
            val dt = timestamp - lastTouchTime

            if (dt > 0 && lastTouchTime > 0) {
                velocityX = (x - lastX) / dt * 1000  // pixels/second
                velocityY = (y - lastY) / dt * 1000
            }

            kalmanX.update(x.toDouble())
            kalmanY.update(y.toDouble())

            lastX = x
            lastY = y
            lastTouchTime = timestamp

            // Predict 16ms ahead (1 frame @ 60fps)
            val predictAheadMs = 16L
            val predictedX = kalmanX.state.toFloat() + velocityX * predictAheadMs / 1000f
            val predictedY = kalmanY.state.toFloat() + velocityY * predictAheadMs / 1000f

            // Confidence dựa trên velocity stability
            val speed = sqrt(velocityX * velocityX + velocityY * velocityY)
            val confidence = (1.0f / (1.0f + speed / 2000f)).coerceIn(0f, 1f)

            return PredictedTouch(
                x = predictedX,
                y = predictedY,
                confidence = confidence,
                timeAhead = predictAheadMs
            )
        }

        fun reset() {
            kalmanX = KalmanFilter1D(processNoise = 0.1, measurementNoise = 1.0)
            kalmanY = KalmanFilter1D(processNoise = 0.1, measurementNoise = 1.0)
            lastTouchTime = 0L
            velocityX = 0f
            velocityY = 0f
            lastX = 0f
            lastY = 0f
        }
    }

    /**
     * Kalman Filter 1D — Smooth touch prediction
     */
    class KalmanFilter1D(
        private val processNoise: Double,
        private val measurementNoise: Double
    ) {
        var state = 0.0
            private set
        private var variance = 1.0

        fun update(measurement: Double) {
            // Predict
            val predictedVariance = variance + processNoise

            // Update
            val kalmanGain = predictedVariance / (predictedVariance + measurementNoise)
            state = state + kalmanGain * (measurement - state)
            variance = (1.0 - kalmanGain) * predictedVariance
        }

        fun reset() {
            state = 0.0
            variance = 1.0
        }
    }

    data class PredictedTouch(
        val x: Float,
        val y: Float,
        val confidence: Float,  // 0-1
        val timeAhead: Long     // ms
    )
}
