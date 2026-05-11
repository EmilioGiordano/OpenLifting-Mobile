package com.openlifting.domain.model

/**
 * Athlete created by an instructor before the athlete has their own account.
 * Backed by `guest_profiles` on Vortex (not the `users` table). After the
 * athlete claims via code, [claimed] turns true and [claimedAt] is set; the
 * row is preserved for the coach's audit trail.
 */
data class GuestProfile(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val bodyweightKg: Float,
    val ageYears: Int,
    val sex: Sex,
    val calibratedAt: Long?,
    val claimed: Boolean,
    val claimedAt: Long?,
    val createdAt: Long
) {
    val fullName: String get() = "$firstName $lastName".trim()
}

sealed interface GuestProfileResult {
    data class Success(val guest: GuestProfile) : GuestProfileResult
    data class ValidationError(val errors: Map<String, List<String>>) : GuestProfileResult
    data object Unauthorized : GuestProfileResult
    data object Forbidden : GuestProfileResult
    data object NotFound : GuestProfileResult
    data object Throttled : GuestProfileResult
    data class NetworkError(val cause: Throwable? = null) : GuestProfileResult
    data class ServerError(val code: Int) : GuestProfileResult
}

sealed interface ClaimCodeResult {
    data class Success(val code: String, val sessionId: Long, val expiresAtEpochMs: Long) : ClaimCodeResult
    data object NotFound : ClaimCodeResult        // session not owned by instructor, or not a guest session
    data object Unauthorized : ClaimCodeResult
    data object Forbidden : ClaimCodeResult
    data class NetworkError(val cause: Throwable? = null) : ClaimCodeResult
    data class ServerError(val code: Int) : ClaimCodeResult
}

/** Outcome of `POST /api/claim` on the athlete side. */
sealed interface ClaimRedeemResult {
    /** Code accepted, session transferred. Returns the local id of the synced session row. */
    data class Success(val sessionLocalId: Long) : ClaimRedeemResult
    data object NotFound : ClaimRedeemResult          // code doesn't exist
    data object ExpiredOrUsed : ClaimRedeemResult     // 410
    data class ValidationError(val errors: Map<String, List<String>>) : ClaimRedeemResult
    data object Forbidden : ClaimRedeemResult         // instructor trying to redeem
    data object Throttled : ClaimRedeemResult         // 429
    data class NetworkError(val cause: Throwable? = null) : ClaimRedeemResult
    data class ServerError(val code: Int) : ClaimRedeemResult
}
