package com.nmdlock.app.data.remote.api

import com.nmdlock.app.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Device management API endpoints.
 */
interface DeviceApi {

    @POST("device/register")
    suspend fun register(
        @Body request: DeviceRegisterRequest
    ): Response<ApiResponse<DeviceStatusResponse>>

    @GET("device/status")
    suspend fun getStatus(
        @Query("deviceId") deviceId: String
    ): Response<ApiResponse<DeviceStatusResponse>>

    @GET("device/history")
    suspend fun getHistory(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<ApiResponse<DeviceHistoryResponse>>
}
