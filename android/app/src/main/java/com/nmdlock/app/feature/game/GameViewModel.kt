package com.nmdlock.app.feature.game

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nmdlock.app.core.services.*
import com.nmdlock.app.data.local.ProfileStatsEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * GameViewModel mở rộng với tất cả v3.0 services
 */
data class GameUiState(
    val isApplying: Boolean = false,
    val appliedActions: List<String> = emptyList(),
    val isSuccess: Boolean = false,
    // DNS
    val currentDns: String = "",
    val dnsApplied: Boolean = false,
    val dnsMessage: String = "",
    // Thermal
    val thermalInfo: ThermalMonitor.ThermalInfo = ThermalMonitor.ThermalInfo(),
    // DND
    val dndEnabled: Boolean = false,
    val dndMessage: String = "",
    // Auto-updates
    val autoUpdatesDisabled: Boolean = false,
    // === v3.0 ===
    // PID Controller
    val pidOutput: CpuGovernorPID.PidOutput = CpuGovernorPID.PidOutput(),
    val pidEnabled: Boolean = false,
    // Predictive Thermal
    val thermalPrediction: ThermalPrediction = ThermalPrediction(),
    val thermalState: ThermalState = ThermalState.NORMAL,
    // Queue Stats
    val queueStats: ShizukuCommandQueue.QueueStats = ShizukuCommandQueue.QueueStats(),
    // SmartAppKiller
    val smartKillResult: SmartAppKiller.KillResult? = null,
    val smartKillAggressive: Boolean = false,
    // Auto-Learner
    val bestProfiles: List<ProfileStatsEntity> = emptyList(),
    val currentArm: String? = null,
    // BurstSpeedTest
    val speedResult: BurstSpeedTester.SpeedResult = BurstSpeedTester.SpeedResult(),
    val speedProgress: Float = 0f,
    // Network TCP
    val tcpOptimized: Boolean = false,
    // Game Lifecycle
    val currentGame: String? = null,
    val lifecycleActive: Boolean = false,
    // === Touch Sensitivity (qwen v2) ===
    val touchSensitivityLevel: String = "Bình thường",
    val touchSensitivityEnabled: Boolean = false,
    val zeroLatencyEnabled: Boolean = false,
    val touchMetricsEnabled: Boolean = false,
    val touchLatencyGrade: String = "--",
    val predictedTouchEnabled: Boolean = false,
    val gameTouchOptimized: Boolean = false,
    val touchActions: List<String> = emptyList(),
)

