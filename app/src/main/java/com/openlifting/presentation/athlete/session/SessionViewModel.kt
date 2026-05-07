package com.openlifting.presentation.athlete.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openlifting.data.local.dao.SessionDao
import com.openlifting.data.local.dao.SetDao
import com.openlifting.data.local.dao.UserDao
import com.openlifting.data.local.entity.SetMetricsEntity
import com.openlifting.data.websocket.EmgDataSourceWithFallback
import com.openlifting.domain.datasource.EmgDataSource
import com.openlifting.domain.datasource.StartSetRequest
import com.openlifting.domain.model.EmgEvent
import com.openlifting.domain.model.Muscle
import com.openlifting.domain.model.MusclePair
import com.openlifting.domain.model.MuscleSide
import com.openlifting.domain.model.Recommendation
import com.openlifting.domain.model.RepPhase
import com.openlifting.domain.model.RiskLevel
import com.openlifting.domain.model.SetMetrics
import com.openlifting.domain.model.SquatDepth
import com.openlifting.domain.model.SquatVariant
import com.openlifting.domain.repository.SessionRepository
import com.openlifting.domain.usecase.metrics.ComputeSetMetrics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

object SessionRouteArgs {
    /** Optional. Used when an instructor is running a session for an athlete (esp. guest). */
    const val ATHLETE_USER_ID    = "athleteUserId"
    /** Optional. Set alongside [ATHLETE_USER_ID] when the instructor is the supervisor. */
    const val INSTRUCTOR_USER_ID = "instructorUserId"
}

data class SetRecapItem(
    val setNumber: Int,
    val loadKg: Float,
    val targetReps: Int,
    val bsaWorstPct: Float,
    val overallRisk: RiskLevel
)

/** A captured rep during the streaming flow — used in the live UI strip. */
data class RepCapture(
    val repNumber: Int,
    val totalDurationMs: Long
)

/** A point in the realtime chart history (averaged L/R per muscle). */
data class ChartPoint(
    val timestampMs: Long,
    val muscleAvgPct: Map<Muscle, Float>
)

sealed interface SessionUiState {
    data object MetadataEntry : SessionUiState

/**
     * Live measurement state. Updated as [EmgEvent]s arrive from the [EmgDataSource].
     * The UI consumes this to render header (rep counter + timer + phase chip), the live
     * bilateral bars per muscle, the realtime chart, and the captured-reps strip.
     */
    data class MeasuringInProgress(
        val setNumber: Int,
        val loadKg: Float,
        val targetReps: Int,
        val variant: SquatVariant,
        val depth: SquatDepth,
        val rpe: Float,
        val currentRep: Int,                              // 1..targetReps once first PhaseStarted; 0 before
        val phase: RepPhase?,                              // null until first PhaseStarted
        val totalElapsedMs: Long,
        val phaseElapsedMs: Long,
        val liveActivations: Map<Muscle, MusclePair>,
        val peaksThisRep: Map<Muscle, MusclePair>,
        val chartHistory: List<ChartPoint>,
        val capturedReps: List<RepCapture>,
        val fallbackUsed: Boolean = false,                // true if WS failed and simulator is being used
        val fallbackMessage: String = ""                  // message explaining why fallback was used
    ) : SessionUiState

    data class AnalysisReady(
        val setNumber: Int,
        val loadKg: Float,
        val targetReps: Int,
        val rpe: Float,
        val variant: SquatVariant,
        val depth: SquatDepth,
        val metrics: SetMetrics,
        val recommendations: List<Recommendation>,
        val activations: Map<Muscle, MusclePair>
    ) : SessionUiState
    data class SessionSummary(
        val sessionId: Long,
        val totalSets: Int,
        val totalVolumeKg: Float,
        val maxLoadKg: Float,
        val durationMinutes: Int?,
        val overallRisk: RiskLevel,
        val sets: List<SetRecapItem>,
        val topRecommendations: List<Recommendation>
    ) : SessionUiState
    data class Error(val message: String) : SessionUiState
}

