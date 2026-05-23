package com.nmdlock.app.data.remote.dto

/**
 * Device registration request.
 */
data class DeviceRegisterRequest(
    val deviceId: String,
    val deviceName: String,
    val deviceModel: String,
    val androidVersion: String,
)

/**
 * Device status response.
 */
data class DeviceStatusResponse(
    val registered: Boolean = false,
    val deviceId: String? = null,
    val deviceName: String? = null,
    val deviceModel: String? = null,
    val androidVersion: String? = null,
    val firstActivationAt: String? = null,
    val lastSeenAt: String? = null,
    val isLocked: Boolean = false,
    val lockReason: String? = null,
    val verifiedCount: Int = 0,
    val status: String? = null,
)

/**
 * Device info from auth response.
 */
data class DeviceInfoResponse(
    val id: String? = null,
    val name: String? = null,
    val model: String? = null,
    val isLocked: Boolean = false,
    val deviceId: String? = null,
    val deviceName: String? = null,
    val deviceModel: String? = null,
    val androidVersion: String? = null,
    val firstActivationAt: String? = null,
    val lastSeenAt: String? = null,
    val isLocked_device: Boolean? = null,
    val lockReason: String? = null,
    val verifiedCount: Int? = null,
)

/**
 * Audit log entry for device history.
 */
data class AuditLogEntry(
    val id: Int,
    val action: String,
    val entityType: String? = null,
    val deviceId: String? = null,
    val details: String? = null,
    val severity: String? = null,
    val created_at: String? = null,
)

/**
 * Device history list response.
 */
data class DeviceHistoryResponse(
    val items: List<AuditLogEntry> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val limit: Int = 20,
)
