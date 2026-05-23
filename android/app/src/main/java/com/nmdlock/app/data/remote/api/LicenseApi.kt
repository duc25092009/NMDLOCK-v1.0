package com.nmdlock.app.data.remote.api

import com.nmdlock.app.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface LicenseApi {

    @POST("/api/activate")
    suspend fun activate(
        @Body request: ActivateLicenseRequest
    ): Response<ApiResponse<ActivateLicenseResponse>>

    @POST("/api/check")
    suspend fun validate(
        @Body request: ValidateLicenseRequest
    ): Response<ApiResponse<ValidateLicenseResponse>>

    @POST("/api/activate")
    suspend fun redeem(
        @Body request: ActivateLicenseRequest
    ): Response<ApiResponse<ActivateLicenseResponse>>

    @GET("/api/health")
    suspend fun health(): Response<Map<String, Any>>
}
