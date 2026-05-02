package com.openlifting.presentation.athlete.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openlifting.data.local.dao.SessionDao
import com.openlifting.data.local.dao.SetDao
import com.openlifting.data.local.dao.UserDao
import com.openlifting.data.mapper.toDomain
import com.openlifting.domain.model.RiskLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SessionHistoryItem(
    val sessionId: Long,
    val startedAt: Long,
    val setCount: Int,
    val maxLoadKg: Float,
    val bsaWorstPct: Float,
    val overallRisk: RiskLevel
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val userDao: UserDao,
    private val sessionDao: SessionDao,
    private val setDao: SetDao
) : ViewModel() {

    val sessions: StateFlow<List<SessionHistoryItem>> =
        flow { emit(userDao.getLoggedInUser()) }
            .flatMapLatest { user ->
                if (user == null) flow { emit(emptyList()) }
                else sessionDao.observeForAthlete(user.id).map { entities ->
                    entities.map { session ->
                        val sets = setDao.getSetsForSession(session.localId)
                        val metricsList = sets.mapNotNull { setDao.getMetricsForSet(it.localId)?.toDomain() }

                        val worstRisk = metricsList
                            .map { it.overallRisk }
                            .maxByOrNull { it.ordinal }
                            ?: RiskLevel.NORMAL

                        val bsaWorst = metricsList.maxOfOrNull { it.bsaWorstPct } ?: 0f
                        val maxLoad  = sets.maxOfOrNull { it.loadKg } ?: 0f

                        SessionHistoryItem(
                            sessionId   = session.localId,
                            startedAt   = session.startedAt,
                            setCount    = sets.size,
                            maxLoadKg   = maxLoad,
                            bsaWorstPct = bsaWorst,
                            overallRisk = worstRisk
                        )
                    }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
