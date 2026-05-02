package com.openlifting.presentation.common.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openlifting.data.local.dao.UserDao
import com.openlifting.data.preferences.ThemePreferences
import com.openlifting.domain.model.ThemeMode
import com.openlifting.domain.model.User
import com.openlifting.domain.model.UserRole
import com.openlifting.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Shared by [com.openlifting.presentation.athlete.profile.AthleteProfileScreen] and
 * [com.openlifting.presentation.instructor.profile.InstructorProfileScreen]. Reads the
 * currently logged-in user, exposes the theme preference, and handles logout.
 *
 * Athlete-specific data (bodyweight, age, sex, calibration status) is intentionally NOT
 * loaded here yet — it will move to a dedicated AthleteProfileViewModel when the
 * AthleteProfile entity is populated by the onboarding flow.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userDao: UserDao,
    private val authRepository: AuthRepository,
    private val themePreferences: ThemePreferences
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    val themeMode: StateFlow<ThemeMode> = themePreferences.themeMode

    init {
        viewModelScope.launch {
            val u = userDao.getLoggedInUser() ?: return@launch
            _user.value = User(
                id        = u.id,
                email     = u.email,
                name      = u.name,
                role      = UserRole.valueOf(u.role),
                authToken = u.authToken,
                serverId  = u.serverId
            )
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        themePreferences.setThemeMode(mode)
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onDone()
        }
    }
}
