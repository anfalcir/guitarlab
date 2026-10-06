# CUE dual-output capability — implementation plan

Date: 2026-10-06
Status: **ACTIVE IMPLEMENTATION PLAN — no product capability promotion**
Scope: Android Studio playback/recording on the target Samsung SM-X230 / Android 16 / API 36, with M-VAVE MK-300 over USB as MAIN and a wired tablet headset as the requested CUE.

## 1. Objective

Provide a robust, evidence-based CUE workflow in which:

- the complete Studio mixer reaches MAIN only;
- only tracks explicitly routed to `CUE` (in the owner workflow, the original reference guitars) reach CUE only;
- loss, convergence or uncertainty of CUE never leaks reference audio to MAIN and never stalls MAIN;
- the application distinguishes an unavailable Android/HAL capability from an app defect; and
- a user receives an actionable result instead of a generic routing failure.

This plan does **not** promise that every Android tablet can split two `USAGE_MEDIA` `AudioTrack`s between two physical devices. Android output preference is not proof of effective routing; only the route observed while each track is playing is authoritative.

## Implementation checkpoint — 2026-10-06

Source implementation now covers G0 and the principal G1–G2 safety paths:

- core probe reports actual canonical route sets and typed missing/converged/wrong-or-mirrored/lost outcomes;
- Android preflight returns bounded structured evidence, including format/track/preference/write/route data, and diagnostics export a versioned JSON object;
- `CueRouteController` owns cancellation, process-session cache, retry, journaling and device-topology invalidation;
- Settings persists only `SUPPORTED` and explains `CONVERGED_TO_MAIN` as an unsupported combination;
- playback proves preferred MAIN after activation, listens for route changes, retains polling, reprimes runtime queues after preflight and keeps CUE non-blocking.

Targeted qualification passed with a temporary Java 17/Gradle 9.6.1 toolchain: 81 `core:audio`, 17 `platform:audio-android` and 121 app unit tests completed with zero failures; platform/app Kotlin compilation passed; and platform audio lint passed with two unrelated pre-existing warnings. The full app lint exceeded the local operational window without producing a report and was stopped, so it remains pending alongside Android instrumentation, release assembly and physical API 36 execution. W5 currently records advertised and actual format evidence but does not introduce resampling or claim fallback-rate support. W6 and G4/G5 necessarily remain owner/hardware decisions. Therefore this checkpoint is digitally compiled/tested source evidence only and must not be described as a working dual-output release.

## 2. Evidence and diagnosis baseline

The owner supplied a screen recording and a diagnostic bundle captured immediately after the test. The bundle is internally intact (archive CRC and all 12 exported SHA-256 entries validate) and identifies:

- app: `studio.guitarlab.app`, `0.5.0-rc29` / `49`, `release`;
- device: Samsung `SM-X230` (`gta11pwifi`), Android 16 / API 36;
- requested MAIN physical key: `usb|usb-audio - mk300|card=1`;
- requested CUE physical key: `wired-jack|`;
- probe sample rate: 44.1 kHz;
- observed MAIN route: `usb|usb-audio - mk300|card=1`;
- observed CUE route: `usb|usb-audio - mk300|card=1`.

The current `ROUTE_UNCONFIRMED` result is therefore a proven **effective-route convergence**: the Android AudioPolicy/HAL routes the CUE stream to MK-300 even though the wired headset is present and accepted as a preferred device. It is not:

- missing device discovery;
- a USB logical-endpoint alias problem;
- a route-settlement timeout alone; or
- a timestamp, initial-offset or drift rejection.

The existing fail-closed behavior is correct: admitting that pair would send the reference guitar to the monitor mix. The target pair must remain unavailable unless a later exact test proves otherwise.

## 3. Product decisions and non-negotiable invariants