@HiltViewModel
class GameViewModel @Inject constructor(
    private val gameProfileEngine: GameProfileEngine,
    private val dnsManager: DnsManager,
    private val thermalMonitor: ThermalMonitor,
    private val dndManager: DoNotDisturbManager,
    // v3.0 services
    private val predictiveThermal: PredictiveThermalEngine,
    private val pidController: CpuGovernorPID,
    private val commandQueue: ShizukuCommandQueue,
    private val smartAppKiller: SmartAppKiller,
    private val profileAutoLearner: ProfileAutoLearner,
    private val burstSpeedTester: BurstSpeedTester,
    private val networkKernelTuner: NetworkKernelTuner,
    private val gameLifecycleManager: GameLifecycleManager,
    // Touch Sensitivity (qwen v2)
    private val touchSensitivityBooster: TouchSensitivityBooster,
    private val inputLatencyReducer: InputLatencyReducer,
    private val gameTouchOptimizer: GameTouchOptimizer,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    override fun onCleared() {
        super.onCleared()
        stopAllMonitoring()
    }

    init {
        viewModelScope.launch {
            val currentDns = dnsManager.getCurrentDnsMode()
            _uiState.value = _uiState.value.copy(currentDns = currentDns)
            monitorThermal()       // Legacy thermal
            monitorV3States()      // v3.0 monitoring loops
            startLifecycleManager()
        }
    }

    // ──────────────────────────────────────────────
    // PROFILE
    // ──────────────────────────────────────────────

    fun applyProfile(profileName: String, isMax: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isApplying = true, appliedActions = emptyList())
            val result = gameProfileEngine.applyProfile(profileName, isMax)
            _uiState.value = _uiState.value.copy(
                isApplying = false,
                appliedActions = result.actionsApplied,
                isSuccess = result.isSuccess,
            )
        }
    }

    // profile auto-learn: ghi nhận session sau khi apply
    fun recordProfileSession(
        gamePackage: String,
        stats: ProfileAutoLearner.SessionStats
    ) {
        viewModelScope.launch {
            val arm = _uiState.value.currentArm ?: "gaming_balanced"
            profileAutoLearner.recordSession(arm, gamePackage, android.os.Build.MODEL, stats)
        }
    }

    // ──────────────────────────────────────────────
    // DNS
    // ──────────────────────────────────────────────

    fun applyDns(providerName: String) {
        viewModelScope.launch {
            val result = dnsManager.applyDns(providerName)
            _uiState.value = _uiState.value.copy(
                dnsApplied = result.isSuccess,
                dnsMessage = result.message,
                currentDns = dnsManager.getCurrentDnsMode(),
            )
        }
    }

    fun disableDns() {
        viewModelScope.launch {
            val result = dnsManager.disableDns()
            _uiState.value = _uiState.value.copy(
                dnsApplied = result.isSuccess,
                dnsMessage = result.message,
                currentDns = dnsManager.getCurrentDnsMode(),
            )
        }
    }

    // ──────────────────────────────────────────────
    // DND
    // ──────────────────────────────────────────────

    fun toggleDnd() {
        viewModelScope.launch {
            val current = _uiState.value.dndEnabled
            if (current) {
                val result = dndManager.disableGameMode()
                _uiState.value = _uiState.value.copy(
                    dndEnabled = false,
                    dndMessage = result.message,
                )
            } else {
                val result = dndManager.enableGameMode()
                _uiState.value = _uiState.value.copy(
                    dndEnabled = true,
                    dndMessage = result.message,
                )
            }
        }
    }

    // ──────────────────────────────────────────────
    // FPS Counter
    // ──────────────────────────────────────────────

    fun toggleFps(context: Context) {
        if (FpsOverlayService.isRunning()) {
            FpsOverlayService.stop(context)
        } else {
            FpsOverlayService.start(context)
        }
    }

    // ──────────────────────────────────────────────
    // Auto Updates
    // ──────────────────────────────────────────────

    fun toggleAutoUpdates() {
        viewModelScope.launch {
            val current = _uiState.value.autoUpdatesDisabled
            if (current) {
                dndManager.enableAutoUpdates()
            } else {
                dndManager.disableAutoUpdates()
            }
            _uiState.value = _uiState.value.copy(autoUpdatesDisabled = !current)
        }
    }

    // ──────────────────────────────────────────────
    // Legacy Thermal Monitoring
    // ──────────────────────────────────────────────

    private fun monitorThermal() {
        viewModelScope.launch {
            while (true) {
                val info = thermalMonitor.getThermalInfo()
                _uiState.value = _uiState.value.copy(thermalInfo = info)
                delay(5000)
            }
        }
    }

    private fun stopAllMonitoring() {
        predictiveThermal.stopMonitoring()
        pidController.stopAutoTuning()
        commandQueue.stop()
        gameLifecycleManager.stop()
    }

    // ──────────────────────────────────────────────
    // v3.0 Monitoring Loops
    // ──────────────────────────────────────────────

    private fun monitorV3States() {
        // Theo dõi thermal prediction
        viewModelScope.launch {
            predictiveThermal.startMonitoring(viewModelScope)
            predictiveThermal.prediction.collect { pred ->
                _uiState.value = _uiState.value.copy(
                    thermalPrediction = pred,
                    thermalState = pred.state,
                )
            }
        }
        // Theo dõi PID output
        viewModelScope.launch {
            pidController.pidOutput.collect { pid ->
                _uiState.value = _uiState.value.copy(pidOutput = pid)
            }
        }
        // Theo dõi queue stats
        viewModelScope.launch {
            commandQueue.stats.collect { stats ->
                _uiState.value = _uiState.value.copy(queueStats = stats)
            }
        }
        // Theo dõi speed test
        viewModelScope.launch {
            burstSpeedTester.speedResult.collect { speed ->
                _uiState.value = _uiState.value.copy(speedResult = speed)
            }
        }
        viewModelScope.launch {
            burstSpeedTester.progress.collect { pct ->
                _uiState.value = _uiState.value.copy(speedProgress = pct.toFloat())
            }
        }
        // Theo dõi auto-learner arm
        viewModelScope.launch {
            profileAutoLearner.currentArm.collect { arm ->
                _uiState.value = _uiState.value.copy(currentArm = arm)
            }
        }
        // Theo dõi game lifecycle
        viewModelScope.launch {
            gameLifecycleManager.currentGame.collect { game ->
                _uiState.value = _uiState.value.copy(
                    currentGame = game,
                    lifecycleActive = game != null,
                )
            }
        }
        // Refresh best profiles định kỳ
        viewModelScope.launch {
            while (true) {
                try {
                    val profiles = profileAutoLearner.getBestProfilesSync(
                        "com.dts.freefireth",
                        android.os.Build.MODEL
                    )
                    _uiState.value = _uiState.value.copy(bestProfiles = profiles)
                } catch (_: Exception) {}
                delay(30000)
            }
        }
    }

    // ──────────────────────────────────────────────
    // PID — Bật/tắt auto-tuning
    // ──────────────────────────────────────────────

    fun togglePid() {
        val current = _uiState.value.pidEnabled
        if (current) {
            pidController.stopAutoTuning()
        } else {
            pidController.startAutoTuning(viewModelScope) {
                FpsOverlayService.getCurrentFps()
            }
        }
        _uiState.value = _uiState.value.copy(pidEnabled = !current)
    }

    // ──────────────────────────────────────────────
    // Smart App Killer
    // ──────────────────────────────────────────────

    fun runSmartKill(exclude: Set<String> = emptySet()) {
        viewModelScope.launch {
            val aggressive = _uiState.value.smartKillAggressive
            val result = smartAppKiller.killSmartly(exclude, aggressive)
            _uiState.value = _uiState.value.copy(smartKillResult = result)
        }
    }

    fun toggleSmartKillAggressive() {
        val current = _uiState.value.smartKillAggressive
        _uiState.value = _uiState.value.copy(smartKillAggressive = !current)
    }

    // ──────────────────────────────────────────────
    // Burst Speed Test
    // ──────────────────────────────────────────────

    fun runSpeedTest() {
        viewModelScope.launch {
            burstSpeedTester.runBurstTest(viewModelScope)
        }
    }

    // ──────────────────────────────────────────────
    // Network Kernel Tuning
    // ──────────────────────────────────────────────

    fun toggleTcpOptimization() {
        viewModelScope.launch {
            val current = _uiState.value.tcpOptimized
            if (current) {
                networkKernelTuner.restoreTcp()
            } else {
                networkKernelTuner.applyGamingTcp()
            }
            _uiState.value = _uiState.value.copy(tcpOptimized = !current)
        }
    }

    // ──────────────────────────────────────────────
    // Game Lifecycle Manager
    // ──────────────────────────────────────────────

    private fun startLifecycleManager() {
        gameLifecycleManager.start(viewModelScope)
        commandQueue.start(viewModelScope)
    }

    fun startAutoDetection() {
        viewModelScope.launch {
            gameLifecycleManager.stop()
            delay(100)
            gameLifecycleManager.start(viewModelScope)
            _uiState.value = _uiState.value.copy(lifecycleActive = true)
        }
    }

    fun stopAutoDetection() {
        gameLifecycleManager.stop()
        _uiState.value = _uiState.value.copy(lifecycleActive = false, currentGame = null)
    }

    // ──────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────

    fun getDnsProviders(): List<DnsManager.DnsProvider> = dnsManager.providers

    fun resetSettings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isApplying = true)
            val result = gameProfileEngine.resetSettings()
            _uiState.value = _uiState.value.copy(
                isApplying = false,
                appliedActions = result.actionsApplied,
                isSuccess = result.isSuccess,
            )
        }
    }

    // ──────────────────────────────────────────────
    // TOUCH SENSITIVITY (qwen v2)
    // ──────────────────────────────────────────────

    /**
     * Apply touch sensitivity level
     */
    fun applyTouchSensitivity(level: TouchSensitivityBooster.SensitivityLevel) {
        viewModelScope.launch {
            touchSensitivityBooster.enable(level)
            _uiState.value = _uiState.value.copy(
                touchSensitivityLevel = level.displayName,
                touchSensitivityEnabled = true,
                touchActions = listOf("Touch sensitivity: ${level.displayName}"),
            )
        }
    }

    /**
     * Disable touch sensitivity boost
     */
    fun disableTouchSensitivity() {
        viewModelScope.launch {
            touchSensitivityBooster.disable()
            _uiState.value = _uiState.value.copy(
                touchSensitivityEnabled = false,
                touchSensitivityLevel = "Bình thường",
                touchActions = listOf("Touch sensitivity: Tắt"),
            )
        }
    }

    /**
     * Toggle zero-latency mode
     */
    fun toggleZeroLatencyMode() {
        viewModelScope.launch {
            val current = _uiState.value.zeroLatencyEnabled
            if (current) {
                inputLatencyReducer.disableZeroLatencyMode()
                _uiState.value = _uiState.value.copy(
                    zeroLatencyEnabled = false,
                    touchActions = listOf("Zero-Latency: Tắt"),
                )
            } else {
                inputLatencyReducer.enableZeroLatencyMode()
                _uiState.value = _uiState.value.copy(
                    zeroLatencyEnabled = true,
                    touchActions = listOf("Zero-Latency: Bật"),
                )
            }
        }
    }

    /**
     * Apply Free Fire game touch optimization
     */
    fun applyGameTouchOptimization() {
        viewModelScope.launch {
            gameTouchOptimizer.applyFreeFireOptimization()
            _uiState.value = _uiState.value.copy(
                gameTouchOptimized = true,
                touchSensitivityEnabled = true,
                zeroLatencyEnabled = true,
                touchActions = listOf(
                    "Free Fire touch optimization: Bật",
                    "Touch sensitivity: Siêu nhạy",
                    "Zero-Latency mode: Bật",
                ),
            )
        }
    }

    /**
     * Restore game touch optimization
     */
    fun restoreGameTouchOptimization() {
        viewModelScope.launch {
            gameTouchOptimizer.restoreFreeFireOptimization()
            _uiState.value = _uiState.value.copy(
                gameTouchOptimized = false,
                touchSensitivityEnabled = false,
                zeroLatencyEnabled = false,
                touchActions = listOf("Game touch optimization: Tắt"),
            )
        }
    }

    /**
     * Toggle touch metrics overlay
     */
    fun toggleTouchMetrics(context: android.content.Context) {
        val current = _uiState.value.touchMetricsEnabled
        if (current) {
            TouchMetricsOverlay.stop(context)
            _uiState.value = _uiState.value.copy(
                touchMetricsEnabled = false,
                touchLatencyGrade = "--",
            )
        } else {
            TouchMetricsOverlay.start(context)
            _uiState.value = _uiState.value.copy(touchMetricsEnabled = true)
        }
    }

    /**
     * Get touch sensitivity levels for UI
     */
    fun getTouchSensitivityLevels(): Array<TouchSensitivityBooster.SensitivityLevel> {
        return TouchSensitivityBooster.SensitivityLevel.entries.toTypedArray()
    }

    /**
     * Update touch latency grade from overlay
     */
    fun refreshTouchLatencyGrade() {
        _uiState.value = _uiState.value.copy(
            touchLatencyGrade = TouchMetricsOverlay.getLatencyGrade(),
        )
    }
}
