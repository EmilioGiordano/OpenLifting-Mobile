package com.openlifting.data.repository

import com.openlifting.data.local.dao.UserDao
import com.openlifting.data.local.entity.UserEntity
import com.openlifting.data.local.preferences.TokenStore
import com.openlifting.data.remote.api.VortexApi
import com.openlifting.data.remote.dto.AuthResponse
import com.openlifting.data.remote.dto.UserDto
import com.openlifting.domain.model.AuthResult
import com.openlifting.domain.model.UserRole
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class AuthRepositoryImplTest {

    private val api        = mockk<VortexApi>()
    private val userDao    = mockk<UserDao>(relaxed = true)
    private val tokenStore = mockk<TokenStore>(relaxed = true)
    private val json       = Json { ignoreUnknownKeys = true; isLenient = true }

    @Before
    fun setUp() {
        every { tokenStore.read() } returns null
    }

    private fun buildRepo() = AuthRepositoryImpl(api, userDao, tokenStore, json)

    private fun authResponse(
        token: String = "11|abc123",
        id: Long = 13L,
        email: String = "user@example.com",
        name: String = "Test User",
        role: String = "athlete"
    ) = AuthResponse(
        token = token,
        user = UserDto(id = id, name = name, email = email, role = role, createdAt = "2026-05-09T20:54:34Z")
    )

    private fun errorResponse(code: Int, body: String): Response<AuthResponse> {
        val errorBody = body.toResponseBody("application/json".toMediaTypeOrNull())
        return Response.error(code, errorBody)
    }

    // ── login ──────────────────────────────────────────────────────────────────

    @Test
    fun `login success persists token and user, returns Success`() = runTest {
        val response = authResponse(token = "42|tok", id = 7L, email = "a@b.com", role = "athlete")
        coEvery { api.login(any()) } returns Response.success(response)

        val result = buildRepo().login("a@b.com", "password123")

        assertTrue("expected Success, got $result", result is AuthResult.Success)
        val user = (result as AuthResult.Success).user
        assertEquals(7L, user.id)
        assertEquals("a@b.com", user.email)
        assertEquals(UserRole.ATHLETE, user.role)
        assertEquals("42|tok", user.authToken)

        verify { tokenStore.save("42|tok") }

        val captured = slot<UserEntity>()
        coVerify { userDao.insert(capture(captured)) }
        assertEquals(7L, captured.captured.id)
        assertEquals("42|tok", captured.captured.authToken)
        assertEquals("ATHLETE", captured.captured.role)
    }

    @Test
    fun `login 422 with errors map returns ValidationError preserving keys`() = runTest {
        val body = """
            {
              "message": "The email field is required.",
              "errors": {
                "email": ["The email field is required."],
                "password": ["The password must be at least 8 characters."]
              }
            }
        """.trimIndent()
        coEvery { api.login(any()) } returns errorResponse(422, body)

        val result = buildRepo().login("", "short")

        assertTrue(result is AuthResult.ValidationError)
        val errors = (result as AuthResult.ValidationError).errors
        assertEquals(listOf("The email field is required."), errors["email"])
        assertEquals(
            listOf("The password must be at least 8 characters."),
            errors["password"]
        )
        verify(exactly = 0) { tokenStore.save(any()) }
    }

    @Test
    fun `login 422 with credentials error surfaces message under email key`() = runTest {
        val body = """
            {
              "message": "Las credenciales no coinciden.",
              "errors": { "email": ["Las credenciales no coinciden."] }
            }
        """.trimIndent()
        coEvery { api.login(any()) } returns errorResponse(422, body)

        val result = buildRepo().login("a@b.com", "wrong")

        assertTrue(result is AuthResult.ValidationError)
        val errors = (result as AuthResult.ValidationError).errors
        assertEquals(listOf("Las credenciales no coinciden."), errors["email"])
    }

    @Test
    fun `login 422 with empty errors map falls back to message under underscore key`() = runTest {
        val body = """{ "message": "Validation failed.", "errors": {} }"""
        coEvery { api.login(any()) } returns errorResponse(422, body)

        val result = buildRepo().login("a@b.com", "x")

        assertTrue(result is AuthResult.ValidationError)
        val errors = (result as AuthResult.ValidationError).errors
        assertEquals(listOf("Validation failed."), errors["_"])
    }

    @Test
    fun `login 422 with malformed body returns ServerError`() = runTest {
        coEvery { api.login(any()) } returns errorResponse(422, "<html>not json</html>")

        val result = buildRepo().login("a@b.com", "x")

        assertTrue(result is AuthResult.ServerError)
        assertEquals(422, (result as AuthResult.ServerError).code)
    }

    @Test
    fun `login 429 returns Throttled`() = runTest {
        coEvery { api.login(any()) } returns errorResponse(429, """{"message":"Too Many Attempts."}""")

        val result = buildRepo().login("a@b.com", "x")

        assertTrue(result is AuthResult.Throttled)
        verify(exactly = 0) { tokenStore.save(any()) }
    }

    @Test
    fun `login 401 returns Unauthorized`() = runTest {
        coEvery { api.login(any()) } returns errorResponse(401, """{"message":"Unauthenticated."}""")

        val result = buildRepo().login("a@b.com", "x")

        assertTrue(result is AuthResult.Unauthorized)
    }

    @Test
    fun `login 500 returns ServerError with code`() = runTest {
        coEvery { api.login(any()) } returns errorResponse(503, """{"message":"down"}""")

        val result = buildRepo().login("a@b.com", "x")

        assertTrue(result is AuthResult.ServerError)
        assertEquals(503, (result as AuthResult.ServerError).code)
    }

    @Test
    fun `login on IOException returns NetworkError`() = runTest {
        coEvery { api.login(any()) } throws IOException("connection refused")

        val result = buildRepo().login("a@b.com", "x")

        assertTrue(result is AuthResult.NetworkError)
    }

    @Test
    fun `login success body null returns ServerError`() = runTest {
        coEvery { api.login(any()) } returns Response.success(null)

        val result = buildRepo().login("a@b.com", "x")

        assertTrue(result is AuthResult.ServerError)
    }

    // ── register ───────────────────────────────────────────────────────────────

    @Test
    fun `register success persists and maps role from API string`() = runTest {
        val response = authResponse(role = "instructor", id = 99L)
        coEvery { api.register(any()) } returns Response.success(response)

        val result = buildRepo().register(
            name = "Coach",
            email = "coach@x.com",
            password = "password123",
            role = UserRole.INSTRUCTOR
        )

        assertTrue(result is AuthResult.Success)
        assertEquals(UserRole.INSTRUCTOR, (result as AuthResult.Success).user.role)
        assertEquals(99L, result.user.id)
    }

    @Test
    fun `register 422 with email already taken returns ValidationError`() = runTest {
        val body = """
            {
              "message": "The email has already been taken.",
              "errors": { "email": ["The email has already been taken."] }
            }
        """.trimIndent()
        coEvery { api.register(any()) } returns errorResponse(422, body)

        val result = buildRepo().register("X", "dup@x.com", "password123", UserRole.ATHLETE)

        assertTrue(result is AuthResult.ValidationError)
        assertEquals(
            listOf("The email has already been taken."),
            (result as AuthResult.ValidationError).errors["email"]
        )
    }

    // ── probeSession ───────────────────────────────────────────────────────────

    @Test
    fun `probeSession with no cached user returns Unauthorized`() = runTest {
        coEvery { userDao.getLoggedInUser() } returns null

        val result = buildRepo().probeSession()

        assertTrue(result is AuthResult.Unauthorized)
    }

    @Test
    fun `probeSession with cached user but no token returns Unauthorized`() = runTest {
        coEvery { userDao.getLoggedInUser() } returns userEntity(token = "tok")
        every { tokenStore.read() } returns null

        val result = buildRepo().probeSession()

        assertTrue(result is AuthResult.Unauthorized)
    }

    @Test
    fun `probeSession 200 returns Success with cached user`() = runTest {
        coEvery { userDao.getLoggedInUser() } returns userEntity()
        every { tokenStore.read() } returns "tok"
        coEvery { api.currentUser() } returns Response.success(Unit)

        val result = buildRepo().probeSession()

        assertTrue(result is AuthResult.Success)
        assertEquals(13L, (result as AuthResult.Success).user.id)
    }

    @Test
    fun `probeSession 401 clears local state and returns Unauthorized`() = runTest {
        coEvery { userDao.getLoggedInUser() } returns userEntity()
        every { tokenStore.read() } returns "tok"
        coEvery { api.currentUser() } returns Response.error(
            401,
            "{}".toResponseBody("application/json".toMediaTypeOrNull())
        )

        val result = buildRepo().probeSession()

        assertTrue(result is AuthResult.Unauthorized)
        verify { tokenStore.clear() }
        coVerify { userDao.clearToken(13L) }
    }

    @Test
    fun `probeSession on IOException with cached user returns Success fallback`() = runTest {
        coEvery { userDao.getLoggedInUser() } returns userEntity()
        every { tokenStore.read() } returns "tok"
        coEvery { api.currentUser() } throws IOException("offline")

        val result = buildRepo().probeSession()

        assertTrue("expected Success fallback, got $result", result is AuthResult.Success)
        verify(exactly = 0) { tokenStore.clear() }
    }

    @Test
    fun `probeSession 5xx returns ServerError`() = runTest {
        coEvery { userDao.getLoggedInUser() } returns userEntity()
        every { tokenStore.read() } returns "tok"
        coEvery { api.currentUser() } returns Response.error(
            502,
            "{}".toResponseBody("application/json".toMediaTypeOrNull())
        )

        val result = buildRepo().probeSession()

        assertTrue(result is AuthResult.ServerError)
        assertEquals(502, (result as AuthResult.ServerError).code)
    }

    // ── logout ─────────────────────────────────────────────────────────────────

    @Test
    fun `logout calls api and clears local state`() = runTest {
        coEvery { userDao.getLoggedInUser() } returns userEntity()
        coEvery { api.logout() } returns Response.success(Unit)

        buildRepo().logout()

        coVerify { api.logout() }
        verify { tokenStore.clear() }
        coVerify { userDao.clearToken(13L) }
    }

    @Test
    fun `logout still clears local state when network fails`() = runTest {
        coEvery { userDao.getLoggedInUser() } returns userEntity()
        coEvery { api.logout() } throws IOException("offline")

        buildRepo().logout()

        verify { tokenStore.clear() }
        coVerify { userDao.clearToken(13L) }
    }

    // ── getCachedUser ──────────────────────────────────────────────────────────

    @Test
    fun `getCachedUser returns null when no logged-in user`() = runTest {
        coEvery { userDao.getLoggedInUser() } returns null

        assertNull(buildRepo().getCachedUser())
    }

    @Test
    fun `getCachedUser maps Room entity to domain`() = runTest {
        coEvery { userDao.getLoggedInUser() } returns userEntity()

        val user = buildRepo().getCachedUser()

        assertEquals(13L, user?.id)
        assertEquals(UserRole.ATHLETE, user?.role)
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private fun userEntity(token: String? = "tok") = UserEntity(
        id = 13L,
        email = "user@example.com",
        name = "Test User",
        role = "ATHLETE",
        authToken = token,
        serverId = 13L
    )
}
