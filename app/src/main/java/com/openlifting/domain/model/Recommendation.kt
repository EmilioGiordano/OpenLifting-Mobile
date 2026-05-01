package com.openlifting.domain.model

data class Recommendation(
    val id: Long = 0,
    val setLocalId: Long,
    val text: String,
    val severity: RiskLevel,
    val evidence: String = ""
)
