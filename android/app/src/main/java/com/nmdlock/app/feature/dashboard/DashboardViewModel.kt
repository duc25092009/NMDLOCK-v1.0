package com.nmdlock.app.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nmdlock.app.core.services.OptimizationEngine
import com.nmdlock.app.core.services.SystemInfoProvider
import com.nmdlock.app.data.repository.DeviceRepository
import com.nmdlock.app.data.repository.LicenseRepository
import com.nmdlock.app.domain.model.DeviceInfo
import com.nmdlock.app.domain.model.LicenseInfo
import com.nmdlock.app.domain.model.SystemStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Dashboard state holder.
 */
data class DashboardUiState(
    val isLoading: Boolean = true,
    val deviceInfo: DeviceInfo? = null,
    val licenseInfo: LicenseInfo? = null,
    val systemStats: SystemStats? = null,
    val alerts: List<String> = emptyList(),
    val error: String? = null,
    val optimizationResult: String? = null,
)

/**
 * ViewModel for the Dashboard screen.
 * Manages REAL system stats, device info, and license status.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val licenseRepository: LicenseRepository,
    private val systemInfoProvider: SystemInfoProvider,
    private val optimizationEngine: OptimizationEngine,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // Load REAL system stats
                val realStats = systemInfoProvider.getSystemStats()

                // Load device info
                val deviceResult = deviceRepository.getDeviceStatus()
                val deviceInfo = deviceResult.getOrNull()?.let {
                    DeviceInfo(
                        deviceId = it.deviceId ?: "",
                        deviceName = it.deviceName ?: "",
                        deviceModel = it.deviceModel ?: "",
                        androidVersion = it.androidVersion ?: "",
                        firstActivationAt = it.firstActivationAt,
                        lastSeenAt = it.lastSeenAt,
                        isLocked = it.isLocked,
                        lockReason = it.lockReason,
                        verifiedCount = it.verifiedCount,
                        isRegistered = it.registered,
                    )
                }

                // Load license info
                val licenseResult = licenseRepository.getMyLicense()
                val licenseInfo = licenseResult.getOrNull()?.let {
                    LicenseInfo(
                        keyValue = it.keyValue,
                        type = it.type,
                        status = it.status,
                        isPermanent = it.isPermanent,
                        expiresAt = it.expiresAt,
                        remainingDays = it.remainingDays,
                        remainingHours = null,
                        isValid = it.active,
                        message = it.message,
                    )
                }

                // Generate real alerts
                val alerts = mutableListOf<String>()
                if (licenseInfo?.isValid == false) {
                    alerts.add("Key chưa được kích hoạt hoặc đã hết hạn")
                }
                if (deviceInfo?.isLocked == true) {
                    alerts.add("Thiết bị đã bị khóa: ${deviceInfo.lockReason ?: "Không rõ lý do"}")
                }
                if (realStats.batteryPercent <= 20) {
                    alerts.add("Pin yếu (${realStats.batteryPercent}%), hãy sạc thiết bị")
                }
                if (realStats.storageUsedPercent > 85) {
                    alerts.add("Bộ nhớ gần đầy (${realStats.storageUsedPercent.toInt()}%), dọn dẹp để giải phóng không gian")
                }

                _uiState.value = DashboardUiState(
                    isLoading = false,
                    deviceInfo = deviceInfo,
                    licenseInfo = licenseInfo,
                    systemStats = realStats,
                    alerts = alerts,
                )
            } catch (e: Exception) {
                _uiState.value = DashboardUiState(
                    isLoading = false,
                    error = e.message ?: "Failed to load dashboard",
                    systemStats = SystemStats(),
                )
            }
        }
    }

    fun performQuickOptimize() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, optimizationResult = null)
            val result = optimizationEngine.quickOptimize()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                optimizationResult = result.message,
                systemStats = systemInfoProvider.getSystemStats(), // Refresh stats
            )
        }
    }

    fun refreshStats() {
        viewModelScope.launch {
            val realStats = systemInfoProvider.getSystemStats()
            _uiState.value = _uiState.value.copy(systemStats = realStats)
        }
    }
}
