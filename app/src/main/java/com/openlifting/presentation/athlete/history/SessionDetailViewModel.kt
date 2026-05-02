package com.openlifting.presentation.athlete.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openlifting.data.local.dao.SessionDao
import com.openlifting.data.local.dao.SetDao
import com.openlifting.data.mapper.toDomain
import com.openlifting.domain.model.Muscle
import com.openlifting.domain.model.MuscleSide
import com.openlifting.domain.model.Recommendation
import com.openlifting.domain.model.RiskLevel
import com.openlifting.domain.model.SetMetrics
import com.openlifting.domain.model.SquatDepth
import com.openlifting.domain.model.SquatVariant
import com.openlifting.presentation.athlete.session.MusclePair
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetExpandedItem(
    val setNumber: Int,
    val loadKg: Float,
    val targetReps: Int,
    val rpe: Float,
    val variant: SquatVariant,
    val depth: SquatDepth,
    val metrics: SetMetrics,
    val activations: Map<Muscle, MusclePair>,
    val recommendations: List<Recommendation>,
    val overallRisk: RiskLevel
)

data class SessionDetailUiData(
    val sessionId: Long,
    val startedAt: Long,
    val totalSets: Int,
    val totalVolumeKg: Float,
    val maxLoadKg: Float,
    val durationMinutes: Int?,
    val overallRisk: RiskLevel,
    val sets: List<SetExpandedItem>
)

sealed interface SessionDetailUiState {
    data object Loading : SessionDetailUiState
    data object NotFound : SessionDetailUiState
    data class Loaded(val data: SessionDetailUiData) : SessionDetailUiState
}

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    private val setDao: SetDao,
    private val sessionDao: SessionDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<SessionDetailUiState>(SessionDetailUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private var loadedId = -1L

    fun load(sessionId: Long) {
        if (loadedId == sessionId) return
        loadedId = sessionId
        viewModelScope.launch {
            val session = sessionDao.getById(sessionId)
            if (session == null) {
                _uiState.value = SessionDetailUiState.NotFound
                return@launch
            }

            val setEntities = setDao.getSetsForSession(sessionId)
            val sets = setEntities.mapNotNull { setEntity ->
                val metricsDomain = setDao.getMetricsForSet(setEntity.localId)?.toDomain()
                    ?: return@mapNotNull null
                val recs = setDao.getRecommendationsForSet(setEntity.localId).map { it.toDomain() }
                val activations = aggregateActivations(setEntity.localId)
                SetExpandedItem(
                    setNumber       = setEntity.setNumber,
                    loadKg          = setEntity.loadKg,
                    targetReps      = setEntity.targetReps,
                    rpe             = setEntity.rpe,
                    variant         = runCatching { SquatVariant.valueOf(setEntity.variant) }.getOrDefault(SquatVariant.LOW_BAR),
                    depth           = runCatching { SquatDepth.valueOf(setEntity.depth) }.getOrDefault(SquatDepth.PARALLEL),
                    metrics         = metricsDomain,
                    activations     = activations,
                    recommendations = recs,
                    overallRisk     = metricsDomain.overallRisk
                )
            }

            val durationMin = session.endedAt?.let {
                ((it - session.startedAt) / 60_000L).toInt().coerceAtLeast(0)
            }

            val data = SessionDetailUiData(
                sessionId       = sessionId,
                startedAt       = session.startedAt,
                totalSets       = sets.size,
                totalVolumeKg   = setEntities.sumOf { (it.loadKg * it.targetReps).toDouble() }.toFloat(),
                maxLoadKg       = setEntities.maxOfOrNull { it.loadKg } ?: 0f,
                durationMinutes = durationMin,
                overallRisk     = sets.maxByOrNull { it.overallRisk.ordinal }?.overallRisk ?: RiskLevel.NORMAL,
                sets            = sets
            )
            _uiState.value = SessionDetailUiState.Loaded(data)
        }
    }

    private suspend fun aggregateActivations(setId: Long): Map<Muscle, MusclePair> {
        val reps = setDao.getRepsForSet(setId)
        if (reps.isEmpty()) return emptyMap()

        val allActivations = reps.flatMap { setDao.getActivationsForRep(it.id) }
        return Muscle.entries.associateWith { muscle ->
            val left = allActivations
                .filter { it.muscle == muscle.name && it.side == MuscleSide.LEFT.name }
                .map { it.percentMvc }
                .ifEmpty { listOf(0f) }
                .average()
                .toFloat()
            val right = allActivations
                .filter { it.muscle == muscle.name && it.side == MuscleSide.RIGHT.name }
                .map { it.percentMvc }
                .ifEmpty { listOf(0f) }
                .average()
                .toFloat()
            MusclePair(left, right)
        }
    }
}
