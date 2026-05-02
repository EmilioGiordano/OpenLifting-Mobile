package com.openlifting.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Many-to-many link between an instructor user and an athlete user.
 *
 * One instructor can manage N athletes; one athlete can have N instructors. Each row
 * represents an active link.
 *
 * [linkType] tracks how the link was created so the UI can label rows ("INVITADO" vs
 * "REGISTRADO" vs "TRANSFERIDO"):
 *  - GUEST: instructor created a guest profile, this is the implicit link to the stub
 *    user backing that profile.
 *  - REGISTERED_VIA_QR: registered athlete scanned the instructor's QR.
 *  - TRANSFERRED: athlete claimed a previously-guest profile by scanning a transfer QR;
 *    the original GUEST row's athleteUserId got updated to the real user.
 */
@Entity(
    tableName = "instructor_athlete",
    primaryKeys = ["instructorUserId", "athleteUserId"],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"], childColumns = ["instructorUserId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"], childColumns = ["athleteUserId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("instructorUserId"), Index("athleteUserId")]
)
data class InstructorAthleteEntity(
    val instructorUserId: Long,
    val athleteUserId: Long,
    val linkedAt: Long = System.currentTimeMillis(),
    val linkType: String = LINK_TYPE_GUEST
) {
    companion object {
        const val LINK_TYPE_GUEST              = "GUEST"
        const val LINK_TYPE_REGISTERED_VIA_QR  = "REGISTERED_VIA_QR"
        const val LINK_TYPE_TRANSFERRED        = "TRANSFERRED"
    }
}
