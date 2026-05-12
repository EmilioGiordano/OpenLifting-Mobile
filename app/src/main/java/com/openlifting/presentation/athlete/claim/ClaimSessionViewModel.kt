package com.openlifting.presentation.athlete.claim

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openlifting.domain.model.ClaimRedeemResult
import com.openlifting.domain.repository.AthleteProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ClaimSessionUiState {
    data object Idle : ClaimSessionUiState
    data object Submitting : ClaimSessionUiState
    data class Success(val sessionLocalId: Long) : ClaimSessionUiState
    data class Error(val message: String) : ClaimSessionUiState
}

@HiltViewModel
class ClaimSessionViewModel @Inject constructor(
    private val athleteProfileRepository: AthleteProfileRepository
) : ViewModel() {

    private val _code = MutableStateFlow("")
    val code: StateFlow<String> = _code.asStateFlow()

    private val _ui = MutableStateFlow<ClaimSessionUiState>(ClaimSessionUiState.Idle)
    val uiState: StateFlow<ClaimSessionUiState> = _ui.asStateFlow()

    /** Codes are 8 chars, uppercase alphanumeric without 0/O/1/I/L. */
    private val codeAlphabet = Regex("^[A-Z2-9]*$")

    fun setCode(v: String) {
        // Normalise to uppercase and drop chars outside the backend alphabet so we don't even
        // attempt to send malformed input.
        val cleaned = v.uppercase().filter { it.toString().matches(codeAlphabet) }.take(8)
        _code.value = cleaned
        if (_ui.value is ClaimSessionUiState.Error) _ui.value = ClaimSessionUiState.Idle
    }

    val isReady: Boolean get() = _code.value.length == 8

    fun submit() {
        if (!isReady || _ui.value is ClaimSessionUiState.Submitting) return
        val current = _code.value
        viewModelScope.launch {
            _ui.value = ClaimSessionUiState.Submitting
            when (val result = athleteProfileRepository.claimSession(current)) {
                is ClaimRedeemResult.Success ->
                    _ui.value = ClaimSessionUiState.Success(result.sessionLocalId)
                ClaimRedeemResult.NotFound ->
                    _ui.value = ClaimSessionUiState.Error("Ese código no existe.")
                ClaimRedeemResult.ExpiredOrUsed ->
                    _ui.value = ClaimSessionUiState.Error("El código expiró o ya fue usado.")
                ClaimRedeemResult.Forbidden ->
                    _ui.value = ClaimSessionUiState.Error("Tu cuenta no puede reclamar sesiones.")
                ClaimRedeemResult.Throttled ->
                    _ui.value = ClaimSessionUiState.Error("Demasiados intentos. Espere unos minutos.")
                is ClaimRedeemResult.ValidationError ->
                    _ui.value = ClaimSessionUiState.Error(
                        result.errors.values.flatten().firstOrNull() ?: "El código no es válido."
                    )
                is ClaimRedeemResult.NetworkError ->
                    _ui.value = ClaimSessionUiState.Error("Sin conexión. Revise la red.")
                is ClaimRedeemResult.ServerError ->
                    _ui.value = ClaimSessionUiState.Error("Error del servidor (${result.code}).")
            }
        }
    }

    fun reset() {
        _code.value = ""
        _ui.value = ClaimSessionUiState.Idle
    }
}
