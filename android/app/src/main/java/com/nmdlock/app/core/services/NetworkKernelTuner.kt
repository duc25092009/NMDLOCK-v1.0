package com.nmdlock.app.core.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ═══════════════════════════════════════════════════════════════════════
 * PILLAR 4: NETWORK KERNEL TUNER — TCP Optimization + Android network settings
 * ═══════════════════════════════════════════════════════════════════════
 *
 * ⚠️ sysctl commands yêu cầu ROOT. Fallback: settings put global (Shizuku)
 */
@Singleton
class NetworkKernelTuner @Inject constructor(
    private val shizukuManager: ShizukuManager
) {
    /**
     * Áp dụng gaming TCP optimization
     * BBR congestion control + TCP Fast Open + Low latency tuning
     */
    suspend fun applyGamingTcp(): List<String> {
        val commands = getGamingTcpCommands()
        val results = mutableListOf<String>()

        for (cmd in commands) {
            val result = shizukuManager.executeShellCommand(cmd)
            results.add(result.getOrElse { "FAILED: $cmd" })
        }

        return results
    }

    suspend fun restoreTcp(): List<String> {
        val commands = getRestoreTcpCommands()
        val results = mutableListOf<String>()

        for (cmd in commands) {
            val result = shizukuManager.executeShellCommand(cmd)
            results.add(result.getOrElse { "FAILED: $cmd" })
        }

        return results
    }

    fun getGamingTcpCommands(): List<String> = listOf(
        // BBR congestion control (Google's algorithm, best for gaming)
        "sysctl -w net.ipv4.tcp_congestion_control=bbr 2>/dev/null",

        // TCP Fast Open (skip 3-way handshake cho subsequent connections)
        "sysctl -w net.ipv4.tcp_fastopen=3 2>/dev/null",

        // Low latency tuning
        "sysctl -w net.ipv4.tcp_low_latency=1 2>/dev/null",

        // SACK enabled (selective ACK for packet loss recovery)
        "sysctl -w net.ipv4.tcp_sack=1 2>/dev/null",

        // Timestamps for RTT measurement
        "sysctl -w net.ipv4.tcp_timestamps=1 2>/dev/null",

        // Window scaling
        "sysctl -w net.ipv4.tcp_window_scaling=1 2>/dev/null",

        // Buffer tuning (gaming: small buffers = low latency)
        "sysctl -w net.ipv4.tcp_rmem='4096 87380 2097152' 2>/dev/null",
        "sysctl -w net.ipv4.tcp_wmem='4096 65536 2097152' 2>/dev/null",

        // Disable slow start after idle
        "sysctl -w net.ipv4.tcp_slow_start_after_idle=0 2>/dev/null",

        // FIN timeout reduction
        "sysctl -w net.ipv4.tcp_fin_timeout=15 2>/dev/null",

        // Keepalive tuning
        "sysctl -w net.ipv4.tcp_keepalive_time=300 2>/dev/null",
        "sysctl -w net.ipv4.tcp_keepalive_intvl=30 2>/dev/null",
        "sysctl -w net.ipv4.tcp_keepalive_probes=3 2>/dev/null",

        // Android-level: disable captive portal checks (causes ping spikes)
        "settings put global captive_portal_mode 0 2>/dev/null",
        "settings put global captive_portal_detection_enabled 0 2>/dev/null",
        "settings put global wifi_scan_throttle_enabled 0 2>/dev/null",

        // Disable WiFi metrics collection
        "settings put global metrics_wifi_scope_limit 0 2>/dev/null",

        // Disable network validation
        "settings put global network_recommendation_enabled 0 2>/dev/null",

        // Force WiFi to stay awake
        "settings put global wifi_sleep_policy 2 2>/dev/null"
    )

    fun getRestoreTcpCommands(): List<String> = listOf(
        "sysctl -w net.ipv4.tcp_congestion_control=cubic 2>/dev/null",
        "sysctl -w net.ipv4.tcp_fastopen=0 2>/dev/null",
        "sysctl -w net.ipv4.tcp_low_latency=0 2>/dev/null",
        "settings put global captive_portal_mode 1 2>/dev/null",
        "settings put global captive_portal_detection_enabled 1 2>/dev/null",
        "settings put global wifi_sleep_policy 0 2>/dev/null"
    )
}
