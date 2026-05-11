package com.openlifting.domain.repository

import com.openlifting.domain.model.AthleteProfile
import com.openlifting.domain.model.ClaimCodeResult
import com.openlifting.domain.model.GuestProfileResult
import com.openlifting.domain.model.MvcCalibration
import com.openlifting.domain.model.MvcCalibrationResult
import com.openlifting.domain.model.Sex
import kotlinx.coroutines.flow.Flow

/**
 * Instructor-side operations. Guest profiles, calibrations and sessions live on Vortex (Postgres
 * is the source of truth); this repo mirrors the data into Room so the existing instructor
 * screens (home, athlete detail, session flow) keep working without rewrites.
 */
interface CoachRepository {

    /** Athletes (registered + guests) currently managed by the instructor. Cached in Room. */
    fun observeManagedAthletes(instructorUserId: Long): Flow<List<ManagedAthlete>>

    /**
     * Creates a guest on Vortex and mirrors the new profile + stub user locally so the
     * existing UI (which navigates by local profile id and stub user id) keeps working.
     * Returns the new local profile + user ids alongside the backend `guest_profile_id`.
     */
    suspend fun createGuest(
        instructorUserId: Long,
        firstName: String,
        lastName: String,
        bodyweightKg: Float,
        ageYears: Int,
        sex: Sex
    ): GuestProfileResult

    /** Calibrates a guest on Vortex (`POST /api/instructor/guests/{id}/mvc`) and mirrors locally. */
    suspend fun calibrateGuest(
        athleteProfileLocalId: Long,
        calibrations: List<MvcCalibration>
    ): MvcCalibrationResult

    /** Generates (or rotates) a claim code for the given training session local id. */
    suspend fun generateClaimCode(sessionLocalId: Long): ClaimCodeResult

    /** Reads a single managed athlete by their athlete profile id. */
    suspend fun getManagedAthlete(athleteProfileId: Long): ManagedAthlete?
}

data class ManagedAthlete(
    val profile: AthleteProfile,
    /** True when this athlete was created in guest mode and has not been claimed yet. */
    val isGuest: Boolean
) {
    val athleteUserId: Long get() = profile.userId
}
