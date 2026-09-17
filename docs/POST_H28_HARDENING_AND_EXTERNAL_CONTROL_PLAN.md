# Post-H28 Hardening and External Control Implementation Plan

Updated: 2026-09-17  
Status: **APPROVED SCOPE / IMPLEMENTATION PENDING**  
Baseline: `0.5.0-rc3` / CI #653 / source `d09fc003e2ae2d699238eb39ba699f75a746fea4`

## 1. Purpose

This document is the authoritative implementation plan for the GuitarLab development line after H28. It deliberately separates:

1. **hardening/refinement of capabilities already present**, which must be completed before stable `1.0.0`; and
2. **one new feature family only**: external MIDI/footswitch control, planned after the stable hardening line.

The plan preserves the current guitar-first scope. It does not turn GuitarLab into a general-purpose DAW and does not reopen feature families explicitly excluded below.

## 2. Baseline and evidence boundary

The implementation baseline is the exact H28 signed candidate produced by CI #653:

- workflow: `GuitarLab Android CI` #653 / run `35207902169`;
- producer/source SHA: `d09fc003e2ae2d699238eb39ba699f75a746fea4`;
- package: `studio.guitarlab.app`;
- version: `0.5.0-rc3` / versionCode `23`;
- signed APK SHA-256: `1a36efcbe24d5995dd3609889237ca112670e52554e187d1b93ed5a8649263c0`;
- signer certificate SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- H28 materialization PASS;
- 294/294 JVM/unit tests PASS;
- Android Lint PASS with 0 errors;
- 32/32 standard API36 tests PASS;
- 1/1 isolated 1920×1200 geometry test PASS;
- software, API36 and signed-homologation jobs all PASS.

Target-device validation after #653 confirmed the corrected H28 backup/provider-consistency workflow is functioning correctly. The remaining RC3 physical blocker is recording latency/synchronization acceptance on the intended USB route.

## 3. Locked scope

### 3.1 Hardening/refinement to complete before 1.0.0

The following existing areas are approved:

1. recording synchronization and latency hardening;
2. user-facing interrupted-recording recovery;
3. USB audio resilience during real use, including disconnect/reconnect behavior;
4. takes-management refinement;
5. recording/session quality and stress coverage for realistic song projects through **10 minutes**;
6. diagnostics/reporting refinement;
7. H28 backup/restore regression hardening;
8. final UX/accessibility polish.

### 3.2 Only new feature approved

After the stable hardening line, add:

- **external MIDI/footswitch control**.

No other new feature family is part of this roadmap.

## 4. Explicitly excluded scope

The following items are intentionally excluded:

- marker/section workflow enhancements;
- clip-gain UI work;
- Reference × My Guitar comparison/loudness enhancements;
- a separate “large project” performance program or artificial mega-project workload;
- projects longer than 10 minutes as a release-quality target for this line;
- metronome/BPM;
- time-stretch;
- pitch-shift;
- stems;
- musical grid;
- trainer/setlist;
- comping expansion;
- freeze/bounce;
- tuner functionality;
- per-track independent physical output routing.

Existing regression coverage for currently implemented behavior remains mandatory even when that behavior is not being expanded.

## 5. Engineering principles

All implementation blocks must preserve these rules:

- no hard-coded global latency offset;
- explicit selected recording input remains fail-closed;
- playback/backing/monitoring may never feed the recording writer in software;
- source audio remains immutable; creative edits stay non-destructive;
- destructive actions are transactional and recoverable where practical;
- temporary/recoverable captured audio is never silently discarded;
- transient Android numeric device IDs are not durable hardware identity;
- normal user-facing flows use semantic language; raw technical identifiers remain in Diagnostics;
- every persistent schema addition has backward-compatible reading/migration tests;
- asynchronous results verify project/session identity before publication;
- CI remains manual-only through `workflow_dispatch`;
- ordinary source/docs commits use `[skip ci]`;
- source materialization remains deterministic, hash-verified, idempotent and fail-closed;
- no implementation block is promoted to DIGITAL PASS from source review alone.

