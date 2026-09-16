# Android CI / release pipeline

Updated: 2026-09-16

## Contract
`.github/workflows/android-ci.yml` is manual-only (`workflow_dispatch`). Commits do not automatically consume hosted CI. The assistant must not dispatch, rerun a workflow, rerun a job or change this policy.

The pipeline has three authority layers:
1. **software gate** — deterministic source materialization, JVM/unit/audio/DSP/persistence/migration tests, performance evidence, Lint, debug/release assembly and unsigned provenance;
2. **API36 gate** — standard connected instrumentation plus isolated target-tablet geometry;
3. **signed homologation** — signs the exact tested unsigned release artifact only after required upstream gates pass.

## Source materialization contract
`.source-parts/` plus `scripts/materialize_ci_sources.sh` are part of the source-of-truth build contract. Unexpected source drift fails closed by exact Git blob hashes.

Canonical tail after H11b:
`H12 engine → H12 UI → H13 → H14 → H14a → H15 → H16 → H17 → H18 → H19 → H18a → H20 → H21 → H22 → H22a → H23 → H23a → H23b → H24 → H24a`.

H24 uses `.source-parts/H24HomeProjectLibrary.patch.gz.part00`. H24a uses `.source-parts/H24aAndroidTestCompileFix.patch.gz.part00` and corrects only the instrumented-test import discovered by CI #640.

Expected final materializer message:
`Source patch chain materialized through H24a with verified final hashes`.

## Current signed authority — CI #641
CI #641 / run `35105065689` / exact producer source `b11769f340f7056c37dfb17d95b062909dad87bf` is authoritative through H24a.

Software gate:
- **269/269** JVM/unit tests PASS, 0 failures/errors/skips;
- Android Lint gate PASS; warnings/deprecations are non-blocking and are not represented as zero warnings;
- debug + release assembly PASS;
- unsigned release provenance PASS;
- unsigned APK SHA-256 `824e8c070ebee6bbf920990dce3c948d2fc3474610524f3a087c38bd27ca5248`.

Android integration gate:
- standard API36: **25/25 PASS**;
- isolated 1920×1200 geometry: **1/1 PASS**;
- H24 Android test source compiles and participates in the standard regression.

Canonical reporting is **25/25 standard + 1/1 isolated geometry**. Do not flatten it to 26/26.

Signed homologation:
- exact same-run unsigned artifact was downloaded and checksum-verified before signing;
- package `studio.guitarlab.app`;
- versionName `0.5.0-rc3`, versionCode `23`;
- zipalign verification PASS;
- APK Signature Scheme v2 PASS; v1/v3/v4 disabled;
- signer count 1, RSA 4096;
- certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- signed APK SHA-256 `d3698067ed7117d3c3c844b0d94bb117c3448cac3da2897329e4dbfcd71e0f39`;
- signed artifact ID `10450495802`;
- signing material cleanup PASS.

`BUILD_IDENTITY.txt`, `SHA256SUMS.txt`, job logs and independently recalculated downloaded-APK checksum agree on the producer/source and signed SHA.

## Historical CI #640
CI #640 / run `35103316449` remains point-in-time evidence:
- software gate PASS;
- API36 job failed before instrumentation at `:app:compileDebugAndroidTestKotlin` because H24 imported `assertDoesNotExist` as an unavailable top-level symbol;
- signing skipped.

H24a corrected only that test-source issue. CI #641 is the full superseding authority.

## Artifact identity discipline
The signed authority SHA is always the exact workflow producer SHA. A later documentation-only commit never changes the producer identity of an existing APK.

For the current physical candidate, product/source identity remains `b11769f340f7056c37dfb17d95b062909dad87bf` even after documentation promotion.

## Next gate
No new deterministic CI is required merely to reconfirm #641. The active gate is residual physical homologation using the exact #641 APK. A new full workflow becomes necessary only if source changes after a physical finding or a deliberate next release candidate.
