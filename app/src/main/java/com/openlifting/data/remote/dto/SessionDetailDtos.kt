package com.openlifting.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response from `GET /api/sessions/{id}` — full session tree (sets + reps with activations
 * inline + metrics + recommendations). Used to hydrate Room after a claim or for a resync
 * between devices. The flat `TrainingSessionDto` does NOT carry `sets[]`; this DTO is the
 * one that includes the nested array.
 */
@Serializable
data class SessionWithSetsDto(
    val id: Long,
    val exercise: String,
    @SerialName("started_at")    val startedAt: String,
    @SerialName("ended_at")      val endedAt: String? = null,
    @SerialName("device_source") val deviceSource: String,
    @SerialName("created_at")    val createdAt: String,
    val sets: List<NestedSetDto> = emptyList()
)

@Serializable
data class NestedSetDto(
    val id: Long,
    @SerialName("session_id")  val sessionId: Long,
    @SerialName("set_number")  val setNumber: Int,
    @SerialName("load_kg")     val loadKg: Double,
    @SerialName("target_reps") val targetReps: Int,
    val variant: String,
    val depth: String,
    val rpe: Double,
    @SerialName("created_at")  val createdAt: String,
    val reps: List<NestedRepDto> = emptyList(),
    val metrics: NestedMetricsDto? = null,
    val recommendations: List<NestedRecommendationDto> = emptyList()
)

@Serializable
data class NestedRepDto(
    val id: Long,
    @SerialName("rep_number")  val repNumber: Int,
    @SerialName("duration_ms") val durationMs: Long = 0,
    val activations: List<NestedActivationDto> = emptyList()
)

@Serializable
data class NestedActivationDto(
    val muscle: String,
    val side: String,
    @SerialName("percent_mvc")      val percentMvc: Double,
    @SerialName("peak_percent_mvc") val peakPercentMvc: Double
)

@Serializable
data class NestedMetricsDto(
    @SerialName("bsa_vl_pct")              val bsaVlPct: Double,
    @SerialName("bsa_vm_pct")              val bsaVmPct: Double,
    @SerialName("bsa_gmax_pct")            val bsaGmaxPct: Double,
    @SerialName("bsa_es_pct")              val bsaEsPct: Double,
    @SerialName("hq_ratio")                val hqRatio: Double,
    @SerialName("es_gmax_ratio")           val esGmaxRatio: Double,
    @SerialName("intra_set_fatigue_ratio") val intraSetFatigueRatio: Double,
    @SerialName("thresholds_version")      val thresholdsVersion: Int = 1
)

@Serializable
data class NestedRecommendationDto(
    val id: Long,
    val text: String,
    val severity: String,
    val evidence: String? = null
)
