package com.openlifting.data.repository

import com.openlifting.data.local.dao.AthleteProfileDao
import com.openlifting.data.local.dao.InstructorAthleteDao
import com.openlifting.data.local.dao.SessionDao
import com.openlifting.data.local.dao.UserDao
import com.openlifting.data.local.entity.AthleteProfileEntity
import com.openlifting.data.local.entity.InstructorAthleteEntity
import com.openlifting.data.local.entity.MvcCalibrationEntity
import com.openlifting.data.local.entity.UserEntity
import com.openlifting.data.mapper.toApiInput
import com.openlifting.data.remote.api.VortexInstructorApi
import com.openlifting.data.remote.dto.ClaimCodeResponse
import com.openlifting.data.remote.dto.CreateGuestRequest
import com.openlifting.data.remote.dto.GuestProfileDto
import com.openlifting.data.remote.dto.MvcCalibrationDto
import com.openlifting.data.remote.dto.StoreMvcCalibrationsRequest
import com.openlifting.data.remote.dto.ValidationErrorResponse
import com.openlifting.domain.model.AthleteProfile
import com.openlifting.domain.model.ClaimCodeResult
import com.openlifting.domain.model.GuestProfileResult
import com.openlifting.domain.model.MvcCalibration
import com.openlifting.domain.model.MvcCalibrationResult
import com.openlifting.domain.model.Sex
import com.openlifting.domain.repository.CoachRepository
import com.openlifting.domain.repository.ManagedAthlete
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.io.IOException
import java.time.Instant
import java.time.format.DateTimeParseException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VortexCoachRepository @Inject constructor(
    private val instructorApi: VortexInstructorApi,
    private val userDao: UserDao,
    private val athleteProfileDao: AthleteProfileDao,
    private val instructorAthleteDao: InstructorAthleteDao,
    private val sessionDao: SessionDao,
    private val json: Json
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
    ): GuestProfileResult {
        val request = CreateGuestRequest(
            firstName    = firstName,
            lastName     = lastName,
            bodyweightKg = bodyweightKg.toDouble(),
            ageYears     = ageYears,
            sex          = sex.name
        )
        val response = try {
            instructorApi.createGuest(request)
        } catch (e: IOException)   { return GuestProfileResult.NetworkError(e) }
          catch (e: Exception)     { return GuestProfileResult.NetworkError(e) }

        if (!response.isSuccessful) return mapErrorToGuestResult(response)
        val dto = response.body() ?: return GuestProfileResult.ServerError(response.code())

        // Mirror locally so the existing instructor UI (which navigates by local profile id and
        // stub user id) keeps working without a wider refactor.
        val stubUserId = System.currentTimeMillis()
        userDao.insert(
            UserEntity(
                id        = stubUserId,
                email     = "guest.$stubUserId@local.openlifting",
                name      = "${dto.firstName} ${dto.lastName}".trim(),
                role      = "ATHLETE",
                authToken = null,
                serverId  = null
            )
        )
        val profileLocalId = athleteProfileDao.insert(
            AthleteProfileEntity(
                userId               = stubUserId,
                firstName            = dto.firstName,
                lastName             = dto.lastName,
                bodyweightKg         = dto.bodyweightKg.toFloat(),
                ageYears             = dto.ageYears,
                sex                  = dto.sex,
                calibratedAt         = dto.calibratedAt?.toEpochMillisOrNull(),
                guestOfInstructorId  = instructorUserId,
                guestProfileServerId = dto.id
            )
        )
        instructorAthleteDao.insert(
            InstructorAthleteEntity(
                instructorUserId = instructorUserId,
                athleteUserId    = stubUserId,
                linkType         = InstructorAthleteEntity.LINK_TYPE_GUEST
            )
        )

        return GuestProfileResult.Success(
            com.openlifting.domain.model.GuestProfile(
                id            = profileLocalId,                  // local id so the UI can navigate to detail
                firstName     = dto.firstName,
                lastName      = dto.lastName,
                bodyweightKg  = dto.bodyweightKg.toFloat(),
                ageYears      = dto.ageYears,
                sex           = runCatching { Sex.valueOf(dto.sex) }.getOrDefault(Sex.MALE),
                calibratedAt  = dto.calibratedAt?.toEpochMillisOrNull(),
                claimed       = dto.claimed,
                claimedAt     = dto.claimedAt?.toEpochMillisOrNull(),
                createdAt     = dto.createdAt.toEpochMillisOrFallback()
            )
        )
    }

    override suspend fun calibrateGuest(
        athleteProfileLocalId: Long,
        calibrations: List<MvcCalibration>
    ): MvcCalibrationResult {
        val profile = athleteProfileDao.getById(athleteProfileLocalId)
            ?: return MvcCalibrationResult.ServerError(0)
        val guestServerId = profile.guestProfileServerId
            ?: return MvcCalibrationResult.ValidationError(
                mapOf("guest_profile_id" to listOf("Este atleta no es un invitado del backend."))
            )

        val request = StoreMvcCalibrationsRequest(calibrations.map { it.toApiInput() })
        val response = try {
            instructorApi.calibrateGuest(guestServerId, request)
        } catch (e: IOException) { return MvcCalibrationResult.NetworkError(e) }
          catch (e: Exception)   { return MvcCalibrationResult.NetworkError(e) }

        if (!response.isSuccessful) return mapErrorToCalibrationResult(response)
        val body = response.body() ?: return MvcCalibrationResult.ServerError(response.code())

        // Mirror locally so the existing UI shows the calibrated_at and bars correctly.
        val entities = body.map { it.toLocalEntity(profile.id) }
        athleteProfileDao.deleteCalibrations(profile.id)
        athleteProfileDao.insertCalibrations(entities)
        athleteProfileDao.markCalibrated(profile.id, System.currentTimeMillis())

        return MvcCalibrationResult.Success(
            body.map {
                MvcCalibration(
                    athleteProfileId = profile.id,
                    muscle = com.openlifting.domain.model.Muscle.valueOf(it.muscle),
                    side   = com.openlifting.domain.model.MuscleSide.valueOf(it.side),
                    mvcValue = it.mvcValue.toFloat()
                )
            }
        )
    }

    override suspend fun generateClaimCode(sessionLocalId: Long): ClaimCodeResult {
        val session = sessionDao.getById(sessionLocalId)
            ?: return ClaimCodeResult.NotFound
        val serverId = session.serverId
            ?: return ClaimCodeResult.NotFound

        val response = try {
            instructorApi.generateClaimCode(serverId)
        } catch (e: IOException) { return ClaimCodeResult.NetworkError(e) }
          catch (e: Exception)   { return ClaimCodeResult.NetworkError(e) }

        if (!response.isSuccessful) return when (response.code()) {
            401  -> ClaimCodeResult.Unauthorized
            403  -> ClaimCodeResult.Forbidden
            404  -> ClaimCodeResult.NotFound
            else -> ClaimCodeResult.ServerError(response.code())
        }
        val body = response.body() ?: return ClaimCodeResult.ServerError(response.code())
        return ClaimCodeResult.Success(
            code             = body.code,
            sessionId        = body.sessionId,
            expiresAtEpochMs = body.expiresAt.toEpochMillisOrFallback()
        )
    }

    override suspend fun getManagedAthlete(athleteProfileId: Long): ManagedAthlete? =
        athleteProfileDao.getById(athleteProfileId)?.toManagedAthlete()

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun AthleteProfileEntity.toManagedAthlete(): ManagedAthlete = ManagedAthlete(
        profile = AthleteProfile(
            id           = id,
            userId       = userId,
            firstName    = firstName,
            lastName     = lastName,
            bodyweightKg = bodyweightKg,
            ageYears     = ageYears,
            sex          = runCatching { Sex.valueOf(sex) }.getOrDefault(Sex.OTHER),
            calibratedAt = calibratedAt
        ),
        isGuest = guestOfInstructorId != null
    )

    private fun MvcCalibrationDto.toLocalEntity(athleteProfileId: Long): MvcCalibrationEntity =
        MvcCalibrationEntity(
            athleteProfileId = athleteProfileId,
            muscle = muscle,
            side = side,
            mvcValue = mvcValue.toFloat()
        )

    private fun mapErrorToGuestResult(response: Response<GuestProfileDto>): GuestProfileResult =
        when (response.code()) {
            401  -> GuestProfileResult.Unauthorized
            403  -> GuestProfileResult.Forbidden
            404  -> GuestProfileResult.NotFound
            422  -> parseValidation(response, ::guestValidation)
            429  -> GuestProfileResult.Throttled
            else -> GuestProfileResult.ServerError(response.code())
        }

    private fun mapErrorToCalibrationResult(response: Response<List<MvcCalibrationDto>>): MvcCalibrationResult =
        when (response.code()) {
            401  -> MvcCalibrationResult.Unauthorized
            403  -> MvcCalibrationResult.Forbidden
            422  -> parseValidation(response, ::calibrationValidation)
            429  -> MvcCalibrationResult.Throttled
            else -> MvcCalibrationResult.ServerError(response.code())
        }

    private fun guestValidation(errors: Map<String, List<String>>) =
        GuestProfileResult.ValidationError(errors)

    private fun calibrationValidation(errors: Map<String, List<String>>) =
        MvcCalibrationResult.ValidationError(errors)

    private fun <R> parseValidation(
        response: Response<*>,
        wrap: (Map<String, List<String>>) -> R
    ): R {
        val raw = response.errorBody()?.string().orEmpty()
        val parsed = try {
            json.decodeFromString(ValidationErrorResponse.serializer(), raw)
        } catch (_: Exception) {
            return wrap(mapOf("_" to listOf("Validación falló.")))
        }
        val errors = parsed.errors.ifEmpty { mapOf("_" to listOf(parsed.message)) }
        return wrap(errors)
    }

    private fun String.toEpochMillisOrNull(): Long? = try {
        Instant.parse(this).toEpochMilli()
    } catch (_: DateTimeParseException) {
        null
    }

    private fun String.toEpochMillisOrFallback(): Long =
        toEpochMillisOrNull() ?: System.currentTimeMillis()
}
