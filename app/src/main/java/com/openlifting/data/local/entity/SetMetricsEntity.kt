package com.openlifting.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "set_metrics",
    foreignKeys = [ForeignKey(TrainingSetEntity::class, ["localId"], ["setLocalId"], onDelete = ForeignKey.CASCADE)]
)
data class SetMetricsEntity(
    @PrimaryKey val setLocalId: Long,
    val bsaVlPct: Float,
    val bsaVmPct: Float,
    val bsaGmaxPct: Float,
    val bsaEsPct: Float,
    val hqRatio: Float,
    val esGmaxRatio: Float,
    val intraSetFatigueRatio: Float,
    val thresholdsVersion: Int = 1
)
