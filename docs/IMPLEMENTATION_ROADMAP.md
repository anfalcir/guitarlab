# Implementation Roadmap

Updated: 2026-09-14

## M1 — Project/model foundation — CLOSED
Project model, templates, persistence baseline and repository structure.

## M2 — Android hardware/audio baseline — PASS/CLOSED
Original Samsung SM-X230 USB-audio baseline established. Historical Pocket Amp evidence remains valid for the tested combination only.

## M3 — Codec/import foundation — ABSORBED
Codec/import work was consolidated into later milestones.

## M4 — Studio playback/edit/mix foundation — ABSORBED
Timeline, playback, editing, Mixer/Master and project interaction foundations are integrated.

## M5 — Reliable recording + Studio consolidation + Media I/O — PASS/CLOSED
Closed after explicit physical approval of `0.2.0-alpha14`.

## M6 — Measured latency and synchronization — PASS/CLOSED
Closed after explicit physical approval of `0.3.0-alpha1`. Loopback calibration, route-scoped compensation, clocks, live Mute/Solo and project rename are no longer pending milestone work.

## M7 — Production audio polish — FINAL PHYSICAL GATE
Implemented scope includes:
- managed immutable sources plus validated editing proxies;
- bounded sample-rate conversion and editing-domain validation;
- non-destructive fades/crossfades with shared playback/export math;
- transactional recording and conservative interrupted-take recovery;
- live waveform during recording;
- fail-closed selected recording input;
- monitoring separated from recorded content;
- take management and active-take policy;
- practice markers, sections, automatic section suggestions and comparison modes;
- transient loop-derived punch recording with pre/post-roll and latency-aware retention;
- level analysis/suggestion;
- project/master export and package round-trip hardening.

M7 is not closed until the exact active RC passes the canonical automated gates and the residual Samsung SM-X230 + M-VAVE MK-300 physical checklist receives explicit approval.

## M8 — Release hardening — DIGITAL BASELINE GREEN / CURRENT RC3 DELTA PENDING
### M8.A — Compatibility + edge cases
Covered by deterministic regression for:
- legacy/current project migration;
- malformed/truncated package rejection and rollback;
- project duplication and managed-media handling;
- editing-domain/fade/frame validation;
- SRC/master-render regression;
- interrupted import/recording recovery;
- safe publication/cancellation behavior.

### M8.B — UX + accessibility + performance
Objective coverage includes:
- Compose accessibility/callback checks;
- lifecycle/activity recreation;
- target geometry instrumentation;
- structural and render performance evidence;
- large/fuzz project regression.

Residual target-device-only items remain tactile/visual ergonomics, real USB route performance and subjective monitoring/listening.

### M8.C — Release candidate progression
- `0.4.0-rc2` / versionCode 20: full API 36 + target-geometry + signed baseline PASS in CI #593.
- `0.5.0-rc1` / versionCode 21: recording/practice hardening candidate.
- `0.5.0-rc2` / versionCode 22: shared timeline/practice visual refinement.
- `0.5.0-rc3` / versionCode 23: active candidate containing final loop playback, section preview/clear, transient REC/punch and physical-review UX refinements.
- CI #613 / `db5a4208848e4b6ca2163ce715d0c5bb464cfe37`: complete software + API 36 + tablet geometry + signed homologation PASS; latest fully green RC3 baseline.

Current source advanced after #613 with section-boundary/rendering hardening, fixed Auto-seções slot geometry, shared Home help entry and the 3-second overlay countdown. Ordinary commits do not trigger hosted CI; one new exact-source manual run is required before promoting the updated APK.

## RC3 acceptance gates
RC3 may advance to final physical homologation only after one exact-source manual workflow run proves all of the following for the same `github.sha`:
1. unit/JVM regression PASS;
2. Android Lint PASS;
3. debug APK PASS;
4. instrumentation compilation PASS;
5. API 36 connected regression PASS;
6. isolated 1920×1200 geometry PASS;
7. signed homologation release PASS;
8. signer certificate equals the locked GuitarLab homologation identity;
9. APK SHA-256 and build identity are published by the workflow.

After that, `RC3_FINAL_PHYSICAL_HOMOLOGATION.md` is the only active manual checklist. Zero repeatable P0/P1 plus explicit user approval closes the residual M7/M8 release gate.

## Gate discipline
`.github/workflows/android-ci.yml` is canonical and manual-only. The signed homologation job depends on both `software-gate` and `android-integration-gate`; signing cannot bypass a failed mandatory gate. Documentation must distinguish source review, JVM/build evidence, emulator evidence and target-hardware evidence.

`CANDIDATE_IDENTITY_POLICY.md` is authoritative for active candidate identity. Historical candidate/checklist documents are evidence only and must not override the active RC3 documents listed in `DOCUMENTATION_MAP.md`.
