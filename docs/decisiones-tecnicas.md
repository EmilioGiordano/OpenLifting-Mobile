# Decisiones técnicas — OpenLifting Mobile

Documento de decisiones para la entrega de **Computación Móvil**. Pensado como referencia rápida para defender el porqué de cada elección durante el final. Las decisiones están agrupadas por capa.

---

## 1. Arquitectura general

### 1.1 MVVM + Clean Architecture "lite"

**Decisión**: arquitectura MVVM con boundaries de Clean Architecture (`presentation -> domain <- data`), pero en un único módulo Gradle.

**Por qué**:
- MVVM es el patrón canónico recomendado por Google para apps Android modernas con Jetpack Compose.
- Las boundaries de Clean (presentation desconoce data, domain no depende de Android/Room/Retrofit) hacen el código testeable y portable.
- Multi-módulo Gradle (Clean "puro") agrega tiempo de build y complejidad de configuración sin beneficio claro para un proyecto de un solo dev y 18 días.

**Trade-off aceptado**: cuando el proyecto crezca (post-entrega), partir el módulo en `:domain`, `:data`, `:presentation` es un refactor mecánico, no de rediseño.

### 1.2 Jetpack Compose + Material 3

**Decisión**: UI 100% en Compose, sobre Material 3.

**Por qué**:
- Compose es la dirección oficial de Google para nuevas apps Android — XML está en mantenimiento.
- Modelo declarativo: menos boilerplate, mejor manejo de estado (Flow → State), tema dinámico nativo.
- Material 3 tiene first-class support en Compose y un sistema de color slots semántico (primary, surface, onSurface, etc) que mapea limpio al sistema de diseño OpenLifting.

### 1.3 Hilt para inyección de dependencias

**Decisión**: Hilt sobre Koin, manual DI u otros.

**Por qué**:
- Hilt está respaldado por Google, integra natural con `hiltViewModel()` en Compose y con SavedStateHandle en navegación.
- Validación en compile time (vs. Koin que es runtime) — los errores de DI aparecen al compilar, no a runtime.

**Decisión secundaria**: KSP en lugar de KAPT como procesador de anotaciones.
- KSP es ~2x más rápido y es la recomendación oficial de JetBrains a partir de Kotlin 2.0.

### 1.4 Room + offline-first para mediciones

**Decisión**: Room como persistencia local con `Flow<T>` reactivo. Estrategia offline-first solo para los datos del flujo de medición.

**Por qué offline-first acotado**:
- Caso de uso real: el atleta usa el app en un gimnasio, posiblemente sin señal. La medición DEBE poder grabarse, computarse y verse sin conexión.
- Pero NO toda la app es offline-first. Las relaciones cross-user (instructor↔atleta) son inherentemente multi-device y viven en backend (Postgres / Laravel). Room ahí es **cache**, no source-of-truth.

**Boundary explícito**: ver §4 más abajo.

---

## 2. Sistema de diseño

### 2.1 Dual theme: light + dark warm

**Decisión**: dos modos con misma estructura semántica, dark warm en lugar de dark navy.

**Por qué warm en lugar del azul de Material por defecto**:
- El feel "instrumento clínico / herramienta de medición" se logra mejor con neutrales cálidos. Azules tech (navy, indigo) leen como "fitness app", no como "lab tool".
- Decisión validada visualmente con prototipos en Claude Design antes de implementar.

**Tokens canónicos** documentados en `CLAUDE.md` y derivados de `UI-inspiration/Openlifting-design.html`.

### 2.2 CTA primario = neutral de máximo contraste

**Decisión**:
- **Light mode**: CTA = `ink` (`#1A1523`) — botón negro sobre warm white.
- **Dark warm mode**: CTA = `cream` (`#F4EEE6`) — botón warm-white sobre warm dark.
- **Ámbar nunca es CTA primario**, ni en light ni en dark.

