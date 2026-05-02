package com.openlifting.domain.repository

import com.openlifting.domain.model.AthleteProfile
import com.openlifting.domain.model.Sex
import kotlinx.coroutines.flow.Flow

/**
 * Backend-mediated relationship between an instructor (coach) and the athletes they manage.
 *
 * IMPORTANT: this is a stand-in for what will be a backend-served resource. Two distinct
 * users live on two distinct devices in production — the source of truth for the
 * coach <-> athlete link is the Postgres backend, not local Room. The current local
 * implementation uses Room as a single-device demo facade and will be replaced with
 * a Retrofit-based impl when the Laravel backend lands.
 *
 * The contract here is what the backend will eventually expose; ViewModels should depend on
 * this interface, not on DAOs directly, for any cross-user concept.
 */
interface CoachRepository {

    /** Athletes (registered + guests) currently managed by the instructor. */
    fun observeManagedAthletes(instructorUserId: Long): Flow<List<ManagedAthlete>>

    /**
     * Creates a guest athlete fully owned by the instructor: a stub user, an athlete profile
     * marked as guest of [instructorUserId], and the bookkeeping link in the join table.
     * Returns the new athlete profile id so the caller can navigate to calibration.
     */
    suspend fun createGuest(
        instructorUserId: Long,
        firstName: String,
        lastName: String,
        bodyweightKg: Float,
        ageYears: Int,
        sex: Sex
    ): GuestCreated

    /** Reads a single managed athlete by their athlete profile id. */
    suspend fun getManagedAthlete(athleteProfileId: Long): ManagedAthlete?
}

/**
 * View model of an athlete from the instructor's perspective. Combines the persisted
 * [AthleteProfile] with derived flags useful for the list UI.
 */
data class ManagedAthlete(
    val profile: AthleteProfile,
    /** True when this athlete was created in guest mode and has not been claimed yet. */
    val isGuest: Boolean
) {
    val athleteUserId: Long get() = profile.userId
}

data class GuestCreated(
    val athleteUserId: Long,
    val athleteProfileId: Long
)
