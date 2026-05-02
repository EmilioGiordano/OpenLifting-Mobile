# OpenLifting Mobile — Views inventory (Phase 1)

> Living document. Goal: enumerate every user-facing surface in the app, what data it reads, what actions it exposes, and how it behaves in empty/error/loading states. No code yet — this is the map we use before designing in Claude Design (Phase 2) and implementing (Phase 3).

## How to read this doc

Each view follows the same skeleton:

- **Route / entry**: how the user gets here.
- **Purpose**: one line, why the view exists.
- **Data shown**: which domain models/fields drive the content (grounded in `domain/model/*`).
- **Actions (CTAs)**: primary + secondary actions.
- **Empty state**: what shows when there is no data yet.
- **Error state**: what shows when the underlying call fails (only listed when meaningful).
- **Loading state**: only when the view has a non-trivial async load.
- **Key components**: reusable pieces this view needs (these will become the shared component library).
- **Notes / open questions**: anything ambiguous or pending decision.

Conventions assumed throughout (from `CLAUDE.md`):
- Spanish copy, mono font on all numeric values.
- Emerald = data/status. Primary CTA = highest-contrast neutral (ink in light, cream in dark warm). Amber = warn states + accents only, NEVER primary CTA. Risk colors via `RiskLevel` (NORMAL/MONITOR/RISK).
- Light + dark warm themes share semantics; both must be supported.

---

## 0. Cross-cutting / global

### 0.1 Splash / Boot
- **Route**: implicit, before `LOGIN`. Currently handled inside `AppNavGraph` via `LoginViewModel.checkSession()`.
- **Purpose**: decide whether to send the user to Login, Athlete root, or Instructor root.
- **Data shown**: nothing visible — at most the app logotype while session is being resolved.
- **Actions**: none (auto-routed).
- **Loading state**: the splash itself *is* the loading state. Keep it ≤300 ms in the happy path.
- **Notes**: implemented as a real `SplashScreen` composable (decided in §7.10) — brand mark + subtle loading indicator while `LoginViewModel.checkSession()` resolves. Replaces the brief Login flash that currently occurs.

### 0.2 Global error / "something went wrong"
- Not a route — a reusable layout used by views when their root data load fails.
- **Components**: full-screen icon + Spanish message + "Reintentar" primary CTA button (ink/cream) + small "Volver" text button.
- **When used**: failure of the initial Flow load on Home, History, SessionDetail, AthleteDetail.

### 0.3 Connectivity / offline banner (optional)
- Subtle top banner when the device is offline AND the screen depends on backend. Since the MVP is offline-first (Room is source of truth), this is *informational only* on screens that would later sync (History, Profile data updates). Skip for MVP unless trivial.

---

## 1. Auth

### 1.1 Login
- **Route**: `LOGIN` — start destination.
- **Purpose**: authenticate an existing user.
- **Data shown**: app name + tagline; email field; password field.
- **Actions**:
  - Primary: `Iniciar sesión` (primary CTA: ink in light, cream in dark).
  - Secondary: `Crear cuenta` → goes to Register.
  - Tertiary (future): `Olvidé mi contraseña` — out of MVP scope, not shown for now.
- **Empty state**: N/A (form-only screen).
- **Error state**: inline field errors ("Email inválido", "Contraseña muy corta") + form-level error ("Credenciales inválidas") under the CTA. Hardcoded auth today: any email + 4+ char password is accepted, so the only realistic failures are validation.
- **Loading state**: CTA shows spinner + disabled state during the auth call.
- **Key components**: `BrandHeader`, `OLTextField` (email + password variants), `OLPrimaryButton`, inline error caption.
- **Notes**: keep the role selector OUT of Login — role comes from the user record, not from the user's choice at sign-in.

### 1.2 Register
- **Route**: `REGISTER`.
- **Purpose**: create a new account and route to the right home.
- **Data shown**: name, email, password, **role selector** (Atleta / Entrenador) — segmented control.
- **Actions**:
  - Primary: `Crear cuenta`.
  - Secondary: `Volver` (back arrow in top bar) → Login.
- **Error state**: same pattern as Login (inline + form-level). Email already in use is the realistic failure when the backend is wired.
- **Loading state**: CTA spinner.
- **Key components**: `BrandHeader`, `OLTextField`, `RoleSegmented`, `OLPrimaryButton`.
- **Notes**: the **AthleteProfile** (bodyweight, age, sex) is NOT collected here. That belongs to onboarding so we can keep this form short. After register → if role is Atleta, we route into the onboarding flow (1.3 below); if Entrenador, straight to instructor home (no calibration needed).

