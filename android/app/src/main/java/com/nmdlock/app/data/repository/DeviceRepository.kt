package com.nmdlock.app.data.repository

import com.nmdlock.app.core.security.DeviceIdManager
import com.nmdlock.app.data.remote.api.DeviceApi
import com.nmdlock.app.data.remote.dto.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository handling device registration and status.
 */
@Singleton
class DeviceRepository @Inject constructor(
    private val deviceApi: DeviceApi,
    private val deviceIdManager: DeviceIdManager,
) {
    /**
     * Register or sync device with server.
     */
    suspend fun registerDevice(): Result<DeviceStatusResponse> {
        return try {
            val info = deviceIdManager.getDeviceInfo()
            val response = deviceApi.register(
                DeviceRegisterRequest(
                    deviceId = info.deviceId,
                    deviceName = info.deviceName,
                    deviceModel = info.deviceModel,
                    androidVersion = info.androidVersion,
                )
            )

            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Registration failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get device status from server.
     */
    suspend fun getDeviceStatus(): Result<DeviceStatusResponse> {
        return try {
            val deviceId = deviceIdManager.getDeviceId()
            val response = deviceApi.getStatus(deviceId)

            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception("Failed to get device status"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get device history.
     */
    suspend fun getHistory(page: Int = 1, limit: Int = 20): Result<DeviceHistoryResponse> {
        return try {
            val response = deviceApi.getHistory(page, limit)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.success(DeviceHistoryResponse())
            }
        } catch (e: Exception) {
            Result.success(DeviceHistoryResponse())
        }
    }

    /**
     * Get the device's unique ID.
     */
    fun getDeviceId(): String = deviceIdManager.getDeviceId()
}
