package com.openlifting.domain.model

/**
 * Bilateral pair of values for the same muscle, in %MVC.
 *
 * Used during live measurement (current snapshot per side) and for aggregated phase
 * summaries (avg / peak per side). Lives in domain so both data, presentation and
 * datasource layers can reference it without a cycle.
 */
data class MusclePair(val left: Float, val right: Float) {
    val avg: Float get() = (left + right) / 2f
    val max: Float get() = maxOf(left, right)
    val asymmetryPct: Float get() {
        val major = max
        return if (major <= 0f) 0f else ((major - minOf(left, right)) / major) * 100f
    }
}
