package com.openlifting.data.mapper

import com.openlifting.domain.model.Muscle
import com.openlifting.domain.model.MuscleActivation
import com.openlifting.domain.model.MuscleSide
import com.openlifting.domain.model.Recommendation
import com.openlifting.domain.model.RiskLevel
import com.openlifting.domain.model.SetMetrics
import com.openlifting.domain.model.SquatDepth
import com.openlifting.domain.model.SquatVariant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SetIngestionMapperTest {

    private fun activation(muscle: Muscle, side: MuscleSide, percent: Float, peak: Float = percent + 10f) =
        MuscleActivation(repId = 0L, muscle = muscle, side = side,
            percentMvc = percent, peakPercentMvc = peak)

    private fun metrics() = SetMetrics(
        setLocalId = 0L,
        bsaVlPct = 32.5f, bsaVmPct = 28.5f, bsaGmaxPct = 25.0f, bsaEsPct = 14.0f,
        hqRatio = 0.45f, esGmaxRatio = 0.62f, intraSetFatigueRatio = 0.18f,
        thresholdsVersion = 1
    )

    @Test
    fun `top-level fields are passed through with the right types and casing`() {
        val req = buildPostSetRequest(
            setNumber = 3,
            loadKg = 140.5f,
            targetReps = 5,
            variant = SquatVariant.LOW_BAR,
            depth = SquatDepth.PARALLEL,
            rpe = 8.5f,
            activationsByRep = listOf(emptyList()),
            metrics = metrics(),
            recommendations = emptyList()
        )

        assertEquals(3, req.setNumber)
        assertEquals(140.5, req.loadKg, 0.001)
        assertEquals(5, req.targetReps)
        assertEquals("LOW_BAR", req.variant)
        assertEquals("PARALLEL", req.depth)
        assertEquals(8.5, req.rpe, 0.001)
    }

    @Test
    fun `reps are numbered 1-based and preserve activation order`() {
        val req = buildPostSetRequest(
            setNumber = 1, loadKg = 100f, targetReps = 3,
            variant = SquatVariant.HIGH_BAR, depth = SquatDepth.BELOW_PARALLEL, rpe = 7f,
            activationsByRep = listOf(
                listOf(activation(Muscle.VASTUS_LATERALIS, MuscleSide.LEFT, 80f)),
                listOf(activation(Muscle.GLUTEUS_MAXIMUS,  MuscleSide.RIGHT, 70f))
            ),
            metrics = metrics(), recommendations = emptyList()
        )

        assertEquals(2, req.reps.size)
        assertEquals(1, req.reps[0].repNumber)
        assertEquals(2, req.reps[1].repNumber)
        assertEquals("VASTUS_LATERALIS", req.reps[0].activations[0].muscle)
        assertEquals("LEFT", req.reps[0].activations[0].side)
        assertEquals(80.0, req.reps[0].activations[0].percentMvc, 0.001)
        assertEquals(90.0, req.reps[0].activations[0].peakPercentMvc, 0.001)
    }

    @Test
    fun `reps with no activations are still emitted`() {
        // electrode disconnected mid-rep — backend allows zero activations per rep
        val req = buildPostSetRequest(
            setNumber = 1, loadKg = 100f, targetReps = 1,
            variant = SquatVariant.LOW_BAR, depth = SquatDepth.PARALLEL, rpe = 6f,
            activationsByRep = listOf(emptyList()),
            metrics = metrics(), recommendations = emptyList()
        )

        assertEquals(1, req.reps.size)
        assertTrue(req.reps[0].activations.isEmpty())
    }

    @Test
    fun `metrics are forwarded with the same values`() {
        val req = buildPostSetRequest(
            setNumber = 1, loadKg = 100f, targetReps = 1,
            variant = SquatVariant.LOW_BAR, depth = SquatDepth.PARALLEL, rpe = 6f,
            activationsByRep = listOf(emptyList()),
            metrics = metrics(), recommendations = emptyList()
        )

        assertEquals(32.5, req.metrics.bsaVlPct, 0.001)
        assertEquals(28.5, req.metrics.bsaVmPct, 0.001)
        assertEquals(25.0, req.metrics.bsaGmaxPct, 0.001)
        assertEquals(14.0, req.metrics.bsaEsPct, 0.001)
        assertEquals(0.45, req.metrics.hqRatio, 0.001)
        assertEquals(0.62, req.metrics.esGmaxRatio, 0.001)
        assertEquals(0.18, req.metrics.intraSetFatigueRatio, 0.001)
        assertEquals(1, req.metrics.thresholdsVersion)
    }

    @Test
    fun `recommendations preserve severity name and evidence`() {
        val recs = listOf(
            Recommendation(setLocalId = 0L, text = "ok", severity = RiskLevel.NORMAL, evidence = ""),
            Recommendation(setLocalId = 0L, text = "lookout", severity = RiskLevel.MONITOR, evidence = "es=0.62"),
            Recommendation(setLocalId = 0L, text = "stop", severity = RiskLevel.RISK, evidence = "bsa=22%")
        )
        val req = buildPostSetRequest(
            setNumber = 1, loadKg = 100f, targetReps = 1,
            variant = SquatVariant.LOW_BAR, depth = SquatDepth.PARALLEL, rpe = 6f,
            activationsByRep = listOf(emptyList()),
            metrics = metrics(), recommendations = recs
        )

        assertEquals(3, req.recommendations.size)
        assertEquals("NORMAL", req.recommendations[0].severity)
        assertEquals("", req.recommendations[0].evidence)
        assertEquals("MONITOR", req.recommendations[1].severity)
        assertEquals("es=0.62", req.recommendations[1].evidence)
        assertEquals("RISK", req.recommendations[2].severity)
    }
}
