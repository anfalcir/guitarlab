# Android CI / release pipeline

Updated: 2026-09-16

## Contract
`.github/workflows/android-ci.yml` is manual-only (`workflow_dispatch`). Commits do not automatically consume hosted CI. The assistant must not dispatch or rerun workflows/jobs without explicit user instruction.

Authority layers:
1. **software gate** — deterministic source materialization, JVM/unit/audio/DSP/persistence/migration tests, performance evidence, Lint, debug/release assembly and unsigned provenance;
2. **API36 gate** — standard connected instrumentation plus isolated target-tablet geometry;
3. **signed homologation** — signs the exact tested unsigned release artifact only after upstream gates pass.

## Source materialization
`.source-parts/` + `scripts/materialize_ci_sources.sh` are source-of-truth build inputs. Unexpected drift fails closed by exact Git blob hashes.

Canonical tail: `… → H23b → H24 → H24a → H25`.

H25 source part: `.source-parts/H25UiSettingsSafety.patch.gz.part00`.
Expected final message: `Source patch chain materialized through H25 with verified final hashes`.

## Current signed authority — CI #642
CI #642 / run `35121955150` / exact producer `7bfd876b6a5b0701ab0cf5203de36c31cd117632` is authoritative through H25.

Software gate:
- H25 materialization/final hashes: PASS;
- **269/269** JVM/unit PASS, 0 failures/errors/skips;
- performance evidence: PASS;
- Android Lint: PASS; warnings/deprecations are non-blocking and are not represented as zero warnings;
- debug + release assembly: PASS;
- unsigned release provenance: PASS;
- unsigned APK SHA-256 `f6b4f21d0f514bad06b80eabdad141dac5cd236a707842c21258ec2072868c0e`.

Android integration gate:
- standard API36: **28/28 PASS**;
- isolated 1920×1200 geometry: **1/1 PASS**.

Canonical reporting is **28/28 standard + 1/1 isolated geometry**. Do not flatten it to 29/29.

Signed homologation:
- exact same-run unsigned artifact downloaded and checksum-verified before signing;
- package `studio.guitarlab.app`;
- versionName `0.5.0-rc3`, versionCode `23`;
- zipalign verification PASS;
- APK Signature Scheme v2 PASS; v1/v3/v3.1/v4 false;
- one RSA-4096 signer;
- certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- signed APK SHA-256 `916758f694735febb8cccfe58f46f290562aaba1b5483ccc727e2be0cefba5aa`;
- signed artifact ID `10458236596`;
- signing material cleanup PASS.

`BUILD_IDENTITY.txt`, `SHA256SUMS.txt`, job logs and an independent checksum of the downloaded APK agree on producer/source and signed SHA.

## Artifact identity discipline
The signed authority SHA is always the exact workflow producer SHA. A later documentation-only commit never changes the producer identity of an existing APK.

## Next gate
No deterministic CI is required merely to reconfirm #642. The active gate is residual physical homologation using the exact #642 APK. A new full workflow becomes necessary only after a source change or deliberate next candidate.
