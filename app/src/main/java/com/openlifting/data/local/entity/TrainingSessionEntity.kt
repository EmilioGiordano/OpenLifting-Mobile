package com.openlifting.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "training_sessions",
    indices = [Index("athleteUserId")]
)
data class TrainingSessionEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val serverId: Long? = null,
    val athleteUserId: Long,
    val instructorUserId: Long? = null,
    val exercise: String = "back_squat",
    val startedAt: Long = System.currentTimeMillis(),
    val endedAt: Long? = null,
    val deviceSource: String = "SIMULATED",
    val synced: Boolean = false
)
