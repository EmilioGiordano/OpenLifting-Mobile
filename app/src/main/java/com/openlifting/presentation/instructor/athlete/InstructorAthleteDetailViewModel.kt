package com.openlifting.presentation.instructor.athlete

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openlifting.data.local.dao.AthleteProfileDao
import com.openlifting.data.local.dao.SessionDao
import com.openlifting.data.local.dao.SetDao
import com.openlifting.data.local.dao.UserDao
import com.openlifting.data.local.entity.MvcCalibrationEntity
import com.openlifting.data.local.entity.TrainingSessionEntity
import com.openlifting.data.mapper.toDomain
import com.openlifting.domain.model.Muscle
import com.openlifting.domain.model.MuscleSide
import com.openlifting.domain.model.RiskLevel
import com.openlifting.domain.repository.CoachRepository
import com.openlifting.domain.repository.ManagedAthlete
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AthleteSessionRow(
    val sessionId: Long,
    val startedAt: Long,
    val setCount: Int,
    val maxLoadKg: Float,
    val overallRisk: RiskLevel
)

data class InstructorAthleteDetailUiData(
    val athlete: ManagedAthlete,
    val instructorUserId: Long,
    val recentSessions: List<AthleteSessionRow>,
    val mvc: Map<Pair<Muscle, MuscleSide>, Float>
)

sealed interface InstructorAthleteDetailUiState {
    data object Loading : InstructorAthleteDetailUiState
    data object NotFound : InstructorAthleteDetailUiState
    data class Loaded(val data: InstructorAthleteDetailUiData) : InstructorAthleteDetailUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InstructorAthleteDetailViewModel @Inject constructor(
    private val userDao: UserDao,
    private val athleteProfileDao: AthleteProfileDao,
    private val sessionDao: SessionDao,
    private val setDao: SetDao,
    private val coachRepository: CoachRepository
) : ViewModel() {

    private val _state = MutableStateFlow<InstructorAthleteDetailUiState>(
        InstructorAthleteDetailUiState.Loading
    )
    val uiState: StateFlow<InstructorAthleteDetailUiState> = _state

    private var loadedProfileId: Long = -1L

    /** Idempotent loader. Re-running with the same profileId is a no-op. */
    fun load(profileId: Long) {
        if (loadedProfileId == profileId) return
        loadedProfileId = profileId
        viewModelScope.launch {
            val instructor = userDao.getLoggedInUser() ?: run {
                _state.value = InstructorAthleteDetailUiState.NotFound; return@launch
            }
            val athlete = coachRepository.getManagedAthlete(profileId) ?: run {
                _state.value = InstructorAthleteDetailUiState.NotFound; return@launch
            }
            // Subscribe to sessions + MVC calibrations together
            val sessionFlow = sessionDao.observeForAthlete(athlete.athleteUserId)
            val mvcFlow     = athleteProfileDao.observeCalibrations(athlete.profile.id)

            combine(sessionFlow, mvcFlow) { sessions, mvcs ->
                InstructorAthleteDetailUiData(
                    athlete          = athlete,
                    instructorUserId = instructor.id,
                    recentSessions   = sessions.toRows().take(MAX_RECENT_SESSIONS),
                    mvc              = mvcs.toDomainMap()
                )
            }.collect { _state.value = InstructorAthleteDetailUiState.Loaded(it) }
        }
    }

    private suspend fun List<TrainingSessionEntity>.toRows(): List<AthleteSessionRow> = map { session ->
        val sets = setDao.getSetsForSession(session.localId)
        val metrics = sets.mapNotNull { setDao.getMetricsForSet(it.localId)?.toDomain() }
        val worstRisk = metrics.maxByOrNull { it.overallRisk.ordinal }?.overallRisk ?: RiskLevel.NORMAL
        AthleteSessionRow(
            sessionId   = session.localId,
            startedAt   = session.startedAt,
            setCount    = sets.size,
            maxLoadKg   = sets.maxOfOrNull { it.loadKg } ?: 0f,
            overallRisk = worstRisk
        )
    }

    private fun List<MvcCalibrationEntity>.toDomainMap(): Map<Pair<Muscle, MuscleSide>, Float> =
        associate { (Muscle.valueOf(it.muscle) to MuscleSide.valueOf(it.side)) to it.mvcValue }

    companion object {
        private const val MAX_RECENT_SESSIONS = 3
    }
}
