// File: app/src/main/java/com/nmdlock/app/feature/dashboard/DashboardViewModel.kt

package com.nmdlock.app.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nmdlock.app.data.repository.LicenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val licenseRepo: LicenseRepository
) : ViewModel() {
    
    // ... existing code ...
    
    fun loadLicenseInfo(deviceId: String) {
        viewModelScope.launch {
            // FIX: Xử lý Result đúng cách, tránh "Unresolved reference: it"
            licenseRepo.getMyLicense(deviceId)
                .onSuccess { license ->
                    // FIX: Kiểm tra null trước khi dùng "it"
                    _licenseState.value = license
                    _deviceName.value = license.deviceName
                    _expiryDate.value = license.expiryDate
                    _featuresEnabled.value = license.enabledFeatures
                    _licenseType.value = license.type
                    _isActive.value = license.isValid()
                    _remainingDays.value = license.remainingDays
                }
                .onFailure { error ->
                    _licenseError.value = error.message ?: "Unknown error"
                }
        }
    }
    
    // Hoặc dùng Flow cách khác:
    fun loadLicenseInfoFlow(deviceId: String): StateFlow<LicenseState> {
        return licenseRepo.getMyLicense(deviceId)
            .map { result ->
                result.fold(
                    onSuccess = { license -> LicenseState.Success(license) },
                    onFailure = { error -> LicenseState.Error(error.message ?: "Error") }
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LicenseState.Loading)
    }
    
    // ... existing code ...
}

// Helper sealed class nếu chưa có
sealed class LicenseState {
    object Loading : LicenseState()
    data class Success(val license: License) : LicenseState()
    data class Error(val message: String) : LicenseState()
}
