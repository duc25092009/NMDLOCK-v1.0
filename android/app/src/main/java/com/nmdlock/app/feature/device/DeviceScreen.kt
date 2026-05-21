package com.nmdlock.app.feature.device

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
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
    val history: List<String> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class DeviceViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
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
                _uiState.value = DeviceUiState(
                    isLoading = false,
                    deviceId = deviceId,
                    deviceName = status?.deviceName ?: "Unknown",
                    deviceModel = status?.deviceModel ?: "Unknown",
                    androidVersion = status?.androidVersion ?: "Unknown",
                    firstActivationAt = status?.firstActivationAt,
                    lastSeenAt = status?.lastSeenAt,
                    isLocked = status?.isLocked ?: false,
                    verifiedCount = status?.verifiedCount ?: 0,
                    isRegistered = status?.registered ?: false,
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
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "Thiết bị",
            style = MaterialTheme.typography.headlineMedium,
            color = DarkText,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Device ID Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Device ID", color = DarkTextSecondary, style = MaterialTheme.typography.bodySmall)
                    StatusBadge(text = if (uiState.isRegistered) "ĐÃ ĐĂNG KÝ" else "CỤC BỘ", color = if (uiState.isRegistered) Success else Warning)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = uiState.deviceId,
                        color = DarkText,
                        style = MaterialTheme.typography.titleSmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Device ID", uiState.deviceId))
                    }) {
                        Icon(Icons.Default.ContentCopy, "Copy", tint = Purple400)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Device Details
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                DetailRow("Tên máy", uiState.deviceName)
                DetailRow("Model", uiState.deviceModel)
                DetailRow("Android", uiState.androidVersion)
                DetailRow("Kích hoạt lần đầu", uiState.firstActivationAt?.take(10) ?: "—")
                DetailRow("Hoạt động gần nhất", uiState.lastSeenAt?.take(10) ?: "—")
                DetailRow("Số lần xác minh", uiState.verifiedCount.toString())
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // Buttons
        GradientButton(
            text = "Đồng bộ với server",
            onClick = { viewModel.loadDeviceInfo() },
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Default.Sync,
        )
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = DarkTextSecondary, style = MaterialTheme.typography.bodySmall)
        Text(value, color = DarkText, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}
