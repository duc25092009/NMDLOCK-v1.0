package com.nmdlock.app.feature.license

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.nmdlock.app.data.repository.LicenseRepository
import kotlinx.coroutines.launch

@Composable
fun LicenseGateScreen(
    repository: LicenseRepository = hiltViewModel()
) {

    var key by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun activate(deviceId: String) {

        scope.launch {

            repository.activateLicense(
                key = key,
                deviceId = deviceId
            )
        }
    }
}
