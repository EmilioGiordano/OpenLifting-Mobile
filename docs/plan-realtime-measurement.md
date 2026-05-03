# Plan — Realtime Measurement (post-instructor close)

Este documento es el contrato entre dos sesiones de trabajo paralelas:
- **Sesión mobile (Android Compose / Kotlin)** — implementa el cliente.
- **Sesión Python (mock server WebSocket)** — implementa la fuente de datos.

Ambas trabajan contra el **wire protocol** definido en §3. Si el protocolo se respeta, las sesiones pueden avanzar independientemente y se integran al final.

---

## 1. Contexto

Hoy la "medición" en el app es un spinner de 2s seguido de la pantalla de Analysis con todos los datos de golpe. No transmite la sensación de captura en tiempo real.

**Objetivo**: reemplazar la pantalla de Measuring por una vista activa de ~15-20s donde el atleta ve, rep a rep:
- Contador de repetición y temporizador
- Fase actual (excéntrica / isométrica / concéntrica)
- Activación muscular bilateral en tiempo real (5 músculos × izq/der)
- Gráfico tiempo real con la actividad de los últimos ~5 segundos
- Marcadores por rep capturada

Al terminar el set: transición a la pantalla de Analysis ya existente, sin cambios en su renderizado.

**Decisión arquitectónica clave**: la vista del mobile NO simula los datos. Los **consume desde un WebSocket externo**. La fuente de los datos es un script de Python local (que más adelante será reemplazado por el firmware del ESP32 conectado al mismo endpoint, sin cambios en el cliente).

---

## 2. Arquitectura

```
┌─────────────────────┐         WebSocket          ┌─────────────────────┐
│  Python mock server │  ────────────────────────► │  Android app        │
│  (tools/emg-mock-   │   Wire protocol §3         │  (WebSocketEmg-     │
│   server/)          │   Eventos JSON             │   Client)           │
│                     │                            │                     │
│  Genera datos       │                            │  Consume Flow<Emg-  │
│  realistas con      │  ◄────── start_set ─────── │  Event>             │
│  distribuciones     │                            │  Refresca UI live   │
│  literarias         │                            │                     │
└─────────────────────┘                            └─────────────────────┘
                                                            │
                                                            ▼
                                                   ┌──────────────────┐
                                                   │  ComputeSetMetri │
                                                   │  cs (ya existe)  │
                                                   └──────────────────┘
                                                            │
                                                            ▼
                                                   ┌──────────────────┐
                                                   │  Analysis screen │
                                                   │  (ya existe)     │
                                                   └──────────────────┘
```

**Conexión**: WebSocket sobre `ws://localhost:8765/emg` por defecto. Configurable en el app via build config / preferences.

**Iniciativa**: el cliente abre la conexión y manda un mensaje `start_set` con metadata. El servidor responde con un stream de eventos hasta `set_complete`.

**Fallback**: si la conexión WebSocket falla, el app cae al `Esp32Simulator` actual (que se mantiene como `EmgDataSource` alternativo en DI). El demo no se rompe si el script de Python no está corriendo.

---

## 3. Wire Protocol (CONTRATO ENTRE LAS DOS SESIONES)

### 3.1 Connection

- Endpoint: `ws://<host>:<port>/emg`
- Default: `ws://localhost:8765/emg`
- Sin autenticación (LAN-only / dev)
- JSON-encoded text frames (no binary)

### 3.2 Client → Server (un solo mensaje, al inicio del set)

```json
{
  "action": "start_set",
  "set_request_id": "client-uuid-v4",
  "load_kg": 100.0,
  "target_reps": 5,
  "variant": "LOW_BAR",
  "depth": "PARALLEL",
  "athlete_id": "user-123"
}
```

Campos:
- `action` (string, requerido): siempre `"start_set"` en este MVP.
- `set_request_id` (string, requerido): UUID generado por el cliente para correlacionar con los eventos del servidor.
- `load_kg` (float, requerido): peso de la barra.
- `target_reps` (int, requerido): cantidad de repeticiones esperadas.
- `variant` (string, requerido): `"LOW_BAR"` | `"HIGH_BAR"`.
- `depth` (string, requerido): `"ABOVE_PARALLEL"` | `"PARALLEL"` | `"BELOW_PARALLEL"`.
- `athlete_id` (string, opcional): identificador del atleta para contexto.

### 3.3 Server → Client (stream de eventos)

Todos los eventos llevan `type` (string), `set_id` (string, igual al `set_request_id` del cliente), y un `timestamp_ms` (long, epoch ms server-side).

#### 3.3.1 `set_started`

Emitido inmediatamente al recibir `start_set`.

