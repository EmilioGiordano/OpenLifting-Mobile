package com.openlifting.data.model

data class UserProfile(
    val name: String,
    val email: String,
    val heightCm: Int,
    val weightKg: Float,
    val age: Int
)
