package com.nmdlock.app.data.repository

import com.nmdlock.app.core.security.DeviceIdManager
import com.nmdlock.app.data.local.DataStoreManager
import com.nmdlock.app.data.remote.api.LicenseApi
import com.nmdlock.app.data.remote.dto.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository handling license/key operations.
 * Coordinates between API, local cache, and device binding.
 */
@Singleton
class LicenseRepository @Inject constructor(
    private val licenseApi: LicenseApi,
    private val deviceIdManager: DeviceIdManager,
    private val dataStoreManager: DataStoreManager,
) {
    /**
     * Activate a license key for this device.
     */
    suspend fun activateLicense(keyValue: String): Result<ActivateLicenseResponse> {
        return try {
            val deviceInfo = deviceIdManager.getDeviceInfo()
            val response = licenseApi.activate(
                ActivateLicenseRequest(
                    keyValue = keyValue,
                    device = DeviceInfoRequest(
                        deviceId = deviceInfo.deviceId,
                        deviceName = deviceInfo.deviceName,
                        deviceModel = deviceInfo.deviceModel,
                        androidVersion = deviceInfo.androidVersion,
                    ),
                )
            )

            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()!!.data!!
                dataStoreManager.setCachedLicenseStatus("active")
                Result.success(data)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Activation failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Validate current license for this device.
     */
    suspend fun validateLicense(): Result<ValidateLicenseResponse> {
        return try {
            val deviceId = deviceIdManager.getDeviceId()
            val response = licenseApi.validate(
                ValidateLicenseRequest(deviceId = deviceId)
            )

            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()!!.data!!
                if (data.valid) {
                    dataStoreManager.setCachedLicenseStatus("active")
                } else {
                    dataStoreManager.setCachedLicenseStatus(data.reason ?: "inactive")
                }
                Result.success(data)
            } else {
                // Try cached status
                val cached = dataStoreManager.cachedLicenseStatus.first()
                Result.success(
                    ValidateLicenseResponse(
                        valid = cached == "active",
                        reason = if (cached != "active") "Cached: $cached" else null,
                    )
                )
            }
        } catch (e: Exception) {
            // Offline - use cached status
            val cached = dataStoreManager.cachedLicenseStatus.first()
            Result.success(
                ValidateLicenseResponse(
                    valid = cached == "active",
                    reason = "Offline mode",
                )
            )
        }
    }

    /**
     * Get current license info for display.
     */
    suspend fun getMyLicense(): Result<LicenseInfoResponse> {
        return try {
            val deviceId = deviceIdManager.getDeviceId()
            val response = licenseApi.getMyLicense(deviceId)

            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception("Failed to get license info"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get license activation history.
     */
    suspend fun getHistory(): Result<List<LicenseHistoryEntry>> {
        return try {
            val deviceId = deviceIdManager.getDeviceId()
            val response = licenseApi.getHistory(deviceId)

            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data?.items ?: emptyList())
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.success(emptyList())
        }
    }

    /**
     * Check if the current license is valid (uses cache if offline).
     */
    suspend fun isLicenseValid(): Boolean {
        val result = validateLicense()
        return result.getOrNull()?.valid == true
    }
}
