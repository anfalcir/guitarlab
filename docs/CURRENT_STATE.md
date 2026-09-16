# Current State — GuitarLab Studio

Updated: 2026-09-16

## Active line
- Repository/branch: `anfalcir/guitarlab` / `main`.
- Version: `0.5.0-rc3`, versionCode `23`, package `studio.guitarlab.app`.
- Last signed DIGITAL PASS: **CI #639** / run `35096711936` / exact product source `eb9c4a4ca2a6269fbe2f2211a0807b8c703e115c`.
- Signed APK SHA-256: `ffac9da48c17fe2bd28172d357c2f45e906c15b20a216443c1b8b78a9a893696`.
- Unsigned APK SHA-256: `195aa82a581bbcc30890b278cab03bc029eb5d3376fa99130b67e24f6213e21a`.
- Locked signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.
- `.github/workflows/android-ci.yml` remains manual-only (`workflow_dispatch`). The assistant must not dispatch or rerun it.

## Evidence boundary
CI #639 is now the authoritative digital gate through **H23b**. It tested and signed the exact product source `eb9c4a4ca2a6269fbe2f2211a0807b8c703e115c`.

A later documentation-only commit does **not** replace that product source SHA and must never be described as the source that produced the APK.

## CI #639 — DIGITAL PASS
- unit/JVM suites: **260/260 PASS**, 0 failures, 0 errors, 0 skipped;
- Android Lint gate: PASS; non-blocking warnings may remain, but no gating Lint error was reported;
- debug/release assembly + unsigned provenance: PASS;
- H23b source materializer: `Source patch chain materialized through H23b with verified final hashes`;
- standard API36 connected regression: **23/23 PASS**;
- isolated 1920×1200 target-tablet geometry: **1/1 PASS**;
- signed homologation job: PASS;
- package/version identity: `studio.guitarlab.app`, `0.5.0-rc3`, versionCode `23`;
- APK Signature Scheme v2 verification: PASS;
- signer count: 1; RSA 4096;
- signer certificate matches the locked SHA-256 above;
- private signing bundle cleanup step: PASS.

Do not flatten the Android regression result to 24/24. The canonical report is **23/23 standard + 1/1 isolated geometry**.

## H22/H22a — route UX — PHYSICAL PASS retained
Focused physical review on Samsung SM-X230 approved semantic physical routing: duplicate internal endpoints are collapsed, built-in devices show friendly labels, and MK-300 endpoints are consolidated. H23/H23a/H23b must not regress this behavior.

## H23/H23a — DIGITAL PASS at CI #638
H23 introduced signed session-clock mapping, route/rate calibration, residual fine adjustment and centralized transient-feedback policy. H23a changed only the app test annotation import so the app test source set uses JUnit 4.

## H23b — corrective hardening — DIGITAL PASS at CI #639 / PHYSICAL VALIDATION PENDING
H23b closes the remaining audit gaps without introducing a magic global offset.

### Recording timing / Plan A
- signed `captureOrigin - backingOrigin` mapping remains the authoritative session-clock relationship;
- timestamp anchors require genuinely progressing frame and monotonic-time observations; stale samples do not count as independent evidence and backwards progression fails closed;
- hardware timestamp evidence is never mixed with command-clock fallback evidence;
- nanoseconds-to-frames coverage explicitly includes **44.1 / 48 / 88.2 / 96 kHz**;
- placement arithmetic saturates instead of wrapping at pathological `Long` bounds;
- route latency and residual fine adjustment remain separate from session-clock alignment;
- punch crops from the final compensated placement so timing compensation is not applied twice;
- timing state is per take and stop/error paths clear active recording timing state.

### Calibration / Plan B
- calibration/fine adjustment is keyed by exact `input route + output route + sample rate` using the unambiguous v2 persistence key;
- the old 32-bit hashed key is not auto-applied; after upgrading, recalibrate before relying on route compensation;
- unstable/rejected calibration is diagnostic-only and cannot be auto-applied;
- analyzer shows selected input/output, session rate, status, median latency, attempts, jitter, drift and confidence;
- residual adjustment defaults to zero, is bounded to ±120 ms and supports both signs.

### Transient feedback
- normal Play/Stop/Pause, CUT/Trim mode, REC state/countdown, mute/solo/arm, navigation and visible state changes do not own a Snackbar;
- Snackbar is reserved for errors, meaningful degradation/warnings and non-obvious asynchronous completion;
- centralized notices and raw error strings pass through the same technical-route sanitizer;
- technical Android route identifiers stay out of normal transient UX and remain available to diagnostics.

## Local H23b pre-gate validation retained
Before CI #639, H23b also passed pure Kotlin policy compilation, explicit 44.1/48/88.2/96 kHz checks, 100,000 randomized timing-placement property cases, patch/gzip/base64 round-trip, materializer idempotency, final blob-hash verification and `git diff --check`.

The official Android build/Lint/API36/signing authority is now CI #639, not those local checks.

## Next gate — focused physical validation
Install only the CI #639 signed APK identified above on Samsung SM-X230 + M-VAVE MK-300.

Start recording-timing validation with residual fine adjustment at **0.0 ms**. Verify at least three independent takes, include a 44.1 kHz project, exercise non-zero playhead and punch/loop, confirm no timing state leaks between takes, and retain a short H22 route/feedback/backing-leakage regression smoke.

If a repeatable route-specific residual remains, use accepted route/rate calibration before any manual fine adjustment. Final RC3 approval remains pending explicit physical approval of this exact signed APK.
