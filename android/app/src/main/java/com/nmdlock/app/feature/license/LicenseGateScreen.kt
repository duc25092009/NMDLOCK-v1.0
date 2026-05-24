package com.nmdlock.app.feature.license

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
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

// ============================================================================
// UI STATE
// ============================================================================
data class LicenseGateUiState(
    val isLoading: Boolean = false,
    val isChecking: Boolean = true,          // true = đang check license từ server
    val keyInput: String = "",
    val licenseValid: Boolean = false,       // true = license hợp lệ → vào app
    val message: String? = null,
    val isError: Boolean = false,
    val deviceId: String = "",
    val hasCheckedOnce: Boolean = false,     // FIX: Track đã check chưa để tránh loop
)

// ============================================================================
// VIEWMODEL
// ============================================================================
@HiltViewModel
class LicenseGateViewModel @Inject constructor(
    private val licenseRepository: LicenseRepository,
    private val deviceIdManager: DeviceIdManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LicenseGateUiState())
    val uiState: StateFlow<LicenseGateUiState> = _uiState.asStateFlow()

    // FIX: Không gọi checkLicense() trong init để tránh race condition
    // Gọi từ Composable qua LaunchedEffect có control tốt hơn

    fun checkLicense() {
        viewModelScope.launch {
            Log.d("NMD_LICENSE", "ViewModel: Starting license check...")
            _uiState.value = _uiState.value.copy(
                isChecking = true,
                hasCheckedOnce = true,
                message = null,
                isError = false
            )
            
            try {
                val deviceId = deviceIdManager.getDeviceId()
                Log.d("NMD_LICENSE", "ViewModel: Device ID = $deviceId")
                
                // Gọi repository check license
                val valid = licenseRepository.isLicenseValid()
                Log.d("NMD_LICENSE", "ViewModel: License valid = $valid")
                
                _uiState.value = _uiState.value.copy(
                    isChecking = false,
                    licenseValid = valid,
                    deviceId = deviceId,
                    message = if (!valid) "License không hợp lệ. Vui lòng nhập key." else null,
                    isError = !valid
                )
            } catch (e: Exception) {
                Log.e("NMD_LICENSE", "ViewModel: Error checking license", e)
                _uiState.value = _uiState.value.copy(
                    isChecking = false,
                    licenseValid = false,
                    message = "Lỗi kết nối: ${e.message ?: "Unknown"}",
                    isError = true
                )
            }
        }
    }

    fun onKeyInputChanged(value: String) {
        _uiState.value = _uiState.value.copy(
            keyInput = value,
            message = null,
            isError = false
        )
    }

    fun activateKey() {
        val key = _uiState.value.keyInput.trim()
        
        if (key.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                message = "Vui lòng nhập license key",
                isError = true
            )
            return
        }
        
        viewModelScope.launch {
            Log.d("NMD_LICENSE", "ViewModel: Activating key: ${key.take(4)}...")
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                message = null,
                isError = false
            )
            
            try {
                val result = licenseRepository.activateLicense(key)
                result.fold(
                    onSuccess = {
                        Log.d("NMD_LICENSE", "ViewModel: Key activated successfully!")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            message = "Đã kích hoạt License Premium thành công!",
                            isError = false,
                            licenseValid = true,  // ← QUAN TRỌNG: Set true để trigger onLicenseValid
                            keyInput = "",
                        )
                    },
                    onFailure = { error ->
                        Log.e("NMD_LICENSE", "ViewModel: Activation failed", error)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            message = error.message ?: "Kích hoạt thất bại",
                            isError = true,
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e("NMD_LICENSE", "ViewModel: Exception during activation", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Lỗi: ${e.message ?: "Unknown error"}",
                    isError = true,
                )
            }
        }
    }

    fun retryCheck() {
        Log.d("NMD_LICENSE", "ViewModel: Retry check requested")
        checkLicense()
    }
    
    // Helper để reset state khi cần
    fun resetState() {
        _uiState.value = LicenseGateUiState()
    }
}

// ============================================================================
// COMPOSABLE SCREEN
// ============================================================================
@Composable
fun LicenseGateScreen(
    onLicenseValid: () -> Unit,  // ← Bắt buộc, không có default để tránh gọi nhầm
    viewModel: LicenseGateViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Log state changes cho debugging
    Log.d("NMD_LICENSE_UI", "Screen: Rendering with state = ${uiState.licenseValid}, isChecking = ${uiState.isChecking}")
    
    // FIX QUAN TRỌNG: Gọi onLicenseValid chỉ khi licenseValid = true VÀ chưa được gọi trước đó
    // Dùng remember để tránh gọi lại mỗi lần recompose
    val hasCalledCallback = remember { mutableStateOf(false) }
    
    LaunchedEffect(uiState.licenseValid) {
        if (uiState.licenseValid && !hasCalledCallback.value) {
            Log.d("NMD_LICENSE_UI", "Screen: License valid + callback not called → invoking onLicenseValid()")
            hasCalledCallback.value = true
            onLicenseValid()  // ← Gọi callback để MainActivity chuyển sang AppState.Ready
        }
    }
    
    // Auto-check license khi màn hình lần đầu render
    LaunchedEffect(Unit) {
        if (!uiState.hasCheckedOnce) {
            Log.d("NMD_LICENSE_UI", "Screen: First launch → calling viewModel.checkLicense()")
            viewModel.checkLicense()
        }
    }
    
    // Main UI container
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        contentAlignment = Alignment.Center,
    ) {
        when {
            // === TRẠNG THÁI 1: Đang check license từ server ===
            uiState.isChecking -> {
                Log.d("NMD_LICENSE_UI", "Screen: Showing loading state")
                LoadingState()
            }
            
            // === TRẠNG THÁI 2: License hợp lệ → UI trống để chuyển sang app ===
            uiState.licenseValid -> {
                Log.d("NMD_LICENSE_UI", "Screen: License valid → showing empty box (transitioning to app)")
                // FIX: Hiển thị Box rỗng với background đồng nhất để tránh flash
                // Content thực sự sẽ được render bởi MainActivity sau khi onLicenseValid() được gọi
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkBg)
                )
            }
            
            // === TRẠNG THÁI 3: License không hợp lệ → hiển thị form nhập key ===
            else -> {
                Log.d("NMD_LICENSE_UI", "Screen: Showing license gate form")
                LicenseGateForm(
                    uiState = uiState,
                    onKeyInputChanged = viewModel::onKeyInputChanged,
                    onActivateKey = viewModel::activateKey,
                    onRetryCheck = viewModel::retryCheck,
                    onCopyDeviceId = { deviceId ->
                        // Copy device ID vào clipboard
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Device ID", deviceId)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Đã copy Device ID", Toast.LENGTH_SHORT).show()
                        Log.d("NMD_LICENSE_UI", "Screen: Copied device ID to clipboard")
                    }
                )
            }
        }
    }
}

