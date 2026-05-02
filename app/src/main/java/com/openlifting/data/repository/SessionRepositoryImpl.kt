package com.openlifting.data.repository

import androidx.room.withTransaction
import com.openlifting.data.local.OpenLiftingDatabase
import com.openlifting.data.local.dao.SessionDao
import com.openlifting.data.local.dao.SetDao
import com.openlifting.data.local.entity.MuscleActivationEntity
import com.openlifting.data.local.entity.RepEntity
import com.openlifting.data.local.entity.RecommendationEntity
import com.openlifting.data.local.entity.SetMetricsEntity
import com.openlifting.data.local.entity.TrainingSessionEntity
import com.openlifting.data.local.entity.TrainingSetEntity
import com.openlifting.domain.model.MuscleActivation
import com.openlifting.domain.model.Recommendation
import com.openlifting.domain.model.RiskLevel
import com.openlifting.domain.model.SetMetrics
import com.openlifting.domain.model.SquatDepth
import com.openlifting.domain.model.SquatVariant
import com.openlifting.domain.model.TrainingSession
import com.openlifting.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SessionRepositoryImpl @Inject constructor(
    private val db: OpenLiftingDatabase,
    private val sessionDao: SessionDao,
    private val setDao: SetDao
) : SessionRepository {

    override suspend fun createSession(athleteUserId: Long, instructorUserId: Long?): Long =
        sessionDao.insert(TrainingSessionEntity(
            athleteUserId    = athleteUserId,
            instructorUserId = instructorUserId
        ))

    override suspend fun endSession(sessionLocalId: Long) {
        val session = sessionDao.getById(sessionLocalId) ?: return
        sessionDao.update(session.copy(endedAt = System.currentTimeMillis()))
    }

    override suspend fun saveSetWithDetails(
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
    ): Long = db.withTransaction {
        val setId = setDao.insertSet(
            TrainingSetEntity(
                sessionLocalId = sessionLocalId,
                setNumber = setNumber,
                loadKg = loadKg,
                targetReps = targetReps,
                variant = variant.name,
                depth = depth.name,
                rpe = rpe
            )
        )
        activationsByRep.forEachIndexed { index, repActivations ->
            val repId = setDao.insertRep(RepEntity(setLocalId = setId, repNumber = index + 1))
            setDao.insertActivations(repActivations.map { it.toEntity(repId) })
        }
        setDao.insertMetrics(metrics.copy(setLocalId = setId).toEntity())
        setDao.insertRecommendations(recommendations.map { it.toEntity(setId) })
        setId
    }

    override fun observeSessionsForAthlete(userId: Long): Flow<List<TrainingSession>> =
        sessionDao.observeForAthlete(userId).map { list ->
            list.map { entity ->
                TrainingSession(
                    localId = entity.localId,
                    serverId = entity.serverId,
                    athleteUserId = entity.athleteUserId,
                    startedAt = entity.startedAt,
                    endedAt = entity.endedAt,
                    synced = entity.synced
                )
            }
        }

    override suspend fun getMetrics(setLocalId: Long): SetMetrics? =
        setDao.getMetricsForSet(setLocalId)?.toDomain()

    override suspend fun getRecommendations(setLocalId: Long): List<Recommendation> =
        setDao.getRecommendationsForSet(setLocalId).map { it.toDomain() }

    override suspend fun getActivationsForSet(setLocalId: Long): List<List<MuscleActivation>> {
        val reps = setDao.getRepsForSet(setLocalId)
        return reps.map { rep -> setDao.getActivationsForRep(rep.id).map { it.toDomain() } }
    }

    // Mappers
    private fun MuscleActivation.toEntity(repId: Long) = MuscleActivationEntity(
        repId = repId, muscle = muscle.name, side = side.name,
        percentMvc = percentMvc, peakPercentMvc = peakPercentMvc
    )

    private fun SetMetrics.toEntity() = SetMetricsEntity(
        setLocalId = setLocalId, bsaVlPct = bsaVlPct, bsaVmPct = bsaVmPct,
        bsaGmaxPct = bsaGmaxPct, bsaEsPct = bsaEsPct, hqRatio = hqRatio,
        esGmaxRatio = esGmaxRatio, intraSetFatigueRatio = intraSetFatigueRatio,
        thresholdsVersion = thresholdsVersion
    )

    private fun SetMetricsEntity.toDomain() = SetMetrics(
        setLocalId = setLocalId, bsaVlPct = bsaVlPct, bsaVmPct = bsaVmPct,
        bsaGmaxPct = bsaGmaxPct, bsaEsPct = bsaEsPct, hqRatio = hqRatio,
        esGmaxRatio = esGmaxRatio, intraSetFatigueRatio = intraSetFatigueRatio,
        thresholdsVersion = thresholdsVersion
    )

    private fun Recommendation.toEntity(setId: Long) = RecommendationEntity(
        setLocalId = setId, text = text, severity = severity.name, evidence = evidence
    )

    private fun RecommendationEntity.toDomain() = Recommendation(
        id = id, setLocalId = setLocalId, text = text,
        severity = RiskLevel.valueOf(severity), evidence = evidence
    )

    private fun MuscleActivationEntity.toDomain(): MuscleActivation {
        val m = com.openlifting.domain.model.Muscle.valueOf(muscle)
        val s = com.openlifting.domain.model.MuscleSide.valueOf(side)
        return MuscleActivation(id = id, repId = repId, muscle = m, side = s,
            percentMvc = percentMvc, peakPercentMvc = peakPercentMvc)
    }
}
