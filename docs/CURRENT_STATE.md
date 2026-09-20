# Current State — GuitarLab Studio

Updated: 2026-09-20

## Active line
- Repository/branch: `anfalcir/guitarlab` / `main`.
- Version: `0.5.0-rc3`, versionCode `23`, package `studio.guitarlab.app`.
- Current signed source line: CI #663 producer `51d4098fa7b1b44a9fa315e939541020f594654d`.
- Latest signed DIGITAL PASS: **CI #663** / run `35523442620` / producer `51d4098fa7b1b44a9fa315e939541020f594654d`.
- #663 scope: H28 → H29 → H30 → H31 → H32 → H33 → H33a → H33b → H34 → H35 → H35a → H36 → H36a → H36b → H36c.
- Signed APK SHA-256: `8e6e0f555bc5e834c4bccbdb011134c42d124806ce0316dd28787a6c98fe7bf3`.
- Signed APK size: `13,835,802` bytes.
- Locked signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.
- CI remains manual-only (`workflow_dispatch`).

## #663 digital evidence
All three canonical jobs passed on the exact producer SHA:
- deterministic materialization through H36c PASS;
- **330/330 JVM/unit tests PASS**, 0 failures/errors/skips;
- Android Lint PASS with **0 errors, 50 warnings and 4 hints**;
- debug/release assembly and unsigned provenance PASS;
- unsigned tested APK SHA-256 `215315f8b943704b9b820f98b4b7747df2fbbc62b862ab08e6c14d4dea9e89ad`;
- **34/34 standard API36 instrumented tests PASS**;
- **1/1 isolated 1920×1200 geometry PASS**;
- exact tested-artifact signing, zipalign, package/version and certificate verification PASS;
- signed APK SHA-256 `8e6e0f555bc5e834c4bccbdb011134c42d124806ce0316dd28787a6c98fe7bf3`.

Locked signer SHA-256 remains `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

## H28 physical backup result
Target-device testing after #653 confirmed the corrected backup behavior is working correctly. In particular, the false commit-failure/selective-duplicate behavior that triggered H28 is no longer reproduced in the accepted backup workflow.

H28 remains a protected regression contract covering immutable `projectId`, deterministic `revisionId`, package SHA-256 integrity, provider-lag tolerance, idempotent unchanged backup and bounded retention.

## H29-H36c digital closure
H29-H36c are digitally closed by CI #663.

H34/H35/H35a are now part of the exact signed candidate:
- global route/rate manual residual range is ±500 ms and applies only to future recordings;
- persistent take-specific synchronization is delta-based, non-destructive and Undo/Redo-aware;
- silent digital verification uses PCM zero and never stores fake physical round-trip latency;
- physical calibration validates the exact live input/output IDs before emitting a deterministic 32 ms windowed chirp capped at 12% peak;
- the H35a explicit RECORD_AUDIO guard passed Android Lint without suppression.

## H36/H36a/H36b/H36c — Settings UX Polish — DIGITAL PASS
H36 is now part of the exact signed #663 candidate.

Closed behavior:
- main Settings content is centered/capped on wide tablet layouts;
- section chrome and secondary actions use the compact responsive hierarchy;
- the calibration modal exposes all ±25/±5/±1 ms controls without horizontal scrolling;
- Settings/External Control/diagnostics/calibration instrumented coverage passes on API36;
- H36a/H36b/H36c remain traceable test-only correctives; runtime `SettingsScreen.kt` is unchanged from H36.

CI progression is retained as evidence: #660 exposed a test-import issue, #661 exposed three viewport assumptions, #662 reached 33/34 instrumentation PASS, and #663 closed the complete canonical gate at 34/34 + geometry + signing.

## Remaining RC3 physical blocker
The remaining release-critical target-only item is **recording latency/synchronization acceptance** on the intended real USB route. The recording timing architecture is already implemented and digitally covered; the unresolved boundary is physical driver/hardware behavior.

Final RC3/1.0 promotion still requires no repeatable P0/P1 and explicit approval of the exact signed candidate.

## Approved next-development scope
The authoritative plan is `POST_H28_HARDENING_AND_EXTERNAL_CONTROL_PLAN.md`.

Before stable 1.0.0, the #663 baseline is digitally closed; only the remaining hardware-only H29/H30/H32/H35 residual must now be physically closed:
- recording synchronization/session-health hardening;
- interrupted-recording recovery UX;
- USB audio disconnect/reconnect resilience;
- takes-management refinement;
- song/session quality and stress coverage through 10 minutes;
- diagnostics refinement;
- H28 backup/restore regression hardening;
- final UX/accessibility polish.

The only approved new feature after the stable hardening line is external MIDI/footswitch control. Marker/section enhancements, clip-gain UI, Reference × My Guitar comparison enhancements and a separate large-project performance program are explicitly excluded from this roadmap.
