package com.openlifting.data.model

data class EmgReading(
    val quadricepsLeft: Float,
    val quadricepsRight: Float,
    val glutesLeft: Float,
    val glutesRight: Float,
    val hamstringsLeft: Float,
    val hamstringsRight: Float,
    val lowerBackLeft: Float,
    val lowerBackRight: Float
)
