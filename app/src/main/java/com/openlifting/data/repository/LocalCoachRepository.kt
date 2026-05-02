package com.openlifting.data.repository

import com.openlifting.data.local.dao.AthleteProfileDao
import com.openlifting.data.local.dao.InstructorAthleteDao
import com.openlifting.data.local.dao.UserDao
import com.openlifting.data.local.entity.AthleteProfileEntity
import com.openlifting.data.local.entity.InstructorAthleteEntity
import com.openlifting.data.local.entity.UserEntity
import com.openlifting.data.mapper.toDomain
import com.openlifting.domain.model.Sex
import com.openlifting.domain.repository.CoachRepository
import com.openlifting.domain.repository.GuestCreated
import com.openlifting.domain.repository.ManagedAthlete
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * STAND-IN implementation of [CoachRepository] for the offline demo.
 *
 * In production this contract will be served by a Retrofit-backed implementation that talks
 * to the Laravel backend. For the mobile-course delivery the demo runs on a single device,
 * so we use Room as a local mock of what the backend would store. None of this is "real"
 * offline-first — the cross-user relationship between an instructor and an athlete is
 * inherently a multi-device concern.
 *
 * Specifically:
 *  - Guest creation here writes a stub UserEntity + AthleteProfileEntity + an
 *    InstructorAthleteEntity link, all locally. With the real backend, this would be a
 *    POST /coach/guests that returns a guest user id which the device caches.
 *  - observeManagedAthletes reads from local Room. With the backend it would be a GET
 *    /coach/{id}/athletes call (cached locally for offline UI, refreshed on connectivity).
 */
@Singleton
class LocalCoachRepository @Inject constructor(
    private val userDao: UserDao,
    private val athleteProfileDao: AthleteProfileDao,
    private val instructorAthleteDao: InstructorAthleteDao
) : CoachRepository {

    override fun observeManagedAthletes(instructorUserId: Long): Flow<List<ManagedAthlete>> =
        athleteProfileDao.observeGuestsForInstructor(instructorUserId).map { profiles ->
            profiles.map { it.toManagedAthlete() }
        }

    override suspend fun createGuest(
        instructorUserId: Long,
        firstName: String,
        lastName: String,
        bodyweightKg: Float,
        ageYears: Int,
        sex: Sex
    ): GuestCreated {
        // 1. Stub user — synthetic email so it can never be used to log in
        val stubUserId = System.currentTimeMillis()
        val stubUser = UserEntity(
            id        = stubUserId,
            email     = "guest.$stubUserId@local.openlifting",
            name      = "$firstName $lastName".trim(),
            role      = "ATHLETE",
            authToken = null,
            serverId  = null
        )
        userDao.insert(stubUser)

        // 2. Athlete profile owned by the instructor (guest mode)
        val profile = AthleteProfileEntity(
            id                   = 0L,
            userId               = stubUserId,
            firstName            = firstName.trim(),
            lastName             = lastName.trim(),
            bodyweightKg         = bodyweightKg,
            ageYears             = ageYears,
            sex                  = sex.name,
            calibratedAt         = null,
            guestOfInstructorId  = instructorUserId
        )
        val profileId = athleteProfileDao.insert(profile)

        // 3. Bookkeeping link in the join table
        instructorAthleteDao.insert(
            InstructorAthleteEntity(
                instructorUserId = instructorUserId,
                athleteUserId    = stubUserId,
                linkType         = InstructorAthleteEntity.LINK_TYPE_GUEST
            )
        )

        return GuestCreated(athleteUserId = stubUserId, athleteProfileId = profileId)
    }

    override suspend fun getManagedAthlete(athleteProfileId: Long): ManagedAthlete? =
        athleteProfileDao.getById(athleteProfileId)?.toManagedAthlete()

    private fun AthleteProfileEntity.toManagedAthlete(): ManagedAthlete = ManagedAthlete(
        profile = this.toDomainProfile(),
        isGuest = guestOfInstructorId != null
    )

    private fun AthleteProfileEntity.toDomainProfile() =
        com.openlifting.domain.model.AthleteProfile(
            id           = id,
            userId       = userId,
            firstName    = firstName,
            lastName     = lastName,
            bodyweightKg = bodyweightKg,
            ageYears     = ageYears,
            sex          = runCatching { Sex.valueOf(sex) }.getOrDefault(Sex.OTHER),
            calibratedAt = calibratedAt
        )
}
