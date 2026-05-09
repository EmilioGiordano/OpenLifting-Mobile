package com.openlifting.data.remote.api

import com.openlifting.data.remote.dto.AuthResponse
import com.openlifting.data.remote.dto.HealthResponse
import com.openlifting.data.remote.dto.LoginRequest
import com.openlifting.data.remote.dto.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface VortexApi {
    @GET("up")
    suspend fun health(): HealthResponse

    @POST("api/register")
    suspend fun register(@Body body: RegisterRequest): Response<AuthResponse>

    @POST("api/login")
    suspend fun login(@Body body: LoginRequest): Response<AuthResponse>

    @POST("api/logout")
    suspend fun logout(): Response<Unit>

    @GET("api/user")
    suspend fun currentUser(): Response<Unit>
}
