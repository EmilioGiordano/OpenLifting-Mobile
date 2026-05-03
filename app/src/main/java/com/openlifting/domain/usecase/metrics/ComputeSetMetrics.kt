package com.openlifting.domain.usecase.metrics

import com.openlifting.domain.model.Muscle
import com.openlifting.domain.model.MuscleActivation
import com.openlifting.domain.model.MuscleSide
import com.openlifting.domain.model.Recommendation
import com.openlifting.domain.model.RiskLevel
import com.openlifting.domain.model.SetMetrics
import javax.inject.Inject

/**
 * Computes SetMetrics and Recommendations from a flat list of MuscleActivations.
 * Pure domain logic — no Android or Room dependencies.
 *
 * Input: all activations for all reps of a set, with repNumber ordering.
 * The activations list is expected to be ordered rep1..repN.
 *
 * Las fórmulas (BSA, H:Q, ES:GMax, fatiga intra-serie) y los thresholds de
 * clasificación de riesgo están justificados en `docs/decisiones-tecnicas.md`
 * §3 ("Modelo de dominio — métricas y thresholds").
 *
 * Las recomendaciones generadas en español ([generateRecommendations]) son
 * heurísticas escritas en este file, no validadas por kinesiólogo. Suficientes
 * para defensa académica; uso clínico real requiere revisión profesional.
 */
class ComputeSetMetrics @Inject constructor() {

    data class Result(
        val metrics: SetMetrics,
        val recommendations: List<Recommendation>
    )

    operator fun invoke(
        setLocalId: Long,
        activationsByRep: List<List<MuscleActivation>>
    ): Result {
        if (activationsByRep.isEmpty()) return emptyResult(setLocalId)

        val allActivations = activationsByRep.flatten()

        // Average %MVC per muscle per side across all reps (concentric proxy)
        fun avg(muscle: Muscle, side: MuscleSide): Float =
            allActivations
                .filter { it.muscle == muscle && it.side == side }
                .map { it.percentMvc }
                .average()
                .toFloat()
                .takeIf { !it.isNaN() } ?: 0f

        // BSA = ((major - minor) / major) * 100
        fun bsa(muscle: Muscle): Float {
            val l = avg(muscle, MuscleSide.LEFT)
            val r = avg(muscle, MuscleSide.RIGHT)
            val major = maxOf(l, r)
            return if (major <= 0f) 0f else ((major - minOf(l, r)) / major) * 100f
        }

        val vlL  = avg(Muscle.VASTUS_LATERALIS, MuscleSide.LEFT)
        val vlR  = avg(Muscle.VASTUS_LATERALIS, MuscleSide.RIGHT)
        val vmL  = avg(Muscle.VASTUS_MEDIALIS,  MuscleSide.LEFT)
        val vmR  = avg(Muscle.VASTUS_MEDIALIS,  MuscleSide.RIGHT)
        val gmL  = avg(Muscle.GLUTEUS_MAXIMUS,  MuscleSide.LEFT)
        val gmR  = avg(Muscle.GLUTEUS_MAXIMUS,  MuscleSide.RIGHT)
        val esL  = avg(Muscle.ERECTOR_SPINAE,   MuscleSide.LEFT)
        val esR  = avg(Muscle.ERECTOR_SPINAE,   MuscleSide.RIGHT)
        val bfL  = avg(Muscle.BICEPS_FEMORIS,   MuscleSide.LEFT)
        val bfR  = avg(Muscle.BICEPS_FEMORIS,   MuscleSide.RIGHT)

        val quadAvg = (vlL + vlR + vmL + vmR) / 4f
        val gmAvg   = (gmL + gmR) / 2f
        val esAvg   = (esL + esR) / 2f
        val bfAvg   = (bfL + bfR) / 2f

        val hqRatio     = if (quadAvg <= 0f) 0f else bfAvg / quadAvg
        val esGmaxRatio = if (gmAvg   <= 0f) 0f else esAvg / gmAvg

        // Intra-set fatigue: last rep max activation vs first rep
        val firstRepMax = activationsByRep.first().maxOf { it.percentMvc }
        val lastRepMax  = activationsByRep.last().maxOf  { it.percentMvc }
        val fatigueRatio = if (firstRepMax <= 0f) 1f else lastRepMax / firstRepMax

        val metrics = SetMetrics(
            setLocalId             = setLocalId,
            bsaVlPct               = bsa(Muscle.VASTUS_LATERALIS),
            bsaVmPct               = bsa(Muscle.VASTUS_MEDIALIS),
            bsaGmaxPct             = bsa(Muscle.GLUTEUS_MAXIMUS),
            bsaEsPct               = bsa(Muscle.ERECTOR_SPINAE),
            hqRatio                = hqRatio,
            esGmaxRatio            = esGmaxRatio,
            intraSetFatigueRatio   = fatigueRatio
        )

        return Result(metrics, generateRecommendations(setLocalId, metrics))
    }

