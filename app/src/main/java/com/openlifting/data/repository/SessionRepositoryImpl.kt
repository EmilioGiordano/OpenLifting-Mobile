package com.openlifting.data.repository

import androidx.room.withTransaction
import com.openlifting.data.local.OpenLiftingDatabase
import com.openlifting.data.local.dao.AthleteProfileDao
import com.openlifting.data.local.dao.SessionDao
import com.openlifting.data.local.dao.SetDao
import com.openlifting.data.local.entity.MuscleActivationEntity
import com.openlifting.data.local.entity.RepEntity
import com.openlifting.data.local.entity.RecommendationEntity
import com.openlifting.data.local.entity.SetMetricsEntity
import com.openlifting.data.local.entity.TrainingSessionEntity
import com.openlifting.data.local.entity.TrainingSetEntity
import com.openlifting.data.mapper.buildPostSetRequest
import com.openlifting.data.mapper.toEntity
import com.openlifting.data.mapper.toIsoInstant
import com.openlifting.data.remote.api.VortexSessionApi
import com.openlifting.data.remote.api.VortexInstructorApi
import com.openlifting.data.remote.dto.CreateGuestSessionRequest
import com.openlifting.data.remote.dto.CreateSessionRequest
import com.openlifting.data.remote.dto.EndSessionRequest
import com.openlifting.data.remote.dto.PatchSessionRequest
import com.openlifting.data.remote.dto.PostSetRequest
import com.openlifting.data.remote.dto.SessionWithSetsDto
import com.openlifting.data.remote.dto.TrainingSessionDto
import com.openlifting.domain.model.DeviceSource
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
    private val setDao: SetDao,
    private val athleteProfileDao: AthleteProfileDao,
    private val sessionApi: VortexSessionApi,
    private val instructorApi: VortexInstructorApi
) : SessionRepository {

    override suspend fun createSession(athleteUserId: Long, instructorUserId: Long?): Long {
        val now = System.currentTimeMillis()
        val guestProfileServerId = athleteProfileDao.getByUserId(athleteUserId)?.guestProfileServerId

        if (guestProfileServerId != null) {
            // Online-first for guest sessions: the coach NEEDS a backend session id to post
            // sets and to issue claim codes. If POST /api/instructor/sessions fails (no
            // connection, guest already claimed, instructor not authorised...) we surface the
            // error by returning -1 so the ViewModel can show a real message — instead of
            // silently dropping into a Room-only state that later blocks claim-code generation
            // with a confusing "todavía no se sincronizó" guard.
            val remote = tryRemoteCreateGuest(guestProfileServerId, now) ?: return -1L
            val entity = TrainingSessionEntity(
                serverId         = remote.id,
                athleteUserId    = athleteUserId,
                instructorUserId = instructorUserId,
                exercise         = remote.exercise,
                startedAt        = now,
                deviceSource     = remote.deviceSource,
                synced           = true
            )
            return sessionDao.insert(entity)
        }

        // Athlete-owned session: offline-first is fine — the athlete can measure offline and
        // sync later via syncSessionsFromBackend.
        val remote = tryRemoteCreate(now)
        val entity = if (remote != null) {
            TrainingSessionEntity(
                serverId         = remote.id,
                athleteUserId    = athleteUserId,
                instructorUserId = instructorUserId,
                exercise         = remote.exercise,
                startedAt        = now,
                deviceSource     = remote.deviceSource,
                synced           = true
            )
        } else {
            TrainingSessionEntity(
                athleteUserId    = athleteUserId,
                instructorUserId = instructorUserId,
                startedAt        = now,
                synced           = false
            )
        }
        return sessionDao.insert(entity)
    }

    override suspend fun endSession(sessionLocalId: Long) {
        val session = sessionDao.getById(sessionLocalId) ?: return
        val now = System.currentTimeMillis()
        val remoteOk = session.serverId?.let { tryRemoteEnd(it, now) } ?: false
        sessionDao.update(
            session.copy(
                endedAt = now,
                synced  = session.synced && remoteOk
            )
        )
    }

    override suspend fun syncSessionsFromBackend(athleteUserId: Long): Int? {
        val response = try {
            sessionApi.listSessions(page = 1)
        } catch (_: Exception) {
            return null
        }
        if (!response.isSuccessful) return null
        val dtos = response.body()?.data ?: return 0
        for (dto in dtos) {
            val existing = sessionDao.getByServerId(dto.id)
            val entity = dto.toEntity(
                athleteUserId    = athleteUserId,
                instructorUserId = existing?.instructorUserId,
                existingLocalId  = existing?.localId ?: 0
            )
            if (existing == null) sessionDao.insert(entity) else sessionDao.update(entity)

            // GET /api/sessions (list) omits the `sets` key. Without this second call the local
            // session row would stay with zero children, which is what the user sees as
            // "0 series · 0 kg · 9 kg MAX" in history after a fresh login against a seeded DB.
            // The N+1 is acceptable here — a typical athlete has < 30 sessions.
            hydrateSessionFromBackend(dto.id)
        }
        return dtos.size
    }

    private suspend fun tryRemoteCreateGuest(
        guestProfileServerId: Long,
        startedAtMs: Long
    ): TrainingSessionDto? = try {
        val res = instructorApi.createGuestSession(
            CreateGuestSessionRequest(
                guestProfileId = guestProfileServerId,
                startedAt      = startedAtMs.toIsoInstant()
            )
        )
        if (res.isSuccessful) res.body() else null
    } catch (_: Exception) {
        null
    }

    private suspend fun tryRemoteCreate(startedAtMs: Long): TrainingSessionDto? = try {
        val res = sessionApi.createSession(
            CreateSessionRequest(startedAt = startedAtMs.toIsoInstant())
        )
        if (res.isSuccessful) res.body() else null
    } catch (_: Exception) {
        null
    }

    private suspend fun tryRemoteEnd(serverId: Long, endedAtMs: Long): Boolean = try {
        sessionApi.endSession(serverId, EndSessionRequest(endedAt = endedAtMs.toIsoInstant()))
            .isSuccessful
    } catch (_: Exception) {
        false
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
    ): Long {
        val setId = db.withTransaction {
            val id = setDao.insertSet(
                TrainingSetEntity(
                    sessionLocalId = sessionLocalId,
                    setNumber      = setNumber,
                    loadKg         = loadKg,
                    targetReps     = targetReps,
                    variant        = variant.name,
                    depth          = depth.name,
                    rpe            = rpe
                )
            )
            activationsByRep.forEachIndexed { index, repActivations ->
                val repId = setDao.insertRep(RepEntity(setLocalId = id, repNumber = index + 1))
                setDao.insertActivations(repActivations.map { it.toEntity(repId) })
            }
            setDao.insertMetrics(metrics.copy(setLocalId = id).toEntity())
            setDao.insertRecommendations(recommendations.map { it.toEntity(id) })
            id
        }

        val sessionServerId = sessionDao.getById(sessionLocalId)?.serverId
        if (sessionServerId != null) {
            val request = buildPostSetRequest(
                setNumber        = setNumber,
                loadKg           = loadKg,
                targetReps       = targetReps,
                variant          = variant,
                depth            = depth,
                rpe              = rpe,
                activationsByRep = activationsByRep,
                metrics          = metrics,
                recommendations  = recommendations
            )
            val remoteId = tryRemotePostSet(sessionServerId, request)
            if (remoteId != null) setDao.markSynced(localId = setId, serverId = remoteId)
        }
        return setId
    }

    override suspend fun updateSessionDeviceSource(sessionLocalId: Long, source: DeviceSource) {
        val session = sessionDao.getById(sessionLocalId) ?: return
        if (session.deviceSource == source.name) return    // already correct, skip the round-trip
        val remoteOk = session.serverId?.let { tryRemotePatchDeviceSource(it, source) } ?: false
        sessionDao.update(
            session.copy(
                deviceSource = source.name,
                synced       = session.synced && (session.serverId == null || remoteOk)
            )
        )
    }

    override suspend fun hydrateSessionFromBackend(sessionServerId: Long): Long? {
        val response = try {
            sessionApi.getSessionWithSets(sessionServerId)
        } catch (_: Exception) {
            return null
        }
        if (!response.isSuccessful) return null
        val dto = response.body() ?: return null
        val athleteUserId = sessionDao.getByServerId(sessionServerId)?.athleteUserId
            ?: sessionDao.getRecentForAthlete(0L, 1).firstOrNull()?.athleteUserId
            ?: return null
        // ↑ fallback rare; the claim flow already inserted the session row before calling us,
        // so getByServerId should resolve. The second fallback only fires if a caller hydrates
        // a brand-new session without inserting first — defensive, not a real path today.

        return db.withTransaction {
            val existing = sessionDao.getByServerId(sessionServerId)
            val sessionEntity = dto.toLocalSessionEntity(
                athleteUserId    = existing?.athleteUserId ?: athleteUserId,
                instructorUserId = existing?.instructorUserId,
                existingLocalId  = existing?.localId ?: 0L
            )
            val localId = if (existing == null) {
                sessionDao.insert(sessionEntity)
            } else {
                sessionDao.update(sessionEntity); existing.localId
            }
            // Wipe any prior children and rebuild from the response. CASCADE on FK takes care
            // of reps, activations, metrics and recommendations transitively.
            setDao.deleteSetsForSession(localId)
            for (setDto in dto.sets) insertNestedSet(setDto, localId)
            localId
        }
    }

    private suspend fun insertNestedSet(
        setDto: com.openlifting.data.remote.dto.NestedSetDto,
        sessionLocalId: Long
    ) {
        val setLocalId = setDao.insertSet(
            TrainingSetEntity(
                serverId       = setDto.id,
                sessionLocalId = sessionLocalId,
                setNumber      = setDto.setNumber,
                loadKg         = setDto.loadKg.toFloat(),
                targetReps     = setDto.targetReps,
                variant        = setDto.variant,
                depth          = setDto.depth,
                rpe            = setDto.rpe.toFloat(),
                synced         = true
            )
        )
        for (rep in setDto.reps) {
            val repLocalId = setDao.insertRep(
                RepEntity(
                    setLocalId = setLocalId,
                    repNumber  = rep.repNumber,
                    durationMs = rep.durationMs.toInt()
                )
            )
            if (rep.activations.isNotEmpty()) {
                setDao.insertActivations(
                    rep.activations.map { a ->
                        MuscleActivationEntity(
                            repId          = repLocalId,
                            muscle         = a.muscle,
                            side           = a.side,
                            percentMvc     = a.percentMvc.toFloat(),
                            peakPercentMvc = a.peakPercentMvc.toFloat()
                        )
                    }
                )
            }
        }
        setDto.metrics?.let { m ->
            setDao.insertMetrics(
                SetMetricsEntity(
                    setLocalId           = setLocalId,
                    bsaVlPct             = m.bsaVlPct.toFloat(),
                    bsaVmPct             = m.bsaVmPct.toFloat(),
                    bsaGmaxPct           = m.bsaGmaxPct.toFloat(),
                    bsaEsPct             = m.bsaEsPct.toFloat(),
                    hqRatio              = m.hqRatio.toFloat(),
                    esGmaxRatio          = m.esGmaxRatio.toFloat(),
                    intraSetFatigueRatio = m.intraSetFatigueRatio.toFloat(),
                    thresholdsVersion    = m.thresholdsVersion
                )
            )
        }
        if (setDto.recommendations.isNotEmpty()) {
            setDao.insertRecommendations(
                setDto.recommendations.map { r ->
                    RecommendationEntity(
                        setLocalId = setLocalId,
                        text       = r.text,
                        severity   = r.severity,
                        evidence   = r.evidence.orEmpty()
                    )
                }
            )
        }
    }

    private fun SessionWithSetsDto.toLocalSessionEntity(
        athleteUserId: Long,
        instructorUserId: Long?,
        existingLocalId: Long
    ): TrainingSessionEntity = TrainingSessionEntity(
        localId          = existingLocalId,
        serverId         = id,
        athleteUserId    = athleteUserId,
        instructorUserId = instructorUserId,
        exercise         = exercise,
        startedAt        = com.openlifting.data.mapper.parseIsoOrNow(startedAt),
        endedAt          = endedAt?.let { com.openlifting.data.mapper.parseIsoOrNull(it) },
        deviceSource     = deviceSource,
        synced           = true
    )

    private suspend fun tryRemotePostSet(
        sessionServerId: Long,
        request: PostSetRequest
    ): Long? = try {
        val res = sessionApi.postSet(sessionServerId, request)
        if (res.isSuccessful) res.body()?.id else null
    } catch (_: Exception) {
        null
    }

    private suspend fun tryRemotePatchDeviceSource(
        sessionServerId: Long,
        source: DeviceSource
    ): Boolean = try {
        sessionApi.patchSession(sessionServerId, PatchSessionRequest(deviceSource = source.name))
            .isSuccessful
    } catch (_: Exception) {
        false
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
