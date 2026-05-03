package com.openlifting.data.simulator

import com.openlifting.domain.datasource.EmgDataSource
import com.openlifting.domain.datasource.StartSetRequest
import com.openlifting.domain.model.EmgEvent
import com.openlifting.domain.model.Muscle
import com.openlifting.domain.model.MuscleActivation
import com.openlifting.domain.model.MusclePair
import com.openlifting.domain.model.MuscleSide
import com.openlifting.domain.model.PhaseSummary
import com.openlifting.domain.model.RepPhase
import com.openlifting.domain.model.SquatDepth
import com.openlifting.domain.model.SquatVariant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * In-app implementation of [EmgDataSource] that emits the same event stream a real ESP32
 * (or its Python mock) would push over WebSocket. Used as the offline fallback when no
 * WebSocket source is reachable, and as the source of truth during development of the UI.
 *
 * Las medias por músculo en [baseMeans] están sacadas de literatura (Bogdanis 2019,
 * Yavuz 2017, Caterisano 2002) — son %MVC promedio durante la fase concéntrica de
 * sentadilla a ~80% 1RM. Las distribuciones (Normal con SD=5 inter-rep + ruido=2)
 * tienen valores realistas.
 *
 * Lo "engineered para el demo" (multiplicadores de carga / profundidad / variante,
 * factores por fase, asimetría bilateral random 2-10%) son aproximaciones direccionales
 * correctas pero sin cita peer-reviewed específica — están documentadas como tales en
 * `docs/decisiones-tecnicas.md` §5.2 y `docs/plan-realtime-measurement.md` §3.4.
 *
 * Returns %MVC values directly (simulates the hardware normalization pipeline).
 */
@Singleton
class Esp32Simulator @Inject constructor() : EmgDataSource {

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

    /**
     * Phase factors relative to the rep's average activation:
     *  - eccentric (descent): lower amplitude, the muscle works lengthening under load
     *  - isometric (bottom pause): brief, lower steady-state
     *  - concentric (ascent): full mean — the "working" phase
     */
    private val phaseFactor = mapOf(
        RepPhase.ECCENTRIC  to 0.78f,
        RepPhase.ISOMETRIC  to 0.65f,
        RepPhase.CONCENTRIC to 1.00f
    )

    /**
     * Base phase durations at RPE 7 (the "moderate working set" reference). Real durations
     * are scaled by [velocityFactorFromRpe] — heavier RPE means slower reps because the
     * concentric grinds and the eccentric is more controlled.
     */
    private val baseEccentricMs   = 2200L
    private val baseIsometricMs   = 450L
    private val baseConcentricMs  = 1500L
    private val baseRestMs        = 1100L

    private val tickIntervalMs = 50L                  // 20 Hz

    // ── EmgDataSource ──────────────────────────────────────────────────────

    override fun streamSet(request: StartSetRequest): Flow<EmgEvent> = flow {
        val random = Random.Default
        val setId  = request.setRequestId

        // Velocity factor scales every duration based on RPE (1.0 = RPE 7 reference).
        // Heavy RPE → slower reps (concentric grinds); light RPE → faster reps.
        val velocity = velocityFactorFromRpe(request.rpe)
        val phaseDurations = mapOf(
            RepPhase.ECCENTRIC  to (baseEccentricMs  * velocity).toLong(),
            RepPhase.ISOMETRIC  to (baseIsometricMs  * velocity).toLong(),
            RepPhase.CONCENTRIC to (baseConcentricMs * velocity).toLong()
        )
        val restMs = (baseRestMs * velocity).toLong()

        // Generate the per-rep summary activations once (matches what ComputeSetMetrics
        // expects) and use those values as the targets that streaming snapshots converge to.
        val activationsByRep = simulateSet(
            loadKg     = request.loadKg,
            targetReps = request.targetReps,
            variant    = request.variant,
            depth      = request.depth,
            random     = random
        )

        emit(EmgEvent.SetStarted(
            setId      = setId,
            targetReps = request.targetReps,
            loadKg     = request.loadKg,
            rpe        = request.rpe
        ))

        for (repIdx in activationsByRep.indices) {
            val repNum = repIdx + 1
            // Per-side per-muscle target value for this rep
            val targets: Map<Pair<Muscle, MuscleSide>, Float> =
                activationsByRep[repIdx].associate { (it.muscle to it.side) to it.percentMvc }

            // Run the three phases of this rep
            val phaseSummaries = mutableMapOf<RepPhase, PhaseSummary>()
            for (phase in listOf(RepPhase.ECCENTRIC, RepPhase.ISOMETRIC, RepPhase.CONCENTRIC)) {
                phaseSummaries[phase] = streamPhase(
                    setId      = setId,
                    repNum     = repNum,
                    phase      = phase,
                    durationMs = phaseDurations.getValue(phase),
                    targets    = targets,
                    random     = random
                )
            }

            emit(EmgEvent.RepComplete(
                setId            = setId,
                rep              = repNum,
                totalDurationMs  = phaseSummaries.values.sumOf { it.durationMs },
                eccentric        = phaseSummaries.getValue(RepPhase.ECCENTRIC),
                isometric        = phaseSummaries.getValue(RepPhase.ISOMETRIC),
                concentric       = phaseSummaries.getValue(RepPhase.CONCENTRIC)
            ))

            // Rest between reps (skip after the last one) — scales with RPE too
            if (repNum < activationsByRep.size) delay(restMs)
        }

        emit(EmgEvent.SetComplete(
            setId             = setId,
            totalReps         = request.targetReps,
            activationsByRep  = activationsByRep
        ))
    }

