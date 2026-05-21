package com.nmdlock.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Generic API response wrapper.
 */
data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null,
    val errors: List<String>? = null,
    val timestamp: String? = null,
)

/**
 * Register request body.
 */
data class RegisterRequest(
    val username: String,
    val email: String? = null,
    val password: String,
)

/**
 * Login request body.
 */
data class LoginRequest(
    val username: String,
    val password: String,
    val device: DeviceInfoRequest? = null,
)

/**
 * Device info sent with auth requests.
 */
data class DeviceInfoRequest(
    val deviceId: String,
    val deviceName: String,
    val deviceModel: String,
    val androidVersion: String,
)

/**
 * Refresh token request.
 */
data class RefreshRequest(
    val refreshToken: String,
    val deviceId: String? = null,
)

/**
 * Auth response with tokens and user data.
 */
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int,
    val user: UserResponse? = null,
    val device: DeviceInfoResponse? = null,
)

/**
 * User info from server.
 */
data class UserResponse(
    val id: Int,
    val username: String,
    val role: String,
)

/**
 * Refresh token response.
 */
data class RefreshResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int,
)
