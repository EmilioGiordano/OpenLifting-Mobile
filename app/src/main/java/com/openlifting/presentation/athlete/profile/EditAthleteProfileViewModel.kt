package com.openlifting.presentation.athlete.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openlifting.data.local.dao.UserDao
import com.openlifting.domain.model.AthleteProfileResult
import com.openlifting.domain.model.Sex
import com.openlifting.domain.repository.AthleteProfileRepository
import com.openlifting.presentation.onboarding.SubmissionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditProfileDraft(
    val firstName: String = "",
    val lastName: String = "",
    val bodyweightKg: String = "",
    val ageYears: String = "",
    val sex: Sex = Sex.MALE,
    val loaded: Boolean = false
) {
    val firstNameError: String? get() = when {
        firstName.isBlank() -> null
        firstName.length > 100 -> "Máximo 100 caracteres."
        else -> null
    }
    val lastNameError: String? get() = when {
        lastName.isBlank() -> null
        lastName.length > 100 -> "Máximo 100 caracteres."
        else -> null
    }
    val bodyweightError: String? get() = when {
        bodyweightKg.isBlank() -> null
        bodyweightKg.toDoubleOrNull() == null -> "Ingrese un número válido."
        bodyweightKg.toDouble() !in 30.0..300.0 -> "El peso debe estar entre 30 y 300 kg."
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
                lastName.isNotBlank() && lastNameError == null &&
                bodyweightKg.isNotBlank() && bodyweightError == null &&
                ageYears.isNotBlank() && ageError == null
}

@HiltViewModel
class EditAthleteProfileViewModel @Inject constructor(
    private val userDao: UserDao,
    private val repository: AthleteProfileRepository
) : ViewModel() {

    private val _draft = MutableStateFlow(EditProfileDraft())
    val draft: StateFlow<EditProfileDraft> = _draft.asStateFlow()

    private val _submission = MutableStateFlow<SubmissionState>(SubmissionState.Idle)
    val submission: StateFlow<SubmissionState> = _submission.asStateFlow()

    init {
        viewModelScope.launch {
            val userId = userDao.getLoggedInUser()?.id ?: return@launch
            val cached = repository.getCachedProfile(userId) ?: return@launch
            _draft.value = EditProfileDraft(
                firstName    = cached.firstName,
                lastName     = cached.lastName,
                bodyweightKg = cached.bodyweightKg.toInt().toString(),
                ageYears     = cached.ageYears.toString(),
                sex          = cached.sex,
                loaded       = true
            )
        }
    }

    fun setFirstName(v: String)   { _draft.value = _draft.value.copy(firstName = v) }
    fun setLastName(v: String)    { _draft.value = _draft.value.copy(lastName = v) }
    fun setBodyweight(v: String)  { _draft.value = _draft.value.copy(bodyweightKg = v.filter { it.isDigit() || it == '.' }) }
    fun setAge(v: String)         { _draft.value = _draft.value.copy(ageYears = v.filter { it.isDigit() }) }
    fun setSex(v: Sex)            { _draft.value = _draft.value.copy(sex = v) }

    fun clearError() {
        if (_submission.value !is SubmissionState.Submitting) {
            _submission.value = SubmissionState.Idle
        }
    }

    fun save(onSaved: () -> Unit) {
        val d = _draft.value
        if (!d.isValid) return
        viewModelScope.launch {
            _submission.value = SubmissionState.Submitting
            val result = repository.updateProfile(
                firstName    = d.firstName.trim(),
                lastName     = d.lastName.trim(),
                bodyweightKg = d.bodyweightKg.toDouble(),
                ageYears     = d.ageYears.toInt(),
                sex          = d.sex
            )
            when (result) {
                is AthleteProfileResult.Success -> {
                    _submission.value = SubmissionState.Idle
                    onSaved()
                }
                is AthleteProfileResult.ValidationError ->
                    _submission.value = SubmissionState.FieldErrors(result.errors)
                AthleteProfileResult.NotFound ->
                    _submission.value = SubmissionState.Error("No tiene perfil creado todavía.")
                AthleteProfileResult.Forbidden ->
                    _submission.value = SubmissionState.Error("No tiene permisos para editar este perfil.")
                AthleteProfileResult.Unauthorized ->
                    _submission.value = SubmissionState.Error("Su sesión expiró. Vuelva a iniciar sesión.")
                AthleteProfileResult.Throttled ->
                    _submission.value = SubmissionState.Error("Demasiados intentos. Espere un momento.")
                is AthleteProfileResult.NetworkError ->
                    _submission.value = SubmissionState.NetworkError
                is AthleteProfileResult.ServerError ->
                    _submission.value = SubmissionState.Error("Error del servidor (${result.code}).")
            }
        }
    }
}
