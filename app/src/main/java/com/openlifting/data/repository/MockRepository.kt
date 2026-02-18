package com.openlifting.data.repository

import com.openlifting.data.model.*
import java.time.LocalDateTime
import kotlin.random.Random

object MockRepository {

    private val random = Random(42)

    private var userProfile = UserProfile(
        name = "Juan Perez",
        email = "juan@email.com",
        heightCm = 175,
        weightKg = 80f,
        age = 28
    )

    private val sessions: List<Session> = generateSessions()

    fun getSessions(): List<Session> = sessions

    fun getSessionById(id: String): Session? = sessions.find { it.id == id }

    fun getUserProfile(): UserProfile = userProfile

    fun updateUserProfile(profile: UserProfile) {
        userProfile = profile
    }

    fun getLatestSession(): Session? = sessions.maxByOrNull { it.date }

    private fun generateSessions(): List<Session> {
        return (1..8).map { sessionIndex ->
            Session(
                id = "session_$sessionIndex",
                date = LocalDateTime.now().minusDays((8 - sessionIndex).toLong()),
                series = generateSeries(sessionIndex)
            )
        }
    }

    private fun generateSeries(sessionIndex: Int): List<Series> {
        val baseSeries = listOf(60f, 80f, 100f, 100f, 80f)
        return baseSeries.mapIndexed { index, weight ->
            Series(
                number = index + 1,
                weightKg = weight,
                repetitions = generateRepetitions(
                    count = if (weight >= 100f) 3 else 5,
                    weight = weight,
                    fatigueBase = index * 5f,
                    sessionIndex = sessionIndex
                )
            )
        }
    }

    private fun generateRepetitions(
        count: Int,
        weight: Float,
        fatigueBase: Float,
        sessionIndex: Int
    ): List<Repetition> {
        return (1..count).map { repNumber ->
            val fatigueFactor = fatigueBase + (repNumber - 1) * 3f
            val weightFactor = weight / 100f

            Repetition(
                number = repNumber,
                emgReading = generateEmgReading(
                    fatigueFactor = fatigueFactor,
                    weightFactor = weightFactor,
                    sessionIndex = sessionIndex
                )
            )
        }
    }

    private fun generateEmgReading(
        fatigueFactor: Float,
        weightFactor: Float,
        sessionIndex: Int
    ): EmgReading {
        val baseQuad = 65f * weightFactor
        val baseGlute = 55f * weightFactor
        val baseHam = 35f * weightFactor
        val baseLowBack = 25f * weightFactor

        // Simular desbalance bilateral progresivo (lado derecho ligeramente mas fuerte)
        val bilateralImbalance = if (sessionIndex <= 3) 8f else 4f

        // Con fatiga, la zona lumbar compensa mas
        val fatigueCompensation = fatigueFactor * 0.3f

        return EmgReading(
            quadricepsLeft = clampPercent(baseQuad - bilateralImbalance / 2 + randomVariation()),
            quadricepsRight = clampPercent(baseQuad + bilateralImbalance / 2 + randomVariation()),
            glutesLeft = clampPercent(baseGlute - fatigueFactor * 0.2f + randomVariation()),
            glutesRight = clampPercent(baseGlute - fatigueFactor * 0.15f + randomVariation()),
            hamstringsLeft = clampPercent(baseHam + randomVariation()),
            hamstringsRight = clampPercent(baseHam + bilateralImbalance * 0.3f + randomVariation()),
            lowerBackLeft = clampPercent(baseLowBack + fatigueCompensation + randomVariation()),
            lowerBackRight = clampPercent(baseLowBack + fatigueCompensation + randomVariation())
        )
    }

    private fun randomVariation(): Float = random.nextFloat() * 6f - 3f

    private fun clampPercent(value: Float): Float = value.coerceIn(0f, 100f)

    // Utilidades de analisis

    fun calculateBilateralDifference(reading: EmgReading, muscle: MuscleGroup): Float {
        return when (muscle) {
            MuscleGroup.QUADRICEPS -> kotlin.math.abs(reading.quadricepsLeft - reading.quadricepsRight)
            MuscleGroup.GLUTES -> kotlin.math.abs(reading.glutesLeft - reading.glutesRight)
            MuscleGroup.HAMSTRINGS -> kotlin.math.abs(reading.hamstringsLeft - reading.hamstringsRight)
            MuscleGroup.LOWER_BACK -> kotlin.math.abs(reading.lowerBackLeft - reading.lowerBackRight)
        }
    }

    fun getSeriesBalanceStatus(series: Series): BalanceStatus {
        val maxImbalance = series.repetitions.maxOf { rep ->
            MuscleGroup.entries.maxOf { muscle ->
                calculateBilateralDifference(rep.emgReading, muscle)
            }
        }
        return when {
            maxImbalance > 15f -> BalanceStatus.ALERT
            maxImbalance > 8f -> BalanceStatus.WARNING
            else -> BalanceStatus.GOOD
        }
    }

