package com.openlifting.presentation.instructor.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openlifting.data.local.dao.UserDao
import com.openlifting.domain.repository.CoachRepository
import com.openlifting.domain.repository.ManagedAthlete
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class InstructorHomeUiData(
    val instructorFirstName: String,
    val athletes: List<ManagedAthlete>
) {
    val isEmpty: Boolean get() = athletes.isEmpty()
    val guestCount: Int get() = athletes.count { it.isGuest }
    val registeredCount: Int get() = athletes.count { !it.isGuest }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InstructorHomeViewModel @Inject constructor(
    private val userDao: UserDao,
    private val coachRepository: CoachRepository
) : ViewModel() {

    val uiState: StateFlow<InstructorHomeUiData?> =
        flow { emit(userDao.getLoggedInUser()) }
            .flatMapLatest { user ->
                if (user == null) flowOf(null)
                else coachRepository.observeManagedAthletes(user.id).map { athletes ->
                    InstructorHomeUiData(
                        instructorFirstName = user.name.substringBefore(' '),
                        athletes            = athletes
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}