## 6. Delivery sequence

The hardening line is split into four implementation blocks before `1.0.0`. External control follows as the first post-1.0 feature block.

---

## H29 — Recording synchronization closure and session health

### Objective

Close the remaining latency/synchronization risk without replacing the existing H23b timing architecture. H29 makes the timing model more observable and easier to validate while keeping compensation conservative.

### Existing foundation to preserve

- stable `AudioRecord`/`AudioTrack` timestamp-anchor acquisition;
- session capture/backing origin mapping;
- exact input + output + sample-rate calibration scope;
- current `LatencyCalibrationPolicy` acceptance thresholds;
- bounded residual fine adjustment;
- one-time final placement calculation;
- punch/pre-roll applied after final compensated placement.

### Implementation

1. Introduce a bounded **Recording Session Health** record for each completed/failed recording session containing, where available:
   - semantic selected/effective input and output identities;
   - sample rate;
   - timing evidence basis used by capture/playback;
   - capture/backing anchor quality and jitter;
   - computed session delta;
   - accepted route-latency term;
   - residual fine-adjustment term;
   - output fallback/route-change flags;
   - capture-frame count and final take placement;
   - underrun/dropout evidence when Android exposes trustworthy data.
2. Derive deterministic diagnostic health classes such as `OK`, `DEGRADED` and `INVALID FOR ROUTE-SPECIFIC COMPENSATION` from existing policy evidence. This must never invent a new compensation term.
3. Keep only bounded recent session-health history; no unbounded telemetry store.
4. Expose the latest session evidence in Diagnostics, not in the creative timeline.
5. Extend the existing one-action technical report so future late/early-take defects can be diagnosed without collecting audio content.
6. Confirm route/rate changes always invalidate incompatible calibration/fine-adjustment state for the affected take.

### Automated acceptance

- every supported timing-evidence combination has pure policy coverage;
- stale, backward and incompatible mixed-clock evidence remains rejected;
- compensation terms are demonstrably combined exactly once;
- no overflow/negative invalid placement under boundary values;
- route/rate key separation remains exact;
- diagnostic history remains bounded and deterministic;
- diagnostic evidence cannot mutate creative project state;
- save/reopen does not change take placement merely because diagnostics changed.

### Physical acceptance

On the exact signed candidate using the intended USB route:

- record repeated takes against a deterministic transient/backing reference;
- no repeatable systematic late/early displacement remains;
- changing sample rate or route never reuses incompatible calibration;
- unplug/replug never causes silent microphone fallback;
- lack of trustworthy timing evidence degrades conservatively instead of fabricating precision.

**H29 closes only after both automated evidence and target-device timing acceptance pass.**

---

## H30 — Interrupted-recording recovery UX and USB route resilience

### Objective

Turn already-existing recording-recovery primitives and fail-closed routing into complete user-safe workflows for abrupt interruption and USB route loss.

### A. Interrupted-recording recovery

The current recording media store already preserves payload-bearing `.recording.part.wav` data and supports repair of GuitarLab recording headers. H30 exposes this safely.

Implementation:

1. Detect recoverable interrupted recordings at project open and after process restart.
2. Never auto-delete payload-bearing interrupted recordings.
3. Present a concise recovery flow with useful metadata: approximate duration, creation time and target project/track when reconstructable.
4. Provide:
   - **Recover** — repair/validate and publish transactionally as managed take/clip;
   - **Preview** — only when repaired media is safely playable;
   - **Discard** — explicit confirmation required;
   - **Later** — leaves recoverable payload untouched.
5. Recovery is idempotent: reopening the app cannot publish the same recovered take twice.
6. Unsafe/unrecognized payload is preserved and reported rather than guessed or destroyed.
7. Recovery publication participates in project history/Undo where semantically valid without cleaning media still referenced by history.

Automated acceptance covers:

- valid interrupted GuitarLab WAV;
- zero-byte temporary;
- truncated header with valid payload;
- malformed/unrecognized payload;
- process death between repair and project publication;
- duplicate recovery attempt;
- project rename before recovery;
- cleanup only after explicit discard.

