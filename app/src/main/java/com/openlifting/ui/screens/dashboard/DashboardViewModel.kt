package com.openlifting.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import com.openlifting.data.model.BalanceStatus
import com.openlifting.data.model.Session
import com.openlifting.data.repository.MockRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DashboardUiState(
    val latestSession: Session? = null,
    val balanceStatus: BalanceStatus = BalanceStatus.GOOD,
    val recommendations: List<String> = emptyList(),
    val totalSessions: Int = 0
)

class DashboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    private fun loadDashboard() {
        val latest = MockRepository.getLatestSession()
        val sessions = MockRepository.getSessions()

        _uiState.value = DashboardUiState(
            latestSession = latest,
            balanceStatus = latest?.let { MockRepository.getSessionBalanceStatus(it) } ?: BalanceStatus.GOOD,
            recommendations = latest?.let { MockRepository.generateRecommendations(it) } ?: emptyList(),
            totalSessions = sessions.size
        )
    }
}
