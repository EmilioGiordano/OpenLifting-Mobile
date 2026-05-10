package com.openlifting.data.mapper

import com.openlifting.data.local.entity.MuscleActivationEntity
import com.openlifting.data.local.entity.RecommendationEntity
import com.openlifting.data.local.entity.SetMetricsEntity
import com.openlifting.data.local.entity.TrainingSessionEntity
import com.openlifting.data.local.entity.TrainingSetEntity
import com.openlifting.data.remote.dto.TrainingSessionDto
import com.openlifting.domain.model.Muscle
import com.openlifting.domain.model.MuscleActivation
import com.openlifting.domain.model.MuscleSide
import com.openlifting.domain.model.Recommendation
import com.openlifting.domain.model.RiskLevel
import com.openlifting.domain.model.SetMetrics
import com.openlifting.domain.model.SquatDepth
import com.openlifting.domain.model.SquatVariant
import com.openlifting.domain.model.TrainingSet
import java.time.Instant
import java.time.format.DateTimeParseException

/**
 * Public mappers for Set / Metrics / Recommendation / Activation entities.
 * Use these from ViewModels that need the domain models (and their computed risk properties)
 * instead of duplicating threshold logic.
 *
 * NOTE: SessionRepositoryImpl has private versions of the same conversions for historical
 * reasons. Those will be removed when we centralise data access through these mappers.
 */

fun SetMetricsEntity.toDomain(): SetMetrics = SetMetrics(
    setLocalId           = setLocalId,
    bsaVlPct             = bsaVlPct,
    bsaVmPct             = bsaVmPct,
    bsaGmaxPct           = bsaGmaxPct,
    bsaEsPct             = bsaEsPct,
    hqRatio              = hqRatio,
    esGmaxRatio          = esGmaxRatio,
    intraSetFatigueRatio = intraSetFatigueRatio,
    thresholdsVersion    = thresholdsVersion
)

fun RecommendationEntity.toDomain(): Recommendation = Recommendation(
    id         = id,
    setLocalId = setLocalId,
    text       = text,
    severity   = runCatching { RiskLevel.valueOf(severity) }.getOrDefault(RiskLevel.NORMAL),
    evidence   = evidence
)

fun MuscleActivationEntity.toDomain(): MuscleActivation = MuscleActivation(
    id           = id,
    repId        = repId,
    muscle       = Muscle.valueOf(muscle),
    side         = MuscleSide.valueOf(side),
    percentMvc   = percentMvc,
    peakPercentMvc = peakPercentMvc
)

fun TrainingSetEntity.toDomain(): TrainingSet = TrainingSet(
    localId        = localId,
    serverId       = serverId,
    sessionLocalId = sessionLocalId,
    setNumber      = setNumber,
    loadKg         = loadKg,
    targetReps     = targetReps,
    variant        = runCatching { SquatVariant.valueOf(variant) }.getOrDefault(SquatVariant.LOW_BAR),
    depth          = runCatching { SquatDepth.valueOf(depth) }.getOrDefault(SquatDepth.PARALLEL),
    rpe            = rpe,
    synced         = synced
)

fun TrainingSessionDto.toEntity(
    athleteUserId: Long,
    instructorUserId: Long? = null,
    existingLocalId: Long = 0
): TrainingSessionEntity = TrainingSessionEntity(
    localId          = existingLocalId,
    serverId         = id,
    athleteUserId    = athleteUserId,
    instructorUserId = instructorUserId,
    exercise         = exercise,
    startedAt        = parseIsoOrNow(startedAt),
    endedAt          = endedAt?.let(::parseIsoOrNull),
    deviceSource     = deviceSource,
    synced           = true
)

internal fun parseIsoOrNull(iso: String): Long? = try {
    Instant.parse(iso).toEpochMilli()
} catch (_: DateTimeParseException) {
    null
}

internal fun parseIsoOrNow(iso: String): Long =
    parseIsoOrNull(iso) ?: System.currentTimeMillis()

internal fun Long.toIsoInstant(): String = Instant.ofEpochMilli(this).toString()
