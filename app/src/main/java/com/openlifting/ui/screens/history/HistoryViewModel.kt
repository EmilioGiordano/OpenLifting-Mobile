package com.openlifting.ui.screens.history

import androidx.lifecycle.ViewModel
import com.openlifting.data.model.BalanceStatus
import com.openlifting.data.model.Session
import com.openlifting.data.repository.MockRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SessionSummary(
    val session: Session,
    val balanceStatus: BalanceStatus,
    val alertSummary: String?
)

data class HistoryUiState(
    val sessions: List<SessionSummary> = emptyList()
)

class HistoryViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadSessions()
    }

    private fun loadSessions() {
        val sessions = MockRepository.getSessions()
            .sortedByDescending { it.date }
            .map { session ->
                SessionSummary(
                    session = session,
                    balanceStatus = MockRepository.getSessionBalanceStatus(session),
                    alertSummary = MockRepository.getSessionAlertSummary(session)
                )
            }
        _uiState.value = HistoryUiState(sessions = sessions)
    }
}
