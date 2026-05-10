package com.openlifting.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AthleteProfileDto(
    val id: Long,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    @SerialName("bodyweight_kg") val bodyweightKg: Double,
    @SerialName("age_years") val ageYears: Int,
    val sex: String,
    @SerialName("calibrated_at") val calibratedAt: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class CreateAthleteProfileRequest(
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    @SerialName("bodyweight_kg") val bodyweightKg: Double,
    @SerialName("age_years") val ageYears: Int,
    val sex: String
)

@Serializable
data class UpdateAthleteProfileRequest(
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    @SerialName("bodyweight_kg") val bodyweightKg: Double? = null,
    @SerialName("age_years") val ageYears: Int? = null,
    val sex: String? = null
)

@Serializable
data class MvcCalibrationDto(
    val muscle: String,
    val side: String,
    @SerialName("mvc_value") val mvcValue: Double,
    @SerialName("recorded_at") val recordedAt: String
)

@Serializable
data class MvcCalibrationInput(
    val muscle: String,
    val side: String,
    @SerialName("mvc_value") val mvcValue: Double
)

@Serializable
data class StoreMvcCalibrationsRequest(
    val calibrations: List<MvcCalibrationInput>
)
