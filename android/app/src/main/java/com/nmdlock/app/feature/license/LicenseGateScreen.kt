package com.nmdlock.app.feature.license

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nmdlock.app.core.security.DeviceIdManager
import com.nmdlock.app.core.ui.theme.*
import com.nmdlock.app.data.repository.LicenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LicenseGateUiState(
    val isLoading: Boolean = false,
    val isChecking: Boolean = true,
    val keyInput: String = "",
    val licenseValid: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
    val deviceId: String = "",
)

@HiltViewModel
class LicenseGateViewModel @Inject constructor(
    private val licenseRepository: LicenseRepository,
    private val deviceIdManager: DeviceIdManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LicenseGateUiState())
    val uiState: StateFlow<LicenseGateUiState> = _uiState.asStateFlow()

    init { checkLicense() }

    fun checkLicense() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isChecking = true)
            val deviceId = deviceIdManager.getDeviceId()
            val valid = licenseRepository.isLicenseValid()
            _uiState.value = _uiState.value.copy(
                isChecking = false,
                licenseValid = valid,
                deviceId = deviceId,
            )
        }
    }

    fun onKeyInputChanged(value: String) {
        _uiState.value = _uiState.value.copy(keyInput = value, message = null, isError = false)
    }

    fun activateKey() {
        val key = _uiState.value.keyInput.trim()
        if (key.isEmpty()) {
            _uiState.value = _uiState.value.copy(message = "Vui long nhap license key", isError = true)
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, message = null, isError = false)
            val result = licenseRepository.activateLicense(key)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "Da kich hoat License Premium thanh cong!",
                        isError = false,
                        licenseValid = true,
                        keyInput = "",
                    )
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = it.message ?: "Kich hoat that bai",
                        isError = true,
                    )
                }
            )
        }
    }

    fun retryCheck() { checkLicense() }
}

@Composable
fun LicenseGateScreen(
    onLicenseValid: () -> Unit = {},
    viewModel: LicenseGateViewModel = hiltViewModel(),
) {
    val uiStateVal by viewModel.uiState.collectAsState()

    LaunchedEffect(uiStateVal.licenseValid) {
        if (uiStateVal.licenseValid) {
            onLicenseValid()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        contentAlignment = Alignment.Center,
    ) {
        if (uiStateVal.isChecking) {
            // Loading check screen
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Gradient spinner
                Box(modifier = Modifier.size(50.dp)) {
                    CircularProgressIndicator(
                        color = AccentCyan,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(50.dp),
                    )
                    CircularProgressIndicator(
                        color = AccentPurple.copy(alpha = 0.3f),
                        strokeWidth = 3.dp,
                        modifier = Modifier
                            .size(50.dp)
                            .padding(3.dp),
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    "Dang kiem tra license...",
                    color = DarkTextSecondary,
                    fontSize = 14.sp,
                )
            }
        } else if (!uiStateVal.licenseValid) {
            // ═══ GATE SCREEN — matches preview HTML .gate-screen ═══
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Logo
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.horizontalGradient(listOf(AccentPurple, AccentCyan))
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.VpnKey,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(38.dp),
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    "NMDLock v1.0",
                    color = DarkText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Kich hoat license cao cap de tiep tuc",
                    color = DarkTextSecondary,
                    fontSize = 14.sp,
                )
                Spacer(modifier = Modifier.height(28.dp))

                // Device ID card
                androidx.compose.material3.Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp, 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.PhoneAndroid, null, tint = AccentCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "MA THIET BI CUA BAN (DEVICE ID)",
                                color = DarkTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.5.sp,
                            )
                            Text(
                                uiStateVal.deviceId,
                                color = DarkText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            )
                        }
                        Icon(
                            Icons.Default.ContentCopy,
                            null,
                            tint = AccentCyan,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Key input
                OutlinedTextField(
                    value = uiStateVal.keyInput,
                    onValueChange = { viewModel.onKeyInputChanged(it) },
                    placeholder = {
                        Text(
                            "Nhap Key (VD: NMDK-XXXX-XXXX-XXXX)",
                            color = DarkTextSecondary,
                            fontSize = 14.sp,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = DarkText,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentPurple,
                        unfocusedBorderColor = DarkSurface2,
                        focusedTextColor = DarkText,
                        unfocusedTextColor = DarkText,
                        cursorColor = AccentPurple,
                        focusedContainerColor = DarkSurface2,
                        unfocusedContainerColor = DarkSurface,
                    ),
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { viewModel.activateKey() }),
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Activate button — gradient matching preview
                Button(
                    onClick = { viewModel.activateKey() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !uiStateVal.isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                    ),
                    shape = RoundedCornerShape(14.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.horizontalGradient(listOf(AccentPurple, AccentCyan)),
                                shape = RoundedCornerShape(14.dp),
                            )
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        if (uiStateVal.isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Icon(Icons.Default.VpnKey, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (uiStateVal.isLoading) "Dang xac thuc chung chi..." else "Kich hoat ngay",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                        )
                    }
                }

                // Message
                if (uiStateVal.message != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        uiStateVal.message!!,
                        color = if (uiStateVal.isError) Error else Success,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Helper text
                Text(
                    "Chua co key? Vui long lien he Admin de mua key tu Admin.",
                    color = DarkTextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = { viewModel.retryCheck() }) {
                    Icon(Icons.Default.Refresh, null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Thu lai", color = AccentCyan, fontSize = 13.sp)
                }
            }
        }
    }
}