@HiltViewModel
class SessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionRepository: SessionRepository,
    private val emgDataSource: EmgDataSource,
    private val computeMetrics: ComputeSetMetrics,
    private val userDao: UserDao,
    private val setDao: SetDao,
    private val sessionDao: SessionDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<SessionUiState>(SessionUiState.MetadataEntry)
    val uiState = _uiState.asStateFlow()

    private var sessionLocalId: Long = -1L
    private var currentSetNumber: Int = 1

    /**
     * Override athlete user id for instructor-driven sessions. When null, the session is
     * created for whoever is currently logged in (the athlete-side flow).
     */
    private val explicitAthleteUserId: Long? =
        savedStateHandle.get<Long>(SessionRouteArgs.ATHLETE_USER_ID)?.takeIf { it > 0L }

    /** Set when an instructor supervises this session, paired with [explicitAthleteUserId]. */
    private val explicitInstructorUserId: Long? =
        savedStateHandle.get<Long>(SessionRouteArgs.INSTRUCTOR_USER_ID)?.takeIf { it > 0L }

    fun currentSetNumber(): Int = currentSetNumber

    fun measureSet(
        loadKg: Float,
        targetReps: Int,
        variant: SquatVariant,
        depth: SquatDepth,
        rpe: Float
    ) {
        viewModelScope.launch {
            // Initialize the live state immediately so the UI can transition.
            _uiState.value = SessionUiState.MeasuringInProgress(
                setNumber       = currentSetNumber,
                loadKg          = loadKg,
                targetReps      = targetReps,
                variant         = variant,
                depth           = depth,
                rpe             = rpe,
                currentRep      = 0,
                phase           = null,
                totalElapsedMs  = 0L,
                phaseElapsedMs  = 0L,
                liveActivations = emptyMap(),
                peaksThisRep    = emptyMap(),
                chartHistory    = emptyList(),
                capturedReps    = emptyList(),
                fallbackUsed    = false,
                fallbackMessage = ""
            )

            // Background check for fallback - update UI after 2.5s if WS didn't connect
            viewModelScope.launch {
                kotlinx.coroutines.delay(2_500)
                val current = _uiState.value
                val fallback = emgDataSource as? EmgDataSourceWithFallback
                if (current is SessionUiState.MeasuringInProgress && fallback?.fallbackUsed() == true) {
                    _uiState.value = current.copy(
                        fallbackUsed = true,
                        fallbackMessage = fallback.fallbackMessage()
                    )
                }
            }

            // Ensure the session row exists before the first set.
            if (sessionLocalId == -1L) {
                val athleteUserId = explicitAthleteUserId
                    ?: userDao.getLoggedInUser()?.id
                    ?: run {
                        _uiState.value = SessionUiState.Error("Usuario no encontrado")
                        return@launch
                    }
                sessionLocalId = sessionRepository.createSession(
                    athleteUserId    = athleteUserId,
                    instructorUserId = explicitInstructorUserId
                )
            }

            // Stream events and update state per event. SetComplete carries the final
            // activations payload that ComputeSetMetrics expects.
            val request = StartSetRequest(
                setRequestId = "set-${UUID.randomUUID()}",
                loadKg       = loadKg,
                targetReps   = targetReps,
                variant      = variant,
                depth        = depth,
                rpe          = rpe,
                athleteId    = explicitAthleteUserId?.toString()
            )

            val startedAtMs = System.currentTimeMillis()
            var capturedActivations: List<List<com.openlifting.domain.model.MuscleActivation>> = emptyList()
            var streamFailed = false
            var failureMessage = ""

            emgDataSource.streamSet(request).collect { event ->
                val current = _uiState.value as? SessionUiState.MeasuringInProgress ?: return@collect

                when (event) {
                    is EmgEvent.SetStarted -> {
                        // Initial state already set above; nothing else to do.
                    }
                    is EmgEvent.PhaseStarted -> {
                        val isNewRep = event.rep != current.currentRep
                        _uiState.value = current.copy(
                            currentRep      = event.rep,
                            phase           = event.phase,
                            phaseElapsedMs  = 0L,
                            peaksThisRep    = if (isNewRep) emptyMap() else current.peaksThisRep
                        )
                    }
                    is EmgEvent.Snapshot -> {
                        val newPeaks = updatePeaks(current.peaksThisRep, event.muscles)
                        val newChart = appendChartPoint(
                            history       = current.chartHistory,
                            timestampMs   = System.currentTimeMillis() - startedAtMs,
                            muscles       = event.muscles
                        )
                        _uiState.value = current.copy(
                            phase           = event.phase,
                            phaseElapsedMs  = event.elapsedPhaseMs,
                            totalElapsedMs  = System.currentTimeMillis() - startedAtMs,
                            liveActivations = event.muscles,
                            peaksThisRep    = newPeaks,
                            chartHistory    = newChart
                        )
                    }
                    is EmgEvent.PhaseComplete -> {
                        // Lock peaks of the just-finished phase visually.
                        _uiState.value = current.copy(peaksThisRep = event.musclesPeak)
                    }
                    is EmgEvent.RepComplete -> {
                        _uiState.value = current.copy(
                            capturedReps = current.capturedReps + RepCapture(
                                repNumber       = event.rep,
                                totalDurationMs = event.totalDurationMs
                            ),
                            peaksThisRep = emptyMap()
                        )
                    }
                    is EmgEvent.SetComplete -> {
                        capturedActivations = event.activationsByRep
                    }
                    is EmgEvent.Error -> {
                        streamFailed   = true
                        failureMessage = event.message
                    }
                }
            }

            if (streamFailed || capturedActivations.isEmpty()) {
                _uiState.value = SessionUiState.Error(
                    if (streamFailed) failureMessage else "No se recibieron datos del sensor"
                )
                return@launch
            }

            // Existing analysis pipeline — unchanged from the bulk path.
            val result = computeMetrics(setLocalId = 0L, activationsByRep = capturedActivations)
            val setLocalId = sessionRepository.saveSetWithDetails(
                sessionLocalId   = sessionLocalId,
                setNumber        = currentSetNumber,
                loadKg           = loadKg,
                targetReps       = targetReps,
                variant          = variant,
                depth            = depth,
                rpe              = rpe,
                activationsByRep = capturedActivations,
                metrics          = result.metrics,
                recommendations  = result.recommendations
            )

            val summaryMap = buildMap {
                Muscle.entries.forEach { muscle ->
                    val leftAvg = capturedActivations.flatten()
                        .filter { it.muscle == muscle && it.side == MuscleSide.LEFT }
                        .map { it.percentMvc }.average().toFloat()
                    val rightAvg = capturedActivations.flatten()
                        .filter { it.muscle == muscle && it.side == MuscleSide.RIGHT }
                        .map { it.percentMvc }.average().toFloat()
                    put(muscle, MusclePair(leftAvg, rightAvg))
                }
            }

            _uiState.value = SessionUiState.AnalysisReady(
                setNumber       = currentSetNumber,
                loadKg          = loadKg,
                targetReps      = targetReps,
                rpe             = rpe,
                variant         = variant,
                depth           = depth,
                metrics         = result.metrics.copy(setLocalId = setLocalId),
                recommendations = result.recommendations,
                activations     = summaryMap
            )
            currentSetNumber++
        }
    }

    fun nextSet() { _uiState.value = SessionUiState.MetadataEntry }

    /**
     * Ends the active session (writes endedAt) and transitions the UI to the [SessionSummary]
     * recap state. The user can then view the summary and choose to exit.
     *
     * If no session was started (e.g. the user opens "Nueva sesión" and immediately taps
     * "Finalizar"), this is a no-op aside from invoking [onSkipped] so navigation still works.
     */
    fun finalizeSession(onSkipped: () -> Unit) {
        viewModelScope.launch {
            if (sessionLocalId == -1L) { onSkipped(); return@launch }
            sessionRepository.endSession(sessionLocalId)

            val sets = setDao.getSetsForSession(sessionLocalId)
            if (sets.isEmpty()) {
                resetState()
                onSkipped()
                return@launch
            }

            val recap = sets.map { setEntity ->
                val metrics = setDao.getMetricsForSet(setEntity.localId)
                val bsaWorst = metrics?.let {
                    maxOf(it.bsaVlPct, it.bsaVmPct, it.bsaGmaxPct, it.bsaEsPct)
                } ?: 0f
                val risk = metrics?.let { computeOverallRisk(it) } ?: RiskLevel.NORMAL
                SetRecapItem(
                    setNumber   = setEntity.setNumber,
                    loadKg      = setEntity.loadKg,
                    targetReps  = setEntity.targetReps,
                    bsaWorstPct = bsaWorst,
                    overallRisk = risk
                )
            }

            val sessionEntity = sessionDao.getById(sessionLocalId)
            val duration = sessionEntity?.let {
                val end = it.endedAt ?: System.currentTimeMillis()
                ((end - it.startedAt) / 60_000L).toInt().coerceAtLeast(0)
            }

            val topRecs = sets.flatMap { setEntity ->
                setDao.getRecommendationsForSet(setEntity.localId).map { e ->
                    Recommendation(
                        id         = e.id,
                        setLocalId = e.setLocalId,
                        text       = e.text,
                        severity   = runCatching { RiskLevel.valueOf(e.severity) }.getOrDefault(RiskLevel.NORMAL),
                        evidence   = e.evidence
                    )
                }
            }
                .sortedByDescending { it.severity.ordinal }
                .distinctBy { it.text }
                .take(3)

            _uiState.value = SessionUiState.SessionSummary(
                sessionId       = sessionLocalId,
                totalSets       = sets.size,
                totalVolumeKg   = sets.sumOf { (it.loadKg * it.targetReps).toDouble() }.toFloat(),
                maxLoadKg       = sets.maxOf { it.loadKg },
                durationMinutes = duration,
                overallRisk     = recap.maxByOrNull { it.overallRisk.ordinal }?.overallRisk ?: RiskLevel.NORMAL,
                sets            = recap,
                topRecommendations = topRecs
            )
        }
    }

    /**
     * Called when the user dismisses the summary screen — resets local state and invokes
     * the navigation callback to leave the session flow.
     */
    fun exitSummary(onDone: () -> Unit) {
        resetState()
        onDone()
    }

    private fun resetState() {
        sessionLocalId = -1L
        currentSetNumber = 1
        _uiState.value = SessionUiState.MetadataEntry
    }

    // ── Live-state helpers ──────────────────────────────────────────────────

    private fun updatePeaks(
        existing: Map<Muscle, MusclePair>,
        latest: Map<Muscle, MusclePair>
    ): Map<Muscle, MusclePair> {
        return Muscle.entries.associateWith { muscle ->
            val prev = existing[muscle] ?: MusclePair(0f, 0f)
            val next = latest[muscle] ?: prev
            MusclePair(
                left  = maxOf(prev.left, next.left),
                right = maxOf(prev.right, next.right)
            )
        }
    }

    private fun appendChartPoint(
        history: List<ChartPoint>,
        timestampMs: Long,
        muscles: Map<Muscle, MusclePair>
    ): List<ChartPoint> {
        val point = ChartPoint(
            timestampMs   = timestampMs,
            muscleAvgPct  = muscles.mapValues { (_, pair) -> pair.avg }
        )
        // Keep only the last ~5 seconds (snapshots come at 20Hz → ~100 points).
        val cutoff = timestampMs - CHART_WINDOW_MS
        return (history + point).filter { it.timestampMs >= cutoff }
    }

    // ── risk helpers (mirror domain thresholds) ─────────────────────────────

    private fun computeOverallRisk(m: SetMetricsEntity): RiskLevel {
        val all = listOf(
            bsaRisk(m.bsaVlPct), bsaRisk(m.bsaVmPct),
            bsaRisk(m.bsaGmaxPct), bsaRisk(m.bsaEsPct),
            esGmaxRisk(m.esGmaxRatio),
            hqRisk(m.hqRatio),
            if (m.intraSetFatigueRatio > 1.3f) RiskLevel.RISK else RiskLevel.NORMAL
        )
        return all.maxByOrNull { it.ordinal } ?: RiskLevel.NORMAL
    }

    private fun bsaRisk(v: Float): RiskLevel = when {
        v >= 15f -> RiskLevel.RISK; v >= 10f -> RiskLevel.MONITOR; else -> RiskLevel.NORMAL
    }
    private fun esGmaxRisk(v: Float): RiskLevel = when {
        v >= 2f -> RiskLevel.RISK; v >= 1.5f -> RiskLevel.MONITOR; else -> RiskLevel.NORMAL
    }
    private fun hqRisk(v: Float): RiskLevel = when {
        v < 0.45f -> RiskLevel.RISK; v < 0.60f -> RiskLevel.MONITOR; else -> RiskLevel.NORMAL
    }

    private companion object {
        const val CHART_WINDOW_MS = 5_000L
    }
}
