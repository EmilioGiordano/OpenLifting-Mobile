package com.openlifting.domain.model

sealed interface AthleteProfileResult {
    data class Success(val profile: AthleteProfile) : AthleteProfileResult
    data object NotFound : AthleteProfileResult
    data class ValidationError(val errors: Map<String, List<String>>) : AthleteProfileResult
    data object Throttled : AthleteProfileResult
    data object Unauthorized : AthleteProfileResult
    data object Forbidden : AthleteProfileResult
    data class NetworkError(val cause: Throwable? = null) : AthleteProfileResult
    data class ServerError(val code: Int) : AthleteProfileResult
}

sealed interface MvcCalibrationResult {
    data class Success(val calibrations: List<MvcCalibration>) : MvcCalibrationResult
    data class ValidationError(val errors: Map<String, List<String>>) : MvcCalibrationResult
    data object Throttled : MvcCalibrationResult
    data object Unauthorized : MvcCalibrationResult
    data object Forbidden : MvcCalibrationResult
    data class NetworkError(val cause: Throwable? = null) : MvcCalibrationResult
    data class ServerError(val code: Int) : MvcCalibrationResult
}
