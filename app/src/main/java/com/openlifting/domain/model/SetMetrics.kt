package com.openlifting.domain.model

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

    val hqRisk: RiskLevel get() = when {
        hqRatio < 0.45f -> RiskLevel.RISK
        hqRatio < 0.60f -> RiskLevel.MONITOR
        else            -> RiskLevel.NORMAL
    }

    val esGmaxRisk: RiskLevel get() = when {
        esGmaxRatio >= 2.0f -> RiskLevel.RISK
        esGmaxRatio >= 1.5f -> RiskLevel.MONITOR
        else                -> RiskLevel.NORMAL
    }

    val fatigueRisk: RiskLevel get() =
        if (intraSetFatigueRatio > 1.3f) RiskLevel.RISK else RiskLevel.NORMAL

    val overallRisk: RiskLevel get() {
        val all = listOf(vlRisk, vmRisk, gmaxRisk, esRisk, hqRisk, esGmaxRisk, fatigueRisk)
        return when {
            all.any { it == RiskLevel.RISK }    -> RiskLevel.RISK
            all.any { it == RiskLevel.MONITOR } -> RiskLevel.MONITOR
            else                                -> RiskLevel.NORMAL
        }
    }

    private fun bsaRisk(bsa: Float): RiskLevel = when {
        bsa >= 15f -> RiskLevel.RISK
        bsa >= 10f -> RiskLevel.MONITOR
        else       -> RiskLevel.NORMAL
    }
}