---

## 2. Onboarding (athlete first-run only)

> Goal: collect the athlete profile + walk them through MVC calibration so we have real reference values for %MVC. Skipped for instructors. Skippable per-step? **No** for the calibration itself — the app is unusable without an MVC reference, but the simulator will fall back to defaults if the user dismisses, so we treat the skip as "use default MVC" with a banner shown later.

### 2.1 Onboarding — Welcome
- **Route**: `ONBOARDING_WELCOME` (athlete root + first-launch only).
- **Purpose**: explain what the app does in 2 short bullets and set expectations for the next 3 minutes.
- **Data shown**: hero illustration, title, 2–3 bullets in Spanish, "Comenzar" primary CTA.
- **Actions**: `Comenzar` → Profile data.
- **Notes**: NO sign-in copy. The user is already logged in.

### 2.2 Onboarding — Athlete profile data
- **Route**: `ONBOARDING_PROFILE`.
- **Purpose**: collect `AthleteProfile` (firstName, lastName, bodyweightKg, ageYears, sex).
- **Data shown**: form with 5 fields. `firstName`/`lastName` may be prefilled from `User.name`.
- **Actions**: `Continuar` → MVC explanation.
- **Error state**: per-field inline ("Peso debe ser > 30 kg", etc.).
- **Components**: `OLTextField`, `SexSelector`, step indicator (1/3).

### 2.3 Onboarding — MVC explanation
- **Route**: `ONBOARDING_MVC_EXPLAIN`.
- **Purpose**: explain in plain Spanish what MVC is, why we calibrate, and what the user will do next (one short flexion per muscle/side, ~10s).
- **Data shown**: short copy, an animated diagram or simple illustration of muscles being calibrated, list of the 5 muscles × 2 sides = 10 measurements.
- **Actions**: `Empezar calibración` (primary CTA) + `Saltar por ahora` (text, sets `MvcCalibration` to defaults and shows a persistent "Calibración pendiente" banner on Home).
- **Components**: `MuscleDiagram`, `OLPrimaryButton`, `OLLinkButton`, step indicator (2/3).

### 2.4 Onboarding — MVC calibration (per muscle × side)
- **Route**: `ONBOARDING_MVC_CAPTURE`.
- **Purpose**: walk through the 10 calibration measurements with clear "prepare → contract → release" feedback per measurement.
- **Data shown**: current muscle name (`Muscle.displayName`) + side (`MuscleSide.label`), instruction text, big timer (3-2-1 prepare → 5s contract), live %-bar, captured `mvcValue` afterwards. Progress chip "3 / 10".
- **Actions**:
  - During measurement: `Cancelar` (stop the current capture).
  - After measurement: `Repetir` (redo this one) + `Siguiente` (continue).
  - On the last one: `Finalizar` (saves all `MvcCalibration` rows and updates `AthleteProfile.calibratedAt`).
- **Empty state**: N/A.
- **Error state**: if simulated capture returns garbage (e.g. peak below noise floor): "No detectamos contracción suficiente, repetí" + `Repetir`.
- **Loading state**: the timer + bar IS the loading state.
- **Components**: `Countdown`, `LiveActivationBar`, `MuscleDiagram` (with the current muscle highlighted), `StepProgressDots` (1/10 → 10/10).
- **Notes**: this view is also reused for **Instructor → Calibrate guest** (3.4). Same component, different "who is this for" header.

### 2.5 Onboarding — Done
- **Route**: `ONBOARDING_DONE`.
- **Purpose**: confirmation, recap of calibrated muscles, CTA into the app.
- **Data shown**: checkmark, "Calibración lista" headline, table of 10 captured MVC values with mono numbers.
- **Actions**: `Ir al inicio` → Athlete Home.

---

## 3. Athlete

> 4 bottom-tab scaffold (already in `AthleteScaffold.kt`): **Home / Nueva sesión / Historial / Perfil**.

