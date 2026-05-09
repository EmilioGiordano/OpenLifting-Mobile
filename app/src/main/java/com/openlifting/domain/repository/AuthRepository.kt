package com.openlifting.domain.repository

import com.openlifting.domain.model.AuthResult
import com.openlifting.domain.model.User
import com.openlifting.domain.model.UserRole

interface AuthRepository {
    suspend fun login(email: String, password: String): AuthResult
    suspend fun register(name: String, email: String, password: String, role: UserRole): AuthResult
    suspend fun getCachedUser(): User?
    suspend fun probeSession(): AuthResult
    suspend fun logout()
}
