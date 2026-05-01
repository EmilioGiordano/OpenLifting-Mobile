package com.openlifting.domain.model

data class TrainingSession(
    val localId: Long = 0,
    val serverId: Long? = null,
    val athleteUserId: Long,
    val instructorUserId: Long? = null,
    val exercise: String = "back_squat",
    val startedAt: Long = System.currentTimeMillis(),
    val endedAt: Long? = null,
    val deviceSource: DeviceSource = DeviceSource.SIMULATED,
    val synced: Boolean = false
)
