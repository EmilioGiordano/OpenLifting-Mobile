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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AthleteProfileViewModel @Inject constructor(
    private val userDao: UserDao,
    private val repository: AthleteProfileRepository
) : ViewModel() {

    private val userIdFlow = flow {
        emit(userDao.getLoggedInUser()?.id)
    }

    val athleteProfile: StateFlow<AthleteProfile?> =
        userIdFlow.flatMapLatest { id ->
            if (id == null) flowOf(null) else repository.observeCachedProfile(id)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = null
        )

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _refreshing.value = true
            repository.fetchProfile()
            _refreshing.value = false
        }
    }
}
