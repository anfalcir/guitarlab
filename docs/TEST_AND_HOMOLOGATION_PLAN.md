# Test and Homologation Plan

Updated: 2026-09-16

## Active candidate
`0.5.0-rc3` / versionCode `23` / package `studio.guitarlab.app`.

Current signed DIGITAL PASS: **CI #641** / run `35105065689` / exact producer source `b11769f340f7056c37dfb17d95b062909dad87bf`, authoritative through H24a.

Current signed APK SHA-256: `d3698067ed7117d3c3c844b0d94bb117c3448cac3da2897329e4dbfcd71e0f39`.

## Automated coverage — promoted at #641
The RC automation covers project persistence/package round-trip, media integrity, import/codecs, trim/split/drag/take-lineage, Undo/Redo, lifecycle, transport/loop/punch, waveform, mixer/export, route semantics, recording timing/calibration, transient-feedback, accessibility, responsive geometry, Home project-library behavior and exact release provenance.

Promoted counts:
- JVM/unit: **269/269 PASS**, 0 failures/errors/skips;
- Android Lint gate: PASS;
- standard API36 connected regression: **25/25 PASS**;
- isolated 1920×1200 geometry: **1/1 PASS**;
- signed homologation: PASS.

Keep canonical Android reporting as **25/25 standard + 1/1 isolated geometry**.

### H23b retained requirements
- progressing timestamp evidence; stale/backwards evidence fails closed;
- exact route+sample-rate calibration/fine-adjustment scope;
- 44.1/48/88.2/96 kHz timing coverage;
- no double compensation;
- route identifiers sanitized from normal transient UX;
- unstable calibration remains diagnostic-only.

### H24/H24a project-library requirements — DIGITAL PASS
Pure/JVM policy coverage includes:
- case/accent-insensitive search and deterministic normalization;
- template/content/sample-rate filters independently and combined;
- recordings vs any clips vs no clips;
- Auto + 44.1/48/88.2/96 kHz matrix;
- modified/created/name sort in both directions;
- deterministic tie-breaks and default updated-desc behavior;
- clear-search/filter semantics and active-filter metadata;
- randomized library determinism.

Android instrumentation verifies Home search/filter/sort discoverability, semantic labels/state and standard regression participation. H24 contributed two standard API36 cases; #641 proves the complete standard suite at 25/25.

## H24/H24a source materialization
Canonical tail ends at H24a. The gate verifies deterministic gzip+base64 source parts, integrity, patch applicability/already-materialized recognition, final Git blob hashes, idempotency and fail-closed behavior on unexplained drift.

Expected message:
`Source patch chain materialized through H24a with verified final hashes`.

## Historical CI #640 lesson
CI #640 reached a green software gate but Android integration stopped at test compilation because H24 had an invalid explicit `assertDoesNotExist` import. H24a removed only that import. CI #641 supersedes #640 with full exact-source API36/signing PASS.

## Canonical manual CI gate
The canonical full gate remains `.github/workflows/android-ci.yml` with `signed_homologation=true`, manually dispatched by the user only.

A full gate requires on one exact `head_sha`:
1. checkout + diff/materialization sanity;
2. JVM/unit suites;
3. performance evidence;
4. Android Lint;
5. debug/release assembly + unsigned provenance;
6. standard API36 regression;
7. isolated 1920×1200 geometry;
8. signing from the exact tested unsigned artifact;
9. package/version/source/checksum/signer verification;
10. signing-material cleanup.

CI #641 satisfies this gate. Do not rerun it merely for reassurance. A new full gate is required only after a source change or deliberate next candidate.

## Residual physical checks
Use exact CI #641 APK and delegate only target-only evidence:
- H24 Home search/filter/sort visual/touch usability on SM-X230;
- retained H22 route semantics after MK-300 reconnect;
- H23b zero-adjustment repeated-take recording synchronization, especially 44.1 kHz;
- backing isolation, live waveform/meters and selected-input fail-closed behavior;
- one representative edit/save/reopen/export smoke;
- subjective listening where programmatic comparison is not sufficient.

Use `RC3_FINAL_PHYSICAL_HOMOLOGATION.md` as the physical checklist.

## Closure
M7/M8/RC3 close only after:
- exact-source #641 automated PASS — **complete**;
- verified signed identity — **complete**;
- residual target-device PASS — pending;
- no repeatable P0/P1 — pending physical confirmation;
- explicit user approval of exact signed APK SHA `d3698067ed7117d3c3c844b0d94bb117c3448cac3da2897329e4dbfcd71e0f39` — pending.
