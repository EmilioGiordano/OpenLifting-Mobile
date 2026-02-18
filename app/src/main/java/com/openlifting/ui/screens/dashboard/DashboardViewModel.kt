package com.openlifting.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import com.openlifting.data.model.BalanceStatus
import com.openlifting.data.model.MuscleGroup
import com.openlifting.data.model.Session
import com.openlifting.data.repository.MockRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class RecommendationSeverity { HIGH, MEDIUM, LOW }

data class DashboardRecommendation(
    val text: String,
    val severity: RecommendationSeverity
)

data class DashboardUiState(
    val latestSession: Session? = null,
    val balanceStatus: BalanceStatus = BalanceStatus.GOOD,
    val recommendations: List<DashboardRecommendation> = emptyList(),
    val totalSessions: Int = 0,
    val qualityScore: Int = 0,
    val alertSummary: String? = null,
    val muscleAverages: Map<MuscleGroup, Float> = emptyMap(),
    val symmetryIndex: Float = 100f,
    val sessionScoreTrend: List<Int> = emptyList(),
    val userName: String = "",
    val maxWeightHistoric: Int = 0
)

class DashboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    private fun loadDashboard() {
        val latest = MockRepository.getLatestSession()
        val sessions = MockRepository.getSessions().sortedBy { it.date }
        val lastSeries = latest?.series?.lastOrNull()
        val profile = MockRepository.getUserProfile()

        val rawRecommendations = latest?.let { MockRepository.generateRecommendations(it) } ?: emptyList()
        val classifiedRecommendations = rawRecommendations.map { text ->
            val severity = when {
                text.contains("lumbar", ignoreCase = true) ||
                    text.contains("desbalance", ignoreCase = true) -> RecommendationSeverity.HIGH
                text.contains("fatiga", ignoreCase = true) ||
                    text.contains("degrada", ignoreCase = true) -> RecommendationSeverity.MEDIUM
                else -> RecommendationSeverity.LOW
            }
            DashboardRecommendation(text = text, severity = severity)
        }

        _uiState.value = DashboardUiState(
            latestSession = latest,
            balanceStatus = latest?.let { MockRepository.getSessionBalanceStatus(it) } ?: BalanceStatus.GOOD,
            recommendations = classifiedRecommendations,
            totalSessions = sessions.size,
            qualityScore = lastSeries?.let { MockRepository.getSeriesQualityScore(it) } ?: 0,
            alertSummary = latest?.let { MockRepository.getSessionAlertSummary(it) },
            muscleAverages = lastSeries?.let { series ->
                series.repetitions.lastOrNull()?.let { rep ->
                    MockRepository.getRepAverageActivations(rep)
                }
            } ?: emptyMap(),
            symmetryIndex = lastSeries?.let { MockRepository.getSeriesSymmetryIndex(it) } ?: 100f,
            sessionScoreTrend = sessions.mapNotNull { s ->
                s.series.lastOrNull()?.let { MockRepository.getSeriesQualityScore(it) }
            },
            userName = profile.name.split(" ").firstOrNull() ?: profile.name,
            maxWeightHistoric = sessions.flatMap { it.series }.maxOfOrNull { it.weightKg.toInt() } ?: 0
        )
    }
}
