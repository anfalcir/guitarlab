# Current State — GuitarLab Studio

Updated: 2026-09-20

## Active line
- Repository/branch: `anfalcir/guitarlab` / `main`.
- Version: `0.5.0-rc3`, versionCode `23`, package `studio.guitarlab.app`.
- Current signed source line: CI #659 producer `a6a53e8ba9e75b32565e451870758c7c65ad687f`.
- Latest signed DIGITAL PASS: **CI #659** / run `35512894518` / producer `a6a53e8ba9e75b32565e451870758c7c65ad687f`.
- #659 scope: H28 → H29 → H30 → H31 → H32 → H33 → H33a → H33b → H34 → H35 → H35a.
- Signed APK SHA-256: `e7ddce638a83d92af0ef2a01e46a10152f8c0eb4c535f0ddbc8671d9b94a1295`.
- Signed APK size: `13,835,802` bytes.
- Locked signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.
- CI remains manual-only (`workflow_dispatch`).

## #659 digital evidence
All three canonical jobs passed on the exact producer SHA:
- deterministic materialization through H35a PASS;
- **330/330 JVM/unit tests PASS**, 0 failures/errors/skips;
- Android Lint PASS with **0 errors, 50 warnings and 4 hints**;
- debug/release assembly and unsigned provenance PASS;
- unsigned tested APK SHA-256 `4351458825b7a07c53ff2827e69477414896b497b0a77e3aa7e681c29d97c5da`;
- **33/33 standard API36 PASS**;
- **1/1 isolated 1920×1200 geometry PASS**;
- exact tested-artifact signing, zipalign, package/version and certificate verification PASS;
- signed APK SHA-256 `e7ddce638a83d92af0ef2a01e46a10152f8c0eb4c535f0ddbc8671d9b94a1295`.

Locked signer SHA-256 remains `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

## H28 physical backup result
Target-device testing after #653 confirmed the corrected backup behavior is working correctly. In particular, the false commit-failure/selective-duplicate behavior that triggered H28 is no longer reproduced in the accepted backup workflow.

H28 remains a protected regression contract covering immutable `projectId`, deterministic `revisionId`, package SHA-256 integrity, provider-lag tolerance, idempotent unchanged backup and bounded retention.

## H29-H35a digital closure
H29-H35a are digitally closed by CI #659.

H34/H35/H35a are now part of the exact signed candidate:
- global route/rate manual residual range is ±500 ms and applies only to future recordings;
- persistent take-specific synchronization is delta-based, non-destructive and Undo/Redo-aware;
- silent digital verification uses PCM zero and never stores fake physical round-trip latency;
- physical calibration validates the exact live input/output IDs before emitting a deterministic 32 ms windowed chirp capped at 12% peak;
- the H35a explicit RECORD_AUDIO guard passed Android Lint without suppression.

## Remaining RC3 physical blocker
The remaining release-critical target-only item is **recording latency/synchronization acceptance** on the intended real USB route. The recording timing architecture is already implemented and digitally covered; the unresolved boundary is physical driver/hardware behavior.

Final RC3/1.0 promotion still requires no repeatable P0/P1 and explicit approval of the exact signed candidate.

## Approved next-development scope
The authoritative plan is `POST_H28_HARDENING_AND_EXTERNAL_CONTROL_PLAN.md`.

Before stable 1.0.0, the #659 baseline is digitally closed; only the remaining hardware-only H29/H30/H32/H35 residual must now be physically closed:
- recording synchronization/session-health hardening;
- interrupted-recording recovery UX;
- USB audio disconnect/reconnect resilience;
- takes-management refinement;
- song/session quality and stress coverage through 10 minutes;
- diagnostics refinement;
- H28 backup/restore regression hardening;
- final UX/accessibility polish.

The only approved new feature after the stable hardening line is external MIDI/footswitch control. Marker/section enhancements, clip-gain UI, Reference × My Guitar comparison enhancements and a separate large-project performance program are explicitly excluded from this roadmap.
