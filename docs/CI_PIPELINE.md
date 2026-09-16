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

H24 is stored as deterministic gzip+base64 source part `.source-parts/H24HomeProjectLibrary.patch.gz.part00`. H24a adds `.source-parts/H24aAndroidTestCompileFix.patch.gz.part00` to correct only the instrumented-test import discovered by CI #640. Materialization validates gzip, dry-runs/apply semantics, recognizes both H24 and H24a final hashes and remains idempotent.

Expected current final message:
`Source patch chain materialized through H24a with verified final hashes`.

## Last signed authority — CI #639
CI #639 / run `35096711936` / exact producer source `eb9c4a4ca2a6269fbe2f2211a0807b8c703e115c` remains authoritative through H23b:
- JVM/unit: **260/260 PASS**;
- Lint/build/provenance: PASS;
- standard API36: **23/23 PASS**;
- isolated 1920×1200 geometry: **1/1 PASS**;
- signed homologation: PASS;
- unsigned APK SHA-256 `195aa82a581bbcc30890b278cab03bc029eb5d3376fa99130b67e24f6213e21a`;
- signed APK SHA-256 `ffac9da48c17fe2bd28172d357c2f45e906c15b20a216443c1b8b78a9a893696`;
- certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

Do not flatten the API36 report to 24/24; report **23/23 standard + 1/1 isolated geometry** for #639.

## H24/H24a pre-gate evidence
H24 is locally/source-validated against the exact materialized H23b state:
- pure Kotlin project-library policy compiles;
- deterministic behavior checks pass;
- 10,000 randomized libraries across every sort order pass deterministic invariants;
- accent/case search, combined filters, Auto/44.1/48/88.2/96 kHz and tie-breaks pass;
- forward/reverse patch round-trip is byte-exact;
- gzip/base64 integrity passes;
- first materialization passes and second materialization is idempotent;
- corruption probe fails closed;
- every modified final source/test blob matches declared H24 hashes;
- materializer `bash -n` and `git diff --check` pass;
- Home source parser scan reports no syntax/parser diagnostic without Android/Compose classpath.

CI #640 produced software-gate PASS but failed while compiling the new H24 Android test source, before instrumentation ran. H24a corrects that compile-only defect. This is still **not** an API36/signing PASS for H24/H24a; those labels require a fresh full workflow.

H24 adds Android instrumentation source for two Home-library behavior/semantics cases. Do not predict or report the next API36 pass count until the official run completes.

## Next signed gate
After documentation consolidation, the user manually runs:
`Actions → GuitarLab Android CI → main → signed_homologation=true`.

All three authority layers must be green on the exact same workflow `head_sha` before the generated APK becomes the new physical candidate.

## Artifact identity discipline
The SHA recorded as a signed authority is the exact workflow producer SHA. A documentation-only commit never retroactively changes an older APK's producer identity. Conversely, if a future workflow intentionally runs on a documentation-inclusive `main` SHA, that exact SHA is the producer identity of the new artifacts.
