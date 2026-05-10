package com.openlifting.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TrainingSessionDto(
    val id: Long,
    val exercise: String,
    @SerialName("started_at") val startedAt: String,
    @SerialName("ended_at") val endedAt: String? = null,
    @SerialName("device_source") val deviceSource: String,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class CreateSessionRequest(
    @SerialName("started_at") val startedAt: String,
    val exercise: String? = null,
    @SerialName("device_source") val deviceSource: String? = null
)

@Serializable
data class EndSessionRequest(
    @SerialName("ended_at") val endedAt: String
)

@Serializable
data class PaginatedSessions(
    val data: List<TrainingSessionDto>,
    val meta: PaginationMeta? = null
)

@Serializable
data class PaginationMeta(
    @SerialName("current_page") val currentPage: Int,
    @SerialName("per_page") val perPage: Int,
    val total: Int,
    @SerialName("last_page") val lastPage: Int
)
