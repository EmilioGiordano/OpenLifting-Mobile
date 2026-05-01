package com.openlifting.domain.model

data class MuscleActivation(
    val id: Long = 0,
    val repId: Long,
    val muscle: Muscle,
    val side: MuscleSide,
    val percentMvc: Float,
    val peakPercentMvc: Float = percentMvc
)
