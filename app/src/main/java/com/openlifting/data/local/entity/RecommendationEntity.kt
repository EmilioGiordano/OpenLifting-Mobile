package com.openlifting.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recommendations",
    foreignKeys = [ForeignKey(TrainingSetEntity::class, ["localId"], ["setLocalId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("setLocalId")]
)
data class RecommendationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val setLocalId: Long,
    val text: String,
    val severity: String,
    val evidence: String = ""
)
