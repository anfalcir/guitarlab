# Current State — GuitarLab Studio

Updated: 2026-09-20

## Active line
- Repository/branch: `anfalcir/guitarlab` / `main`.
- Version: `0.5.0-rc3`, versionCode `23`, package `studio.guitarlab.app`.
- Current source line: signed #657 producer plus H34/H35 source-only deltas.
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

## H29-H33 digital closure + H34/H35 source delta
H29-H33/H33a/H33b are digitally closed by CI #657.

H34/H35 are **SOURCE PRE-GATE READY**:
- H34 raises the global route/rate-scoped manual residual range from ±120 ms to ±500 ms and adds practical ±25/±5/±1 ms steps;
- the global value remains default-zero, exact input + output + sample-rate scoped and is applied only to new recordings;
- H35 adds persistent `RecordingTake.fineAdjustmentFrames` for an explicit take-specific synchronization edit;
- changing a take adjustment moves the entire `takeId` lineage by only the delta from the previously stored value, preserving source offsets, lengths, media bytes and relative split spacing;
- returning a take adjustment to 0 reverses the lineage movement exactly; crossing timeline frame 0 fails closed instead of silently trimming source audio;
- the take edit uses the normal persisted project history, so save/reopen and Undo/Redo remain coherent;
- `Verificação digital silenciosa` uses PCM zero to validate exact effective routes and stable AudioRecord/AudioTrack clock anchors without pretending to measure physical round-trip latency;
- physical calibration activates/validates routes in silence first, requires the exact current Android device IDs, then emits a deterministic 32 ms windowed chirp with adaptive peak gain 0.03 → 0.06 → maximum 0.12;
- the previous harsh pseudo-random ±0.62 stimulus is removed;
- actual-domain Kotlin harness PASS;
- exact H34→H35 and full H28→H35 materialization PASS;
- second materialization PASS/idempotent in both paths;
- corrupt H35 source-part rejected fail-closed;
- all 12 H35 terminal Git blob hashes match the reviewed workspace and full materialization output;
- diff whitespace sanity PASS.

There are no retained user takes requiring a legacy per-take synchronization migration, so H35 intentionally avoids speculative legacy reconstruction.

#657 remains the signed authority until H34/H35 receive the canonical exact-source Android CI gate.

## Remaining RC3 physical blocker
The remaining release-critical target-only item is **recording latency/synchronization acceptance** on the intended real USB route. The recording timing architecture is already implemented and digitally covered; the unresolved boundary is physical driver/hardware behavior.

Final RC3/1.0 promotion still requires no repeatable P0/P1 and explicit approval of the exact signed candidate.

## Approved next-development scope
The authoritative plan is `POST_H28_HARDENING_AND_EXTERNAL_CONTROL_PLAN.md`.

Before stable 1.0.0, the #657 baseline is digitally closed; H34/H35 must pass their exact-source digital gate, and the remaining hardware-only H29/H30/H32 residual must then be physically closed:
- recording synchronization/session-health hardening;
- interrupted-recording recovery UX;
- USB audio disconnect/reconnect resilience;
- takes-management refinement;
- song/session quality and stress coverage through 10 minutes;
- diagnostics refinement;
- H28 backup/restore regression hardening;
- final UX/accessibility polish.

The only approved new feature after the stable hardening line is external MIDI/footswitch control. Marker/section enhancements, clip-gain UI, Reference × My Guitar comparison enhancements and a separate large-project performance program are explicitly excluded from this roadmap.