1. **No CUE-to-MAIN fallback.** A track set to `CUE` stays out of MAIN when CUE cannot be proven. Main playback may continue without CUE.
2. **Preference is not proof.** `AudioTrack.setPreferredDevice()` is an input to routing; the only admission proof is the complete effective routed-device set while both tracks play.
3. **A physical pair, not a device label, is the capability.** Android logical IDs are session-local. Capability decisions use canonical physical identities plus device/OS/build context for diagnostics.
4. **CUE capability is conditional.** A visible wired headset is a candidate, not a supported CUE endpoint. UI and documentation must make that distinction clear.
5. **Clock tests occur only after route proof.** Never call an unproven routing failure a synchronization failure.
6. **No magic timing workaround.** Do not hide policy convergence by increasing timeouts, injecting a fixed delay, accepting an unverified route, or silently changing `AudioAttributes`.
7. **Physical proof remains mandatory.** Emulator/JVM tests prove policy and regressions; they cannot prove Android AudioPolicy, HAL routing, monitor isolation or audible alignment on this hardware.
8. **Export semantics remain independent.** `CUE` is live monitoring/playback routing, not an exclusion from the Studio Master render.

## 4. Target architecture

```text
Audio device callback ─┐
                         ├─> CueRouteController ─> Session capability state ─> Settings / Mixer UI
User device selection ──┘             │                         │
                                      │                         └─> diagnostics bundle
                                      v
                              AndroidCueRouteVerifier
                                      │
                 MAIN AudioTrack <── effective route evidence ──> CUE AudioTrack
                                      │
                                      v
                       Playback engine: isolated MAIN/CUE buses, runtime route guard
```

### 4.1 Capability state model

Add a platform-neutral state model in `core:audio`; Android maps framework observations into it.

| State | Meaning | UI / transport consequence |
|---|---|---|
| `NOT_CONFIGURED` | MAIN or CUE was not explicitly selected. | CUE controls unavailable; MAIN remains normal. |
| `CANDIDATE_AVAILABLE` | Both endpoints are connected, distinct candidates. | User may start validation; this is not approval. |
| `VERIFYING` | Tracks are active and evidence is being collected. | Disable selection changes; show cancellable progress. |
| `SUPPORTED` | Exact pair, clocks and startup conditions qualified. | Persist CUE selection; enable stopped-state CUE assignment. |
| `CONVERGED_TO_MAIN` | CUE track effectively routes to MAIN. | Do not persist CUE; show unsupported-combination explanation. |
| `MISSING_EFFECTIVE_ROUTE` | One track exposes no route before deadline. | Do not persist CUE; request reconnection/retry. |
| `WRONG_OR_MIRRORED_ROUTE` | A stream reaches an unexpected or additional physical endpoint. | Do not persist CUE; fail closed. |
| `CLOCK_UNAVAILABLE` / `OFFSET_EXCEEDED` | Routes are correct but synchronization cannot be proven. | Do not persist synchronized CUE; report the specific reason. |
| `LOST` | A qualified route disconnects or changes at runtime. | Silence/release CUE immediately; MAIN continues when safe. |

`CONVERGED_TO_MAIN` is distinct from generic `ROUTE_UNCONFIRMED`. This is the mandatory classification for the proven SM-X230/MK-300/wired-headset result.

### 4.2 Evidence model

Replace the lossy transition string with a bounded, sanitized `CuePreflightEvidence` record. Each sample contains:

- elapsed time and probe phase;
- expected and preferred canonical physical keys;
- actual routed canonical keys for MAIN and CUE; logical IDs/types only as transient diagnostic fields;
- track state, requested/configured sample rate, channel mask, encoding, buffer capacity/size, start threshold and actual performance mode;
- `setPreferredDevice()` outcome before and after `play()`;
- non-blocking write count/result for each track;
- timestamp availability, frame position and monotonic time; and
- the exact terminal state/reason.

Keep at most the first 64 samples, each physical-route transition, and the last 64 samples. Addresses must remain canonicalized/redacted in exported diagnostics. The UI receives a localized reason; raw evidence belongs only in the support bundle.

