package com.openlifting.domain.usecase.auth

import com.openlifting.domain.model.AthleteProfile
import com.openlifting.domain.model.AthleteProfileResult
import com.openlifting.domain.model.User
import com.openlifting.domain.model.UserRole
import com.openlifting.domain.repository.AthleteProfileRepository
import javax.inject.Inject

enum class StartRoute {
    Login,
    AthleteOnboardingProfile,
    AthleteCalibration,
    AthleteHome,
    InstructorHome
}

class DecideStartRoute @Inject constructor(
    private val athleteProfileRepository: AthleteProfileRepository
) {
    suspend operator fun invoke(user: User?): StartRoute {
        if (user == null) return StartRoute.Login
        if (user.role == UserRole.INSTRUCTOR) return StartRoute.InstructorHome

        return when (val result = athleteProfileRepository.fetchProfile()) {
            is AthleteProfileResult.Success -> routeForAthleteProfile(result.profile)
            AthleteProfileResult.NotFound   -> StartRoute.AthleteOnboardingProfile
            AthleteProfileResult.Unauthorized -> StartRoute.Login
            is AthleteProfileResult.NetworkError ->
                routeFromCacheOrFallback(user.id, fallback = StartRoute.AthleteHome)
            else -> routeFromCacheOrFallback(user.id, fallback = StartRoute.AthleteHome)
        }
    }

    private suspend fun routeFromCacheOrFallback(userId: Long, fallback: StartRoute): StartRoute {
        val cached = athleteProfileRepository.getCachedProfile(userId) ?: return fallback
        return routeForAthleteProfile(cached)
    }

    private fun routeForAthleteProfile(profile: AthleteProfile): StartRoute =
        if (profile.calibratedAt == null) StartRoute.AthleteCalibration
        else StartRoute.AthleteHome
}
