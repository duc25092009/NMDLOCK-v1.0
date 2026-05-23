package com.nmdlock.app.feature.device

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nmdlock.app.core.services.SystemInfoProvider
import com.nmdlock.app.core.ui.components.*
import com.nmdlock.app.core.ui.theme.*
import com.nmdlock.app.data.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeviceUiState(
    val isLoading: Boolean = true,
    val deviceId: String = "",
    val deviceName: String = "",
    val deviceModel: String = "",
    val androidVersion: String = "",
    val firstActivationAt: String? = null,
    val lastSeenAt: String? = null,
    val isLocked: Boolean = false,
    val verifiedCount: Int = 0,
    val isRegistered: Boolean = false,
    val error: String? = null,
    // REAL system info
    val ramTotalMB: Long = 0,
    val ramUsedMB: Long = 0,
    val storageTotalGB: Long = 0,
    val storageUsedGB: Long = 0,
    val batteryPercent: Int = 0,
    val batteryTemp: Float = 0f,
    val cpuUsage: Float = 0f,
    val networkType: String = "",
    val networkStrength: String = "",
    val shizukuAvailable: Boolean = false,
)

@HiltViewModel
class DeviceViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val systemInfoProvider: SystemInfoProvider,
    private val shizukuManager: com.nmdlock.app.core.services.ShizukuManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceUiState())
    val uiState: StateFlow<DeviceUiState> = _uiState.asStateFlow()

    init { loadDeviceInfo() }

    fun loadDeviceInfo() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val deviceId = deviceRepository.getDeviceId()
                val status = deviceRepository.getDeviceStatus().getOrNull()
                val realStats = systemInfoProvider.getSystemStats()

                _uiState.value = DeviceUiState(
                    isLoading = false,
                    deviceId = deviceId,
                    deviceName = status?.deviceName ?: deviceId.take(12),
                    deviceModel = status?.deviceModel ?: android.os.Build.MODEL,
                    androidVersion = status?.androidVersion ?: android.os.Build.VERSION.RELEASE,
                    firstActivationAt = status?.firstActivationAt,
                    lastSeenAt = status?.lastSeenAt,
                    isLocked = status?.isLocked ?: false,
                    verifiedCount = status?.verifiedCount ?: 0,
                    isRegistered = status?.registered ?: false,
                    ramTotalMB = realStats.ramTotalMB,
                    ramUsedMB = realStats.ramUsedMB,
                    storageTotalGB = realStats.storageTotalGB,
                    storageUsedGB = realStats.storageUsedGB,
                    batteryPercent = realStats.batteryPercent,
                    batteryTemp = realStats.batteryTemp,
                    cpuUsage = realStats.cpuUsage,
                    networkType = realStats.networkType,
                    networkStrength = realStats.networkStrength,
                    shizukuAvailable = shizukuManager.isShizukuAvailable(),
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}

@Composable
fun DeviceScreen(
    viewModel: DeviceViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        SectionHeader(title = "Thong tin thiet bi")
        Spacer(modifier = Modifier.height(12.dp))

        // Device ID Card — matches preview .device-id-card
        DeviceIdCard(
            deviceId = uiState.deviceId,
            modifier = Modifier.padding(horizontal = 16.dp),
            onCopy = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Device ID", uiState.deviceId))
            },
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Device details — matches preview .key-detail-card
        GlowCard(modifier = Modifier.padding(horizontal = 16.dp)) {
            DetailRow(label = "Ten may", value = uiState.deviceName)
            DetailRow(label = "Ma Model", value = uiState.deviceModel)
            DetailRow(label = "He dieu hanh", value = "Android ${uiState.androidVersion}")
            DetailRow(
                label = "Trang thai Shizuku",
                value = if (uiState.shizukuAvailable) "San sang" else "Khong kha dung",
                valueColor = if (uiState.shizukuAvailable) Success else Error,
            )
            DetailRow(label = "Kich hoat lan dau", value = uiState.firstActivationAt?.take(10) ?: "—")
            DetailRow(label = "Hoat dong gan nhat", value = uiState.lastSeenAt?.take(10) ?: "—")
            DetailRow(label = "So lan xac minh", value = uiState.verifiedCount.toString())
        }
        Spacer(modifier = Modifier.height(20.dp))

        // REAL hardware resources — matching preview
        SectionHeader(title = "Tai nguyen phan cung hien tai")
        Spacer(modifier = Modifier.height(12.dp))

        GlowCard(modifier = Modifier.padding(horizontal = 16.dp)) {
            val ramPercent = if (uiState.ramTotalMB > 0) uiState.ramUsedMB * 100 / uiState.ramTotalMB else 0
            val storagePercent = if (uiState.storageTotalGB > 0) uiState.storageUsedGB * 100 / uiState.storageTotalGB else 0
            DetailRow(
                label = "Dung luong bo nho RAM",
                value = "${uiState.ramUsedMB}MB / ${uiState.ramTotalMB}MB ($ramPercent%)",
                valueColor = if (ramPercent > 75) Error else if (ramPercent > 50) Warning else Success,
            )
            DetailRow(
                label = "Bo nho luu tru trong",
                value = "${uiState.storageUsedGB}GB / ${uiState.storageTotalGB}GB ($storagePercent%)",
                valueColor = if (storagePercent > 85) Error else if (storagePercent > 60) Warning else Success,
            )
            DetailRow(
                label = "Nhiet do Pin",
                value = "${uiState.batteryTemp}°C",
                valueColor = if (uiState.batteryTemp > 42) Error else if (uiState.batteryTemp > 38) Warning else Success,
            )
            DetailRow(
                label = "Xung nhip CPU load",
                value = "${uiState.cpuUsage.toInt()}%",
                valueColor = if (uiState.cpuUsage > 80) Error else if (uiState.cpuUsage > 50) Warning else Success,
            )
            DetailRow(label = "Mang", value = "${uiState.networkType} - ${uiState.networkStrength}")
        }
        Spacer(modifier = Modifier.height(20.dp))

        // Sync button
        GradientButton(
            text = "Dong bo du lieu voi Server",
            onClick = { viewModel.loadDeviceInfo() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            icon = Icons.Default.Sync,
        )
        Spacer(modifier = Modifier.height(80.dp))
    }
}
