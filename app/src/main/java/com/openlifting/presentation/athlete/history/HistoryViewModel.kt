package com.openlifting.presentation.athlete.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openlifting.data.local.dao.SessionDao
import com.openlifting.data.local.dao.SetDao
import com.openlifting.data.local.dao.UserDao
import com.openlifting.domain.model.RiskLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SessionHistoryItem(
    val sessionId: Long,
    val startedAt: Long,
    val setCount: Int,
    val overallRisk: RiskLevel
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val userDao: UserDao,
    private val sessionDao: SessionDao,
    private val setDao: SetDao
) : ViewModel() {

    val sessions = flow { emit(userDao.getLoggedInUser()) }
        .flatMapLatest { user ->
            if (user == null) flow { emit(emptyList<SessionHistoryItem>()) }
            else sessionDao.observeForAthlete(user.id).map { entities ->
                entities.map { session ->
                    val sets = setDao.getSetsForSession(session.localId)
                    val worstRisk = sets.mapNotNull { set ->
                        setDao.getMetricsForSet(set.localId)
                    }.map { metrics ->
                        from(metrics.bsaVlPct, metrics.esGmaxRatio, metrics.hqRatio)
                    }.maxByOrNull { it.ordinal } ?: RiskLevel.NORMAL

                    SessionHistoryItem(
                        sessionId = session.localId,
                        startedAt = session.startedAt,
                        setCount = sets.size,
                        overallRisk = worstRisk
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun from(bsa: Float, esGmax: Float, hq: Float): RiskLevel {
        val bsaRisk = when { bsa >= 15f -> RiskLevel.RISK; bsa >= 10f -> RiskLevel.MONITOR; else -> RiskLevel.NORMAL }
        val esRisk = when { esGmax >= 2f -> RiskLevel.RISK; esGmax >= 1.5f -> RiskLevel.MONITOR; else -> RiskLevel.NORMAL }
        val hqRisk = when { hq < 0.45f -> RiskLevel.RISK; hq < 0.6f -> RiskLevel.MONITOR; else -> RiskLevel.NORMAL }
        return listOf(bsaRisk, esRisk, hqRisk).maxByOrNull { it.ordinal } ?: RiskLevel.NORMAL
    }
}
