package com.nmdlock.app.core.services

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * Provides real network diagnostics: ping, DNS, speed test.
 * Uses standard Java/Kotlin networking APIs (no root required).
 */
@Singleton
class NetworkDiagnostics(
    private val context: Context,
    private val shizukuManager: ShizukuManager,
) {

    data class PingResult(
        val minMs: Long = 0,
        val maxMs: Long = 0,
        val avgMs: Long = 0,
        val packetLoss: Float = 0f,
        val isSuccess: Boolean = false,
        val rawOutput: String = "",
    )

    data class DnsResult(
        val ipAddresses: List<String> = emptyList(),
        val hostname: String = "",
        val responseTimeMs: Long = 0,
        val isSuccess: Boolean = false,
    )

    data class SpeedTestResult(
        val downloadMbps: Double = 0.0,
        val uploadMbps: Double = 0.0,
        val isSuccess: Boolean = false,
        val message: String = "",
    )

    /**
     * Perform REAL ping to a host using system ping command.
     * Returns detailed results including min/max/avg latency and packet loss.
     */
    suspend fun ping(host: String = "google.com", count: Int = 4): PingResult = withContext(Dispatchers.IO) {
        try {
            val result = shizukuManager.executeCommand("ping -c $count -W 5 $host")
            val output = result.getOrNull() ?: ""

            if (output.isBlank()) {
                return@withContext PingResult(isSuccess = false, rawOutput = "No output from ping")
            }

            // Parse ping times
            val timeRegex = Regex("time=(\\d+\\.?\\d*)\\s*ms")
            val times = timeRegex.findAll(output).map {
                it.groupValues[1].toDoubleOrNull() ?: 0.0
            }.toList()

            // Parse packet loss
            val lossRegex = Regex("(\\d+)\\%\\s*packet loss")
            val packetLoss = lossRegex.find(output)?.groupValues?.getOrNull(1)?.toFloatOrNull() ?: 100f

            if (times.isEmpty()) {
                return@withContext PingResult(
                    isSuccess = packetLoss < 100,
                    packetLoss = packetLoss,
                    rawOutput = output,
                )
            }

            val minMs = times.minOrNull()?.roundToInt()?.toLong() ?: 0
            val maxMs = times.maxOrNull()?.roundToInt()?.toLong() ?: 0
            val avgMs = (times.average()).roundToInt().toLong()

            PingResult(
                minMs = minMs,
                maxMs = maxMs,
                avgMs = avgMs,
                packetLoss = packetLoss,
                isSuccess = packetLoss < 100,
                rawOutput = output,
            )
        } catch (e: Exception) {
            PingResult(isSuccess = false, rawOutput = "Error: ${e.message}")
        }
    }

    /**
     * Perform REAL DNS lookup using InetAddress.
     */
    suspend fun dnsLookup(hostname: String): DnsResult = withContext(Dispatchers.IO) {
        try {
            val startTime = System.currentTimeMillis()
            val addresses = InetAddress.getAllByName(hostname)
            val elapsed = System.currentTimeMillis() - startTime

            val ips = addresses.map { it.hostAddress ?: "Unknown" }

            DnsResult(
                ipAddresses = ips,
                hostname = hostname,
                responseTimeMs = elapsed,
                isSuccess = ips.isNotEmpty(),
            )
        } catch (e: Exception) {
            DnsResult(isSuccess = false, hostname = hostname, responseTimeMs = 0)
        }
    }

    /**
     * Perform a REAL speed test by downloading a file and measuring throughput.
     * Uses a well-known test file from speedtest servers.
     */
    suspend fun speedTest(): SpeedTestResult = withContext(Dispatchers.IO) {
        try {
            // Download from a fast CDN test file
            val testUrl = "https://proof.ovh.net/files/10Mb.dat" // 10MB test file
            val url = URL(testUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 30000
            connection.connect()

            val contentLength = connection.contentLengthLong
            if (contentLength <= 0) {
                return@withContext SpeedTestResult(isSuccess = false, message = "Cannot determine file size")
            }

            val inputStream = connection.inputStream
            val buffer = ByteArray(8192)
            var totalBytes = 0L
            val startTime = System.currentTimeMillis()

            inputStream.use { stream ->
                var bytesRead: Int
                while (stream.read(buffer).also { bytesRead = it } != -1) {
                    totalBytes += bytesRead
                }
            }

            val elapsedMs = System.currentTimeMillis() - startTime
            if (elapsedMs <= 0) {
                return@withContext SpeedTestResult(isSuccess = false, message = "Test too fast")
            }

            // Calculate Mbps: (bytes * 8) / (time in seconds) / 1,000,000
            val bitsPerSecond = (totalBytes * 8).toDouble() / (elapsedMs / 1000.0)
            val downloadMbps = (bitsPerSecond / 1_000_000.0).let {
                (it * 10).roundToInt() / 10.0 // Round to 1 decimal
            }

            SpeedTestResult(
                downloadMbps = downloadMbps,
                isSuccess = downloadMbps > 0,
                message = if (downloadMbps > 50) "Mạng nhanh" else if (downloadMbps > 10) "Mạng ổn định" else "Mạng chậm",
            )
        } catch (e: Exception) {
            SpeedTestResult(isSuccess = false, message = "Lỗi: ${e.message}")
        }
    }

    /**
     * Simple connectivity check - try to reach a known server.
     */
    suspend fun checkConnectivity(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://www.google.com")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            connection.connect()
            connection.responseCode == 200
        } catch (e: Exception) {
            false
        }
    }
}
