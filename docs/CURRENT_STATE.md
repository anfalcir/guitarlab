# Current State — GuitarLab Studio

Updated: 2026-09-16

## Active line
- Repository/branch: `anfalcir/guitarlab` / `main`.
- Version: `0.5.0-rc3`, versionCode `23`, package `studio.guitarlab.app`.
- Last signed DIGITAL PASS: CI #638 / run `35084703365` / exact product source `c310be6779e6591c57399257f380588c27bdf20a`.
- Signed APK SHA-256: `a650afa5edfd2b8c4f8393e65b314ae9fbb59487a87c2d3ea85ef978d7d895dc`.
- Unsigned APK SHA-256: `621e02355d265bdb6c24cb5e324b445b63e739f8e7d6adc233eebea3b31fe0d5`.
- Locked signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.
- `.github/workflows/android-ci.yml` remains manual-only (`workflow_dispatch`). The assistant must not dispatch or rerun it.

## Evidence boundary
CI #638 is authoritative through H23/H23a only. It passed software/unit/Lint/build/provenance, standard API36 **23/23**, isolated 1920×1200 geometry **1/1**, and signed homologation on the same exact source SHA.

H23b changes product/test source after #638 and is therefore **PRE-GATE** until the user manually runs the full signed workflow on the H23b commit. A documentation-only commit never replaces the exact source SHA that produced an APK.

## H22/H22a — route UX
Focused physical review on Samsung SM-X230 approved semantic physical routing: duplicate internal endpoints are collapsed, built-in devices show friendly labels, and MK-300 endpoints are consolidated. Do not regress this behavior.

## H23/H23a — DIGITAL PASS at CI #638
H23 introduced the signed session-clock mapping, route/rate calibration, residual fine adjustment and centralized transient-feedback policy. H23a changed only the app test annotation import so the app test source set uses JUnit 4.

## H23b — corrective hardening — SOURCE-VALIDATED / PRE-GATE
H23b closes edge cases found by auditing the exact materialized source artifact from CI #638 rather than inventing a new latency model.

### Recording timing / Plan A
- signed `captureOrigin - backingOrigin` mapping remains the authoritative session-clock relationship;
- timestamp anchors now require genuinely progressing frame **and** monotonic-time observations; repeated/stale samples do not count as independent evidence and backwards progression fails closed;
- hardware timestamp evidence is never mixed with command-clock fallback evidence;
- nanoseconds-to-frames coverage explicitly includes **44.1 / 48 / 88.2 / 96 kHz**;
- placement arithmetic saturates instead of wrapping at pathological `Long` bounds;
- route latency and residual fine adjustment preserve the existing sign convention and remain separate from session-clock alignment;
- punch continues to crop from the final compensated placement, so timing compensation is not applied twice;
- timing policy remains stateless between takes; ViewModel stop/error paths clear active timing state.

### Calibration / Plan B
- calibration/fine adjustment is keyed by exact `input route + output route + sample rate` using an unambiguous v2 persistence key;
- the prior 32-bit hashed key is **not** auto-applied by H23b because it cannot prove the originating route tuple in the event of a collision; after upgrading, rerun calibration before relying on route compensation;
- unstable/rejected calibration remains diagnostic-only and cannot be auto-applied;
- analyzer UI shows selected input/output, session rate, status, median latency, attempts, jitter, drift and confidence;
- residual adjustment defaults to zero, remains bounded to ±120 ms and supports both signs.

### Transient feedback
- normal Play/Stop/Pause, CUT/Trim mode, REC state/countdown, mute/solo/arm, navigation and visible state changes do not own a Snackbar;
- Snackbar is reserved for errors, meaningful degradation/warnings and non-obvious asynchronous completion;
- both centralized notices **and raw error strings** are passed through the same technical-route sanitizer before normal Studio display;
- raw `deviceId`, `productName=`, `address=`, endpoint indices, `remote-submix`, `hsp:`, `route2:`, `route3:`, `back/bottom/0` stay out of normal transient UX;
- technical identifiers remain available to diagnostics.

## Local H23b validation completed
- exact source basis: materialized CI #638 source artifact (`c310be...`);
- pure Kotlin policy compilation: PASS;
- sample-rate policy compilation with serialization annotation stub: PASS;
- 100,000 randomized timing-placement property checks: PASS;
- explicit 44.1/48/88.2/96 kHz timing checks: PASS;
- transient-feedback policy compile/smoke: PASS;
- Android/Compose changed-source parser scan: no syntax/parser diagnostics; Android symbols intentionally unresolved without SDK/Compose classpath;
- H23b source-part gzip integrity + base64 split round-trip: PASS;
- patch forward dry-run/apply: PASS;
- reverse dry-run/reverse byte round-trip to exact #638 materialized source: PASS;
- materializer first H23b application: PASS;
- second materializer execution idempotent: PASS;
- final materialized source Git blob hashes: PASS;
- `git diff --check`: PASS.

No local Android Gradle build is claimed for H23b. The official build/Lint/API36/signing evidence must come from the next user-dispatched workflow.

## Next gate
Do **not** use the #638 APK to physically approve H23b. After the H23b repository commit is published, the user must manually run:

`Actions → GuitarLab Android CI → main → signed_homologation=true`

Only after that exact H23b source passes all three gates should its signed APK be used for focused SM-X230 + MK-300 recording-latency validation, beginning with residual fine adjustment at `0.0 ms`.
