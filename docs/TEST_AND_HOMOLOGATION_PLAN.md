# Test and Homologation Plan

Updated: 2026-09-17

## Current evidence boundary
The current signed DIGITAL PASS is **CI #653** / run `35207902169` / producer `d09fc003e2ae2d699238eb39ba699f75a746fea4`, through H28.

Signed APK SHA-256: `1a36efcbe24d5995dd3609889237ca112670e52554e187d1b93ed5a8649263c0`.

Audited digital evidence:
- 294/294 JVM/unit PASS, 0 failures/errors/skips;
- performance evidence PASS;
- Android Lint PASS, 0 errors;
- debug/release assembly and unsigned provenance PASS;
- 32/32 standard API36 PASS;
- 1/1 isolated target-tablet geometry PASS;
- exact tested-artifact signed homologation/provenance/certificate verification PASS.

Target-device backup testing after #653 confirmed the H28 corrective is functioning correctly. The release-critical physical residual is now recording latency/synchronization on the intended USB route.

## Protected H28 regression contract
Automated or programmatic coverage must continue to prove:
- rename preserves `projectId`;
- duplicate project receives a new `projectId`;
- revision ID is deterministic for unchanged canonical state;
- revision ID changes when canonical state changes, even under equal timestamp;
- v2 dedup never collapses distinct same-timestamp states;
- v1 backup metadata remains readable;
- targeted revision discovery prevents duplicate commit under stale provider listing;
- exact remote write verification does not depend on immediate parent-list visibility;
- retention remains bounded per project and fail-safe on partial/total failure.

## Remaining RC3 physical residual
Keep manual work limited to what software/emulator cannot prove:
- intended USB input/output is the effective route during REC;
- hardware loopback remains off and backing is not printed into the guitar take;
- live waveform/meters remain temporally coherent;
- no silent microphone fallback occurs;
- no repeatable systematic late/early placement remains;
- representative transport/edit/save/reopen/export smoke remains healthy.

Everything objectively established by #653 does not need to be repeated manually unless a later source change invalidates that evidence.

## Post-H28 forward quality plan
The authoritative H29-H33 implementation/test plan is `POST_H28_HARDENING_AND_EXTERNAL_CONTROL_PLAN.md`.

Key additions:
- H29 recording session-health evidence and latency closure;
- H30 interrupted-recording recovery + USB resilience;
- H31 takes + diagnostic report refinement;
- H32 quality/stress target for songs up to 10 minutes + backup regression + UX/accessibility;
- H33 external MIDI/HID footswitch control after stable 1.0.

For the 10-minute quality line, stress must model realistic song projects rather than create a separate artificial mega-project product requirement.

## Final rule
Any promoted release requires exact-source digital PASS, only the genuinely hardware-dependent residual applicable to that source, no repeatable P0/P1 and explicit approval of the exact signed APK SHA-256.
