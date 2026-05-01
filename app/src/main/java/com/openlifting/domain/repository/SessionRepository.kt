package com.openlifting.domain.repository

import com.openlifting.domain.model.MuscleActivation
import com.openlifting.domain.model.Recommendation
import com.openlifting.domain.model.SetMetrics
import com.openlifting.domain.model.SquatDepth
import com.openlifting.domain.model.SquatVariant
import com.openlifting.domain.model.TrainingSession
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    suspend fun createSession(athleteUserId: Long): Long
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
}
