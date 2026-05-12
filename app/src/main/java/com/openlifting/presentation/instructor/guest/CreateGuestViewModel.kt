package com.openlifting.presentation.instructor.guest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openlifting.data.local.dao.AthleteProfileDao
import com.openlifting.data.local.dao.UserDao
import com.openlifting.domain.model.GuestProfileResult
import com.openlifting.domain.model.Sex
import com.openlifting.domain.repository.CoachRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GuestDraft(
    val firstName: String     = "",
    val lastName: String      = "",
    val bodyweightKg: String   = "",
    val ageYears: String       = "",
    val sex: Sex               = Sex.MALE
) {
    // Mirror the backend CHECK constraints (chk_athlete_profiles_bodyweight 30-300,
    // chk_athlete_profiles_age 14-100) so the form rejects the same range the server would.
    val firstNameError: String? get() = when {
        firstName.isBlank() -> null
        firstName.trim().length !in 1..100 -> "Entre 1 y 100 caracteres."
        else -> null
    }
    val lastNameError: String? get() = when {
        lastName.isBlank() -> null
        lastName.trim().length !in 1..100 -> "Entre 1 y 100 caracteres."
        else -> null
    }
    val bodyweightError: String? get() = when {
        bodyweightKg.isBlank() -> null
        bodyweightKg.toFloatOrNull() == null -> "Ingrese un número válido."
        bodyweightKg.toFloat() !in 30f..300f -> "El peso debe estar entre 30 y 300 kg."
        else -> null
    }
    val ageError: String? get() = when {
        ageYears.isBlank() -> null
        ageYears.toIntOrNull() == null -> "Ingrese un número entero."
        ageYears.toInt() !in 14..100 -> "La edad debe estar entre 14 y 100 años."
        else -> null
    }

    val isValid: Boolean
        get() = firstName.isNotBlank() && firstNameError == null &&
                lastName.isNotBlank()  && lastNameError == null &&
                bodyweightKg.isNotBlank() && bodyweightError == null &&
                ageYears.isNotBlank() && ageError == null
}

sealed interface CreateGuestUiEffect {
    data class Created(val athleteProfileId: Long) : CreateGuestUiEffect
    data class Error(val message: String) : CreateGuestUiEffect
}

@HiltViewModel
class CreateGuestViewModel @Inject constructor(
    private val userDao: UserDao,
    private val athleteProfileDao: AthleteProfileDao,
    private val coachRepository: CoachRepository
) : ViewModel() {

    private val _draft = MutableStateFlow(GuestDraft())
    val draft: StateFlow<GuestDraft> = _draft.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    /** Per-field errors mirrored from the backend 422 response (e.g. server-side rejection). */
    private val _fieldErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val fieldErrors: StateFlow<Map<String, String>> = _fieldErrors.asStateFlow()

    private val _submissionError = MutableStateFlow<String?>(null)
    val submissionError: StateFlow<String?> = _submissionError.asStateFlow()

    fun setFirstName(v: String)  { _draft.value = _draft.value.copy(firstName = v); clearErrors() }
    fun setLastName(v: String)   { _draft.value = _draft.value.copy(lastName = v); clearErrors() }
    fun setBodyweight(v: String) {
        _draft.value = _draft.value.copy(bodyweightKg = v.filter { it.isDigit() || it == '.' })
        clearErrors()
    }
    fun setAge(v: String) {
        _draft.value = _draft.value.copy(ageYears = v.filter { it.isDigit() })
        clearErrors()
    }
    fun setSex(v: Sex) { _draft.value = _draft.value.copy(sex = v); clearErrors() }

    private fun clearErrors() {
        if (_fieldErrors.value.isNotEmpty()) _fieldErrors.value = emptyMap()
        if (_submissionError.value != null) _submissionError.value = null
    }

    /**
     * Persists the guest. On success invokes [onCreated] with the new local athlete profile id
     * so the caller can navigate to the calibration flow for that guest.
     */
    fun createGuest(onCreated: (athleteProfileId: Long) -> Unit) {
        val d = _draft.value
        if (!d.isValid || _isSaving.value) return
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val instructor = userDao.getLoggedInUser() ?: run {
                    _submissionError.value = "No hay un instructor logueado."
                    return@launch
                }
                when (val result = coachRepository.createGuest(
                    instructorUserId = instructor.id,
                    firstName        = d.firstName.trim(),
                    lastName         = d.lastName.trim(),
                    bodyweightKg     = d.bodyweightKg.toFloat(),
                    ageYears         = d.ageYears.toInt(),
                    sex              = d.sex
                )) {
                    is GuestProfileResult.Success -> {
                        // The repo writes to Room with the local profile id we need to navigate to.
                        // Fetch the freshly inserted profile by stub user to recover the local id.
                        // The repo returns the local profile id in `result.guest.id` for convenience.
                        onCreated(result.guest.id)
                    }
                    is GuestProfileResult.ValidationError ->
                        _fieldErrors.value = result.errors.mapValues { it.value.firstOrNull().orEmpty() }
                    GuestProfileResult.Forbidden ->
                        _submissionError.value = "Solo un entrenador puede crear invitados."
                    GuestProfileResult.Unauthorized ->
                        _submissionError.value = "Su sesión expiró. Vuelva a iniciar sesión."
                    GuestProfileResult.NotFound,
                    GuestProfileResult.Throttled ->
                        _submissionError.value = "No se pudo crear al invitado. Intente de nuevo."
                    is GuestProfileResult.NetworkError ->
                        _submissionError.value = "Sin conexión. Revise la red e intente de nuevo."
                    is GuestProfileResult.ServerError ->
                        _submissionError.value = "Error del servidor (${result.code}). Intente más tarde."
                }
            } finally {
                _isSaving.value = false
            }
        }
    }
}