    fun getSessionBalanceStatus(session: Session): BalanceStatus {
        val statuses = session.series.map { getSeriesBalanceStatus(it) }
        return when {
            statuses.any { it == BalanceStatus.ALERT } -> BalanceStatus.ALERT
            statuses.any { it == BalanceStatus.WARNING } -> BalanceStatus.WARNING
            else -> BalanceStatus.GOOD
        }
    }

    fun getSessionAlertSummary(session: Session): String? {
        val issues = mutableListOf<String>()
        val lastSeries = session.series.lastOrNull() ?: return null
        val avgReading = averageReading(lastSeries)

        val avgLowBack = (avgReading.lowerBackLeft + avgReading.lowerBackRight) / 2
        val avgGlute = (avgReading.glutesLeft + avgReading.glutesRight) / 2
        if (avgLowBack > avgGlute * 0.7f) {
            issues.add("Compensacion lumbar")
        }

        for (muscle in MuscleGroup.entries) {
            val diff = calculateBilateralDifference(avgReading, muscle)
            if (diff > 10f) {
                issues.add("Desbalance en ${muscle.displayName}")
            }
        }

        if (session.series.size >= 3) {
            val firstStatus = getSeriesBalanceStatus(session.series.first())
            val lastStatus = getSeriesBalanceStatus(session.series.last())
            if (firstStatus == BalanceStatus.GOOD && lastStatus != BalanceStatus.GOOD) {
                issues.add("Fatiga progresiva")
            }
        }

        return if (issues.isEmpty()) null else issues.joinToString(" | ")
    }

    fun generateRecommendations(session: Session): List<String> {
        val recommendations = mutableListOf<String>()
        val lastSeries = session.series.lastOrNull() ?: return recommendations
        val avgReading = averageReading(lastSeries)

        // Detectar compensacion lumbar
        val avgLowBack = (avgReading.lowerBackLeft + avgReading.lowerBackRight) / 2
        val avgGlute = (avgReading.glutesLeft + avgReading.glutesRight) / 2
        if (avgLowBack > avgGlute * 0.7f) {
            recommendations.add("Zona lumbar sobrecargada respecto a gluteos. Focalice la fuerza en gluteos y trabaje movilidad de cadera.")
        }

        // Detectar desbalance bilateral
        for (muscle in MuscleGroup.entries) {
            val diff = calculateBilateralDifference(avgReading, muscle)
            if (diff > 10f) {
                val side = when (muscle) {
                    MuscleGroup.QUADRICEPS -> if (avgReading.quadricepsLeft < avgReading.quadricepsRight) "izquierdo" else "derecho"
                    MuscleGroup.GLUTES -> if (avgReading.glutesLeft < avgReading.glutesRight) "izquierdo" else "derecho"
                    MuscleGroup.HAMSTRINGS -> if (avgReading.hamstringsLeft < avgReading.hamstringsRight) "izquierdo" else "derecho"
                    MuscleGroup.LOWER_BACK -> if (avgReading.lowerBackLeft < avgReading.lowerBackRight) "izquierdo" else "derecho"
                }
                recommendations.add("Desbalance en ${muscle.displayName}: lado $side con menor activacion. Considere ejercicios unilaterales.")
            }
        }

        // Detectar fatiga progresiva
        if (session.series.size >= 3) {
            val firstStatus = getSeriesBalanceStatus(session.series.first())
            val lastStatus = getSeriesBalanceStatus(session.series.last())
            if (firstStatus == BalanceStatus.GOOD && lastStatus != BalanceStatus.GOOD) {
                recommendations.add("El balance muscular se degrada con la fatiga. Considere reducir el volumen o descansar mas entre series.")
            }
        }

        if (recommendations.isEmpty()) {
            recommendations.add("Buen balance muscular general. Continue con el plan de entrenamiento actual.")
        }

        return recommendations
    }

    // Scoring y metricas avanzadas

    fun getRepQualityScore(rep: Repetition): Int {
        val reading = rep.emgReading

        // Simetria (0-50): penaliza desbalance bilateral
        val maxImbalance = MuscleGroup.entries.maxOf { calculateBilateralDifference(reading, it) }
        val symmetryScore = (50f - maxImbalance * 2.5f).coerceIn(0f, 50f)

        // Compensacion (0-30): penaliza sobrecarga lumbar vs gluteos
        val avgLumbar = (reading.lowerBackLeft + reading.lowerBackRight) / 2f
        val avgGlute = (reading.glutesLeft + reading.glutesRight) / 2f
        val compRatio = if (avgGlute > 1f) avgLumbar / avgGlute else 2f
        val compensationScore = ((1.5f - compRatio.coerceIn(0f, 1.5f)) / 1.5f * 30f)

        // Calidad de activacion (0-20): motores primarios deben dominar
        val avgQuad = (reading.quadricepsLeft + reading.quadricepsRight) / 2f
        val avgHam = (reading.hamstringsLeft + reading.hamstringsRight) / 2f
        val primaryAvg = (avgQuad + avgGlute) / 2f
        val secondaryAvg = (avgHam + avgLumbar) / 2f
        val dominanceRatio = if (secondaryAvg > 0) primaryAvg / secondaryAvg else 3f
        val qualityScore = ((dominanceRatio - 0.5f) / 2f * 20f).coerceIn(0f, 20f)

        return (symmetryScore + compensationScore + qualityScore).toInt().coerceIn(0, 100)
    }

