package com.openlifting.data.websocket

import com.openlifting.domain.datasource.EmgDataSource
import com.openlifting.domain.datasource.StartSetRequest
import com.openlifting.domain.model.EmgEvent
import com.openlifting.domain.model.Muscle
import com.openlifting.domain.model.MusclePair
import com.openlifting.domain.model.MuscleSide
import com.openlifting.domain.model.PhaseSummary
import com.openlifting.domain.model.RepPhase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketEmgClient @Inject constructor() : EmgDataSource {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override fun streamSet(request: StartSetRequest): Flow<EmgEvent> = callbackFlow {
        val wsUrl = "ws://${HOST}:${PORT}/emg"
        val setId = request.setRequestId

        val startPayload = JSONObject().apply {
            put("action", "start_set")
            put("set_request_id", request.setRequestId)
            put("load_kg", request.loadKg.toDouble())
            put("target_reps", request.targetReps)
            put("variant", request.variant.name)
            put("depth", request.depth.name)
            request.rpe?.let { put("rpe", it.toDouble()) }
            request.athleteId?.let { put("athlete_id", it) }
        }

        val webSocket = client.newWebSocket(
            Request.Builder().url(wsUrl).build(),
            object : WebSocketListener() {
                private var isClosed = false

                override fun onOpen(webSocket: WebSocket, response: Response) {
                    val sent = webSocket.send(startPayload.toString())
                    if (!sent) {
                        trySend(EmgEvent.Error(setId = setId, code = "SEND_FAILED", message = "No se pudo enviar start_set"))
                        webSocket.close(1000, "client done")
                    }
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    if (isClosed) return

                    try {
                        val json = JSONObject(text)
                        val type = json.optString("type", "")
                        val event = parseEvent(json, type, setId)
                        if (event != null) {
                            trySend(event)
                            if (event is EmgEvent.SetComplete || event is EmgEvent.Error) {
                                isClosed = true
                                webSocket.close(1000, "set complete")
                            }
                        }
                    } catch (e: Exception) {
                        trySend(EmgEvent.Error(setId = setId, code = "PARSE_ERROR", message = "Error al parsear: ${e.message}"))
                        isClosed = true
                        webSocket.close(1001, "parse error")
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    trySend(EmgEvent.Error(setId = setId, code = "CONNECTION_FAILED", message = t.message ?: "Conexión fallida"))
                    close()
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    close()
                }
            }
        )

        awaitClose {
            webSocket.close(1000, "cancelled")
        }
    }

    private fun parseEvent(json: JSONObject, type: String, setId: String): EmgEvent? {
        return when (type) {
            "set_started" -> parseSetStarted(json, setId)
            "phase_started" -> parsePhaseStarted(json, setId)
            "snapshot" -> parseSnapshot(json, setId)
            "phase_complete" -> parsePhaseComplete(json, setId)
            "rep_complete" -> parseRepComplete(json, setId)
            "set_complete" -> parseSetComplete(json, setId)
            "error" -> parseError(json, setId)
            else -> null
        }
    }

    private fun parseSetStarted(json: JSONObject, setId: String): EmgEvent {
        return EmgEvent.SetStarted(
            setId = setId,
            targetReps = json.optInt("target_reps", 0),
            loadKg = json.optDouble("load_kg", 0.0).toFloat(),
            rpe = if (json.has("rpe")) json.optDouble("rpe", 7.0).toFloat() else null
        )
    }

    private fun parsePhaseStarted(json: JSONObject, setId: String): EmgEvent {
        return EmgEvent.PhaseStarted(
            setId = setId,
            rep = json.optInt("rep", 0),
            phase = parsePhase(json.optString("phase", ""))
        )
    }

    private fun parseSnapshot(json: JSONObject, setId: String): EmgEvent {
        val musclesJson = json.optJSONObject("muscles") ?: JSONObject()
        val muscles = mutableMapOf<Muscle, MusclePair>()
        musclesJson.keys().forEach { key ->
            val muscle = parseMuscle(key) ?: return@forEach
            val sideJson = musclesJson.optJSONObject(key) ?: return@forEach
            val pair = MusclePair(
                left = sideJson.optDouble("L", 0.0).toFloat(),
                right = sideJson.optDouble("R", 0.0).toFloat()
            )
            muscles[muscle] = pair
        }
        return EmgEvent.Snapshot(
            setId = setId,
            rep = json.optInt("rep", 0),
            phase = parsePhase(json.optString("phase", "")),
            elapsedPhaseMs = json.optLong("elapsed_phase_ms", 0),
            muscles = muscles
        )
    }

    private fun parsePhaseComplete(json: JSONObject, setId: String): EmgEvent {
        val avgJson = json.optJSONObject("muscles_avg") ?: JSONObject()
        val peakJson = json.optJSONObject("muscles_peak") ?: JSONObject()
        val musclesAvg = parseMusclePairs(avgJson)
        val musclesPeak = parseMusclePairs(peakJson)
        return EmgEvent.PhaseComplete(
            setId = setId,
            rep = json.optInt("rep", 0),
            phase = parsePhase(json.optString("phase", "")),
            durationMs = json.optLong("duration_ms", 0),
            musclesAvg = musclesAvg,
            musclesPeak = musclesPeak
        )
    }

    private fun parseRepComplete(json: JSONObject, setId: String): EmgEvent {
        val eccentric = parsePhaseData(json.optJSONObject("eccentric"))
        val isometric = parsePhaseData(json.optJSONObject("isometric"))
        val concentric = parsePhaseData(json.optJSONObject("concentric"))
        return EmgEvent.RepComplete(
            setId = setId,
            rep = json.optInt("rep", 0),
            totalDurationMs = json.optLong("total_duration_ms", 0),
            eccentric = eccentric,
            isometric = isometric,
            concentric = concentric
        )
    }

    private fun parsePhaseData(json: JSONObject?): PhaseSummary {
        if (json == null) return PhaseSummary(0, emptyMap(), emptyMap())
        val avgJson = json.optJSONObject("muscles_avg") ?: JSONObject()
        val peakJson = json.optJSONObject("muscles_peak") ?: JSONObject()
        return PhaseSummary(
            durationMs = json.optLong("duration_ms", 0),
            musclesAvg = parseMusclePairs(avgJson),
            musclesPeak = parseMusclePairs(peakJson)
        )
    }

    private fun parseMusclePairs(json: JSONObject): Map<Muscle, MusclePair> {
        val result = mutableMapOf<Muscle, MusclePair>()
        json.keys().forEach { key ->
            val muscle = parseMuscle(key) ?: return@forEach
            val sideJson = json.optJSONObject(key) ?: return@forEach
            result[muscle] = MusclePair(
                left = sideJson.optDouble("L", 0.0).toFloat(),
                right = sideJson.optDouble("R", 0.0).toFloat()
            )
        }
        return result
    }

    private fun parseSetComplete(json: JSONObject, setId: String): EmgEvent {
        val activationsArray = json.optJSONArray("activations_by_rep") ?: return EmgEvent.Error(
            setId = setId, code = "INVALID_DATA", message = "No activations"
        )
        val activationsByRep = mutableListOf<List<com.openlifting.domain.model.MuscleActivation>>()
        for (i in 0 until activationsArray.length()) {
            val repArray = activationsArray.optJSONArray(i) ?: continue
            val repsList = mutableListOf<com.openlifting.domain.model.MuscleActivation>()
            for (j in 0 until repArray.length()) {
                val actObj = repArray.optJSONObject(j) ?: continue
                val activation = com.openlifting.domain.model.MuscleActivation(
                    repId = 0L,
                    muscle = parseMuscle(actObj.optString("muscle", "")) ?: Muscle.VASTUS_LATERALIS,
                    side = parseSide(actObj.optString("side", "")),
                    percentMvc = actObj.optDouble("percent_mvc", 0.0).toFloat(),
                    peakPercentMvc = actObj.optDouble("peak_percent_mvc", 0.0).toFloat()
                )
                repsList.add(activation)
            }
            activationsByRep.add(repsList)
        }
        return EmgEvent.SetComplete(
            setId = setId,
            totalReps = json.optInt("total_reps", 0),
            activationsByRep = activationsByRep
        )
    }

    private fun parseError(json: JSONObject, setId: String): EmgEvent {
        return EmgEvent.Error(
            setId = setId,
            code = json.optString("code", "UNKNOWN"),
            message = json.optString("message", "Error desconocido")
        )
    }

    private fun parsePhase(s: String): RepPhase = when (s.lowercase()) {
        "eccentric" -> RepPhase.ECCENTRIC
        "isometric" -> RepPhase.ISOMETRIC
        "concentric" -> RepPhase.CONCENTRIC
        else -> RepPhase.CONCENTRIC
    }

    private fun parseMuscle(s: String): Muscle? = when (s.uppercase()) {
        "VL" -> Muscle.VASTUS_LATERALIS
        "VM" -> Muscle.VASTUS_MEDIALIS
        "GMAX", "GMAX" -> Muscle.GLUTEUS_MAXIMUS
        "ES" -> Muscle.ERECTOR_SPINAE
        "BF" -> Muscle.BICEPS_FEMORIS
        else -> null
    }

    private fun parseSide(s: String): MuscleSide = when (s.uppercase()) {
        "LEFT", "L" -> MuscleSide.LEFT
        "RIGHT", "R" -> MuscleSide.RIGHT
        else -> MuscleSide.LEFT
    }

    private companion object {
        const val HOST = "10.0.2.2"
        const val PORT = 8765
    }
}