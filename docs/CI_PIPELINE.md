# Android CI / release pipeline

Updated: 2026-09-16

## Contract
`.github/workflows/android-ci.yml` is manual-only (`workflow_dispatch`). Commits do not automatically consume hosted CI. The assistant must not dispatch, rerun a workflow, rerun a job, or change this policy.

The pipeline has three authority layers:
1. **software gate** — deterministic source materialization, JVM/unit/audio/DSP/persistence/migration tests, performance evidence, Lint, debug/release assembly and unsigned provenance;
2. **API36 gate** — standard connected instrumentation plus isolated target-tablet geometry;
3. **signed homologation** — signs the exact tested unsigned release artifact only after the required upstream gates pass.

## Source materialization contract
`.source-parts/` plus `scripts/materialize_ci_sources.sh` are part of the source-of-truth build contract. Unexpected source drift fails closed by exact Git blob hashes.

Canonical tail after H11b:
`H12 engine → H12 UI → H13 → H14 → H14a → H15 → H16 → H17 → H18 → H19 → H18a → H20 → H21 → H22 → H22a → H23 → H23a → H23b`.

- H23: deterministic gzip+base64 source-parts.
- H23a: test-only JUnit annotation alignment.
- H23b: deterministic gzip+base64 corrective patch over the exact H23a materialized state.
- H23b materialization verifies every modified final source/test blob, validates gzip before patching, uses `patch --dry-run`, and is idempotent on a second run.

Expected final message after H23b materialization:
`Source patch chain materialized through H23b with verified final hashes`.

## Current signed authority — CI #639
CI #639 / run `35096711936` / exact product source `eb9c4a4ca2a6269fbe2f2211a0807b8c703e115c` is authoritative through H23b:
- H23b materializer final-hash verification: PASS;
- unit/JVM suites: **260/260 PASS**, 0 failures/errors/skips;
- Android Lint gate: PASS;
- debug/release assembly and unsigned provenance: PASS;
- standard API36 connected regression: **23/23 PASS**;
- isolated 1920×1200 geometry: **1/1 PASS**;
- signed homologation: PASS;
- package/version: `studio.guitarlab.app`, `0.5.0-rc3`, versionCode `23`;
- unsigned APK SHA-256 `195aa82a581bbcc30890b278cab03bc029eb5d3376fa99130b67e24f6213e21a`;
- signed APK SHA-256 `ffac9da48c17fe2bd28172d357c2f45e906c15b20a216443c1b8b78a9a893696`;
- certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- APK Signature Scheme v2 verification: PASS, one RSA-4096 signer;
- signing bundle cleanup: PASS.

Do not flatten the Android report to 24/24; report **23/23 standard + 1/1 isolated geometry**.

## Provenance chain for CI #639
The signed job downloaded artifact `guitarlab-release-unsigned-eb9c4a4ca2a6269fbe2f2211a0807b8c703e115c`, verified its artifact digest, verified `UNSIGNED_SHA256SUMS.txt`, checked `sourceSha == GITHUB_SHA`, package/version/versionCode, then aligned and signed that exact tested APK.

The homologation artifact `BUILD_IDENTITY.txt` records:
- `commit=eb9c4a4ca2a6269fbe2f2211a0807b8c703e115c`;
- `unsignedApkSha256=195aa82a581bbcc30890b278cab03bc029eb5d3376fa99130b67e24f6213e21a`;
- `signedApkSha256=ffac9da48c17fe2bd28172d357c2f45e906c15b20a216443c1b8b78a9a893696`;
- `gate=software+android-integration-passed;physical-validation-pending`.

## Warnings versus failures
Compiler/deprecation and environment warnings are non-gating unless a job fails. CI #639 completed all required jobs successfully; no blocking Lint/test/build/signing failure remains in the run. Emulator startup emitted transient environment warnings before recovery, but both connected test executions finished `BUILD SUCCESSFUL`.

## Artifact identity discipline
A documentation-only commit after CI #639 never becomes the source of its APK. Keep exact product source SHA `eb9c4a4ca2a6269fbe2f2211a0807b8c703e115c`, APK hashes and signer identity tied to run `35096711936`.

## Next gate
No additional CI is required merely to promote documentation. The remaining gate is focused physical validation of the exact CI #639 signed candidate on Samsung SM-X230 + M-VAVE MK-300, beginning with residual fine adjustment at `0.0 ms`.
