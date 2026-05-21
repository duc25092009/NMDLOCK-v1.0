package com.nmdlock.app.feature.settings

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
import com.nmdlock.app.core.ui.components.SectionHeader
import com.nmdlock.app.core.ui.theme.*
import com.nmdlock.app.data.local.DataStoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isDarkTheme: Boolean = true,
    val language: String = "vi",
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val autoSyncEnabled: Boolean = true,
    val loggingEnabled: Boolean = false,
    val appVersion: String = "1.0.0",
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStoreManager: DataStoreManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init { loadSettings() }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.value = SettingsUiState(
                isDarkTheme = dataStoreManager.isDarkTheme.first(),
                language = dataStoreManager.language.first(),
                soundEnabled = dataStoreManager.soundEnabled.first(),
                vibrationEnabled = dataStoreManager.vibrationEnabled.first(),
                notificationsEnabled = dataStoreManager.notificationsEnabled.first(),
                autoSyncEnabled = dataStoreManager.autoSyncEnabled.first(),
                loggingEnabled = dataStoreManager.loggingEnabled.first(),
            )
        }
    }

    fun setDarkTheme(isDark: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setDarkTheme(isDark)
            _uiState.value = _uiState.value.copy(isDarkTheme = isDark)
        }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            dataStoreManager.setLanguage(lang)
            _uiState.value = _uiState.value.copy(language = lang)
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setSoundEnabled(enabled)
            _uiState.value = _uiState.value.copy(soundEnabled = enabled)
        }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setVibrationEnabled(enabled)
            _uiState.value = _uiState.value.copy(vibrationEnabled = enabled)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setNotificationsEnabled(enabled)
            _uiState.value = _uiState.value.copy(notificationsEnabled = enabled)
        }
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setAutoSyncEnabled(enabled)
            _uiState.value = _uiState.value.copy(autoSyncEnabled = enabled)
        }
    }

    fun setLoggingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setLoggingEnabled(enabled)
            _uiState.value = _uiState.value.copy(loggingEnabled = enabled)
        }
    }

    fun resetApp() {
        viewModelScope.launch {
            dataStoreManager.clearAll()
            loadSettings()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = DarkText)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Cài đặt",
                style = MaterialTheme.typography.headlineMedium,
                color = DarkText,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Display settings
        SectionHeader(title = "Giao diện")
        SettingsGroup {
            SettingsToggle(
                title = "Chế độ tối",
                subtitle = "Chuyển đổi giao diện tối/sáng",
                icon = Icons.Default.DarkMode,
                checked = uiState.isDarkTheme,
                onCheckedChange = { viewModel.setDarkTheme(it) },
            )
            HorizontalDivider(color = DarkSurface2, thickness = 0.5.dp)
            SettingsToggle(
                title = "Ngôn ngữ",
                subtitle = if (uiState.language == "vi") "Tiếng Việt" else "English",
                icon = Icons.Default.Language,
                trailing = {
                    Icon(Icons.Default.ChevronRight, null, tint = DarkTextSecondary)
                },
                onToggle = { viewModel.setLanguage(if (uiState.language == "vi") "en" else "vi") },
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Sound & Vibration
        SectionHeader(title = "Âm thanh & Rung")
        SettingsGroup {
            SettingsToggle(
                title = "Âm thanh",
                subtitle = "Bật/tắt hiệu ứng âm thanh",
                icon = Icons.Default.VolumeUp,
                checked = uiState.soundEnabled,
                onCheckedChange = { viewModel.setSoundEnabled(it) },
            )
            HorizontalDivider(color = DarkSurface2, thickness = 0.5.dp)
            SettingsToggle(
                title = "Rung",
                subtitle = "Bật/tắt phản hồi rung",
                icon = Icons.Default.Vibration,
                checked = uiState.vibrationEnabled,
                onCheckedChange = { viewModel.setVibrationEnabled(it) },
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Notifications & Sync
        SectionHeader(title = "Đồng bộ & Thông báo")
        SettingsGroup {
            SettingsToggle(
                title = "Thông báo",
                subtitle = "Nhận thông báo từ ứng dụng",
                icon = Icons.Default.Notifications,
                checked = uiState.notificationsEnabled,
                onCheckedChange = { viewModel.setNotificationsEnabled(it) },
            )
            HorizontalDivider(color = DarkSurface2, thickness = 0.5.dp)
            SettingsToggle(
                title = "Tự động đồng bộ",
                subtitle = "Đồng bộ dữ liệu với server",
                icon = Icons.Default.Sync,
                checked = uiState.autoSyncEnabled,
                onCheckedChange = { viewModel.setAutoSyncEnabled(it) },
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Debug
        SectionHeader(title = "Gỡ lỗi")
        SettingsGroup {
            SettingsToggle(
                title = "Ghi log",
                subtitle = "Ghi log hoạt động để chẩn đoán",
                icon = Icons.Default.BugReport,
                checked = uiState.loggingEnabled,
                onCheckedChange = { viewModel.setLoggingEnabled(it) },
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        // App Info
        SectionHeader(title = "Thông tin ứng dụng")
        SettingsGroup {
            SettingsInfo(
                title = "Phiên bản",
                subtitle = uiState.appVersion,
                icon = Icons.Default.Info,
            )
            HorizontalDivider(color = DarkSurface2, thickness = 0.5.dp)
            SettingsInfo(
                title = "Server",
                subtitle = "Online",
                icon = Icons.Default.Cloud,
                iconTint = Success,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Reset
        SectionHeader(title = "Dữ liệu")
        SettingsGroup {
            SettingsAction(
                title = "Reset ứng dụng",
                subtitle = "Xóa toàn bộ dữ liệu cục bộ",
                icon = Icons.Default.RestartAlt,
                iconTint = Error,
                onClick = { viewModel.resetApp() },
            )
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp), content = content)
    }
}

@Composable
private fun SettingsToggle(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Purple400, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = DarkText, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, color = DarkTextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Purple400,
                checkedTrackColor = Purple600.copy(alpha = 0.5f),
                uncheckedThumbColor = DarkTextSecondary,
                uncheckedTrackColor = DarkSurface2,
            ),
        )
    }
}

@Composable
private fun SettingsToggle(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    trailing: @Composable () -> Unit,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Purple400, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = DarkText, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, color = DarkTextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        trailing()
    }
}

@Composable
private fun SettingsInfo(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color = Purple400,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = DarkText, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, color = DarkTextSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SettingsAction(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color = Error,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Error, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, color = DarkTextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        Icon(Icons.Default.ChevronRight, null, tint = DarkTextSecondary)
    }
}
