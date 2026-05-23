package com.nmdlock.app.feature.network

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nmdlock.app.core.services.NetworkDiagnostics
import com.nmdlock.app.core.ui.theme.DarkTextSecondary
import com.nmdlock.app.core.ui.theme.Error
import com.nmdlock.app.core.ui.theme.Success
import com.nmdlock.app.core.ui.theme.Warning
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NetworkUiState(
    val isTesting: Boolean = false,
    val pingAvgMs: Long? = null,
    val pingMinMs: Long? = null,
    val pingMaxMs: Long? = null,
    val pingLoss: Float? = null,
    val speedMbps: String? = null,
    val speedMessage: String? = null,
    val dnsIps: String = "",
    val dnsTimeMs: Long? = null,
    val stabilityText: String = "—",
    val stabilityColor: Color = DarkTextSecondary,
)

@HiltViewModel
class NetworkViewModel @Inject constructor(
    private val networkDiagnostics: NetworkDiagnostics,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NetworkUiState())
    val uiState: StateFlow<NetworkUiState> = _uiState.asStateFlow()

    fun testPing() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTesting = true)
            val result = networkDiagnostics.ping("google.com", 4)
            _uiState.value = _uiState.value.copy(
                isTesting = false,
                pingAvgMs = result.avgMs,
                pingMinMs = result.minMs,
                pingMaxMs = result.maxMs,
                pingLoss = result.packetLoss,
            )
            updateStability()
        }
    }

    fun testSpeed() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTesting = true)
            val result = networkDiagnostics.speedTest()
            _uiState.value = _uiState.value.copy(
                isTesting = false,
                speedMbps = if (result.isSuccess) "${result.downloadMbps} Mbps" else null,
                speedMessage = if (result.isSuccess) result.message else result.message,
            )
        }
    }

    fun testDns() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTesting = true)
            val result = networkDiagnostics.dnsLookup("google.com")
            _uiState.value = _uiState.value.copy(
                isTesting = false,
                dnsIps = result.ipAddresses.joinToString("\n"),
                dnsTimeMs = result.responseTimeMs,
            )
        }
    }

    fun testCustomDns(name: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTesting = true)
            val hostname = when (name) {
                "Google DNS" -> "dns.google"
                "Cloudflare" -> "one.one.one.one"
                "OpenDNS" -> "resolver1.opendns.com"
                else -> "google.com"
            }
            val result = networkDiagnostics.dnsLookup(hostname)
            _uiState.value = _uiState.value.copy(
                isTesting = false,
                dnsIps = "✓ ${result.ipAddresses.firstOrNull() ?: ""}",
                dnsTimeMs = result.responseTimeMs,
            )
        }
    }

    private fun updateStability() {
        val state = _uiState.value
        val stabilityText: String
        val stabilityColor: Color

        when {
            state.pingLoss != null && state.pingLoss > 50 -> {
                stabilityText = "Kém"
                stabilityColor = Error
            }
            state.pingAvgMs != null && state.pingAvgMs < 50 -> {
                stabilityText = "Tốt"
                stabilityColor = Success
            }
            state.pingAvgMs != null && state.pingAvgMs < 150 -> {
                stabilityText = "Trung bình"
                stabilityColor = Warning
            }
            state.pingAvgMs != null -> {
                stabilityText = "Kém"
                stabilityColor = Error
            }
            else -> {
                stabilityText = "—"
                stabilityColor = DarkTextSecondary
            }
        }

        _uiState.value = _uiState.value.copy(
            stabilityText = stabilityText,
            stabilityColor = stabilityColor,
        )
    }
}
