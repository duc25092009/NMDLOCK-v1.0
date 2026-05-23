package com.nmdlock.app.feature.game

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nmdlock.app.core.services.*
import com.nmdlock.app.core.ui.components.*
import com.nmdlock.app.core.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Game Profile Screen v3.0 — Tích hợp đầy đủ:
 * - PID Controller | Thermal Prediction | Smart App Killer
 * - Burst Speed Test | Network TCP | Auto-Learner
 * - Queue Stats | Game Lifecycle | Crosshair | DNS | DND
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameProfileScreen(
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val viewModel: GameViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedProfile by remember { mutableIntStateOf(0) }
    var showCrosshairSettings by remember { mutableStateOf(false) }
    var showDnsSettings by remember { mutableStateOf(false) }

    // Crosshair state
    var crosshairEnabled by remember { mutableStateOf(false) }
    var crosshairSize by remember { mutableFloatStateOf(40f) }
    var crosshairColorIndex by remember { mutableIntStateOf(0) }
    var crosshairOpacity by remember { mutableFloatStateOf(0.8f) }
    var crosshairStyle by remember { mutableIntStateOf(0) }

    // DNS state
    var selectedDnsIndex by remember { mutableIntStateOf(0) }

    // UI expand states
    var showPidDetails by remember { mutableStateOf(false) }
    var showThermalPredictionDetails by remember { mutableStateOf(false) }
    var showSmartKillDetails by remember { mutableStateOf(false) }
    var showSpeedTestDetails by remember { mutableStateOf(false) }

    val crosshairColors = listOf(Color.Red, Color.Green, Color.Cyan, Color.Yellow, Color(0xFFFF5722), Color.White)
    val crosshairStyles = listOf("Vòng tròn", "Dấu cộng", "Chấm", "Chữ thập")
    val crosshairStyleIcons = listOf(Icons.Default.Circle, Icons.Default.Add, Icons.Default.FiberManualRecord, Icons.Default.Close)

    val profileNames = listOf("Mượt", "Cân bằng", "Hiệu năng", "Tiết kiệm pin")
    val profileIcons = listOf(Icons.Default.Speed, Icons.Default.Balance, Icons.Default.Bolt, Icons.Default.BatterySaver)
    val profileDescs = listOf("Giảm đồ họa, tăng FPS", "Đồ họa TB, FPS ổn định", "Đồ họa cao, FPS tối đa", "Chơi lâu, tiết kiệm pin")
    val profileDetails = listOf("720p + MSAA + Kill apps", "MSAA + WiFi lock + Kill MXH", "480p/720p + MSAA + 120Hz + Kill all", "Reset về mặc định")
    val dnsProviders = viewModel.getDnsProviders()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // ═══ HEADER ═══
        HeaderSection(onBack = onBack)

        // ═══ FF / FF MAX TABS ═══
        GameTabs(selectedTab = selectedTab) { selectedTab = it }

        Spacer(modifier = Modifier.height(16.dp))

        // ═══ STATUS BAR (FPS + Nhiệt + DNS + DND + PID) ═══
        StatusBarSection(uiState = uiState)

        Spacer(modifier = Modifier.height(20.dp))

        // ════════════════════════════════════════════════
        // PHẦN 1: PID PERFORMANCE MONITOR (v3.0)
        // ════════════════════════════════════════════════
        SectionHeader(title = "⚙️ PID Performance Monitor")
        Spacer(modifier = Modifier.height(8.dp))
        PidPerformanceCard(
            pidOutput = uiState.pidOutput,
            pidEnabled = uiState.pidEnabled,
            onToggle = { viewModel.togglePid() },
            expanded = showPidDetails,
            onToggleExpand = { showPidDetails = !showPidDetails },
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ════════════════════════════════════════════════
        // PHẦN 2: THERMAL PREDICTION ENGINE (v3.0)
        // ════════════════════════════════════════════════
        SectionHeader(title = "🌡️ Thermal Prediction Engine")
        Spacer(modifier = Modifier.height(8.dp))
        ThermalPredictionCard(
            prediction = uiState.thermalPrediction,
            state = uiState.thermalState,
            expanded = showThermalPredictionDetails,
            onToggleExpand = { showThermalPredictionDetails = !showThermalPredictionDetails },
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ════════════════════════════════════════════════
        // PHẦN 3: GAME PROFILES (existing)
        // ════════════════════════════════════════════════
        SectionHeader(title = "🎮 Chọn profile tối ưu")
        Spacer(modifier = Modifier.height(8.dp))

        ProfileCards(
            profileNames = profileNames,
            profileIcons = profileIcons,
            profileDescs = profileDescs,
            profileDetails = profileDetails,
            selectedProfile = selectedProfile,
            onSelect = { selectedProfile = it },
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Apply profile button
        ApplyProfileButton(
            isApplying = uiState.isApplying,
            onClick = { viewModel.applyProfile(profileNames[selectedProfile], selectedTab == 1) },
        )

        // Applied actions feedback
        AppliedActionsCard(
            actions = uiState.appliedActions,
            isSuccess = uiState.isSuccess,
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ════════════════════════════════════════════════
        // PHẦN 4: SMART APP KILLER (v3.0)
        // ════════════════════════════════════════════════
        SectionHeader(title = "🔪 Smart App Killer")
        Spacer(modifier = Modifier.height(8.dp))
        SmartAppKillerCard(
            killResult = uiState.smartKillResult,
            aggressive = uiState.smartKillAggressive,
            onRun = { viewModel.runSmartKill() },
            onToggleAggressive = { viewModel.toggleSmartKillAggressive() },
            expanded = showSmartKillDetails,
            onToggleExpand = { showSmartKillDetails = !showSmartKillDetails },
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ════════════════════════════════════════════════
        // PHẦN 5: BURST SPEED TEST (v3.0)
        // ════════════════════════════════════════════════
        SectionHeader(title = "📶 Burst Speed Test")
        Spacer(modifier = Modifier.height(8.dp))
        BurstSpeedTestCard(
            speedResult = uiState.speedResult,
            progress = uiState.speedProgress,
            onRun = { viewModel.runSpeedTest() },
            expanded = showSpeedTestDetails,
            onToggleExpand = { showSpeedTestDetails = !showSpeedTestDetails },
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ════════════════════════════════════════════════
        // PHẦN 6: NETWORK TCP OPTIMIZATION (v3.0)
        // ════════════════════════════════════════════════
        SectionHeader(title = "🌐 Network TCP Optimization")
        Spacer(modifier = Modifier.height(8.dp))
        NetworkTcpCard(
            enabled = uiState.tcpOptimized,
            onToggle = { viewModel.toggleTcpOptimization() },
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ════════════════════════════════════════════════
        // PHẦN 7: AUTO-LEARNER STATS (v3.0)
        // ════════════════════════════════════════════════
        SectionHeader(title = "🤖 Auto-Learner (Multi-Armed Bandit)")
        Spacer(modifier = Modifier.height(8.dp))
        AutoLearnerCard(
            bestProfiles = uiState.bestProfiles,
            currentArm = uiState.currentArm,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ════════════════════════════════════════════════
        // PHẦN 8: TOUCH SENSITIVITY BOOSTER (qwen v2)
        // ════════════════════════════════════════════════
        SectionHeader(title = "🖐️ Touch Sensitivity Booster")
        Spacer(modifier = Modifier.height(8.dp))

        // Touch Sensitivity card
        TouchSensitivityCard(
            currentLevel = uiState.touchSensitivityLevel,
            isEnabled = uiState.touchSensitivityEnabled,
            onApplySensitivity = { viewModel.applyTouchSensitivity(it) },
            onDisable = { viewModel.disableTouchSensitivity() },
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ════════════════════════════════════════════════
        // PHẦN 9: INPUT LATENCY REDUCER + GAME TOUCH (qwen v2)
        // ════════════════════════════════════════════════
        SectionHeader(title = "⚡ Input Latency & Game Touch")
        Spacer(modifier = Modifier.height(8.dp))

        // Zero-Latency Mode toggle
        ZeroLatencyCard(
            enabled = uiState.zeroLatencyEnabled,
            onToggle = { viewModel.toggleZeroLatencyMode() },
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Game Touch Optimization toggle
        GameTouchOptimizationCard(
            enabled = uiState.gameTouchOptimized,
            onApply = { viewModel.applyGameTouchOptimization() },
            onRestore = { viewModel.restoreGameTouchOptimization() },
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Touch Metrics overlay toggle
        TouchMetricsCard(
            enabled = uiState.touchMetricsEnabled,
            latencyGrade = uiState.touchLatencyGrade,
            onToggle = { viewModel.toggleTouchMetrics(context) },
            onRefresh = { viewModel.refreshTouchLatencyGrade() },
        )

        // Applied touch actions feedback
        AnimatedVisibility(visible = uiState.touchActions.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(14.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Touch Settings Applied:", color = Success, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    uiState.touchActions.forEach { action ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                            Icon(Icons.Default.CheckCircle, null, tint = Success, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(action, color = DarkTextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ════════════════════════════════════════════════
        // PHẦN 10: QUICK TOGGLES + CROSSHAIR + DNS + THERMAL
        // (existing sections)
        // ════════════════════════════════════════════════

        SectionHeader(title = "🔘 Bật tắt nhanh")
        Spacer(modifier = Modifier.height(8.dp))

        // FPS Counter toggle
        val fpsRunning = FpsOverlayService.isRunning()
        QuickToggleCard(
            icon = Icons.Default.Speed,
            title = "FPS Counter",
            subtitle = if (fpsRunning) "Đang hiển thị: ${FpsOverlayService.getCurrentFps()} fps" else "Hiển thị FPS thực tế",
            isEnabled = fpsRunning,
            onToggle = { viewModel.toggleFps(context) },
            accentColor = if (fpsRunning && FpsOverlayService.getCurrentFps() >= 55) Success
                else if (fpsRunning) Warning else DarkTextSecondary,
        )

        // DND toggle
        QuickToggleCard(
            icon = Icons.Default.NotificationsOff,
            title = "Chế độ không làm phiền",
            subtitle = if (uiState.dndEnabled) "Đã chặn thông báo" else "Tự động chặn thông báo",
            isEnabled = uiState.dndEnabled,
            onToggle = { viewModel.toggleDnd() },
            accentColor = if (uiState.dndEnabled) Warning else DarkTextSecondary,
        )

        // Auto-update toggle
        QuickToggleCard(
            icon = Icons.Default.SystemUpdateAlt,
            title = "Tắt cập nhật tự động",
            subtitle = if (uiState.autoUpdatesDisabled) "Đã tắt cập nhật" else "Tắt cập nhật Play Store",
            isEnabled = uiState.autoUpdatesDisabled,
            onToggle = { viewModel.toggleAutoUpdates() },
            accentColor = if (uiState.autoUpdatesDisabled) Error else DarkTextSecondary,
        )

        // Game Lifecycle toggle
        QuickToggleCard(
            icon = Icons.Default.Sensors,
            title = "Tự động phát hiện game",
            subtitle = if (uiState.lifecycleActive) {
                if (uiState.currentGame != null) "Đang chơi: ${gameShortName(uiState.currentGame!!)}"
                else "Đang chờ vào game..."
            } else "Tắt phát hiện tự động",
            isEnabled = uiState.lifecycleActive,
            onToggle = {
                if (uiState.lifecycleActive) viewModel.stopAutoDetection()
                else viewModel.startAutoDetection()
            },
            accentColor = if (uiState.lifecycleActive) Success else DarkTextSecondary,
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ═══ CROSSHAIR ═══
        CrosshairSection(
            crosshairEnabled = crosshairEnabled,
            crosshairSize = crosshairSize,
            crosshairColorIndex = crosshairColorIndex,
            crosshairOpacity = crosshairOpacity,
            crosshairStyle = crosshairStyle,
            crosshairColors = crosshairColors,
            crosshairStyles = crosshairStyles,
            crosshairStyleIcons = crosshairStyleIcons,
            onToggle = { crosshairEnabled = it },
            onSizeChange = { crosshairSize = it },
            onColorIndexChange = { crosshairColorIndex = it },
            onOpacityChange = { crosshairOpacity = it },
            onStyleChange = { crosshairStyle = it },
            onApply = {
                val ctx = com.nmdlock.app.core.NMDLockApplication.instance
                if (crosshairEnabled) CrosshairOverlayService.start(ctx) else CrosshairOverlayService.stop(ctx)
            },
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ═══ DNS CHANGER ═══
        DnsChangerSection(
            currentDns = uiState.currentDns,
            dnsApplied = uiState.dnsApplied,
            dnsProviders = dnsProviders,
            selectedDnsIndex = selectedDnsIndex,
            dnsMessage = uiState.dnsMessage,
            onSelectProvider = { selectedDnsIndex = it },
            onApply = { viewModel.applyDns(dnsProviders[selectedDnsIndex].name) },
            onDisable = { viewModel.disableDns() },
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ═══ THERMAL MONITOR (legacy) ═══
        ThermalMonitorSection(thermalInfo = uiState.thermalInfo)

        Spacer(modifier = Modifier.height(20.dp))

        // ═══ QUEUE STATS (v3.0) ═══
        SectionHeader(title = "📊 Shizuku Command Queue")
        Spacer(modifier = Modifier.height(8.dp))
        CommandQueueCard(stats = uiState.queueStats)

        Spacer(modifier = Modifier.height(20.dp))

        // ═══ RESET ═══
        GradientButton(
            text = "Khôi phục cài đặt gốc",
            onClick = { viewModel.resetSettings() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            icon = Icons.Default.Restore,
            enabled = !uiState.isApplying,
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ═══ PERMISSION GUIDE ═══
        PermissionGuide()

        Spacer(modifier = Modifier.height(40.dp))
    }
}

// ══════════════════════════════════════════════════════════════
// SECTION COMPOSABLES
// ══════════════════════════════════════════════════════════════

@Composable
private fun HeaderSection(onBack: () -> Unit) {
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
        Column {
            Text("Game Profile", style = MaterialTheme.typography.headlineMedium, color = DarkText, fontWeight = FontWeight.Bold)
            Text("Tối ưu Free Fire & Free Fire Max", style = MaterialTheme.typography.bodySmall, color = DarkTextSecondary)
        }
    }
}

@Composable
private fun GameTabs(selectedTab: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf("Free Fire", "Free Fire Max").forEachIndexed { index, title ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .then(
                        if (selectedTab == index) Modifier.background(Brush.horizontalGradient(listOf(Purple600, Purple400)))
                        else Modifier.background(DarkSurface2)
                    )
                    .clickable { onSelect(index) }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(title, color = if (selectedTab == index) Color.White else DarkTextSecondary,
                    fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun StatusBarSection(uiState: GameUiState) {
    val fpsRunning = FpsOverlayService.isRunning()
    val tempInfo = uiState.thermalInfo
    val tempColor = when {
        tempInfo.isThrottling -> Error
        tempInfo.throttlingLevel == ThermalMonitor.ThrottleLevel.WARM -> Warning
        else -> Success
    }
    val tempDisplay = if (tempInfo.cpuTemp != null) "${tempInfo.cpuTemp.toInt()}°C" else "--"
    val dnsShort = when {
        uiState.currentDns.contains("Cloudflare") -> "CF"
        uiState.currentDns.contains("Google") -> "GG"
        uiState.currentDns.contains("AdGuard") -> "AG"
        uiState.currentDns.contains("Quad9") -> "Q9"
        uiState.currentDns.contains("Tắt") || uiState.currentDns.isEmpty() -> "Tắt"
        else -> "MĐ"
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        MiniStatusChip("FPS", if (fpsRunning) "${FpsOverlayService.getCurrentFps()}" else "Tắt",
            if (fpsRunning && FpsOverlayService.getCurrentFps() >= 55) Success else if (fpsRunning) Warning else DarkTextSecondary)
        MiniStatusChip("Nhiệt", tempDisplay, tempColor)
        MiniStatusChip("DNS", dnsShort, if (uiState.dnsApplied) Purple400 else DarkTextSecondary)
        MiniStatusChip("PID", if (uiState.pidEnabled) "Bật" else "Tắt",
            if (uiState.pidEnabled) Info else DarkTextSecondary)
        MiniStatusChip("Kill", if (uiState.smartKillResult != null) "${uiState.smartKillResult!!.killedCount}" else "0",
            if ((uiState.smartKillResult?.killedCount ?: 0) > 0) Error else DarkTextSecondary)
        MiniStatusChip("Touch", if (uiState.touchSensitivityEnabled) "${uiState.touchSensitivityLevel.take(3)}" else "Tắt",
            if (uiState.touchSensitivityEnabled) Purple400 else DarkTextSecondary)
    }
}

// ══════════════════════════════════════════════════════════════
// 1. PID PERFORMANCE MONITOR
// ══════════════════════════════════════════════════════════════

@Composable
private fun PidPerformanceCard(
    pidOutput: CpuGovernorPID.PidOutput,
    pidEnabled: Boolean,
    onToggle: () -> Unit,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
) {
    FeatureCard(
        title = "PID Controller Auto-Tune",
        description = if (pidEnabled) "Đang điều chỉnh CPU theo FPS" else "Tắt — chạm để bật",
        icon = Icons.Default.Tune,
        onClick = onToggleExpand,
        iconTint = if (pidEnabled) Info else DarkTextSecondary,
        modifier = Modifier.padding(horizontal = 16.dp),
    )

    AnimatedVisibility(visible = expanded) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 4.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(14.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("PID Auto-Tune", color = DarkText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(checked = pidEnabled, onCheckedChange = { onToggle() },
                        colors = SwitchDefaults.colors(checkedThumbColor = Info, checkedTrackColor = Info.copy(alpha = 0.5f)))
                }
                Spacer(modifier = Modifier.height(12.dp))

                // FPS gauge
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GaugeItem("Target FPS", "${pidOutput.targetFps.toInt()}", Success)
                    GaugeItem("Current FPS", "${pidOutput.currentFps.toInt()}", infoColor(pidOutput.currentFps))
                    GaugeItem("Error", "%.1f".format(pidOutput.error), if (pidOutput.error < 10) Success else Warning)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // PID Terms
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniTerm("P", "%.1f".format(pidOutput.pTerm), Purple400)
                    MiniTerm("I", "%.1f".format(pidOutput.iTerm), Info)
                    MiniTerm("D", "%.1f".format(pidOutput.dTerm), Warning)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Current config
                val cfg = pidOutput.config
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Config: ", color = DarkTextSecondary, fontSize = 12.sp)
                    StatusBadge("${cfg.name} (${cfg.description})",
                        color = when (cfg.name) {
                            "extreme" -> Error
                            "gaming" -> Warning
                            "balanced" -> Info
                            "efficient" -> Success
                            else -> DarkTextSecondary
                        })
                }
                Text("Output: %.1f".format(pidOutput.output), color = DarkTextSecondary, fontSize = 12.sp)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// 2. THERMAL PREDICTION ENGINE
// ══════════════════════════════════════════════════════════════

@Composable
private fun ThermalPredictionCard(
    prediction: ThermalPrediction,
    state: ThermalState,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
) {
    FeatureCard(
        title = "Dự đoán nhiệt độ thông minh",
        description = when (state) {
            ThermalState.CRITICAL -> "🔥 NGUY HIỂM! Đang thực thi emergency action!"
            ThermalState.WARNING -> "⚠️ Nhiệt độ tăng — đang áp dụng warning action"
            ThermalState.NORMAL -> "✅ Bình thường — slope: %.1f°C/phút".format(prediction.slope)
        },
        icon = Icons.Default.Thermostat,
        onClick = onToggleExpand,
        iconTint = when (state) {
            ThermalState.CRITICAL -> Error
            ThermalState.WARNING -> Warning
            ThermalState.NORMAL -> Success
        },
        modifier = Modifier.padding(horizontal = 16.dp),
    )

    AnimatedVisibility(visible = expanded) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 4.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(14.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // State indicator
                val stateColor = when (state) {
                    ThermalState.CRITICAL -> Error
                    ThermalState.WARNING -> Warning
                    ThermalState.NORMAL -> Success
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(state.name, stateColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Risk: %.0f%%".format(prediction.riskScore * 100), color = DarkTextSecondary, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Temperature values
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GaugeItem("Hiện tại", "%.0f°C".format(prediction.currentTemp),
                        if (prediction.currentTemp > 42) Error else if (prediction.currentTemp > 38) Warning else Success)
                    GaugeItem("EMA (lọc)", "%.1f°C".format(prediction.emaTemp), Info)
                    GaugeItem("Kalman", "%.1f°C".format(prediction.kalmanTemp), Purple400)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Predictions
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GaugeItem("Slope", "%.1f°C/ph".format(prediction.slope),
                        if (prediction.slope > 1) Error else if (prediction.slope > 0.3) Warning else Success)
                    GaugeItem("Dự đoán 5 ph", "%.0f°C".format(prediction.predictedTemp5Min),
                        if (prediction.predictedTemp5Min > 45) Error else if (prediction.predictedTemp5Min > 40) Warning else Success)
                    GaugeItem("Còn", timeToStr(prediction.timeToThrottleSeconds),
                        if (prediction.timeToThrottleSeconds < 120) Error else if (prediction.timeToThrottleSeconds < 300) Warning else Success)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Risk score bar
                Text("Risk Score", color = DarkTextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = prediction.riskScore.coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = when {
                        prediction.riskScore > 0.7f -> Error
                        prediction.riskScore > 0.4f -> Warning
                        else -> Success
                    },
                    trackColor = DarkSurface2,
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Z-Score anomaly
                Row {
                    Text("Z-Score: %.2f".format(prediction.zScore), color = DarkTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    if (prediction.isAnomaly) {
                        Text("⚠️ BẤT THƯỜNG!", color = Error, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // GPU + Battery temp
                Text("GPU: %.0f°C  Battery: %.0f°C".format(prediction.gpuTemp, prediction.batteryTemp),
                    color = DarkTextSecondary, fontSize = 11.sp)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// 3. SMART APP KILLER
// ══════════════════════════════════════════════════════════════

@Composable
private fun SmartAppKillerCard(
    killResult: SmartAppKiller.KillResult?,
    aggressive: Boolean,
    onRun: () -> Unit,
    onToggleAggressive: () -> Unit,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
) {
    FeatureCard(
        title = "Thông minh — PageRank-inspired",
        description = killResult?.let {
            "Đã kill ${it.killedCount} apps (tổng: ${it.totalKilledSession})"
        } ?: "Chạy để dọn app ngầm không cần thiết",
        icon = Icons.Default.Block,
        onClick = onToggleExpand,
        iconTint = if (killResult != null) Error else DarkTextSecondary,
        modifier = Modifier.padding(horizontal = 16.dp),
    )

    AnimatedVisibility(visible = expanded) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 4.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(14.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GradientButton(
                        text = if (killResult?.killedCount ?: 0 > 0) "Kill again" else "▶ Kill ngay",
                        onClick = onRun,
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Block,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Aggressive", color = DarkTextSecondary, fontSize = 10.sp)
                        Switch(checked = aggressive, onCheckedChange = { onToggleAggressive() },
                            colors = SwitchDefaults.colors(checkedThumbColor = Error, checkedTrackColor = Error.copy(alpha = 0.5f)))
                    }
                }

                // Results
                killResult?.let { result ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GaugeItem("Killed", "${result.killedCount}", Error)
                        GaugeItem("Errors", "${result.errors.size}", if (result.errors.isEmpty()) Success else Error)
                        GaugeItem("Session", "${result.totalKilledSession}", Warning)
                    }
                    if (result.killed.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Đã kill:", color = DarkTextSecondary, fontSize = 12.sp)
                        result.killed.take(10).forEach { pkg ->
                            Text("• ${pkg.substringAfterLast('.')}", color = DarkTextSecondary, fontSize = 11.sp)
                        }
                        if (result.killed.size > 10) {
                            Text("... và ${result.killed.size - 10} apps khác", color = DarkTextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// 4. BURST SPEED TEST
// ══════════════════════════════════════════════════════════════

@Composable
private fun BurstSpeedTestCard(
    speedResult: BurstSpeedTester.SpeedResult,
    progress: Float,
    onRun: () -> Unit,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
) {
    FeatureCard(
        title = "8-thread concurrent speed test",
        description = if (speedResult.isRunning) "Đang test: %.0f%%".format(progress * 100)
            else if (speedResult.downloadMbps > 0) "DL: %.1f Mbps | Lat: %.0fms".format(speedResult.downloadMbps, speedResult.latencyMs)
            else "Kiểm tra tốc độ mạng thực tế",
        icon = Icons.Default.NetworkCheck,
        onClick = onToggleExpand,
        iconTint = if (speedResult.downloadMbps > 0) Success else DarkTextSecondary,
        modifier = Modifier.padding(horizontal = 16.dp),
    )

    AnimatedVisibility(visible = expanded) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 4.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(14.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                GradientButton(
                    text = if (speedResult.isRunning) "Đang test..." else "▶ Chạy Speed Test",
                    onClick = onRun,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.Speed,
                    enabled = !speedResult.isRunning,
                )

                if (speedResult.isRunning) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = progress.coerceIn(0f, 1f),
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = Purple400, trackColor = DarkSurface2
                    )
                    Text("Đang tải... %.0f%%".format(progress * 100), color = DarkTextSecondary, fontSize = 12.sp)
                }

                if (speedResult.downloadMbps > 0) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Download + Upload
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SpeedMeter("Download", speedResult.downloadMbps, "Mbps", Success)
                        SpeedMeter("Upload", speedResult.uploadMbps, "Mbps", Info)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Latency + Jitter + Packet Loss
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GaugeItem("Latency", "%.0fms".format(speedResult.latencyMs),
                            if (speedResult.latencyMs < 30) Success else if (speedResult.latencyMs < 80) Warning else Error)
                        GaugeItem("Jitter", "%.0fms".format(speedResult.jitterMs),
                            if (speedResult.jitterMs < 10) Success else if (speedResult.jitterMs < 30) Warning else Error)
                        GaugeItem("Loss", "%.1f%%".format(speedResult.packetLossPercent),
                            if (speedResult.packetLossPercent < 1) Success else Error)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Tail latency + Burst consistency
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GaugeItem("Tail (95th)", "%.0fms".format(speedResult.tailLatencyMs),
                            if (speedResult.tailLatencyMs < 100) Success else if (speedResult.tailLatencyMs < 300) Warning else Error)
                        GaugeItem("Consistency", "%.0f%%".format(speedResult.burstConsistency),
                            if (speedResult.burstConsistency > 90) Success else if (speedResult.burstConsistency > 70) Warning else Error)
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// 5. NETWORK TCP OPTIMIZATION
// ══════════════════════════════════════════════════════════════

@Composable
private fun NetworkTcpCard(
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (enabled) Warning.copy(alpha = 0.2f) else DarkSurface2),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Router, null, tint = if (enabled) Warning else DarkTextSecondary, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("BBR + TCP Fast Open", color = DarkText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(
                    if (enabled) "Đã tối ưu TCP cho gaming" else "Tối ưu kernel TCP giảm lag",
                    color = if (enabled) Warning else DarkTextSecondary, fontSize = 12.sp)
            }
            Switch(checked = enabled, onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(checkedThumbColor = Warning, checkedTrackColor = Warning.copy(alpha = 0.5f)))
        }
    }
}

// ══════════════════════════════════════════════════════════════
// 6. AUTO-LEARNER STATS
// ══════════════════════════════════════════════════════════════

@Composable
private fun AutoLearnerCard(
    bestProfiles: List<com.nmdlock.app.data.local.ProfileStatsEntity>,
    currentArm: String?,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Psychology, null, tint = Purple400, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Epsilon-Greedy Multi-Armed Bandit", color = DarkText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Khám phá: 10% | Khai thác: 90%", color = DarkTextSecondary, fontSize = 12.sp)
            Text("Arm hiện tại: ${currentArm ?: "chưa chọn"}", color = if (currentArm != null) Purple400 else DarkTextSecondary, fontSize = 12.sp)

            if (bestProfiles.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                bestProfiles.forEach { profile ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        StatusBadge(profile.profileId.take(18), color = when {
                            profile.avgReward > 1.5f -> Success
                            profile.avgReward > 1.0f -> Info
                            else -> DarkTextSecondary
                        })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reward: %.2f (x%s)".format(profile.avgReward, profile.totalTrials),
                            color = DarkTextSecondary, fontSize = 11.sp)
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Chưa có dữ liệu — chơi game để thu thập!", color = DarkTextSecondary, fontSize = 11.sp)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// 7. COMMAND QUEUE STATS
// ══════════════════════════════════════════════════════════════

@Composable
private fun CommandQueueCard(stats: ShizukuCommandQueue.QueueStats) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            CqStat("Hàng đợi", "${stats.queued}", Purple400)
            CqStat("Đã chạy", "${stats.executed}", Success)
            CqStat("TB chạy", "${stats.avgExecutionTime}ms", Info)
            CqStat("Lỗi", "${stats.errorCount}", if (stats.errorCount > 0) Error else DarkTextSecondary)
            CqStat("Throughput", "%.1f/s".format(stats.throughputPerSecond), Warning)
        }
    }
}

@Composable
private fun CqStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(label, color = DarkTextSecondary, fontSize = 9.sp)
    }
}

// ══════════════════════════════════════════════════════════════
// EXISTING COMPOSABLES (giữ nguyên)
// ══════════════════════════════════════════════════════════════

@Composable
private fun ProfileCards(
    profileNames: List<String>,
    profileIcons: List<ImageVector>,
    profileDescs: List<String>,
    profileDetails: List<String>,
    selectedProfile: Int,
    onSelect: (Int) -> Unit,
) {
    profileNames.forEachIndexed { index, name ->
        val isSelected = selectedProfile == index
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) Purple600.copy(alpha = 0.15f) else DarkSurface),
            shape = RoundedCornerShape(14.dp),
            onClick = { onSelect(index) },
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Purple600.copy(alpha = 0.3f) else DarkSurface2),
                    contentAlignment = Alignment.Center,
                ) { Icon(profileIcons[index], null, tint = if (isSelected) Purple400 else DarkTextSecondary, modifier = Modifier.size(22.dp)) }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(name, color = DarkText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(profileDescs[index], color = DarkTextSecondary, fontSize = 12.sp)
                    Text(profileDetails[index], color = DarkTextSecondary.copy(alpha = 0.7f), fontSize = 11.sp)
                }
                if (isSelected) {
                    Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Success), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ApplyProfileButton(isApplying: Boolean, onClick: () -> Unit) {
    GradientButton(
        text = if (isApplying) "Đang áp dụng..." else "Áp dụng cấu hình",
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        icon = Icons.Default.PlayArrow,
        enabled = !isApplying,
    )
}

@Composable
private fun AppliedActionsCard(actions: List<String>, isSuccess: Boolean) {
    AnimatedVisibility(visible = actions.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(14.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(if (isSuccess) "✅ Đã áp dụng:" else "❌ Lỗi:", color = if (isSuccess) Success else Error,
                    fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                actions.forEach { action ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                        Icon(if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Warning, null,
                            tint = if (isSuccess) Success else Error, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(action, color = DarkTextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CrosshairSection(
    crosshairEnabled: Boolean,
    crosshairSize: Float,
    crosshairColorIndex: Int,
    crosshairOpacity: Float,
    crosshairStyle: Int,
    crosshairColors: List<Color>,
    crosshairStyles: List<String>,
    crosshairStyleIcons: List<ImageVector>,
    onToggle: (Boolean) -> Unit,
    onSizeChange: (Float) -> Unit,
    onColorIndexChange: (Int) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onStyleChange: (Int) -> Unit,
    onApply: () -> Unit,
) {
    SectionHeader(title = "🎯 Tâm ngắm (Crosshair)")
    Spacer(modifier = Modifier.height(8.dp))

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(DarkSurface2),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.MyLocation, null, tint = Purple400, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Tâm ngắm ảo", color = DarkText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(if (crosshairEnabled) "Đang hiển thị" else "Hiển thị trên mọi ứng dụng",
                    color = if (crosshairEnabled) Success else DarkTextSecondary, fontSize = 12.sp)
            }
            Switch(checked = crosshairEnabled, onCheckedChange = { onToggle(it) },
                colors = SwitchDefaults.colors(checkedThumbColor = Purple400, checkedTrackColor = Purple600.copy(alpha = 0.5f)))
        }
    }

    AnimatedVisibility(visible = crosshairEnabled) {
        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface), shape = RoundedCornerShape(14.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Preview
                val cColor = crosshairColors[crosshairColorIndex]
                Box(modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(12.dp)).background(DarkSurface2),
                    contentAlignment = Alignment.Center) {
                    when (crosshairStyle) {
                        0 -> { Box(modifier = Modifier.size(crosshairSize.dp).border(1.5.dp, cColor.copy(alpha = crosshairOpacity), CircleShape))
                            Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(cColor)) }
                        1 -> { Box(modifier = Modifier.width(crosshairSize.dp).height(1.5.dp).background(cColor.copy(alpha = crosshairOpacity)))
                            Box(modifier = Modifier.width(1.5.dp).height(crosshairSize.dp).background(cColor.copy(alpha = crosshairOpacity)))
                            Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(cColor)) }
                        2 -> { Box(modifier = Modifier.size(crosshairSize.dp * 0.4f).clip(CircleShape).background(cColor.copy(alpha = crosshairOpacity))) }
                        3 -> { val g = crosshairSize.dp; val s = g * 0.4f
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(modifier = Modifier.width(1.5.dp).height(s).background(cColor.copy(alpha = crosshairOpacity)))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.width(s).height(1.5.dp).background(cColor.copy(alpha = crosshairOpacity)))
                                    Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(cColor))
                                    Box(modifier = Modifier.width(s).height(1.5.dp).background(cColor.copy(alpha = crosshairOpacity))) }
                                Box(modifier = Modifier.width(1.5.dp).height(s).background(cColor.copy(alpha = crosshairOpacity))) } }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Style
                Text("Kiểu tâm ngắm", color = DarkTextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    crosshairStyles.forEachIndexed { index, styleName ->
                        val isSelected = crosshairStyle == index
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clip(RoundedCornerShape(10.dp))
                                .then(if (isSelected) Modifier.background(Purple600.copy(alpha = 0.3f)) else Modifier)
                                .clickable { onStyleChange(index) }.padding(8.dp)) {
                            Icon(crosshairStyleIcons[index], null, tint = if (isSelected) Purple400 else DarkTextSecondary, modifier = Modifier.size(24.dp))
                            Text(styleName, color = if (isSelected) Purple400 else DarkTextSecondary, fontSize = 10.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Kích thước", color = DarkTextSecondary, fontSize = 13.sp)
                Slider(value = crosshairSize, onValueChange = { onSizeChange(it) }, valueRange = 20f..80f,
                    colors = SliderDefaults.colors(thumbColor = Purple400, activeTrackColor = Purple600, inactiveTrackColor = DarkSurface2))

                Spacer(modifier = Modifier.height(8.dp))
                Text("Màu sắc", color = DarkTextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    crosshairColors.forEachIndexed { index, color ->
                        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(color)
                            .border(2.dp, if (crosshairColorIndex == index) Color.White.copy(alpha = 0.8f) else Color.Transparent, CircleShape)
                            .clickable { onColorIndexChange(index) })
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Độ trong suốt", color = DarkTextSecondary, fontSize = 13.sp)
                Slider(value = crosshairOpacity, onValueChange = { onOpacityChange(it) }, valueRange = 0.2f..1.0f,
                    colors = SliderDefaults.colors(thumbColor = Purple400, activeTrackColor = Purple600, inactiveTrackColor = DarkSurface2))

                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GradientButton(text = "Áp dụng", onClick = onApply, modifier = Modifier.weight(1f), icon = Icons.Default.Check)
                    OutlinedButton(onClick = { onToggle(false) }, modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Error)) { Text("Tắt") }
                }
            }
        }
    }
}

@Composable
private fun DnsChangerSection(
    currentDns: String,
    dnsApplied: Boolean,
    dnsProviders: List<DnsManager.DnsProvider>,
    selectedDnsIndex: Int,
    dnsMessage: String,
    onSelectProvider: (Int) -> Unit,
    onApply: () -> Unit,
    onDisable: () -> Unit,
) {
    SectionHeader(title = "🌐 DNS Changer")
    Spacer(modifier = Modifier.height(8.dp))

    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface), shape = RoundedCornerShape(14.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(DarkSurface2),
                    contentAlignment = Alignment.Center) { Icon(Icons.Default.Dns, null, tint = Purple400, modifier = Modifier.size(22.dp)) }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("DNS Server", color = DarkText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text("Hiện tại: $currentDns", color = if (dnsApplied) Purple400 else DarkTextSecondary, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            dnsProviders.forEachIndexed { index, provider ->
                val isSelected = selectedDnsIndex == index
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) Purple600.copy(alpha = 0.15f) else Color.Transparent)
                    .clickable { onSelectProvider(index) }.padding(vertical = 10.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = isSelected, onClick = { onSelectProvider(index) },
                        colors = RadioButtonDefaults.colors(selectedColor = Purple400))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(provider.name, color = DarkText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(provider.ipDisplay, color = DarkTextSecondary, fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GradientButton(text = "Áp dụng DNS", onClick = onApply, modifier = Modifier.weight(1f), icon = Icons.Default.Check)
                OutlinedButton(onClick = onDisable, modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkTextSecondary)) { Text("Tắt DNS") }
            }
            if (dnsMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(dnsMessage, color = if (dnsApplied) Success else Error, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ThermalMonitorSection(thermalInfo: ThermalMonitor.ThermalInfo) {
    SectionHeader(title = "🌡️ Nhiệt độ thiết bị")
    Spacer(modifier = Modifier.height(8.dp))

    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface), shape = RoundedCornerShape(14.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ThermalChip("CPU", thermalInfo.cpuTemp,
                    if ((thermalInfo.cpuTemp ?: 0f) >= 75) Error else if ((thermalInfo.cpuTemp ?: 0f) >= 65) Warning else Success)
                ThermalChip("GPU", thermalInfo.gpuTemp,
                    if ((thermalInfo.gpuTemp ?: 0f) >= 70) Error else if ((thermalInfo.gpuTemp ?: 0f) >= 60) Warning else Success)
                ThermalChip("Pin", thermalInfo.batteryTemp,
                    if ((thermalInfo.batteryTemp ?: 0f) >= 45) Error else if ((thermalInfo.batteryTemp ?: 0f) >= 40) Warning else Success)
            }
            if (thermalInfo.isThrottling) {
                Spacer(modifier = Modifier.height(12.dp))
                val warningLevel = when (thermalInfo.throttlingLevel) {
                    ThermalMonitor.ThrottleLevel.CRITICAL -> "⚠️ NGUY HIỂM! Máy sẽ giảm hiệu năng mạnh!"
                    ThermalMonitor.ThrottleLevel.HOT -> "🔥 Máy quá nóng! Hiệu năng đang giảm!"
                    ThermalMonitor.ThrottleLevel.WARM -> "🌡️ Máy đang ấm dần"
                    else -> ""
                }
                Text(warningLevel, color = Error, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                thermalInfo.coolingSuggestions.forEach { Text("• $it", color = DarkTextSecondary, fontSize = 11.sp) }
            } else if (thermalInfo.cpuTemp != null || thermalInfo.gpuTemp != null || thermalInfo.batteryTemp != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("✅ Nhiệt độ bình thường", color = Success, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                Text("ℹ️ Đang chờ dữ liệu nhiệt... (cần Shizuku)", color = DarkTextSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun PermissionGuide() {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface2.copy(alpha = 0.5f)), shape = RoundedCornerShape(14.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("📌 Yêu cầu quyền", color = DarkText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(6.dp))
            PermissionItem("Cài Shizuku & cấp quyền ADB", "Để thay đổi cài đặt hệ thống (DNS, DND, MSAA...)")
            PermissionItem("Cấp quyền hiển thị trên ứng dụng khác", "Cho phép hiển thị tâm ngắm & FPS counter")
            PermissionItem("Cấp quyền thông báo", "Cho phép dịch vụ nền chạy ổn định")
        }
    }
}

// ══════════════════════════════════════════════════════════════
// TOUCH SENSITIVITY COMPOSABLES (qwen v2)
// ══════════════════════════════════════════════════════════════

@Composable
private fun TouchSensitivityCard(
    currentLevel: String,
    isEnabled: Boolean,
    onApplySensitivity: (TouchSensitivityBooster.SensitivityLevel) -> Unit,
    onDisable: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedLevel by remember { mutableStateOf(0) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                        .background(if (isEnabled) Purple400.copy(alpha = 0.2f) else DarkSurface2),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.TouchApp, null, tint = if (isEnabled) Purple400 else DarkTextSecondary, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Siêu nhạy màn hình", color = DarkText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text("Level: $currentLevel", color = if (isEnabled) Purple400 else DarkTextSecondary, fontSize = 12.sp)
                }
                Switch(
                    checked = expanded,
                    onCheckedChange = { expanded = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Purple400, checkedTrackColor = Purple600.copy(alpha = 0.5f))
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Sensitivity level chips
                    Text("Chọn mức độ nhạy:", color = DarkTextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    val sensitivityLevels = listOf(
                        TouchSensitivityBooster.SensitivityLevel.NORMAL,
                        TouchSensitivityBooster.SensitivityLevel.HIGH,
                        TouchSensitivityBooster.SensitivityLevel.ULTRA,
                        TouchSensitivityBooster.SensitivityLevel.EXTREME,
                    )

                    sensitivityLevels.forEachIndexed { index, level ->
                        val isSelected = selectedLevel == index
                        Card(
                            modifier = Modifier.fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedLevel = index },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Purple600.copy(alpha = 0.2f) else DarkSurface2.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedLevel = index },
                                    colors = RadioButtonDefaults.colors(selectedColor = Purple400),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(level.displayName, color = if (isSelected) Purple400 else DarkText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Text(when (level) {
                                        TouchSensitivityBooster.SensitivityLevel.NORMAL -> "Mặc định"
                                        TouchSensitivityBooster.SensitivityLevel.HIGH -> "Touch slop giảm, pointer speed tăng"
                                        TouchSensitivityBooster.SensitivityLevel.ULTRA -> "Cực nhạy + Gaming mode"
                                        TouchSensitivityBooster.SensitivityLevel.EXTREME -> "⚠️ Tối đa — có thể false touch"
                                    }, color = DarkTextSecondary, fontSize = 11.sp)
                                }
                                if (level == TouchSensitivityBooster.SensitivityLevel.EXTREME) {
                                    Text("🔥", fontSize = 16.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GradientButton(
                            text = "Áp dụng: ${sensitivityLevels[selectedLevel].displayName}",
                            onClick = { onApplySensitivity(sensitivityLevels[selectedLevel]) },
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.TouchApp,
                        )
                        OutlinedButton(
                            onClick = {
                                onDisable()
                                expanded = false
                            },
                            modifier = Modifier.weight(0.5f).height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Error),
                        ) { Text("Tắt") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ZeroLatencyCard(
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (enabled) Success.copy(alpha = 0.2f) else DarkSurface2),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.FlashOn, null, tint = if (enabled) Success else DarkTextSecondary, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Zero-Latency Touch Pipeline", color = DarkText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(
                    if (enabled) "Input → SF → Render: 25+ optimizations active"
                    else "Giảm độ trễ cảm ứng tối đa (SurfaceFlinger + HWUI)",
                    color = if (enabled) Success else DarkTextSecondary, fontSize = 12.sp)
            }
            Switch(
                checked = enabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(checkedThumbColor = Success, checkedTrackColor = Success.copy(alpha = 0.5f))
            )
        }
    }
}

@Composable
private fun GameTouchOptimizationCard(
    enabled: Boolean,
    onApply: () -> Unit,
    onRestore: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                        .background(if (enabled) Warning.copy(alpha = 0.2f) else DarkSurface2),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.SportsEsports, null, tint = if (enabled) Warning else DarkTextSecondary, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Free Fire Touch Optimization", color = DarkText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(
                        if (enabled) "Đã tối ưu: Sensitivity + Latency + Precision"
                        else "Touch profile: Siêu nhạy + Zero-Latency + Multi-touch",
                        color = if (enabled) Warning else DarkTextSecondary, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (enabled) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    StatusBadge("Sensitivity: Siêu nhạy", Success)
                    StatusBadge("Zero-Latency", Success)
                    StatusBadge("Multi-touch 10", Success)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GradientButton(
                    text = if (enabled) "Tắt tối ưu" else "Bật tối ưu Free Fire",
                    onClick = if (enabled) onRestore else onApply,
                    modifier = Modifier.weight(1f),
                    icon = if (enabled) Icons.Default.PowerSettingsNew else Icons.Default.SportsEsports,
                )
            }
        }
    }
}

@Composable
private fun TouchMetricsCard(
    enabled: Boolean,
    latencyGrade: String,
    onToggle: () -> Unit,
    onRefresh: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (enabled) Info.copy(alpha = 0.2f) else DarkSurface2),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Analytics, null, tint = if (enabled) Info else DarkTextSecondary, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Touch Metrics Overlay", color = DarkText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(
                    if (enabled) "Grade: $latencyGrade | Theo dõi độ trễ touch"
                    else "Overlay đo độ trễ cảm ứng real-time",
                    color = if (enabled) Info else DarkTextSecondary, fontSize = 12.sp)
            }
            Switch(
                checked = enabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(checkedThumbColor = Info, checkedTrackColor = Info.copy(alpha = 0.5f))
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════
// UTILITY COMPOSABLES
// ══════════════════════════════════════════════════════════════

@Composable
private fun RowScope.MiniStatusChip(label: String, value: String, color: Color) {
    Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = DarkSurface), shape = RoundedCornerShape(10.dp)) {
        Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(label, color = DarkTextSecondary, fontSize = 9.sp)
        }
    }
}

@Composable
private fun ThermalChip(label: String, temp: Float?, tempColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = if (temp != null) "${temp.toInt()}°C" else "--", color = tempColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, color = DarkTextSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun QuickToggleCard(icon: ImageVector, title: String, subtitle: String, isEnabled: Boolean, onToggle: () -> Unit, accentColor: Color = DarkTextSecondary) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = if (isEnabled) accentColor.copy(alpha = 0.08f) else DarkSurface),
        shape = RoundedCornerShape(14.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                .background(if (isEnabled) accentColor.copy(alpha = 0.2f) else DarkSurface2),
                contentAlignment = Alignment.Center) { Icon(icon, null, tint = if (isEnabled) accentColor else DarkTextSecondary, modifier = Modifier.size(22.dp)) }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = if (isEnabled) accentColor else DarkText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(subtitle, color = if (isEnabled) accentColor.copy(alpha = 0.8f) else DarkTextSecondary, fontSize = 12.sp)
            }
            Switch(checked = isEnabled, onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(checkedThumbColor = accentColor, checkedTrackColor = accentColor.copy(alpha = 0.5f)))
        }
    }
}

@Composable
private fun RowScope.GaugeItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(label, color = DarkTextSecondary, fontSize = 10.sp)
    }
}

@Composable
private fun RowScope.MiniTerm(label: String, value: String, color: Color) {
    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.1f)).padding(8.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(value, color = color.copy(alpha = 0.8f), fontSize = 11.sp)
        }
    }
}

@Composable
private fun RowScope.SpeedMeter(label: String, value: Double, unit: String, color: Color) {
    Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.1f)).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text("%.1f".format(value), color = color, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text(unit, color = color.copy(alpha = 0.7f), fontSize = 11.sp)
        Text(label, color = DarkTextSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun FeatureCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = Purple400,
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(DarkSurface2),
                contentAlignment = Alignment.Center) { Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp)) }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = DarkText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(description, color = DarkTextSecondary, fontSize = 11.sp, maxLines = 2)
            }
            Icon(Icons.Default.ChevronRight, null, tint = DarkTextSecondary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun PermissionItem(title: String, description: String) {
    Row(modifier = Modifier.padding(vertical = 3.dp)) {
        Text("• ", color = Purple400, fontSize = 12.sp)
        Column {
            Text(title, color = DarkText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(description, color = DarkTextSecondary, fontSize = 10.sp)
        }
    }
}

// ══════════════════════════════════════════════════════════════
// HELPERS
// ══════════════════════════════════════════════════════════════

private fun infoColor(fps: Float): Color = when {
    fps >= 55 -> Success
    fps >= 30 -> Warning
    fps > 0 -> Error
    else -> DarkTextSecondary
}

private fun timeToStr(seconds: Int): String = when {
    seconds >= Int.MAX_VALUE -> "∞"
    seconds < 60 -> "${seconds}s"
    seconds < 3600 -> "${seconds / 60}p ${seconds % 60}s"
    else -> "${seconds / 3600}h"
}

private fun gameShortName(pkg: String): String = when (pkg) {
    "com.dts.freefireth" -> "Free Fire"
    "com.dts.freefiremax" -> "FF Max"
    "com.tencent.ig" -> "PUBG"
    "com.mobile.legends" -> "MLBB"
    else -> pkg.substringAfterLast('.')
}
