package com.openlifting.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "training_sets",
    foreignKeys = [ForeignKey(TrainingSessionEntity::class, ["localId"], ["sessionLocalId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("sessionLocalId")]
)
data class TrainingSetEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val serverId: Long? = null,
    val sessionLocalId: Long,
    val setNumber: Int,
    val loadKg: Float,
    val targetReps: Int,
    val variant: String,
    val depth: String,
    val rpe: Float,
    val synced: Boolean = false
)