### B. USB audio resilience

Implementation:

1. Establish one explicit route/session state machine shared by playback and recording orchestration:
   - selected route available;
   - route starting;
   - active and confirmed;
   - degraded/lost;
   - finalizing/stopped;
   - compatible route reappeared.
2. During REC, route mismatch/loss must:
   - stop/finalize safely;
   - preserve all already-captured valid audio;
   - identify the reason clearly;
   - never silently switch to microphone input.
3. During Play, output loss follows only an explicitly safe existing fallback/stop policy and produces one meaningful message rather than feedback storms.
4. Reconnect uses semantic physical-route identity, never a stale transient Android numeric ID.
5. REC never resumes automatically after reconnect; a new recording requires explicit user intent.
6. A confirmed compatible reconnect may restore selection for the next operation.
7. Route changes invalidate session-local timing evidence and incompatible route-specific compensation exactly once.

Automated acceptance covers disappearance/reappearance, transient-ID changes, same-model identity handling, no double finalization, capture preservation and stale-session rejection.

Physical acceptance covers hot-unplug during idle, Play, countdown and REC, followed by reconnect and validation of route identity, no microphone fallback and preservation of valid pre-unplug capture.

---

## H31 — Takes management and diagnostics refinement

### Objective

Make the existing non-destructive take model fully manageable and make Diagnostics capable of explaining audio failures without polluting the creative UI.

### A. Takes management

Preserve the invariant that a track may retain multiple takes and exactly one is active whenever takes exist.

Implementation:

1. Add a dedicated take manager scoped to the selected track.
2. Support:
   - activate/use take;
   - rename take;
   - delete one take with explicit confirmation;
   - optional short note;
   - favorite flag;
   - deterministic ordering with active/favorite state visible;
   - rapid audition through the existing playback engine, not a parallel engine.
3. Deleting the active take deterministically chooses a valid fallback when another take remains.
4. Deleting the final take leaves the track valid and never deletes source media still referenced elsewhere.
5. Split/moved clip lineage rules remain intact.
6. Rename/note/favorite changes are persisted, Undo/Redo-aware and backward compatible.
7. `.guitarlab`, project duplication and backup/restore preserve new take metadata.

Automated acceptance covers active-take invariants, shared-media protection, split lineage, Undo/Redo, save/reopen, duplicate, portable-project round trip, backup/restore and legacy projects with safe defaults.

### B. Diagnostics refinement

Extend the existing diagnostic surface rather than create another competing tool.

A coherent support report should contain:

- app version/source identity when available;
- semantic selected/effective routes;
- sample rate/channel/encoding facts;
- buffer and underrun evidence exposed by Android;
- calibration median/jitter/drift/confidence/attempt count;
- bounded Recording Session Health evidence from H29;
- last meaningful route failure/reconnect reason;
- no project audio samples and no secrets.

The default report is sanitized: private filesystem paths and unstable raw identifiers stay out unless intentionally placed in an advanced technical subsection. Report generation must never block or alter the audio route.

---

## H32 — 10-minute song-quality gate, backup regression and final UX/accessibility hardening

### Objective

Create the stable-release quality boundary around the real product model: GuitarLab projects are songs. This line guarantees representative quality through **10 minutes** instead of inventing an artificial ultra-large DAW workload.

### A. Song/session quality and stress

Duration matrix:

- 1 minute;
- 3 minutes;
- 5 minutes;
- 10 minutes.

Coverage exercises realistic combinations of recording, playback, live waveform, save/reopen and export.

Automated/programmatic requirements:

- frame-count/duration invariants through 10 minutes;
- live-waveform storage remains bounded and covers the full captured interval uniformly;
- no progressive timing error caused by callback cadence or waveform compaction;
- save/reopen retains exact clip/take placement;
- representative WAV/FLAC render duration/channel invariants;
- cancellation/cleanup at representative long-running boundaries;
- no NaN, Infinity or negative-frame state;
- memory growth is bounded by designed waveform/buffer policy rather than duration-proportional UI state;
- use track/clip complexity appropriate to a song, but create no independent “mega-project” product requirement.

