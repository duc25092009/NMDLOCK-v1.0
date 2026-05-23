package com.nmdlock.app.core.services

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * ═══════════════════════════════════════════════════════════════════════
 * PILLAR 4: BURST SPEED TESTER — Gaming-optimized multi-threaded speedtest
 * ═══════════════════════════════════════════════════════════════════════
 *
 * Tải 8 chunks 256KB song song, đo tail latency, jitter, packet loss
 * Gaming-optimized: ưu tiên đo latency + jitter + burst consistency
 */
@Singleton
class BurstSpeedTester @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val _speedResult = MutableStateFlow(SpeedResult())
    val speedResult: StateFlow<SpeedResult> = _speedResult.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    data class SpeedResult(
        val downloadMbps: Double = 0.0,
        val uploadMbps: Double = 0.0,
        val latencyMs: Double = 0.0,
        val jitterMs: Double = 0.0,
        val packetLossPercent: Double = 0.0,
        val tailLatencyMs: Double = 0.0,
        val burstConsistency: Double = 0.0,
        val isRunning: Boolean = false
    )

    /**
     * Chạy burst speed test
     * 1. Latency test (10 pings)
     * 2. Burst download (8 concurrent chunks)
     * 3. Upload test
     */
    suspend fun runBurstTest(
        scope: CoroutineScope,
        testUrl: String = "https://proof.ovh.net/files/10Mb.dat"
    ): SpeedResult = withContext(Dispatchers.IO) {
        _speedResult.value = SpeedResult(isRunning = true)
        _progress.value = 0f

        try {
            // ── Phase 1: Latency test (10 pings) ──
            _progress.value = 0.1f
            val latencies = (1..10).map {
                measureSinglePing(testUrl)
            }.filter { it > 0 }

            val avgLatency = if (latencies.isNotEmpty()) latencies.average() else 0.0
            val jitter = calculateJitter(latencies)
            val packetLoss = if (latencies.size < 10) {
                ((10 - latencies.size) / 10.0) * 100
            } else 0.0

            _progress.value = 0.3f

            // ── Phase 2: Burst download test (8 concurrent 256KB) ──
            val chunkSize = 256 * 1024
            val threadCount = 8
            val startTime = System.nanoTime()

            val chunkTimes = (0 until threadCount).map { i ->
                async {
                    val chunkStart = System.nanoTime()
                    try {
                        val request = Request.Builder()
                            .url(testUrl)
                            .header("Range", "bytes=${i * chunkSize}-${(i + 1) * chunkSize - 1}")
                            .header("Connection", "keep-alive")
                            .header("Cache-Control", "no-cache")
                            .build()

                        client.newCall(request).execute().use { response ->
                            response.body?.bytes()
                        }
                    } catch (e: Exception) { null }

                    (System.nanoTime() - chunkStart) / 1_000_000.0 // ms
                }
            }.awaitAll()

            _progress.value = 0.7f

            val validChunkTimes = chunkTimes.filter { it > 0 }
            val totalTimeMs = if (validChunkTimes.isNotEmpty()) {
                (System.nanoTime() - startTime) / 1_000_000.0
            } else 1000.0

            val totalBytes = chunkSize * threadCount
            val downloadMbps = if (totalTimeMs > 0) {
                (totalBytes * 8.0 / 1_000_000.0) / (totalTimeMs / 1000.0)
            } else 0.0

            // Tail latency (95th percentile)
            val sorted = validChunkTimes.sorted()
            val tailLatency = if (sorted.isNotEmpty()) {
                val idx = (sorted.size * 0.95).toInt().coerceAtMost(sorted.size - 1)
                sorted[idx]
            } else 0.0

            // Burst consistency score
            val avgChunkTime = validChunkTimes.average()
            val variance = if (validChunkTimes.size > 1) {
                validChunkTimes.map { (it - avgChunkTime).let { d -> d * d } }.average()
            } else 0.0
            val consistency = (100.0 - sqrt(variance)).coerceIn(0.0, 100.0)

            _progress.value = 0.8f

            // ── Phase 3: Upload test ──
            val uploadMbps = try {
                measureUpload(testUrl)
            } catch (e: Exception) { 0.0 }

            _progress.value = 1.0f

            val result = SpeedResult(
                downloadMbps = downloadMbps,
                uploadMbps = uploadMbps,
                latencyMs = avgLatency,
                jitterMs = jitter,
                packetLossPercent = packetLoss,
                tailLatencyMs = tailLatency,
                burstConsistency = consistency,
                isRunning = false
            )

            _speedResult.value = result
            result

        } catch (e: Exception) {
            _speedResult.value = SpeedResult(isRunning = false)
            SpeedResult(isRunning = false)
        }
    }

    private fun calculateJitter(latencies: List<Double>): Double {
        if (latencies.size < 2) return 0.0
        return latencies.zipWithNext { a, b -> kotlin.math.abs(a - b) }.average()
    }

    private fun measureSinglePing(url: String): Double {
        val start = System.nanoTime()
        return try {
            val request = Request.Builder()
                .url(url)
                .head()
                .build()
            client.newCall(request).execute().use { }
            (System.nanoTime() - start) / 1_000_000.0
        } catch (e: Exception) { -1.0 }
    }

    private fun measureUpload(url: String): Double {
        val payload = ByteArray(64 * 1024) { it.toByte() } // 64KB payload
        val start = System.nanoTime()

        val request = Request.Builder()
            .url(url.replace("10Mb.dat", ""))
            .post(payload.toRequestBody("application/octet-stream".toMediaType()))
            .build()

        client.newCall(request).execute().use { }
        val elapsed = (System.nanoTime() - start) / 1_000_000.0
        if (elapsed <= 0) return 0.0
        return (payload.size * 8.0 / 1_000_000.0) / (elapsed / 1000.0)
    }
}
