package com.nmdlock.app.data.repository

import android.util.Log
import com.nmdlock.app.core.security.DeviceIdManager
import com.nmdlock.app.data.local.DataStoreManager
import com.nmdlock.app.data.remote.api.LicenseApi
import com.nmdlock.app.data.remote.api.ActivateLicenseRequest
import com.nmdlock.app.data.remote.api.ActivateLicenseResponse
import com.nmdlock.app.data.remote.api.CheckLicenseResponse
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository handling license/key operations.
 * Tích hợp với backend CloudFlare: https://border-late-dryer-indicate.trycloudflare.com/api/
 */
@Singleton
class LicenseRepository @Inject constructor(
    private val licenseApi: LicenseApi,
    private val deviceIdManager: DeviceIdManager,
    private val dataStoreManager: DataStoreManager,
) {
    private val TAG = "LicenseRepository"

    /**
     * Kích hoạt license key
     * Backend endpoint: POST /api/activate
     * Request body: { key, hwid, device_model }
     */
    suspend fun activateLicense(keyValue: String): Result<ActivateLicenseResponse> {
        return try {
            val deviceInfo = deviceIdManager.getDeviceInfo()
            
            Log.d(TAG, "Activating license key: $keyValue")
            Log.d(TAG, "Device ID: ${deviceInfo.deviceId}")
            Log.d(TAG, "Device Model: ${deviceInfo.deviceModel}")

            // Tạo request theo format backend
            val request = ActivateLicenseRequest(
                key = keyValue.trim().uppercase(),
                hwid = deviceInfo.deviceId,
                device_model = deviceInfo.deviceModel
            )

            val response = licenseApi.activateLicense(request)

            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                
                if (apiResponse.success && apiResponse.data != null) {
                    val data = apiResponse.data
                    Log.d(TAG, "License activated successfully: ${data.status}")
                    
                    // Lưu vào local cache
                    dataStoreManager.setFullLicenseCache(
                        status = data.status,
                        keyValue = keyValue,
                        type = "device_locked", // default type
                        expiresAt = data.expires_at,
                        maxDevices = 1,
                    )
                    
                    Result.success(data)
                } else {
                    val errorMsg = apiResponse.error ?: apiResponse.message ?: "Activation failed"
                    Log.e(TAG, "Activation failed: $errorMsg")
                    dataStoreManager.clearLicenseCache()
                    Result.failure(Exception(errorMsg))
                }
            } else {
                val httpMsg = "HTTP ${response.code()}: ${response.message()}"
                Log.e(TAG, "HTTP error: $httpMsg")
                dataStoreManager.clearLicenseCache()
                Result.failure(Exception(httpMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during activation", e)
            try {
                dataStoreManager.clearLicenseCache()
            } catch (clearEx: Exception) {
                Log.e(TAG, "Failed to clear cache", clearEx)
            }
            Result.failure(e)
        }
    }

    /**
     * Kiểm tra license key có hợp lệ không
     * Backend endpoint: GET /api/check?key=<key>
     */
    suspend fun checkLicense(keyValue: String): Result<CheckLicenseResponse> {
        return try {
            Log.d(TAG, "Checking license key: $keyValue")
            
            val response = licenseApi.checkLicense(keyValue.trim().uppercase())

            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                
                if (apiResponse.success && apiResponse.data != null) {
                    Log.d(TAG, "License check result: ${apiResponse.data.valid}")
                    Result.success(apiResponse.data)
                } else {
                    val errorMsg = apiResponse.error ?: "Check failed"
                    Log.e(TAG, "Check failed: $errorMsg")
                    Result.failure(Exception(errorMsg))
                }
            } else {
                val httpMsg = "HTTP ${response.code()}: ${response.message()}"
                Log.e(TAG, "HTTP error: $httpMsg")
                Result.failure(Exception(httpMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during check", e)
            Result.failure(e)
        }
    }

    /**
     * Lấy license info từ local cache
     */
    suspend fun getCachedLicense(): Result<LicenseCacheData> {
        return try {
            val cached = dataStoreManager.getLicenseCache()
            if (cached != null && cached.status == "active") {
                Result.success(cached)
            } else {
                Result.failure(Exception("No active license cached"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Clear local license cache
     */
    suspend fun clearLicense(): Result<Unit> {
        return try {
            dataStoreManager.clearLicenseCache()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Local cached license data
 */
data class LicenseCacheData(
    val status: String,
    val keyValue: String,
    val type: String,
    val expiresAt: String?,
    val maxDevices: Int,
)
