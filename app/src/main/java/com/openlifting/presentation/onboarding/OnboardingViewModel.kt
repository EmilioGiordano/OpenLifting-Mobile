package com.openlifting.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openlifting.data.local.dao.AthleteProfileDao
import com.openlifting.data.local.dao.UserDao
import com.openlifting.data.simulator.Esp32Simulator
import com.openlifting.domain.model.AthleteProfileResult
import com.openlifting.domain.model.Muscle
import com.openlifting.domain.model.MuscleSide
import com.openlifting.domain.model.MvcCalibration
import com.openlifting.domain.model.MvcCalibrationResult
import com.openlifting.domain.model.Sex
import com.openlifting.domain.repository.AthleteProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileDraft(
    val firstName: String     = "",
    val lastName: String      = "",
    val bodyweightKg: String   = "",
    val ageYears: String       = "",
    val sex: Sex               = Sex.MALE
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
        bodyweightKg.toFloatOrNull() == null -> "Ingresá un número válido."
        bodyweightKg.toFloat() !in 30f..300f -> "El peso debe estar entre 30 y 300 kg."
        else -> null
    }
    val ageError: String? get() = when {
        ageYears.isBlank() -> null
        ageYears.toIntOrNull() == null -> "Ingresá un número entero."
        ageYears.toInt() !in 14..100 -> "La edad debe estar entre 14 y 100 años."
        else -> null
    }

    val isValid: Boolean
        get() = firstName.isNotBlank() && firstNameError == null &&
                lastName.isNotBlank() && lastNameError == null &&
                bodyweightKg.isNotBlank() && bodyweightError == null &&
                ageYears.isNotBlank() && ageError == null
}

enum class CapturePhase { PREPARE, CONTRACT, DONE }

data class MvcMeasurement(
    val muscle: Muscle,
    val side: MuscleSide,
    val capturedPct: Float? = null
)

data class MvcCaptureUiState(
    val measurements: List<MvcMeasurement>,
    val currentIndex: Int,
    val phase: CapturePhase,
    val livePct: Float,
    val countdown: Int,
    val finished: Boolean
) {
    val current: MvcMeasurement get() = measurements[currentIndex]
    val totalSteps: Int get() = measurements.size
    val stepLabel: String get() = "${currentIndex + 1} / $totalSteps"
}

