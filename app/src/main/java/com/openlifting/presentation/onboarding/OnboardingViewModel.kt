package com.openlifting.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openlifting.data.local.dao.AthleteProfileDao
import com.openlifting.data.local.dao.UserDao
import com.openlifting.data.local.entity.AthleteProfileEntity
import com.openlifting.data.local.entity.MvcCalibrationEntity
import com.openlifting.data.simulator.Esp32Simulator
import com.openlifting.domain.model.Muscle
import com.openlifting.domain.model.MuscleSide
import com.openlifting.domain.model.Sex
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Owns the entire onboarding state machine for an athlete:
 *  - draft of AthleteProfile (name + bodyweight + age + sex)
 *  - 10-step MVC calibration (5 muscles x 2 sides)
 *
 * Each onboarding screen reads the slice of state it needs and calls one of the
 * lifecycle methods. The ViewModel persists once: AthleteProfile on submit, the 10
 * MvcCalibration rows + calibratedAt at the end of the capture flow.
 */

data class ProfileDraft(
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
    val livePct: Float,        // current live bar value (only meaningful in CONTRACT)
    val countdown: Int,        // 3-2-1 in PREPARE, seconds remaining in CONTRACT
    val finished: Boolean
) {
    val current: MvcMeasurement get() = measurements[currentIndex]
    val totalSteps: Int get() = measurements.size
    val stepLabel: String get() = "${currentIndex + 1} / $totalSteps"
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userDao: UserDao,
    private val athleteProfileDao: AthleteProfileDao,
    private val simulator: Esp32Simulator
) : ViewModel() {

    private val _profile = MutableStateFlow(ProfileDraft())
    val profile: StateFlow<ProfileDraft> = _profile.asStateFlow()

    private val _mvc = MutableStateFlow(initialMvcState())
    val mvc: StateFlow<MvcCaptureUiState> = _mvc.asStateFlow()

    /** Persisted profile id, available after [saveProfile] completes OR after [setTargetProfile]. */
    private var savedProfileId: Long = -1L

    /**
     * When set, calibration writes to this profile id (used by the instructor flow when
     * calibrating a guest athlete). Default -1L means "use the logged-in user's profile".
     */
    fun setTargetProfile(profileId: Long) {
        savedProfileId = profileId
    }

    init {
        // Prefill firstName / lastName from the logged-in User if possible
        viewModelScope.launch {
            val user = userDao.getLoggedInUser() ?: return@launch
            val parts = user.name.trim().split(' ', limit = 2)
            _profile.value = _profile.value.copy(
                firstName = parts.firstOrNull().orEmpty(),
                lastName  = parts.getOrNull(1).orEmpty()
            )
        }
    }

    // ── Profile draft setters ───────────────────────────────────────────────

    fun setFirstName(v: String)   { _profile.value = _profile.value.copy(firstName = v) }
    fun setLastName(v: String)    { _profile.value = _profile.value.copy(lastName = v) }
    fun setBodyweight(v: String)  { _profile.value = _profile.value.copy(bodyweightKg = v.filter { it.isDigit() || it == '.' }) }
    fun setAge(v: String)         { _profile.value = _profile.value.copy(ageYears = v.filter { it.isDigit() }) }
    fun setSex(v: Sex)            { _profile.value = _profile.value.copy(sex = v) }

    /** Persists the profile (without calibration) and returns through [onSaved]. */
    fun saveProfile(onSaved: () -> Unit) {
        val draft = _profile.value
        if (!draft.isValid) return
        viewModelScope.launch {
            val user = userDao.getLoggedInUser() ?: return@launch

            val existing = athleteProfileDao.getByUserId(user.id)
            val entity = AthleteProfileEntity(
                id            = existing?.id ?: 0L,
                userId        = user.id,
                firstName     = draft.firstName.trim(),
                lastName      = draft.lastName.trim(),
                bodyweightKg  = draft.bodyweightKg.toFloat(),
                ageYears      = draft.ageYears.toInt(),
                sex           = draft.sex.name,
                calibratedAt  = existing?.calibratedAt   // preserve existing calibration on edit
            )
            savedProfileId = if (existing == null) {
                athleteProfileDao.insert(entity)
            } else {
                athleteProfileDao.update(entity); existing.id
            }
            onSaved()
        }
    }

    // ── MVC capture flow ────────────────────────────────────────────────────

    /** Resets the capture state and starts the first measurement's PREPARE phase. */
    fun startCapture() {
        _mvc.value = initialMvcState()
        runCurrentMeasurement()
    }

    /** Re-runs the current measurement (PREPARE -> CONTRACT -> DONE again). */
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

    /** Advances to the next measurement, or marks the flow as finished. */
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

    /**
     * Persists the 10 captured MVC values and updates calibratedAt. After this returns,
     * subsequent sessions will use the captured values as the reference 100%.
     */
    fun finalizeCalibration(onDone: () -> Unit) {
        viewModelScope.launch {
            if (savedProfileId == -1L) {
                val user = userDao.getLoggedInUser() ?: return@launch
                savedProfileId = athleteProfileDao.getByUserId(user.id)?.id ?: return@launch
            }
            val captured = _mvc.value.measurements.mapNotNull { m ->
                val v = m.capturedPct ?: return@mapNotNull null
                MvcCalibrationEntity(
                    athleteProfileId = savedProfileId,
                    muscle           = m.muscle.name,
                    side             = m.side.name,
                    mvcValue         = v
                )
            }
            athleteProfileDao.deleteCalibrations(savedProfileId)  // overwrite previous
            athleteProfileDao.insertCalibrations(captured)
            athleteProfileDao.markCalibrated(savedProfileId, System.currentTimeMillis())
            onDone()
        }
    }

    /** Skip the calibration entirely (defaults will be used until the user calibrates). */
    fun skipCalibration(onDone: () -> Unit) {
        // We DO NOT mark calibratedAt — leaving the banner active so the user knows their
        // metrics are approximate.
        onDone()
    }

    // ── Internals ───────────────────────────────────────────────────────────

    private fun initialMvcState(): MvcCaptureUiState {
        val sequence = buildList {
            // VL → VM → GMax → ES → BF, each L then R
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
            // PREPARE: 3 -> 2 -> 1
            for (i in 3 downTo 1) {
                _mvc.value = _mvc.value.copy(phase = CapturePhase.PREPARE, countdown = i, livePct = 0f)
                delay(800L)
            }

            // CONTRACT: ramp the live bar up to a simulated peak over ~3s, hold briefly, settle
            val s = _mvc.value
            val target = simulator.captureMvc(s.current.muscle, s.current.side)
            _mvc.value = s.copy(phase = CapturePhase.CONTRACT, countdown = 3, livePct = 0f)

            val rampSteps = 30
            for (step in 1..rampSteps) {
                val progress = step.toFloat() / rampSteps
                val live = (target * (0.6f + 0.4f * progress)).coerceAtMost(target)  // ramp up
                val seconds = (3 - (step * 3 / rampSteps)).coerceAtLeast(0)
                _mvc.value = _mvc.value.copy(livePct = live, countdown = seconds)
                delay(80L)
            }

            // DONE: settle at peak, expose captured value, wait for user
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