```json
{
  "type": "set_started",
  "set_id": "client-uuid-v4",
  "timestamp_ms": 1729123456789,
  "target_reps": 5,
  "load_kg": 100.0
}
```

#### 3.3.2 `phase_started`

Emitido al inicio de cada fase de cada rep.

```json
{
  "type": "phase_started",
  "set_id": "client-uuid-v4",
  "timestamp_ms": 1729123457012,
  "rep": 1,
  "phase": "eccentric"
}
```

`phase` ∈ `"eccentric"` | `"isometric"` | `"concentric"`.

#### 3.3.3 `snapshot` (alta frecuencia, ~20Hz durante una fase)

Emitido continuamente durante una fase con la activación instantánea de los 5 músculos × 2 lados.

```json
{
  "type": "snapshot",
  "set_id": "client-uuid-v4",
  "timestamp_ms": 1729123457062,
  "rep": 1,
  "phase": "eccentric",
  "elapsed_phase_ms": 50,
  "muscles": {
    "VL":   { "L": 62.3, "R": 65.1 },
    "VM":   { "L": 67.2, "R": 70.4 },
    "GMax": { "L": 41.5, "R": 43.8 },
    "ES":   { "L": 47.0, "R": 48.3 },
    "BF":   { "L": 32.1, "R": 33.8 }
  }
}
```

Llaves de músculo: `VL`, `VM`, `GMax`, `ES`, `BF`.
Llaves de lado: `L`, `R`.
Valor: %MVC en float, rango realista 0..100, picos pueden alcanzar ~95-98%.

#### 3.3.4 `phase_complete`

Emitido al final de cada fase.

```json
{
  "type": "phase_complete",
  "set_id": "client-uuid-v4",
  "timestamp_ms": 1729123458862,
  "rep": 1,
  "phase": "eccentric",
  "duration_ms": 1800,
  "muscles_avg": {
    "VL":   { "L": 58.1, "R": 61.4 },
    "VM":   { "L": 64.2, "R": 67.0 },
    "GMax": { "L": 38.5, "R": 40.2 },
    "ES":   { "L": 49.1, "R": 50.0 },
    "BF":   { "L": 30.0, "R": 31.5 }
  },
  "muscles_peak": {
    "VL":   { "L": 78.0, "R": 82.0 },
    "VM":   { "L": 82.0, "R": 85.0 },
    "GMax": { "L": 52.0, "R": 55.0 },
    "ES":   { "L": 65.0, "R": 67.0 },
    "BF":   { "L": 45.0, "R": 47.0 }
  }
}
```

#### 3.3.5 `rep_complete`

Emitido al final de cada repetición (después del `phase_complete` de la fase concéntrica).

```json
{
  "type": "rep_complete",
  "set_id": "client-uuid-v4",
  "timestamp_ms": 1729123461012,
  "rep": 1,
  "total_duration_ms": 3300,
  "eccentric":  { "duration_ms": 1800, "muscles_avg": { ... }, "muscles_peak": { ... } },
  "isometric":  { "duration_ms": 300,  "muscles_avg": { ... }, "muscles_peak": { ... } },
  "concentric": { "duration_ms": 1200, "muscles_avg": { ... }, "muscles_peak": { ... } }
}
```

#### 3.3.6 `set_complete`

Emitido al final del set, incluye la lista completa de activaciones por rep en el formato que `ComputeSetMetrics` ya consume hoy.

```json
{
  "type": "set_complete",
  "set_id": "client-uuid-v4",
  "timestamp_ms": 1729123472012,
  "total_reps": 5,
  "activations_by_rep": [
    [
      { "muscle": "VL",   "side": "LEFT",  "percent_mvc": 67.0, "peak_percent_mvc": 78.0 },
      { "muscle": "VL",   "side": "RIGHT", "percent_mvc": 70.0, "peak_percent_mvc": 82.0 },
      { "muscle": "VM",   "side": "LEFT",  "percent_mvc": 71.0, "peak_percent_mvc": 84.0 },
      ...
    ],
    [
      ... (rep 2)
    ],
    ...
  ]
}
```

`activations_by_rep` es un array donde cada entry son las activaciones promediadas para una rep (5 músculos × 2 lados = 10 entries por rep).

Esta es la estructura que el cliente Android pasa a `ComputeSetMetrics` para producir el SetMetrics final.

#### 3.3.7 `error` (opcional)

Emitido si algo sale mal del lado del servidor.

```json
{
  "type": "error",
  "set_id": "client-uuid-v4",
  "timestamp_ms": 1729123472012,
  "code": "INVALID_PARAMS",
  "message": "target_reps must be > 0"
}
```

### 3.4 Tiempos esperados

