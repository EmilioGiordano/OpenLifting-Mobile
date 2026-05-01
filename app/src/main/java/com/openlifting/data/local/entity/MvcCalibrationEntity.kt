package com.openlifting.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "mvc_calibrations",
    foreignKeys = [ForeignKey(AthleteProfileEntity::class, ["id"], ["athleteProfileId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("athleteProfileId")]
)
data class MvcCalibrationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val athleteProfileId: Long,
    val muscle: String,
    val side: String,
    val mvcValue: Float
)
