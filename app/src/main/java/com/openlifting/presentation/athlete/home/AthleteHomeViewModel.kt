package com.openlifting.presentation.athlete.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openlifting.data.local.dao.AthleteProfileDao
import com.openlifting.data.local.dao.SessionDao
import com.openlifting.data.local.dao.SetDao
import com.openlifting.data.local.dao.UserDao
import com.openlifting.data.local.entity.SetMetricsEntity
import com.openlifting.data.local.entity.TrainingSessionEntity
import com.openlifting.domain.model.RiskLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class LastSessionSummary(
    val sessionId: Long,
    val startedAt: Long,
    val setCount: Int,
    val maxLoadKg: Float,
    val durationMinutes: Int?,
    val overallRisk: RiskLevel,
    val bsaWorstPct: Float,
    val esGmaxRatio: Float,
    val hqRatio: Float
)

data class TrendPoint(val timestamp: Long, val value: Float)

data class MetricSnapshot(
    val current: Float,
    val deltaVsPrevious: Float?,
    val risk: RiskLevel
)

sealed interface AthleteHomeUiState {
    data object Loading : AthleteHomeUiState
    data class Empty(
        val athleteFirstName: String,
        val mvcCalibrated: Boolean
    ) : AthleteHomeUiState
    data class Loaded(
        val athleteFirstName: String,
        val mvcCalibrated: Boolean,
        val lastSession: LastSessionSummary,
        val bsaTrend: List<TrendPoint>,
        val esGmax: MetricSnapshot,
        val hq: MetricSnapshot
    ) : AthleteHomeUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AthleteHomeViewModel @Inject constructor(
    private val userDao: UserDao,
    private val athleteProfileDao: AthleteProfileDao,
    private val sessionDao: SessionDao,
    private val setDao: SetDao
) : ViewModel() {

    val uiState: kotlinx.coroutines.flow.StateFlow<AthleteHomeUiState> =
        flow { emit(userDao.getLoggedInUser()) }
            .flatMapLatest { user ->
                if (user == null) flow { emit(AthleteHomeUiState.Loading) }
                else sessionDao.observeForAthlete(user.id).map { sessions ->
                    buildState(
                        firstName = athleteProfileDao.getByUserId(user.id)?.firstName
                            ?: user.name.substringBefore(' '),
                        mvcCalibrated = athleteProfileDao.getByUserId(user.id)?.calibratedAt != null,
                        sessions = sessions
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AthleteHomeUiState.Loading)

    private suspend fun buildState(
        firstName: String,
        mvcCalibrated: Boolean,
        sessions: List<TrainingSessionEntity>
    ): AthleteHomeUiState {
        if (sessions.isEmpty()) {
            return AthleteHomeUiState.Empty(firstName, mvcCalibrated)
        }

        val sessionsWithMetrics: List<SessionMetricsAggregate> = sessions
            .sortedByDescending { it.startedAt }
            .map { aggregateSession(it) }

        // Most recent session (head of the sorted list)
        val latest = sessionsWithMetrics.first()
        val latestKey = latest.lastSetMetrics
            ?: return AthleteHomeUiState.Empty(firstName, mvcCalibrated)

        val previousKey = sessionsWithMetrics.getOrNull(1)?.lastSetMetrics

        val bsaTrend = sessionsWithMetrics
            .reversed()                           // chronological: oldest -> newest
            .takeLast(MAX_TREND_POINTS)
            .map { TrendPoint(it.session.startedAt, it.bsaWorstPct ?: 0f) }
            .filter { it.value > 0f }

        val esGmaxSnapshot = MetricSnapshot(
            current         = latestKey.esGmaxRatio,
            deltaVsPrevious = previousKey?.let { latestKey.esGmaxRatio - it.esGmaxRatio },
            risk            = riskFromEsGmax(latestKey.esGmaxRatio)
        )
        val hqSnapshot = MetricSnapshot(
            current         = latestKey.hqRatio,
            deltaVsPrevious = previousKey?.let { latestKey.hqRatio - it.hqRatio },
            risk            = riskFromHq(latestKey.hqRatio)
        )

        return AthleteHomeUiState.Loaded(
            athleteFirstName = firstName,
            mvcCalibrated    = mvcCalibrated,
            lastSession      = LastSessionSummary(
                sessionId       = latest.session.localId,
                startedAt       = latest.session.startedAt,
                setCount        = latest.setCount,
                maxLoadKg       = latest.maxLoadKg,
                durationMinutes = latest.durationMinutes,
                overallRisk     = latest.overallRisk,
                bsaWorstPct     = latest.bsaWorstPct ?: 0f,
                esGmaxRatio     = latestKey.esGmaxRatio,
                hqRatio         = latestKey.hqRatio
            ),
            bsaTrend = bsaTrend,
            esGmax   = esGmaxSnapshot,
            hq       = hqSnapshot
        )
    }

    private suspend fun aggregateSession(session: TrainingSessionEntity): SessionMetricsAggregate {
        val sets = setDao.getSetsForSession(session.localId)
        val metricsList = sets.mapNotNull { setDao.getMetricsForSet(it.localId) }
        val bsaWorst = metricsList.maxOfOrNull { maxOf(it.bsaVlPct, it.bsaVmPct, it.bsaGmaxPct, it.bsaEsPct) }
        val overallRisk = metricsList.flatMap { metricsToRisks(it) }
            .maxByOrNull { it.ordinal } ?: RiskLevel.NORMAL
        val maxLoad = sets.maxOfOrNull { it.loadKg } ?: 0f
        val duration = session.endedAt?.let { ((it - session.startedAt) / 60_000L).toInt() }
        return SessionMetricsAggregate(
            session         = session,
            setCount        = sets.size,
            maxLoadKg       = maxLoad,
            durationMinutes = duration,
            overallRisk     = overallRisk,
            bsaWorstPct     = bsaWorst,
            lastSetMetrics  = metricsList.lastOrNull()
        )
    }

    private fun metricsToRisks(m: SetMetricsEntity): List<RiskLevel> = listOf(
        riskFromBsa(m.bsaVlPct), riskFromBsa(m.bsaVmPct),
        riskFromBsa(m.bsaGmaxPct), riskFromBsa(m.bsaEsPct),
        riskFromEsGmax(m.esGmaxRatio),
        riskFromHq(m.hqRatio),
        if (m.intraSetFatigueRatio > 1.3f) RiskLevel.RISK else RiskLevel.NORMAL
    )

    private fun riskFromBsa(v: Float) = when {
        v >= 15f -> RiskLevel.RISK; v >= 10f -> RiskLevel.MONITOR; else -> RiskLevel.NORMAL
    }
    private fun riskFromEsGmax(v: Float) = when {
        v >= 2.0f -> RiskLevel.RISK; v >= 1.5f -> RiskLevel.MONITOR; else -> RiskLevel.NORMAL
    }
    private fun riskFromHq(v: Float) = when {
        v < 0.45f -> RiskLevel.RISK; v < 0.60f -> RiskLevel.MONITOR; else -> RiskLevel.NORMAL
    }

    private data class SessionMetricsAggregate(
        val session: TrainingSessionEntity,
        val setCount: Int,
        val maxLoadKg: Float,
        val durationMinutes: Int?,
        val overallRisk: RiskLevel,
        val bsaWorstPct: Float?,
        val lastSetMetrics: SetMetricsEntity?
    )

    companion object {
        private const val MAX_TREND_POINTS = 6
    }
}