### 4.3 Capability cache

Do not permanently persist a hardware rejection. Cache a result only for the current process/session and invalidate it on:

- `AudioDeviceCallback` add/remove events;
- MAIN/CUE selection change;
- app process recreation;
- operating-system/app update; or
- explicit user retry.

A session cache avoids repeated five-second probes while preserving the possibility that a reconnect or platform update changes the result.

## 5. Implementation workstreams

### W1 — Make convergence diagnosable

**Goal:** turn the present evidence into an explicit, testable product state.

1. Extend the core probe contract so Android can report actual physical route sets, not only `expectedId` or `null`.
2. Add failure reasons for convergence-to-MAIN, absent route, unexpected route and mirrored route.
3. Refactor `AndroidCueRouteVerifier` to assemble `CuePreflightEvidence` and return a typed outcome.
4. Export typed outcome and bounded evidence in `audio-route.json`; retain a short human summary for the UI.
5. Add an application diagnostic-journal event when validation starts and finishes. It must include no song audio, credentials or raw identifying device address.

Likely touched areas:

- `core/audio/.../CueStartupProbe.kt` and a new capability/evidence model;
- `platform/audio-android/.../AndroidCueRouteVerifier.kt`;
- `app/.../diagnostics/DiagnosticBundleExporter.kt` and journal integration;
- Settings state/UI tests.

**Done when:** the same target test states `CONVERGED_TO_MAIN` and records that MAIN and CUE both reached MK-300.

### W2 — Treat CUE support as a route-pair capability in UI

**Goal:** prevent a present-but-unsupported wired headset from appearing like an app malfunction.

1. Introduce `CueRouteController` as the sole owner of preflight lifecycle, cancellation, session cache and route-change invalidation.
2. Keep device enumeration in `StudioAudioRoutingStore`; move admission/result ownership out of composable-local state.
3. Show candidate availability separately from capability result:
   - `Fone com fio detectado`;
   - `Verificando MAIN + CUE…`;
   - `Não suportado nesta combinação: Android redirecionou CUE para USB-Audio - MK300.`
4. Provide `Testar novamente` only after the current validation completes or a device change occurs.
5. Preserve the current rule: a rejected CUE selection never changes track metadata or project history.
6. On the proven target pair, retain `Desativada` as the effective CUE state; do not save the unsupported request as a working selection.

**Done when:** the user can distinguish “fone desconectado”, “fone disponível porém convergente”, “clock não comprovado” and “CUE pronto”, with no ambiguity about whether reference guitar will reach MAIN.

### W3 — Harden MAIN and runtime route lifecycle

**Goal:** apply the same truthfulness to MAIN and react promptly to route changes.

1. After `play()`, verify MAIN effective routing even when no CUE track is required. A successful preferred-device call alone must not produce `usingPreferredOutput = true`.
2. Register `AudioRouting.OnRoutingChangedListener` on every live MAIN/CUE `AudioTrack`; retain polling only as a defensive fallback.
3. Register one lifecycle-owned `AudioDeviceCallback` for device add/remove. Invalidate route capability, clear stale endpoint caches and notify the controller.
4. On CUE loss/convergence, atomically silence/release CUE, post one deduplicated warning and never block the MAIN writer.
5. On MAIN loss or mismatch, follow existing selected-output safety policy; do not report a preferred route as active without effective evidence.
6. Include effective MAIN/CUE route labels and transient IDs in live session diagnostics, never as persisted device preference identifiers.

**Done when:** a headphone unplug, USB disconnect or route convergence is reflected once, promptly and safely during Play and backing playback during recording.

### W4 — Make startup deterministic after a successful preflight

**Goal:** avoid a validated silent probe being followed by an uncoordinated real playback start.

