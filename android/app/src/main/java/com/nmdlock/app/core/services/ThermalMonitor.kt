package com.nmdlock.app.core.services

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
        NORMAL, WARM, HOT, CRITICAL
    }

    suspend fun getThermalInfo(): ThermalInfo = withContext(Dispatchers.IO) {
        try {
            val cmd1 = "for z in /sys/class/thermal/thermal_zone*/type; do echo \$z: \$(cat \$z); done"
            val zoneTypesResult = shizukuManager.executeCommand(cmd1).getOrNull() ?: ""

            val cmd2 = "for z in /sys/class/thermal/thermal_zone*/temp; do echo \$z: \$(cat \$z 2>/dev/null || echo 0); done"
            val zoneTempsResult = shizukuManager.executeCommand(cmd2).getOrNull() ?: ""

            val typeMap = mutableMapOf<String, String>()
            zoneTypesResult.lines().forEach { line ->
                val parts = line.split(": ")
                if (parts.size >= 2) {
                    typeMap[parts[0].trim()] = parts[1].trim()
                }
            }

            var cpuTemp: Float? = null
            var gpuTemp: Float? = null
            var maxTemp = 0f

            zoneTempsResult.lines().forEach { line ->
                val parts = line.split(": ")
                if (parts.size >= 2) {
                    val path = parts[0].removeSuffix("/temp").trim()
                    val tempC = parts[1].trim().toFloatOrNull()?.let { it / 1000f } ?: return@forEach

                    val zoneType = typeMap[path + "/type"] ?: ""
                    if (zoneType.contains("cpu")) {
                        if (cpuTemp == null || tempC > cpuTemp) cpuTemp = tempC
                    } else if (zoneType.contains("gpu")) {
                        if (gpuTemp == null || tempC > gpuTemp) gpuTemp = tempC
                    }
                    if (tempC > maxTemp) maxTemp = tempC
                }
            }

            val batteryTemp = try {
                val intent = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
                intent?.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, 0)?.let { it / 10f }
            } catch (e: Exception) { null }

            val level = when {
                maxTemp >= 75 -> ThrottleLevel.CRITICAL
                maxTemp >= 65 -> ThrottleLevel.HOT
                maxTemp >= 55 -> ThrottleLevel.WARM
                else -> ThrottleLevel.NORMAL
            }

            ThermalInfo(cpuTemp, gpuTemp, batteryTemp, maxTemp, level >= ThrottleLevel.HOT, level, getCoolingSuggestions(level))
        } catch (e: Exception) {
            ThermalInfo()
        }
    }

    private fun getCoolingSuggestions(level: ThrottleLevel): List<String> {
        return when (level) {
            ThrottleLevel.CRITICAL -> listOf("⚠️ NGUY HIỂM! Dừng game ngay", "Tắt máy 10 phút")
            ThrottleLevel.HOT -> listOf("🔥 Quá nóng! Giảm đồ họa", "Tắt app nền", "Tháo ốp lưng")
            ThrottleLevel.WARM -> listOf("🌡️ Đang ấm, giảm độ sáng")
            ThrottleLevel.NORMAL -> emptyList()
        }
    }

    fun getRecommendedFpsLimit(thermalInfo: ThermalInfo): Int {
        return when (thermalInfo.throttlingLevel) {
            ThrottleLevel.NORMAL -> 60
            ThrottleLevel.WARM -> 45
            ThrottleLevel.HOT -> 30
            ThrottleLevel.CRITICAL -> 20
        }
    }
}
