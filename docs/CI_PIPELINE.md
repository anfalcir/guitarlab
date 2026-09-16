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

## Last signed authority — CI #638
CI #638 / run `35084703365` / exact source `c310be6779e6591c57399257f380588c27bdf20a` remains authoritative through H23/H23a:
- software/unit/audio/DSP/persistence/migration/performance/Lint/build/provenance: PASS;
- standard API36: **23/23 PASS**;
- isolated 1920×1200 geometry: **1/1 PASS**;
- signed homologation: PASS;
- unsigned APK SHA-256 `621e02355d265bdb6c24cb5e324b445b63e739f8e7d6adc233eebea3b31fe0d5`;
- signed APK SHA-256 `a650afa5edfd2b8c4f8393e65b314ae9fbb59487a87c2d3ea85ef978d7d895dc`;
- certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

Do not flatten the API36 report to 24/24; report **23/23 standard + 1/1 isolated geometry**.

## H23b PRE-GATE evidence
Before repository publication, H23b is locally source-validated against the exact #638 materialized source:
- pure Kotlin timing/calibration/feedback policies compile;
- 100,000 randomized placement cases pass invariants/determinism;
- 44.1/48/88.2/96 kHz conversion checks pass;
- H23b patch forward/reverse round-trip is byte-exact;
- gzip/base64 split archive integrity passes;
- first materialization passes and second materialization is idempotent;
- all final modified blobs match the declared materializer hashes;
- `git diff --check` passes.

This is **not** an Android build/Lint/API36/signing PASS for H23b. Those labels are reserved for the official workflow.

## Next signed gate
After the H23b commit lands on `main`, the user manually runs:
`Actions → GuitarLab Android CI → main → signed_homologation=true`.

All three authority layers must be green on the exact same H23b SHA before that APK can become the new physical candidate.

## Artifact identity discipline
A later documentation-only commit never becomes the source of an older APK. Keep product source SHA, APK hash and signing identity tied to the workflow run that actually produced the artifact.
