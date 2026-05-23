package com.nmdlock.app.data.remote.dto

/**
 * Activate license request.
 */
data class ActivateLicenseRequest(
    val key: String,
    val hwid: String,
    val device_model: String,
)

/**
 * Validate license request.
 */
data class ValidateLicenseRequest(
    val key: String,
    val hwid: String,
)

/**
 * License activation response.
 */
data class ActivateLicenseResponse(
    val activated: Boolean = false,
    val revalidated: Boolean = false,
    val license: LicenseData? = null,
    val device: DeviceInfoResponse? = null,
)

/**
 * License data from server.
 */
data class LicenseData(
    val keyValue: String? = null,
    val type: String? = null,
    val status: String? = null,
    val isPermanent: Boolean = false,
    val isTrial: Boolean = false,
    val maxDevices: Int = 1,
    val activationCount: Int = 0,
    val activatedAt: String? = null,
    val expiresAt: String? = null,
    val remainingMs: Long? = null,
    val remainingDays: Int? = null,
    val remainingHours: Int? = null,
    val deviceCount: Int = 0,
)

/**
 * License validation response.
 */
data class ValidateLicenseResponse(
    val valid: Boolean = false,
    val reason: String? = null,
    val license: LicenseData? = null,
    val device: DeviceInfoResponse? = null,
)

/**
 * License info response for GET /license/me.
 */
data class LicenseInfoResponse(
    val active: Boolean = false,
    val keyValue: String? = null,
    val type: String? = null,
    val isPermanent: Boolean = false,
    val status: String? = null,
    val maxDevices: Int = 1,
    val expiresAt: String? = null,
    val remainingMs: Long? = null,
    val remainingDays: Int? = null,
    val lastValidatedAt: String? = null,
    val message: String? = null,
)

/**
 * License history entry.
 */
data class LicenseHistoryEntry(
    val id: Int,
    val licenseId: Int,
    val keyValue: String? = null,
    val type: String? = null,
    val assignedAt: String? = null,
    val releasedAt: String? = null,
    val isActive: Boolean = false,
    val lastValidatedAt: String? = null,
)

/**
 * License history list response.
 */
data class LicenseHistoryResponse(
    val items: List<LicenseHistoryEntry> = emptyList(),
    val total: Int = 0,
)

/**
 * Server config response.
 */
data class ConfigResponse(
    val config: Map<String, Any>? = null,
    val timestamp: String? = null,
    val version: String? = null,
)
