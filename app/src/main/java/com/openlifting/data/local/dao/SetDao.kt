package com.openlifting.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.openlifting.data.local.entity.MuscleActivationEntity
import com.openlifting.data.local.entity.RepEntity
import com.openlifting.data.local.entity.RecommendationEntity
import com.openlifting.data.local.entity.SetMetricsEntity
import com.openlifting.data.local.entity.TrainingSetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SetDao {
    @Insert
    suspend fun insertSet(set: TrainingSetEntity): Long

    @Insert
    suspend fun insertRep(rep: RepEntity): Long

    @Insert
    suspend fun insertActivations(activations: List<MuscleActivationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetrics(metrics: SetMetricsEntity)

    @Insert
    suspend fun insertRecommendations(recommendations: List<RecommendationEntity>)

    @Query("SELECT * FROM training_sets WHERE sessionLocalId = :sessionId ORDER BY setNumber")
    fun observeSetsForSession(sessionId: Long): Flow<List<TrainingSetEntity>>

    @Query("SELECT * FROM training_sets WHERE sessionLocalId = :sessionId ORDER BY setNumber")
    suspend fun getSetsForSession(sessionId: Long): List<TrainingSetEntity>

    @Query("SELECT * FROM reps WHERE setLocalId = :setId ORDER BY repNumber")
    suspend fun getRepsForSet(setId: Long): List<RepEntity>

    @Query("SELECT * FROM muscle_activations WHERE repId = :repId")
    suspend fun getActivationsForRep(repId: Long): List<MuscleActivationEntity>

    @Query("SELECT * FROM set_metrics WHERE setLocalId = :setId")
    suspend fun getMetricsForSet(setId: Long): SetMetricsEntity?

    @Query("SELECT * FROM recommendations WHERE setLocalId = :setId ORDER BY severity DESC")
    suspend fun getRecommendationsForSet(setId: Long): List<RecommendationEntity>

    @Query("SELECT * FROM training_sets WHERE synced = 0")
    suspend fun getUnsynced(): List<TrainingSetEntity>

    @Query("UPDATE training_sets SET serverId = :serverId, synced = 1 WHERE localId = :localId")
    suspend fun markSynced(localId: Long, serverId: Long)
}
