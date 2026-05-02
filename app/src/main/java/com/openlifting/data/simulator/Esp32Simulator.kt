package com.openlifting.data.simulator

import com.openlifting.domain.model.Muscle
import com.openlifting.domain.model.MuscleActivation
import com.openlifting.domain.model.MuscleSide
import com.openlifting.domain.model.SquatDepth
import com.openlifting.domain.model.SquatVariant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.random.Random

/**
 * Simulates the EMG data batch that a real ESP32 would send after a set.
 * Uses parameterized Normal distributions derived from the literature
 * (Bogdanis 2019, Caterisano 2002, Yavuz 2015-2017).
 *
 * Returns %MVC values directly (simulates hardware normalization pipeline).
 * The app pipeline is identical whether data comes from here or a real ESP32.
 */
@Singleton
class Esp32Simulator @Inject constructor() {

    // Mean %MVC at ~80% 1RM (concentric phase) — Bogdanis 2019 / Yavuz 2017
    private val baseMeans = mapOf(
        Muscle.VASTUS_LATERALIS to 65f,
        Muscle.VASTUS_MEDIALIS  to 69f,
        Muscle.GLUTEUS_MAXIMUS  to 44f,
        Muscle.ERECTOR_SPINAE  to 49f,
        Muscle.BICEPS_FEMORIS  to 35f
    )

    private val baseSD = 5f   // realistic inter-rep variation
    private val noise  = 2f   // sensor noise

    fun simulateSet(
        loadKg: Float,
        targetReps: Int,
        variant: SquatVariant,
        depth: SquatDepth,
        bilateralImbalancePct: Float = Random.nextFloat() * 8f + 2f,  // 2-10%
        random: Random = Random.Default
    ): List<List<MuscleActivation>> {
        // Load factor: higher load → higher activation, but saturates above 80% 1RM
        val loadFactor = (loadKg / 100f).coerceIn(0.6f, 1.15f)

        // Depth factor: deeper squat → more glute and quad, less lumbar
        val depthFactor = when (depth) {
            SquatDepth.ABOVE_PARALLEL -> 0.85f
            SquatDepth.PARALLEL       -> 1.0f
            SquatDepth.BELOW_PARALLEL -> 1.1f
        }

        // Variant factor: low-bar shifts load to posterior chain
        val variantGluteBoost = if (variant == SquatVariant.LOW_BAR) 1.08f else 1.0f
        val variantEsBoost    = if (variant == SquatVariant.LOW_BAR) 1.12f else 1.0f

        return (1..targetReps).map { repNumber ->
            val fatigueFactor = 1f + (repNumber - 1) * 0.015f  // ~1.5% fatigue per rep

            Muscle.entries.flatMap { muscle ->
                val baseMean = baseMeans[muscle] ?: 40f
                val muscleBoost = when (muscle) {
                    Muscle.GLUTEUS_MAXIMUS -> variantGluteBoost * depthFactor
                    Muscle.ERECTOR_SPINAE  -> variantEsBoost
                    else                   -> depthFactor
                }

                val mean = baseMean * loadFactor * muscleBoost * fatigueFactor

                MuscleSide.entries.map { side ->
                    val imbalanceOffset = when (side) {
                        MuscleSide.LEFT  -> -bilateralImbalancePct / 2f
                        MuscleSide.RIGHT -> +bilateralImbalancePct / 2f
                    }
                    val raw = mean + imbalanceOffset + gaussian(random, baseSD) + gaussian(random, noise)
                    MuscleActivation(
                        repId      = 0L,  // assigned when saved to Room
                        muscle     = muscle,
                        side       = side,
                        percentMvc = raw.coerceIn(5f, 100f)
                    )
                }
            }
        }
    }

    /**
     * Simulates the captured peak %MVC of a maximum voluntary contraction for one
     * muscle/side. Used during the calibration onboarding.
     *
     * MVC tests in real life rarely hit 100% — neural inhibition, technique, etc. cap
     * actual peaks at 85-95%. We sample a peak in the 80-98% range with a small
     * inter-side bias so the calibrated values reflect the inherent left/right
     * asymmetry that the rest of the simulator will use later.
     */
    fun captureMvc(muscle: Muscle, side: MuscleSide, random: Random = Random.Default): Float {
        val mean = 89f                  // typical achievable max
        val sideBias = when (side) {    // small inherent asymmetry per athlete (±2%)
            MuscleSide.LEFT  -> -1f
            MuscleSide.RIGHT -> +1f
        }
        val muscleBias = when (muscle) {
            Muscle.GLUTEUS_MAXIMUS -> -3f   // glutes typically harder to fully recruit
            Muscle.ERECTOR_SPINAE  -> -2f
            Muscle.BICEPS_FEMORIS  -> -4f
            else                   ->  0f
        }
        val raw = mean + sideBias + muscleBias + gaussian(random, sd = 3.5f)
        return raw.coerceIn(72f, 99f)
    }

    private fun gaussian(random: Random, sd: Float): Float {
        // Box-Muller transform
        val u1 = max(1e-10, random.nextDouble()).toFloat()
        val u2 = random.nextDouble().toFloat()
        return sd * kotlin.math.sqrt(-2f * kotlin.math.ln(u1)) *
                kotlin.math.cos(2f * Math.PI.toFloat() * u2)
    }
}
