package com.openlifting.domain.model

data class TrainingSet(
    val localId: Long = 0,
    val serverId: Long? = null,
    val sessionLocalId: Long,
    val setNumber: Int,
    val loadKg: Float,
    val targetReps: Int,
    val variant: SquatVariant,
    val depth: SquatDepth,
    val rpe: Float,
    val synced: Boolean = false
)
