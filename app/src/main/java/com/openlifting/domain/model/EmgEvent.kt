package com.openlifting.domain.model

/**
 * Events streamed by an [com.openlifting.domain.datasource.EmgDataSource] during a set.
 *
 * Mirrors the WebSocket wire protocol defined in
 * [`docs/plan-realtime-measurement.md`](../../../../../../../../../docs/plan-realtime-measurement.md)
 * §3, deserialised into typed Kotlin events. The same protocol is implemented by:
 *  - [com.openlifting.data.simulator.Esp32Simulator] (in-app fallback, no network)
 *  - WebSocketEmgClient (consumes the Python mock or a real ESP32 broadcasting WS)
 *
 * Event lifecycle for a single set:
 *  - [SetStarted] (1)
 *  - For each rep:
 *      - [PhaseStarted] eccentric → many [Snapshot]s → [PhaseComplete] eccentric
 *      - [PhaseStarted] isometric → many [Snapshot]s → [PhaseComplete] isometric
 *      - [PhaseStarted] concentric → many [Snapshot]s → [PhaseComplete] concentric
 *      - [RepComplete]
 *  - [SetComplete] (1, with the full activations payload that downstream metrics
 *    computation already consumes)
 *  - [Error] is terminal — the source closes the stream after emitting it
 */
sealed interface EmgEvent {

    val setId: String

    data class SetStarted(
        override val setId: String,
        val targetReps: Int,
        val loadKg: Float,
        val rpe: Float? = null
    ) : EmgEvent

    data class PhaseStarted(
        override val setId: String,
        val rep: Int,
        val phase: RepPhase
    ) : EmgEvent

    /** High-frequency live update during a phase (~20 Hz). */
    data class Snapshot(
        override val setId: String,
        val rep: Int,
        val phase: RepPhase,
        val elapsedPhaseMs: Long,
        val muscles: Map<Muscle, MusclePair>
    ) : EmgEvent

    data class PhaseComplete(
        override val setId: String,
        val rep: Int,
        val phase: RepPhase,
        val durationMs: Long,
        val musclesAvg: Map<Muscle, MusclePair>,
        val musclesPeak: Map<Muscle, MusclePair>
    ) : EmgEvent

    data class RepComplete(
        override val setId: String,
        val rep: Int,
        val totalDurationMs: Long,
        val eccentric: PhaseSummary,
        val isometric: PhaseSummary,
        val concentric: PhaseSummary
    ) : EmgEvent

    /**
     * Final event of a successful set. [activationsByRep] is the same shape that
     * [com.openlifting.domain.usecase.metrics.ComputeSetMetrics] already consumes —
     * the analysis pipeline downstream is unchanged.
     */
    data class SetComplete(
        override val setId: String,
        val totalReps: Int,
        val activationsByRep: List<List<MuscleActivation>>
    ) : EmgEvent

    /** Terminal failure event. The source closes the stream after this. */
    data class Error(
        override val setId: String,
        val code: String,
        val message: String
    ) : EmgEvent
}

data class PhaseSummary(
    val durationMs: Long,
    val musclesAvg: Map<Muscle, MusclePair>,
    val musclesPeak: Map<Muscle, MusclePair>
)
