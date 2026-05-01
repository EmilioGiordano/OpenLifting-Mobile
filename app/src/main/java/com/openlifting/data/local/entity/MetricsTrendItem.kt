package com.openlifting.data.local.entity

import androidx.room.ColumnInfo

data class MetricsTrendItem(
    @ColumnInfo(name = "bsaVlPct")     val bsaVlPct: Float,
    @ColumnInfo(name = "bsaGmaxPct")   val bsaGmaxPct: Float,
    @ColumnInfo(name = "esGmaxRatio")  val esGmaxRatio: Float,
    @ColumnInfo(name = "startedAt")    val startedAt: Long
)
