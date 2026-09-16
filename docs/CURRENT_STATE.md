# Current State — GuitarLab Studio

Updated: 2026-09-16

## Active line
- Repository/branch: `anfalcir/guitarlab` / `main`.
- Version: `0.5.0-rc3`, versionCode `23`, package `studio.guitarlab.app`.
- Last signed DIGITAL PASS: CI #636 / run `35040569179` / exact source `b0a39a765f7cfbb0e9320ee847809300bc1e3d01`.
- Signed APK SHA-256: `b195d8fc4d90fa0f8fa8c826525d08328f65090859386a90eaa37e4bcdf4087e`.
- Unsigned APK SHA-256: `de996298a451cd559320cf71498121a652f9e3ca8054dba8bf2d95a281d08c47`.
- Locked signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.
- Workflow remains manual-only: `.github/workflows/android-ci.yml` uses `workflow_dispatch`; the assistant must not dispatch or rerun it.

## Evidence boundary
CI #636 is the authoritative signed baseline through H22/H22a. Physical review of that exact APK confirmed that input/output presentation now behaves correctly on the Samsung tablet: built-in routes no longer duplicate and the UI uses semantic labels such as `Microfone do tablet` and `Alto-falante do tablet`.

The same physical review exposed two residual product issues:
1. normal playback feedback could still surface a low-level Android route name;
2. recorded guitar remained repeatably a few milliseconds late against the backing.

User evidence `WATG - Enemy-master.wav` is a 44.1 kHz recording; the user identified the left channel as the valid recorded-guitar channel. The WAV is evidence of residual timing error, not a trustworthy source for a hard-coded compensation constant because it does not contain an isolated timing reference.

## H22/H22a — DIGITAL PASS + focused physical route UX PASS
- Canonicalization applies to recording inputs and playback outputs.
- Samsung/OEM internal BUS endpoints are folded into semantic physical microphone/speaker families.
- `remote-submix`, telephony/system-only endpoints and low-level address suffixes are not user-facing choices.
- USB endpoints are grouped by physical interface identity.
- Physical review confirmed the duplicate-route defect is resolved on the target tablet.

## H23 — recording timing + transient-feedback hardening — IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE

### Recording timing — Plan A
- Capture and backing start are mapped using a signed clock offset; both capture-before-backing and capture-after-backing cases are represented.
- Hardware timestamps are accepted only after repeated observations produce a stable stream-origin estimate.
- A single/stale Android `AudioTimestamp` is never treated as authoritative.
- Hardware-timestamp time is never mixed with command-time fallback. If capture and playback do not share the same clock-evidence class, startup offset fails closed to zero.
- Route latency, startup clock offset and residual user adjustment are separate domains and are applied exactly once.
- Punch recording derives the kept window from the final compensated take placement rather than duplicating latency math.
- Recording timing state is cleared after stop/error so a later take cannot inherit stale compensation.

### Latency analyzer — Plan B
- Calibration now uses the actual project/editing/recording sample rate instead of a fixed 48 kHz assumption.
- Auto projects with an established 44.1/48/96 kHz editing rate calibrate at that exact rate.
- Ambiguous mixed editing rates fail closed rather than guessing.
- Round-trip calibration remains route + sample-rate specific and is used only when accepted by confidence/jitter/drift policy.
- Fine residual adjustment is stored separately for the same input + output + sample-rate tuple, bounded to ±120 ms and defaults to zero.
- Positive fine adjustment advances the take; negative adjustment delays it.

### Transient feedback contract
- Normal Play/Stop, opening CUT, countdown/REC state, mute/solo, navigation and routine mode changes do not generate Snackbar feedback.
- Snackbars are reserved for errors, meaningful degradation/warnings and asynchronous completion that is not otherwise obvious.
- Repeated warnings use cooldown/de-duplication.
- User-facing transient feedback is filtered so low-level route tokens such as `remote-submix`, `hsp:`, `route2:`, `route3:` and `• back/bottom/0` do not leak outside diagnostics.
- Import/REC/export progress remains in the owning UI rather than being duplicated as transient feedback.
- See `docs/TRANSIENT_FEEDBACK_CONTRACT.md`.

## Local H23 validation
- deterministic H23 patch generated against the exact materialized CI #636 source;
- `git diff --check`: PASS;
- source-part gzip integrity: PASS;
- materializer shell syntax: PASS;
- first H23 materialization: PASS;
- second/idempotent H23 materialization: PASS;
- all 19 H23 materialized source hashes match the audited contract exactly;
- reverse patch round-trip returns all 19 source files byte-for-byte to the CI #636 baseline;
- pure Kotlin timing policies compile locally;
- 100,000 randomized timing-placement property checks: PASS;
- transient-feedback policy compile/smoke: PASS;
- model + recording/practice policies compile locally with only the serialization annotation stubbed;
- sample-rate/punch smoke: PASS;
- changed Android/Compose sources show no Kotlin syntax diagnostics in parser-oriented compilation; Android/Compose dependency resolution is intentionally left to the official CI gate.

## Milestone state
- M2–M6: PASS/CLOSED.
- M7/M8 through H22/H22a: DIGITAL PASS at CI #636; H22 focused physical route UX is PASS.
- H23: PRE-GATE; signed digital homologation and focused physical latency validation pending.

## Next gate
Run the full workflow manually on the exact H23 source after the H23 repository commit is published. DIGITAL PASS requires software/unit/Lint/build/provenance, API36 connected regression, isolated target-tablet geometry and signed homologation all green on the same SHA. After that, perform a focused physical REC A/B on SM-X230 + MK-300 at the actual project rate (including 44.1 kHz where applicable) and verify no repeatable systematic late placement remains before using any non-zero fine adjustment.