**Por qué cambió**: la primera versión usaba ámbar como CTA en dark mode. Al renderizarlo en device se vio que el dark warm bg ya tira al naranja, y agregar ámbar lo oversatura. Cambio documentado en commit `6ec0b74`.

**Regla mental simple**: el CTA es siempre el neutral más contrastado del modo activo. Color queda reservado para semántica (emerald=data/ok, amber=warn, risk=red).

### 2.3 Tipografía mono en numerics

**Decisión**: todos los valores numéricos (BSA %, ratios, %MVC, kg, RPE, timers) renderizados en monoespaciada.

**Por qué**: el feel de "equipo de medición" viene del rendering tabular de números. Sizes y weights diferenciados por jerarquía; mono está reservado para datos numéricos.

**Estado actual**: usando `FontFamily.Monospace` del sistema (Roboto Mono en Android). Pendiente: swap por IBM Plex Mono via Google Fonts en commit aparte.

---

## 3. Modelo de dominio — métricas y thresholds

Esta es la capa más sensible y la que más probablemente reciba preguntas en el final.

### 3.1 Por qué estos 5 músculos

**Músculos elegidos**: Vasto Lateral (VL), Vasto Medial (VM), Glúteo Mayor (GMax), Erector Espinal (ES), Bíceps Femoral (BF).

**Por qué estos**: cubren los 4 grupos funcionales críticos en la sentadilla:
- **Extensores de rodilla** (cuádriceps): VL + VM
- **Extensores de cadera** (glúteo): GMax
- **Estabilizadores de columna**: ES
- **Flexores de rodilla / extensores de cadera secundarios** (isquios): BF

**Por qué no más**: cada músculo extra requiere un par de electrodos en el sensor real. 5 músculos × 2 lados = 10 puntos de medición — ya es invasivo. Más músculos darían cobertura marginal a costo alto.

### 3.2 Las 4 métricas

#### BSA (Bilateral Symmetry Assessment) — la métrica protagonista

**Fórmula**: `((mayor − menor) / mayor) × 100` por músculo.

**Por qué**: estándar de literatura EMG bilateral. Normaliza al lado más fuerte, no a un valor absoluto — funciona aunque los electrodos del lado izquierdo registren más mV brutos por imprecisión de colocación.

**Thresholds**:
- ≥15% → **RIESGO** (asimetría significativa)
- 10–14.9% → **MONITORAR**
- <10% → **NORMAL**

**Origen de los cortes**: literatura de asimetría bilateral en atletas. Los estudios usan cutoffs entre 10-20% según contexto (atletas vs. población general). El corte conservador a 10% es para alerta temprana; 15% es el umbral más citado para "asimetría clínicamente relevante".

**Limitación**: la literatura de BSA es más madura para mediciones isokinéticas que para EMG dinámica. Los cutoffs adaptados son razonables pero no son peer-reviewed específicamente para sentadilla con sEMG.

#### H:Q (Hamstring / Quadriceps ratio)

**Fórmula**: `bíceps_femoral_avg / cuádriceps_avg` (promedios bilaterales).

**Por qué**: indicador de balance agonista/antagonista en la rodilla.

**Thresholds**:
- <0.45 → **RIESGO**
- 0.45–0.59 → **MONITORAR**
- ≥0.60 → **NORMAL**

**Origen**: literatura isokinética clásica (Heiderscheit et al, Croisier 2008). Los valores son para isokinético; en EMG dinámico se mueven un poco, pero el rango general se mantiene como referencia.

**Limitación importante**: H:Q es **indicador**, NO **predictor directo de lesión**. La copy de la app lo aclara explícitamente. Un atleta con H:Q "bajo" no está garantizado a lesionarse, solo está más fuera del rango óptimo de balance.

#### ES:GMax (Erector Spinae / Gluteus Maximus ratio)

**Fórmula**: `erector_espinal_avg / glúteo_mayor_avg` (promedios bilaterales).

