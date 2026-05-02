package com.openlifting.presentation.athlete.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openlifting.data.local.dao.SessionDao
import com.openlifting.data.local.dao.SetDao
import com.openlifting.data.local.dao.UserDao
import com.openlifting.data.local.entity.SetMetricsEntity
import com.openlifting.data.simulator.Esp32Simulator
import com.openlifting.domain.model.Muscle
import com.openlifting.domain.model.MuscleSide
import com.openlifting.domain.model.Recommendation
import com.openlifting.domain.model.RiskLevel
import com.openlifting.domain.model.SetMetrics
import com.openlifting.domain.model.SquatDepth
import com.openlifting.domain.model.SquatVariant
import com.openlifting.domain.repository.SessionRepository
import com.openlifting.domain.usecase.metrics.ComputeSetMetrics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MusclePair(val left: Float, val right: Float)

data class SetRecapItem(
    val setNumber: Int,
    val loadKg: Float,
    val targetReps: Int,
    val bsaWorstPct: Float,
    val overallRisk: RiskLevel
)

sealed interface SessionUiState {
    data object MetadataEntry : SessionUiState
    data object Measuring : SessionUiState
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
    private val sessionRepository: SessionRepository,
    private val simulator: Esp32Simulator,
    private val computeMetrics: ComputeSetMetrics,
    private val userDao: UserDao,
    private val setDao: SetDao,
    private val sessionDao: SessionDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<SessionUiState>(SessionUiState.MetadataEntry)
    val uiState = _uiState.asStateFlow()

    private var sessionLocalId: Long = -1L
    private var currentSetNumber: Int = 1

    fun currentSetNumber(): Int = currentSetNumber

    fun measureSet(
        loadKg: Float,
        targetReps: Int,
        variant: SquatVariant,
        depth: SquatDepth,
        rpe: Float
    ) {
        viewModelScope.launch {
            _uiState.value = SessionUiState.Measuring
            if (sessionLocalId == -1L) {
                val user = userDao.getLoggedInUser() ?: run {
                    _uiState.value = SessionUiState.Error("Usuario no encontrado")
                    return@launch
                }
                sessionLocalId = sessionRepository.createSession(user.id)
            }
            delay(2000L)  // simulate ESP32 transmission

            val activationsByRep = simulator.simulateSet(
                loadKg = loadKg,
                targetReps = targetReps,
                variant = variant,
                depth = depth
            )

            val result = computeMetrics(setLocalId = 0L, activationsByRep = activationsByRep)

            val setLocalId = sessionRepository.saveSetWithDetails(
                sessionLocalId = sessionLocalId,
                setNumber      = currentSetNumber,
                loadKg         = loadKg,
                targetReps     = targetReps,
                variant        = variant,
                depth          = depth,
                rpe            = rpe,
                activationsByRep = activationsByRep,
                metrics        = result.metrics,
                recommendations = result.recommendations
            )

            val summaryMap = buildMap {
                Muscle.entries.forEach { muscle ->
                    val leftAvg = activationsByRep.flatten()
                        .filter { it.muscle == muscle && it.side == MuscleSide.LEFT }
                        .map { it.percentMvc }.average().toFloat()
                    val rightAvg = activationsByRep.flatten()
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
}
