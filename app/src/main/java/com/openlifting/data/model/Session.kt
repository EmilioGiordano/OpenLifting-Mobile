package com.openlifting.data.model

import java.time.LocalDateTime

data class Session(
    val id: String,
    val date: LocalDateTime,
    val series: List<Series>
)
