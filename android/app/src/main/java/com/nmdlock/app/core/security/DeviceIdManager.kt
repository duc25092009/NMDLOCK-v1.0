package com.nmdlock.app.core.security

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages a persistent, unique Device ID for this device.
 * Uses Android ID as base with fallback to generated UUID.
 * ID is stored encrypted via EncryptedSharedPreferences.
 */
@Singleton
class DeviceIdManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "nmdlock_device_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Returns the persistent Device ID, generating one if needed.
     * Based on Android ID (SSAID) with UUID fallback.
     */
    fun getDeviceId(): String {
        val storedId = prefs.getString(KEY_DEVICE_ID, null)
        if (storedId != null) return storedId

        val deviceId = generateDeviceId()
        prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        return deviceId
    }

    /**
     * Returns device information bundle for API calls.
     */
    fun getDeviceInfo(): DeviceInfo {
        return DeviceInfo(
            deviceId = getDeviceId(),
            deviceName = "${Build.MANUFACTURER} ${Build.MODEL}",
            deviceModel = Build.MODEL,
            androidVersion = Build.VERSION.RELEASE,
        )
    }

    /**
     * Generates a device ID from Android ID with UUID fallback.
     */
    private fun generateDeviceId(): String {
        val androidId = try {
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )
        } catch (e: Exception) {
            null
        }

        return if (!androidId.isNullOrBlank() && androidId != "9774d56d682e549c") {
            "NMD-${androidId}-${Build.BOARD.hashCode().toString(16)}"
        } else {
            // Fallback: UUID based on hardware identifiers
            val unique = UUID.nameUUIDFromBytes(
                "${Build.MANUFACTURER}|${Build.MODEL}|${Build.BOARD}|${Build.HARDWARE}|${Build.FINGERPRINT}"
                    .toByteArray()
            )
            "NMD-${unique.toString().substring(0, 16).uppercase()}"
        }
    }

    data class DeviceInfo(
        val deviceId: String,
        val deviceName: String,
        val deviceModel: String,
        val androidVersion: String,
    )

    companion object {
        private const val KEY_DEVICE_ID = "persistent_device_id"
    }
}
