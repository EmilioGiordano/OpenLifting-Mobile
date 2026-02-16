package com.openlifting.ui.screens.session

import androidx.lifecycle.ViewModel
import com.openlifting.data.model.*
import com.openlifting.data.repository.MockRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MuscleActivation(
    val muscle: MuscleGroup,
    val leftPercent: Float,
    val rightPercent: Float,
    val difference: Float,
    val hasImbalance: Boolean
)

data class SessionDetailUiState(
    val session: Session? = null,
    val selectedSeriesIndex: Int = 0,
    val recommendations: List<String> = emptyList()
)

class SessionDetailViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SessionDetailUiState())
    val uiState: StateFlow<SessionDetailUiState> = _uiState.asStateFlow()

    fun loadSession(sessionId: String) {
        val session = MockRepository.getSessionById(sessionId) ?: return
        _uiState.value = SessionDetailUiState(
            session = session,
            selectedSeriesIndex = 0,
            recommendations = MockRepository.generateRecommendations(session)
        )
    }

    fun selectSeries(index: Int) {
        _uiState.value = _uiState.value.copy(selectedSeriesIndex = index)
    }

    companion object {
        private const val IMBALANCE_THRESHOLD = 8f

        fun getActivationsForRep(repetition: Repetition): List<MuscleActivation> {
            val reading = repetition.emgReading
            return listOf(
                MuscleActivation(
                    muscle = MuscleGroup.QUADRICEPS,
                    leftPercent = reading.quadricepsLeft,
                    rightPercent = reading.quadricepsRight,
                    difference = kotlin.math.abs(reading.quadricepsLeft - reading.quadricepsRight),
                    hasImbalance = kotlin.math.abs(reading.quadricepsLeft - reading.quadricepsRight) > IMBALANCE_THRESHOLD
                ),
                MuscleActivation(
                    muscle = MuscleGroup.GLUTES,
                    leftPercent = reading.glutesLeft,
                    rightPercent = reading.glutesRight,
                    difference = kotlin.math.abs(reading.glutesLeft - reading.glutesRight),
                    hasImbalance = kotlin.math.abs(reading.glutesLeft - reading.glutesRight) > IMBALANCE_THRESHOLD
                ),
                MuscleActivation(
                    muscle = MuscleGroup.HAMSTRINGS,
                    leftPercent = reading.hamstringsLeft,
                    rightPercent = reading.hamstringsRight,
                    difference = kotlin.math.abs(reading.hamstringsLeft - reading.hamstringsRight),
                    hasImbalance = kotlin.math.abs(reading.hamstringsLeft - reading.hamstringsRight) > IMBALANCE_THRESHOLD
                ),
                MuscleActivation(
                    muscle = MuscleGroup.LOWER_BACK,
                    leftPercent = reading.lowerBackLeft,
                    rightPercent = reading.lowerBackRight,
                    difference = kotlin.math.abs(reading.lowerBackLeft - reading.lowerBackRight),
                    hasImbalance = kotlin.math.abs(reading.lowerBackLeft - reading.lowerBackRight) > IMBALANCE_THRESHOLD
                )
            )
        }
    }
}
