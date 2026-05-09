package com.openlifting.data.repository

import com.openlifting.data.local.dao.UserDao
import com.openlifting.data.local.preferences.TokenStore
import com.openlifting.data.mapper.toApi
import com.openlifting.data.mapper.toDomain
import com.openlifting.data.mapper.toEntity
import com.openlifting.data.remote.api.VortexApi
import com.openlifting.data.remote.dto.AuthResponse
import com.openlifting.data.remote.dto.LoginRequest
import com.openlifting.data.remote.dto.RegisterRequest
import com.openlifting.data.remote.dto.ValidationErrorResponse
import com.openlifting.domain.model.AuthResult
import com.openlifting.domain.model.User
import com.openlifting.domain.model.UserRole
import com.openlifting.domain.repository.AuthRepository
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: VortexApi,
    private val userDao: UserDao,
    private val tokenStore: TokenStore,
    private val json: Json
) : AuthRepository {

    override suspend fun login(email: String, password: String): AuthResult =
        runCatchingHttp {
            api.login(LoginRequest(email = email, password = password))
        }.foldAuth { persist(it) }

    override suspend fun register(
        name: String,
        email: String,
        password: String,
        role: UserRole
    ): AuthResult = runCatchingHttp {
        api.register(
            RegisterRequest(
                name = name,
                email = email,
                password = password,
                passwordConfirmation = password,
                role = role.toApi()
            )
        )
    }.foldAuth { persist(it) }

    override suspend fun getCachedUser(): User? =
        userDao.getLoggedInUser()?.toDomain()

    override suspend fun probeSession(): AuthResult {
        val cached = userDao.getLoggedInUser() ?: return AuthResult.Unauthorized
        if (tokenStore.read().isNullOrBlank()) return AuthResult.Unauthorized

        return try {
            val response = api.currentUser()
            when (response.code()) {
                200 -> AuthResult.Success(cached.toDomain())
                401 -> {
                    clearLocal()
                    AuthResult.Unauthorized
                }
                in 500..599 -> AuthResult.ServerError(response.code())
                else -> AuthResult.Success(cached.toDomain())
            }
        } catch (e: IOException) {
            AuthResult.Success(cached.toDomain())
        }
    }

    override suspend fun logout() {
        try {
            api.logout()
        } catch (_: IOException) {
            // network failure is fine — client-side cleanup must still happen
        } finally {
            clearLocal()
        }
    }

    private suspend fun persist(response: AuthResponse): AuthResult.Success {
        tokenStore.save(response.token)
        val entity = response.user.toEntity(authToken = response.token)
        userDao.insert(entity)
        return AuthResult.Success(entity.toDomain())
    }

    private suspend fun clearLocal() {
        tokenStore.clear()
        userDao.getLoggedInUser()?.let { userDao.clearToken(it.id) }
    }

    private inline fun runCatchingHttp(
        call: () -> Response<AuthResponse>
    ): HttpOutcome = try {
        HttpOutcome.Ok(call())
    } catch (e: IOException) {
        HttpOutcome.Network(e)
    } catch (e: Exception) {
        HttpOutcome.Unexpected(e)
    }

    private suspend inline fun HttpOutcome.foldAuth(
        onSuccess: suspend (AuthResponse) -> AuthResult.Success
    ): AuthResult = when (this) {
        is HttpOutcome.Ok -> handleHttpResponse(response, onSuccess)
        is HttpOutcome.Network -> AuthResult.NetworkError(cause)
        is HttpOutcome.Unexpected -> AuthResult.NetworkError(cause)
    }

    private suspend inline fun handleHttpResponse(
        response: Response<AuthResponse>,
        onSuccess: suspend (AuthResponse) -> AuthResult.Success
    ): AuthResult {
        if (response.isSuccessful) {
            val body = response.body() ?: return AuthResult.ServerError(response.code())
            return onSuccess(body)
        }
        return when (response.code()) {
            422 -> parseValidation(response)
            429 -> AuthResult.Throttled
            401 -> AuthResult.Unauthorized
            in 500..599 -> AuthResult.ServerError(response.code())
            else -> AuthResult.ServerError(response.code())
        }
    }

    private fun parseValidation(response: Response<*>): AuthResult {
        val raw = response.errorBody()?.string().orEmpty()
        return try {
            val parsed = json.decodeFromString(ValidationErrorResponse.serializer(), raw)
            AuthResult.ValidationError(parsed.errors.ifEmpty { mapOf("_" to listOf(parsed.message)) })
        } catch (_: Exception) {
            AuthResult.ServerError(422)
        }
    }

    private sealed interface HttpOutcome {
        data class Ok(val response: Response<AuthResponse>) : HttpOutcome
        data class Network(val cause: Throwable) : HttpOutcome
        data class Unexpected(val cause: Throwable) : HttpOutcome
    }
}
