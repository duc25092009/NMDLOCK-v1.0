// File: app/src/main/java/com/nmdlock/app/feature/license/LicenseGateScreen.kt

package com.nmdlock.app.feature.license

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LicenseGateScreen(
    viewModel: LicenseViewModel = hiltViewModel(),
    deviceId: String,
    onLicenseValid: () -> Unit
) {
    // FIX: Gọi đúng hàm từ ViewModel
    val isValid by viewModel.isLicenseValid(deviceId)
        .collectAsStateWithLifecycle(initialValue = false)
    
    LaunchedEffect(isValid) {
        if (isValid) {
            onLicenseValid()
        }
    }
    
    if (!isValid) {
        // Show license validation UI
        LicenseValidationContent(
            onKeySubmit = { key ->
                viewModel.validateKey(deviceId, key)
            }
        )
    }
}
