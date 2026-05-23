package com.nmdlock.app.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nmdlock.app.data.repository.LicenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val licenseRepository: LicenseRepository
) : ViewModel() {

    private val _licenseState = MutableStateFlow(false)
    val licenseState: StateFlow<Boolean> = _licenseState

    fun validateLicense(
        key: String,
        deviceId: String
    ) {

        viewModelScope.launch {

            val result = licenseRepository.validateLicense(
                key = key,
                deviceId = deviceId
            )

            _licenseState.value = result.isSuccess
        }
    }
}
