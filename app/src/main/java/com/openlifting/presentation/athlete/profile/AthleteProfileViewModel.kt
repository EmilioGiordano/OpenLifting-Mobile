package com.openlifting.presentation.athlete.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openlifting.data.local.dao.UserDao
import com.openlifting.domain.model.AthleteProfile
import com.openlifting.domain.repository.AthleteProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AthleteProfileUiState {
    data object Loading : AthleteProfileUiState
    data object Missing : AthleteProfileUiState
    data class Loaded(val profile: AthleteProfile) : AthleteProfileUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AthleteProfileViewModel @Inject constructor(
    private val userDao: UserDao,
    private val repository: AthleteProfileRepository
) : ViewModel() {

    private val userIdFlow = flow {
        emit(userDao.getLoggedInUser()?.id)
    }

    private val cachedProfile = userIdFlow.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.observeCachedProfile(id)
    }

    private val _initialFetchDone = MutableStateFlow(false)

    val uiState: StateFlow<AthleteProfileUiState> =
        combine(cachedProfile, _initialFetchDone) { profile, fetched ->
            when {
                profile != null -> AthleteProfileUiState.Loaded(profile)
                !fetched        -> AthleteProfileUiState.Loading
                else            -> AthleteProfileUiState.Missing
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = AthleteProfileUiState.Loading
        )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            repository.fetchProfile()
            _initialFetchDone.value = true
        }
    }
}
