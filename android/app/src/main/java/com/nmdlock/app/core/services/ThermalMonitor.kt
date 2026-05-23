package com.nmdlock.app.core.services

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Monitors CPU/GPU temperature in real-time via thermal zones.
 * Detects thermal throttling and suggests cooling actions.
 */
@Singleton
class ThermalMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shizukuManager: ShizukuManager,
) {

    data class ThermalInfo(
        val cpuTemp: Float? = null,
        val gpuTemp: Float? = null,
        val batteryTemp: Float? = null,
        val maxTemp: Float = 0f,
        val isThrottling: Boolean = false,
        val throttlingLevel: ThrottleLevel = ThrottleLevel.NORMAL,
        val coolingSuggestions: List<String> = emptyList(),
    )

    enum class ThrottleLevel {
        NORMAL,      // Under 55°C
        WARM,        // 55-65°C
        HOT,         // 65-75°C
        CRITICAL,    // 75°C+
    }

    /**
     * Get real-time temperature from all thermal zones.
     * Parses /sys/class/thermal/thermal_zone*/temp
     */
    suspend fun getThermalInfo(): ThermalInfo = withContext(Dispatchers.IO) {
        try {
            // Sử dụng dấu nháy đơn '' bọc ngoài lệnh shell để Kotlin không bị lỗi parse dấu $ và \
            val zoneTypesResult = shizukuManager.executeCommand(
                "for z in /sys/class/thermal/thermal_zone*/type; do echo '\$z: \$(cat \$z)'; done"
            ).getOrNull() ?: ""

            val zoneTempsResult = shizukuManager.executeCommand(
                "for z in /sys/class/thermal/thermal_zone*/temp; do echo '\$z: \$(cat \$z 2>/dev/null || echo 0)'; done"
            ).getOrNull() ?: ""

            // Parse types
            val typeMap = mutableMapOf<String, String>()
            zoneTypesResult.lines().forEach { line ->
                val parts = line.split(": ")
                if (parts.size >= 2) {
                    val path = parts[0].trim()
                    val type = parts[1].trim()
                    typeMap[path] = type
                }
            }

            // Parse temperatures
            var cpuTemp: Float? = null
            var gpuTemp: Float? = null
            var maxTemp = 0f

            zoneTempsResult.lines().forEach { line ->
                val parts = line.split(": ")
                if (parts.size >= 2) {
                    val path = parts[0].removeSuffix("/temp").trim()
                    val tempStr = parts[1].trim()
                    val tempC = tempStr.toFloatOrNull()?.let { it / 1000f } ?: return@forEach

                    val zoneType = typeMap[path + "/type"] ?: ""
                    when {
                        zoneType.contains("cpu") -> {
                            if (cpuTemp == null || tempC > cpuTemp) cpuTemp = tempC
                        }
                        zoneType.contains("gpu") -> {
                            if (gpuTemp == null || tempC > gpuTemp) gpuTemp = tempC
                        }
                    }
                    if (tempC > maxTemp) maxTemp = tempC
                }
            }

            // Get battery temp from SystemInfoProvider
            val batteryTemp = try {
                val intent = context.registerReceiver(
                    null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
                )
                intent?.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, 0)?.let { it / 10f }
            } catch (e: Exception) { null }

            // Determine throttle level
            val level = when {
                maxTemp >= 75 -> ThrottleLevel.CRITICAL
                maxTemp >= 65 -> ThrottleLevel.HOT
                maxTemp >= 55 -> ThrottleLevel.WARM
                else -> ThrottleLevel.NORMAL
            }

            val suggestions = getCoolingSuggestions(level, cpuTemp, gpuTemp)

            ThermalInfo(
                cpuTemp = cpuTemp,
                gpuTemp = gpuTemp,
                batteryTemp = batteryTemp,
                maxTemp = maxTemp,
                isThrottling = level >= ThrottleLevel.HOT,
                throttlingLevel = level,
                coolingSuggestions = suggestions,
            )
        } catch (e: Exception) {
            ThermalInfo()
        }
    }

    private fun getCoolingSuggestions(
        level: ThrottleLevel,
        cpuTemp: Float?,
        gpuTemp: Float?,
    ): List<String> {
        val suggestions = mutableListOf<String>()
        when (level) {
            ThrottleLevel.CRITICAL -> {
                suggestions.add("⚠️ NHIỆT ĐỘ NGUY HIỂM! Dừng chơi game ngay!")
                suggestions.add("Tắt máy và để nguội 10-15 phút")
                suggestions.add("Kiểm tra quạt tản nhiệt / thông gió")
                suggestions.add("Giảm độ sáng màn hình xuống mức thấp nhất")
            }
            ThrottleLevel.HOT -> {
                suggestions.add("🔥 Máy đang quá nóng, hiệu năng sẽ giảm!")
                suggestions.add("Giảm chất lượng đồ họa xuống mức thấp hơn")
                suggestions.add("Tắt các ứng dụng chạy nền")
                suggestions.add("Giảm độ sáng màn hình")
                suggestions.add("Tháo ốp lưng để tản nhiệt tốt hơn")
            }
            ThrottleLevel.WARM -> {
                suggestions.add("🌡️ Nhiệt độ đang tăng, chuẩn bị hạ nhiệt")
                suggestions.add("Giảm độ sáng nếu cảm thấy máy nóng")
                suggestions.add("Đóng các tab trình duyệt không dùng đến")
            }
            ThrottleLevel.NORMAL -> { }
        }
        return suggestions
    }

    /**
     * Get FPS limit recommendation based on temperature.
     */
    fun getRecommendedFpsLimit(thermalInfo: ThermalInfo): Int {
        return when (thermalInfo.throttlingLevel) {
            ThrottleLevel.NORMAL -> 60
            ThrottleLevel.WARM -> 45
            ThrottleLevel.HOT -> 30
            ThrottleLevel.CRITICAL -> 20
        }
    }
}