Para una rep "normal" (configurable en el server):
- **Excéntrica**: 1.5-2.0s
- **Isométrica (parada abajo)**: 0.2-0.4s
- **Concéntrica**: 1.0-1.5s
- **Recuperación entre reps**: 0.8-1.2s (entre rep N `rep_complete` y rep N+1 `phase_started`)

Total por rep: ~3-4s. Para un set de 5 reps: ~16-20s.

### 3.5 Frecuencia de snapshots

20Hz (1 snapshot cada 50ms) durante las fases de movimiento. Durante la recuperación entre reps no se emiten snapshots.

---

## 4. Mobile — qué construye la sesión Android

### 4.1 Domain

Crear:
- `domain/datasource/EmgDataSource.kt` — interface con `fun streamSet(...): Flow<EmgEvent>`
- `domain/model/EmgEvent.kt` — sealed type con todos los eventos del protocolo

### 4.2 Data layer

- Refactor `Esp32Simulator` → implementar `EmgDataSource`. Emite el mismo flujo de eventos que el servidor Python (para fallback offline). Mantener distribuciones literarias actuales.
- Nuevo `WebSocketEmgClient` → implementación con OkHttp WebSocket. Maneja conexión, deserializa JSON via kotlinx.serialization, expone Flow<EmgEvent>. Cierra al recibir `set_complete` o ante error.
- Hilt binding: `EmgDataSource` → `WebSocketEmgClient` por defecto; en debug build / si conexión falla, fallback a `Esp32Simulator`.

### 4.3 Presentation

- `SessionViewModel`: reemplazar `measureSet(...)` que llama bulk `simulateSet()` por una versión que **consume `Flow<EmgEvent>`**. Por cada evento, transiciona estado:
  - `set_started` → entra a `MeasuringInProgress(rep=0, phase=null, ...)`
  - `phase_started` → actualiza fase actual
  - `snapshot` → actualiza barras live + agrega punto al chart
  - `phase_complete` → marca peaks en barras
  - `rep_complete` → incrementa contador, captura datos para el resumen
  - `set_complete` → toma `activations_by_rep`, llama a `ComputeSetMetrics` (igual que hoy), persiste, transiciona a `AnalysisReady`

- Nuevo substate `SessionUiState.MeasuringInProgress`:
  ```kotlin
  data class MeasuringInProgress(
      val currentRep: Int,
      val totalReps: Int,
      val phase: RepPhase?,
      val timeElapsedInPhaseMs: Long,
      val totalTimeMs: Long,
      val liveActivations: Map<Muscle, MusclePair>,
      val peaksThisRep: Map<Muscle, MusclePair>,
      val chartHistory: List<ChartPoint>,
      val capturedReps: List<RepSummary>
  ) : SessionUiState
  ```

- Nuevo screen `MeasuringInProgressContent`:
  - Header: contador "REP X / Y", timer mono, chip de fase (color por fase)
  - Set header (load kg × reps + chips) — reuso del SetHeader actual
  - 5 filas bilaterales con barras + número mono live + peak markers
  - Gráfico tiempo real Compose Canvas: 5 líneas (una por músculo, L/R promediado), últimos ~5s, scroll automático, marcadores verticales discretos por rep
  - Footer: status text dinámico ("Excéntrica", "Rep 3 capturado", etc) + botón Cancelar

- Reemplazar el actual `MeasuringContent` (que es el bloque estático del 60% fixed) por este nuevo content cuando el state sea `MeasuringInProgress`.

### 4.4 Tests

- `SessionViewModelTest`: nuevos casos para state machine consumiendo eventos:
  - `set_started` → estado `MeasuringInProgress(rep=0)`
  - `phase_started` → fase actualizada
  - `snapshot` → live activations actualizadas
  - `rep_complete` → contador incrementado, summary capturado
  - `set_complete` → transición a `AnalysisReady` con datos correctos
- Mock del `EmgDataSource` con un `MutableSharedFlow<EmgEvent>` para emitir eventos en orden controlado.

### 4.5 Out of scope para mobile en este batch

- Configuración de URL del WebSocket vía UI (queda hardcoded a `ws://localhost:8765/emg` o leído de BuildConfig por ahora).
- Reconexión automática si se pierde conexión a media medición (por ahora: error → fallback a simulator OR error toast).
- Backend Laravel (separate session, separate branch).

---

## 5. Python script — qué construye la sesión paralela

### 5.1 Estructura del proyecto

Carpeta nueva en el repo: `tools/emg-mock-server/`

```
tools/emg-mock-server/
├── README.md            # Cómo correrlo, protocolo, opciones de config
├── pyproject.toml       # Dependencias (Python 3.10+)
├── server.py            # Entry point
├── simulator.py         # Lógica de generación de datos
├── protocol.py          # Tipos JSON / dataclasses
└── tests/
    └── test_protocol.py
```

