package com.openlifting.data.remote.api

import com.openlifting.data.remote.dto.CreateSessionRequest
import com.openlifting.data.remote.dto.EndSessionRequest
import com.openlifting.data.remote.dto.PaginatedSessions
import com.openlifting.data.remote.dto.TrainingSessionDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface VortexSessionApi {
    @GET("api/sessions")
    suspend fun listSessions(@Query("page") page: Int = 1): Response<PaginatedSessions>

    @POST("api/sessions")
    suspend fun createSession(@Body body: CreateSessionRequest): Response<TrainingSessionDto>

    @PUT("api/sessions/{id}/end")
    suspend fun endSession(
        @Path("id") id: Long,
        @Body body: EndSessionRequest
    ): Response<TrainingSessionDto>
}
