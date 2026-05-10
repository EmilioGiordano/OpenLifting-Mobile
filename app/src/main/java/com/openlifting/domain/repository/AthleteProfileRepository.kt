package com.openlifting.domain.repository

import com.openlifting.domain.model.AthleteProfile
import com.openlifting.domain.model.AthleteProfileResult
import com.openlifting.domain.model.MvcCalibration
import com.openlifting.domain.model.MvcCalibrationResult
import com.openlifting.domain.model.Sex
import kotlinx.coroutines.flow.Flow

interface AthleteProfileRepository {
    suspend fun fetchProfile(): AthleteProfileResult

    suspend fun createProfile(
        firstName: String,
        lastName: String,
        bodyweightKg: Double,
        ageYears: Int,
        sex: Sex
    ): AthleteProfileResult

    suspend fun updateProfile(
        firstName: String? = null,
        lastName: String? = null,
        bodyweightKg: Double? = null,
        ageYears: Int? = null,
        sex: Sex? = null
    ): AthleteProfileResult

    suspend fun calibrate(calibrations: List<MvcCalibration>): MvcCalibrationResult

    fun observeCachedProfile(userId: Long): Flow<AthleteProfile?>

    suspend fun getCachedProfile(userId: Long): AthleteProfile?
}
