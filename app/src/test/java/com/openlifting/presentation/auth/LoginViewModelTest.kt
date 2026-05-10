package com.openlifting.presentation.auth

import com.openlifting.domain.model.AuthResult
import com.openlifting.domain.model.User
import com.openlifting.domain.model.UserRole
import com.openlifting.domain.repository.AuthRepository
import com.openlifting.domain.usecase.auth.DecideStartRoute
import com.openlifting.domain.usecase.auth.StartRoute
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val authRepository = mockk<AuthRepository>()
    private val decideStartRoute = mockk<DecideStartRoute>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun build() = LoginViewModel(authRepository, decideStartRoute)

    private fun athleteUser() = User(
        id = 1L, email = "a@b.com", name = "Test", role = UserRole.ATHLETE,
        authToken = "tok", serverId = 1L
    )

    // ── login ──────────────────────────────────────────────────────────────────

    @Test
    fun `login success emits Success with route from DecideStartRoute`() = runTest {
        coEvery { authRepository.login("a@b.com", "password123") } returns
            AuthResult.Success(athleteUser())
        coEvery { decideStartRoute(any()) } returns StartRoute.AthleteHome

        val vm = build()
        vm.login("a@b.com", "password123")

        val state = vm.uiState.value
        assertTrue("expected Success, got $state", state is LoginUiState.Success)
        assertEquals(StartRoute.AthleteHome, (state as LoginUiState.Success).route)
    }

    @Test
    fun `login success for athlete without profile routes to onboarding`() = runTest {
        coEvery { authRepository.login(any(), any()) } returns AuthResult.Success(athleteUser())
        coEvery { decideStartRoute(any()) } returns StartRoute.AthleteOnboardingProfile

        val vm = build()
        vm.login("a@b.com", "x")

        assertEquals(
            StartRoute.AthleteOnboardingProfile,
            (vm.uiState.value as LoginUiState.Success).route
        )
    }

    @Test
    fun `login trims email before calling repository`() = runTest {
        coEvery { authRepository.login("a@b.com", "password123") } returns
            AuthResult.Success(athleteUser())
        coEvery { decideStartRoute(any()) } returns StartRoute.AthleteHome

        val vm = build()
        vm.login("  a@b.com  ", "password123")

        coVerify { authRepository.login("a@b.com", "password123") }
    }

    @Test
    fun `login validation error emits FieldErrors with map`() = runTest {
        val errors = mapOf(
            "email" to listOf("The email has already been taken."),
            "password" to listOf("Too short.")
        )
        coEvery { authRepository.login(any(), any()) } returns AuthResult.ValidationError(errors)

        val vm = build()
        vm.login("a@b.com", "x")

        val state = vm.uiState.value
        assertTrue(state is LoginUiState.FieldErrors)
        assertEquals(errors, (state as LoginUiState.FieldErrors).errors)
    }

    @Test
    fun `login throttled emits Throttled`() = runTest {
        coEvery { authRepository.login(any(), any()) } returns AuthResult.Throttled

        val vm = build()
        vm.login("a@b.com", "x")

        assertEquals(LoginUiState.Throttled, vm.uiState.value)
    }

    @Test
    fun `login network error emits NetworkError`() = runTest {
        coEvery { authRepository.login(any(), any()) } returns
            AuthResult.NetworkError(IOException("offline"))

        val vm = build()
        vm.login("a@b.com", "x")

        assertEquals(LoginUiState.NetworkError, vm.uiState.value)
    }

    @Test
    fun `login server error emits Error containing the code`() = runTest {
        coEvery { authRepository.login(any(), any()) } returns AuthResult.ServerError(503)

        val vm = build()
        vm.login("a@b.com", "x")

        val state = vm.uiState.value
        assertTrue(state is LoginUiState.Error)
        assertTrue((state as LoginUiState.Error).message.contains("503"))
    }

    @Test
    fun `login unauthorized emits Error`() = runTest {
        coEvery { authRepository.login(any(), any()) } returns AuthResult.Unauthorized

        val vm = build()
        vm.login("a@b.com", "x")

        assertTrue(vm.uiState.value is LoginUiState.Error)
    }

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    fun `register athlete emits Success with route from DecideStartRoute`() = runTest {
        coEvery {
            authRepository.register("Coach", "c@x.com", "password123", UserRole.ATHLETE)
        } returns AuthResult.Success(athleteUser())
        coEvery { decideStartRoute(any()) } returns StartRoute.AthleteOnboardingProfile

        val vm = build()
        vm.register("Coach", "c@x.com", "password123", UserRole.ATHLETE)

        val state = vm.uiState.value
        assertTrue(state is LoginUiState.Success)
        assertEquals(StartRoute.AthleteOnboardingProfile, (state as LoginUiState.Success).route)
    }

    @Test
    fun `register instructor routes to instructor home`() = runTest {
        val coach = User(
            id = 2L, email = "c@x.com", name = "Coach",
            role = UserRole.INSTRUCTOR, authToken = "tok2", serverId = 2L
        )
        coEvery {
            authRepository.register(any(), any(), any(), UserRole.INSTRUCTOR)
        } returns AuthResult.Success(coach)
        coEvery { decideStartRoute(any()) } returns StartRoute.InstructorHome

        val vm = build()
        vm.register("Coach", "c@x.com", "password123", UserRole.INSTRUCTOR)

        assertEquals(
            StartRoute.InstructorHome,
            (vm.uiState.value as LoginUiState.Success).route
        )
    }

    @Test
    fun `register field errors are surfaced as FieldErrors`() = runTest {
        val errors = mapOf("email" to listOf("The email has already been taken."))
        coEvery {
            authRepository.register(any(), any(), any(), any())
        } returns AuthResult.ValidationError(errors)

        val vm = build()
        vm.register("X", "dup@x.com", "password123", UserRole.ATHLETE)

        val state = vm.uiState.value
        assertTrue(state is LoginUiState.FieldErrors)
        assertEquals(errors, (state as LoginUiState.FieldErrors).errors)
    }

    @Test
    fun `register trims name and email before calling repository`() = runTest {
        coEvery {
            authRepository.register("Test", "a@b.com", "password123", UserRole.ATHLETE)
        } returns AuthResult.Success(athleteUser())
        coEvery { decideStartRoute(any()) } returns StartRoute.AthleteOnboardingProfile

        val vm = build()
        vm.register("  Test  ", "  a@b.com  ", "password123", UserRole.ATHLETE)

        coVerify { authRepository.register("Test", "a@b.com", "password123", UserRole.ATHLETE) }
    }

    // ── checkSession ───────────────────────────────────────────────────────────

    @Test
    fun `checkSession with no cached user emits Idle`() = runTest {
        coEvery { authRepository.getCachedUser() } returns null

        val vm = build()
        vm.checkSession()

        assertEquals(LoginUiState.Idle, vm.uiState.value)
    }

    @Test
    fun `checkSession with cached user and successful probe routes via DecideStartRoute`() = runTest {
        val cached = athleteUser()
        coEvery { authRepository.getCachedUser() } returns cached
        coEvery { authRepository.probeSession() } returns AuthResult.Success(cached)
        coEvery { decideStartRoute(cached) } returns StartRoute.AthleteHome

        val vm = build()
        vm.checkSession()

        val state = vm.uiState.value
        assertTrue(state is LoginUiState.Success)
        assertEquals(StartRoute.AthleteHome, (state as LoginUiState.Success).route)
    }

    @Test
    fun `checkSession with cached user but probe Unauthorized emits Idle`() = runTest {
        coEvery { authRepository.getCachedUser() } returns athleteUser()
        coEvery { authRepository.probeSession() } returns AuthResult.Unauthorized

        val vm = build()
        vm.checkSession()

        assertEquals(LoginUiState.Idle, vm.uiState.value)
    }

    @Test
    fun `checkSession with cached user and network error still routes via DecideStartRoute`() = runTest {
        val cached = athleteUser()
        coEvery { authRepository.getCachedUser() } returns cached
        coEvery { authRepository.probeSession() } returns AuthResult.NetworkError()
        coEvery { decideStartRoute(cached) } returns StartRoute.AthleteHome

        val vm = build()
        vm.checkSession()

        val state = vm.uiState.value
        assertTrue("expected Success fallback, got $state", state is LoginUiState.Success)
        assertEquals(StartRoute.AthleteHome, (state as LoginUiState.Success).route)
    }

    // ── clearTransientError ────────────────────────────────────────────────────

    @Test
    fun `clearTransientError resets Throttled to Idle`() = runTest {
        coEvery { authRepository.login(any(), any()) } returns AuthResult.Throttled

        val vm = build()
        vm.login("a@b.com", "x")
        assertEquals(LoginUiState.Throttled, vm.uiState.value)

        vm.clearTransientError()
        assertEquals(LoginUiState.Idle, vm.uiState.value)
    }

    @Test
    fun `clearTransientError resets FieldErrors to Idle`() = runTest {
        coEvery { authRepository.login(any(), any()) } returns
            AuthResult.ValidationError(mapOf("email" to listOf("bad")))

        val vm = build()
        vm.login("a@b.com", "x")
        assertTrue(vm.uiState.value is LoginUiState.FieldErrors)

        vm.clearTransientError()
        assertEquals(LoginUiState.Idle, vm.uiState.value)
    }

    @Test
    fun `clearTransientError preserves Success state`() = runTest {
        coEvery { authRepository.login(any(), any()) } returns AuthResult.Success(athleteUser())
        coEvery { decideStartRoute(any()) } returns StartRoute.AthleteHome

        val vm = build()
        vm.login("a@b.com", "x")
        val before = vm.uiState.value
        assertTrue(before is LoginUiState.Success)

        vm.clearTransientError()
        assertEquals(before, vm.uiState.value)
    }
}