### 3.1 Home (dashboard)
- **Route**: athlete tab `home`.
- **Purpose**: at-a-glance status of the athlete's recent training. This is the most visited screen.
- **Data shown**:
  - Greeting row: `Hola, ${AthleteProfile.firstName}` + small chip with last session date or "Sin sesiones".
  - **Last session card**: date, set count, overall risk badge (worst risk across sets), top metric snapshot (BSA worst %, ES:GMax, H:Q).
  - **BSA trend sparkline** (Vico, large card): BSA-worst per session over the last N sessions (e.g. 6). Dotted reference lines at 10% (monitor) and 15% (risk). This is the protagonist visualisation per §7.7.
  - **Compact metric chips** (row below the sparkline, two chips): ES:GMax current value + delta vs previous session; H:Q current value + delta. No sparklines for these on Home — the multi-metric trend stack lives in History.
  - **Action card**: "Nueva sesión" primary CTA (ink in light, cream in dark). Shows next-suggested target if we have one, otherwise just the CTA.
  - Pending notices (banners, in priority order): "Calibración MVC pendiente" if `AthleteProfile.calibratedAt == null`; "Sin conexión, cambios guardados localmente" when offline (deferred).
- **Actions**:
  - Primary: `Nueva sesión` → Session metadata (3.2).
  - Card tap on last-session card → opens that `SessionDetail` (3.5).
  - Sparkline tap → opens History (3.4) filtered by metric.
- **Empty state**: hero "Aún no tenés sesiones" illustration + 2-line copy + primary CTA `Comenzar primera sesión`. No sparkline. No last-session card.
- **Error state**: 0.2 layout if the Flow throws.
- **Loading state**: skeleton cards (3 stacked rectangles) while the Flow emits its first value.
- **Key components**: `BrandHeader`, `LastSessionCard`, `BsaTrendCard`, `MetricDeltaChip` (ES:GMax + H:Q), `PendingBanner`, `OLPrimaryButton`, `EmptyHero`, `Skeleton`.
- **Notes**: this replaces the current placeholder. Hierarchy: BSA dominates visually (large sparkline), ES:GMax and H:Q are secondary (compact chips with deltas). The full multi-metric view lives in History.

### 3.2 New session — pre-flight
- **Route**: athlete tab `session` (entry).
- **Purpose**: confirm the device source + exercise before the first set, so the rest of the flow is just "set after set".
- **Data shown**: exercise (read-only "Sentadilla trasera"), device source picker (`SIMULATED` / `REAL`, but for the delivery only `SIMULATED` is enabled), short reminder of how the flow works (3 mini-icons: ingresar serie → simular → ver análisis).
- **Actions**:
  - Primary: `Comenzar sesión` (creates the `TrainingSession` row and goes to 3.3).
  - Secondary: back to home.
- **Empty state**: N/A.
- **Notes**: if we want to keep things minimal, this can be merged into 3.3 — but having a clear "Comenzar" gesture before any data entry feels right. **Decide together** before designing.

### 3.3 New session — Set metadata + measure
> Already implemented as `SessionScreen.SetMetadataContent` + `MeasuringContent`. Three substates of the same conceptual view.

#### 3.3.a Set metadata
- **Purpose**: capture the inputs needed for one set before measuring.
- **Data shown**: header `Serie N`, fields: load (kg), target reps, variant (`SquatVariant`), depth (`SquatDepth`), RPE (1–10).
- **Actions**:
  - Primary: `Simular medición`.
  - Secondary: `Finalizar sesión` (ends the session as-is; goes to summary 3.3.d).
- **Error state**: per-field validation; CTA disabled until kg > 0 and reps > 0.
- **Components**: `OLNumberField` (kg, reps, RPE), `OLDropdown` (variant, depth), `SetHeader`.

#### 3.3.b Measuring
- **Purpose**: visible "the ESP32 is sending data" state. This is intentionally transparent — the simulator must be visible to the evaluator (CLAUDE.md rule).
- **Data shown**: animated chip "Simulando ESP32…", live %MVC bars per muscle (animated, not real). Approx 2–3 s.
- **Actions**: `Cancelar` (text) — kills the simulated measurement and goes back to 3.3.a.
- **Components**: `LiveActivationBar` (reused from calibration), `OLLinkButton`.

