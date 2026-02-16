package com.openlifting.ui.screens.profile

import androidx.lifecycle.ViewModel
import com.openlifting.data.model.UserProfile
import com.openlifting.data.repository.MockRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ProfileUiState(
    val name: String = "",
    val email: String = "",
    val heightCm: String = "",
    val weightKg: String = "",
    val age: String = "",
    val saved: Boolean = false
)

class ProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        val profile = MockRepository.getUserProfile()
        _uiState.value = ProfileUiState(
            name = profile.name,
            email = profile.email,
            heightCm = profile.heightCm.toString(),
            weightKg = profile.weightKg.toInt().toString(),
            age = profile.age.toString()
        )
    }

    fun updateName(value: String) {
        _uiState.value = _uiState.value.copy(name = value, saved = false)
    }

    fun updateEmail(value: String) {
        _uiState.value = _uiState.value.copy(email = value, saved = false)
    }

    fun updateHeight(value: String) {
        _uiState.value = _uiState.value.copy(heightCm = value, saved = false)
    }

    fun updateWeight(value: String) {
        _uiState.value = _uiState.value.copy(weightKg = value, saved = false)
    }

    fun updateAge(value: String) {
        _uiState.value = _uiState.value.copy(age = value, saved = false)
    }

    fun saveProfile() {
        val state = _uiState.value
        val profile = UserProfile(
            name = state.name,
            email = state.email,
            heightCm = state.heightCm.toIntOrNull() ?: 0,
            weightKg = state.weightKg.toFloatOrNull() ?: 0f,
            age = state.age.toIntOrNull() ?: 0
        )
        MockRepository.updateUserProfile(profile)
        _uiState.value = _uiState.value.copy(saved = true)
    }
}
