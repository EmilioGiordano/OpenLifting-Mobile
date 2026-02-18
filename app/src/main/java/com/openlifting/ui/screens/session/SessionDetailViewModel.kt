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

data class SeriesMetrics(
    val qualityScore: Int,
    val symmetryIndex: Float,
    val compensationIndex: Float,
    val peakMuscle: MuscleGroup,
    val peakRep: Int,
    val peakActivation: Float,
    val peakSide: String
)

enum class RepQualityLevel(val label: String) {
    GOOD("Buena"),
    ACCEPTABLE("Aceptable"),
    POOR("Deficiente")
}

data class RepQuality(
    val score: Int,
    val level: RepQualityLevel
)

data class SessionDetailUiState(
    val session: Session? = null,
    val selectedSeriesIndex: Int = 0,
    val recommendations: List<String> = emptyList(),
    val seriesScores: List<Int> = emptyList(),
    val currentSeriesMetrics: SeriesMetrics? = null,
    val repQualities: List<RepQuality> = emptyList(),
    val fatigueData: List<Map<MuscleGroup, Float>> = emptyList()
)

class SessionDetailViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SessionDetailUiState())
    val uiState: StateFlow<SessionDetailUiState> = _uiState.asStateFlow()

    fun loadSession(sessionId: String) {
        val session = MockRepository.getSessionById(sessionId) ?: return
        val seriesScores = session.series.map { MockRepository.getSeriesQualityScore(it) }
        val recommendations = MockRepository.generateRecommendations(session)

        _uiState.value = SessionDetailUiState(
            session = session,
            selectedSeriesIndex = 0,
            recommendations = recommendations,
            seriesScores = seriesScores
        )
        updateSeriesMetrics(0)
    }

    fun selectSeries(index: Int) {
        _uiState.value = _uiState.value.copy(selectedSeriesIndex = index)
        updateSeriesMetrics(index)
    }

    private fun updateSeriesMetrics(index: Int) {
        val session = _uiState.value.session ?: return
        val series = session.series.getOrNull(index) ?: return

        val peak = MockRepository.getSeriesPeakActivation(series)

        val metrics = SeriesMetrics(
            qualityScore = MockRepository.getSeriesQualityScore(series),
            symmetryIndex = MockRepository.getSeriesSymmetryIndex(series),
            compensationIndex = MockRepository.getSeriesCompensationIndex(series),
            peakMuscle = peak.muscle,
            peakRep = peak.rep,
            peakActivation = peak.value,
            peakSide = peak.side
        )

        val repQualities = series.repetitions.map { rep ->
            val score = MockRepository.getRepQualityScore(rep)
            RepQuality(
                score = score,
                level = when {
                    score >= 75 -> RepQualityLevel.GOOD
                    score >= 50 -> RepQualityLevel.ACCEPTABLE
                    else -> RepQualityLevel.POOR
                }
            )
        }

        val fatigueData = series.repetitions.map { rep ->
            MockRepository.getRepAverageActivations(rep)
        }

        _uiState.value = _uiState.value.copy(
            currentSeriesMetrics = metrics,
            repQualities = repQualities,
            fatigueData = fatigueData
        )
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
