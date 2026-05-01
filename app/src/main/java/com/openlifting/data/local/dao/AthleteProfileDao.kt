package com.openlifting.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.openlifting.data.local.entity.AthleteProfileEntity
import com.openlifting.data.local.entity.MvcCalibrationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AthleteProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: AthleteProfileEntity): Long

    @Update
    suspend fun update(profile: AthleteProfileEntity)

    @Query("SELECT * FROM athlete_profiles WHERE userId = :userId LIMIT 1")
    suspend fun getByUserId(userId: Long): AthleteProfileEntity?

    @Query("SELECT * FROM athlete_profiles WHERE userId = :userId LIMIT 1")
    fun observeByUserId(userId: Long): Flow<AthleteProfileEntity?>

    @Query("UPDATE athlete_profiles SET calibratedAt = :timestamp WHERE id = :profileId")
    suspend fun markCalibrated(profileId: Long, timestamp: Long)

    // MVC Calibrations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalibrations(calibrations: List<MvcCalibrationEntity>)

    @Query("SELECT * FROM mvc_calibrations WHERE athleteProfileId = :profileId")
    suspend fun getCalibrationsForProfile(profileId: Long): List<MvcCalibrationEntity>

    @Query("SELECT * FROM mvc_calibrations WHERE athleteProfileId = :profileId")
    fun observeCalibrations(profileId: Long): Flow<List<MvcCalibrationEntity>>

    @Query("DELETE FROM mvc_calibrations WHERE athleteProfileId = :profileId")
    suspend fun deleteCalibrations(profileId: Long)
}
