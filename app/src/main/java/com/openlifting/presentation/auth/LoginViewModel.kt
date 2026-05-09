package com.openlifting.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openlifting.domain.model.AuthResult
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
    data class FieldErrors(val errors: Map<String, List<String>>) : LoginUiState
    data class Error(val message: String) : LoginUiState
    data object Throttled : LoginUiState
    data object NetworkError : LoginUiState
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
            _uiState.value = mapResult(authRepository.login(email.trim(), password))
        }
    }

    fun register(name: String, email: String, password: String, role: UserRole) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            _uiState.value = mapResult(
                authRepository.register(name.trim(), email.trim(), password, role)
            )
        }
    }

    fun checkSession() {
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            val cached = authRepository.getCachedUser()
            if (cached == null) {
                _uiState.value = LoginUiState.Idle
                return@launch
            }
            when (val result = authRepository.probeSession()) {
                is AuthResult.Success    -> _uiState.value = LoginUiState.Success(result.user.role)
                AuthResult.Unauthorized  -> _uiState.value = LoginUiState.Idle
                else                     -> _uiState.value = LoginUiState.Success(cached.role)
            }
        }
    }

    fun clearTransientError() {
        if (_uiState.value !is LoginUiState.Success && _uiState.value !is LoginUiState.Loading) {
            _uiState.value = LoginUiState.Idle
        }
    }

    private fun mapResult(result: AuthResult): LoginUiState = when (result) {
        is AuthResult.Success         -> LoginUiState.Success(result.user.role)
        is AuthResult.ValidationError -> LoginUiState.FieldErrors(result.errors)
        AuthResult.Throttled          -> LoginUiState.Throttled
        AuthResult.Unauthorized       -> LoginUiState.Error("Sesión no válida. Iniciá sesión de nuevo.")
        is AuthResult.NetworkError    -> LoginUiState.NetworkError
        is AuthResult.ServerError     -> LoginUiState.Error("Error del servidor (${result.code}).")
    }
}
