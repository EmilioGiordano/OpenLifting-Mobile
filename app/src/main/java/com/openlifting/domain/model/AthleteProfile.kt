package com.openlifting.domain.model

data class AthleteProfile(
    val id: Long = 0,
    val userId: Long,
    val firstName: String,
    val lastName: String,
    val bodyweightKg: Float,
    val ageYears: Int,
    val sex: Sex,
    val calibratedAt: Long? = null
) {
    val fullName: String get() = "$firstName $lastName"
}
