package com.openlifting.domain.repository

import com.openlifting.domain.model.User
import com.openlifting.domain.model.UserRole

interface AuthRepository {
    suspend fun login(email: String, password: String): User?
    suspend fun register(name: String, email: String, password: String, role: UserRole): User
    suspend fun getLoggedInUser(): User?
    suspend fun logout()
}
