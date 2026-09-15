# Android CI / release pipeline

Updated: 2026-09-15

## Contract
`.github/workflows/android-ci.yml` is manual-only (`workflow_dispatch`). Commits do not automatically consume hosted CI, and the assistant must not dispatch or rerun the workflow.

The pipeline has three authority layers:
1. software gate — materialization, JVM/unit/audio/DSP/persistence/migration, performance, Lint, debug/release build and unsigned provenance;
2. API36 gate — standard connected instrumentation plus isolated 1920×1200 / 240dpi geometry;
3. signed homologation — signs the exact tested unsigned artifact only after both mandatory gates pass.

## Source materialization
`.source-parts/` plus `scripts/materialize_ci_sources.sh` are part of the build contract. Unexpected source drift fails closed.

Canonical tail after H11b:
`H12 engine → H12 UI → H13 → H14 → H14a → H15 → H16 → H17 → H18 → H19 → H18a`.

Current tail files:
- `.source-parts/H18AdaptivePracticeBar.patch.gz`
- `.source-parts/H19UsbOutputRouteCanonicalization.patch.gz`
- `.source-parts/H18aAdaptivePracticeBarNarrowFallback.patch`

## Canonical signed authority — CI #631
CI #631 / run `35025012392` / exact source `33fb05a504be2d047259b1d967e6ab1a7e48a68c` is the authoritative signed DIGITAL PASS through H18/H18a/H19.

Evidence:
- software/unit/performance/Lint/build/provenance: PASS;
- H19 route-policy JVM tests: PASS;
- standard API36 connected regression: **23/23 PASS**, 0 failures/errors/skips;
- isolated tablet geometry: **1/1 PASS**;
- H18a narrow semantic-group discoverability: PASS;
- H18a target-tablet logical-width comparison containment: PASS;
- signed homologation: PASS;
- version `0.5.0-rc3`, versionCode `23`, package `studio.guitarlab.app`;
- unsigned APK SHA-256 `895ed8ccc957bf0bb17addfdd98806fd3425cc695443f234e27bbae62607cfd8`;
- signed APK SHA-256 `61441b92e3065ba845d9f3e0ed6791d35d41975180a01bb21b612427b493c02d`;
- certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- APK Signature Scheme v2, one RSA-4096 signer.

Keep the reporting distinction **23/23 standard + 1/1 isolated geometry**; do not flatten it to 24/24.

## Diagnostic history
CI #628 / run `35021968990` / source `ec05eec58397dc09237d163d6537eb49cfbd3650` proved the software/H19 path but ended API36 at 21/22 because the first H18 narrow fallback could scroll `Ajustes` completely outside the viewport. H18a replaced that fallback with stacked semantic groups at narrow widths.

CI #629/#630 on `60ba2740f5d1bb3fb286dd927f294aa5d188390c` stopped before build at `git diff --check`: four blank lines in the H18a source-part carried trailing spaces. Commit `33fb05a...` removed only that whitespace. The materialized H18a product output was unchanged, and #631 subsequently passed.

## H19 evidence boundary
The digital pipeline proves canonicalization policy, migration/ranking tests, compilation and integration. It cannot establish how the physical MK-300 publishes and routes its USB endpoints on the Samsung tablet. Therefore the following remain residual physical assertions:
- the MK-300 is displayed once in the output selector;
- selecting that choice produces audible playback;
- disconnect/reconnect and reselection remain correct.

## Artifact identity discipline
The exact product/source SHA for the #631 APK is `33fb05a504be2d047259b1d967e6ab1a7e48a68c`. Any later documentation-only commit must not replace this identity. If the exact #631 APK passes the residual physical checklist and no product code changes are made, no additional digital CI run is required for RC3 closure.
