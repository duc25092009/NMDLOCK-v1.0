package com.nmdlock.app.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nmdlock.app.data.repository.DeviceRepository
import com.nmdlock.app.data.repository.LicenseRepository
import com.nmdlock.app.domain.model.DeviceInfo
import com.nmdlock.app.domain.model.LicenseInfo
import com.nmdlock.app.domain.model.SystemStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
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
)

/**
 * ViewModel for the Dashboard screen.
 * Manages system stats, device info, and license status.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val licenseRepository: LicenseRepository,
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

                // Generate alerts
                val alerts = mutableListOf<String>()
                if (licenseInfo?.isValid == false) {
                    alerts.add("Key chưa được kích hoạt hoặc đã hết hạn")
                }
                if (deviceInfo?.isLocked == true) {
                    alerts.add("Thiết bị đã bị khóa: ${deviceInfo.lockReason ?: "Không rõ lý do"}")
                }

                _uiState.value = DashboardUiState(
                    isLoading = false,
                    deviceInfo = deviceInfo,
                    licenseInfo = licenseInfo,
                    systemStats = SystemStats(), // Will be filled by system service
                    alerts = alerts,
                )
            } catch (e: Exception) {
                _uiState.value = DashboardUiState(
                    isLoading = false,
                    error = e.message ?: "Failed to load dashboard",
                )
            }
        }
    }

    fun performQuickOptimize() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            delay(1500) // Simulate optimization
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
}
