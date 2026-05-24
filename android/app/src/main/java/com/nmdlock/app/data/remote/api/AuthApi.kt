package com.nmdlock.app.data.remote.api

import com.nmdlock.app.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Authentication API endpoints.
 */
interface AuthApi {

    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<ApiResponse<AuthResponse>>

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<ApiResponse<AuthResponse>>

    @POST("auth/refresh")
    suspend fun refreshToken(
        @Body request: RefreshRequest
    ): Response<ApiResponse<RefreshResponse>>

    @POST("auth/logout")
    suspend fun logout(): Response<ApiResponse<Unit>>
}