sealed interface SubmissionState {
    data object Idle : SubmissionState
    data object Submitting : SubmissionState
    data class FieldErrors(val errors: Map<String, List<String>>) : SubmissionState
    data class Error(val message: String) : SubmissionState
    data object NetworkError : SubmissionState
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userDao: UserDao,
    private val athleteProfileDao: AthleteProfileDao,
    private val athleteProfileRepository: AthleteProfileRepository,
    private val simulator: Esp32Simulator
) : ViewModel() {

    private val _profile = MutableStateFlow(ProfileDraft())
    val profile: StateFlow<ProfileDraft> = _profile.asStateFlow()

    private val _mvc = MutableStateFlow(initialMvcState())
    val mvc: StateFlow<MvcCaptureUiState> = _mvc.asStateFlow()

    private val _profileSubmission = MutableStateFlow<SubmissionState>(SubmissionState.Idle)
    val profileSubmission: StateFlow<SubmissionState> = _profileSubmission.asStateFlow()

    private val _calibrationSubmission = MutableStateFlow<SubmissionState>(SubmissionState.Idle)
    val calibrationSubmission: StateFlow<SubmissionState> = _calibrationSubmission.asStateFlow()

    private var savedProfileId: Long = -1L

    fun setTargetProfile(profileId: Long) {
        savedProfileId = profileId
    }

    init {
        viewModelScope.launch {
            val user = userDao.getLoggedInUser() ?: return@launch
            val parts = user.name.trim().split(' ', limit = 2)
            _profile.value = _profile.value.copy(
                firstName = parts.firstOrNull().orEmpty(),
                lastName  = parts.getOrNull(1).orEmpty()
            )
        }
    }

    fun setFirstName(v: String)   { _profile.value = _profile.value.copy(firstName = v) }
    fun setLastName(v: String)    { _profile.value = _profile.value.copy(lastName = v) }
    fun setBodyweight(v: String)  { _profile.value = _profile.value.copy(bodyweightKg = v.filter { it.isDigit() || it == '.' }) }
    fun setAge(v: String)         { _profile.value = _profile.value.copy(ageYears = v.filter { it.isDigit() }) }
    fun setSex(v: Sex)            { _profile.value = _profile.value.copy(sex = v) }

    fun clearProfileSubmissionError() {
        if (_profileSubmission.value !is SubmissionState.Submitting) {
            _profileSubmission.value = SubmissionState.Idle
        }
    }

    fun clearCalibrationSubmissionError() {
        if (_calibrationSubmission.value !is SubmissionState.Submitting) {
            _calibrationSubmission.value = SubmissionState.Idle
        }
    }

    fun saveProfile(onSaved: () -> Unit) {
        val draft = _profile.value
        if (!draft.isValid) return
        viewModelScope.launch {
            _profileSubmission.value = SubmissionState.Submitting
            val result = athleteProfileRepository.createProfile(
                firstName = draft.firstName.trim(),
                lastName = draft.lastName.trim(),
                bodyweightKg = draft.bodyweightKg.toDouble(),
                ageYears = draft.ageYears.toInt(),
                sex = draft.sex
            )
            when (result) {
                is AthleteProfileResult.Success -> {
                    savedProfileId = result.profile.id
                    _profileSubmission.value = SubmissionState.Idle
                    onSaved()
                }
                is AthleteProfileResult.ValidationError ->
                    _profileSubmission.value = SubmissionState.FieldErrors(result.errors)
                AthleteProfileResult.NotFound ->
                    _profileSubmission.value = SubmissionState.Error("Recurso no encontrado en el servidor.")
                AthleteProfileResult.Forbidden ->
                    _profileSubmission.value = SubmissionState.Error("No tenés permisos para crear un perfil de atleta.")
                AthleteProfileResult.Unauthorized ->
                    _profileSubmission.value = SubmissionState.Error("Tu sesión expiró. Volvé a iniciar sesión.")
                AthleteProfileResult.Throttled ->
                    _profileSubmission.value = SubmissionState.Error("Demasiados intentos. Esperá un momento.")
                is AthleteProfileResult.NetworkError ->
                    _profileSubmission.value = SubmissionState.NetworkError
                is AthleteProfileResult.ServerError ->
                    _profileSubmission.value = SubmissionState.Error("Error del servidor (${result.code}).")
            }
        }
    }

    fun startCapture() {
        _mvc.value = initialMvcState()
        runCurrentMeasurement()
    }

    fun repeatCurrent() {
        val s = _mvc.value
        _mvc.value = s.copy(
            measurements = s.measurements.toMutableList().also {
                it[s.currentIndex] = it[s.currentIndex].copy(capturedPct = null)
            },
            phase     = CapturePhase.PREPARE,
            livePct   = 0f,
            countdown = 3
        )
        runCurrentMeasurement()
    }

    fun next() {
        val s = _mvc.value
        if (s.currentIndex + 1 < s.totalSteps) {
            _mvc.value = s.copy(
                currentIndex = s.currentIndex + 1,
                phase        = CapturePhase.PREPARE,
                livePct      = 0f,
                countdown    = 3
            )
            runCurrentMeasurement()
        } else {
            _mvc.value = s.copy(finished = true)
        }
    }

    fun finalizeCalibration(onDone: () -> Unit) {
        viewModelScope.launch {
            _calibrationSubmission.value = SubmissionState.Submitting

            val targetProfileId = ensureProfileId()
            if (targetProfileId == null) {
                _calibrationSubmission.value =
                    SubmissionState.Error("No encontramos tu perfil. Reiniciá el flujo.")
                return@launch
            }

            val captured = _mvc.value.measurements.mapNotNull { m ->
                val v = m.capturedPct ?: return@mapNotNull null
                MvcCalibration(
                    athleteProfileId = targetProfileId,
                    muscle = m.muscle,
                    side = m.side,
                    mvcValue = v
                )
            }

            if (captured.isEmpty()) {
                _calibrationSubmission.value =
                    SubmissionState.Error("No hay calibraciones para guardar.")
                return@launch
            }

            when (val result = athleteProfileRepository.calibrate(captured)) {
                is MvcCalibrationResult.Success -> {
                    _calibrationSubmission.value = SubmissionState.Idle
                    onDone()
                }
                is MvcCalibrationResult.ValidationError ->
                    _calibrationSubmission.value = SubmissionState.FieldErrors(result.errors)
                MvcCalibrationResult.Forbidden ->
                    _calibrationSubmission.value = SubmissionState.Error("No tenés permisos para calibrar.")
                MvcCalibrationResult.Unauthorized ->
                    _calibrationSubmission.value = SubmissionState.Error("Tu sesión expiró. Volvé a iniciar sesión.")
                MvcCalibrationResult.Throttled ->
                    _calibrationSubmission.value = SubmissionState.Error("Demasiados intentos. Esperá un momento.")
                is MvcCalibrationResult.NetworkError ->
                    _calibrationSubmission.value = SubmissionState.NetworkError
                is MvcCalibrationResult.ServerError ->
                    _calibrationSubmission.value = SubmissionState.Error("Error del servidor (${result.code}).")
            }
        }
    }

    fun skipCalibration(onDone: () -> Unit) {
        onDone()
    }

    private suspend fun ensureProfileId(): Long? {
        if (savedProfileId != -1L) return savedProfileId
        val user = userDao.getLoggedInUser() ?: return null

        athleteProfileDao.getByUserId(user.id)?.let { return it.id.also { id -> savedProfileId = id } }

        // Cache miss — pull from backend before giving up
        athleteProfileRepository.fetchProfile()
        return athleteProfileDao.getByUserId(user.id)?.id?.also { savedProfileId = it }
    }

    private fun initialMvcState(): MvcCaptureUiState {
        val sequence = buildList {
            for (muscle in listOf(
                Muscle.VASTUS_LATERALIS, Muscle.VASTUS_MEDIALIS,
                Muscle.GLUTEUS_MAXIMUS,  Muscle.ERECTOR_SPINAE, Muscle.BICEPS_FEMORIS
            )) {
                add(MvcMeasurement(muscle, MuscleSide.LEFT))
                add(MvcMeasurement(muscle, MuscleSide.RIGHT))
            }
        }
        return MvcCaptureUiState(
            measurements = sequence,
            currentIndex = 0,
            phase        = CapturePhase.PREPARE,
            livePct      = 0f,
            countdown    = 3,
            finished     = false
        )
    }

    private fun runCurrentMeasurement() {
        viewModelScope.launch {
            for (i in 3 downTo 1) {
                _mvc.value = _mvc.value.copy(phase = CapturePhase.PREPARE, countdown = i, livePct = 0f)
                delay(800L)
            }

            val s = _mvc.value
            val target = simulator.captureMvc(s.current.muscle, s.current.side)
            _mvc.value = s.copy(phase = CapturePhase.CONTRACT, countdown = 3, livePct = 0f)

            val rampSteps = 30
            for (step in 1..rampSteps) {
                val progress = step.toFloat() / rampSteps
                val live = (target * (0.6f + 0.4f * progress)).coerceAtMost(target)
                val seconds = (3 - (step * 3 / rampSteps)).coerceAtLeast(0)
                _mvc.value = _mvc.value.copy(livePct = live, countdown = seconds)
                delay(80L)
            }

            _mvc.value = _mvc.value.copy(
                phase   = CapturePhase.DONE,
                livePct = target,
                measurements = _mvc.value.measurements.toMutableList().also {
                    it[_mvc.value.currentIndex] = it[_mvc.value.currentIndex].copy(capturedPct = target)
                }
            )
        }
    }
}