Physical residual:

- one continuous 10-minute target-device recording/playback exercise on the intended route;
- no repeatable dropout, wrong pitch/speed, runaway waveform accumulation or growing synchronization offset;
- save/reopen and one representative export after the run.

### B. Protected H28 backup/restore regression

H28 is already functionally accepted on target hardware. It becomes a protected contract, not a new feature stream.

Mandatory coverage:

- unchanged repeated backup remains idempotent;
- rename preserves project identity/history;
- one project edit creates exactly one new logical revision;
- retention maximum keeps newest unique revisions;
- provider listing delay creates neither false failure nor duplicate upload;
- restart/reboot convergence;
- restore-one and restore-all do not silently overwrite unrelated local projects;
- destination change/disconnect/revocation is non-destructive;
- incomplete/cancelled upload never becomes a valid restorable version;
- legacy H26/H27 metadata remains readable.

### C. Final UX/accessibility polish

Perform a complete pass over Home, Studio, Options, Backup, Recovery, Takes and Diagnostics:

- TalkBack labels and logical focus order;
- touch-target sizes;
- destructive-action confirmations;
- font-scale stress;
- responsive tablet/phone layouts;
- loading/empty/error/retry states;
- consistent transient-feedback cooldown behavior;
- no development jargon in normal creative flows;
- advanced Diagnostics may remain intentionally technical;
- no duplicate/fake-enabled controls;
- shared Help updated in the same block as user-visible workflow changes.

### H32 exit / 1.0 readiness

Before stable `1.0.0`:

- H29-H32 automated suites pass on exact source;
- Android Lint/build/release provenance pass;
- API36 standard regression + isolated target geometry pass;
- signed artifact package/version/certificate/zipalign identity passes;
- physical recording latency/synchronization PASS;
- USB hot-unplug/reconnect PASS;
- 10-minute quality smoke PASS;
- H28 backup regression smoke PASS;
- no repeatable P0/P1;
- explicit approval of the exact signed APK.

No external-control feature is allowed to destabilize stable-release closure.

---

## 1.0.0 — Stable release

Stable 1.0 is cut only after H29-H32 are complete. It is not blocked on MIDI/footswitch work.

Definition of Done:

- deterministic software/build/signing evidence;
- no repeatable P0/P1;
- accepted real-device recording alignment;
- safe USB disconnect/reconnect behavior;
- user-facing interrupted-recording recovery;
- robust takes management;
- useful sanitized diagnostic reporting;
- verified song/session quality through 10 minutes;
- protected H28 backup/restore behavior;
- final UX/accessibility acceptance.

---

## H33 / 1.1 — External MIDI/footswitch control

### Objective

Add hands-free control by routing external hardware events into the **existing** GuitarLab commands. External controllers are input affordances, never an alternate transport/recording implementation.

### Supported controller classes

Initial scope:

1. Android MIDI devices exposed through the platform MIDI API, including USB MIDI and Bluetooth MIDI when Android exposes them as standard MIDI devices;
2. keyboard/HID-style footswitches that Android exposes as key events, using an explicit opt-in mapping layer.

Out of initial scope:

- proprietary vendor BLE reverse engineering;
- arbitrary system-wide key interception;
- audio-route changes caused by control devices;
- MIDI clock/tempo synchronization, because BPM/metronome is outside this roadmap.

### Architecture

Use one transport-agnostic control domain:

`external event -> normalized control token -> user mapping -> ExternalControlAction -> existing ViewModel/domain command`

No mapped action may bypass existing transport guards, route validation, confirmation policy or recording finalization.

Initial actions:

- Play/Stop;
- REC toggle through the same guarded start/finalize path used by the UI;
- Return to start;
- Loop toggle;
- Undo;
- Redo.

Context-sensitive/destructive actions remain unmapped until they have an explicit safety contract.

### Mapping and UX

