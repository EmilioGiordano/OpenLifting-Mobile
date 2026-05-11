package com.openlifting.data.repository

import com.openlifting.data.local.dao.AthleteProfileDao
import com.openlifting.data.local.dao.UserDao
import com.openlifting.data.local.entity.MvcCalibrationEntity
import com.openlifting.data.mapper.toApiInput
import com.openlifting.data.mapper.toDomain
import com.openlifting.data.mapper.toEntity
import com.openlifting.data.local.dao.SessionDao
import com.openlifting.data.local.entity.TrainingSessionEntity
import com.openlifting.data.mapper.toEntity
import com.openlifting.data.remote.api.VortexAthleteApi
import com.openlifting.data.remote.dto.AthleteProfileDto
import com.openlifting.data.remote.dto.ClaimRequest
import com.openlifting.data.remote.dto.CreateAthleteProfileRequest
import com.openlifting.data.remote.dto.MvcCalibrationDto
import com.openlifting.data.remote.dto.StoreMvcCalibrationsRequest
import com.openlifting.data.remote.dto.TrainingSessionDto
import com.openlifting.data.remote.dto.UpdateAthleteProfileRequest
import com.openlifting.data.remote.dto.ValidationErrorResponse
import com.openlifting.domain.model.AthleteProfile
import com.openlifting.domain.model.AthleteProfileResult
import com.openlifting.domain.model.ClaimRedeemResult
import com.openlifting.domain.model.MvcCalibration
import com.openlifting.domain.model.MvcCalibrationResult
import com.openlifting.domain.model.Sex
import com.openlifting.domain.repository.AthleteProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject

