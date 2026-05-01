package com.openlifting.domain.model

data class Rep(
    val id: Long = 0,
    val setLocalId: Long,
    val repNumber: Int,
    val durationMs: Int = 0
)
