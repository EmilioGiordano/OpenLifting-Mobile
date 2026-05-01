package com.openlifting.presentation.athlete.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openlifting.data.local.dao.SetDao
import com.openlifting.domain.model.RiskLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetSummaryItem(
    val setNumber: Int,
    val loadKg: Float,
    val targetReps: Int,
    val overallRisk: RiskLevel,
    val recommendations: List<String>
)

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    private val setDao: SetDao
) : ViewModel() {

    private val _sets = MutableStateFlow<List<SetSummaryItem>>(emptyList())
    val sets = _sets.asStateFlow()

    private var loadedId = -1L

    fun load(sessionId: Long) {
        if (loadedId == sessionId) return
        loadedId = sessionId
        viewModelScope.launch {
            val entities = setDao.getSetsForSession(sessionId)
            _sets.value = entities.mapNotNull { setEntity ->
                val metrics = setDao.getMetricsForSet(setEntity.localId) ?: return@mapNotNull null
                val recs = setDao.getRecommendationsForSet(setEntity.localId).map { it.text }
                val risk = with(metrics) {
                    val r = listOf(
                        bsaLevel(bsaVlPct), bsaLevel(bsaGmaxPct),
                        esGmaxLevel(esGmaxRatio), hqLevel(hqRatio)
                    )
                    r.maxByOrNull { it.ordinal } ?: RiskLevel.NORMAL
                }
                SetSummaryItem(
                    setNumber = setEntity.setNumber,
                    loadKg = setEntity.loadKg,
                    targetReps = setEntity.targetReps,
                    overallRisk = risk,
                    recommendations = recs
                )
            }
        }
    }

    private fun bsaLevel(bsa: Float) =
        when { bsa >= 15f -> RiskLevel.RISK; bsa >= 10f -> RiskLevel.MONITOR; else -> RiskLevel.NORMAL }
    private fun esGmaxLevel(v: Float) =
        when { v >= 2f -> RiskLevel.RISK; v >= 1.5f -> RiskLevel.MONITOR; else -> RiskLevel.NORMAL }
    private fun hqLevel(v: Float) =
        when { v < 0.45f -> RiskLevel.RISK; v < 0.6f -> RiskLevel.MONITOR; else -> RiskLevel.NORMAL }
}
