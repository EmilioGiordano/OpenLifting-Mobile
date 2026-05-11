package com.openlifting.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "athlete_profiles",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"], childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"], childColumns = ["guestOfInstructorId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("userId"), Index("guestOfInstructorId")]
)
data class AthleteProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val firstName: String,
    val lastName: String,
    val bodyweightKg: Float,
    val ageYears: Int,
    val sex: String,
    val calibratedAt: Long? = null,
    /**
     * If non-null, this profile was created in guest mode by the given instructor.
     * The [userId] still points to a stub User row created for the guest. When the
     * guest claims their data via QR transfer, this is cleared and userId is updated
     * to the real athlete's user id.
     */
    val guestOfInstructorId: Long? = null,

    /**
     * Server id of the corresponding `guest_profiles` row on Vortex. Only set for profiles
     * created via [com.openlifting.data.repository.VortexCoachRepository]. Used by session
     * and calibration repositories to route requests to the instructor-side endpoints
     * (`/api/instructor/guests/{id}/...` and `/api/instructor/sessions`).
     */
    val guestProfileServerId: Long? = null
)
