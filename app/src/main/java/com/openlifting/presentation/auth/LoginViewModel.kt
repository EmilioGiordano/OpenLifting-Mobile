package com.openlifting.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openlifting.domain.model.UserRole
import com.openlifting.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class Success(val role: UserRole) : LoginUiState
    data class Error(val message: String) : LoginUiState
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            val user = authRepository.login(email, password)
            _uiState.value = if (user != null)
                LoginUiState.Success(user.role)
            else
                LoginUiState.Error("Credenciales inválidas")
        }
    }

    fun register(name: String, email: String, password: String, role: UserRole) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            val user = authRepository.register(name, email, password, role)
            _uiState.value = LoginUiState.Success(user.role)
        }
    }

    fun checkSession() {
        viewModelScope.launch {
            val user = authRepository.getLoggedInUser()
            if (user != null) _uiState.value = LoginUiState.Success(user.role)
        }
    }
}
