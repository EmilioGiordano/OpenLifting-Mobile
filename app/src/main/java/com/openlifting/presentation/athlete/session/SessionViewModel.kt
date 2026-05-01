package com.openlifting.presentation.athlete.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openlifting.data.local.dao.UserDao
import com.openlifting.data.simulator.Esp32Simulator
import com.openlifting.domain.model.Muscle
import com.openlifting.domain.model.MuscleSide
import com.openlifting.domain.model.Recommendation
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

sealed interface SessionUiState {
    data object MetadataEntry : SessionUiState
    data object Measuring : SessionUiState
    data class AnalysisReady(
        val setNumber: Int,
        val loadKg: Float,
        val targetReps: Int,
        val metrics: SetMetrics,
        val recommendations: List<Recommendation>,
        val activations: Map<Muscle, MusclePair>
    ) : SessionUiState
    data class Error(val message: String) : SessionUiState
}

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val simulator: Esp32Simulator,
    private val computeMetrics: ComputeSetMetrics,
    private val userDao: UserDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<SessionUiState>(SessionUiState.MetadataEntry)
    val uiState = _uiState.asStateFlow()

    private var sessionLocalId: Long = -1L
    private var currentSetNumber: Int = 1

    // Session is created lazily on first measurement — not on screen entry
    fun measureSet(
        loadKg: Float,
        targetReps: Int,
        variant: SquatVariant,
        depth: SquatDepth,
        rpe: Float
    ) {
        viewModelScope.launch {
            _uiState.value = SessionUiState.Measuring
            // Create session on first set if not yet created
            if (sessionLocalId == -1L) {
                val user = userDao.getLoggedInUser() ?: run {
                    _uiState.value = SessionUiState.Error("Usuario no encontrado")
                    return@launch
                }
                sessionLocalId = sessionRepository.createSession(user.id)
            }
            delay(2000L)  // simulates ESP32 transmission delay

            val activationsByRep = simulator.simulateSet(
                loadKg = loadKg,
                targetReps = targetReps,
                variant = variant,
                depth = depth
            )

            val result = computeMetrics(
                setLocalId = 0L,  // temp; real id assigned after Room insert
                activationsByRep = activationsByRep
            )

            val setLocalId = sessionRepository.saveSetWithDetails(
                sessionLocalId = sessionLocalId,
                setNumber = currentSetNumber,
                loadKg = loadKg,
                targetReps = targetReps,
                variant = variant,
                depth = depth,
                rpe = rpe,
                activationsByRep = activationsByRep,
                metrics = result.metrics,
                recommendations = result.recommendations
            )

            val summaryMap = buildMap {
                Muscle.entries.forEach { muscle ->
                    val leftAvg = activationsByRep
                        .flatten()
                        .filter { it.muscle == muscle && it.side == MuscleSide.LEFT }
                        .map { it.percentMvc }
                        .average().toFloat()
                    val rightAvg = activationsByRep
                        .flatten()
                        .filter { it.muscle == muscle && it.side == MuscleSide.RIGHT }
                        .map { it.percentMvc }
                        .average().toFloat()
                    put(muscle, MusclePair(leftAvg, rightAvg))
                }
            }

            _uiState.value = SessionUiState.AnalysisReady(
                setNumber = currentSetNumber,
                loadKg = loadKg,
                targetReps = targetReps,
                metrics = result.metrics.copy(setLocalId = setLocalId),
                recommendations = result.recommendations,
                activations = summaryMap
            )
            currentSetNumber++
        }
    }

    fun currentSetNumber(): Int = currentSetNumber
    fun nextSet() { _uiState.value = SessionUiState.MetadataEntry }

    fun endSession(onDone: () -> Unit) {
        viewModelScope.launch {
            sessionRepository.endSession(sessionLocalId)
            sessionLocalId = -1L
            currentSetNumber = 1
            onDone()
        }
    }
}
