# Test and Homologation Plan

Updated: 2026-09-20

## Current evidence boundary
The current signed DIGITAL PASS is **CI #657** / run `35290128876` / producer `e371bb2a5c8040c668b926b2077c03d1c7c8c7d6`, through H33b.

Signed APK SHA-256: `05d6eaf71fb69ce55b28b8e3214e619a15f3b862dada973740927c2b9ecf2cd6`.

Audited digital evidence:
- 323/323 JVM/unit PASS, 0 failures/errors/skips;
- performance evidence PASS;
- Android Lint PASS, 0 errors;
- debug/release assembly and unsigned provenance PASS;
- 33/33 standard API36 PASS;
- 1/1 isolated target-tablet geometry PASS;
- exact tested-artifact signed homologation/provenance/certificate verification PASS.

Target-device backup testing after #653 confirmed the H28 corrective is functioning correctly. CI #657 remains the latest signed DIGITAL PASS through H33b; H34/H35 are source PRE-GATE.

## H29-H33 digital closure and H34/H35 pre-gate evidence
CI #657 digitally closes H29-H33/H33a/H33b on exact source.

H34/H35 local/source evidence:
- ±500 ms global future-recording clamp remains exact at 44.1/48/88.2/96 kHz;
- per-take synchronization shifts every clip in one take lineage by the exact stored-value delta with no drift;
- sourceStartFrame/length/media bytes remain unchanged by take synchronization;
- returning take adjustment to zero restores the exact inverse movement;
- advancing across timeline zero fails closed atomically;
- the take-specific value round-trips through project serialization;
- the calibration chirp is deterministic/windowed and capped at 0.12 peak for 44.1/48/88.2/96 kHz;
- silent digital verification is explicitly non-calibrating;
- physical calibration requires exact effective live routes before any non-zero stimulus;
- exact H34→H35 and full H28→H35 materialization PASS;
- second materialization PASS/idempotent for both paths;
- corrupt H35 archive rejected fail-closed;
- all 12 terminal H35 Git blobs match the reviewed workspace.

H34/H35 still require the normal exact-source Android unit/Lint/build/API36/signing gate before becoming DIGITAL PASS.

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

## Remaining target-device residual
For H35, physical validation should additionally confirm that:
- changing the global Settings value does not move an already recorded take;
- changing the take-specific value in the take editor moves that take/splits coherently and survives save/reopen;
- silent digital verification produces no intentional non-zero test signal;
- physical calibration no longer emits the previous harsh burst and aborts silently if the selected output/input are not the actual routed devices.

Keep manual work limited to what software/emulator cannot prove for the exact future signed candidate:
- intended USB input/output is the effective route during REC;
- hardware loopback remains off and backing is not printed into the guitar take;
- live waveform/meters remain temporally coherent;
- no silent microphone fallback occurs;
- no repeatable systematic late/early placement remains;
- USB hot-unplug/reconnect preserves valid capture and does not auto-resume REC;
- one continuous 10-minute recording/playback quality smoke shows no growing offset/dropout/wrong speed and survives save/reopen/export;
- representative transport/edit/save/reopen/export smoke remains healthy.

H33 real-controller connect/map/reconnect/tactile double-trigger acceptance remains the separate 1.1 physical residual and is not a stable-1.0 completion requirement.

Everything objectively established by #657 does not need to be repeated manually unless a later source change invalidates that evidence.

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