// ============================================================================
// SUB-COMPOSABLES (Tách nhỏ để dễ maintain)
// ============================================================================

@Composable
private fun LoadingState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        // Dual-ring gradient spinner
        Box(modifier = Modifier.size(60.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                color = AccentCyan,
                strokeWidth = 3.dp,
                modifier = Modifier.size(50.dp),
            )
            CircularProgressIndicator(
                color = AccentPurple.copy(alpha = 0.4f),
                strokeWidth = 3.dp,
                modifier = Modifier
                    .size(50.dp)
                    .padding(3.dp),
                trackColor = DarkSurface
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "Đang kiểm tra license...",
            color = DarkTextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Subtle loading dots animation
        LoadingDots()
    }
}

@Composable
private fun LoadingDots() {
    var count by remember { mutableStateOf(0) }
    
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(400)
            count = (count + 1) % 4
        }
    }
    
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = if (index < count) AccentCyan else DarkSurface2,
                        shape = RoundedCornerShape(4.dp)
                    )
            )
        }
    }
}

@Composable
private fun LicenseGateForm(
    uiState: LicenseGateUiState,
    onKeyInputChanged: (String) -> Unit,
    onActivateKey: () -> Unit,
    onRetryCheck: () -> Unit,
    onCopyDeviceId: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // === Logo / Icon ===
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + scaleIn(),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
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
                    contentDescription = "License Key",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp),
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // === Title ===
        Text(
            "NMDLock v1.0",
            color = DarkText,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        
        Text(
            "Kích hoạt license cao cấp để tiếp tục",
            color = DarkTextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
        
        Spacer(modifier = Modifier.height(28.dp))
        
        // === Device ID Card (có thể copy) ===
        AnimatedVisibility(
            visible = uiState.deviceId.isNotEmpty(),
            enter = slideInVertically() + fadeIn(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCopyDeviceId(uiState.deviceId) },
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "MÃ THIẾT BỊ (DEVICE ID)",
                            color = DarkTextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.5.sp,
                        )
                        Text(
                            uiState.deviceId,
                            color = DarkText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        )
                    }
                    
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy Device ID",
                        tint = AccentCyan.copy(alpha = 0.8f),
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onCopyDeviceId(uiState.deviceId) }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // === Key Input Field ===
        OutlinedTextField(
            value = uiState.keyInput,
            onValueChange = onKeyInputChanged,
            placeholder = {
                Text(
                    "Nhập Key (VD: NMDK-XXXX-XXXX-XXXX)",
                    color = DarkTextSecondary,
                    fontSize = 14.sp,
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .animateEnterExit(enter = slideInHorizontally() + fadeIn()),
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
                errorBorderColor = Error,
            ),
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { onActivateKey() }),
            isError = uiState.isError && uiState.message != null,
            supportingText = if (uiState.isError && uiState.message != null) {
                {
                    Text(
                        uiState.message!!,
                        color = Error,
                        fontSize = 12.sp
                    )
                }
            } else null,
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // === Activate Button (Gradient) ===
        Button(
            onClick = onActivateKey,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .animateEnterExit(enter = scaleIn() + fadeIn()),
            enabled = !uiState.isLoading && uiState.keyInput.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(14.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        ) {
            // Gradient background layer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(listOf(AccentPurple, AccentCyan)),
                        shape = RoundedCornerShape(14.dp),
                    )
            )
            
            // Content layer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                } else {
                    Icon(
                        Icons.Default.VpnKey,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                Text(
                    text = if (uiState.isLoading) "Đang xác thực..." else "Kích hoạt ngay",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
            }
        }
        
        // === Success Message ===
        AnimatedVisibility(
            visible = !uiState.isError && uiState.message != null && uiState.licenseValid,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                uiState.message!!,
                color = Success,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
        }
        
        // === Error Message ===
        AnimatedVisibility(
            visible = uiState.isError && uiState.message != null && !uiState.licenseValid,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                uiState.message!!,
                color = Error,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // === Helper Text ===
        Text(
            "Chưa có key? Vui lòng liên hệ Admin để mua key.",
            color = DarkTextSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // === Retry Button ===
        TextButton(
            onClick = onRetryCheck,
            modifier = Modifier.animateEnterExit(enter = fadeIn())
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = null,
                tint = AccentCyan,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "Thử lại",
                color = AccentCyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
