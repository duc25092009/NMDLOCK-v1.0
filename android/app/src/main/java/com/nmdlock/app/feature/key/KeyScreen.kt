package com.nmdlock.app.feature.key

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
            _uiState.value = _uiState.value.copy(activationMessage = "Vui long nhap key", isError = true)
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, activationMessage = null, isError = false)
            val result = licenseRepository.activateLicense(key)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        activationMessage = "Kich hoat thanh cong!",
                        isError = false,
                        keyInput = "",
                    )
                    loadLicenseInfo()
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        activationMessage = it.message ?: "Kich hoat that bai",
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
    val uiStateVal by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Header
        Spacer(modifier = Modifier.height(12.dp))
        SectionHeader(title = "Thong tin ban quyen")
        Spacer(modifier = Modifier.height(12.dp))

        // Key Status Card — matching preview .key-status
        KeyStatusCard(
            title = if (uiStateVal.isLicenseValid) "Ban quyen: Hop le" else "Chua co key",
            subtitle = when {
                uiStateVal.isPermanent -> "Vinh vien"
                uiStateVal.remainingDays != null -> "Con ${uiStateVal.remainingDays} ngay"
                else -> "Kich hoat de su dung"
            },
            isValid = uiStateVal.isLicenseValid,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))

        // License details — matching preview .key-detail-card
        GlowCard(modifier = Modifier.padding(horizontal = 16.dp)) {
            DetailRow(label = "Trang thai", value = uiStateVal.licenseStatus?.uppercase() ?: "—")
            DetailRow(label = "Loai ban quyen", value = uiStateVal.licenseType ?: "—")
            if (uiStateVal.remainingDays != null) {
                DetailRow(label = "Con lai", value = "${uiStateVal.remainingDays} ngay")
            }
            if (uiStateVal.expiresAt != null) {
                DetailRow(label = "Het han", value = uiStateVal.expiresAt.take(10))
            }
            if (uiStateVal.isPermanent) {
                Spacer(modifier = Modifier.height(8.dp))
                StatusBadge(text = "VINH VIEN", color = Purple400)
            }
        }
        Spacer(modifier = Modifier.height(20.dp))

        // Renew / new key section — matching preview HTML key input
        SectionHeader(title = "Gia han hoac Doi ma Key moi")
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = uiStateVal.keyInput,
            onValueChange = { viewModel.onKeyInputChanged(it) },
            placeholder = { Text("Nhap ma License moi", color = DarkTextSecondary) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
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

        GradientButton(
            text = if (uiStateVal.isLoading) "Dang xac thuc..." else "Xac nhan doi ma Key",
            onClick = { viewModel.activateKey() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            icon = Icons.Default.VpnKey,
            isLoading = uiStateVal.isLoading,
        )

        // Message
        if (uiStateVal.activationMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            AlertBanner(
                message = uiStateVal.activationMessage!!,
                type = if (uiStateVal.isError) AlertType.ERROR else AlertType.SUCCESS,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // History
        if (uiStateVal.history.isNotEmpty()) {
            SectionHeader(title = "Lich su key")
            Spacer(modifier = Modifier.height(8.dp))
            GlowCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                uiStateVal.history.forEach { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = entry.keyValue?.take(20) ?: "—",
                            color = DarkText,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = MaterialTheme.typography.bodySmall.fontSize,
                        )
                        val entryType = entry.type ?: "—"
                        StatusBadge(
                            text = if (entryType.contains("device_locked") || entryType.contains("lock")) "LOCK"
                                   else if (entryType.contains("flex")) "FLEX"
                                   else entryType.take(8),
                            color = if (entryType.contains("lock")) Warning else Info,
                            small = true,
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}
