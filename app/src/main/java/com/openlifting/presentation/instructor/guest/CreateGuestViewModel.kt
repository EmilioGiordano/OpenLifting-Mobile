package com.openlifting.presentation.instructor.guest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openlifting.data.local.dao.UserDao
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
    val isValid: Boolean
        get() = firstName.isNotBlank() &&
                lastName.isNotBlank() &&
                (bodyweightKg.toFloatOrNull()?.let { it in 30f..250f } ?: false) &&
                (ageYears.toIntOrNull()?.let { it in 10..90 } ?: false)
}

@HiltViewModel
class CreateGuestViewModel @Inject constructor(
    private val userDao: UserDao,
    private val coachRepository: CoachRepository
) : ViewModel() {

    private val _draft = MutableStateFlow(GuestDraft())
    val draft: StateFlow<GuestDraft> = _draft.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    fun setFirstName(v: String)  { _draft.value = _draft.value.copy(firstName = v) }
    fun setLastName(v: String)   { _draft.value = _draft.value.copy(lastName = v) }
    fun setBodyweight(v: String) { _draft.value = _draft.value.copy(bodyweightKg = v.filter { it.isDigit() || it == '.' }) }
    fun setAge(v: String)        { _draft.value = _draft.value.copy(ageYears = v.filter { it.isDigit() }) }
    fun setSex(v: Sex)           { _draft.value = _draft.value.copy(sex = v) }

    /**
     * Persists the guest. On success invokes [onCreated] with the new athlete profile id so the
     * caller can navigate to the calibration flow for that guest.
     */
    fun createGuest(onCreated: (athleteProfileId: Long) -> Unit) {
        val d = _draft.value
        if (!d.isValid || _isSaving.value) return
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val instructor = userDao.getLoggedInUser() ?: return@launch
                val created = coachRepository.createGuest(
                    instructorUserId = instructor.id,
                    firstName        = d.firstName.trim(),
                    lastName         = d.lastName.trim(),
                    bodyweightKg     = d.bodyweightKg.toFloat(),
                    ageYears         = d.ageYears.toInt(),
                    sex              = d.sex
                )
                onCreated(created.athleteProfileId)
            } finally {
                _isSaving.value = false
            }
        }
    }
}
