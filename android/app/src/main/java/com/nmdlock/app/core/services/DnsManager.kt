package com.nmdlock.app.core.services

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages DNS settings via Shizuku (ADB-level).
 * Changes to Private DNS (DNS over TLS) on Android 9+.
 */
@Singleton
class DnsManager @Inject constructor(
    private val shizukuManager: ShizukuManager,
) {

    data class DnsProvider(
        val name: String,
        val hostname: String,
        val ipDisplay: String,
    )

    val providers = listOf(
        DnsProvider("Cloudflare", "cloudflare-dns.com", "1.1.1.1 / 1.0.0.1"),
        DnsProvider("Google DNS", "dns.google", "8.8.8.8 / 8.8.4.4"),
        DnsProvider("OpenDNS", "dns.opendns.com", "208.67.222.222"),
        DnsProvider("Quad9", "dns.quad9.net", "9.9.9.9"),
        DnsProvider("AdGuard", "dns.adguard.com", "94.140.14.14"),
    )

    data class DnsResult(
        val isSuccess: Boolean = false,
        val currentProvider: String = "",
        val message: String = "",
    )

    /**
     * Get current DNS mode.
     */
    suspend fun getCurrentDnsMode(): String = withContext(Dispatchers.IO) {
        val mode = shizukuManager.executeCommand(
            "settings get global private_dns_mode"
        ).getOrNull() ?: "off"
        when (mode.trim()) {
            "hostname" -> {
                val specifier = shizukuManager.executeCommand(
                    "settings get global private_dns_specifier"
                ).getOrNull() ?: ""
                "DNS: ${specifier.trim()}"
            }
            "opportunistic" -> "Tự động"
            else -> "Tắt"
        }
    }

    /**
     * Apply a DNS provider via Private DNS (Android 9+).
     */
    suspend fun applyDns(providerName: String): DnsResult = withContext(Dispatchers.IO) {
        try {
            val provider = providers.find { it.name == providerName }
            if (provider == null) {
                return@withContext DnsResult(isSuccess = false, message = "Không tìm thấy DNS: $providerName")
            }

            // Set DNS mode to hostname and specify the provider
            val setModeCmd = shizukuManager.executeCommand(
                "settings put global private_dns_mode hostname"
            )
            if (setModeCmd.isFailure) {
                return@withContext DnsResult(
                    isSuccess = false,
                    message = "Lỗi đặt chế độ DNS (cần Shizuku)"
                )
            }

            val setSpecCmd = shizukuManager.executeCommand(
                "settings put global private_dns_specifier ${provider.hostname}"
            )
            if (setSpecCmd.isFailure) {
                shizukuManager.executeCommand("settings put global private_dns_mode off")
                return@withContext DnsResult(
                    isSuccess = false,
                    message = "Lỗi đặt DNS specifier"
                )
            }

            // Verify
            val verify = shizukuManager.executeCommand("settings get global private_dns_mode").getOrNull() ?: ""

            DnsResult(
                isSuccess = verify.contains("hostname"),
                currentProvider = provider.name,
                message = "Đã chuyển sang DNS ${provider.name} (${provider.ipDisplay})",
            )
        } catch (e: Exception) {
            DnsResult(isSuccess = false, message = "Lỗi: ${e.message}")
        }
    }

    /**
     * Disable Private DNS (back to default).
     */
    suspend fun disableDns(): DnsResult = withContext(Dispatchers.IO) {
        try {
            shizukuManager.executeCommand("settings put global private_dns_mode off")
            DnsResult(isSuccess = true, message = "Đã tắt DNS tùy chỉnh")
        } catch (e: Exception) {
            DnsResult(isSuccess = false, message = "Lỗi: ${e.message}")
        }
    }

    /**
     * Ping DNS server to check latency.
     */
    suspend fun testDnsLatency(hostname: String): Long? = withContext(Dispatchers.IO) {
        try {
            val result = shizukuManager.executeCommand("ping -c 1 -W 3 $hostname")
            val output = result.getOrNull() ?: return@withContext null
            val regex = Regex("time=(\\d+\\.?\\d*)\\s*ms")
            val match = regex.find(output)
            match?.groupValues?.getOrNull(1)?.toDoubleOrNull()?.toLong()
        } catch (e: Exception) {
            null
        }
    }
}
