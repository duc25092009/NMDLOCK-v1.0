package com.nmdlock.app.core.services

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.nmdlock.app.core.util.BloomFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ═══════════════════════════════════════════════════════════════════════
 * PILLAR 7: SMART APP KILLING (PAGERANK-INSPIRED)
 * ═══════════════════════════════════════════════════════════════════════
 *
 * PageRank-inspired scoring system
 * - Apps có "độ quan trọng" cao (usage + permissions) sẽ KHÔNG bị kill
 * - Bloom Filter dedup: không kill cùng app 2 lần trong 1 session
 * - NEVER kill: system apps, NMDLock, Shizuku, phone, launcher
 */
@Singleton
class SmartAppKiller @Inject constructor(
    private val context: Context,
    private val shizukuManager: ShizukuManager
) {
    // White list — KHÔNG BAO GIỜ kill
    private val neverKill = setOf(
        "com.nmdlock.app",
        "com.google.android.gms",
        "com.google.android.gsf",
        "com.android.systemui",
        "com.android.phone",
        "com.android.settings",
        "com.android.launcher3",
        "com.google.android.apps.nexuslauncher",
        "com.miui.home",
        "moe.shizuku.privileged.api",
        "rikka.shizuku"
    )

    // Music players + VoIP apps (don't kill)
    private val protectedCategories = setOf(
        "com.spotify", "com.apple.android.music",
        "com.google.android.apps.youtube.music",
        "com.soundcloud.android",
        "com.whatsapp", "com.tencent.mm", "org.telegram.messenger",
        "com.facebook.orca", "com.skype.raider",
        "com.google.android.dialer", "com.android.dialer"
    )

    // Bloom Filter để dedup
    private val killedFilter = BloomFilter(1024, 3)
    private var sessionKilledCount = 0

    data class AppImportance(
        val packageName: String,
        val score: Float,
        val reason: String = ""
    )

    /**
     * Kill apps thông minh dựa trên PageRank-inspired scoring
     * @param excludePackages Các app cần giữ lại (game đang chạy)
     * @param aggressive Nếu true, kill nhiều hơn (70% thay vì 50%)
     */
    suspend fun killSmartly(
        excludePackages: Set<String>,
        aggressive: Boolean = false
    ): KillResult = withContext(Dispatchers.IO) {
        val allApps = getAllThirdPartyApps()
            .filter { it !in neverKill }
            .filter { it !in excludePackages }
            .filter { !killedFilter.mightContain(it) }

        // Score từng app
        val scoredApps = allApps.map { pkg ->
            val importance = calculateImportanceScore(pkg)
            AppImportance(pkg, importance.score, importance.reason)
        }.sortedByDescending { it.score }

        // Chỉ kill app có score thấp (không quan trọng)
        val killRatio = if (aggressive) 0.7f else 0.5f
        val toKill = scoredApps.takeLast((scoredApps.size * killRatio).toInt())
            .filter { it.score < 50f } // Không kill app có score > 50

        // Batch execute via Shizuku
        val killed = mutableListOf<String>()
        val errors = mutableListOf<String>()

        // Chia batch mỗi lần 10 app để tránh overload
        toKill.chunked(10).forEach { batch ->
            val commands = batch.joinToString(" && ") { app ->
                "am force-stop ${app.packageName} 2>/dev/null"
            }

            if (commands.isNotEmpty()) {
                val result = shizukuManager.executeShellCommand(commands)
                if (result.isSuccess) {
                    batch.forEach { app ->
                        killedFilter.put(app.packageName)
                        killed.add(app.packageName)
                        sessionKilledCount++
                    }
                } else {
                    errors.add("Batch kill failed: ${result.exceptionOrNull()?.message}")
                }
            }
        }

        KillResult(
            killedCount = killed.size,
            killed = killed,
            errors = errors,
            totalKilledSession = sessionKilledCount
        )
    }

    /**
     * PageRank-inspired importance scoring
     * Càng nhiều yếu tố => score càng cao => càng KHÔNG bị kill
     */
    private fun calculateImportanceScore(pkg: String): AppImportance {
        var score = 0f
        val reasons = mutableListOf<String>()

        try {
            val pm = context.packageManager
            val ai = pm.getApplicationInfo(pkg, 0)

            // 1. Recently used (30 phút gần đây = rất quan trọng)
            val usageStats = getUsageStats(pkg)
            val hoursSinceLastUse = if (usageStats.lastTimeUsed > 0) {
                (System.currentTimeMillis() - usageStats.lastTimeUsed) / 3600000f
            } else 999f

            if (hoursSinceLastUse < 0.5f) {
                score += 100f
                reasons.add("vừa dùng")
            } else if (hoursSinceLastUse < 2f) {
                score += 50f
                reasons.add("dùng gần đây")
            }

            // 2. Foreground time (càng nhiều càng quan trọng)
            val hoursForeground = usageStats.totalTimeInForeground / 3600000f
            score += hoursForeground.coerceAtMost(20f)
            if (hoursForeground > 10f) reasons.add("thường dùng")

            // 3. Protected category (music, VoIP)
            if (protectedCategories.any { pkg.startsWith(it) }) {
                score += 100f
                reasons.add("music/voip")
            }

            // 4. Has foreground service (đang chạy ngầm = quan trọng)
            try {
                val runningServices = context.getSystemService(Context.ACTIVITY_SERVICE)
                    ?.let { it as? android.app.ActivityManager }
                    ?.getRunningServices(Int.MAX_VALUE)
                if (runningServices?.any { s -> s.service?.packageName == pkg } == true) {
                    score += 30f
                    reasons.add("dang chay service")
                }
            } catch (e: Exception) { }

            // 5. Persistent app
            if (ai.flags and ApplicationInfo.FLAG_PERSISTENT != 0) {
                score += 50f
                reasons.add("persistent")
            }

            // 6. Has launcher icon (ứng dụng thật, không phải service)
            if (pm.getLaunchIntentForPackage(pkg) != null) {
                score += 10f
            }

            // 7. System update / downloaded
            if (ai.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0) {
                score += 5f
            }

        } catch (e: Exception) {
            // App bị uninstall trong lúc scan
            score = -100f // Cho kill ngay
        }

        return AppImportance(pkg, score, reasons.joinToString(", "))
    }

    private fun getUsageStats(pkg: String): android.app.usage.UsageStats {
        return try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val now = System.currentTimeMillis()
            usm.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                now - 24 * 60 * 60 * 1000,
                now
            )?.firstOrNull { it.packageName == pkg }
                ?: android.app.usage.UsageStats()
        } catch (e: Exception) {
            android.app.usage.UsageStats()
        }
    }

    private fun getAllThirdPartyApps(): List<String> {
        return try {
            val pm = context.packageManager
            pm.getInstalledApplications(PackageManager.MATCH_UNINSTALLED_PACKAGES)
                .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
                .map { it.packageName }
        } catch (e: Exception) { emptyList() }
    }

    fun resetSession() {
        killedFilter.clear()
        sessionKilledCount = 0
    }

    data class KillResult(
        val killedCount: Int,
        val killed: List<String>,
        val errors: List<String>,
        val totalKilledSession: Int
    )
}
