// File: app/src/main/java/com/nmdlock/app/feature/key/KeyScreen.kt

package com.nmdlock.app.feature.key

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.nmdlock.app.feature.key.KeyViewModel

@Composable
fun KeyScreen(
    viewModel: KeyViewModel = hiltViewModel(),
    deviceId: String
) {
    // FIX: Dùng collectAsStateWithLifecycle hoặc proper Flow handling
    val licenseState by viewModel.getMyLicense(deviceId)
        .collectAsStateWithLifecycle(initialValue = LicenseState.Loading)
    
    val isValid by viewModel.isLicenseValid(deviceId)
        .collectAsStateWithLifecycle(initialValue = false)
    
    val history by viewModel.getHistory()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    
    LaunchedEffect(deviceId) {
        viewModel.loadLicense(deviceId)
    }
    
    when (val state = licenseState) {
        is LicenseState.Loading -> {
            // Show loading
            CircularProgressIndicator()
        }
        is LicenseState.Success -> {
            // FIX: Dùng state.license thay vì "it"
            val license = state.license
            Text(text = "Device: ${license.deviceName}")
            Text(text = "Expiry: ${license.expiryDate}")
            // ... rest of UI
        }
        is LicenseState.Error -> {
            Text(text = "Error: ${state.message}")
        }
    }
    
    // ... rest of code ...
}
