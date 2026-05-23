package com.nmdlock.app.core.services

import android.content.Context
import com.nmdlock.app.data.local.GameSessionEntity
import com.nmdlock.app.data.local.ProfileStatsEntity
import com.nmdlock.app.data.local.SessionDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.room.Room
import javax.inject.Singleton
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * ═══════════════════════════════════════════════════════════════════════
 * PILLAR 6: AI-POWERED AUTO-LEARNING PROFILES
 * ═══════════════════════════════════════════════════════════════════════
 *
 * Multi-Armed Bandit (Epsilon-Greedy) — Auto-learn best profile
 * - 90% Exploit: chọn profile có reward cao nhất
 * - 10% Explore: thử profile ngẫu nhiên
 *
 * Reward = FPS_avg × stability / (temperature_penalty × battery_drain)
 */
@Singleton
class ProfileAutoLearner(
    private val context: Context
) {
    private val db by lazy {
        Room.databaseBuilder(
            context,
            SessionDatabase::class.java,
            "nmdlock_sessions.db"
        ).build()
    }

    private val _currentArm = MutableStateFlow<String?>(null)
    val currentArm: StateFlow<String?> = _currentArm.asStateFlow()

    // Epsilon-Greedy parameters
    private var epsilon = 0.1f  // 10% exploration
    private val random = Random

    data class SessionStats(
        val avgFps: Float,
        val onePercentLow: Int,
        val fpsVariance: Float,
        val jankCount: Int,
        val maxTemp: Float,
        val batteryDrainPercent: Float,
        val durationMinutes: Int
    )

    /**
     * Lấy danh sách best profiles (sync version cho UI polling)
     */
    suspend fun getBestProfilesSync(game: String, device: String): List<ProfileStatsEntity> {
        return db.gameSessionDao().getBestProfiles(game, device)
    }

    /**
     * Epsilon-Greedy selection
     * 90% exploit best profile, 10% explore new ones
     */
    suspend fun selectProfile(game: String, device: String): String {
        val stats = db.gameSessionDao().getBestProfiles(game, device)

        return if (stats.isEmpty() || random.nextFloat() < epsilon) {
            // EXPLORE: random profile
            val profiles = listOf(
                "gaming_smooth", "gaming_balanced", "gaming_performance",
                "gaming_powersave", "ff_pro", "ff_max_pro"
            )
            profiles.random()
        } else {
            // EXPLOIT: best arm
            stats.maxByOrNull { it.avgReward }?.profileId ?: "gaming_balanced"
        }
    }

    /**
     * Calculate reward và persist vào database
     * Reward = FPS_avg * stability / (temperature_penalty * battery_drain)
     */
    suspend fun recordSession(
        profileId: String,
        gamePackage: String,
        deviceModel: String,
        stats: SessionStats
    ) {
        val reward = calculateReward(stats)

        // Insert session record
        db.gameSessionDao().insertSession(
            GameSessionEntity(
                gamePackage = gamePackage,
                profileId = profileId,
                deviceModel = deviceModel,
                avgFps = stats.avgFps,
                onePercentLowFps = stats.onePercentLow,
                fpsVariance = stats.fpsVariance,
                jankCount = stats.jankCount,
                maxTemp = stats.maxTemp,
                batteryDrainPercent = stats.batteryDrainPercent,
                durationMinutes = stats.durationMinutes,
                rewardScore = reward
            )
        )

        // Update profile stats
        val existring = db.gameSessionDao().getBestProfiles(gamePackage, deviceModel)
            .firstOrNull { it.profileId == profileId }

        val newTrials = (existring?.totalTrials ?: 0) + 1
        val newTotalReward = (existring?.totalReward ?: 0f) + reward

        db.gameSessionDao().upsertProfileStats(
            ProfileStatsEntity(
                profileId = profileId,
                gamePackage = gamePackage,
                deviceModel = deviceModel,
                totalTrials = newTrials,
                totalReward = newTotalReward,
                avgReward = newTotalReward / newTrials,
                lastUsed = System.currentTimeMillis()
            )
        )
    }

    /**
     * Multi-factor reward calculation
     * Output: 0.0 - 2.0 (cao hơn = profile tốt hơn)
     */
    private fun calculateReward(stats: SessionStats): Float {
        // FPS component (higher = better)
        val fpsScore = (stats.avgFps / 60f).coerceAtMost(1f)

        // Stability (lower variance = better)
        val stabilityScore = (1f - (stats.fpsVariance / 100f)).coerceIn(0f, 1f)

        // 1% low (avoid stutter - quan trọng cho gaming)
        val lowFpsScore = (stats.onePercentLow / 30f).coerceAtMost(1f)

        // Temperature penalty
        val tempPenalty = if (stats.maxTemp > 45) {
            (1f + (stats.maxTemp - 45f) / 10f)
        } else 1f

        // Jank penalty (micro-stutter)
        val jankPenalty = 1f + (stats.jankCount.toFloat() / 200f).coerceAtMost(1f)

        // Battery drain penalty
        val batteryPenalty = 1f + (stats.batteryDrainPercent / 50f).coerceAtMost(1f)

        // Final reward
        return (fpsScore * 0.35f +
                stabilityScore * 0.20f +
                lowFpsScore * 0.25f) / (tempPenalty * jankPenalty * batteryPenalty / 3f)
    }
}
