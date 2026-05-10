package com.openlifting.data.remote.api

import com.openlifting.data.remote.dto.CreateSessionRequest
import com.openlifting.data.remote.dto.EndSessionRequest
import com.openlifting.data.remote.dto.PaginatedSessions
import com.openlifting.data.remote.dto.PatchSessionRequest
import com.openlifting.data.remote.dto.PostSetRequest
import com.openlifting.data.remote.dto.PostSetResponse
import com.openlifting.data.remote.dto.TrainingSessionDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface VortexSessionApi {
    @GET("api/sessions")
    suspend fun listSessions(@Query("page") page: Int = 1): Response<PaginatedSessions>

    @POST("api/sessions")
    suspend fun createSession(@Body body: CreateSessionRequest): Response<TrainingSessionDto>

    @PATCH("api/sessions/{id}")
    suspend fun patchSession(
        @Path("id") id: Long,
        @Body body: PatchSessionRequest
    ): Response<TrainingSessionDto>

    @PUT("api/sessions/{id}/end")
    suspend fun endSession(
        @Path("id") id: Long,
        @Body body: EndSessionRequest
    ): Response<TrainingSessionDto>

    @POST("api/sessions/{session_id}/sets")
    suspend fun postSet(
        @Path("session_id") sessionServerId: Long,
        @Body body: PostSetRequest
    ): Response<PostSetResponse>
}
