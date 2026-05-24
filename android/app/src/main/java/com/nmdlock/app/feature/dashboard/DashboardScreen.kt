package com.nmdlock.app.feature.dashboard

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nmdlock.app.core.ui.components.*
import com.nmdlock.app.core.ui.theme.*

/**
 * Dashboard screen — real device data, license status, quick actions
 * Redesigned to match NMDLock-App-Preview-v2.html design language
 */
@Composable
fun DashboardScreen(
    onNavigateToGame: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToSupport: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiStateVal by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // ═══ HEADER — matches preview .app-header ═══
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
                    letterSpacing = (-0.5).sp,
                )
                Text(
                    text = "Toi uu hoa phan cung thong minh",
                    style = MaterialTheme.typography.bodySmall,
                    color = DarkTextSecondary,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Support button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkSurface)
                        .clickable(onClick = onNavigateToSupport),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.HeadsetMic, "Ho tro", tint = DarkTextSecondary, modifier = Modifier.size(20.dp))
                }
                // Settings button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkSurface)
                        .clickable(onClick = onNavigateToSettings),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Settings, "Cai dat", tint = DarkTextSecondary, modifier = Modifier.size(20.dp))
                }
            }
        }

        // ═══ ALERTS ═══
        uiStateVal.alerts.forEach { alert ->
            AlertBanner(
                message = alert,
                type = if (alert.contains("khoa")) AlertType.ERROR else AlertType.WARNING,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        // ═══ KEY STATUS CARD — matches preview .key-status ═══
        val licenseInfo = uiStateVal.licenseInfo
        KeyStatusCard(
            title = if (licenseInfo?.isValid == true) "Ban quyen: Hop le" else "Chua co key",
            subtitle = if (licenseInfo?.remainingDays != null) "Con ${licenseInfo.remainingDays} ngay" else "Kich hoat de su dung",
            isValid = licenseInfo?.isValid == true,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ═══ STATS GRID — matches preview .stats-grid ═══
        val stats = uiStateVal.systemStats
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard(
                label = "RAM",
                value = if (stats != null) "${stats.ramUsedPercent.toInt()}%" else "—",
                icon = Icons.Default.Memory,
                modifier = Modifier.weight(1f),
                valueColor = if (stats != null && stats.ramUsedPercent > 75) Warning else AccentCyan,
            )
            StatCard(
                label = "Pin",
                value = if (stats != null) "${stats.batteryPercent}%" else "—",
                icon = if (stats != null && stats.batteryPercent <= 20) Icons.Default.BatteryAlert else Icons.Default.BatteryFull,
                modifier = Modifier.weight(1f),
                valueColor = if (stats != null && stats.batteryPercent <= 20) Error else AccentCyan,
            )
            StatCard(
                label = "Ping",
                value = if (stats != null) "${stats.batteryTemp.toInt()}°C" else "—",
                icon = Icons.Default.Thermostat,
                modifier = Modifier.weight(1f),
                valueColor = if (stats != null && stats.batteryTemp > 40) Warning else AccentCyan,
            )
        }

        // Optimization result
        val optResult = uiStateVal.optimizationResult
        if (optResult != null) {
            Spacer(modifier = Modifier.height(8.dp))
            AlertBanner(
                message = optResult,
                type = AlertType.SUCCESS,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        // ═══ QUICK ACTIONS — matches preview .quick-actions ═══
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GradientButton(
                text = if (uiStateVal.isLoading) "Dang toi uu..." else "Toi uu nhanh",
                onClick = { viewModel.performQuickOptimize() },
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Bolt,
                enabled = !uiStateVal.isLoading,
            )
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkSurface)
                    .clickable { viewModel.refreshStats() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Refresh, "Lam moi", tint = AccentCyan, modifier = Modifier.size(20.dp))
            }
            Box(
                modifier = Modifier
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkSurface)
                    .clickable(onClick = onNavigateToGame)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SportsEsports, null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Game Mode", color = DarkText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ═══ FEATURES — matches preview .feature-list ═══
        SectionHeader(title = "Tinh nang cot loi")
        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FeatureCard(
                title = "Don dep & Giai phong RAM",
                description = "Dong tac vu ngam, giai phong dung luong dem cache",
                icon = Icons.Default.Speed,
                onClick = { /* Navigate to optimization */ },
            )
            FeatureCard(
                title = "Tang toc duong truyen mang",
                description = "BBR, TCP Fast Open toi uu goi tin game",
                icon = Icons.Default.WifiTethering,
                onClick = { /* Navigate to network */ },
            )
            FeatureCard(
                title = "Ho so Game Profile",
                description = "Toi uu chuyen sau cho Free Fire & FF Max",
                icon = Icons.Default.SportsEsports,
                onClick = onNavigateToGame,
            )
            FeatureCard(
                title = "Quan ly Key",
                description = "Kich hoat, gia han, kiem tra ban quyen",
                icon = Icons.Default.VpnKey,
                onClick = { /* Navigate to key */ },
            )
        }
        Spacer(modifier = Modifier.height(80.dp)) // Bottom nav padding
    }
}
