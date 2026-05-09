package com.openlifting.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    @SerialName("password_confirmation") val passwordConfirmation: String,
    val role: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val user: UserDto
)

@Serializable
data class UserDto(
    val id: Long,
    val name: String,
    val email: String,
    val role: String,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class ValidationErrorResponse(
    val message: String,
    val errors: Map<String, List<String>> = emptyMap()
)

@Serializable
data class HealthResponse(
    val status: String,
    val checks: HealthChecks,
    val timestamp: String
)

@Serializable
data class HealthChecks(
    val app: String,
    val database: HealthDatabase
)

@Serializable
data class HealthDatabase(
    val status: String,
    val connection: String
)
