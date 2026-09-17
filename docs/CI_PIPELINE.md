# Android CI / release pipeline

Updated: 2026-09-17

## Contract
`.github/workflows/android-ci.yml` is manual-only (`workflow_dispatch`). Commits do not automatically consume hosted CI. The assistant must not dispatch or rerun workflows/jobs without explicit user instruction.

Authority layers:
1. **software gate** — deterministic source materialization, JVM/unit/audio/DSP/persistence/migration tests, performance evidence, Lint, debug/release assembly and unsigned provenance;
2. **API36 gate** — standard connected instrumentation plus isolated target-tablet geometry;
3. **signed homologation** — signs the exact tested unsigned release artifact only after upstream gates pass.

## Current source materialization
`.source-parts/` + `scripts/materialize_ci_sources.sh` are source-of-truth build inputs. Unexpected drift fails closed by exact SHA-256/Git blob checks.

Canonical tail: `… → H25 → H26 → H26a → H26b → H26e → H27 → H28`.

H28 input: `.source-parts/H28BackupIdentityConsistency.patch.gz.b64`.
Decoded gzip SHA-256: `1abd8101361b241dfb443950c2a635141b41fecca77442d86343eb3f3025d3be`.
Decoded patch SHA-256: `3d06aa851ad1dc88dd078d60bb24ea097bbca7ef4f3e93089f47c9bd4a0a6e71`.
Expected terminal message: `Source patch chain materialized through H28 with verified final hashes`.

H28 verifies exact final Git blobs for its six changed production/test files, applies only after a valid H27 state, is idempotent on a second invocation and fails before mutation if its source-part digest is wrong.

## Current signed authority — CI #653
CI #653 / run `35207902169` / producer `d09fc003e2ae2d699238eb39ba699f75a746fea4` is the signed DIGITAL PASS through H28.

Audited evidence:
- materialization through H28 PASS;
- **294/294 JVM/unit tests PASS**, 0 failures/errors/skips;
- performance evidence PASS;
- Android Lint PASS with **43 warnings + 3 hints, 0 errors** across app/platform reports;
- `assembleDebug` + `assembleRelease` PASS;
- unsigned provenance/identity PASS;
- **32/32 standard API36 PASS**;
- **1/1 isolated 1920×1200 geometry PASS**;
- exact tested-artifact signing PASS;
- signed package/version/zipalign/certificate verification PASS;
- signer certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- signed APK SHA-256 `1a36efcbe24d5995dd3609889237ca112670e52554e187d1b93ed5a8649263c0`;
- signing cleanup PASS.

Do not add standard API36 and isolated geometry counts together in canonical reporting.

## Forward gate policy
For H29 onward, every promoted source block must preserve the same exact-source discipline: deterministic materialization, current tests plus new block-specific regression, Lint/build/provenance, API36 standard + isolated geometry, and signing of the exact tested unsigned artifact.

Actual counts and hashes are always taken from the new run; prior counts are historical evidence only.

## Artifact identity discipline
The signed authority SHA is always the exact workflow producer SHA. A later source/docs HEAD never changes an earlier APK producer identity.
