package com.openlifting.data.websocket

import com.openlifting.data.simulator.Esp32Simulator
import com.openlifting.domain.datasource.EmgDataSource
import com.openlifting.domain.datasource.StartSetRequest
import com.openlifting.domain.model.EmgEvent
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

/**
 * Tries [WebSocketEmgClient] first; falls back to [Esp32Simulator] if:
 *  - the WS connection fails (exception), or
 *  - the first event received is an [EmgEvent.Error] (server-side reject before any data), or
 *  - no data event arrives within [FALLBACK_TIMEOUT_MS] (server unreachable / silent).
 *
 * Once a non-error event arrives from the WS the fallback is permanently disabled for that set.
 * Call [fallbackUsed] after the set to check which source was used (e.g. to show a banner).
 */
@Singleton
class EmgDataSourceWithFallback @Inject constructor(
    private val webSocketClient: WebSocketEmgClient,
    private val simulator: Esp32Simulator,
) : EmgDataSource {

    @Volatile private var _fallbackUsed = false
    @Volatile private var _fallbackMessage = ""

    fun fallbackUsed(): Boolean = _fallbackUsed
    fun fallbackMessage(): String = _fallbackMessage

    override fun streamSet(request: StartSetRequest): Flow<EmgEvent> = callbackFlow {
        _fallbackUsed = false
        _fallbackMessage = ""

        // true once the WS has sent at least one non-Error event
        val receivedDataEvent = AtomicBoolean(false)
        // ensures only one path (WS failure vs timeout) starts the simulator
        val fallbackStarted = AtomicBoolean(false)

        suspend fun runSimulatorFallback() {
            _fallbackUsed = true
            _fallbackMessage = "Sin conexión al sensor — usando simulación"
            try {
                simulator.streamSet(request).collect { trySend(it) }
            } catch (_: Exception) {}
            close()
        }

        // WS collection job — child of callbackFlow scope, cancelled by awaitClose
        val wsJob = launch(Dispatchers.IO) {
            var setFinished = false
            try {
                webSocketClient.streamSet(request).collect { event ->
                    val isData = event !is EmgEvent.Error
                    if (isData) receivedDataEvent.set(true)

                    if (receivedDataEvent.get()) {
                        // WS is alive — forward all events including subsequent errors
                        trySend(event)
                        if (event is EmgEvent.SetComplete || event is EmgEvent.Error) {
                            setFinished = true
                            close()
                        }
                    }
                    // If first event is Error: don't forward; let collect end; fallback below
                }
            } catch (_: CancellationException) {
                // Cancelled by timeout job — expected, don't fallback from here
                return@launch
            } catch (_: Exception) {
                // WS threw before producing any data
            }

            // Collect ended without producing usable data → fallback if timeout hasn't already
            if (!setFinished && !receivedDataEvent.get() &&
                fallbackStarted.compareAndSet(false, true)
            ) {
                runSimulatorFallback()
            }
        }

        // Timeout job — if WS hasn't produced a data event in time, cancel it and fallback
        val timeoutJob = launch {
            delay(FALLBACK_TIMEOUT_MS)
            if (!receivedDataEvent.get() && fallbackStarted.compareAndSet(false, true)) {
                wsJob.cancel()
                runSimulatorFallback()
            }
        }

        awaitClose {
            wsJob.cancel()
            timeoutJob.cancel()
        }
    }

    private companion object {
        const val FALLBACK_TIMEOUT_MS = 2_000L
    }
}
