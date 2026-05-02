# CLAUDE.md — OpenLifting Mobile

This file provides context to Claude Code when working in this repository.

## What this project is

**OpenLifting Mobile** is a native Android app for analyzing muscle activation (sEMG) during powerlifting back squats. It's the mobile component of a thesis project (Licenciatura en Informática) that connects ESP32 + MyoWare 2.0 sensors to a phone via BLE and provides bilateral muscle analysis.

**This delivery is a university course assignment** (mobile development course) with a tight timeline (~18 days). The full thesis context lives outside this repo — see "External references" below.

**Critical**: For this delivery the ESP32 is **simulated in-app**. The simulator lives in `data/simulator/Esp32Simulator.kt` and produces realistic %MVC data using parameterized Normal distributions from EMG literature (Bogdanis 2019, Caterisano 2002, Yavuz 2017). The app is identical whether data comes from a real sensor or the simulator.

## Architecture

**Stack:**
- Kotlin 2.0.21 + Jetpack Compose + Material 3
- Hilt (DI) — KSP, not kapt
- Room 2.6.1 (single source of truth, `Flow<T>`)
- Retrofit + OkHttp + kotlinx.serialization (backend ready, not used yet)
- WorkManager (sync target ready)
- Vico (charts, Compose-native)
- ML Kit Barcode + ZXing (QR — not implemented yet)
- CameraX (for QR scanning)

**Pattern:**
Clean Architecture "lite" in a single Gradle module. Dependencies flow `presentation -> domain <- data`. The `domain` package is pure Kotlin (no Android/Room/Retrofit).

**Package layout:**
```
com.openlifting/
├── data/
│   ├── local/        # Room entities + DAOs + database
│   ├── remote/       # Retrofit (configured, unused)
│   ├── simulator/    # Esp32Simulator — generates %MVC data
│   ├── repository/   # SessionRepositoryImpl, AuthRepositoryImpl
│   └── mapper/       # DTO ↔ Entity ↔ Domain
├── domain/
│   ├── model/        # Pure Kotlin models
│   ├── repository/   # Interfaces
│   └── usecase/
│       └── metrics/  # ComputeSetMetrics — BSA, H:Q, ES:GMax, fatigue
├── di/               # Hilt modules
└── presentation/
    ├── auth/         # Login, Register
    ├── athlete/
    │   ├── home/     # Home (placeholder — needs dashboard)
    │   ├── session/  # Session flow (metadata → simulate → analysis)
    │   ├── history/  # History list + session detail
    │   └── profile/  # Profile with logout, role switch
    ├── instructor/
    │   ├── home/     # Placeholder
    │   └── profile/  # Placeholder with role switch
    ├── navigation/   # AppNavGraph, AthleteScaffold (4 tabs), InstructorScaffold (2 tabs)
    └── common/       # RiskBadge and shared components
```

## Domain concepts (must understand)

- **MVC (Maximum Voluntary Contraction)**: athlete's max activation per muscle, used as 100% reference. The simulator currently uses default values; a real calibration flow is pending.
- **%MVC**: muscle activation normalized against MVC. The unit everyone shows.
- **BSA (Bilateral Symmetry Assessment)**: `((major - minor) / major) * 100`. Normal `<10%`, monitor `10-15%`, risk `>15%`. **The differentiator metric.**
- **ES:GMax ratio**: erector spinae over gluteus maximus. Detects lumbar compensation. Normal `<1.5`, monitor `1.5-2.0`, risk `≥2.0`.
- **H:Q ratio**: biceps femoris over quadriceps. Indicator (NOT predictor) of muscle balance. Normal `≥0.60`, monitor `0.45-0.60`, risk `<0.45`.
- **Intra-set fatigue**: ratio of last rep max activation vs first rep. Risk if `>1.3`.

All thresholds are encoded in `domain/model/SetMetrics.kt` as computed properties.

## Design system

The app supports **two themes**, both with the same semantic structure: **emerald = data/ok, amber = action/warmth**. Reference HTML mockups live in `UI-inspiration/`:

- `OpenLifting Home Hi-Fi Light _standalone_.html` — Light mode (primary)
- `OpenLifting Home Hi-Fi Dark Warm _standalone_.html` — Dark warm mode

