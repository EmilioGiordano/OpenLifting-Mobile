package com.openlifting.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.openlifting.data.local.dao.AthleteProfileDao
import com.openlifting.data.local.dao.InstructorAthleteDao
import com.openlifting.data.local.dao.SessionDao
import com.openlifting.data.local.dao.SetDao
import com.openlifting.data.local.dao.UserDao
import com.openlifting.data.local.entity.AthleteProfileEntity
import com.openlifting.data.local.entity.InstructorAthleteEntity
import com.openlifting.data.local.entity.MuscleActivationEntity
import com.openlifting.data.local.entity.MvcCalibrationEntity
import com.openlifting.data.local.entity.RepEntity
import com.openlifting.data.local.entity.RecommendationEntity
import com.openlifting.data.local.entity.SetMetricsEntity
import com.openlifting.data.local.entity.TrainingSessionEntity
import com.openlifting.data.local.entity.TrainingSetEntity
import com.openlifting.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        AthleteProfileEntity::class,
        MvcCalibrationEntity::class,
        TrainingSessionEntity::class,
        TrainingSetEntity::class,
        RepEntity::class,
        MuscleActivationEntity::class,
        SetMetricsEntity::class,
        RecommendationEntity::class,
        InstructorAthleteEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class OpenLiftingDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun athleteProfileDao(): AthleteProfileDao
    abstract fun sessionDao(): SessionDao
    abstract fun setDao(): SetDao
    abstract fun instructorAthleteDao(): InstructorAthleteDao
}
