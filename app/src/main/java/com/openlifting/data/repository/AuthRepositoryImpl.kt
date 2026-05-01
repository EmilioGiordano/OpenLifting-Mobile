package com.openlifting.data.repository

import com.openlifting.data.local.dao.AthleteProfileDao
import com.openlifting.data.local.dao.UserDao
import com.openlifting.data.local.entity.AthleteProfileEntity
import com.openlifting.data.local.entity.UserEntity
import com.openlifting.data.mapper.toDomain
import com.openlifting.domain.model.Sex
import com.openlifting.domain.model.User
import com.openlifting.domain.model.UserRole
import com.openlifting.domain.repository.AuthRepository
import javax.inject.Inject

// Hardcoded auth — no backend call. Replace login/register bodies
// with API calls when backend is ready. ViewModels don't change.
class AuthRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val profileDao: AthleteProfileDao
) : AuthRepository {

    override suspend fun login(email: String, password: String): User? {
        if (email.isBlank() || password.length < 4) return null
        val existing = userDao.getLoggedInUser()
        if (existing != null) return existing.toDomain()
        return createDemoUser(email.substringBefore("@"), email, UserRole.ATHLETE)
    }

    override suspend fun register(
        name: String,
        email: String,
        password: String,
        role: UserRole
    ): User = createDemoUser(name, email, role)

    override suspend fun getLoggedInUser(): User? =
        userDao.getLoggedInUser()?.toDomain()

    override suspend fun logout() {
        userDao.getLoggedInUser()?.let { userDao.clearToken(it.id) }
    }

    private suspend fun createDemoUser(name: String, email: String, role: UserRole): User {
        val id = System.currentTimeMillis() % 100_000
        val userId = userDao.insert(
            UserEntity(
                id        = id,
                email     = email,
                name      = name,
                role      = role.name,
                authToken = "local-$id"
            )
        )
        if (role == UserRole.ATHLETE) {
            profileDao.insert(
                AthleteProfileEntity(
                    userId       = userId,
                    firstName    = name,
                    lastName     = "",
                    bodyweightKg = 75f,
                    ageYears     = 25,
                    sex          = Sex.MALE.name
                )
            )
        }
        return userDao.getById(userId)!!.toDomain()
    }
}
