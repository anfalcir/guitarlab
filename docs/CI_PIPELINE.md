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

Canonical tail: `… → H23b → H24 → H24a → H25 → H26 → H26a`.

H26 source parts: `.source-parts/H26SafCloudBackup.patch.gz.part00` through `.part04`.
H26a corrective source: `.source-parts/H26aSafCopyPackageContract.patch.b64`.
Expected current final message: `Source patch chain materialized through H26a with verified final hashes`.

Pre-publication H26 materializer proof:
- exact H25 baseline → H26 PASS;
- idempotent second execution PASS;
- all 24 final hashes PASS;
- materialized files byte-identical PASS;
- source archive integrity is checked on every invocation: base64 decode + gzip CRC + fixed archive SHA-256; deliberate corruption fails closed PASS.

## Current signed authority — CI #642
CI #642 / run `35121955150` / producer `7bfd876b6a5b0701ab0cf5203de36c31cd117632` remains authoritative **through H25 only**.
- 269/269 JVM/unit PASS;
- Lint/build/provenance PASS;
- standard API36 28/28 PASS;
- isolated geometry 1/1 PASS;
- signed APK SHA-256 `916758f694735febb8cccfe58f46f290562aaba1b5483ccc727e2be0cefba5aa`;
- signer SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

## CI #643 / #644 feedback and next gate — H26a
- #643 stopped at `Diff sanity` on three Markdown trailing spaces before materialization/build.
- #644 passed `Diff sanity` and materialized H26 in both jobs, then both stopped at the same Kotlin compile mismatch in `SafBackupRemoteStore.copyPackage` (`Long` inferred vs interface `Unit`).
- H26a corrects that contract only and extends the fail-closed materializer serially.

H26/H26a changes source, so one new full manually dispatched workflow with `signed_homologation=true` is mandatory. Audit actual counts and artifact identity from that run; do not reuse #642 counts as H26 evidence.

The H26a gate must prove materialization through H26a, new/existing JVM tests, Lint/build/provenance, standard API36 + isolated geometry, exact-artifact signing and signing cleanup.

## Artifact identity discipline
The signed authority SHA is always the exact workflow producer SHA. Later documentation-only commits never change an existing APK producer identity.
