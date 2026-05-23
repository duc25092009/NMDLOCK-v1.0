package com.nmdlock.app.data.repository

import com.nmdlock.app.core.di.AuthTokenHolder
import com.nmdlock.app.core.security.DeviceIdManager
import com.nmdlock.app.data.local.DataStoreManager
import com.nmdlock.app.data.remote.api.AuthApi
import com.nmdlock.app.data.remote.dto.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository handling authentication operations.
 * Manages tokens and coordinates between API, DataStore, and memory.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val dataStoreManager: DataStoreManager,
    private val deviceIdManager: DeviceIdManager,
) {
    /**
     * Login with username/password and bind device.
     */
    suspend fun login(username: String, password: String): Result<AuthResponse> {
        return try {
            val deviceInfo = deviceIdManager.getDeviceInfo()
            val response = authApi.login(
                LoginRequest(
                    username = username,
                    password = password,
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
                // Store tokens
                AuthTokenHolder.token = data.accessToken
                AuthTokenHolder.refreshToken = data.refreshToken
                dataStoreManager.saveTokens(data.accessToken, data.refreshToken)
                Result.success(data)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Refresh the access token using stored refresh token.
     */
    suspend fun refreshToken(): Result<RefreshResponse> {
        return try {
            val refreshToken = dataStoreManager.refreshToken.first() ?: return Result.failure(Exception("No refresh token"))

            val response = authApi.refreshToken(
                RefreshRequest(
                    refreshToken = refreshToken,
                    deviceId = deviceIdManager.getDeviceId(),
                )
            )

            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()!!.data!!
                AuthTokenHolder.token = data.accessToken
                AuthTokenHolder.refreshToken = data.refreshToken
                dataStoreManager.saveTokens(data.accessToken, data.refreshToken)
                Result.success(data)
            } else {
                // Clear invalid tokens
                clearTokens()
                Result.failure(Exception("Token refresh failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Logout and clear all stored tokens.
     */
    suspend fun logout() {
        try {
            authApi.logout()
        } catch (_: Exception) { }
        clearTokens()
    }

    /**
     * Check if user has valid stored tokens.
     */
    suspend fun isLoggedIn(): Boolean {
        val token = dataStoreManager.accessToken.first()
        return token != null
    }

    /**
     * Restore token from DataStore into memory on app start.
     */
    suspend fun restoreSession() {
        val access = dataStoreManager.accessToken.first()
        val refresh = dataStoreManager.refreshToken.first()
        if (access != null) {
            AuthTokenHolder.token = access
            AuthTokenHolder.refreshToken = refresh
        }
    }

    private suspend fun clearTokens() {
        AuthTokenHolder.token = null
        AuthTokenHolder.refreshToken = null
        dataStoreManager.clearTokens()
    }
}
