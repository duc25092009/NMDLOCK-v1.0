package com.nmdlock.app.data.remote.api

import com.nmdlock.app.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * License management API endpoints.
 */
interface LicenseApi {

    @POST("license/activate")
    suspend fun activate(
        @Body request: ActivateLicenseRequest
    ): Response<ApiResponse<ActivateLicenseResponse>>

    @POST("license/validate")
    suspend fun validate(
        @Body request: ValidateLicenseRequest
    ): Response<ApiResponse<ValidateLicenseResponse>>

    @POST("license/redeem")
    suspend fun redeem(
        @Body request: ActivateLicenseRequest
    ): Response<ApiResponse<ActivateLicenseResponse>>

    @GET("license/me")
    suspend fun getMyLicense(
        @Query("deviceId") deviceId: String
    ): Response<ApiResponse<LicenseInfoResponse>>

    @GET("license/history")
    suspend fun getHistory(
        @Query("deviceId") deviceId: String
    ): Response<ApiResponse<LicenseHistoryResponse>>
}