class AthleteProfileRepositoryImpl @Inject constructor(
    private val api: VortexAthleteApi,
    private val athleteProfileDao: AthleteProfileDao,
    private val userDao: UserDao,
    private val sessionDao: SessionDao,
    private val json: Json
) : AthleteProfileRepository {

    override suspend fun fetchProfile(): AthleteProfileResult = runHttp {
        api.getProfile()
    }.foldProfile { dto -> persistProfile(dto) }

    override suspend fun createProfile(
        firstName: String,
        lastName: String,
        bodyweightKg: Double,
        ageYears: Int,
        sex: Sex
    ): AthleteProfileResult = runHttp {
        api.createProfile(
            CreateAthleteProfileRequest(
                firstName = firstName,
                lastName = lastName,
                bodyweightKg = bodyweightKg,
                ageYears = ageYears,
                sex = sex.name
            )
        )
    }.foldProfile { dto -> persistProfile(dto) }

    override suspend fun updateProfile(
        firstName: String?,
        lastName: String?,
        bodyweightKg: Double?,
        ageYears: Int?,
        sex: Sex?
    ): AthleteProfileResult = runHttp {
        api.updateProfile(
            UpdateAthleteProfileRequest(
                firstName = firstName,
                lastName = lastName,
                bodyweightKg = bodyweightKg,
                ageYears = ageYears,
                sex = sex?.name
            )
        )
    }.foldProfile { dto -> persistProfile(dto) }

    override suspend fun calibrate(calibrations: List<MvcCalibration>): MvcCalibrationResult {
        val request = StoreMvcCalibrationsRequest(calibrations.map { it.toApiInput() })
        return when (val outcome = runHttp { api.calibrate(request) }) {
            is HttpOutcome.Ok -> handleCalibrateResponse(outcome.response)
            is HttpOutcome.Network -> MvcCalibrationResult.NetworkError(outcome.cause)
            is HttpOutcome.Unexpected -> MvcCalibrationResult.NetworkError(outcome.cause)
        }
    }

    override fun observeCachedProfile(userId: Long): Flow<AthleteProfile?> =
        athleteProfileDao.observeByUserId(userId).map { it?.toDomain() }

    override suspend fun getCachedProfile(userId: Long): AthleteProfile? =
        athleteProfileDao.getByUserId(userId)?.toDomain()

    override suspend fun claimSession(code: String): ClaimRedeemResult {
        val response = try {
            api.claim(ClaimRequest(code = code))
        } catch (e: IOException) { return ClaimRedeemResult.NetworkError(e) }
          catch (e: Exception)   { return ClaimRedeemResult.NetworkError(e) }

        if (!response.isSuccessful) return when (response.code()) {
            403  -> ClaimRedeemResult.Forbidden
            404  -> ClaimRedeemResult.NotFound
            410  -> ClaimRedeemResult.ExpiredOrUsed
            422  -> parseClaimValidation(response)
            429  -> ClaimRedeemResult.Throttled
            else -> ClaimRedeemResult.ServerError(response.code())
        }
        val sessionDto = response.body() ?: return ClaimRedeemResult.ServerError(response.code())

        // Mirror the transferred session into Room. Profile + calibrations are not pulled
        // here — the caller (ViewModel) re-fetches them via the existing flows after claim.
        val user = userDao.getLoggedInUser() ?: return ClaimRedeemResult.ServerError(0)
        val existingLocal = sessionDao.getByServerId(sessionDto.id)
        val entity = sessionDto.toEntity(
            athleteUserId    = user.id,
            instructorUserId = existingLocal?.instructorUserId,
            existingLocalId  = existingLocal?.localId ?: 0
        )
        val sessionLocalId = if (existingLocal == null) {
            sessionDao.insert(entity)
        } else {
            sessionDao.update(entity); existingLocal.localId
        }

        // Refresh the athlete profile from backend so any newly copied first_name / bodyweight
        // shows up in the UI. Failure is non-fatal — the claim itself succeeded.
        runCatching { fetchProfile() }

        return ClaimRedeemResult.Success(sessionLocalId = sessionLocalId)
    }

    private fun parseClaimValidation(response: retrofit2.Response<TrainingSessionDto>): ClaimRedeemResult {
        val raw = response.errorBody()?.string().orEmpty()
        val parsed = try {
            json.decodeFromString(ValidationErrorResponse.serializer(), raw)
        } catch (_: Exception) {
            return ClaimRedeemResult.ValidationError(mapOf("_" to listOf("Validación falló.")))
        }
        val errors = parsed.errors.ifEmpty { mapOf("_" to listOf(parsed.message)) }
        return ClaimRedeemResult.ValidationError(errors)
    }

    // ── persistence ───────────────────────────────────────────────────────────

    private suspend fun persistProfile(dto: AthleteProfileDto): AthleteProfile {
        val userId = userDao.getLoggedInUser()?.id
            ?: error("Cannot persist athlete profile without a logged-in user")
        val existing = athleteProfileDao.getByUserId(userId)
        val entity = dto.toEntity(userId = userId, existingLocalId = existing?.id ?: 0)
        if (existing == null) {
            athleteProfileDao.insert(entity)
        } else {
            athleteProfileDao.update(entity)
        }
        return dto.toDomain(userId = userId).copy(id = entity.id.takeIf { it != 0L } ?: existing?.id ?: 0)
    }

    private suspend fun persistCalibrations(
        dtos: List<MvcCalibrationDto>
    ): List<MvcCalibration> {
        val userId = userDao.getLoggedInUser()?.id
            ?: error("Cannot persist calibrations without a logged-in user")
        val profile = athleteProfileDao.getByUserId(userId)
            ?: error("Cannot persist calibrations without a local profile row")

        val entities = dtos.map { it.toEntity(athleteProfileLocalId = profile.id) }
        athleteProfileDao.deleteCalibrations(profile.id)
        athleteProfileDao.insertCalibrations(entities)
        athleteProfileDao.markCalibrated(profile.id, System.currentTimeMillis())
        return dtos.map { it.toDomain(athleteProfileLocalId = profile.id) }
    }

    // ── HTTP helpers ──────────────────────────────────────────────────────────

    private inline fun <T> runHttp(call: () -> Response<T>): HttpOutcome<T> = try {
        HttpOutcome.Ok(call())
    } catch (e: IOException) {
        HttpOutcome.Network(e)
    } catch (e: Exception) {
        HttpOutcome.Unexpected(e)
    }

    private suspend inline fun HttpOutcome<AthleteProfileDto>.foldProfile(
        crossinline onSuccess: suspend (AthleteProfileDto) -> AthleteProfile
    ): AthleteProfileResult = when (this) {
        is HttpOutcome.Ok         -> handleProfileResponse(response, onSuccess)
        is HttpOutcome.Network    -> AthleteProfileResult.NetworkError(cause)
        is HttpOutcome.Unexpected -> AthleteProfileResult.NetworkError(cause)
    }

    private suspend inline fun handleProfileResponse(
        response: Response<AthleteProfileDto>,
        crossinline onSuccess: suspend (AthleteProfileDto) -> AthleteProfile
    ): AthleteProfileResult {
        if (response.isSuccessful) {
            val body = response.body() ?: return AthleteProfileResult.ServerError(response.code())
            return AthleteProfileResult.Success(onSuccess(body))
        }
        return when (response.code()) {
            404 -> AthleteProfileResult.NotFound
            403 -> AthleteProfileResult.Forbidden
            401 -> AthleteProfileResult.Unauthorized
            422 -> parseValidation(response, ::AthleteProfileValidationError)
            429 -> AthleteProfileResult.Throttled
            else -> AthleteProfileResult.ServerError(response.code())
        }
    }

    private suspend fun handleCalibrateResponse(
        response: Response<List<MvcCalibrationDto>>
    ): MvcCalibrationResult {
        if (response.isSuccessful) {
            val body = response.body() ?: return MvcCalibrationResult.ServerError(response.code())
            return MvcCalibrationResult.Success(persistCalibrations(body))
        }
        return when (response.code()) {
            403 -> MvcCalibrationResult.Forbidden
            401 -> MvcCalibrationResult.Unauthorized
            422 -> parseValidation(response, ::MvcCalibrationValidationError)
            429 -> MvcCalibrationResult.Throttled
            else -> MvcCalibrationResult.ServerError(response.code())
        }
    }

    private fun <R> parseValidation(
        response: Response<*>,
        wrap: (Map<String, List<String>>) -> R
    ): R {
        val raw = response.errorBody()?.string().orEmpty()
        val parsed = try {
            json.decodeFromString(ValidationErrorResponse.serializer(), raw)
        } catch (_: Exception) {
            return wrap(mapOf("_" to listOf("Validation failed.")))
        }
        val errors = parsed.errors.ifEmpty { mapOf("_" to listOf(parsed.message)) }
        return wrap(errors)
    }

    private fun AthleteProfileValidationError(errors: Map<String, List<String>>) =
        AthleteProfileResult.ValidationError(errors)

    private fun MvcCalibrationValidationError(errors: Map<String, List<String>>) =
        MvcCalibrationResult.ValidationError(errors)

    private sealed interface HttpOutcome<T> {
        data class Ok<T>(val response: Response<T>) : HttpOutcome<T>
        data class Network<T>(val cause: Throwable) : HttpOutcome<T>
        data class Unexpected<T>(val cause: Throwable) : HttpOutcome<T>
    }
}