1. After probe cleanup, build fresh playback tracks or explicitly reset all route/startup state; do not treat a flushed probe as a ready musical stream.
2. Determine each track's actual buffer capacity and start threshold. Prime both buses with the first rendered frames before requesting `play()`.
3. Use one startup transaction: configure routes, prime both streams, call `play()` in a bounded sequence, verify effective routes, then begin the render loop.
4. Measure and log the first runtime route/timestamp evidence independently from selection-time preflight.
5. Keep CUE writes non-blocking. MAIN must remain the transport clock and must not wait for a slow CUE sink.
6. Retain the existing offset/drift checks until target-device evidence demonstrates a justified replacement. Any calibration must be route-pair-specific and measured, never a global fixed adjustment.

**Done when:** a qualified pair survives repeated starts/seeks/loops without false initial drift or CUE backpressure stalling MAIN.

### W5 — Sample-rate and latency capability negotiation

**Goal:** separate routing support from a format/latency mismatch.

1. Capture device-advertised formats/rates and the actual `AudioTrack` configuration for both endpoints.
2. Probe an ordered, explicit format matrix appropriate to the project: current project rate first, then 48 kHz and 44.1 kHz where both endpoints advertise or leave rates unspecified.
3. Record why a configuration was skipped, rejected, converged, clock-unstable or successful.
4. Do not silently convert project media merely to make a route appear supported. If an accepted pair requires a common render rate, add a tested resampling boundary with exact timeline-frame accounting.
5. Keep `USAGE_MEDIA` for Studio playback unless a separately approved product decision changes its semantics. Changing attributes only to coerce an OEM route is not an acceptable fix.

**Done when:** diagnostics can distinguish policy convergence from format initialization or clock limitations, and no rate conversion changes pitch, duration or recording placement.

### W6 — Choose the supported hardware path

**Goal:** make the required workflow achievable rather than endlessly retrying an unsupported pair.

#### Path A — Continue with the current tablet + MK300 + tablet headset

This path is supported only if W1–W5 produce an actual distinct effective route pair on the exact hardware. The current evidence rejects it for the tested media configuration. No release claim may describe it as dual-output capable until a later signed-candidate physical acceptance passes.

#### Path B — Single interface with independent output buses (recommended)

Use a class-compliant USB interface/mixer that exposes independent monitor and headphone/AUX outputs, preferably at least four playback channels. Route MAIN to outputs 1–2 and CUE to outputs 3–4 under one USB device/clock. Before procurement or implementation, verify on the target tablet:

- Android exposes the required output channel count/masks;
- the interface actually maps channels 1–2 and 3–4 to separate physical buses;
- the public `AudioTrack` path supports the selected multichannel configuration; and
- monitor/headphone isolation is audible and repeatable.

This route needs a separate multichannel render design and physical qualification. It is not a cosmetic extension of the existing two-stereo-track implementation.

#### Path C — External hardware mixer with true auxiliary/headphone bus

Use an external mixer/interface that creates the separate headphone mix in hardware. The Android app supplies the intended buses to hardware that is capable of preserving them. Document the cabling and the hardware bus mapping as part of the target configuration.

**Explicit non-solution:** adding Oboe/AAudio may improve latency and device quirks, but cannot make an AudioPolicy/HAL expose two independent physical routes that it currently converges. Evaluate it only after a supported transport design exists.

## 6. Test strategy

### 6.1 Core/JVM tests

Add deterministic fakes for:

- expected distinct pair;
- CUE converging to MAIN during settlement;
- empty/missing route;
- wrong physical route;
- mirrored extra physical route;
- delayed route settlement;
- route loss after qualification;
- timestamp temporarily unavailable, stale, regressive and stable;
- offset at boundary and above boundary;
- partial/zero/error CUE writes; and
- controller cancellation, device-change invalidation and session-cache invalidation.

Assert typed terminal reasons, evidence truncation/order and that no unsupported outcome marks CUE as persisted/active.

### 6.2 Android/instrumented tests