    /**
     * RPE 7 is the reference (factor 1.0). Each ±1 RPE shifts ±0.15. Clamped to a
     * realistic envelope so out-of-range inputs (or null) don't break the simulation.
     *
     * - RPE 5 → 0.70 (warm-up, fast reps, ~3.7s/rep cycle)
     * - RPE 7 → 1.00 (moderate work, ~5.2s/rep)
     * - RPE 9 → 1.30 (heavy, near-max, ~6.7s/rep)
     * - RPE 10 → 1.45 (max effort, grinding, ~7.5s/rep)
     */
    private fun velocityFactorFromRpe(rpe: Float?): Float {
        if (rpe == null) return 1.0f
        return (1.0f + (rpe - 7f) * 0.15f).coerceIn(0.70f, 1.45f)
    }

    /**
     * Streams one phase: emits PhaseStarted, then ~20 Hz Snapshot ticks for the phase
     * duration, then PhaseComplete. Returns the phase summary so the caller can bundle it
     * into the RepComplete event.
     */
    private suspend fun kotlinx.coroutines.flow.FlowCollector<EmgEvent>.streamPhase(
        setId: String,
        repNum: Int,
        phase: RepPhase,
        durationMs: Long,
        targets: Map<Pair<Muscle, MuscleSide>, Float>,
        random: Random
    ): PhaseSummary {
        emit(EmgEvent.PhaseStarted(setId = setId, rep = repNum, phase = phase))

        val factor     = phaseFactor.getValue(phase)
        val totalTicks = (durationMs / tickIntervalMs).toInt()

        // Track running peaks and accumulators so we can emit avg + peak in PhaseComplete.
        val peaks       = Muscle.entries.associateWith { MusclePair(0f, 0f) }.toMutableMap()
        val accumulator = Muscle.entries.associateWith {
            mutableListOf<Pair<Float, Float>>()
        }.toMutableMap()

        for (tick in 0 until totalTicks) {
            val t        = tick.toFloat() / totalTicks   // 0..1 progress through the phase
            val envelope = phaseEnvelope(phase, t)        // 0..~1, shapes how amplitude rises/falls

            val muscles: Map<Muscle, MusclePair> = Muscle.entries.associateWith { muscle ->
                val tgtL = targets[muscle to MuscleSide.LEFT]  ?: 0f
                val tgtR = targets[muscle to MuscleSide.RIGHT] ?: 0f
                val l = (tgtL * factor * envelope + gaussian(random, sd = 1.2f)).coerceIn(0f, 100f)
                val r = (tgtR * factor * envelope + gaussian(random, sd = 1.2f)).coerceIn(0f, 100f)

                peaks[muscle] = MusclePair(
                    left  = max(peaks.getValue(muscle).left,  l),
                    right = max(peaks.getValue(muscle).right, r)
                )
                accumulator.getValue(muscle).add(l to r)

                MusclePair(l, r)
            }

            emit(EmgEvent.Snapshot(
                setId            = setId,
                rep              = repNum,
                phase            = phase,
                elapsedPhaseMs   = tick * tickIntervalMs,
                muscles          = muscles
            ))
            delay(tickIntervalMs)
        }

        val musclesAvg: Map<Muscle, MusclePair> = accumulator.mapValues { (_, vals) ->
            MusclePair(
                left  = vals.map { it.first  }.average().toFloat(),
                right = vals.map { it.second }.average().toFloat()
            )
        }

        emit(EmgEvent.PhaseComplete(
            setId        = setId,
            rep          = repNum,
            phase        = phase,
            durationMs   = durationMs,
            musclesAvg   = musclesAvg,
            musclesPeak  = peaks.toMap()
        ))

        return PhaseSummary(durationMs = durationMs, musclesAvg = musclesAvg, musclesPeak = peaks.toMap())
    }

    /**
     * Per-phase activation envelope, returns 0..~1 over the phase progress 0..1.
     *
     *  - ECCENTRIC: smooth bell, peaks mid-phase. The descent is gradual, the muscle
     *    activates progressively as the bar lowers.
     *  - ISOMETRIC: flat near-peak with a small ripple (noise of holding tension).
     *  - CONCENTRIC: rises sharply early, holds high, drops at end. The "explosive"
     *    push that powerlifters know.
     */
    private fun phaseEnvelope(phase: RepPhase, t: Float): Float = when (phase) {
        RepPhase.ECCENTRIC  -> 0.45f + 0.55f * sin(PI.toFloat() * t)
        RepPhase.ISOMETRIC  -> 0.92f + 0.04f * sin(PI.toFloat() * 4f * t)
        RepPhase.CONCENTRIC -> {
            // Rises fast, holds, drops at end: piecewise smooth-ish curve
            when {
                t < 0.30f -> 0.55f + (t / 0.30f) * 0.45f         // climb 0.55 -> 1.00
                t < 0.75f -> 1.00f                                // hold near peak
                else      -> 1.00f - ((t - 0.75f) / 0.25f) * 0.35f // taper 1.00 -> 0.65
            }
        }
    }

    // ── Bulk simulator (still used by tests; will be removed once VM refactors away) ───

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
                kotlin.math.cos(2f * PI.toFloat() * u2)
    }
}
