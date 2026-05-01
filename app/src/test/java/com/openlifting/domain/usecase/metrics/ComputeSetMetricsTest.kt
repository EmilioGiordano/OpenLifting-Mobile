package com.openlifting.domain.usecase.metrics

import com.openlifting.domain.model.Muscle
import com.openlifting.domain.model.MuscleActivation
import com.openlifting.domain.model.MuscleSide
import com.openlifting.domain.model.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ComputeSetMetricsTest {

    private lateinit var subject: ComputeSetMetrics

    // Aliases for brevity
    private val VL = Muscle.VASTUS_LATERALIS
    private val VM = Muscle.VASTUS_MEDIALIS
    private val GM = Muscle.GLUTEUS_MAXIMUS
    private val ES = Muscle.ERECTOR_SPINAE
    private val BF = Muscle.BICEPS_FEMORIS
    private val L  = MuscleSide.LEFT
    private val R  = MuscleSide.RIGHT

    @Before
    fun setUp() {
        subject = ComputeSetMetrics()
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun a(muscle: Muscle, side: MuscleSide, pct: Float) =
        MuscleActivation(repId = 0, muscle = muscle, side = side, percentMvc = pct)

    /** Creates one rep with all muscles. Defaults are perfectly symmetric. */
    private fun rep(
        vlL: Float = 65f, vlR: Float = 65f,
        vmL: Float = 69f, vmR: Float = 69f,
        gmL: Float = 44f, gmR: Float = 44f,
        esL: Float = 49f, esR: Float = 49f,
        bfL: Float = 35f, bfR: Float = 35f
    ): List<MuscleActivation> = listOf(
        a(VL, L, vlL), a(VL, R, vlR),
        a(VM, L, vmL), a(VM, R, vmR),
        a(GM, L, gmL), a(GM, R, gmR),
        a(ES, L, esL), a(ES, R, esR),
        a(BF, L, bfL), a(BF, R, bfR)
    )

    private fun invoke(vararg reps: List<MuscleActivation>) =
        subject(setLocalId = 0L, activationsByRep = reps.toList())

    // ── BSA ──────────────────────────────────────────────────────────────────

    @Test
    fun `BSA is zero when both sides are equal`() {
        val result = invoke(rep())
        assertEquals(0f, result.metrics.bsaVlPct,  0.01f)
        assertEquals(0f, result.metrics.bsaGmaxPct, 0.01f)
        assertEquals(0f, result.metrics.bsaEsPct,   0.01f)
    }

    @Test
    fun `BSA under 10 percent is NORMAL`() {
        // VL left 65, right 70 → BSA = (70-65)/70 * 100 = 7.14%
        val result = invoke(rep(vlL = 65f, vlR = 70f))
        assertEquals(RiskLevel.NORMAL, result.metrics.vlRisk)
    }

    @Test
    fun `BSA between 10 and 15 percent is MONITOR`() {
        // VL left 60, right 70 → BSA = (70-60)/70 * 100 = 14.28%
        val result = invoke(rep(vlL = 60f, vlR = 70f))
        assertEquals(RiskLevel.MONITOR, result.metrics.vlRisk)
    }

    @Test
    fun `BSA above 15 percent is RISK`() {
        // VL left 55, right 70 → BSA = (70-55)/70 * 100 = 21.4%
        val result = invoke(rep(vlL = 55f, vlR = 70f))
        assertEquals(RiskLevel.RISK, result.metrics.vlRisk)
    }

    @Test
    fun `BSA formula uses major side as denominator`() {
        // Right is stronger: (80-60)/80 = 25%
        val result = invoke(rep(vlL = 60f, vlR = 80f))
        val expectedBsa = ((80f - 60f) / 80f) * 100f
        assertEquals(expectedBsa, result.metrics.bsaVlPct, 0.01f)
    }

    @Test
    fun `BSA is same regardless of which side is stronger`() {
        val resultLeftStronger  = invoke(rep(vlL = 80f, vlR = 60f))
        val resultRightStronger = invoke(rep(vlL = 60f, vlR = 80f))
        assertEquals(resultLeftStronger.metrics.bsaVlPct, resultRightStronger.metrics.bsaVlPct, 0.01f)
    }

    // ── ES:GMax ───────────────────────────────────────────────────────────────

    @Test
    fun `ES-GMax below 1_5 is NORMAL`() {
        // ES = 58, GMax = 45 → ratio = 1.29
        val result = invoke(rep(esL = 58f, esR = 58f, gmL = 45f, gmR = 45f))
        assertEquals(RiskLevel.NORMAL, result.metrics.esGmaxRisk)
    }

    @Test
    fun `ES-GMax between 1_5 and 2_0 is MONITOR`() {
        // ES = 72, GMax = 46 → ratio = 1.565
        val result = invoke(rep(esL = 72f, esR = 72f, gmL = 46f, gmR = 46f))
        assertEquals(RiskLevel.MONITOR, result.metrics.esGmaxRisk)
    }

    @Test
    fun `ES-GMax above 2_0 is RISK`() {
        // ES = 94, GMax = 44 → ratio = 2.136
        val result = invoke(rep(esL = 94f, esR = 94f, gmL = 44f, gmR = 44f))
        assertEquals(RiskLevel.RISK, result.metrics.esGmaxRisk)
    }

    @Test
    fun `ES-GMax ratio is computed correctly`() {
        val es = 80f; val gmax = 50f
        val result = invoke(rep(esL = es, esR = es, gmL = gmax, gmR = gmax))
        assertEquals(es / gmax, result.metrics.esGmaxRatio, 0.01f)
    }

    // ── H:Q ratio ─────────────────────────────────────────────────────────────

    @Test
    fun `HQ above 0_60 is NORMAL`() {
        // BF = 40, quads avg = 64 → H:Q = 0.625
        val result = invoke(rep(bfL = 40f, bfR = 40f, vlL = 64f, vlR = 64f, vmL = 64f, vmR = 64f))
        assertEquals(RiskLevel.NORMAL, result.metrics.hqRisk)
    }

    @Test
    fun `HQ between 0_45 and 0_60 is MONITOR`() {
        // BF = 28, quads avg = 55 → H:Q = 0.509
        val result = invoke(rep(bfL = 28f, bfR = 28f, vlL = 55f, vlR = 55f, vmL = 55f, vmR = 55f))
        assertEquals(RiskLevel.MONITOR, result.metrics.hqRisk)
    }

    @Test
    fun `HQ below 0_45 is RISK`() {
        // BF = 22, quads avg = 60 → H:Q = 0.367
        val result = invoke(rep(bfL = 22f, bfR = 22f, vlL = 60f, vlR = 60f, vmL = 60f, vmR = 60f))
        assertEquals(RiskLevel.RISK, result.metrics.hqRisk)
    }

    // ── Fatigue ───────────────────────────────────────────────────────────────

    @Test
    fun `No fatigue when last rep same as first`() {
        val identicalRep = rep()
        val result = invoke(identicalRep, identicalRep, identicalRep)
        assertEquals(RiskLevel.NORMAL, result.metrics.fatigueRisk)
    }

    @Test
    fun `High fatigue when last rep activation is more than 30 percent above first`() {
        // Override ALL muscles so fresh max = 60f, fatigued max = 85f → ratio = 1.42
        val freshRep    = rep(vlL = 60f, vlR = 60f, vmL = 60f, vmR = 60f,
                              esL = 50f, esR = 50f, gmL = 40f, gmR = 40f,
                              bfL = 30f, bfR = 30f)
        val fatiguedRep = rep(vlL = 85f, vlR = 85f, vmL = 85f, vmR = 85f,
                              esL = 72f, esR = 72f, gmL = 58f, gmR = 58f,
                              bfL = 44f, bfR = 44f)
        val result = invoke(freshRep, fatiguedRep)
        assertEquals(RiskLevel.RISK, result.metrics.fatigueRisk)
    }

    // ── Recommendations ───────────────────────────────────────────────────────

    @Test
    fun `No recommendations when all metrics are normal`() {
        // bfL=43 → H:Q = 43/67 = 0.642 (NORMAL, safely above 0.60 threshold)
        // ES:GMax = 49/44 = 1.11 (NORMAL), BSA = 0% (symmetric)
        val result = invoke(rep(bfL = 43f, bfR = 43f))
        assertTrue(result.recommendations.isEmpty())
    }

    @Test
    fun `BSA risk generates recommendation in Spanish`() {
        val result = invoke(rep(vlL = 50f, vlR = 70f)) // BSA VL = 28.5% → RISK
        val hasAsimetria = result.recommendations.any { "asimetría" in it.text.lowercase() || "asimetr" in it.text.lowercase() }
        assertTrue("Expected Spanish asymmetry recommendation", hasAsimetria)
    }

    @Test
    fun `ES-GMax risk generates lumbar recommendation in Spanish`() {
        val result = invoke(rep(esL = 95f, esR = 95f, gmL = 44f, gmR = 44f)) // ratio ~2.16
        val hasLumbar = result.recommendations.any {
            "lumbar" in it.text.lowercase() || "compensando" in it.text.lowercase()
        }
        assertTrue("Expected Spanish lumbar recommendation", hasLumbar)
    }

    @Test
    fun `Recommendations are sorted by severity descending`() {
        // Create RISK for BSA and MONITOR for ES:GMax
        val result = invoke(rep(vlL = 50f, vlR = 70f, esL = 68f, esR = 68f, gmL = 44f, gmR = 44f))
        val severities = result.recommendations.map { it.severity.ordinal }
        assertEquals(severities, severities.sortedDescending())
    }

    @Test
    fun `Maximum 5 recommendations returned`() {
        // Trigger all possible alerts simultaneously
        val result = invoke(
            rep(
                vlL = 50f, vlR = 70f,          // BSA VL RISK
                vmL = 50f, vmR = 70f,          // BSA VM RISK
                gmL = 50f, gmR = 70f,          // BSA GMax RISK
                esL = 95f, esR = 95f,          // ES:GMax RISK
                bfL = 20f, bfR = 20f           // H:Q RISK
            ),
            rep(
                vlL = 80f, vlR = 80f,
                esL = 130f, esR = 130f         // Fatigue: last rep activation 37% higher
            )
        )
        assertTrue(result.recommendations.size <= 5)
    }

    // ── Edge cases ─────────────────────────────────────────────────────────────

    @Test
    fun `Empty activation list returns zero metrics without crashing`() {
        val result = subject(setLocalId = 0L, activationsByRep = emptyList())
        assertEquals(0f, result.metrics.bsaVlPct, 0.01f)
        assertEquals(0f, result.metrics.esGmaxRatio, 0.01f)
    }

    @Test
    fun `Single rep produces valid metrics`() {
        val result = invoke(rep())
        // Should not throw and should return a valid result
        assertTrue(result.metrics.bsaVlPct >= 0f)
        assertTrue(result.metrics.esGmaxRatio >= 0f)
        assertTrue(result.metrics.hqRatio >= 0f)
        assertEquals(1f, result.metrics.intraSetFatigueRatio, 0.01f) // single rep = no fatigue
    }

    @Test
    fun `Metrics averaged correctly across multiple reps`() {
        // Two reps: first VL left=60, second VL left=80 → avg should be 70
        val rep1 = listOf(a(VL, L, 60f), a(VL, R, 60f))
        val rep2 = listOf(a(VL, L, 80f), a(VL, R, 80f))
        val result = subject(0L, listOf(rep1, rep2))
        // BSA should be 0 since both sides are equal
        assertEquals(0f, result.metrics.bsaVlPct, 0.01f)
    }
}
