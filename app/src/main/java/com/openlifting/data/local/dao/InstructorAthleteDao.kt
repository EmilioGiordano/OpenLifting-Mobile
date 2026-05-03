package com.openlifting.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.openlifting.data.local.entity.InstructorAthleteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InstructorAthleteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(link: InstructorAthleteEntity)

    @Query("DELETE FROM instructor_athlete WHERE instructorUserId = :instructorId AND athleteUserId = :athleteId")
    suspend fun delete(instructorId: Long, athleteId: Long)

    @Query("SELECT * FROM instructor_athlete WHERE instructorUserId = :instructorId")
    fun observeForInstructor(instructorId: Long): Flow<List<InstructorAthleteEntity>>

    @Query("SELECT * FROM instructor_athlete WHERE instructorUserId = :instructorId")
    suspend fun getForInstructor(instructorId: Long): List<InstructorAthleteEntity>

    @Query("SELECT * FROM instructor_athlete WHERE athleteUserId = :athleteId")
    suspend fun getForAthlete(athleteId: Long): List<InstructorAthleteEntity>

    /**
     * Re-points all links from [oldAthleteUserId] to [newAthleteUserId]. Used when a guest
     * profile is transferred to a registered athlete.
     */
    @Query("UPDATE instructor_athlete SET athleteUserId = :newAthleteUserId, linkType = :newLinkType WHERE athleteUserId = :oldAthleteUserId")
    suspend fun retargetAthlete(oldAthleteUserId: Long, newAthleteUserId: Long, newLinkType: String)
}
