# Test and Homologation Plan

Updated: 2026-09-16

## Active candidate
`0.5.0-rc3` / versionCode `23` / package `studio.guitarlab.app`.

Current signed DIGITAL PASS: **CI #642** / run `35121955150` / exact producer `7bfd876b6a5b0701ab0cf5203de36c31cd117632`, authoritative through H25.

Current signed APK SHA-256: `916758f694735febb8cccfe58f46f290562aaba1b5483ccc727e2be0cefba5aa`.

## Automated coverage promoted at #642
- source materialization through H25/final hashes: PASS;
- JVM/unit: **269/269 PASS**, 0 failures/errors/skips;
- performance evidence: PASS;
- Android Lint: PASS;
- debug/release build + unsigned provenance: PASS;
- standard API36: **28/28 PASS**;
- isolated 1920×1200 geometry: **1/1 PASS**;
- signed homologation, checksum/package/version/signer verification and signing cleanup: PASS.

Keep canonical Android reporting as **28/28 standard + 1/1 isolated geometry**.

## H25 promoted requirements
Digital coverage establishes:
- H25 materialization is exact, idempotent and fail-closed;
- production/test sources compile under the pinned Android/Compose stack;
- delete Cancel does not invoke destructive callback;
- delete Confirm invokes it exactly once;
- calibration details are absent from the main Settings surface;
- `Calibração` opens the dedicated modal and closes cleanly;
- existing standard API36 regression remains green after the H25 UI interaction-shape and diagnostic organization changes.

## Canonical manual CI gate
The canonical full gate remains `.github/workflows/android-ci.yml` with `signed_homologation=true`, manually dispatched by the user only. CI #642 satisfies the gate for H25. Do not rerun it merely for reassurance; a new full gate is required only after another source change or deliberate candidate.

## Residual physical checks
Use only the exact CI #642 APK:
- rounded-square pointer/hover/press feedback visually conforms to the button chassis;
- main Options page is cleaner and calibration modal is readable/scrollable with expected controls;
- diagnostics are clear and non-duplicated;
- project delete requires confirmation; Cancel is safe; Confirm removes the selected project;
- retained H24 Home search/filter/sort smoke;
- retained H22 semantic routes after MK-300 reconnect;
- retained H23b zero-adjustment repeated-take synchronization, especially 44.1 kHz;
- backing isolation, live waveform/meters and selected-input fail-closed behavior;
- one representative edit/save/reopen/export smoke and subjective listening where programmatic comparison is insufficient.

Use `RC3_FINAL_PHYSICAL_HOMOLOGATION.md` as the physical checklist.

## Closure
M7/M8/RC3 close only after exact-source #642 automated PASS and signed identity — **complete** — plus residual target-device PASS, no repeatable P0/P1 and explicit approval of signed APK SHA `916758f694735febb8cccfe58f46f290562aaba1b5483ccc727e2be0cefba5aa`.
