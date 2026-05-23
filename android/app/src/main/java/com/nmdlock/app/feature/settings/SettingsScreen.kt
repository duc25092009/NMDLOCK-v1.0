package com.nmdlock.app.feature.settings

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

// ── Data class & ViewModel (giu nguyen) ──

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

// ── SCREEN ──

@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ─── Header ───
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
                text = "Cai dat",
                style = MaterialTheme.typography.headlineMedium,
                color = DarkText,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ─── Giao dien ───
        SectionHeader(title = "Giao dien")
        Spacer(modifier = Modifier.height(8.dp))
        SettingsGlassCard {
            SettingsToggleRow(
                icon = Icons.Default.DarkMode,
                title = "Che do toi",
                subtitle = "Chuyen doi giao dien toi/sang",
                checked = uiState.isDarkTheme,
                onCheckedChange = { viewModel.setDarkTheme(it) },
                accentColor = AccentPurple,
            )
            SettingsDivider()
            SettingsClickRow(
                icon = Icons.Default.Language,
                title = "Ngon ngu",
                subtitle = if (uiState.language == "vi") "Tieng Viet" else "English",
                onClick = { viewModel.setLanguage(if (uiState.language == "vi") "en" else "vi") },
                accentColor = AccentCyan,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ─── Am thanh & Rung ───
        SectionHeader(title = "Am thanh & Rung")
        Spacer(modifier = Modifier.height(8.dp))
        SettingsGlassCard {
            SettingsToggleRow(
                icon = Icons.Default.VolumeUp,
                title = "Am thanh",
                subtitle = "Bat/tat hieu ung am thanh",
                checked = uiState.soundEnabled,
                onCheckedChange = { viewModel.setSoundEnabled(it) },
                accentColor = Success,
            )
            SettingsDivider()
            SettingsToggleRow(
                icon = Icons.Default.Vibration,
                title = "Rung",
                subtitle = "Bat/tat phan hoi rung",
                checked = uiState.vibrationEnabled,
                onCheckedChange = { viewModel.setVibrationEnabled(it) },
                accentColor = Warning,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ─── Dong bo & Thong bao ───
        SectionHeader(title = "Dong bo & Thong bao")
        Spacer(modifier = Modifier.height(8.dp))
        SettingsGlassCard {
            SettingsToggleRow(
                icon = Icons.Default.Notifications,
                title = "Thong bao",
                subtitle = "Nhan thong bao tu ung dung",
                checked = uiState.notificationsEnabled,
                onCheckedChange = { viewModel.setNotificationsEnabled(it) },
                accentColor = Info,
            )
            SettingsDivider()
            SettingsToggleRow(
                icon = Icons.Default.Sync,
                title = "Tu dong dong bo",
                subtitle = "Dong bo du lieu voi server",
                checked = uiState.autoSyncEnabled,
                onCheckedChange = { viewModel.setAutoSyncEnabled(it) },
                accentColor = AccentCyan,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ─── Go loi ───
        SectionHeader(title = "Go loi")
        Spacer(modifier = Modifier.height(8.dp))
        SettingsGlassCard {
            SettingsToggleRow(
                icon = Icons.Default.BugReport,
                title = "Ghi log",
                subtitle = "Ghi log hoat dong de chan doan",
                checked = uiState.loggingEnabled,
                onCheckedChange = { viewModel.setLoggingEnabled(it) },
                accentColor = Warning,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ─── Thong tin ung dung ───
        SectionHeader(title = "Thong tin ung dung")
        Spacer(modifier = Modifier.height(8.dp))
        SettingsGlassCard {
            SettingsInfoRow(
                icon = Icons.Default.Info,
                title = "Phien ban",
                subtitle = uiState.appVersion,
                iconTint = AccentPurple,
            )
            SettingsDivider()
            SettingsInfoRow(
                icon = Icons.Default.Cloud,
                title = "Server",
                subtitle = "Truc tuyen (Online)",
                iconTint = Success,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ─── Du lieu ───
        SectionHeader(title = "Du lieu")
        Spacer(modifier = Modifier.height(8.dp))
        SettingsGlassCard {
            SettingsDangerRow(
                icon = Icons.Default.RestartAlt,
                title = "Reset ung dung",
                subtitle = "Xoa toan bo du lieu cuc bo",
                onClick = { viewModel.resetApp() },
            )
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

// ── COMPONENTS ──

@Composable
private fun SettingsGlassCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp),
            content = content,
        )
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(0.5.dp)
            .background(Color.White.copy(alpha = 0.06f))
    )
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accentColor: Color = AccentPurple,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (checked) accentColor.copy(alpha = 0.15f) else DarkSurface2),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon, null,
                tint = if (checked) accentColor else DarkTextSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = DarkText,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                subtitle,
                color = DarkTextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accentColor,
                uncheckedThumbColor = DarkTextSecondary,
                uncheckedTrackColor = DarkSurface2,
            ),
        )
    }
}

@Composable
private fun SettingsClickRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    accentColor: Color = AccentCyan,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurface2),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = accentColor, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = DarkText, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, color = DarkTextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        Icon(
            Icons.Default.ChevronRight, null,
            tint = DarkTextSecondary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun SettingsInfoRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color = AccentPurple,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurface2),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = DarkText, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, color = DarkTextSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SettingsDangerRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Error.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = Error, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Error, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, color = DarkTextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        Icon(
            Icons.Default.ChevronRight, null,
            tint = DarkTextSecondary,
            modifier = Modifier.size(20.dp),
        )
    }
}