#### 3.3.c Set analysis
- **Purpose**: show the result of the set: bilateral activation, the four metrics with their `RiskLevel`, and Spanish recommendations.
- **Data shown**:
  - Header: `Serie N — {kg} kg × {reps} reps · RPE {x} · {variant} · {depth}`.
  - **Bilateral activation block**: per `Muscle` (5 rows), `MuscleSide.LEFT` and `RIGHT` bars in %MVC + numeric value (mono) + tiny BSA chip per row. Reuse `bsaXxxPct` from `SetMetrics`.
  - **Metric cards** (2×2 grid): BSA worst, ES:GMax, H:Q, Fatigue intra-serie. Each card: label, big mono number, `RiskBadge`.
  - **Recommendations**: list of `Recommendation` cards, tinted by `severity`. Tappable to expand `evidence` if non-empty.
- **Actions**:
  - Primary: `Siguiente serie` → 3.3.a with set N+1.
  - Secondary: `Finalizar sesión` → summary 3.3.d.
- **Empty state**: N/A — analysis is only reached after a measurement.
- **Components**: `BilateralRow`, `MetricCard`, `RiskBadge`, `RecommendationCard`, `SetHeader`.

#### 3.3.d Session summary (NEW — proposal)
- **Purpose**: closing screen, recap of the whole session before going to History or Home.
- **Data shown**:
  - Total sets, duration, top load, overall risk (worst across sets).
  - Mini-table: one row per set with set number, kg×reps, BSA worst, overall risk badge.
  - Top 1–3 recommendations to keep in mind for next session (aggregated severity).
- **Actions**: `Ver detalle de la sesión` (opens 3.5), `Volver al inicio`.
- **Notes**: today the app navigates straight back after `endSession()`. This summary turns the flow into something the user wants to share with their coach. Treat as **proposal**, confirm before designing.

### 3.4 History (list)
- **Route**: athlete tab `history`.
- **Purpose**: chronological list of past sessions for this athlete.
- **Data shown**: scrollable list. Each row: date (relative + absolute), set count, top load, overall `RiskLevel` badge, BSA-worst chip. Group header by month.
- **Actions**:
  - Tap row → `SessionDetail` (3.5).
  - Future: filter chips (date range, risk level). Out of MVP.
- **Empty state**: "Aún no hay sesiones" + small CTA `Comenzar sesión`.
- **Error state**: 0.2.
- **Loading state**: row skeletons.
- **Components**: `SessionRow`, `MonthHeader`, `RiskBadge`, `EmptyHero`, `Skeleton`.

### 3.5 Session detail
- **Route**: `session/{localId}` (deep param).
- **Purpose**: full read-only view of a past session, set by set.
- **Data shown**:
  - Top: date, exercise, total sets, total volume (sum of `kg × reps`), worst-risk badge.
  - Per set (collapsible): the same content as 3.3.c (analysis), but read-only — bilateral bars, 4 metric cards, recommendations list.
- **Actions**:
  - Primary (nice-to-have): `Compartir` (export as PDF/text). **Out of MVP.**
  - Secondary: back.
- **Empty state**: N/A (the deep param is only valid for an existing session).
- **Error state**: "Sesión no encontrada" + `Volver`.
- **Components**: same as 3.3.c plus `CollapsibleSetCard`.

### 3.6 Profile (athlete)
- **Route**: athlete tab `profile`.
- **Purpose**: account + preferences hub.
- **Data shown**:
  - User: name, email, role chip ("Atleta").
  - AthleteProfile: bodyweight, age, sex, calibrated-at date (or "Sin calibrar").
  - Preferences: theme toggle (system / light / dark warm), language (currently Spanish only — chip).
  - Linked instructor (if any) — shows instructor name + "Desvincular" small action.
- **Actions**:
  - `Editar datos personales` → 3.7.
  - `Recalibrar MVC` → reuses 2.4 in re-calibration mode.
  - `Vincular con entrenador` → 3.9 (Scan QR).
  - `Modo demo: cambiar a Entrenador` (labelled explicitly as demo per §7.9 — caption beneath: "Solo para evaluación, no es una función de producción").
  - `Cerrar sesión`.
- **Empty state**: per row — e.g. "Sin entrenador vinculado" with small CTA.
- **Components**: `ProfileSection`, `ProfileRow`, `ThemeSegmented`, `OLTextButton`, `RiskBadge` (for "Calibración pendiente" status).

