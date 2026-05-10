package com.openlifting.data.mapper

import com.openlifting.data.local.entity.AthleteProfileEntity
import com.openlifting.data.local.entity.MvcCalibrationEntity
import com.openlifting.data.remote.dto.AthleteProfileDto
import com.openlifting.data.remote.dto.MvcCalibrationDto
import com.openlifting.data.remote.dto.MvcCalibrationInput
import com.openlifting.domain.model.AthleteProfile
import com.openlifting.domain.model.Muscle
import com.openlifting.domain.model.MuscleSide
import com.openlifting.domain.model.MvcCalibration
import com.openlifting.domain.model.Sex
import java.time.Instant
import java.time.format.DateTimeParseException

fun AthleteProfileDto.toDomain(userId: Long): AthleteProfile = AthleteProfile(
    id = id,
    userId = userId,
    firstName = firstName,
    lastName = lastName,
    bodyweightKg = bodyweightKg.toFloat(),
    ageYears = ageYears,
    sex = Sex.valueOf(sex),
    calibratedAt = calibratedAt?.toEpochMillisOrNull()
)

fun AthleteProfileDto.toEntity(userId: Long, existingLocalId: Long = 0): AthleteProfileEntity =
    AthleteProfileEntity(
        id = existingLocalId,
        userId = userId,
        firstName = firstName,
        lastName = lastName,
        bodyweightKg = bodyweightKg.toFloat(),
        ageYears = ageYears,
        sex = sex,
        calibratedAt = calibratedAt?.toEpochMillisOrNull()
    )

fun MvcCalibrationDto.toEntity(athleteProfileLocalId: Long): MvcCalibrationEntity =
    MvcCalibrationEntity(
        athleteProfileId = athleteProfileLocalId,
        muscle = muscle,
        side = side,
        mvcValue = mvcValue.toFloat()
    )

fun MvcCalibrationDto.toDomain(athleteProfileLocalId: Long): MvcCalibration =
    MvcCalibration(
        athleteProfileId = athleteProfileLocalId,
        muscle = Muscle.valueOf(muscle),
        side = MuscleSide.valueOf(side),
        mvcValue = mvcValue.toFloat()
    )

fun MvcCalibration.toApiInput(): MvcCalibrationInput = MvcCalibrationInput(
    muscle = muscle.name,
    side = side.name,
    mvcValue = mvcValue.toDouble()
)

private fun String.toEpochMillisOrNull(): Long? = try {
    Instant.parse(this).toEpochMilli()
} catch (_: DateTimeParseException) {
    null
}
