package com.openlifting.domain.model

/**
 * Métricas computadas sobre un set de sentadilla. Combina los valores agregados (BSA por
 * músculo, ratios) con propiedades calculadas que clasifican el riesgo según los thresholds
 * documentados en [`docs/decisiones-tecnicas.md`](../../../../../../../../../docs/decisiones-tecnicas.md) §3.
 *
 * El campo [thresholdsVersion] permite versionar cambios futuros de cortes sin invalidar el
 * historial: mediciones nuevas pasan a una versión nueva, viejas siguen marcadas con la versión
 * en la que fueron computadas.
 */
data class SetMetrics(
    val setLocalId: Long,
    // BSA per muscle (bilateral symmetry, %)
    val bsaVlPct: Float,
    val bsaVmPct: Float,
    val bsaGmaxPct: Float,
    val bsaEsPct: Float,
    // Ratios
    val hqRatio: Float,
    val esGmaxRatio: Float,
    val intraSetFatigueRatio: Float,
    val thresholdsVersion: Int = 1
) {
    val bsaWorstPct: Float get() = maxOf(bsaVlPct, bsaVmPct, bsaGmaxPct, bsaEsPct)

    val vlRisk: RiskLevel   get() = bsaRisk(bsaVlPct)
    val vmRisk: RiskLevel   get() = bsaRisk(bsaVmPct)
    val gmaxRisk: RiskLevel get() = bsaRisk(bsaGmaxPct)
    val esRisk: RiskLevel   get() = bsaRisk(bsaEsPct)

    /**
     * Clasificación de riesgo del ratio H:Q (Hamstring/Quadriceps).
     *
     * Thresholds:
     *  - <0.45 → RISK
     *  - 0.45–0.59 → MONITOR
     *  - ≥0.60 → NORMAL
     *
     * Origen: literatura isokinética clásica (Heiderscheit et al, Croisier 2008).
     * Indicador (NO predictor directo) de balance agonista/antagonista en la rodilla.
     * Ver `docs/decisiones-tecnicas.md` §3.2 (H:Q).
     */
    val hqRisk: RiskLevel get() = when {
        hqRatio < 0.45f -> RiskLevel.RISK
        hqRatio < 0.60f -> RiskLevel.MONITOR
        else            -> RiskLevel.NORMAL
    }

    /**
     * Clasificación de riesgo del ratio ES:GMax (Erector Spinae / Gluteus Maximus).
     *
     * Thresholds:
     *  - ≥2.0 → RISK
     *  - 1.5–1.99 → MONITOR
     *  - <1.5 → NORMAL
     *
     * Origen: Caterisano 2002 y derivados sobre activación muscular en sentadilla.
     * Detecta compensación lumbar — cuando el lumbar agarra la sobrecarga porque el
     * glúteo no recluta bien. Específico de sentadilla; no generalizable a otros
     * movimientos sin recalibración.
     * Ver `docs/decisiones-tecnicas.md` §3.2 (ES:GMax).
     */
    val esGmaxRisk: RiskLevel get() = when {
        esGmaxRatio >= 2.0f -> RiskLevel.RISK
        esGmaxRatio >= 1.5f -> RiskLevel.MONITOR
        else                -> RiskLevel.NORMAL
    }

    /**
     * Clasificación de riesgo de fatiga intra-serie.
     *
     * Threshold: >1.3 → RISK.
     *
     * Heurística: ratio entre el peak de la última repetición y el peak de la primera.
     * Si la última requiere más activación para mover la misma carga, hay fatiga
     * acumulada. La literatura usa también pendiente RMS y mediana de frecuencia
     * (que requieren análisis frecuencial); esta versión simplificada es suficiente
     * para alertas tempranas, no para análisis riguroso de fatiga.
     * Ver `docs/decisiones-tecnicas.md` §3.2 (Fatiga).
     */
    val fatigueRisk: RiskLevel get() =
        if (intraSetFatigueRatio > 1.3f) RiskLevel.RISK else RiskLevel.NORMAL

    /**
     * Riesgo agregado del set: el peor entre todas las métricas.
     * RISK si cualquier métrica está en RISK; MONITOR si la peor es MONITOR; NORMAL solo
     * si todas las métricas están en NORMAL.
     */
    val overallRisk: RiskLevel get() {
        val all = listOf(vlRisk, vmRisk, gmaxRisk, esRisk, hqRisk, esGmaxRisk, fatigueRisk)
        return when {
            all.any { it == RiskLevel.RISK }    -> RiskLevel.RISK
            all.any { it == RiskLevel.MONITOR } -> RiskLevel.MONITOR
            else                                -> RiskLevel.NORMAL
        }
    }

    /**
     * Clasificación BSA común a todos los músculos.
     *
     * Thresholds:
     *  - ≥15% → RISK (asimetría significativa)
     *  - 10–14.9% → MONITOR (alerta temprana)
     *  - <10% → NORMAL
     *
     * Origen: literatura de asimetría bilateral en atletas. La literatura usa cutoffs
     * entre 10–20% según contexto (atletas vs. población general); estos valores son
     * conservadores para detección temprana sin generar falsos positivos excesivos.
     * Ver `docs/decisiones-tecnicas.md` §3.2 (BSA).
     */
    private fun bsaRisk(bsa: Float): RiskLevel = when {
        bsa >= 15f -> RiskLevel.RISK
        bsa >= 10f -> RiskLevel.MONITOR
        else       -> RiskLevel.NORMAL
    }
}
