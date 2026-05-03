package com.openlifting.domain.model

/**
 * Phase of a single repetition during a squat.
 *
 * Mirrors the segmentation that the ESP32 firmware (or its Python mock) reports per rep.
 * The amplitude profile differs per phase — eccentric (descent) is typically lower
 * amplitude and longer duration, isometric is brief, concentric (ascent) is the
 * higher-amplitude "working" phase.
 */
enum class RepPhase(val displayName: String) {
    ECCENTRIC("Excéntrica"),
    ISOMETRIC("Parada"),
    CONCENTRIC("Concéntrica")
}
