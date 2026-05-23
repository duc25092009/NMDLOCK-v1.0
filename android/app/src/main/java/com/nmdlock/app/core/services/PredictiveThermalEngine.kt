package com.nmdlock.app.core.services

import android.content.Context
import com.nmdlock.app.core.util.FloatCircularBuffer
import com.nmdlock.app.core.util.KalmanFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * ═══════════════════════════════════════════════════════════════════════
 * PILLAR 1: PREDICTIVE DYNAMIC ADAPTIVE ENGINE (P-DAE)
 * ═══════════════════════════════════════════════════════════════════════
 *
 * Dự đoán thermal throttling TRƯỚC KHI nó xảy ra 3-5 phút
 * Sử dụng EMA + Linear Regression + Z-Score + Fuzzy Logic + Hysteresis
 */
@Singleton
class PredictiveThermalEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shizukuManager: ShizukuManager
) {
    // ── EMA Parameters ──
    private var emaCpuTemp = 0f
    private var emaGpuTemp = 0f
    private val alpha = 0.3f // Weight cho sample mới (càng cao càng nhạy)

    // ── Kalman Filters ──
    private val cpuKalman = KalmanFilter(processNoise = 0.05, measurementNoise = 2.0)
    private val gpuKalman = KalmanFilter(processNoise = 0.05, measurementNoise = 2.0)
    private val batteryKalman = KalmanFilter(processNoise = 0.1, measurementNoise = 3.0)

    // ── Circular Buffers (60 samples @ 3s = 3 phút history) ──
    private val cpuHistory = FloatCircularBuffer(60)
    private val gpuHistory = FloatCircularBuffer(60)
    private val batteryHistory = FloatCircularBuffer(60)
    private val timestampHistory = com.nmdlock.app.core.util.LongCircularBuffer(60)

    // ── Hysteresis Thresholds ──
    private val THROTTLE_ENTER = 42.0f
    private val THROTTLE_EXIT = 38.0f
    private val CRITICAL_ENTER = 45.0f
    private val CRITICAL_EXIT = 41.0f
    private var wasThrottled = false

    // ── State ──
    private val _thermalState = MutableStateFlow(ThermalState.NORMAL)
    val thermalState: StateFlow<ThermalState> = _thermalState.asStateFlow()

    private val _prediction = MutableStateFlow(ThermalPrediction())
    val prediction: StateFlow<ThermalPrediction> = _prediction.asStateFlow()

    private var monitoringJob: Job? = null

    /**
     * Bắt đầu monitoring vòng lặp
     */
    fun startMonitoring(scope: CoroutineScope) {
        if (monitoringJob?.isActive == true) return
        monitoringJob = scope.launch(Dispatchers.IO + SupervisorJob()) {
            while (isActive) {
                try {
                    val sample = collectThermalSample()
                    processSample(sample)
                } catch (e: Exception) {
                    // Silent fallback
                }
                delay(3000) // 3 giây / sample
            }
        }
    }

    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
    }

    /**
     * Thu thập mẫu nhiệt từ thermal zones
     */
    private fun collectThermalSample(): ThermalSample {
        val now = System.currentTimeMillis()

        // Đọc CPU temp từ /sys/class/thermal/
        val cpuTemp = readThermalZone("cpu") ?:
                      readThermalZone("cpu-thermal") ?:
                      readThermalZone("cpu0") ?: 0f

        // Đọc GPU temp
        val gpuTemp = readThermalZone("gpu") ?:
                      readThermalZone("gpu-thermal") ?: 0f

        // Đọc battery temp từ BatteryManager
        val batteryTemp = readBatteryTemperature()

        // RAM available percentage
        val ramInfo = readRamInfo()

        return ThermalSample(
            cpuTemp = cpuTemp,
            gpuTemp = gpuTemp,
            batteryTemp = batteryTemp,
            ramAvailablePercent = ramInfo.first,
            ramTotalMB = ramInfo.second,
            timestamp = now
        )
    }

    /**
     * Xử lý sample với toàn bộ thuật toán
     */
    private fun processSample(sample: ThermalSample) {
        // 1. Kalman Filter — Smooth noise
        val kalmanCpu = cpuKalman.filterFloat(sample.cpuTemp)
        val kalmanGpu = gpuKalman.filterFloat(sample.gpuTemp)

        // 2. EMA (Exponential Moving Average) — See trend through noise
        emaCpuTemp = alpha * kalmanCpu + (1 - alpha) * emaCpuTemp
        emaGpuTemp = alpha * kalmanGpu + (1 - alpha) * emaGpuTemp

        // 3. Lưu vào buffer history
        cpuHistory.add(emaCpuTemp)
        gpuHistory.add(emaGpuTemp)
        batteryHistory.add(sample.batteryTemp)
        timestampHistory.add(sample.timestamp)

        // 4. Linear Regression Slope (°C/phút)
        val slope = calculateSlope()

        // 5. Dự đoán nhiệt độ 5 phút tới
        val predictedTemp5Min = emaCpuTemp + (slope * 5.0f)

        // 6. Tính thời gian tới ngưỡng throttle
        val timeToThrottle = if (slope > 0.01f) {
            ((THROTTLE_ENTER - emaCpuTemp) / slope * 60).toInt().coerceAtLeast(0)
        } else Int.MAX_VALUE

        // 7. Z-Score Anomaly Detection — Phát hiện spike bất thường
        val zScore = calculateZScore(emaCpuTemp)
        val isAnomaly = abs(zScore) > 2.5f

        // 8. Hysteresis State Machine — Tránh flapping
        val newState = when {
            emaCpuTemp > CRITICAL_ENTER -> ThermalState.CRITICAL
            emaCpuTemp < CRITICAL_EXIT && _thermalState.value == ThermalState.CRITICAL
                -> ThermalState.WARNING
            emaCpuTemp > THROTTLE_ENTER -> ThermalState.WARNING
            emaCpuTemp < THROTTLE_EXIT && _thermalState.value == ThermalState.WARNING
                -> ThermalState.NORMAL
            else -> _thermalState.value
        }

        // 9. Fuzzy Logic Risk Score (0.0 - 1.0)
        val riskScore = calculateFuzzyRisk(
            temp = emaCpuTemp,
            slope = slope,
            predictedTemp = predictedTemp5Min,
            ramAvailable = sample.ramAvailablePercent,
            batteryTemp = sample.batteryTemp
        )

        // 10. Auto-action dựa trên prediction
        when (newState) {
            ThermalState.CRITICAL -> executeEmergencyAction()
            ThermalState.WARNING -> executeWarningAction()
            ThermalState.NORMAL -> {
                if (_thermalState.value != ThermalState.NORMAL) {
                    wasThrottled = true
                }
                if (wasThrottled) {
                    executeRecoveryAction()
                    wasThrottled = false
                }
            }
        }

        _thermalState.value = newState

        _prediction.value = ThermalPrediction(
            currentTemp = sample.cpuTemp,
            kalmanTemp = kalmanCpu,
            emaTemp = emaCpuTemp,
            slope = slope,
            predictedTemp5Min = predictedTemp5Min,
            timeToThrottleSeconds = timeToThrottle,
            riskScore = riskScore,
            zScore = zScore,
            isAnomaly = isAnomaly,
            state = newState,
            gpuTemp = sample.gpuTemp,
            batteryTemp = sample.batteryTemp
        )
    }

    /**
     * Linear Regression Slope — Tính tốc độ tăng nhiệt (°C/phút)
     * Dùng least-squares fit: y = mx + b
     * Chỉ tính trên 5 samples gần nhất (15 giây) để phản hồi nhanh
     */
    private fun calculateSlope(): Float {
        val n = cpuHistory.size.coerceAtMost(10)
        if (n < 3) return 0f

        val firstTimestamp = timestampHistory[0]
        var sumX = 0.0
        var sumY = 0.0
        var sumXY = 0.0
        var sumX2 = 0.0

        for (i in (cpuHistory.size - n) until cpuHistory.size) {
            val x = (timestampHistory[i] - firstTimestamp) / 60000.0 // phút
            val y = cpuHistory[i].toDouble()
            sumX += x
            sumY += y
            sumXY += x * y
            sumX2 += x * x
        }

        val denominator = n * sumX2 - sumX * sumX
        return if (denominator != 0.0) {
            ((n * sumXY - sumX * sumY) / denominator).toFloat()
        } else 0f
    }

    /**
     * Z-Score: (x - μ) / σ
     * Phát hiện thermal spike bất thường
     */
    private fun calculateZScore(value: Float): Float {
        if (cpuHistory.size < 10) return 0f
        var sum = 0.0
        for (i in 0 until cpuHistory.size) {
            sum += cpuHistory[i].toDouble()
        }
        val mean = (sum / cpuHistory.size).toFloat()

        var varianceSum = 0.0
        for (i in 0 until cpuHistory.size) {
            val diff = cpuHistory[i] - mean
            varianceSum += (diff * diff).toDouble()
        }
        val stdDev = sqrt(varianceSum / cpuHistory.size).toFloat()

        return if (stdDev > 0.001f) (value - mean) / stdDev else 0f
    }

    /**
     * Fuzzy Logic — Kết hợp nhiều yếu tố
     * Output: Risk score 0.0 - 1.0
     */
    private fun calculateFuzzyRisk(
        temp: Float, slope: Float, predictedTemp: Float,
        ramAvailable: Float, batteryTemp: Float
    ): Float {
        // Membership functions
        val tempRisk = when {
            temp > 45 -> 1.0f
            temp > 42 -> (temp - 42) / 3
            temp > 38 -> (temp - 38) / 12
            else -> 0f
        }

        val slopeRisk = (slope.coerceIn(0f, 2f) / 2f)

        val predictionRisk = when {
            predictedTemp > 48 -> 1.0f
            predictedTemp > 42 -> (predictedTemp - 42) / 6
            else -> 0f
        }

        val ramRisk = ((30f - ramAvailable).coerceAtLeast(0f) / 30f)

        val batteryRisk = if (batteryTemp > 42) 1f else 0f

        // Weighted aggregation
        return (tempRisk * 0.35f +
                slopeRisk * 0.20f +
                predictionRisk * 0.25f +
                ramRisk * 0.15f +
                batteryRisk * 0.05f).coerceIn(0f, 1f)
    }

    /**
     * Đọc nhiệt độ từ thermal zones
     */
    private fun readThermalZone(type: String): Float? {
        try {
            val thermalDir = File("/sys/class/thermal")
            if (!thermalDir.exists()) return null

            thermalDir.listFiles()?.forEach { zone ->
                val name = File(zone, "type")
                if (name.exists() && name.readText().trim().contains(type, ignoreCase = true)) {
                    val temp = File(zone, "temp")
                    if (temp.exists()) {
                        val raw = temp.readText().trim().toFloatOrNull() ?: return@forEach
                        return raw / 1000f // Chia 1000 vì thermal zone đọc ở millidegrees
                    }
                }
            }
        } catch (e: Exception) { /* Fallback */ }
        return null
    }

    /**
     * Đọc nhiệt pin từ hệ thống
     */
    private fun readBatteryTemperature(): Float {
        return try {
            val manager = context.getSystemService(Context.BATTERY_SERVICE) as? android.os.BatteryManager
            val temp = manager?.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_TEMPERATURE)
            if (temp != null && temp > 0) temp / 10f else 0f
        } catch (e: Exception) { 0f }
    }

    /**
     * Đọc RAM info
     */
    private fun readRamInfo(): Pair<Float, Int> {
        return try {
            val memInfo = File("/proc/meminfo")
            if (!memInfo.exists()) return Pair(50f, 0)

            val lines = memInfo.readLines()
            var memTotal = 0L
            var memAvailable = 0L

            for (line in lines) {
                when {
                    line.startsWith("MemTotal:") -> {
                        memTotal = line.split("\\s+".toRegex()).getOrNull(1)?.toLongOrNull() ?: 0
                    }
                    line.startsWith("MemAvailable:") -> {
                        memAvailable = line.split("\\s+".toRegex()).getOrNull(1)?.toLongOrNull() ?: 0
                    }
                }
            }

            val percent = if (memTotal > 0) (memAvailable.toFloat() / memTotal.toFloat()) * 100f else 50f
            Pair(percent, (memTotal / 1024).toInt())
        } catch (e: Exception) { Pair(50f, 0) }
    }

    private suspend fun executeEmergencyAction() {
        shizukuManager.executeShellCommand(
            "settings put global window_animation_scale 0 && " +
            "settings put global transition_animation_scale 0 && " +
            "settings put global animator_duration_scale 0 && " +
            "settings put system screen_brightness 30 && " +
            "wm size 720x1280"
        )
    }

    private suspend fun executeWarningAction() {
        shizukuManager.executeShellCommand(
            "settings put global window_animation_scale 0.5 && " +
            "settings put global animator_duration_scale 0.5 && " +
            "settings put system screen_brightness 80"
        )
    }

    private suspend fun executeRecoveryAction() {
        shizukuManager.executeShellCommand(
            "wm size reset && " +
            "settings put system screen_brightness 128"
        )
    }
}

enum class ThermalState {
    NORMAL,
    WARNING,
    CRITICAL
}

data class ThermalPrediction(
    val currentTemp: Float = 0f,
    val kalmanTemp: Float = 0f,
    val emaTemp: Float = 0f,
    val slope: Float = 0f,
    val predictedTemp5Min: Float = 0f,
    val timeToThrottleSeconds: Int = Int.MAX_VALUE,
    val riskScore: Float = 0f,
    val zScore: Float = 0f,
    val isAnomaly: Boolean = false,
    val state: ThermalState = ThermalState.NORMAL,
    val gpuTemp: Float = 0f,
    val batteryTemp: Float = 0f
)

data class ThermalSample(
    val cpuTemp: Float,
    val gpuTemp: Float,
    val batteryTemp: Float,
    val ramAvailablePercent: Float,
    val ramTotalMB: Int,
    val timestamp: Long
)
