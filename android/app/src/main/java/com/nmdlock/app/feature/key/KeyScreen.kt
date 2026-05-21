package com.nmdlock.app.feature.key

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nmdlock.app.core.ui.components.*
import com.nmdlock.app.core.ui.theme.*
import com.nmdlock.app.data.repository.LicenseRepository
import com.nmdlock.app.data.remote.dto.LicenseHistoryEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class KeyUiState(
    val isLoading: Boolean = false,
    val keyInput: String = "",
    val isLicenseValid: Boolean = false,
    val licenseType: String? = null,
    val licenseStatus: String? = null,
    val remainingDays: Int? = null,
    val expiresAt: String? = null,
    val isPermanent: Boolean = false,
    val history: List<LicenseHistoryEntry> = emptyList(),
    val activationMessage: String? = null,
    val isError: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class KeyViewModel @Inject constructor(
    private val licenseRepository: LicenseRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(KeyUiState())
    val uiState: StateFlow<KeyUiState> = _uiState.asStateFlow()

    init { loadLicenseInfo() }

    fun onKeyInputChanged(value: String) {
        _uiState.value = _uiState.value.copy(keyInput = value, activationMessage = null, isError = false)
    }

    fun activateKey() {
        val key = _uiState.value.keyInput.trim()
        if (key.isEmpty()) {
            _uiState.value = _uiState.value.copy(activationMessage = "Vui lòng nhập key", isError = true)
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, activationMessage = null, isError = false)
            val result = licenseRepository.activateLicense(key)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        activationMessage = "Kích hoạt thành công!",
                        isError = false,
                        keyInput = "",
                    )
                    loadLicenseInfo()
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        activationMessage = it.message ?: "Kích hoạt thất bại",
                        isError = true,
                    )
                }
            )
        }
    }

    fun loadLicenseInfo() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val licenseResult = licenseRepository.getMyLicense()
                val valid = licenseRepository.isLicenseValid()
                val historyResult = licenseRepository.getHistory()

                licenseResult.getOrNull()?.let {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLicenseValid = valid,
                        licenseType = it.type,
                        licenseStatus = it.status,
                        remainingDays = it.remainingDays,
                        expiresAt = it.expiresAt,
                        isPermanent = it.isPermanent,
                        history = historyResult.getOrNull() ?: emptyList(),
                    )
                } ?: run {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}

@Composable
fun KeyScreen(
    viewModel: KeyViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "License Key",
            style = MaterialTheme.typography.headlineMedium,
            color = DarkText,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Current license status
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
                    Text("Trạng thái", color = DarkTextSecondary)
                    StatusBadge(
                        text = if (uiState.isLicenseValid) "HỢP LỆ" else if (uiState.licenseStatus == "expired") "HẾT HẠN" else "CHƯA CÓ",
                        color = if (uiState.isLicenseValid) Success else Warning,
                    )
                }
                if (uiState.licenseType != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    DetailRow("Loại key", uiState.licenseType)
                }
                if (uiState.remainingDays != null) {
                    DetailRow("Còn lại", "${uiState.remainingDays} ngày")
                }
                if (uiState.expiresAt != null) {
                    DetailRow("Hết hạn", uiState.expiresAt.take(10))
                }
                if (uiState.isPermanent) {
                    Spacer(modifier = Modifier.height(8.dp))
                    StatusBadge(text = "VĨNH VIỄN", color = Purple400)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Key input
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Nhập key kích hoạt", color = DarkText, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = uiState.keyInput,
                    onValueChange = { viewModel.onKeyInputChanged(it) },
                    placeholder = { Text("Nhập license key", color = DarkTextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Purple400,
                        unfocusedBorderColor = DarkSurface2,
                        focusedTextColor = DarkText,
                        unfocusedTextColor = DarkText,
                        cursorColor = Purple400,
                    ),
                    shape = RoundedCornerShape(12.dp),
                )
                Spacer(modifier = Modifier.height(12.dp))
                GradientButton(
                    text = "Kích hoạt",
                    onClick = { viewModel.activateKey() },
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.VpnKey,
                )
                if (uiState.activationMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.activationMessage,
                        color = if (uiState.isError) Error else Success,
                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // History
        if (uiState.history.isNotEmpty()) {
            SectionHeader(title = "Lịch sử key")
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    uiState.history.forEach { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(entry.keyValue?.take(20) ?: "—", color = DarkText, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = MaterialTheme.typography.bodySmall.fontSize)
                            Text(entry.type ?: "—", color = DarkTextSecondary, fontSize = MaterialTheme.typography.bodySmall.fontSize)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = DarkTextSecondary, style = MaterialTheme.typography.bodySmall)
        Text(value, color = DarkText, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}
