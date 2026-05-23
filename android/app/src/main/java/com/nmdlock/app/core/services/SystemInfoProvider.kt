package com.nmdlock.app.core.services

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.StatFs
import com.nmdlock.app.domain.model.SystemStats
import java.io.File
import javax.inject.Singleton

/**
 * Provides real system information by reading /proc files and using Android APIs.
 * Uses Shizuku for elevated access where needed, falls back to standard APIs.
 */
@Singleton
class SystemInfoProvider(
    private val context: Context,
    private val shizukuManager: ShizukuManager,
) {

    /**
     * Get comprehensive system stats with real data.
     */
    suspend fun getSystemStats(): SystemStats {
        val ramInfo = getRamInfo()
        val storageInfo = getStorageInfo()
        val batteryInfo = getBatteryInfo()
        val cpuInfo = getCpuUsage()
        val networkInfo = getNetworkInfo()

        return SystemStats(
            ramUsedPercent = ramInfo.usedPercent,
            ramTotalMB = ramInfo.totalMB,
            ramUsedMB = ramInfo.usedMB,
            storageUsedPercent = storageInfo.usedPercent,
            storageTotalGB = storageInfo.totalGB,
            storageUsedGB = storageInfo.usedGB,
            batteryPercent = batteryInfo.level,
            batteryTemp = batteryInfo.temperature,
            cpuUsage = cpuInfo,
            networkType = networkInfo.type,
            networkStrength = networkInfo.strength,
        )
    }

    /**
     * Get RAM info from /proc/meminfo.
     */
    data class RamInfo(val totalMB: Long, val usedMB: Long, val usedPercent: Float)

    suspend fun getRamInfo(): RamInfo {
        return try {
            val memInfo = shizukuManager.executeCommand("cat /proc/meminfo").getOrNull() ?: ""
            val totalKb = parseMemValue(memInfo, "MemTotal")
            val freeKb = parseMemValue(memInfo, "MemFree")
            val buffersKb = parseMemValue(memInfo, "Buffers")
            val cachedKb = parseMemValue(memInfo, "Cached")

            val usedKb = totalKb - freeKb - buffersKb - cachedKb
            val totalMB = totalKb / 1024
            val usedMB = usedKb / 1024
            val percent = if (totalKb > 0) (usedKb.toFloat() / totalKb * 100) else 0f

            RamInfo(totalMB = totalMB, usedMB = usedMB, usedPercent = percent)
        } catch (e: Exception) {
            // Fallback to ActivityManager
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val mi = ActivityManager.MemoryInfo()
            am?.getMemoryInfo(mi)
            val totalMB = mi.totalMem / (1024 * 1024)
            val availMB = mi.availMem / (1024 * 1024)
            val usedMB = totalMB - availMB
            RamInfo(totalMB = totalMB, usedMB = usedMB, usedPercent = if (totalMB > 0) usedMB.toFloat() / totalMB * 100 else 0f)
        }
    }

    /**
     * Get storage info from StatFs and df.
     */
    data class StorageInfo(val totalGB: Long, val usedGB: Long, val usedPercent: Float)

    suspend fun getStorageInfo(): StorageInfo {
        return try {
            // Try using df command via Shizuku first
            val dfOutput = shizukuManager.executeCommand("df /data").getOrNull() ?: ""
            if (dfOutput.isNotBlank()) {
                val lines = dfOutput.lines()
                for (line in lines) {
                    if (line.contains("/data")) {
                        val parts = line.split("\\s+".toRegex()).filter { it.isNotBlank() }
                        if (parts.size >= 4) {
                            val totalKb = parts.getOrNull(1)?.toLongOrNull() ?: 0
                            val usedKb = parts.getOrNull(2)?.toLongOrNull() ?: 0
                            val totalGB = totalKb / (1024 * 1024)
                            val usedGB = usedKb / (1024 * 1024)
                            val percent = if (totalKb > 0) (usedKb.toFloat() / totalKb * 100) else 0f
                            return StorageInfo(totalGB = totalGB, usedGB = usedGB, usedPercent = percent)
                        }
                    }
                }
            }

            // Fallback to StatFs
            val path = File(context.filesDir.absolutePath)
            while (!path.exists()) { path.parentFile ?: break }
            val stat = StatFs(path.absolutePath)
            val totalBytes = stat.totalBytes
            val freeBytes = stat.availableBytes
            val usedBytes = totalBytes - freeBytes
            val totalGB = totalBytes / (1024 * 1024 * 1024)
            val usedGB = usedBytes / (1024 * 1024 * 1024)
            val percent = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes * 100) else 0f

            StorageInfo(totalGB = totalGB, usedGB = usedGB, usedPercent = percent)
        } catch (e: Exception) {
            StorageInfo(totalGB = 0, usedGB = 0, usedPercent = 0f)
        }
    }

    /**
     * Get battery info from BatteryManager.
     */
    data class BatteryInfo(val level: Int, val temperature: Float, val isCharging: Boolean, val health: String)

    suspend fun getBatteryInfo(): BatteryInfo {
        return try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            if (intent != null) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                val tempC = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
                val healthCode = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
                val health = when (healthCode) {
                    BatteryManager.BATTERY_HEALTH_GOOD -> "Tốt"
                    BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Nóng"
                    BatteryManager.BATTERY_HEALTH_DEAD -> "Hỏng"
                    BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Quá áp"
                    BatteryManager.BATTERY_HEALTH_COLD -> "Lạnh"
                    else -> "Bình thường"
                }
                BatteryInfo(level = level * 100 / scale, temperature = tempC, isCharging = isCharging, health = health)
            } else {
                BatteryInfo(level = 0, temperature = 0f, isCharging = false, health = "Unknown")
            }
        } catch (e: Exception) {
            BatteryInfo(level = 0, temperature = 0f, isCharging = false, health = "Unknown")
        }
    }

    /**
     * Get CPU usage percentage by reading /proc/stat.
     */
    suspend fun getCpuUsage(): Float {
        return try {
            val statOutput = shizukuManager.executeCommand("cat /proc/stat").getOrNull() ?: ""
            val lines = statOutput.lines().filter { it.startsWith("cpu") }

            var totalUsage = 0f
            var coreCount = 0

            for (line in lines) {
                if (line.length < 4 || line[3] !in '0'..'9') continue // skip "cpu" total line
                val parts = line.split("\\s+".toRegex()).filter { it.isNotBlank() }
                if (parts.size < 5) continue

                val user = parts.getOrNull(1)?.toLongOrNull() ?: 0
                val nice = parts.getOrNull(2)?.toLongOrNull() ?: 0
                val system = parts.getOrNull(3)?.toLongOrNull() ?: 0
                val idle = parts.getOrNull(4)?.toLongOrNull() ?: 0

                val total = user + nice + system + idle
                val active = user + nice + system

                if (total > 0) {
                    totalUsage += (active.toFloat() / total * 100)
                    coreCount++
                }
            }

            if (coreCount > 0) totalUsage / coreCount else 0f
        } catch (e: Exception) {
            0f
        }
    }

    /**
     * Get network info.
     */
    data class NetworkInfo(val type: String, val strength: String)

    suspend fun getNetworkInfo(): NetworkInfo {
        return try {
            val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            val network = connManager?.activeNetwork
            val caps = network?.let { connManager.getNetworkCapabilities(it) }

            val type = when {
                caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) == true -> "WiFi"
                caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "4G/5G"
                caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Có dây"
                else -> "Không có mạng"
            }

            val strength = when {
                type == "WiFi" -> {
                    val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
                    val rssi = wifiManager?.connectionInfo?.rssi ?: -100
                    when {
                        rssi >= -50 -> "Rất tốt"
                        rssi >= -70 -> "Tốt"
                        rssi >= -85 -> "Trung bình"
                        else -> "Yếu"
                    }
                }
                type == "4G/5G" -> {
                    caps?.linkUpstreamBandwidthKbps?.let {
                        when {
                            it >= 50000 -> "Rất tốt"
                            it >= 10000 -> "Tốt"
                            it >= 1000 -> "Trung bình"
                            else -> "Yếu"
                        }
                    } ?: "Không rõ"
                }
                else -> "—"
            }

            NetworkInfo(type = type, strength = strength)
        } catch (e: Exception) {
            NetworkInfo(type = "Unknown", strength = "Unknown")
        }
    }

    /**
     * Get CPU temperature (if available).
     */
    suspend fun getCpuTemperature(): Float? {
        return try {
            val paths = listOf(
                "/sys/class/thermal/thermal_zone0/temp",
                "/sys/class/thermal/thermal_zone1/temp",
                "/sys/class/thermal/thermal_zone2/temp",
                "/sys/class/thermal/thermal_zone3/temp",
            )
            for (path in paths) {
                val result = shizukuManager.executeCommand("cat $path").getOrNull() ?: ""
                val temp = result.trim().toFloatOrNull()
                if (temp != null && temp > 0) {
                    return temp / 1000f // Convert millidegrees to degrees
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get running apps list (third-party).
     */
    suspend fun getRunningApps(): List<RunningAppInfo> {
        return try {
            val output = shizukuManager.executeCommand("pm list packages -3").getOrNull() ?: ""
            output.lines()
                .mapNotNull { it.removePrefix("package:").trim().takeIf { pkg -> pkg.isNotBlank() } }
                .mapNotNull { pkg ->
                    try {
                        val appName = getAppNameFromPackage(pkg)
                        RunningAppInfo(packageName = pkg, appName = appName)
                    } catch (e: Exception) { null }
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    data class RunningAppInfo(val packageName: String, val appName: String)

    private fun getAppNameFromPackage(packageName: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    private fun parseMemValue(memInfo: String, key: String): Long {
        val regex = Regex("$key:\\s+(\\d+)")
        return regex.find(memInfo)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
    }
}
