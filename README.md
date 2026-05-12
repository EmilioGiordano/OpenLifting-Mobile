# OpenLifting Mobile

App Android nativa para análisis de activación muscular (sEMG) durante sentadillas de powerlifting. Componente móvil de un proyecto de tesis (Licenciatura en Informática) que se conecta a sensores ESP32 + MyoWare 2.0 y entrega análisis bilateral en %MVC.

> **Esta entrega es la cursada de Computación Móvil.** El ESP32 está **simulado dentro de la app** (`data/simulator/Esp32Simulator.kt`) usando distribuciones normales con parámetros tomados de literatura sEMG (Bogdanis 2019, Caterisano 2002, Yavuz 2017). La app es idéntica con sensor real o simulado, pero requiere programar la configuración mediante Bluetooth.

## Stack

- Kotlin 2.0.21 · Jetpack Compose · Material 3
- Hilt (DI, KSP) · Room 2.6.1 (cache local, `Flow<T>`)
- Retrofit + OkHttp + kotlinx.serialization (backend Vortex)
- WorkManager · Vico (gráficos) · CameraX · ML Kit Barcode + ZXing

## Arquitectura

Clean Architecture "lite" en un único módulo Gradle. Dependencias hacia adentro: `presentation → domain ← data`. El `domain` es Kotlin puro (sin Android, Room ni Retrofit).

```
[ESP32 simulado]  ──►  [App Android (Kotlin)]  ◄──HTTP REST──►  [Backend Vortex (Laravel + PostgreSQL)]
                          │
                          └─ Room (cache local, source of truth = Postgres)
```

- **Métricas en el cliente** (offline-first). El backend es solo sync target.
- **Postgres como fuente de verdad**, Room espejo local.
- **Atleta con cuenta** mide offline. **Atleta invitado** (medido por su entrenador) requiere conexión: el flujo depende del `session_id` del backend para el claim code.

## Conceptos de dominio

| Sigla | Qué mide | Umbrales |
|---|---|---|
| **%MVC** | Activación normalizada al peak personal del atleta | — |
| **BSA** | Asimetría bilateral por músculo `((mayor − menor) / mayor) × 100` | < 10% normal · 10–15% monitor · ≥ 15% riesgo |
| **ES:GMax** | Compensación lumbar (erector espinal / glúteo mayor) | < 1.5 · 1.5–2.0 · ≥ 2.0 |
| **H:Q** | Balance posterior/anterior (isquios / cuádriceps) | ≥ 0.60 · 0.45–0.60 · < 0.45 |
| **Fatiga intra-serie** | Peak última rep / peak primera rep | ≤ 1.3 / > 1.3 |

Todos los thresholds viven en `domain/model/SetMetrics.kt`.

## Build & run

```powershell
# APK debug
./gradlew --no-daemon assembleDebug

# Tests unitarios (JVM)
./gradlew --no-daemon test

# Instalar en device/emulator conectado
./gradlew --no-daemon installDebug
```

`compileSdk 35` · `minSdk 27` · `targetSdk 35` · JDK 17. Usar `--no-daemon` en Windows por inestabilidad del Gradle daemon.

## Estructura

```
com.openlifting/
├── data/        Room, Retrofit, simulator, repositorios, mappers
├── domain/      Modelos puros + casos de uso (ComputeSetMetrics)
├── di/          Módulos Hilt
└── presentation/
    ├── auth/         Login, Register
    ├── athlete/      Home, Session, History, Profile
    ├── instructor/   Athletes, Profile
    ├── onboarding/   Welcome, MVC calibration
    ├── navigation/   AppNavGraph + scaffolds
    └── common/       RiskBadge, BrandMark
```

## Diseño

Dos temas (claro y oscuro warm), misma estructura semántica:

- **Emerald** → datos OK / estado normal
- **Amber** → warn / acento de calidez (nunca es CTA)
- **Risk** → rojo, riesgo

Tipografías: **Space Grotesk** (display), **Inter** (UI), **IBM Plex Mono** (números). Maquetas de referencia en `UI-inspiration/`.

### Light mode
<img width="2400" height="2100" alt="athlete-screens-light-overview" src="https://github.com/user-attachments/assets/13c46c1e-b70d-4f15-916d-42446e8e2fd8" />

#### Charts: Diseños preliminares
<img width="1573" height="1205" alt="image" src="https://github.com/user-attachments/assets/1031ee78-b1f8-4f8f-81a9-f5786280e946" />

>! Las opciones 'V-D · Focus' son los diseños más cercanos al diseño real implementado

### Dark mode
<img width="2400" height="2100" alt="athlete-screens-dark-overview" src="https://github.com/user-attachments/assets/82fa91a5-97d6-4a2c-b0eb-90f6ce0410c6" />

#### Charts: Diseños preliminares
<img width="1588" height="1225" alt="image" src="https://github.com/user-attachments/assets/7d83a43f-2fdf-4ca1-94a9-d72ec4295f32" />

>! Las opciones 'V-D · Focus' son los diseños más cercanos al diseño real implementado



## Convenciones

- **Spanish** para texto al usuario, **English** para código, comentarios y commits.
- Sin gamificación (streaks, badges, puntos). Es herramienta clínica.
- Sin AI/LLM en la primer fase del proyecto (dominio de salud, riesgo de alucinación).
- Tests antes del commit en lógica de dominio / use cases.
- Mensajes de commit: imperativos, descriptivos, en inglés y sin emojis.

## Documentación adicional

- `CLAUDE.md` — instrucciones para colaborar con Claude Code en este repo.
- `docs/presentacion-final.md` — apuntes para la defensa.
- `template/` — assets de presentación (logo, arquitectura, deck HTML).
