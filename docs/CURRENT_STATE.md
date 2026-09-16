# Current State — GuitarLab Studio

Updated: 2026-09-16

## Active line
- Repository/branch: `anfalcir/guitarlab` / `main`.
- Version: `0.5.0-rc3`, versionCode `23`, package `studio.guitarlab.app`.
- Last signed DIGITAL PASS: CI #638 / run `35084703365` / exact product source `c310be6779e6591c57399257f380588c27bdf20a`.
- Signed APK SHA-256: `a650afa5edfd2b8c4f8393e65b314ae9fbb59487a87c2d3ea85ef978d7d895dc`.
- Unsigned APK SHA-256: `621e02355d265bdb6c24cb5e324b445b63e739f8e7d6adc233eebea3b31fe0d5`.
- Locked signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.
- Workflow remains manual-only: `.github/workflows/android-ci.yml` uses `workflow_dispatch`; the assistant must not dispatch or rerun it.

## Evidence boundary
CI #638 is authoritative through H23/H23a. It passed software/unit/Lint/build/provenance, standard API36 **23/23**, isolated 1920×1200 geometry **1/1**, and signed homologation on the same exact source SHA.

CI #637 / source `086fa3b080f0994b9031f52cb8ac3f01754d5378` failed only because the new transient-feedback test used the wrong test annotation import. H23a changed only that test import to the app-standard JUnit 4 `org.junit.Test`; production H23 code did not change. CI #638 then passed fully.

## H22/H22a — DIGITAL PASS + focused physical route UX PASS
Physical review of CI #636 confirmed the duplicate-route problem is resolved on the Samsung tablet. Built-in routes are shown semantically (`Microfone do tablet`, `Alto-falante do tablet`), low-level endpoints do not duplicate, and H23 prevents raw route identifiers from leaking into normal transient feedback.

## H23/H23a — recording timing + transient-feedback hardening — DIGITAL PASS at CI #638

### Recording timing — Plan A
- Capture and backing start are mapped with a signed clock offset; capture-before-backing and capture-after-backing are represented.
- Android hardware timestamps are accepted only after repeated observations produce a stable stream-origin estimate.
- A single/stale timestamp is not authoritative.
- Hardware timestamp time is never mixed with command-time fallback; incompatible clock evidence fails closed to zero startup correction.
- Startup/session offset, measured route latency and residual fine adjustment are separate domains and are applied exactly once.
- Punch recording crops from the final compensated take placement rather than duplicating latency math.
- Recording timing state is cleared after stop/error to prevent stale compensation from contaminating the next take.

### Latency analyzer — Plan B
- Calibration uses the actual project/editing/recording sample rate rather than fixed 48 kHz.
- Established AUTO projects can calibrate at 44.1/48/96 kHz; ambiguous mixed-rate editing domains fail closed instead of guessing.
- Round-trip calibration is stored by input route + output route + sample rate and is applied only when accepted by confidence/jitter/drift policy.
- Fine residual adjustment is stored separately on the same route/rate tuple, defaults to zero, is bounded to ±120 ms, and is intended only after automatic alignment/calibration has been assessed.
- Positive fine adjustment advances the take; negative adjustment delays it.

### Transient feedback contract
- Normal Play/Stop, CUT/Trim entry, REC countdown/state, mute/solo, navigation and routine mode changes do not generate Snackbar feedback.
- Snackbars are reserved for errors, meaningful degradation/warnings and asynchronous completion that is not otherwise obvious.
- Repeated warnings use cooldown/de-duplication.
- Normal feedback filters raw Android route tokens (`remote-submix`, `hsp:`, `route2:`, `route3:`, `• back/bottom/0`). Technical identifiers remain available only in diagnostics.
- Progress belongs to its owning UI rather than being duplicated as transient feedback.
- Contract: `docs/TRANSIENT_FEEDBACK_CONTRACT.md`.

## H23 validation evidence
- deterministic H23 source-parts + H23a test-only correction materialize successfully;
- materializer final message: `Source patch chain materialized through H23a with verified final hashes`;
- unit tests: PASS;
- Android Lint: PASS;
- debug + release assembly: PASS;
- standard API36: **23/23 PASS**;
- isolated target-tablet geometry: **1/1 PASS**;
- signed homologation: PASS;
- artifact package/version/certificate/provenance: PASS.

## User evidence / physical status
- H22 route UX is physically approved on target Samsung hardware.
- User supplied `WATG - Enemy-master.wav`, 44.1 kHz; only the left channel is the valid recorded-guitar evidence channel.
- That file demonstrated residual late placement before H23, but it is not an isolated calibration reference and therefore was not used to hard-code a compensation constant.
- H23 timing correction is digitally validated but still requires focused real-hardware A/B to determine whether systematic late placement is eliminated on SM-X230 + MK-300.

## Milestone state
- M2–M6: PASS/CLOSED.
- M7/M8 through H22/H22a: DIGITAL PASS; focused route UX physically PASS.
- H23/H23a: DIGITAL PASS at CI #638; focused physical REC/latency validation pending.

## Next physical gate
Install only the signed CI #638 APK produced from exact source `c310be6779e6591c57399257f380588c27bdf20a`.

With fine adjustment at **0 ms** first:
1. record a synchronized guitar take against a known backing on SM-X230 + MK-300;
2. test the actual project rate, including 44.1 kHz when applicable;
3. verify whether any repeatable systematic late placement remains;
4. verify normal Play/CUT/REC no longer produces Snackbar spam or raw technical route names;
5. retain smoke for route reconnect, live waveform/meters, punch/loop, trim/undo/redo and export.

If a stable residual remains after Plan A, use the route/rate latency analyzer first. Fine adjustment is Plan B and must remain zero unless objective or repeatable physical evidence justifies a non-zero value.
