package com.openlifting.data.model

data class Series(
    val number: Int,
    val weightKg: Float,
    val repetitions: List<Repetition>
)
