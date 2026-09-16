# Current State — GuitarLab Studio

Updated: 2026-09-16

## Active line
- Repository/branch: `anfalcir/guitarlab` / `main`.
- Version: `0.5.0-rc3`, versionCode `23`, package `studio.guitarlab.app`.
- Last signed DIGITAL PASS: **CI #639** / run `35096711936` / exact producer source `eb9c4a4ca2a6269fbe2f2211a0807b8c703e115c`.
- Signed APK SHA-256: `ffac9da48c17fe2bd28172d357c2f45e906c15b20a216443c1b8b78a9a893696`.
- Unsigned APK SHA-256: `195aa82a581bbcc30890b278cab03bc029eb5d3376fa99130b67e24f6213e21a`.
- Locked signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.
- `.github/workflows/android-ci.yml` remains manual-only (`workflow_dispatch`). The assistant must not dispatch or rerun it.

## Evidence boundary
CI #639 is authoritative through **H23b** only. H24/H24a change product/test source after #639 and are therefore **IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE** until a new user-dispatched full signed workflow succeeds.

A later documentation-only commit never retroactively changes the source SHA that produced an older APK. For the next gate, the exact workflow `head_sha` becomes the new producer identity if all gates pass.

## CI #639 — DIGITAL PASS retained
- unit/JVM suites: **260/260 PASS**, 0 failures, 0 errors, 0 skipped;
- Android Lint/build/unsigned provenance: PASS;
- H23b source materialization with verified final hashes: PASS;
- standard API36 connected regression: **23/23 PASS**;
- isolated 1920×1200 target-tablet geometry: **1/1 PASS**;
- signed homologation: PASS;
- package/version identity: `studio.guitarlab.app`, `0.5.0-rc3`, versionCode `23`;
- APK Signature Scheme v2: PASS;
- signer count 1, RSA 4096, locked certificate match: PASS.

Do not flatten Android regression to 24/24. Canonical reporting is **23/23 standard + 1/1 isolated geometry**.


## CI #640 — FAILED before Android test execution
Run `35103316449` targeted source `665bfbec78d032df21f467f226e353146595f67c`.

Observed result:
- software gate: **PASS** — unit tests, Android Lint, debug/release assembly and unsigned provenance all completed successfully;
- API36 gate: **FAIL** during `:app:compileDebugAndroidTestKotlin`, before instrumented tests executed;
- exact compiler error: unresolved import `androidx.compose.ui.test.assertDoesNotExist` in `HomeProjectLibraryInstrumentedTest.kt`;
- signed homologation: **SKIPPED** because the Android integration prerequisite failed.

This was a test-source compilation defect, not a runtime/functional failure of the Home Project Library.

### H24a corrective alignment
H24a removes the invalid explicit import while preserving the two `SemanticsNodeInteraction.assertDoesNotExist()` calls. The Compose test API in the pinned dependency exposes that assertion through the interaction object and does not provide the imported top-level symbol used by H24.

H24a final corrected instrumented-test blob: `9ed877ddd44c5d271b69f4519e9cf4db68562493`.

H24a validation completed before the next CI:
- applied cleanly over the exact #640 source snapshot;
- reverse patch restores the exact H24 blob `1bafaa45a6372b5728d4a0eb3ba3a90d0eeb7c28`;
- reapply restores the exact H24a blob;
- second materializer run is idempotent;
- corrupted encoded patch fails closed;
- materializer shell syntax PASS.

## H22/H22a — route UX — PHYSICAL PASS retained
Samsung SM-X230 physical review approved semantic physical routing: duplicate internal endpoints are collapsed, built-in routes use friendly labels and MK-300 endpoints are consolidated. Later blocks must not regress this behavior.

## H23/H23a/H23b — recording timing and transient feedback
H23/H23a introduced and aligned the session-clock mapping, route/rate calibration, residual adjustment and transient-feedback policy. H23b hardened stale timestamp rejection, exact route+rate calibration identity, 44.1/48/88.2/96 kHz coverage, overflow-safe placement, analyzer diagnostics and route-token sanitization. H23b is DIGITAL PASS at #639; focused physical recording-timing acceptance is still pending.

## H24/H24a — Home Project Library — IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE
Implementation anchor commit: `96ffe7bd394e2eda68707cf2ad8c8596432cd26a`.

H24 adds:
- real-time project-name search, case- and accent-insensitive;
- combinable template, content and sample-rate filters;
- deterministic sort by modification time, creation time or name in both directions;
- result count `X de Y` when the library is narrowed;
- separate true-empty vs no-results states;
- explicit clear-search and clear-filter actions;
- an immutable in-memory index with normalized names rebuilt only when the repository snapshot changes;
- query/filter/sort preservation across refresh and project operations;
- synchronized in-app Help text;
- pure policy tests plus Android instrumentation source for semantics/control behavior.

H24 does **not** change project schema, managed-media layout or `.guitarlab` package format.

### H24 local/source validation
- pure Kotlin policy compilation: PASS;
- deterministic functional harness: PASS;
- 10,000 randomized library iterations across all sort modes: PASS;
- case/accent normalization, filter combinations, all four fixed sample rates + Auto and deterministic tie-breaks: PASS;
- H24 patch forward/reverse byte round-trip: PASS;
- gzip/base64 integrity: PASS;
- first materialization: PASS;
- second materialization idempotency: PASS;
- corruption probe fails closed: PASS;
- final H24 materialized Git blob hashes: PASS;
- `bash -n scripts/materialize_ci_sources.sh`: PASS;
- `git diff --check`: PASS;
- Home Compose source parser scan: no syntax/parser diagnostics; Android/Compose symbols are intentionally unresolved without SDK/Compose classpath.

CI #640 subsequently confirmed the full software gate PASS but exposed the Android-test compile import defect described above. H24a corrects only that test-source defect; a fresh exact-source API36/signing PASS is still required.

## Next gate
The user should manually run the full signed workflow on the final current `main` after this documentation consolidation:

`Actions → GuitarLab Android CI → main → signed_homologation=true`

Required before H24/H24a promotion: software/unit/Lint/build/provenance PASS, standard API36 PASS, isolated 1920×1200 geometry PASS and signed homologation PASS on the exact same `head_sha`.

After that, physical review should remain residual: H24 Home-library tablet smoke plus the still-pending H23b SM-X230 + MK-300 recording-timing/route validation.