### 3.7 Edit personal data
- **Route**: `profile/edit`.
- **Purpose**: change `AthleteProfile` fields (bodyweight, age, sex). Name and lastname remain read-only to avoid identity drift (decided in §7.3).
- **Data shown**: form with bodyweight, age, sex prefilled from current profile. Name and lastname shown as read-only rows above the form.
- **Actions**: `Guardar`, `Cancelar`.
- **Recalibration nudge**: when bodyweight is changed (delta > 2 kg threshold suggested), surface an inline suggestion banner under the field: "Considerá recalibrar tus MVC — el cambio de peso puede invalidar la calibración actual" with a small `Recalibrar` link that routes to 2.4 in re-calibration mode. The save still succeeds without recalibrating.
- **Components**: `OLNumberField`, `OLTextField`, `SexSelector`, `RecalibrationNudgeBanner`, `OLPrimaryButton`.

### 3.8 Pair ESP32 (NEW — present in your list)
- **Route**: `profile/pair-esp32` (entry from Profile, also reachable from Pre-flight 3.2 if device source is set to REAL).
- **Purpose**: discover/connect to a real ESP32 over BLE.
- **Status for this delivery**: **stub view**, not functional — shows a "Próximamente" card and an explanation of how the pairing will work. Marked as out-of-MVP-scope but kept in the inventory.
- **Data shown**: a list of nearby BLE devices (mocked, empty), pairing instructions.
- **Actions**: `Buscar dispositivos` (no-op for now), `Volver`.
- **Notes**: Per `CLAUDE.md`, the ESP32 is fully simulated for this delivery. Don't invest design time here beyond a placeholder that the professor can see exists.