**Por qué**: detecta compensación lumbar. Cuando el glúteo no recluta bien durante la sentadilla, el lumbar absorbe la sobrecarga — esto se ve clínicamente como "lumbar haciendo el trabajo de las caderas".

**Thresholds**:
- ≥2.0 → **RIESGO**
- 1.5–1.99 → **MONITORAR**
- <1.5 → **NORMAL**

**Origen**: Caterisano 2002 y estudios derivados sobre activación muscular en sentadilla profunda vs. parcial. Idealmente el lumbar activa al ~50-70% del glúteo (ratio ~0.5-0.7), valores ≥2 indican que está activando MÁS que el glúteo.

**Limitación**: específico de sentadilla. No generalizable a otros movimientos sin recalibración.

#### Fatiga intra-serie

**Fórmula**: `peak_última_rep / peak_primera_rep`.

**Por qué**: heurística accesible. Si la última repetición requiere MÁS activación que la primera para mover la misma carga, hay fatiga acumulada — el músculo está "trabajando más" para producir el mismo trabajo.

**Threshold**: >1.3 → **RIESGO**.

**Limitación**: simple. La literatura usa también pendiente RMS, mediana de frecuencia (que requiere análisis frecuencial), valor RMS-AVG. Esta versión simplificada es suficiente para alertas, no para análisis riguroso.

### 3.3 `thresholdsVersion: Int` en SetMetrics

**Decisión**: las métricas guardadas en Room llevan un campo `thresholdsVersion` (default 1).

**Por qué**: cuando los cortes se ajusten en el futuro (post-revisión con director de tesis o ante nueva literatura), las mediciones nuevas pasan a `version 2`, las viejas siguen marcadas con `version 1`. No invalidás el historial — sabés con qué reglas fue computada cada medición.

**Decisión arquitectónica deliberada** para evolución a largo plazo.

### 3.4 Recomendaciones en español

Las recomendaciones que la app muestra (ej: "Lumbar sobrecompensando, fortalecer activación glútea") están **escritas a mano** en `ComputeSetMetrics.generateRecommendations`, gatilladas por los thresholds violados.

**Estado**: textos heurísticos basados en la literatura, **no validados por kinesiólogo** para uso clínico real. Razonables para defensa académica; necesitarían revisión profesional para uso en producción.

---

## 4. Persistencia y boundary backend

### 4.1 Lo que es offline-first legítimo (Room como source-of-truth)

| Concepto | Tabla | Por qué offline |
|---|---|---|
| Sesiones de entrenamiento | `training_sessions` | Atleta puede entrenar sin señal |
| Sets, reps, activaciones | `training_sets`, `reps`, `muscle_activations` | Datos de la medición — deben grabarse al instante |
| Métricas computadas | `set_metrics` | El cómputo es local; no requiere backend |
| Recomendaciones | `recommendations` | Generadas desde `ComputeSetMetrics`, locales |
| Perfil del atleta logueado | `athlete_profiles` | Necesario para normalizar %MVC |
| Calibración MVC del atleta | `mvc_calibrations` | Idem — sin estos valores no se pueden mostrar %MVC reales |
| Preferencia de tema | SharedPreferences | UI local |

### 4.2 Lo que NO es offline-first (Room como stand-in temporal del backend)

| Concepto | Estado actual | Por qué no es offline-first |
|---|---|---|
| Relación instructor↔atleta | `instructor_athlete` (Room, demo) | Multi-device por naturaleza |
| Lista "mis atletas" del instructor | Query a Room | Idem — la verdad vive en Postgres |
| QR linking | TBD (sub-batch C) | Trigger frontend, resolución backend |
| Transferencia de invitado a registrado | TBD (sub-batch C) | Operación cross-user |
| Auth real | Hardcoded test@test.com / 1234 | Sin backend Laravel implementado |

