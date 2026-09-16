# GuitarLab Studio 0.5.0-rc3

Updated: 2026-09-16

## Last signed digital homologation — CI #636
Run `35040569179`, exact source `b0a39a765f7cfbb0e9320ee847809300bc1e3d01`, is the signed DIGITAL PASS through H22/H22a.

Identity:
- package `studio.guitarlab.app`;
- version `0.5.0-rc3` / versionCode `23`;
- unsigned APK SHA-256 `de996298a451cd559320cf71498121a652f9e3ca8054dba8bf2d95a281d08c47`;
- signed APK SHA-256 `b195d8fc4d90fa0f8fa8c826525d08328f65090859386a90eaa37e4bcdf4087e`;
- certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

CI evidence:
- software/unit/audio/DSP/persistence/migration/performance/Lint/build/provenance: PASS;
- API36 standard connected regression: 23/23 PASS;
- isolated 1920×1200 geometry: 1/1 PASS;
- signed homologation: PASS.

## H22/H22a — route UX
- duplicate Samsung internal microphone/speaker endpoints are collapsed into semantic physical choices;
- system-only routes such as `remote-submix` are excluded;
- MK-300 USB endpoints are grouped by physical interface identity;
- focused physical review of the #636 candidate confirmed the duplicate-route issue is resolved and the built-in routes are presented with friendly labels.

## H23 — recording timing + feedback hardening — PRE-GATE
H23 follows the #636 physical review that still found a small repeatable late placement in REC and an obsolete technical route name in playback feedback.

Changes:
- repeated monotonic audio timestamps produce stable capture/playback stream-origin anchors;
- signed capture-vs-backing startup offset handles both early and late capture start;
- mixed hardware/command clock bases fail closed rather than creating a synthetic offset;
- measured route latency, startup offset and residual fine adjustment are separate and applied once;
- calibration uses the real project/editing sample rate, including 44.1 kHz sessions;
- route/rate-specific fine adjustment is available as Plan B, bounded to ±120 ms and zero by default;
- punch crop is derived from the final compensated take placement;
- normal Play/Stop/CUT/REC-state changes no longer create Snackbar spam;
- technical Android route identifiers are filtered from normal transient feedback;
- feedback behavior is now governed by `docs/TRANSIENT_FEEDBACK_CONTRACT.md`;
- timing behavior is governed by `docs/RECORDING_LATENCY_CONTRACT.md`.

## Release decision
The #636 APK remains the last signed authority until H23 receives a new full signed CI pass. After that, the remaining critical physical acceptance is repeatable REC alignment on SM-X230 + MK-300 at the real project sample rate, with fine adjustment at zero first. RC3 is not final while a repeatable systematic recording offset remains.
