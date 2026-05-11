package com.openlifting.data.remote.api

import com.openlifting.data.remote.dto.AthleteProfileDto
import com.openlifting.data.remote.dto.ClaimRequest
import com.openlifting.data.remote.dto.CreateAthleteProfileRequest
import com.openlifting.data.remote.dto.MvcCalibrationDto
import com.openlifting.data.remote.dto.StoreMvcCalibrationsRequest
import com.openlifting.data.remote.dto.TrainingSessionDto
import com.openlifting.data.remote.dto.UpdateAthleteProfileRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST

interface VortexAthleteApi {
    @GET("api/athlete/profile")
    suspend fun getProfile(): Response<AthleteProfileDto>

    @POST("api/athlete/profile")
    suspend fun createProfile(@Body body: CreateAthleteProfileRequest): Response<AthleteProfileDto>

    @PATCH("api/athlete/profile")
    suspend fun updateProfile(@Body body: UpdateAthleteProfileRequest): Response<AthleteProfileDto>

    @POST("api/athlete/mvc")
    suspend fun calibrate(@Body body: StoreMvcCalibrationsRequest): Response<List<MvcCalibrationDto>>

    @POST("api/claim")
    suspend fun claim(@Body body: ClaimRequest): Response<TrainingSessionDto>
}
