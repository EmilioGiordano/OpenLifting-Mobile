package com.openlifting.domain.model

sealed interface AuthResult {
    data class Success(val user: User) : AuthResult
    data class ValidationError(val errors: Map<String, List<String>>) : AuthResult
    data object Throttled : AuthResult
    data object Unauthorized : AuthResult
    data class NetworkError(val cause: Throwable? = null) : AuthResult
    data class ServerError(val code: Int) : AuthResult
}