Cover:

- Settings labels and disabled/enabled actions for every capability state;
- rejected CUE preserving MAIN track routes and undo/redo history;
- MAIN effective-route mismatch reporting;
- route callback and device callback lifecycle cleanup;
- playback and recording backing paths using the same controller contract;
- diagnostic bundle schema/redaction/integrity; and
- API 36 multi-device route-set behavior.

Emulator assertions must not claim physical dual-output support.

### 6.3 Exact physical acceptance

For each candidate, record app version/code, signed artifact identity, tablet build, USB interface identity, headset type, selected rate and exported diagnostic bundle. Test:

1. Device discovery and candidate labels.
2. Twenty select/disable/reselect cycles without stale state.
3. Separate audible markers: MAIN-only reaches monitors/MK300, CUE-only reaches headphones, neither leaks to the other destination.
4. Musical workflow: backing/all non-reference tracks on MAIN; reference guitars only on CUE; user can play along without reference guitar in monitors.
5. Start, stop, seek, loop and 30-minute playback stability.
6. Backing playback while recording, confirming captured input remains isolated.
7. Headset removal/reinsert, USB removal/reinsert, Android audio-focus interruption and app background/foreground transition.
8. Export diagnostics immediately after every failure, in the same app process.

### 6.4 Acceptance criteria

A pair is accepted only when all are true:

- effective MAIN/CUE routes match the selected distinct physical identities throughout admission;
- no additional physical mirroring exists;
- startup and runtime clock/offset/drift policy pass;
- CUE-only audible content is isolated from MAIN and MAIN content is absent from CUE where the configured routing requires that;
- disconnect/reconnect fails closed; and
- the exact signed candidate passes relevant software, API 36 and physical gates.

Failure of any item means the pair is unavailable, not partially supported.

## 7. Delivery sequence and gates

| Gate | Deliverable | Required evidence | Exit condition |
|---|---|---|---|
| G0 | W1 typed evidence | JVM tests + diagnostic schema test | Existing target failure is reported as `CONVERGED_TO_MAIN`. |
| G1 | W2 truthful capability UX | UI/instrumented tests | Unsupported pair cannot be mistaken for an app error or active CUE. |
| G2 | W3/W4 runtime hardening | JVM + API 36 regression | MAIN/CUE lifecycle has route proof, priming and loss handling. |
| G3 | W5 format evidence | Target diagnostic matrix | Cause is classified for each tested pair/configuration. |
| G4 | Hardware path decision | Owner physical result | Current pair is explicitly retained as unsupported or a new hardware topology is selected. |
| G5 | Supported-pair promotion | Exact signed APK + physical acceptance | Product documentation may claim the exact tested CUE configuration. |

No signed candidate is promoted merely because G0–G3 are green. G4/G5 remain mandatory for the feature claim.

## 8. Rollback and operational safety

- Every unsupported/unknown state leaves MAIN operational and CUE silent.
- A diagnostic failure must not delete project routing metadata, audio assets, recordings or calibration data.
- Capability cache is session-only; clearing it cannot force an unsafe route.
- If the new controller fails unexpectedly, preserve the present fail-closed behavior: disable CUE and surface a supportable error.
- Do not change cloud separation, export rendering, backup, project schema or recording input policy unless a directly affected test proves a required integration change.

## 9. Documentation updates on completion

When work advances, update only truthful live contracts:

- `CURRENT_STATE.md` for the active evidence/result;
- `DECISIONS.md` if a durable transport/capability decision is made;
- `STUDIO_OPTIONS_AND_MIXER.md` for visible routing behavior;
- `ARCHITECTURE.md` for the controller/evidence boundary; and
- `TEST_AND_HOMOLOGATION_POLICY.md` for any newly required physical topology.

On closure, retain this document as dated evidence. `RELEASE_BASELINE.md` changes only after a new exact signed candidate receives owner physical acceptance.
