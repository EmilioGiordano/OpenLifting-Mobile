package com.openlifting.data.repository

import com.openlifting.data.local.dao.AthleteProfileDao
import com.openlifting.data.local.dao.UserDao
import com.openlifting.data.local.entity.AthleteProfileEntity
import com.openlifting.data.local.entity.UserEntity
import com.openlifting.data.remote.api.VortexAthleteApi
import com.openlifting.data.remote.dto.AthleteProfileDto
import com.openlifting.data.remote.dto.MvcCalibrationDto
import com.openlifting.domain.model.AthleteProfileResult
import com.openlifting.domain.model.Muscle
import com.openlifting.domain.model.MuscleSide
import com.openlifting.domain.model.MvcCalibration
import com.openlifting.domain.model.MvcCalibrationResult
import com.openlifting.domain.model.Sex
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class AthleteProfileRepositoryImplTest {

    private val api        = mockk<VortexAthleteApi>()
    private val dao        = mockk<AthleteProfileDao>(relaxed = true)
    private val userDao    = mockk<UserDao>()
    private val sessionDao = mockk<com.openlifting.data.local.dao.SessionDao>(relaxed = true)
    private val json       = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun build() = AthleteProfileRepositoryImpl(api, dao, userDao, sessionDao, json)

    private fun userEntity(id: Long = 7L) = UserEntity(
        id = id, email = "a@b.com", name = "A", role = "ATHLETE", authToken = "tok"
    )

    private fun profileDto(
        id: Long = 1L,
        calibratedAt: String? = null
    ) = AthleteProfileDto(
        id = id,
        firstName = "Juan",
        lastName = "Perez",
        bodyweightKg = 82.5,
        ageYears = 28,
        sex = "MALE",
        calibratedAt = calibratedAt,
        createdAt = "2026-05-09T20:54:34Z",
        updatedAt = "2026-05-09T20:54:34Z"
    )

    private fun errorResponse(code: Int, body: String): Response<AthleteProfileDto> =
        Response.error(code, body.toResponseBody("application/json".toMediaTypeOrNull()))

    private fun calibrationsErrorResponse(code: Int, body: String): Response<List<MvcCalibrationDto>> =
        Response.error(code, body.toResponseBody("application/json".toMediaTypeOrNull()))

    // ── fetchProfile ──────────────────────────────────────────────────────────

    @Test
    fun `fetchProfile success persists entity and returns Success`() = runTest {
        coEvery { userDao.getLoggedInUser() } returns userEntity()
        coEvery { dao.getByUserId(7L) } returns null
        coEvery { api.getProfile() } returns Response.success(profileDto(id = 13L))

        val result = build().fetchProfile()

        assertTrue(result is AthleteProfileResult.Success)
        val captured = slot<AthleteProfileEntity>()
        coVerify { dao.insert(capture(captured)) }
        assertEquals(7L, captured.captured.userId)
        assertEquals("Juan", captured.captured.firstName)
        assertEquals(82.5f, captured.captured.bodyweightKg, 0.01f)
    }

    @Test
    fun `fetchProfile success updates existing entity preserving local id`() = runTest {
        coEvery { userDao.getLoggedInUser() } returns userEntity()
        val existing = AthleteProfileEntity(
            id = 99L, userId = 7L, firstName = "old", lastName = "name",
            bodyweightKg = 75f, ageYears = 25, sex = "MALE", calibratedAt = null
        )
        coEvery { dao.getByUserId(7L) } returns existing
        coEvery { api.getProfile() } returns Response.success(profileDto())

        val result = build().fetchProfile()

        assertTrue(result is AthleteProfileResult.Success)
        val captured = slot<AthleteProfileEntity>()
        coVerify { dao.update(capture(captured)) }
        assertEquals(99L, captured.captured.id) // local id preserved
        assertEquals(82.5f, captured.captured.bodyweightKg, 0.01f)
    }

    @Test
    fun `fetchProfile returns NotFound on 404`() = runTest {
        coEvery { userDao.getLoggedInUser() } returns userEntity()
        coEvery { api.getProfile() } returns errorResponse(404, """{"message":"none"}""")

        val result = build().fetchProfile()

        assertTrue(result is AthleteProfileResult.NotFound)
    }

    @Test
    fun `fetchProfile returns Forbidden on 403`() = runTest {
        coEvery { userDao.getLoggedInUser() } returns userEntity()
        coEvery { api.getProfile() } returns errorResponse(403, """{"message":"no"}""")

        assertTrue(build().fetchProfile() is AthleteProfileResult.Forbidden)
    }

    @Test
    fun `fetchProfile returns Unauthorized on 401`() = runTest {
        coEvery { userDao.getLoggedInUser() } returns userEntity()
        coEvery { api.getProfile() } returns errorResponse(401, """{"message":"unauth"}""")

        assertTrue(build().fetchProfile() is AthleteProfileResult.Unauthorized)
    }

    @Test
    fun `fetchProfile returns Throttled on 429`() = runTest {
        coEvery { userDao.getLoggedInUser() } returns userEntity()
        coEvery { api.getProfile() } returns errorResponse(429, """{"message":"too many"}""")

        assertTrue(build().fetchProfile() is AthleteProfileResult.Throttled)
    }

    @Test
    fun `fetchProfile returns ServerError with code on 5xx`() = runTest {
        coEvery { userDao.getLoggedInUser() } returns userEntity()
        coEvery { api.getProfile() } returns errorResponse(503, """{"message":"down"}""")

        val result = build().fetchProfile()
        assertTrue(result is AthleteProfileResult.ServerError)
        assertEquals(503, (result as AthleteProfileResult.ServerError).code)
    }

    @Test
    fun `fetchProfile returns NetworkError on IOException`() = runTest {
        coEvery { userDao.getLoggedInUser() } returns userEntity()
        coEvery { api.getProfile() } throws IOException("offline")

        assertTrue(build().fetchProfile() is AthleteProfileResult.NetworkError)
    }

    // ── createProfile ─────────────────────────────────────────────────────────

    @Test
    fun `createProfile success persists entity`() = runTest {
        coEvery { userDao.getLoggedInUser() } returns userEntity()
        coEvery { dao.getByUserId(7L) } returns null
        coEvery { api.createProfile(any()) } returns Response.success(profileDto(id = 1L))

        val result = build().createProfile(
            firstName = "Juan", lastName = "Perez",
            bodyweightKg = 82.5, ageYears = 28, sex = Sex.MALE
        )

        assertTrue(result is AthleteProfileResult.Success)
        coVerify { dao.insert(any()) }
    }

    @Test
    fun `createProfile 422 returns ValidationError with errors map`() = runTest {
        coEvery { userDao.getLoggedInUser() } returns userEntity()
        val body = """
            {
              "message": "The bodyweight kg field must be between 30 and 300.",
              "errors": { "bodyweight_kg": ["The bodyweight kg field must be between 30 and 300."] }
            }
        """.trimIndent()
        coEvery { api.createProfile(any()) } returns errorResponse(422, body)

        val result = build().createProfile("J", "P", 999.0, 28, Sex.MALE)

        assertTrue(result is AthleteProfileResult.ValidationError)
        val errs = (result as AthleteProfileResult.ValidationError).errors
        assertEquals(
            listOf("The bodyweight kg field must be between 30 and 300."),
            errs["bodyweight_kg"]
        )
    }

    @Test
    fun `createProfile 422 with profile-already-exists key surfaces under profile`() = runTest {
        coEvery { userDao.getLoggedInUser() } returns userEntity()
        val body = """
            {
              "message": "The profile field has been already taken.",
              "errors": { "profile": ["Ya existe un perfil para este usuario."] }
            }
        """.trimIndent()
        coEvery { api.createProfile(any()) } returns errorResponse(422, body)

        val result = build().createProfile("J", "P", 80.0, 28, Sex.MALE)

        assertTrue(result is AthleteProfileResult.ValidationError)
        assertEquals(
            listOf("Ya existe un perfil para este usuario."),
            (result as AthleteProfileResult.ValidationError).errors["profile"]
        )
    }

    // ── updateProfile ─────────────────────────────────────────────────────────

    @Test
    fun `updateProfile success persists update`() = runTest {
        coEvery { userDao.getLoggedInUser() } returns userEntity()
        coEvery { dao.getByUserId(7L) } returns AthleteProfileEntity(
            id = 5L, userId = 7L, firstName = "old", lastName = "x",
            bodyweightKg = 80f, ageYears = 28, sex = "MALE"
        )
        coEvery { api.updateProfile(any()) } returns Response.success(profileDto())

        val result = build().updateProfile(bodyweightKg = 85.0)

        assertTrue(result is AthleteProfileResult.Success)
        coVerify { dao.update(any()) }
    }

    @Test
    fun `updateProfile 404 returns NotFound`() = runTest {
        coEvery { userDao.getLoggedInUser() } returns userEntity()
        coEvery { api.updateProfile(any()) } returns errorResponse(404, """{"message":"none"}""")

        assertTrue(build().updateProfile(bodyweightKg = 85.0) is AthleteProfileResult.NotFound)
    }

    @Test
    fun `updateProfile network error returns NetworkError`() = runTest {
        coEvery { userDao.getLoggedInUser() } returns userEntity()
        coEvery { api.updateProfile(any()) } throws IOException("offline")

        assertTrue(build().updateProfile(bodyweightKg = 85.0) is AthleteProfileResult.NetworkError)
    }

    // ── calibrate ─────────────────────────────────────────────────────────────

    @Test
    fun `calibrate success persists calibrations and bumps calibratedAt`() = runTest {
        coEvery { userDao.getLoggedInUser() } returns userEntity()
        coEvery { dao.getByUserId(7L) } returns AthleteProfileEntity(
            id = 5L, userId = 7L, firstName = "J", lastName = "P",
            bodyweightKg = 80f, ageYears = 28, sex = "MALE"
        )
        coEvery { api.calibrate(any()) } returns Response.success(
            listOf(
                MvcCalibrationDto("VASTUS_LATERALIS", "LEFT",  88.0, "2026-05-10T03:06:28.000000Z"),
                MvcCalibrationDto("VASTUS_LATERALIS", "RIGHT", 86.0, "2026-05-10T03:06:28.000000Z")
            )
        )

        val input = listOf(
            MvcCalibration(athleteProfileId = 5L, muscle = Muscle.VASTUS_LATERALIS, side = MuscleSide.LEFT,  mvcValue = 88f),
            MvcCalibration(athleteProfileId = 5L, muscle = Muscle.VASTUS_LATERALIS, side = MuscleSide.RIGHT, mvcValue = 86f)
        )
        val result = build().calibrate(input)

        assertTrue(result is MvcCalibrationResult.Success)
        coVerify { dao.deleteCalibrations(5L) }
        coVerify { dao.insertCalibrations(any()) }
        coVerify { dao.markCalibrated(5L, any()) }
    }

    @Test
    fun `calibrate 422 returns ValidationError with field map`() = runTest {
        coEvery { userDao.getLoggedInUser() } returns userEntity()
        val body = """
            {
              "message": "The selected calibrations.0.muscle is invalid.",
              "errors": { "calibrations.0.muscle": ["..."] }
            }
        """.trimIndent()
        coEvery { api.calibrate(any()) } returns calibrationsErrorResponse(422, body)

        val result = build().calibrate(emptyList())

        assertTrue(result is MvcCalibrationResult.ValidationError)
    }

    @Test
    fun `calibrate 403 returns Forbidden`() = runTest {
        coEvery { userDao.getLoggedInUser() } returns userEntity()
        coEvery { api.calibrate(any()) } returns calibrationsErrorResponse(403, """{"message":"no"}""")

        assertTrue(build().calibrate(emptyList()) is MvcCalibrationResult.Forbidden)
    }

    @Test
    fun `calibrate network error returns NetworkError`() = runTest {
        coEvery { userDao.getLoggedInUser() } returns userEntity()
        coEvery { api.calibrate(any()) } throws IOException("offline")

        assertTrue(build().calibrate(emptyList()) is MvcCalibrationResult.NetworkError)
    }
}
