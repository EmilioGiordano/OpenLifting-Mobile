package com.openlifting.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Nested payload for `POST /api/sessions/{session_id}/sets`.
 * One request carries: the set, its reps, every activation per rep, the computed
 * metrics and the recommendations — backend persists everything in a single
 * transaction. Idempotency is keyed on `(session_id, set_number)`.
 */
@Serializable
data class PostSetRequest(
    @SerialName("set_number")  val setNumber: Int,
    @SerialName("load_kg")     val loadKg: Double,
    @SerialName("target_reps") val targetReps: Int,
    val variant: String,
    val depth: String,
    val rpe: Double,
    val reps: List<RepRequest>,
    val metrics: MetricsRequest,
    val recommendations: List<RecommendationRequest> = emptyList()
)

@Serializable
data class RepRequest(
    @SerialName("rep_number")  val repNumber: Int,
    @SerialName("duration_ms") val durationMs: Long = 0,
    val activations: List<ActivationRequest>
)

@Serializable
data class ActivationRequest(
    val muscle: String,
    val side: String,
    @SerialName("percent_mvc")      val percentMvc: Double,
    @SerialName("peak_percent_mvc") val peakPercentMvc: Double
)

@Serializable
data class MetricsRequest(
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
data class RecommendationRequest(
    val text: String,
    val severity: String,
    val evidence: String? = null
)

// ── Response ──────────────────────────────────────────────────────────────

/**
 * Response from `POST /sets`. We only persist the top-level `id` to mark the
 * local TrainingSet as synced — rep / activation / recommendation ids are
 * not mirrored locally because no client flow needs them.
 */
@Serializable
data class PostSetResponse(
    val id: Long,
    @SerialName("session_id") val sessionId: Long,
    @SerialName("set_number") val setNumber: Int
)

// ── PATCH /api/sessions/{id} ──────────────────────────────────────────────

@Serializable
data class PatchSessionRequest(
    @SerialName("device_source") val deviceSource: String
)
