package com.nmdlock.app.domain.model

import com.nmdlock.app.data.remote.dto.*

/**
 * Domain model for device information.
 */
data class DeviceInfo(
    val deviceId: String,
    val deviceName: String,
    val deviceModel: String,
    val androidVersion: String,
    val firstActivationAt: String? = null,
    val lastSeenAt: String? = null,
    val isLocked: Boolean = false,
    val lockReason: String? = null,
    val verifiedCount: Int = 0,
    val isRegistered: Boolean = false,
)

/**
 * Domain model for license information.
 */
data class LicenseInfo(
    val keyValue: String? = null,
    val type: String? = null,
    val status: String? = null,
    val isPermanent: Boolean = false,
    val isTrial: Boolean = false,
    val maxDevices: Int = 1,
    val expiresAt: String? = null,
    val remainingDays: Int? = null,
    val remainingHours: Int? = null,
    val isValid: Boolean = false,
    val message: String? = null,
)

/**
 * Optimization profile types.
 */
enum class OptimizationProfile(val displayName: String, val description: String) {
    BALANCED("Cân bằng", "Hiệu năng ổn định, tiết kiệm pin"),
    PERFORMANCE("Hiệu năng", "Ưu tiên tốc độ tối đa"),
    POWER_SAVE("Tiết kiệm pin", "Kéo dài thời lượng pin"),
    GAME("Chơi game", "Tối ưu cho trải nghiệm chơi game"),
}

/**
 * Game profile settings.
 */
data class GameProfile(
    val name: String,
    val description: String,
    val resolution: String = "Auto",
    val fpsTarget: Int = 60,
    val graphicsQuality: String = "Medium",
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val brightnessMode: String = "Auto",
)

/**
 * Network test results.
 */
data class NetworkTestResult(
    val pingMs: Long = 0,
    val downloadSpeed: Double = 0.0,
    val uploadSpeed: Double = 0.0,
    val jitterMs: Long = 0,
    val packetLoss: Double = 0.0,
    val isStable: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
)

/**
 * System stats from device.
 */
data class SystemStats(
    val ramUsedPercent: Float = 0f,
    val ramTotalMB: Long = 0,
    val ramUsedMB: Long = 0,
    val storageUsedPercent: Float = 0f,
    val storageTotalGB: Long = 0,
    val storageUsedGB: Long = 0,
    val batteryPercent: Int = 0,
    val batteryTemp: Float = 0f,
    val cpuUsage: Float = 0f,
    val networkType: String = "Unknown",
    val networkStrength: String = "Unknown",
)