**Boundary explícito**: el contrato `CoachRepository` (`domain/repository/CoachRepository.kt`) define qué expone esta capa. La implementación actual (`LocalCoachRepository`) usa Room como stand-in, con docstrings que aclaran la intención. Cuando el backend Laravel exista, la única cosa que cambia es el binding en `RepositoryModule`:

```kotlin
@Binds @Singleton
abstract fun bindCoachRepository(impl: LocalCoachRepository): CoachRepository
// ↓ swap a:
abstract fun bindCoachRepository(impl: RemoteCoachRepository): CoachRepository
```

Cero cambios en ViewModels, cero cambios en UI.

### 4.3 Sync queue (futuro)

Cuando el backend exista, sesiones grabadas offline van a una queue local. WorkManager (ya en dependencias del proyecto) drena la queue al recuperar conexión. Este sub-sistema NO está implementado en esta entrega.

---

## 5. Simulador del ESP32

### 5.1 Por qué simulador en lugar de hardware

**Decisión arquitectónica explícita** documentada en `CLAUDE.md`: el ESP32 + sensores físicos están **fuera del alcance de la materia de móvil**. El proyecto de tesis los integra eventualmente; el delivery de móvil entrega el app + simulador.

**Pipeline de análisis es idéntico**: el simulador genera `List<List<MuscleActivation>>` con valores en %MVC, exactamente la misma forma que recibiría un cliente del ESP32 real.

### 5.2 Distribuciones del simulador — qué viene de literatura, qué está engineered

**De literatura** (`Esp32Simulator.baseMeans`):
| Músculo | Mean %MVC en sentadilla 80% 1RM |
|---|---|
| VL  | 65% |
| VM  | 69% |
| GMax | 44% |
| ES  | 49% |
| BF  | 35% |

Fuentes: Bogdanis 2019, Yavuz 2017, Caterisano 2002. SDs realistas (5%) + ruido de sensor (2%) sumados con Box-Muller.

**Engineered (aproximaciones razonables sin cita específica)**:
- Multiplicadores de carga (`loadFactor`)
- Multiplicadores de profundidad (`depthFactor`: ABOVE_PARALLEL=0.85, PARALLEL=1.0, BELOW=1.1)
- Multiplicadores de variante (`variantGluteBoost=1.08`, `variantEsBoost=1.12` para low-bar)
- Asimetría bilateral aleatoria 2-10% por defecto

Estos multiplicadores son **direccionales correctos** (más profundidad → más glúteo, low-bar → más cadena posterior) pero los valores específicos los definí yo para que el demo genere datos visualmente interpretables.

### 5.3 Cuando llegue hardware real

Reemplazo del impl `Esp32Simulator` por `Esp32BleClient` o `Esp32HttpClient` (vía backend), con la misma firma pública. El boundary va a estar definido por una interface `EmgDataSource` cuando llegue el momento. Los ViewModels no se enteran del cambio.

---

## 6. Testing

### 6.1 Qué se testea

| Capa | Tests | Razón |
|---|---|---|
| Domain (use cases) | `ComputeSetMetricsTest` (24 cases) | Lógica pura de cómputo de métricas — alto valor |
| ViewModels con lógica | `AthleteHomeViewModelTest`, `SessionViewModelTest`, `OnboardingViewModelTest`, `CreateGuestViewModelTest`, `InstructorAthleteDetailViewModelTest` | State machines, persistencia, agregaciones |

**Frameworks**: JUnit 4 + MockK (mocking) + Turbine (Flow assertions) + kotlinx-coroutines-test.

### 6.2 Qué NO se testea (decisión deliberada)

| Capa | Razón |
|---|---|
| UI Compose | Tests instrumentados son costosos (emulator + run real); ROI bajo para 18 días |
| DAOs Room | Validación implícita en compile time (las queries son chequeadas por Room en build) |
| Mappers entity↔domain | Triviales, evidentes en lectura |
| Esp32Simulator | Es generador determinístico — output validable manualmente |

### 6.3 Total

