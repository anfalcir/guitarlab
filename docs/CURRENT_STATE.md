# Current State — GuitarLab Studio

Updated: 2026-09-17

## Active line
- Repository/branch: `anfalcir/guitarlab` / `main`.
- Version: `0.5.0-rc3`, versionCode `23`, package `studio.guitarlab.app`.
- Source checkpoint immediately before this documentation update: `1df91e16ad0a928b0d5ab93bfd975b49b6d2da62`.
- Latest signed DIGITAL PASS: **CI #653** / run `35207902169` / producer `d09fc003e2ae2d699238eb39ba699f75a746fea4`.
- #653 scope: H28 through the canonical source materialization chain.
- Signed APK SHA-256: `1a36efcbe24d5995dd3609889237ca112670e52554e187d1b93ed5a8649263c0`.
- Signed APK size: `13,737,498` bytes.
- Locked signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.
- CI remains manual-only (`workflow_dispatch`).

## #653 digital evidence
All three canonical jobs passed on the exact producer SHA:
- Unit tests + Android Lint + debug/release APK build/provenance: PASS;
- API 36 emulator regression, including isolated target geometry path: PASS;
- exact tested release candidate signing/homologation verification: PASS.

The materializer completed through H28 with verified final hashes.

## H28 physical backup result
Target-device testing after #653 confirmed the corrected backup behavior is working correctly. In particular, the false commit-failure/selective-duplicate behavior that triggered H28 is no longer reproduced in the accepted backup workflow.

H28 remains a protected regression contract covering immutable `projectId`, deterministic `revisionId`, package SHA-256 integrity, provider-lag tolerance, idempotent unchanged backup and bounded retention.

## H29-H33 source pre-gate checkpoint
Source commit `1df91e16ad0a928b0d5ab93bfd975b49b6d2da62` stages the complete H29-H33 source/materialization chain without running hosted CI.

Implemented source scope:
- **H29:** bounded Recording Session Health, mixed-clock rejection and expanded timing/route diagnostics without changing the H23b compensation formula;
- **H30:** transactional interrupted-recording recovery plus semantic USB route-loss/reconnect state handling, no silent microphone fallback and no automatic REC resume;
- **H31:** take metadata/activation/audition/delete policy with split-lineage protection and sanitized diagnostics;
- **H32:** 1/3/5/10-minute song-quality timing/waveform gates across 44.1/48/88.2/96 kHz;
- **H33:** disabled-by-default MIDI/HID external control mapped onto the same guarded Studio commands, with stable descriptors, Learn mode, held/debounce handling and foreground-only input.

Local/source evidence before publication:
- H28 materializer preserved byte-for-byte as `materialize_ci_sources_through_h28.sh`;
- clean H28→H33 materialization PASS with exact final Git blob hashes;
- second materialization PASS/idempotent;
- intentionally corrupted H33 source-part rejected fail-closed before accepting the materialized state;
- pure Kotlin policy/harness coverage PASS for session health, route state, recovery convergence, take invariants, diagnostic sanitization and MIDI/HID normalization;
- H32 worst local stress PASS at 10 minutes / 96 kHz / 57,600,000 frames / 60,000 irregular waveform updates while remaining bounded to at most 512 stored points.

Evidence boundary: this is **SOURCE PRE-GATE READY**, not DIGITAL PASS. Android Lint/build, API36 instrumentation/1920×1200 geometry, unsigned provenance and signed homologation for this exact source remain pending the user's single manual workflow dispatch.

H33 was source-staged early so the same digital gate can catch integration regressions. Its real-controller physical acceptance and 1.1 promotion remain separate from the H29-H32 stable-1.0 completion contract.

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
