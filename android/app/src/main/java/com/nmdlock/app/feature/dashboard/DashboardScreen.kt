package com.nmdlock.app.feature.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nmdlock.app.core.ui.components.*
import com.nmdlock.app.core.ui.theme.*

/**
 * Dashboard screen - main overview of device stats, license, and quick actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToGame: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToSupport: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "NMDLock 1.0",
                    style = MaterialTheme.typography.headlineMedium,
                    color = DarkText,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Tối ưu thiết bị • Nâng tầm trải nghiệm",
                    style = MaterialTheme.typography.bodySmall,
                    color = DarkTextSecondary,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onNavigateToSupport) {
                    Icon(Icons.Default.HeadsetMic, "Hỗ trợ", tint = DarkTextSecondary)
                }
                IconButton(onClick = onNavigateToSettings) {
                    Icon(Icons.Default.Settings, "Cài đặt", tint = DarkTextSecondary)
                }
            }
        }

        // Alerts
        uiState.alerts.forEach { alert ->
            AlertBanner(
                message = alert,
                type = if (alert.contains("khóa")) AlertType.ERROR else AlertType.WARNING,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Key Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Key status icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (uiState.licenseInfo?.isValid == true)
                                Success.copy(alpha = 0.15f)
                            else
                                Warning.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (uiState.licenseInfo?.isValid == true)
                            Icons.Default.VpnKey else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (uiState.licenseInfo?.isValid == true) Success else Warning,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (uiState.licenseInfo?.isValid == true) "Key hợp lệ" else "Chưa có key",
                        style = MaterialTheme.typography.titleSmall,
                        color = DarkText,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (uiState.licenseInfo?.remainingDays != null) {
                        Text(
                            text = "Còn ${uiState.licenseInfo.remainingDays} ngày",
                            style = MaterialTheme.typography.bodySmall,
                            color = DarkTextSecondary,
                        )
                    }
                }
                StatusBadge(
                    text = if (uiState.licenseInfo?.isValid == true) "ACTIVE" else "INACTIVE",
                    color = if (uiState.licenseInfo?.isValid == true) Success else Warning,
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Stats Grid
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard(
                label = "RAM",
                value = "—",
                icon = Icons.Default.Memory,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "Pin",
                value = "—",
                icon = Icons.Default.BatteryFull,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "Mạng",
                value = "—",
                icon = Icons.Default.Wifi,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Quick Actions
        SectionHeader(title = "Thao tác nhanh")
        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GradientButton(
                    text = "Tối ưu nhanh",
                    onClick = { viewModel.performQuickOptimize() },
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Bolt,
                )
                OutlinedButton(
                    onClick = onNavigateToGame,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Purple400),
                ) {
                    Icon(Icons.Default.SportsEsports, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Game Mode")
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // Features
        SectionHeader(title = "Tính năng")
        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FeatureCard(
                title = "Tối ưu hệ thống",
                description = "Dọn cache, quản lý RAM, tối ưu pin",
                icon = Icons.Default.Speed,
                onClick = { /* Navigate to optimization */ },
            )
            FeatureCard(
                title = "Kiểm tra mạng",
                description = "Ping, tốc độ, độ ổn định",
                icon = Icons.Default.WifiTethering,
                onClick = { /* Navigate to network */ },
            )
            FeatureCard(
                title = "Game Profile",
                description = "Free Fire & Free Fire Max",
                icon = Icons.Default.SportsEsports,
                onClick = onNavigateToGame,
            )
            FeatureCard(
                title = "Quản lý Key",
                description = "Kích hoạt, gia hạn, kiểm tra",
                icon = Icons.Default.VpnKey,
                onClick = { /* Navigate to key */ },
            )
        }
        Spacer(modifier = Modifier.height(80.dp)) // Bottom nav padding
    }
}
