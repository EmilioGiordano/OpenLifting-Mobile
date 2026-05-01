package com.openlifting.domain.model

enum class SquatVariant(val displayName: String) {
    LOW_BAR("Low Bar"),
    HIGH_BAR("High Bar")
}

enum class SquatDepth(val displayName: String) {
    ABOVE_PARALLEL("Sobre paralela"),
    PARALLEL("Paralela"),
    BELOW_PARALLEL("Bajo paralela")
}

enum class DeviceSource { REAL, SIMULATED }