### 3.9 Scan QR (athlete ↔ instructor link)
- **Route**: `profile/scan-qr`.
- **Purpose**: athlete scans a QR generated by an instructor (4.6) to link the two accounts. Also reused for the "transfer guest" flow (4.7).
- **Data shown**: live camera preview with QR target box, hint copy.
- **Actions**:
  - Detected QR → confirmation sheet "Vincular con {instructor name}?" → `Confirmar` / `Cancelar`.
  - Manual fallback: `Ingresar código` → small modal with text field (in case the camera isn't usable).
- **Empty state**: N/A.
- **Error state**: permission denied → "Necesitamos permiso de cámara" + `Abrir ajustes`. Invalid QR → toast "Código inválido".
- **Loading state**: brief spinner while resolving the code with the backend (deferred — for now resolves locally).
- **Components**: `CameraScannerSurface`, `BottomSheet`, `OLPrimaryButton`.

---

## 4. Instructor

> 2 bottom-tab scaffold (already in `InstructorScaffold.kt`): **Atletas / Perfil**.

### 4.1 Athletes home (list)
- **Route**: instructor tab `athletes`.
- **Purpose**: list of athletes the instructor manages, registered + invited (guest).
- **Data shown**: list rows. Each row: athlete full name, last session date / `Sin sesiones`, latest overall risk chip, small icon/tag indicating type (`Registrado` / `Invitado`).
- **Actions**:
  - Primary FAB or top button: `Nuevo invitado` → 4.2.
  - Top action: `Generar QR de vínculo` → 4.6 (so a registered athlete can scan and link to me).
  - Tap row → 4.5 (athlete detail).
  - Filter chips (top): All / Registrado / Invitado.
- **Empty state**: "Aún no tenés atletas. Generá un QR para vincular o creá un invitado".
- **Error state**: 0.2.
- **Loading state**: row skeletons.
- **Components**: `AthleteRow`, `FilterChips`, `Fab`, `EmptyHero`.

### 4.2 Create guest athlete
- **Route**: `instructor/guest/new`.
- **Purpose**: create a guest `AthleteProfile` owned by the instructor (no `User` row, or a stub one) so the instructor can record a session for someone without an account.
- **Data shown**: form with name, lastname, bodyweight, age, sex.
- **Actions**: `Crear y calibrar` → 4.3 (calibration for this guest); `Cancelar`.
- **Error state**: inline per-field.
- **Components**: same form components as 2.2.

### 4.3 Guest MVC calibration
- **Route**: `instructor/guest/{id}/calibrate`.
- **Purpose**: same calibration flow as 2.4 but in instructor context, for a guest.
- **Data shown**: same as 2.4 plus a header "Calibrando: {Guest fullName}".
- **Actions**: same as 2.4. On finish → goes to 4.5 (athlete detail) or directly into a session for that guest.
- **Notes**: reuse the exact same component as 2.4 — only the header context changes.

### 4.4 Session for guest (instructor-driven)
- **Route**: `instructor/guest/{id}/session`.
- **Purpose**: same Session flow as 3.3 (a, b, c, d) but the active athlete is the selected guest. Reuse 3.3 components verbatim.
- **Notes**: the guest's `TrainingSession.instructorUserId` is set to the instructor's id. No design difference — just routing.

### 4.5 Athlete detail
- **Route**: `instructor/athlete/{id}`.
- **Purpose**: instructor's view of one athlete. Same shape as 3.1 (Home dashboard) but read-only and with instructor-only actions.
- **Data shown**: athlete profile summary (name, age, sex, bodyweight, calibrated-at), last session card, trend sparklines (BSA, ES:GMax), full history list (or link to it), MVC calibration table.
- **Actions**:
  - `Iniciar sesión con este atleta` (only for guests) → 4.4.
  - `Recalibrar MVC` (guests only) → 4.3.
  - `Generar QR de transferencia` → 4.7. **Only meaningful for guests** that the athlete might want to "claim" later.
  - `Desvincular` (registered only) → confirm dialog.
- **Empty state**: "Este atleta aún no tiene sesiones".
- **Components**: `AthleteHeaderCard`, `LastSessionCard`, `TrendSparklineCard`, `MvcTable`, `OLPrimaryButton`, confirm dialog.

### 4.6 Generate link QR (instructor → athlete)
- **Route**: `instructor/qr/link`.
- **Purpose**: show a QR that a registered athlete can scan from 3.9 to link.
- **Data shown**: the QR (large, contrast-correct in both themes), human-readable code below it as fallback, expiration timer if we choose to add one.
- **Actions**: `Compartir código` (system share sheet with the text payload), `Cerrar`.
- **Components**: `QrSurface`, `OLPrimaryButton`.

### 4.7 Generate transfer QR (guest → registered athlete)
- **Route**: `instructor/athlete/{id}/transfer-qr`.
- **Purpose**: emit a QR that, when scanned by a registered athlete (3.9), transfers ownership of a guest profile + its sessions to that user.
- **Data shown**: same as 4.6 but with explanatory copy: "Cuando el atleta lo escanee, el historial pasará a su cuenta".
- **Actions**: same as 4.6. Plus a clear `Cancelar transferencia` if the QR is single-use and we want to invalidate it.
- **Notes**: backend-dependent. For the MVP we can mock the resolution and surface the success/failure UX, but the actual transfer is out of scope unless backend lands in time.

### 4.8 Profile (instructor)
- **Route**: instructor tab `profile`.
- **Purpose**: same shape as 3.6 (athlete profile), but instructor-flavoured.
- **Data shown**: User (name, email, role chip "Entrenador"), preferences (theme, language), counters ("3 atletas vinculados", "1 invitado"), `Modo demo: cambiar a Atleta` toggle.
- **Actions**: `Editar perfil`, `Cerrar sesión`, role switch (labelled "Modo demo" with a "Solo para evaluación" caption per §7.9).
- **Notes**: instructors don't have an `AthleteProfile`, so no bodyweight/age. Profile is leaner.

---

## 5. Cross-cutting components (catalog seed)

These are components that show up in 3+ views and should be designed once. The list seeds the component library we'll build in Phase 3.

- **`BrandHeader`** — logo + (optional) tagline. Login, Register, Onboarding welcome.
- **`OLPrimaryButton`** / **`OLOutlineButton`** / **`OLTextButton`** — three button styles, theme-aware.
- **`OLTextField`** / **`OLNumberField`** / **`OLDropdown`** — Material 3 wrappers with our visual language and inline-error pattern.
- **`RiskBadge`** — already exists (`presentation/common/RiskBadge.kt`) — needs visual upgrade for the new theme.
- **`MetricCard`** — label + big mono value + risk badge.
- **`BilateralRow`** — muscle name + L/R bars + per-side mono % + tiny BSA chip.
- **`LiveActivationBar`** — animated %MVC bar; reused in calibration and measuring.
- **`MuscleDiagram`** — body silhouette with muscle highlights; reused in Onboarding 2.3 and Calibration 2.4.
- **`BsaTrendCard`** — large Vico sparkline of BSA-worst over time with dotted threshold reference lines (10%, 15%). Used on Home and AthleteDetail.
- **`TrendSparklineCard`** — generic Vico chart of one metric over time, with threshold reference lines. Used inside History for the multi-metric stack.
- **`MetricDeltaChip`** — compact chip with a metric label, current mono value, and signed delta vs previous. Used on Home for ES:GMax and H:Q.
- **`RecalibrationNudgeBanner`** — inline banner inside Edit Profile that prompts a re-calibration when bodyweight changes.
- **`LastSessionCard`** — used on Home and AthleteDetail.
- **`SessionRow`** — list row used in History and AthleteDetail.
- **`PendingBanner`** — top banner on Home for "calibration pending" / offline.
- **`EmptyHero`** — illustration + copy + CTA, used by every list/empty state.
- **`Skeleton`** — placeholder rectangles with shimmer for loading.
- **`StepProgressDots`** — 1/N visual indicator for onboarding and calibration.
- **`Countdown`** — big numeric timer for calibration.
- **`BottomSheet`** — confirmation sheet wrapper (used by QR scan, recommendation evidence).
- **`QrSurface`** — large, accessible, theme-aware QR renderer.
- **`CameraScannerSurface`** — CameraX preview with QR target overlay.
- **`MvcTable`** — 5×2 table of calibrated MVC values.
- **`ProfileSection`** / **`ProfileRow`** — consistent profile layout primitives.
- **`RoleSegmented`** / **`SexSelector`** / **`ThemeSegmented`** / **`FilterChips`** — small selectors.
- **`SetHeader`** — title + chips (kg, reps, RPE, variant, depth) used at the top of analysis and inside detail.
- **`CollapsibleSetCard`** — used in 3.5 to fold/unfold per-set details.

---

## 6. Routes (proposed)

A flat-ish naming scheme grouped by area. Where the current code already has a route, the existing name is used.

```
LOGIN
REGISTER

ONBOARDING_WELCOME
ONBOARDING_PROFILE
ONBOARDING_MVC_EXPLAIN
ONBOARDING_MVC_CAPTURE
ONBOARDING_DONE

ATHLETE_ROOT  (scaffold with 4 tabs)
  athlete/home
  athlete/session                 (pre-flight 3.2)
  athlete/session/active          (3.3 — set metadata + measuring + analysis as substates)
  athlete/session/{id}/summary    (3.3.d)
  athlete/history
  athlete/session/{id}            (3.5)
  athlete/profile
  athlete/profile/edit
  athlete/profile/pair-esp32
  athlete/profile/scan-qr
  athlete/profile/recalibrate     (reuses ONBOARDING_MVC_CAPTURE)

INSTRUCTOR_ROOT  (scaffold with 2 tabs)
  instructor/athletes
  instructor/guest/new
  instructor/guest/{id}/calibrate
  instructor/guest/{id}/session
  instructor/athlete/{id}
  instructor/athlete/{id}/transfer-qr
  instructor/qr/link
  instructor/profile
  instructor/profile/edit
```

Routes that are **modal/sheet** rather than full screens:
- Recommendation evidence drawer (from 3.3.c / 3.5).
- "Confirmar vinculación" sheet (from 3.9).
- "Desvincular" / "Cancelar transferencia" confirm dialogs.

---

## 7. Resolved decisions

> Reviewed and confirmed by the user. Each item is now a closed decision; the rationale is preserved for future audit. Where the proposal was modified, the change is noted explicitly.

1. **Session pre-flight (3.2)** — **Keep as a separate screen.** That's where the device-source toggle (simulated/real) lives transparently, per the rule in `CLAUDE.md` that the simulator must be visible to evaluators. Folding it into 3.3 would muddle the very first user gesture of a session.

2. **Session summary (3.3.d)** — **Include, minimal.** Total sets + duration + max load + a one-row-per-set table with risk badges + top 3 aggregated recommendations. Reuses components from 3.3.c. Do NOT add anything that isn't already in the persisted data.

3. **Edit personal data (3.7)** — **Include.** Allowing bodyweight/age/sex changes without re-onboarding is essential. **Important nudge**: when the user changes bodyweight, surface an inline suggestion "Considerá recalibrar tus MVC" — bodyweight changes invalidate the calibration baseline. Name and lastname remain read-only to avoid identity drift.

4. **Pair ESP32 (3.8)** — **Stub only, placeholder card.** A "Próximamente — vinculación con sensores físicos" surface plus an empty device list. No invested design time. Documented explicitly on the screen as out-of-MVP.

5. **Recommendation evidence** — **Bottom sheet, conditional.** Inline expansion bloats the analysis screen. The "Ver evidencia" affordance must only appear when `Recommendation.evidence` is non-empty — no empty buttons.

6. **History filtering** — **Post-MVP.** With the typical demo dataset (≤20 sessions) the month grouping is enough. If we have spare cycles, a single "Solo riesgo" chip is the only filter that adds real value.

7. **Trend visualisation on Home** — **MODIFIED FROM ORIGINAL PROPOSAL.** Not three stacked sparklines. Concretely:
   - **One large sparkline** for **BSA worst** (the protagonist metric per `CLAUDE.md`), with dotted reference lines at 10% and 15%.
   - **Below it, two compact chips**: ES:GMax current value + delta vs previous session, and H:Q current value + delta. No sparklines for these two on Home.
   - The full multi-metric trend stack lives in **History** for users who want depth. Home stays scannable.
   This avoids information overload on the most-visited screen and keeps BSA visually dominant — which matches the product positioning.

8. **Instructor ↔ registered athlete linking** — **Design the UI fully, ship functional only if backend lands.** The QR generate (4.6) and scan (3.9) screens must be designed because they're reused for guest transfer (4.7). For the delivery, if no backend, the linking is **mocked locally**: instructor generates a QR, athlete scans, both apps show "Vinculado" without cross-account persistence. Mark the local-mock behaviour clearly in the implementation so it doesn't get shipped as if it were real.

9. **Role switch in profile** — **Visible, labelled "Modo demo".** The professor will use this to test both roles in a single demo without re-login. The label clarifies it's not a production feature.

10. **Splash screen (0.1)** — **Implement a real one (~10 lines of Compose).** Brand mark + a subtle loading indicator while `LoginViewModel.checkSession()` resolves. Removes the brief Login flash currently visible.

---

## 8. Implementation order (Phase 3 priority)

A pragmatic ordering for the codebase as it stands today. Independent of the screen inventory order, this reflects what unblocks the most downstream work:

```
1. Theme refactor                 → unlocks ALL visual work; do first
2. Splash + Auth (repaint)         → first user entry, sets the visual tone
3. Athlete Home (dashboard, NEW)   → most-visited screen, biggest perceived change
4. Session flow (repaint + add 3.3.d summary)
5. History + SessionDetail (repaint)
6. Profile + Edit (repaint + NEW edit screen + theme toggle)
7. Onboarding flow (NEW — welcome + profile + MVC explain + capture + done)
8. Instructor screens + QR generate/scan (NEW)
9. Pair ESP32 stub (final, lowest priority)
```

**Total unique surfaces to design ≈ 15**, not 25 — several screens reuse the same composables with different headers/contexts (calibration in onboarding = calibration in instructor guest flow; session for guest = session for athlete; QR generate for link = QR generate for transfer with different copy).

**Tests-before-commit rule applies throughout.** New ViewModels and use cases get unit tests in the same commit as the production code, following the pattern established in `ComputeSetMetricsTest.kt`.

---

## 9. What is not a view

For completeness, things we will NOT design as full screens:

- App settings page beyond what's already on Profile — language is Spanish-only, we have no notification settings, no privacy policy view in MVP.
- "About OpenLifting" page — out of scope.
- Forgot password — out of scope.
- Real-time monitoring view — explicitly out of scope per `CLAUDE.md`.
- Deadlift screens — explicitly out of scope per `CLAUDE.md`.
- Any LLM/AI helper — out of scope.

---

## Summary count

- Auth: **2** screens
- Onboarding: **5** screens
- Athlete: **9** screens (Home, Pre-flight, 3 session substates, Summary, History, SessionDetail, Profile, Edit profile, Pair ESP32, Scan QR) — counting the session substates as one logical screen this is **9**, otherwise **11**.
- Instructor: **8** screens
- Cross-cutting: **1** real (Splash) + reusable error/empty layouts.

**Total ≈ 25 distinct user-facing surfaces**, of which **~15 are unique designs** to produce in Phase 2 — the remaining ~10 are reuses of the same composables under different routes/contexts (calibration in onboarding ≡ calibration for guest; session for guest ≡ session for athlete; QR generate for link ≡ QR generate for transfer with different copy). See §8 for the implementation order.
