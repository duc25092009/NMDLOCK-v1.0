package com.nmdlock.app.feature.optimization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nmdlock.app.core.services.OptimizationEngine
import com.nmdlock.app.core.services.SystemInfoProvider
import com.nmdlock.app.domain.model.SystemStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OptimizationUiState(
    val isOptimizing: Boolean = false,
    val isScanningRam: Boolean = false,
    val isCleaningCache: Boolean = false,
    val isScanningApps: Boolean = false,
    val systemStats: SystemStats? = null,
    val lastResult: String? = null,
    val scanResult: String? = null,
    val heavyApps: List<OptimizationEngine.HeavyAppInfo> = emptyList(),
)

@HiltViewModel
class OptimizationViewModel @Inject constructor(
    private val optimizationEngine: OptimizationEngine,
    private val systemInfoProvider: SystemInfoProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OptimizationUiState())
    val uiState: StateFlow<OptimizationUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                systemStats = systemInfoProvider.getSystemStats()
            )
        }
    }

    suspend fun quickOptimize() {
        _uiState.value = _uiState.value.copy(isOptimizing = true, lastResult = null)
        val result = optimizationEngine.quickOptimize()
        _uiState.value = _uiState.value.copy(
            isOptimizing = false,
            lastResult = result.message,
            systemStats = systemInfoProvider.getSystemStats(),
        )
    }

    suspend fun fullOptimize() {
        _uiState.value = _uiState.value.copy(isOptimizing = true, lastResult = null)
        val result = optimizationEngine.runFullOptimization()
        _uiState.value = _uiState.value.copy(
            isOptimizing = false,
            lastResult = "Đã dừng ${result.processesKilled} ứng dụng, giải phóng ${result.ramFreedMB}MB RAM. ${result.message}",
            systemStats = systemInfoProvider.getSystemStats(),
        )
    }

    suspend fun scanRam() {
        _uiState.value = _uiState.value.copy(isScanningRam = true, scanResult = null)
        val stats = systemInfoProvider.getRamInfo()
        _uiState.value = _uiState.value.copy(
            isScanningRam = false,
            scanResult = "RAM: ${stats.usedMB}MB / ${stats.totalMB}MB (${stats.usedPercent.toInt()}%)",
        )
    }

    suspend fun cleanCache() {
        _uiState.value = _uiState.value.copy(isCleaningCache = true, scanResult = null)
        val result = optimizationEngine.runFullOptimization(clearCache = true, killProcesses = false, batteryOptimize = false)
        _uiState.value = _uiState.value.copy(
            isCleaningCache = false,
            scanResult = if (result.cacheCleaned) "Đã dọn cache thành công" else "Không có cache cần dọn",
        )
    }

    suspend fun scanHeavyApps() {
        _uiState.value = _uiState.value.copy(isScanningApps = true, scanResult = null)
        val apps = optimizationEngine.scanHeavyApps()
        _uiState.value = _uiState.value.copy(
            isScanningApps = false,
            heavyApps = apps,
            scanResult = if (apps.isNotEmpty()) "Tìm thấy ${apps.size} ứng dụng nặng" else "Không có ứng dụng nặng",
        )
    }
}
