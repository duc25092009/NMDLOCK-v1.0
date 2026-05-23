package com.nmdlock.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * ═══════════════════════════════════════════════════════════════════════
 * PILLAR 6: ROOM DATABASE — Profile learning data
 * ═══════════════════════════════════════════════════════════════════════
 * Lưu lịch sử gaming session, profile performance cho Multi-Armed Bandit
 */

// ── Entity ──

@Entity(tableName = "game_sessions")
data class GameSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gamePackage: String,
    val profileId: String,
    val deviceModel: String,
    val avgFps: Float,
    val onePercentLowFps: Int,
    val fpsVariance: Float,
    val jankCount: Int,
    val maxTemp: Float,
    val batteryDrainPercent: Float,
    val durationMinutes: Int,
    val rewardScore: Float,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "profile_stats")
data class ProfileStatsEntity(
    @PrimaryKey
    val profileId: String,
    val gamePackage: String,
    val deviceModel: String,
    val totalTrials: Int = 0,
    val totalReward: Float = 0f,
    val avgReward: Float = 0f,
    val lastUsed: Long = 0
)

// ── DAO ──

@Dao
interface GameSessionDao {
    @Query("SELECT * FROM game_sessions ORDER BY timestamp DESC LIMIT 100")
    fun getRecentSessions(): Flow<List<GameSessionEntity>>

    @Query("SELECT * FROM game_sessions WHERE gamePackage = :pkg ORDER BY timestamp DESC")
    fun getSessionsByGame(pkg: String): Flow<List<GameSessionEntity>>

    @Query("SELECT * FROM profile_stats WHERE gamePackage = :game AND deviceModel = :device")
    fun getProfileStats(game: String, device: String): Flow<List<ProfileStatsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: GameSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfileStats(stats: ProfileStatsEntity)

    @Query("""
        SELECT * FROM profile_stats 
        WHERE gamePackage = :game AND deviceModel = :device
        ORDER BY avgReward DESC
    """)
    suspend fun getBestProfiles(game: String, device: String): List<ProfileStatsEntity>
}

@Database(
    entities = [GameSessionEntity::class, ProfileStatsEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SessionDatabase : RoomDatabase() {
    abstract fun gameSessionDao(): GameSessionDao
}