### 5.2 Stack

- Python 3.10+
- `websockets` library (~=12.0)
- `json` stdlib
- Sin frameworks pesados (FastAPI, etc) — la idea es que sea minimal y fácil de leer.

### 5.3 Comportamiento esperado

1. Levanta el server WebSocket en `localhost:8765` (configurable).
2. Acepta conexiones; espera mensaje `start_set`.
3. Al recibir `start_set`:
   a. Emite `set_started` inmediatamente.
   b. Por cada rep en 1..target_reps:
      - Emite `phase_started` (eccentric)
      - Loop a 20Hz por ~1.8s emitiendo `snapshot` (datos eccéntricos)
      - Emite `phase_complete` (eccentric) con avg + peaks de la fase
      - `phase_started` (isometric) → ~0.3s de snapshots con valores planos
      - `phase_complete` (isometric)
      - `phase_started` (concentric) → ~1.2s de snapshots con valores más altos
      - `phase_complete` (concentric)
      - Emite `rep_complete` con resumen de las 3 fases
      - `await asyncio.sleep(0.8)` para simular recuperación entre reps
   c. Después de la última rep, emite `set_complete` con `activations_by_rep` agregado.
   d. Cierra la conexión limpiamente.
4. Si el cliente desconecta antes: cancelar la generación, log en stderr.
5. Si recibe input inválido: emitir `error` event y cerrar.

### 5.4 Modelo de simulación

**Mismas medias literarias que `Esp32Simulator.kt` actual** (que están documentadas en `docs/decisiones-tecnicas.md` §5.2):
```
VL:   65% MVC (peak ~85)
VM:   69% MVC (peak ~88)
GMax: 44% MVC (peak ~62)
ES:   49% MVC (peak ~67)
BF:   35% MVC (peak ~52)
```

**Diferencias eccentric vs concentric**:
- Eccentric: ~75% de la media concéntrica (la fase de bajada activa menos)
- Isometric: similar a la transición eccentric→concentric
- Concentric: la media base (es la fase de "trabajo")

**Variabilidad**:
- Inter-rep variability: ±5% normal
- Inter-side imbalance: configurable, default 5% (lado izq menos activo que derecho)
- Noise: ±2% gaussian
- Fatigue accumulation: cada rep aumenta los peaks ligeramente (~1.5% por rep)

**Configurable via CLI o config file**:
- Imbalance % (default 5)
- Fatigue ramp (default on)
- Tiempos de fase (defaults arriba)
- MVC overrides per-muscle (para simular un atleta con calibración no-default)

### 5.5 README del script

Debe documentar:
- Cómo instalar (`pip install -r requirements.txt` o `pyproject.toml`)
- Cómo correr (`python server.py [--port 8765]`)
- Cómo testear con `wscat` o cliente JS rápido antes de probar contra el app
- Ejemplo de `start_set` payload + ejemplo de output stream
- Link a este doc para el protocol completo

### 5.6 Out of scope para Python en este batch

- Auth (LAN-only, sin tokens)
- Múltiples conexiones concurrentes (atender una a la vez es suficiente para demo)
- Persistencia de simulaciones
- TLS / wss:// (HTTP-only es ok para localhost)

---

## 6. Plan de integración

1. **Sesión Python primero termina o avanza sustancialmente** el script (ideal: ya corriendo + testeable con wscat/Postman antes de que el app lo consuma).
2. **Sesión Android** desarrolla con el `Esp32Simulator` actual como `EmgDataSource` durante el desarrollo del UI. La UI funciona sin necesidad del WS server.
3. **Punto de integración**: cuando ambas sesiones tengan algo testeable, swap del binding de Hilt en debug a `WebSocketEmgClient`. El UI queda igual; cambia la fuente.
4. **Demo final**: `python server.py` corriendo en background del laptop, app conectado a `ws://10.0.2.2:8765/emg` (10.0.2.2 = host máquina desde Android emulator, o IP LAN si es device físico).

---

## 7. Out of scope (todo)

- Backend Laravel real (otra sesión, otra branch).
- ESP32 hardware real (post-tesis o versión futura del producto).
- Autenticación del WebSocket.
- Multi-set streaming (cada set es una conexión separada por ahora).

---

## 8. Branches y commits

- Branch nueva: `feat/realtime-measurement` (off main, después del merge de instructor).
- El script Python puede vivir en la misma branch (`tools/emg-mock-server/`) o en branch separada `tools/emg-mock-server`. Mi recomendación: misma branch, así se mergea junto y queda claro el pairing app+server.
- Cada sesión commitea en su área de la branch sin pisarse: la sesión mobile en `app/`, la sesión Python en `tools/`.
- Tests del lado Python en su propia carpeta, no afectan al build de Gradle.
