# Current State — GuitarLab Studio

Updated: 2026-09-17

## Active line
- Repository/branch: `anfalcir/guitarlab` / `main`.
- Version: `0.5.0-rc3`, versionCode `23`, package `studio.guitarlab.app`.
- Current source line: signed #657 producer plus H34 source-only delta.
- Latest signed DIGITAL PASS: **CI #657** / run `35290128876` / producer `e371bb2a5c8040c668b926b2077c03d1c7c8c7d6`.
- #657 scope: H28 → H29 → H30 → H31 → H32 → H33 → H33a → H33b.
- Signed APK SHA-256: `05d6eaf71fb69ce55b28b8e3214e619a15f3b862dada973740927c2b9ecf2cd6`.
- Signed APK size: `13,835,802` bytes.
- Locked signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.
- CI remains manual-only (`workflow_dispatch`).

## #657 digital evidence
All three canonical jobs passed on the exact producer SHA:
- deterministic materialization through H33b PASS;
- **323/323 JVM/unit tests PASS**, 0 failures/errors/skips;
- Android Lint PASS with **0 errors**;
- debug/release assembly and unsigned provenance PASS;
- **33/33 standard API36 PASS**;
- **1/1 isolated 1920×1200 geometry PASS**;
- exact tested-artifact signing, zipalign, package/version and certificate verification PASS.

Locked signer SHA-256 remains `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

## H28 physical backup result
Target-device testing after #653 confirmed the corrected backup behavior is working correctly. In particular, the false commit-failure/selective-duplicate behavior that triggered H28 is no longer reproduced in the accepted backup workflow.

H28 remains a protected regression contract covering immutable `projectId`, deterministic `revisionId`, package SHA-256 integrity, provider-lag tolerance, idempotent unchanged backup and bounded retention.

## H29-H33 digital closure + H34 source delta
H29-H33/H33a/H33b are digitally closed by CI #657.

H34 is **SOURCE PRE-GATE READY**:
- increases only `LatencyFineAdjustmentPolicy.MAX_ABS_MILLISECONDS` from 120.0 to **500.0**;
- leaves automatic clock alignment and automatic route-latency calibration unchanged;
- keeps adjustment exact-route + exact-sample-rate scoped and default zero;
- adds ±25 ms controls while retaining ±5/±1 ms and `Zerar`;
- adds exact boundary tests for 44.1/48/88.2/96 kHz;
- local pure-Kotlin harness PASS;
- exact H33b→H34 and H28→H34 materialization PASS;
- second materialization idempotent;
- corrupt H34 source-part rejected fail-closed.

The former ±120 ms value was a conservative product guardrail for a “small residual correction”; it is not required by the DSP/placement arithmetic. Placement arithmetic already saturates and has extreme-value regression coverage.

#657 remains the signed authority until H34 receives a canonical exact-source CI pass.

## Remaining RC3 physical blocker
The remaining release-critical target-only item is **recording latency/synchronization acceptance** on the intended real USB route. The recording timing architecture is already implemented and digitally covered; the unresolved boundary is physical driver/hardware behavior.

Final RC3/1.0 promotion still requires no repeatable P0/P1 and explicit approval of the exact signed candidate.

## Approved next-development scope
The authoritative plan is `POST_H28_HARDENING_AND_EXTERNAL_CONTROL_PLAN.md`.

Before stable 1.0.0, the staged H29-H32 source must be digitally and physically closed:
- recording synchronization/session-health hardening;
- interrupted-recording recovery UX;
- USB audio disconnect/reconnect resilience;
- takes-management refinement;
- song/session quality and stress coverage through 10 minutes;
- diagnostics refinement;
- H28 backup/restore regression hardening;
- final UX/accessibility polish.

The only approved new feature after the stable hardening line is external MIDI/footswitch control. Marker/section enhancements, clip-gain UI, Reference × My Guitar comparison enhancements and a separate large-project performance program are explicitly excluded from this roadmap.