### Light mode tokens
```
bg              #FFFCFC    (warm off-white)
bg-tint         #FAF7F4
surface         #FFFFFF
ink (text)      #1A1523    (eggplant near-black)
ink-2           #4A4554
ink-3           #7B7585
emerald (ok)    #0D9373
emerald-soft    #E6F4EF
amber (warmth)  #C8956C
amber-soft      #F7EEE3
warn            #B26A0E
risk            #B42318
rule            #E5E2DE
CTA primary     ink (#1A1523)  — black button on white
```

### Dark warm mode tokens
```
bg              #14110F    (warm near-black — NOT navy)
bg-tint         #1A1614    (subtle warm tint above bg)
surface         #1F1B17    (cards, primary surfaces)
surface-2       #2A2218    (nested surfaces, amber-soft fill)
surface-3       #2D2925    (deepest surfaces, equals rule)
ink (text)      #F4EEE6    (warm white — also CTA fill)
ink-2           #C9C2B8
ink-3           #8E8678
rule            #2D2925    (low-emphasis dividers)
rule-strong     #3D3833    (high-emphasis dividers)
emerald         #1FB088
emerald-soft    #112A23
emerald-ink     #5BD0AC    (text on emerald-soft surfaces)
amber           #D9A878    (soft warm accent — NOT CTA, NOT warn)
amber-soft      #2A2218    (= surface-2)
amber-ink       #F0B968
warn            #D9882A    (distinct from amber — used for warn states only)
warn-soft       #2A1E0C
warn-ink        #F0B968
risk            #E04C40
risk-soft       #2A1110
risk-ink        #F4998E    (text on risk-soft surfaces, also bright risk for emphasis)
CTA primary     cream (#F4EEE6)  — warm-white button on warm dark
CTA text on bg  #14110F          (bg color used as the button label)
```

**Light mode tokens** mirror this structure: `--rule` (only one level, no rule-strong), and the same triplet system (`*`, `*-soft`, `*-ink`) for emerald, amber, warn, risk. The full light table is the same as before.

The canonical source for all values is `UI-inspiration/Openlifting-design.html` (CSS custom properties under `:root, [data-theme="light"]` and `[data-theme="dark"]`).

**Semantic rule, never break it:**
- The CTA primary is always the highest-contrast neutral of the mode: ink in light, cream in dark warm. This mirrors the inverse: dark text on light surface ↔ light text on dark surface.
- Emerald is reserved for "data is ok" / status badges. Same role in both modes.
- Amber is reserved for warn states and warmth accents (banner tints, callouts, delta chips when the value moved badly). It is NEVER the primary CTA — the warm dark background already runs warm, and stacking amber on top oversaturates the screen.
- Risk uses risk red, monitor uses warn amber, normal uses emerald — these map to `RiskLevel.NORMAL/MONITOR/RISK`.

### Typography (both modes)
- **Space Grotesk** — display, headings, large numbers, button labels
- **Inter** — body, descriptions, UI text
- **IBM Plex Mono** — all numeric values (BSA, ratios, %MVC, badges)

Mono on numbers is what gives the "lab tool" feel. Use it consistently for any numeric display.

### Current implementation gap
The current `ui/theme/` files use a navy-blue dark theme (`Navy900 = #0B1528`, `Blue400 = #3B6AB8`) — this is **outdated** and needs to be replaced with the two-theme system above. Pending work.

## Voice & content conventions

- **Spanish** for all user-facing content, copy, recommendations, labels
- **English** for code, comments, commit messages, technical docs (this file, plans, ADRs)
- **No engagement gimmicks**: no streaks, badges, points, "you haven't trained in X days" notifications. This is a clinical tool, not a fitness app.
- **No AI/LLM features**: explicitly out of scope (health domain, hallucination risk)
- **Simulator must be transparent**: the "Simulate measurement" button is visible in the session flow, not hidden — the professor evaluating must be able to test without hardware.

## Current state (what's built)

**Working end-to-end:**
- Login + Register (hardcoded credentials — any email + 4+ char password works)
- Athlete 4-tab nav: Home / New session / History / Profile
- Instructor 2-tab nav: Athletes (placeholder) / Profile
- Session flow: metadata form → 2s simulated measurement → analysis screen with bilateral L/R bars, metric cards (BSA, ES:GMax, H:Q, fatigue) and Spanish recommendations
- History list with risk badges + session detail screen
- Profile with logout and role switch
- All persisted in Room (SQLite locally — backend connection ready but not used)
- 24 unit tests for `ComputeSetMetrics` (all passing)

