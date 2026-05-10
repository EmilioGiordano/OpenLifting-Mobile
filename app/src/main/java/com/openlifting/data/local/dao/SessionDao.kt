package com.openlifting.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.openlifting.data.local.entity.MetricsTrendItem
import com.openlifting.data.local.entity.TrainingSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: TrainingSessionEntity): Long

    @Update
    suspend fun update(session: TrainingSessionEntity)

    @Query("SELECT * FROM training_sessions WHERE localId = :id LIMIT 1")
    suspend fun getById(id: Long): TrainingSessionEntity?

    @Query("SELECT * FROM training_sessions WHERE athleteUserId = :userId ORDER BY startedAt DESC")
    fun observeForAthlete(userId: Long): Flow<List<TrainingSessionEntity>>

    @Query("SELECT * FROM training_sessions WHERE athleteUserId = :userId ORDER BY startedAt DESC LIMIT :limit")
    suspend fun getRecentForAthlete(userId: Long, limit: Int = 10): List<TrainingSessionEntity>

    @Query("UPDATE training_sessions SET endedAt = :endedAt WHERE localId = :id")
    suspend fun endSession(id: Long, endedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM training_sessions WHERE synced = 0")
    suspend fun getUnsynced(): List<TrainingSessionEntity>

    @Query("UPDATE training_sessions SET serverId = :serverId, synced = 1 WHERE localId = :localId")
    suspend fun markSynced(localId: Long, serverId: Long)

    @Query("SELECT * FROM training_sessions WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: Long): TrainingSessionEntity?

    @Query("SELECT * FROM training_sessions WHERE athleteUserId = :userId ORDER BY startedAt DESC LIMIT 1")
    suspend fun getLastSession(userId: Long): TrainingSessionEntity?

    @Query("""
        SELECT sm.bsaVlPct, sm.bsaGmaxPct, sm.esGmaxRatio, ts.startedAt
        FROM set_metrics sm
        JOIN training_sets tset ON sm.setLocalId = tset.localId
        JOIN training_sessions ts ON tset.sessionLocalId = ts.localId
        WHERE ts.athleteUserId = :userId
        ORDER BY ts.startedAt DESC
        LIMIT :limit
    """)
    suspend fun getRecentMetrics(userId: Long, limit: Int = 30): List<MetricsTrendItem>
}
