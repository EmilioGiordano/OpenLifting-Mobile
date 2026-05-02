package com.openlifting.data.mapper

import com.openlifting.data.local.entity.AthleteProfileEntity
import com.openlifting.data.local.entity.UserEntity
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
