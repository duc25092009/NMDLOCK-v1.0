package com.nmdlock.app.data.remote.api

import com.nmdlock.app.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * License activation & validation API endpoints.
 * Backend: https://border-late-dryer-indicate.trycloudflare.com/api/
 */
interface LicenseApi {

    /**
     * Kích hoạt license key
     * POST /api/activate
     */
    @POST("activate")
    suspend fun activateLicense(
        @Body request: ActivateLicenseRequest
    ): Response<ApiResponse<ActivateLicenseResponse>>

    /**
     * Kiểm tra key có hợp lệ không
     * GET /api/check (theo backend của bạn)
     */
    @GET("check")
    suspend fun checkLicense(
        @Query("key") key: String
    ): Response<ApiResponse<CheckLicenseResponse>>
}

/**
 * Request body cho activate endpoint
 * Phải match với backend: key, hwid, device_model
 */
data class ActivateLicenseRequest(
    val key: String,        // License key
    val hwid: String,       // Hardware ID (Device ID)
    val device_model: String // Device model name
)

/**
 * Response từ activate endpoint
 */
data class ActivateLicenseResponse(
    val key: String,
    val status: String,     // "active", "banned", "expired", etc
    val expires_at: String,
    val message: String? = null
)

/**
 * Response từ check endpoint
 */
data class CheckLicenseResponse(
    val success: Boolean,
    val valid: Boolean,
    val message: String? = null
)

/**
 * Generic API response wrapper
 */
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null,
    val message: String? = null
)
