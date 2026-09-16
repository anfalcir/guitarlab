# Test and Homologation Plan

Updated: 2026-09-16

## Active candidate
`0.5.0-rc3` / versionCode `23` / package `studio.guitarlab.app`.

Last signed DIGITAL PASS: CI #639 / run `35096711936` / source `eb9c4a4ca2a6269fbe2f2211a0807b8c703e115c`, authoritative through H23b.

H24 Home Project Library is newer and remains **IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE** until one new exact-source full signed workflow succeeds.

## Automated coverage retained
Promoted RC automation retains project persistence/package round-trip, media integrity, import/codecs, trim/split/drag/take-lineage, Undo/Redo, lifecycle, transport/loop/punch, waveform, mixer/export, route semantics, recording timing/calibration, transient-feedback, accessibility, responsive geometry and exact release provenance.

### H23b retained requirements
- progressing timestamp evidence; stale/backwards evidence fails closed;
- exact route+sample-rate calibration/fine-adjustment scope;
- 44.1/48/88.2/96 kHz timing coverage;
- no double compensation;
- route identifiers sanitized from normal transient UX;
- unstable calibration remains diagnostic-only.

### H24 project-library requirements
Pure/JVM policy coverage must include:
- case-insensitive and accent-insensitive search;
- query normalization determinism;
- template/content/sample-rate filters independently and in combination;
- content distinction: recordings vs any audio/clips vs no clips;
- recording detection for existing take lineage metadata;
- Auto + 44.1/48/88.2/96 kHz sample-rate matrix;
- modified/created/name sorting in both directions;
- deterministic tie-break under equal primary keys;
- default updated-desc behavior;
- clear-search/filter semantics without unwanted sort reset;
- active-filter metadata;
- randomized large-library determinism.

Android instrumentation coverage must verify at minimum:
- search and clear affordances are discoverable and semantically labeled;
- filter control exposes active state and can open filter options;
- sort control exposes current selection and updates semantics after selection;
- controls remain part of the standard Home regression surface.

Do not predeclare a future test count. Report the official count only after the workflow completes.

## H24/H24a source materialization
Current canonical tail ends at H24a. Requirements:
- deterministic gzip+base64 source part;
- gzip integrity validation;
- forward patch applicability or exact already-materialized recognition;
- final Git blob hash verification for every H24-modified source/test file;
- idempotent second materializer run;
- fail closed on unexplained source corruption/drift.

Expected message:
`Source patch chain materialized through H24a with verified final hashes`.

### CI #640 lesson / H24a gate
CI #640 reached a green software gate but Android integration stopped at `:app:compileDebugAndroidTestKotlin` because the H24 instrumented test had an invalid explicit import for `assertDoesNotExist`. H24a removes only that import and retains the assertion calls. A fresh full workflow is mandatory; #640 cannot be promoted or partially reused as Android/signing evidence.

## Canonical manual gate
The user manually dispatches `.github/workflows/android-ci.yml` on final current `main` with `signed_homologation=true`.

Mandatory:
1. checkout exact `head_sha` + diff/materialization sanity;
2. all JVM/unit suites including H24 policy tests;
3. performance evidence;
4. Android Lint;
5. debug/release assembly and unsigned provenance;
6. standard API36 connected regression including H24 instrumentation;
7. isolated 1920×1200 tablet geometry;
8. signed homologation from the exact tested unsigned artifact;
9. package/version/source/checksum/signer verification;
10. signing material cleanup.

Any failure blocks promotion. A green run from another SHA does not count.

## Residual physical checks
After exact-source H24 digital PASS, delegate only target-only evidence:
- Home search/filter/sort visual/touch usability on SM-X230, including one combined-filter and one sort smoke;
- retained H22 route semantics after MK-300 reconnect;
- H23b zero-adjustment repeated-take recording synchronization, especially 44.1 kHz;
- backing isolation, live waveform/meters and selected-input fail-closed behavior;
- one representative edit/save/reopen/export smoke;
- subjective listening where programmatic comparison is not sufficient.

Use `RC3_FINAL_PHYSICAL_HOMOLOGATION.md` as the physical checklist.

## Closure
M7/M8/RC3 close only after:
- exact-source automated PASS;
- verified signed identity;
- no repeatable P0/P1;
- residual target-device PASS;
- explicit user approval of the exact signed APK.
