package com.openlifting.domain.usecase.auth

import com.openlifting.domain.model.AthleteProfile
import com.openlifting.domain.model.AthleteProfileResult
import com.openlifting.domain.model.Sex
import com.openlifting.domain.model.User
import com.openlifting.domain.model.UserRole
import com.openlifting.domain.repository.AthleteProfileRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DecideStartRouteTest {

    private val repo = mockk<AthleteProfileRepository>()
    private val decide get() = DecideStartRoute(repo)

    private fun athlete(id: Long = 1L) = User(
        id = id, email = "a@b.com", name = "A", role = UserRole.ATHLETE,
        authToken = "tok", serverId = id
    )

    private fun instructor(id: Long = 2L) = User(
        id = id, email = "c@x.com", name = "C", role = UserRole.INSTRUCTOR,
        authToken = "tok", serverId = id
    )

    private fun profile(calibratedAt: Long? = null) = AthleteProfile(
        id = 1L, userId = 1L, firstName = "Juan", lastName = "Perez",
        bodyweightKg = 80f, ageYears = 28, sex = Sex.MALE, calibratedAt = calibratedAt
    )

    @Test
    fun `null user routes to Login`() = runTest {
        assertEquals(StartRoute.Login, decide(null))
    }

    @Test
    fun `instructor routes to InstructorHome without hitting profile endpoint`() = runTest {
        val route = decide(instructor())

        assertEquals(StartRoute.InstructorHome, route)
        coVerify(exactly = 0) { repo.fetchProfile() }
    }

    @Test
    fun `athlete with profile and calibration routes to AthleteHome`() = runTest {
        coEvery { repo.fetchProfile() } returns AthleteProfileResult.Success(
            profile(calibratedAt = 1_700_000_000_000L)
        )

        assertEquals(StartRoute.AthleteHome, decide(athlete()))
    }

    @Test
    fun `athlete with profile but no calibration routes to AthleteCalibration`() = runTest {
        coEvery { repo.fetchProfile() } returns AthleteProfileResult.Success(
            profile(calibratedAt = null)
        )

        assertEquals(StartRoute.AthleteCalibration, decide(athlete()))
    }

    @Test
    fun `athlete with NotFound routes to AthleteOnboardingProfile`() = runTest {
        coEvery { repo.fetchProfile() } returns AthleteProfileResult.NotFound

        assertEquals(StartRoute.AthleteOnboardingProfile, decide(athlete()))
    }

    @Test
    fun `athlete with Unauthorized routes to Login`() = runTest {
        coEvery { repo.fetchProfile() } returns AthleteProfileResult.Unauthorized

        assertEquals(StartRoute.Login, decide(athlete()))
    }

    @Test
    fun `network error falls back to cache when calibrated`() = runTest {
        val u = athlete()
        coEvery { repo.fetchProfile() } returns AthleteProfileResult.NetworkError()
        coEvery { repo.getCachedProfile(u.id) } returns profile(calibratedAt = 1_700_000_000_000L)

        assertEquals(StartRoute.AthleteHome, decide(u))
    }

    @Test
    fun `network error with cached profile uncalibrated routes to Calibration`() = runTest {
        val u = athlete()
        coEvery { repo.fetchProfile() } returns AthleteProfileResult.NetworkError()
        coEvery { repo.getCachedProfile(u.id) } returns profile(calibratedAt = null)

        assertEquals(StartRoute.AthleteCalibration, decide(u))
    }

    @Test
    fun `network error without cached profile falls back to AthleteHome`() = runTest {
        val u = athlete()
        coEvery { repo.fetchProfile() } returns AthleteProfileResult.NetworkError()
        coEvery { repo.getCachedProfile(u.id) } returns null

        assertEquals(StartRoute.AthleteHome, decide(u))
    }

    @Test
    fun `server error falls back to cache like network error`() = runTest {
        val u = athlete()
        coEvery { repo.fetchProfile() } returns AthleteProfileResult.ServerError(500)
        coEvery { repo.getCachedProfile(u.id) } returns profile(calibratedAt = 1L)

        assertEquals(StartRoute.AthleteHome, decide(u))
    }
}