1. `Opções > Controle externo` owns controller configuration.
2. Enable/disable is explicit.
3. Provide **Learn** mode: select app action, press the desired footswitch/MIDI control, confirm mapping.
4. MIDI normalization distinguishes Note On/Off and CC transitions so press/release cannot double-trigger an action.
5. HID mapping accepts explicit keys only while GuitarLab is foreground/focused.
6. Debounce/repeat policy is deterministic per action.
7. Mappings persist with stable descriptors where possible, not transient Android device IDs.
8. Hot reconnect restores mappings only after compatible identity is confirmed.
9. Studio remains fully usable without a controller.
10. Controller/device status belongs in Options/Diagnostics rather than permanently consuming timeline space.

### Safety rules

- REC remains subject to armed-track, permission, route, project and countdown/revalidation requirements;
- external Stop/REC during capture funnels into the existing idempotent finalize path;
- repeated/bounced events cannot create duplicate REC start/finalize;
- Undo/Redo remains rejected when the existing app state disallows it;
- controller disconnect never changes audio-device selection;
- control-device disappearance never stops audio unless the audio route independently changed.

### Automated acceptance

- MIDI parser/normalizer: Note On, Note Off, velocity-zero Note On, CC and duplicate/reordered events;
- HID key mapping/debounce;
- mapping persistence/migration;
- reconnect under changed transient device ID;
- command parity: external action produces the same state transition as the corresponding on-screen action;
- duplicate press/release cannot double-start or double-finalize REC;
- lifecycle/recreation while controller connected;
- accessibility/semantics for mapping UI.

### Physical acceptance

Using at least one real supported controller:

- connect/select/map;
- operate Play/Stop/REC/Return/Loop hands-free;
- reconnect and verify mapping restoration;
- verify no press/release double-trigger;
- verify recording guards and audio-route independence;
- complete a representative session with controller attached without audio-quality regression.

## 7. Cross-cutting test strategy

Every block extends the existing regression system instead of creating a parallel framework.

### JVM / pure Kotlin

Use for:

- state machines;
- timing/latency policies;
- take invariants;
- recovery publication policy;
- external-control normalization/mapping;
- deterministic report generation/sanitization;
- backup identity/retention/idempotency;
- duration/frame arithmetic.

### Android instrumented

Use for:

- Compose semantics/accessibility;
- lifecycle recreation;
- Recovery/Takes/Diagnostics/External Control UI;
- controlled route/device events where Android APIs can be abstracted;
- MIDI/HID UI integration;
- API36 regression;
- isolated 1920×1200 target geometry.

### Physical-only residual

Keep manual work limited to what emulator/software cannot prove:

- real recording latency/alignment;
- real USB route retention/hot-unplug/reconnect;
- actual 10-minute capture/playback stability;
- real controller compatibility and tactile double-trigger behavior;
- subjective monitoring/listening sanity.

Objectively established automated evidence must not be re-added to the manual checklist without a concrete reason.

## 8. CI and release discipline

For H29 onward:

1. confirm remote `main` HEAD immediately before every write;
2. version each source block reproducibly and extend the canonical materialization chain from H28;
3. verify final Git blob hashes for materialized sources;
4. prove first materialization, idempotent second materialization and corrupted-part fail-closed behavior;
5. run local/source checks whenever available;
6. publish ordinary commits with `[skip ci]`;
7. **never dispatch or rerun GitHub Actions automatically**;
8. the user manually dispatches the canonical workflow when a block is ready;
9. promote to DIGITAL PASS only after auditing the actual run and exact artifact identity;
10. execute only the reduced physical residual genuinely required by that block.

## 9. Completion criteria

### Stable 1.0.0

The hardening program is complete only when H29-H32 are closed and the exact stable candidate satisfies all digital and physical criteria above.

### External control / 1.1

The approved roadmap is fully complete when H33 additionally provides tested MIDI/HID footswitch control with command parity, safety guards, persistent mappings and real-hardware acceptance without regressing the 1.0 audio-quality baseline.
