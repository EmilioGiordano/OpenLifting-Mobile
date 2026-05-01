package com.openlifting.domain.model

data class MvcCalibration(
    val id: Long = 0,
    val athleteProfileId: Long,
    val muscle: Muscle,
    val side: MuscleSide,
    val mvcValue: Float  // baseline RMS value used as 100% reference
)
