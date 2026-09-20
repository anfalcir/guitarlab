# Test and Homologation Plan

Updated: 2026-09-20

## Current evidence boundary
The current signed DIGITAL PASS is **CI #659** / run `35512894518` / producer `a6a53e8ba9e75b32565e451870758c7c65ad687f`, through H35a.

Signed APK SHA-256: `e7ddce638a83d92af0ef2a01e46a10152f8c0eb4c535f0ddbc8671d9b94a1295`.

Audited digital evidence:
- 330/330 JVM/unit PASS, 0 failures/errors/skips;
- Android Lint PASS, 0 errors / 50 warnings / 4 hints;
- debug/release assembly and unsigned provenance PASS;
- unsigned tested APK SHA-256 `4351458825b7a07c53ff2827e69477414896b497b0a77e3aa7e681c29d97c5da`;
- 33/33 standard API36 PASS;
- 1/1 isolated target-tablet geometry PASS;
- exact tested-artifact signed homologation/provenance/certificate verification PASS;
- signed APK SHA-256 `e7ddce638a83d92af0ef2a01e46a10152f8c0eb4c535f0ddbc8671d9b94a1295`.

Target-device backup testing after #653 confirmed the H28 corrective is functioning correctly. CI #659 is the latest signed DIGITAL PASS through H35a.

## H29-H35a digital closure
CI #659 digitally closes H29-H35a on exact source. The H34/H35 take-synchronization, silent-verification and quieter calibration code is therefore covered by the canonical unit/Lint/build/API36/signing pipeline.

## H36 Settings UX pre-gate coverage
H36 must preserve all #659 functional behavior while proving the presentation change:
- existing calibration-modal instrumentation still opens/closes the dedicated panel and sees silent/physical calibration actions;
- calibration instrumentation now verifies both extreme manual controls (−25 ms and +25 ms), inner ±1 ms controls and `Zerar ajuste` are visible without a horizontal adjustment carousel;
- new Settings hierarchy instrumentation verifies the centered Settings root and compact Audio/External Control actions remain discoverable through the vertical Settings flow;
- existing External Control settings instrumentation continues to exercise opt-in, HID and each Learn mapping tag;
- API36/isolated geometry must catch clipping/overflow regressions on the canonical Android gate.

No extra physical audio test is required solely because of H36; it changes presentation, not routing/DSP/calibration semantics. Physical H35 behavior remains the existing target-device residual.

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

Everything objectively established by #659 does not need to be repeated manually unless a later source change invalidates that evidence.

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