    private fun generateRecommendations(setLocalId: Long, m: SetMetrics): List<Recommendation> =
        buildList {
            // BSA alerts (worst muscle first)
            listOf(
                Triple(m.bsaVlPct, m.vlRisk, "cuádriceps (VL)"),
                Triple(m.bsaVmPct, m.vmRisk, "cuádriceps (VM)"),
                Triple(m.bsaGmaxPct, m.gmaxRisk, "glúteos"),
                Triple(m.bsaEsPct, m.esRisk, "erector espinal")
            ).filter { (_, risk, _) -> risk != RiskLevel.NORMAL }
                .sortedByDescending { (bsa, _, _) -> bsa }
                .take(2)
                .forEach { (bsa, risk, name) ->
                    add(Recommendation(
                        setLocalId = setLocalId,
                        severity   = risk,
                        evidence   = "BSA $name = ${bsa.fmt()}%",
                        text       = if (risk == RiskLevel.RISK)
                            "Asimetría significativa en $name (${bsa.fmt()}%). Considerar ejercicios unilaterales."
                        else
                            "Leve asimetría en $name (${bsa.fmt()}%). Mantener bajo observación."
                    ))
                }

            // ES:GMax
            if (m.esGmaxRisk != RiskLevel.NORMAL) add(Recommendation(
                setLocalId = setLocalId,
                severity   = m.esGmaxRisk,
                evidence   = "ES:GMax = ${m.esGmaxRatio.fmt()}",
                text       = if (m.esGmaxRisk == RiskLevel.RISK)
                    "Lumbar sobrecompensando (ES:GMax ${m.esGmaxRatio.fmt()}). Fortalecer activación glútea o revisar profundidad."
                else
                    "ES:GMax en zona de monitoreo (${m.esGmaxRatio.fmt()}). Prestar atención a la activación glútea."
            ))

            // H:Q
            if (m.hqRisk != RiskLevel.NORMAL) add(Recommendation(
                setLocalId = setLocalId,
                severity   = m.hqRisk,
                evidence   = "H:Q = ${m.hqRatio.fmt()}",
                text       = "Isquiotibiales débiles respecto a cuádriceps (H:Q ${m.hqRatio.fmt()}). Indicador de balance — no es predictor directo de lesión."
            ))

            // Fatigue
            if (m.fatigueRisk != RiskLevel.NORMAL) add(Recommendation(
                setLocalId = setLocalId,
                severity   = RiskLevel.MONITOR,
                evidence   = "Fatiga ratio = ${m.intraSetFatigueRatio.fmt()}",
                text       = "Fatiga significativa en el set. Considerar reducir volumen o descansar más entre series."
            ))
        }.sortedByDescending { it.severity.ordinal }.take(5)

    private fun emptyResult(setLocalId: Long) = Result(
        SetMetrics(setLocalId, 0f, 0f, 0f, 0f, 0f, 0f, 1f),
        emptyList()
    )

    private fun Float.fmt() = "%.1f".format(this)
}