    fun getSeriesQualityScore(series: Series): Int {
        return series.repetitions.map { getRepQualityScore(it) }.average().toInt()
    }

    fun getSeriesSymmetryIndex(series: Series): Float {
        val avgImbalance = series.repetitions.flatMap { rep ->
            MuscleGroup.entries.map { calculateBilateralDifference(rep.emgReading, it) }
        }.average()
        return (100f - avgImbalance.toFloat() * 2f).coerceIn(0f, 100f)
    }

    fun getSeriesCompensationIndex(series: Series): Float {
        return series.repetitions.map { rep ->
            val avgLumbar = (rep.emgReading.lowerBackLeft + rep.emgReading.lowerBackRight) / 2f
            val avgGlute = (rep.emgReading.glutesLeft + rep.emgReading.glutesRight) / 2f
            if (avgGlute > 1f) avgLumbar / avgGlute else 2f
        }.average().toFloat()
    }

    data class PeakActivation(
        val muscle: MuscleGroup,
        val rep: Int,
        val value: Float,
        val side: String // "Izq" o "Der"
    )

    fun getSeriesPeakActivation(series: Series): PeakActivation {
        var peakMuscle = MuscleGroup.QUADRICEPS
        var peakRep = 1
        var peakValue = 0f
        var peakSide = "Izq"

        series.repetitions.forEach { rep ->
            val reading = rep.emgReading
            val activations = listOf(
                Triple(MuscleGroup.QUADRICEPS, reading.quadricepsLeft, "Izq"),
                Triple(MuscleGroup.QUADRICEPS, reading.quadricepsRight, "Der"),
                Triple(MuscleGroup.GLUTES, reading.glutesLeft, "Izq"),
                Triple(MuscleGroup.GLUTES, reading.glutesRight, "Der"),
                Triple(MuscleGroup.HAMSTRINGS, reading.hamstringsLeft, "Izq"),
                Triple(MuscleGroup.HAMSTRINGS, reading.hamstringsRight, "Der"),
                Triple(MuscleGroup.LOWER_BACK, reading.lowerBackLeft, "Izq"),
                Triple(MuscleGroup.LOWER_BACK, reading.lowerBackRight, "Der")
            )
            activations.forEach { (muscle, value, side) ->
                if (value > peakValue) {
                    peakValue = value
                    peakMuscle = muscle
                    peakRep = rep.number
                    peakSide = side
                }
            }
        }
        return PeakActivation(peakMuscle, peakRep, peakValue, peakSide)
    }

    fun getRepAverageActivations(rep: Repetition): Map<MuscleGroup, Float> {
        val reading = rep.emgReading
        return mapOf(
            MuscleGroup.QUADRICEPS to (reading.quadricepsLeft + reading.quadricepsRight) / 2f,
            MuscleGroup.GLUTES to (reading.glutesLeft + reading.glutesRight) / 2f,
            MuscleGroup.HAMSTRINGS to (reading.hamstringsLeft + reading.hamstringsRight) / 2f,
            MuscleGroup.LOWER_BACK to (reading.lowerBackLeft + reading.lowerBackRight) / 2f
        )
    }

    private fun averageReading(series: Series): EmgReading {
        val reps = series.repetitions
        val n = reps.size.toFloat()
        return EmgReading(
            quadricepsLeft = reps.sumOf { it.emgReading.quadricepsLeft.toDouble() }.toFloat() / n,
            quadricepsRight = reps.sumOf { it.emgReading.quadricepsRight.toDouble() }.toFloat() / n,
            glutesLeft = reps.sumOf { it.emgReading.glutesLeft.toDouble() }.toFloat() / n,
            glutesRight = reps.sumOf { it.emgReading.glutesRight.toDouble() }.toFloat() / n,
            hamstringsLeft = reps.sumOf { it.emgReading.hamstringsLeft.toDouble() }.toFloat() / n,
            hamstringsRight = reps.sumOf { it.emgReading.hamstringsRight.toDouble() }.toFloat() / n,
            lowerBackLeft = reps.sumOf { it.emgReading.lowerBackLeft.toDouble() }.toFloat() / n,
            lowerBackRight = reps.sumOf { it.emgReading.lowerBackRight.toDouble() }.toFloat() / n
        )
    }
}
