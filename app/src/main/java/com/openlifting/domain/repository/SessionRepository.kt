package com.openlifting.domain.repository

import com.openlifting.domain.model.DeviceSource
import com.openlifting.domain.model.MuscleActivation
import com.openlifting.domain.model.Recommendation
import com.openlifting.domain.model.SetMetrics
import com.openlifting.domain.model.SquatDepth
import com.openlifting.domain.model.SquatVariant
import com.openlifting.domain.model.TrainingSession
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    /**
     * Creates a new training session for the given athlete. When started by an instructor
     * for one of their athletes (e.g., a guest), [instructorUserId] is set so the session
     * row records who supervised it.
     */
    suspend fun createSession(athleteUserId: Long, instructorUserId: Long? = null): Long
    suspend fun saveSetWithDetails(
        sessionLocalId: Long,
        setNumber: Int,
        loadKg: Float,
        targetReps: Int,
        variant: SquatVariant,
        depth: SquatDepth,
        rpe: Float,
        activationsByRep: List<List<MuscleActivation>>,
        metrics: SetMetrics,
        recommendations: List<Recommendation>
    ): Long
    suspend fun endSession(sessionLocalId: Long)
    fun observeSessionsForAthlete(userId: Long): Flow<List<TrainingSession>>
    suspend fun getMetrics(setLocalId: Long): SetMetrics?
    suspend fun getRecommendations(setLocalId: Long): List<Recommendation>
    suspend fun getActivationsForSet(setLocalId: Long): List<List<MuscleActivation>>

    /**
     * Pulls the athlete's sessions from Vortex and merges them into Room by [TrainingSessionEntity.serverId].
     * Local-only sessions ([TrainingSessionEntity.synced] == false) are not affected.
     * Returns the number of remote sessions seen, or null if the call failed.
     */
    suspend fun syncSessionsFromBackend(athleteUserId: Long): Int?

    /**
     * Updates the session's `device_source` both locally and on Vortex via PATCH.
     * Used after the first set completes once the EMG source is known. No-op if
     * the local session does not have a `serverId` (offline-only) — the local
     * row is updated regardless so the UI stays consistent.
     */
    suspend fun updateSessionDeviceSource(sessionLocalId: Long, source: DeviceSource)

    /**
     * Pulls the full session tree (sets + reps + activations + metrics + recommendations) from
     * Vortex's `GET /api/sessions/{id}` and rewrites the local rows for [sessionServerId].
     * Used after a claim to materialise the data on the athlete's device. Returns the local
     * session id (creating one if it didn't exist), or null if the fetch failed.
     */
    suspend fun hydrateSessionFromBackend(sessionServerId: Long): Long?
}
