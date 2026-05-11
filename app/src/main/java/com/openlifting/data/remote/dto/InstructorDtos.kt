package com.openlifting.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GuestProfileDto(
    val id: Long,
    @SerialName("first_name")    val firstName: String,
    @SerialName("last_name")     val lastName: String,
    @SerialName("bodyweight_kg") val bodyweightKg: Double,
    @SerialName("age_years")     val ageYears: Int,
    val sex: String,
    @SerialName("calibrated_at") val calibratedAt: String? = null,
    val claimed: Boolean = false,
    @SerialName("claimed_at")    val claimedAt: String? = null,
    @SerialName("created_at")    val createdAt: String
)

@Serializable
data class CreateGuestRequest(
    @SerialName("first_name")    val firstName: String,
    @SerialName("last_name")     val lastName: String,
    @SerialName("bodyweight_kg") val bodyweightKg: Double,
    @SerialName("age_years")     val ageYears: Int,
    val sex: String
)

@Serializable
data class PaginatedGuests(
    val data: List<GuestProfileDto>,
    val meta: PaginationMeta? = null
)

@Serializable
data class CreateGuestSessionRequest(
    @SerialName("guest_profile_id") val guestProfileId: Long,
    @SerialName("started_at")       val startedAt: String,
    val exercise: String? = null,
    @SerialName("device_source")    val deviceSource: String? = null
)

@Serializable
data class ClaimCodeResponse(
    val code: String,
    @SerialName("session_id") val sessionId: Long,
    @SerialName("expires_at") val expiresAt: String
)

@Serializable
data class ClaimRequest(val code: String)
