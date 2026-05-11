package com.openlifting.data.remote.api

import com.openlifting.data.remote.dto.ClaimCodeResponse
import com.openlifting.data.remote.dto.CreateGuestRequest
import com.openlifting.data.remote.dto.CreateGuestSessionRequest
import com.openlifting.data.remote.dto.GuestProfileDto
import com.openlifting.data.remote.dto.MvcCalibrationDto
import com.openlifting.data.remote.dto.PaginatedGuests
import com.openlifting.data.remote.dto.StoreMvcCalibrationsRequest
import com.openlifting.data.remote.dto.TrainingSessionDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface VortexInstructorApi {
    @GET("api/instructor/guests")
    suspend fun listGuests(@Query("page") page: Int = 1): Response<PaginatedGuests>

    @POST("api/instructor/guests")
    suspend fun createGuest(@Body body: CreateGuestRequest): Response<GuestProfileDto>

    @POST("api/instructor/guests/{id}/mvc")
    suspend fun calibrateGuest(
        @Path("id") guestId: Long,
        @Body body: StoreMvcCalibrationsRequest
    ): Response<List<MvcCalibrationDto>>

    @POST("api/instructor/sessions")
    suspend fun createGuestSession(@Body body: CreateGuestSessionRequest): Response<TrainingSessionDto>

    @POST("api/instructor/sessions/{id}/claim-code")
    suspend fun generateClaimCode(@Path("id") sessionId: Long): Response<ClaimCodeResponse>
}
