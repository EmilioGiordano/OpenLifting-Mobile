package com.openlifting.data.mapper

import com.openlifting.data.remote.dto.ActivationRequest
import com.openlifting.data.remote.dto.MetricsRequest
import com.openlifting.data.remote.dto.PostSetRequest
import com.openlifting.data.remote.dto.RecommendationRequest
import com.openlifting.data.remote.dto.RepRequest
import com.openlifting.domain.model.MuscleActivation
import com.openlifting.domain.model.Recommendation
import com.openlifting.domain.model.SetMetrics
import com.openlifting.domain.model.SquatDepth
import com.openlifting.domain.model.SquatVariant

/**
 * Builds the `POST /sets` payload from the domain inputs that
 * SessionRepository already has at hand. Reps that ended without producing
 * any activation (e.g. electrode disconnected mid-rep) are still sent — the
 * backend allows zero activations per rep.
 */
fun buildPostSetRequest(
    setNumber: Int,
    loadKg: Float,
    targetReps: Int,
    variant: SquatVariant,
    depth: SquatDepth,
    rpe: Float,
    activationsByRep: List<List<MuscleActivation>>,
    metrics: SetMetrics,
    recommendations: List<Recommendation>
): PostSetRequest = PostSetRequest(
    setNumber  = setNumber,
    loadKg     = loadKg.toDouble(),
    targetReps = targetReps,
    variant    = variant.name,
    depth      = depth.name,
    rpe        = rpe.toDouble(),
    reps       = activationsByRep.mapIndexed { index, activations ->
        RepRequest(
            repNumber   = index + 1,
            durationMs  = 0L,                       // not yet tracked client-side
            activations = activations.map { it.toRequest() }
        )
    },
    metrics         = metrics.toRequest(),
    recommendations = recommendations.map { it.toRequest() }
)

private fun MuscleActivation.toRequest(): ActivationRequest = ActivationRequest(
    muscle         = muscle.name,
    side           = side.name,
    percentMvc     = percentMvc.toDouble(),
    peakPercentMvc = peakPercentMvc.toDouble()
)

private fun SetMetrics.toRequest(): MetricsRequest = MetricsRequest(
    bsaVlPct             = bsaVlPct.toDouble(),
    bsaVmPct             = bsaVmPct.toDouble(),
    bsaGmaxPct           = bsaGmaxPct.toDouble(),
    bsaEsPct             = bsaEsPct.toDouble(),
    hqRatio              = hqRatio.toDouble(),
    esGmaxRatio          = esGmaxRatio.toDouble(),
    intraSetFatigueRatio = intraSetFatigueRatio.toDouble(),
    thresholdsVersion    = thresholdsVersion
)

private fun Recommendation.toRequest(): RecommendationRequest = RecommendationRequest(
    text     = text,
    severity = severity.name,
    evidence = evidence
)
