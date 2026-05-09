package com.openlifting.data.mapper

import com.openlifting.data.local.entity.AthleteProfileEntity
import com.openlifting.data.local.entity.UserEntity
import com.openlifting.data.remote.dto.UserDto
import com.openlifting.domain.model.AthleteProfile
import com.openlifting.domain.model.Sex
import com.openlifting.domain.model.User
import com.openlifting.domain.model.UserRole

fun UserEntity.toDomain() = User(
    id        = id,
    email     = email,
    name      = name,
    role      = UserRole.valueOf(role),
    authToken = authToken,
    serverId  = serverId
)

fun User.toEntity() = UserEntity(
    id        = id,
    email     = email,
    name      = name,
    role      = role.name,
    authToken = authToken,
    serverId  = serverId
)

fun UserDto.toEntity(authToken: String): UserEntity = UserEntity(
    id        = id,
    email     = email,
    name      = name,
    role      = roleFromApi(role).name,
    authToken = authToken,
    serverId  = id
)

fun UserDto.toDomain(authToken: String): User = User(
    id        = id,
    email     = email,
    name      = name,
    role      = roleFromApi(role),
    authToken = authToken,
    serverId  = id
)

fun UserRole.toApi(): String = name.lowercase()

private fun roleFromApi(apiValue: String): UserRole =
    when (apiValue.lowercase()) {
        "athlete"    -> UserRole.ATHLETE
        "instructor" -> UserRole.INSTRUCTOR
        else         -> error("Unknown role from API: $apiValue")
    }

fun AthleteProfileEntity.toDomain() = AthleteProfile(
    id            = id,
    userId        = userId,
    firstName     = firstName,
    lastName      = lastName,
    bodyweightKg  = bodyweightKg,
    ageYears      = ageYears,
    sex           = Sex.valueOf(sex),
    calibratedAt  = calibratedAt
)

fun AthleteProfile.toEntity() = AthleteProfileEntity(
    id           = id,
    userId       = userId,
    firstName    = firstName,
    lastName     = lastName,
    bodyweightKg = bodyweightKg,
    ageYears     = ageYears,
    sex          = sex.name,
    calibratedAt = calibratedAt
)