**Hardcoded / stub:**
- Auth (no real backend call)
- MVC values (simulator uses defaults, no real calibration flow)
- Instructor home (just a placeholder)
- ESP32 pairing (no real BLE — simulator only)
- Theme is the old navy one, not the new emerald/amber system

## Pending work — priority order

1. **Theme refactor** — replace `ui/theme/Color.kt` and `Theme.kt` with the two-theme system from `UI-inspiration/`. Add Google Fonts (Space Grotesk, Inter, IBM Plex Mono). Wire up theme toggle in profile.
2. **Athlete Home dashboard** — currently a placeholder. Should show: last session summary + sparkline trend of BSA worst / ES:GMax over recent sessions + CTA "New session". Empty state for new athletes.
3. **Instructor operational** — list of athletes (registered + invited with filter), create guest athlete form, athlete detail screen, session for guest.
4. **MVC calibration flow** — guided onboarding flow that simulates the calibration and saves real values per muscle/side.
5. **More tests** — ViewModels (SessionViewModel especially), repository implementations.
6. **QR linking** — instructor↔athlete linking via QR (ML Kit + ZXing).
7. **Backend** (optional, if time permits) — at minimum auth endpoint with Sanctum.

## Build, run, test

```powershell
# Build debug APK
./gradlew --no-daemon assembleDebug

# Run unit tests (JVM, fast)
./gradlew --no-daemon test

# Specific test class
./gradlew --no-daemon test --tests "com.openlifting.domain.usecase.metrics.ComputeSetMetricsTest"
```

**Note**: `--no-daemon` is recommended on Windows due to occasional Gradle daemon crashes.

The Android app uses `compileSdk 35`, `minSdk 27`, `targetSdk 35`. JDK 17.

## Conventions

- **Tests before commit**: write unit tests for new domain logic / use cases / ViewModels before committing the feature.
- **Commit messages**: English, imperative ("Add", "Fix", "Implement"), no co-author tag, conventional structure (one-line subject + optional body).
- **Don't add Co-Authored-By: Claude lines** — the user has explicitly asked for this.
- **Logical commits**: when a chunk of work touches infra + data + domain + UI, split into separate commits per layer if reasonable.
- **No emojis** in code or commits. Status/alert icons are vector icons via Material Symbols.
- **Modifier order**: structural (size, padding) before visual (background, border).

## External references (outside this repo)

- **Thesis main repo**: `C:\Users\giord\Documents\Obsidian Vault\Reformam\Personal2p\Facultad\Tesis\OpenLifting`
  - `docs/investigacion-sintesis.md` — exact metric thresholds reference
  - `docs/investigacion.md` — sEMG processing pipeline scientific basis
  - `Actividades-Asignatura/02_decisiones.md` — DEC-001..012 technical decisions
  - `Actividades-Asignatura/03_capitulo_1.md` — thesis Chapter 1 (problem statement)
- **Plan file**: `C:\Users\giord\Desktop\Facultad\2026-last-run\2026-finales\Computacion-movil\Mobile-app-april\plan-v2.md` — full architectural plan, user flows F0-F6, weekly breakdown
- **Landing page**: `C:\Users\giord\Documents\Obsidian Vault\Reformam\Personal2p\Facultad\Tesis\OpenLifting\landing\` — the OpenLifting marketing landing whose visual language inspired the app's design system.

## Things NOT to do

- **Don't reintroduce the old navy theme.** The new design system is emerald + amber, two themes (light/dark warm). The old `Navy900` / `Blue400` palette is deprecated.
- **Don't move the simulator to the backend.** It lives in the app intentionally — it replaces the ESP32, not the backend. This was an explicit architectural decision (offline-first).
- **Don't compute metrics on the backend.** Client-side calculation is the rule (offline-first). The backend, when implemented, is just a sync target.
- **Don't add gamification.** Streaks, levels, badges-as-rewards, social features. Clinical tool, not engagement product.
- **Don't add LLM/AI features.** Out of scope, health-domain risk.
- **Don't blindly accept simulator output.** The current simulator uses default MVC values — when the calibration flow is built, the simulator must respect those calibrated values.

## Recent commit history (where we left off)

```
af39ea8  Add unit tests for ComputeSetMetrics use case
52b0cea  Add auth, session flow, history and role-based navigation
37ea0d5  Implement in-app ESP32 simulator and metrics calculator
3dbe175  Add domain models, Room entities and DAOs
9d2c5a4  Set up dependencies, Hilt DI and Room database
```
