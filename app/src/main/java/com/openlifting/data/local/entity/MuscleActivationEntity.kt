package com.openlifting.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "muscle_activations",
    foreignKeys = [ForeignKey(RepEntity::class, ["id"], ["repId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("repId")]
)
data class MuscleActivationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val repId: Long,
    val muscle: String,
    val side: String,
    val percentMvc: Float,
    val peakPercentMvc: Float
)