~50 unit tests pasando al cierre de la entrega del lado atleta. Cada nuevo VM con lógica suma sus tests antes del commit (regla de `CLAUDE.md`).

---

## 7. Trade-offs y limitaciones conocidas

| # | Trade-off | Decisión | Razón |
|---|---|---|---|
| 1 | Hardware ESP32 vs. simulador | Simulador | Fuera del alcance de la materia; pipeline idéntico |
| 2 | Backend Laravel vs. Room stand-in | Stand-in con boundary explícito | Timeline ajustado; backend post-entrega |
| 3 | Auth real vs. mock hardcoded | Mock | Sin backend, mock acceptable; documentado |
| 4 | 5 músculos vs. más | 5 | Cobertura clínica suficiente con setup mínimo invasivo |
| 5 | Threshold versioning | Campo `thresholdsVersion` | Evolución sin invalidar historial |
| 6 | Recomendaciones validadas vs. heurísticas | Heurísticas | Defensa académica OK; uso clínico requiere revisión kinesióloga |
| 7 | Offline-first total vs. híbrido con boundary | Híbrido | Refleja la realidad del producto (cross-user → backend) |
| 8 | Tests E2E Compose vs. unit tests de VMs | Solo unit | ROI alto en lógica de dominio; bajo en glue de UI |
| 9 | Multi-módulo Gradle vs. único | Único | Simplicidad de build; refactor mecánico cuando crezca |
| 10 | KAPT vs. KSP | KSP | ~2x más rápido; oficial recomendación a partir de Kotlin 2.0 |
| 11 | XML themes vs. Compose-only | Compose | Dirección oficial; menos boilerplate; dark mode nativo |
| 12 | Ámbar como CTA dark vs. cream | Cream | Validación visual: ámbar oversatura el warm dark |

---

## 8. Cómo defender esto en el final

Si el evaluador pregunta "¿por qué X?", la respuesta corta debe ser:

1. **"Por qué Compose"** → "Es la dirección oficial; modelo declarativo; menos código; mejor manejo de estado reactivo con Flow."
2. **"Por qué Hilt y no Koin"** → "Validación en compile time; integración nativa con `hiltViewModel()` y SavedStateHandle."
3. **"Por qué Room"** → "Type-safe; integración con Flow; validación de queries en compile time."
4. **"Por qué este threshold de 15%"** → "Literatura de asimetría bilateral en atletas; cutoff conservador para alertas tempranas. El campo `thresholdsVersion` permite ajustarlo sin invalidar historial."
5. **"Por qué simulador"** → "El hardware ESP32 está fuera del alcance de esta materia. La pipeline de análisis es idéntica — el simulador implementa la misma firma que un cliente real."
6. **"Por qué Room para la relación instructor↔atleta si dijiste que no es offline-first"** → "Es un stand-in temporal del backend. El contrato (`CoachRepository`) está separado de la implementación; cuando el Laravel exista, swap el binding en Hilt y nada más cambia."
7. **"Por qué dark warm con CTA cream"** → "El ámbar oversatura el warm dark. El neutral de máximo contraste (cream en dark, ink en light) mantiene la jerarquía visual."

---

## Referencias rápidas

- Tokens de diseño canónicos: `CLAUDE.md` §"Light/Dark warm mode tokens"
- Inventario de pantallas: `docs/views.md`
- Briefs de diseño visual: `docs/design-briefs.md`
- Formulas y thresholds: `app/src/main/java/com/openlifting/domain/model/SetMetrics.kt` y `app/src/main/java/com/openlifting/domain/usecase/metrics/ComputeSetMetrics.kt`
- Simulador: `app/src/main/java/com/openlifting/data/simulator/Esp32Simulator.kt`
- Boundary backend: `app/src/main/java/com/openlifting/domain/repository/CoachRepository.kt` (interface) + `app/src/main/java/com/openlifting/data/repository/LocalCoachRepository.kt` (impl)
